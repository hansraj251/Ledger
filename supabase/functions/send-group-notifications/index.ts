
import { createClient } from "npm:@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

const FCM_PROJECT_ID = Deno.env.get("FCM_PROJECT_ID")!;
const FCM_CLIENT_EMAIL = Deno.env.get("FCM_CLIENT_EMAIL")!;
const FCM_PRIVATE_KEY = Deno.env.get("FCM_PRIVATE_KEY")!;
const WORKER_SECRET = Deno.env.get("NOTIFICATION_WORKER_SECRET")!;

const supabase = createClient(SUPABASE_URL, SERVICE_ROLE_KEY);

function base64Url(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);

  return btoa(binary)
    .replace(/=/g, "")
    .replace(/\+/g, "-")
    .replace(/\//g, "_");
}

function encodeJson(value: unknown): string {
  return base64Url(
    new TextEncoder().encode(JSON.stringify(value))
  );
}

async function getGoogleAccessToken(): Promise<string> {
  const now = Math.floor(Date.now() / 1000);

  const header = encodeJson({
    alg: "RS256",
    typ: "JWT",
  });

  const claim = encodeJson({
    iss: FCM_CLIENT_EMAIL,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  });

  const unsignedToken = `${header}.${claim}`;

  const pem = FCM_PRIVATE_KEY
    .replace(/\\n/g, "\n")
    .replace("-----BEGIN PRIVATE KEY-----", "")
    .replace("-----END PRIVATE KEY-----", "")
    .replace(/\s/g, "");

  const binaryKey = Uint8Array.from(
    atob(pem),
    (c) => c.charCodeAt(0)
  );

  const privateKey = await crypto.subtle.importKey(
    "pkcs8",
    binaryKey.buffer,
    {
      name: "RSASSA-PKCS1-v1_5",
      hash: "SHA-256",
    },
    false,
    ["sign"]
  );

  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    privateKey,
    new TextEncoder().encode(unsignedToken)
  );

  const jwt = `${unsignedToken}.${base64Url(new Uint8Array(signature))}`;

  const response = await fetch(
    "https://oauth2.googleapis.com/token",
    {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body: new URLSearchParams({
        grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
        assertion: jwt,
      }),
    }
  );

  const result = await response.json();

  if (!response.ok || !result.access_token) {
    throw new Error(
      `Google OAuth failed: ${JSON.stringify(result)}`
    );
  }

  return result.access_token;
}

async function sendFcm(
  accessToken: string,
  token: string,
  title: string,
  body: string,
  data: Record<string, string>
) {
  const response = await fetch(
    `https://fcm.googleapis.com/v1/projects/${FCM_PROJECT_ID}/messages:send`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        message: {
          token,
          data: {
            ...data,
            title,
            body,
          },
          android: {
            priority: "high",
          },
        },
      }),
    }
  );

  const result = await response.json().catch(() => ({}));

  if (!response.ok) {
    const errorCode = result?.error?.details?.find(
      (item: any) =>
        item["@type"]?.includes("FcmError")
    )?.errorCode;

    return {
      success: false,
      invalidToken:
        response.status === 404 ||
        errorCode === "UNREGISTERED",
      error: JSON.stringify(result),
    };
  }

  return {
    success: true,
    invalidToken: false,
    error: null,
  };
}

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return new Response("Method not allowed", { status: 405 });
  }

  if (
    !WORKER_SECRET ||
    req.headers.get("x-worker-secret") !== WORKER_SECRET
  ) {
    return new Response("Unauthorized", { status: 401 });
  }

  try {
    const accessToken = await getGoogleAccessToken();

    const { data: events, error: claimError } =
      await supabase.rpc("claim_group_notifications", {
        p_batch_size: 50,
      });

    if (claimError) throw claimError;

    let sent = 0;
    let failed = 0;

    for (const event of events ?? []) {
      const { data: devices, error: tokenError } = await supabase
        .from("user_fcm_tokens")
        .select("id, fcm_token")
        .eq("user_id", event.recipient_user_id)
        .eq("is_active", true);

      if (tokenError) {
        throw tokenError;
      }

      if (!devices || devices.length === 0) {
        const nextStatus =
          event.attempts >= 8 ? "failed" : "pending";

        await supabase
          .from("notification_outbox")
          .update({
            status: nextStatus,
            last_error: "No active FCM device tokens found",
          })
          .eq("id", event.id);

        failed++;
        continue;
      }

      let allSucceeded = true;
      const errors: string[] = [];

      for (const device of devices) {
        const result = await sendFcm(
          accessToken,
          device.fcm_token,
          event.title,
          event.body,
          Object.fromEntries(
            Object.entries(event.data ?? {}).map(
              ([key, value]) => [key, String(value)]
            )
          )
        );

        if (result.invalidToken) {
          await supabase
            .from("user_fcm_tokens")
            .update({
              is_active: false,
              updated_at: new Date().toISOString(),
            })
            .eq("id", device.id);
        }

        if (!result.success) {
          allSucceeded = false;
          errors.push(result.error ?? "FCM delivery failed");
        }
      }

      if (allSucceeded) {
        await supabase
          .from("notification_outbox")
          .update({
            status: "sent",
            processed_at: new Date().toISOString(),
            last_error: null,
          })
          .eq("id", event.id);

        sent++;
      } else {
        const nextStatus =
          event.attempts >= 8 ? "failed" : "pending";

        await supabase
          .from("notification_outbox")
          .update({
            status: nextStatus,
            last_error: errors.join(" | ").slice(0, 4000),
          })
          .eq("id", event.id);

        failed++;
      }
    }

    return Response.json({
      success: true,
      processed: events?.length ?? 0,
      sent,
      failed,
    });
  } catch (error) {
    console.error("Notification worker error:", error);

    return Response.json(
      {
        success: false,
        error: String(error),
      },
      { status: 500 }
    );
  }
});
