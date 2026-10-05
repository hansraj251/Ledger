require("dotenv").config();
const YOUTUBE_API_BASE =
    "https://www.googleapis.com/youtube/v3";
const db =
    require("../db");
const getYouTubeApiKey = () => {
    const apiKey =
        process.env.YOUTUBE_API_KEY;

    if (!apiKey) {
        throw new Error(
            "YOUTUBE_API_KEY is not configured."
        );
    }

    return apiKey;
};
const getVideoDuration = async (
    videoId
) => {

    if (!videoId) {
        return null;
    }

    const params =
        new URLSearchParams({
            part: "snippet,contentDetails",
            id: String(videoId),
            key: getYouTubeApiKey()
        });

    const response =
        await fetch(
            `${YOUTUBE_API_BASE}/videos?${params.toString()}`
        );

    let data = {};

    try {
        data = await response.json();
    }
    catch {
        return null;
    }

    if (!response.ok) {
        console.error(
            "YouTube duration API error:",
            data
        );

        return null;
    }

    return (
        data?.items?.[0]?.contentDetails?.duration ||
        null
    );
};
const saveSongsToMusicDB = async (songs = []) => {
    for (const song of songs) {
        const videoId =
            String(song?.videoId || "").trim();

        if (!videoId) {
            continue;
        }

        try {
            const existing =
                await db.getAsync(
                    `
                    SELECT id
                    FROM music_songs
                    WHERE youtube_video_id = ?
                    LIMIT 1
                    `,
                    [videoId]
                );

            if (existing?.id) {
                await db.runAsync(
                    `
                    UPDATE music_songs
                    SET
  title = ?,
  artist = ?,
  channel_title = ?,
  channel_id = ?,
  thumbnail_url = ?,
                        duration = ?,
                        language = ?,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE id = ?
                    `,
                    [
                        song.title || "",
                        song.channelTitle || "",
                        song.channelTitle || "",
                        song.channelId || null,
                        song.thumbnails?.high ||
                            song.thumbnails?.medium ||
                            song.thumbnails?.default ||
                            null,
                        song.duration || null,
                        song.language || null,
                        existing.id
                    ]
                );
            } else {
                await db.runAsync(
                    `
                    INSERT INTO music_songs (
                        youtube_video_id,
                        title,
                        artist,
                        channel_title,
                         channel_id,
                        thumbnail_url,
                        duration,
                        language,
                        updated_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    `,
                    [
                        videoId,
                        song.title || "",
                        song.channelTitle || "",
                        song.channelTitle || "",
                        song.channelId || null,
                        song.thumbnails?.high ||
                            song.thumbnails?.medium ||
                            song.thumbnails?.default ||
                            null,
                        song.duration || null,
                        song.language || null
                    ]
                );
            }
        } catch (error) {
            console.error(
                "Failed to save music song to DB:",
                videoId,
                error
            );
        }
    }
};
const seedMusicArtists = async (artists = []) => {
    for (const item of artists) {
        const artist = String(item?.artist || "").trim();

        if (!artist) {
            continue;
        }

        try {
            const existing = await db.getAsync(
                `
                SELECT id
                FROM music_artists
                WHERE
                    LOWER(artist) = LOWER(?)
                    AND (
                        channel_id = ?
                        OR (
                            channel_id IS NULL
                            AND ? IS NULL
                        )
                    )
                LIMIT 1
                `,
                [
                    artist,
                    item.channelId || null,
                    item.channelId || null
                ]
            );

            if (existing?.id) {
                await db.runAsync(
                    `
                    UPDATE music_artists
                    SET
                        channel = ?,
                        channel_id = ?,
                        language = ?,
                        status = 'active',
                        updated_at = CURRENT_TIMESTAMP
                    WHERE id = ?
                    `,
                    [
                        item.channel || null,
                        item.channelId || null,
                        item.language || null,
                        existing.id
                    ]
                );
            } else {
                await db.runAsync(
                    `
                    INSERT INTO music_artists (
                        artist,
                        channel,
                        channel_id,
                        language,
                        status
                    )
                    VALUES (?, ?, ?, ?, 'active')
                    `,
                    [
                        artist,
                        item.channel || null,
                        item.channelId || null,
                        item.language || null
                    ]
                );
            }
        } catch (error) {
            console.error(
                "Failed to seed music artist:",
                artist,
                error
            );
        }
    }
};
const discoverAndSaveArtist = async (artist, language = null) => {
    const cleanArtist = String(artist || "").trim();

    if (!cleanArtist) {
        return null;
    }
    const existingArtists = await db.allAsync(
    `
    SELECT
        artist,
        channel,
        channel_id AS channelId,
        language
    FROM music_artists
    WHERE LOWER(TRIM(artist)) = ?
      AND status = 'active'
    `,
    [
        cleanArtist.toLowerCase()
    ]
);

if (existingArtists?.length) {
    const matchingLanguage = language
        ? existingArtists.find(item =>
            String(item?.language || "")
                .trim()
                .toLowerCase() ===
            String(language)
                .trim()
                .toLowerCase()
        )
        : existingArtists[0];

    if (matchingLanguage) {
        return {
            artist: matchingLanguage.artist,
            channel: matchingLanguage.channel || null,
            channelId: matchingLanguage.channelId || null,
            language: matchingLanguage.language || language || null
        };
    }
}

    try {
        const result = await searchMusic({
    query: `${cleanArtist} official`,
    maxResults: 5,
    order: "relevance"
});

        const items = Array.isArray(result?.items)
            ? result.items
            : [];

        if (!items.length) {
            return null;
        }

        /*
         * Prefer a channel appearing repeatedly in the results.
         */
        const channelMap = new Map();

        for (const item of items) {
            const channelId = String(
                item?.channelId || ""
            ).trim();

            const channelTitle = String(
                item?.channelTitle || ""
            ).trim();

            if (!channelId) {
                continue;
            }

            const count =
                channelMap.get(channelId) || {
                    count: 0,
                    title: channelTitle
                };

            count.count += 1;

            if (!count.title && channelTitle) {
                count.title = channelTitle;
            }

            channelMap.set(channelId, count);
        }

        const channels = [...channelMap.entries()]
            .sort((a, b) => b[1].count - a[1].count);

        if (!channels.length) {
            return null;
        }

        const [
            channelId,
            channelData
        ] = channels[0];

        await seedMusicArtists([
            {
                artist: cleanArtist,
                channel: channelData.title || null,
                channelId,
                language
            }
        ]);

        return {
            artist: cleanArtist,
            channel: channelData.title || null,
            channelId,
            language
        };

    } catch (error) {
        console.error(
            "Failed to discover artist channel:",
            cleanArtist,
            error
        );

        return null;
    }
};
const addYouTubeVideo = async (youtubeUrl) => {
    const url = String(youtubeUrl || "").trim();

    if (!url) {
        throw new Error("YouTube URL is required.");
    }

    let videoId = null;

    try {
        const parsed = new URL(url);

        if (
            parsed.hostname === "youtube.com" ||
            parsed.hostname === "www.youtube.com" ||
            parsed.hostname === "m.youtube.com"
        ) {
            videoId = parsed.searchParams.get("v");
        }

        if (
            parsed.hostname === "youtu.be" ||
            parsed.hostname === "www.youtu.be"
        ) {
            videoId = parsed.pathname.split("/")[1];
        }
    } catch {
        throw new Error("Invalid YouTube URL.");
    }

    videoId = String(videoId || "").trim();

    if (!/^[A-Za-z0-9_-]{11}$/.test(videoId)) {
        throw new Error("Invalid YouTube video URL.");
    }

    const existing = await db.getAsync(
        `
        SELECT
            youtube_video_id AS videoId,
            title,
            artist,
            channel_title AS channelTitle,
            channel_id AS channelId,
            thumbnail_url AS thumbnailUrl,
            duration,
            language
        FROM music_songs
        WHERE youtube_video_id = ?
        LIMIT 1
        `,
        [videoId]
    );

    if (existing) {
        return {
            ...existing,
            alreadyExists: true
        };
    }

    const params = new URLSearchParams({
        part: "snippet,contentDetails",
        id: videoId,
        key: getYouTubeApiKey()
    });

    const response = await fetch(
        `${YOUTUBE_API_BASE}/videos?${params.toString()}`
    );

    let data = {};

    try {
        data = await response.json();
    } catch {
        data = {};
    }

    if (!response.ok) {
        throw new Error(
            data?.error?.message ||
            "Unable to fetch YouTube video."
        );
    }

    const video = data?.items?.[0];

    if (!video) {
        throw new Error("YouTube video not found.");
    }

    const song = {
        videoId: video.id,
        title: video?.snippet?.title || "",
        description: video?.snippet?.description || "",
        channelTitle:
            video?.snippet?.channelTitle || "",
        channelId:
            video?.snippet?.channelId || null,
        publishedAt:
            video?.snippet?.publishedAt || null,
        duration:
            video?.contentDetails?.duration || null,
        language:
            video?.snippet?.defaultAudioLanguage || null,
        thumbnails: {
            default:
                video?.snippet?.thumbnails?.default?.url || null,
            medium:
                video?.snippet?.thumbnails?.medium?.url || null,
            high:
                video?.snippet?.thumbnails?.high?.url || null
        }
    };

    await saveSongsToMusicDB([song]);

    return {
        ...song,
        alreadyExists: false
    };
};
const extractYouTubeVideoIdsFromHtml = (html) => {
    console.log(
    "DDG HTML SAMPLE:",
    String(html || "").slice(0, 5000)
);
    const ids = new Set();

    const source = String(html || "");

    const addVideoId = (value) => {
        const id = String(value || "").trim();

        if (/^[A-Za-z0-9_-]{11}$/.test(id)) {
            ids.add(id);
        }
    };

    // 1. DuckDuckGo redirect URLs
    for (const match of source.matchAll(/uddg=([^"&]+)/g)) {
        try {
            const url = decodeURIComponent(match[1]);

            const videoMatch = url.match(
                /youtube\.com\/watch\?(?:[^#]*&)?v=([A-Za-z0-9_-]{11})/
            );

            if (videoMatch) {
                addVideoId(videoMatch[1]);
            }
        } catch {
            // Ignore malformed result
        }
    }

    // 2. Direct YouTube watch URLs present in HTML
    for (const match of source.matchAll(
        /https?:\/\/(?:www\.)?youtube\.com\/watch\?[^"'<>\\\s]*v=([A-Za-z0-9_-]{11})/g
    )) {
        addVideoId(match[1]);
    }

    // 3. HTML-encoded URLs
    const decodedHtml = source
        .replace(/&amp;/g, "&")
        .replace(/&quot;/g, '"')
        .replace(/&#x2F;/g, "/")
        .replace(/\\u002F/g, "/");

    for (const match of decodedHtml.matchAll(
        /youtube\.com\/watch\?[^"'<>\\\s]*v=([A-Za-z0-9_-]{11})/g
    )) {
        addVideoId(match[1]);
    }

    return [...ids];
};
const searchMusic = async ({
    query,
    maxResults = 20,
    pageToken = "",
    order = "relevance"
}) => {

    const cleanQuery =
        String(query || "").trim();

    if (!cleanQuery) {
        throw new Error(
            "Search query is required."
        );
    }
    const searchTerm = cleanQuery.toLowerCase().trim();
const likeTerm = `%${searchTerm}%`;

const dbSongs = await db.allAsync(
    `
    SELECT
        youtube_video_id AS videoId,
        title,
        artist,
        channel_title AS channelTitle,
        channel_id AS channelId,
        thumbnail_url AS thumbnailUrl,
        duration,
        language,
        updated_at
    FROM music_songs
    WHERE
        LOWER(title) LIKE ?
        OR LOWER(artist) LIKE ?
        OR LOWER(channel_title) LIKE ?
    ORDER BY updated_at DESC
    LIMIT ?
    `,
    [
        likeTerm,
        likeTerm,
        likeTerm,
        Math.min(
            Math.max(
                Number(maxResults) || 12,
                1
            ),
            50
        )
    ]
);

if (dbSongs?.length) {
    console.log(
        "Music DB search HIT:",
        cleanQuery,
        dbSongs.length
    );

    return {
        items: dbSongs.map(song => ({
            videoId: song.videoId || null,
            title: song.title || "",
            description: "",
            channelTitle:
                song.channelTitle || "",
            channelId:
                song.channelId || null,
            publishedAt:
                song.updated_at || null,
            duration:
                song.duration || null,
            language:
                song.language || null,
            thumbnails: {
                default:
                    song.thumbnailUrl || null,
                medium:
                    song.thumbnailUrl || null,
                high:
                    song.thumbnailUrl || null
            }
        })),
        nextPageToken: null,
        prevPageToken: null,
        totalResults: dbSongs.length
    };
}

    const params =
        new URLSearchParams({
            part: "snippet",
            q: cleanQuery,
            type: "video",
            order: order === "date" ? "date" : "relevance",
            videoCategoryId: "10",
            maxResults: String(
                Math.min(
                    Math.max(
                        Number(maxResults) || 12,
                        1
                    ),
                    50
                )
            ),
            regionCode: "IN",
            relevanceLanguage: "en",
            videoEmbeddable: "true",
            key: getYouTubeApiKey()
        });

    if (pageToken) {
        params.set(
            "pageToken",
            String(pageToken)
        );
    }

   const braveApiKeys = [
    process.env.BRAVE_SEARCH_API_KEY,
    process.env.BRAVE_SEARCH_API_KEY_2
].filter(Boolean);

console.log(
    "BRAVE SEARCH CONFIG:",
    {
        keyCount: braveApiKeys.length,
        query: cleanQuery
    }
);

if (braveApiKeys.length) {
    try {
        const braveParams =
            new URLSearchParams({
                q: `site:youtube.com/watch ${cleanQuery}`,
                count: String(
                    Math.min(
                        Math.max(
                            Number(maxResults) || 10,
                            1
                        ),
                        10
                    )
                )
            });

        let braveResponse = null;

for (let i = 0; i < braveApiKeys.length; i++) {
    const currentBraveApiKey =
        braveApiKeys[i];

    try {
        console.log(
            `Brave Search: trying key ${i + 1}`
        );

        const response =
            await fetch(
                `https://api.search.brave.com/res/v1/web/search?${braveParams.toString()}`,
                {
                    headers: {
                        Accept: "application/json",
                        "X-Subscription-Token":
                            currentBraveApiKey
                    }
                }
            );

        if (response.ok) {
            braveResponse = response;

            console.log(
                `Brave Search: key ${i + 1} succeeded`
            );

            break;
        }

        console.warn(
            `Brave Search: key ${i + 1} failed with status ${response.status}`
        );

    } catch (error) {
        console.warn(
            `Brave Search: key ${i + 1} request failed:`,
            error.message
        );
    }
}

        let braveData = {};

        try {
            braveData =
                await braveResponse.json();
        } catch {
            braveData = {};
        }

        if (braveResponse?.ok) {
            const braveItems =
                Array.isArray(
                    braveData?.web?.results
                )
                    ? braveData.web.results
                    : [];

            const braveVideoIds = [];

            for (const item of braveItems) {
                const link =
                    String(
                        item?.url || ""
                    ).trim();

                const match =
                    link.match(
                        /(?:youtube\.com\/watch\?[^#]*v=|youtu\.be\/)([A-Za-z0-9_-]{11})/
                    );

                if (
                    match?.[1] &&
                    !braveVideoIds.includes(
                        match[1]
                    )
                ) {
                    braveVideoIds.push(
                        match[1]
                    );
                }
            }

            console.log(
                "Brave YouTube IDs:",
                braveVideoIds
            );

            if (braveVideoIds.length) {
                const videoParams =
                    new URLSearchParams({
                        part:
                            "snippet,contentDetails",
                        id:
                            braveVideoIds
                                .slice(0, 10)
                                .join(","),
                        key:
                            getYouTubeApiKey()
                    });

                const videoResponse =
                    await fetch(
                        `${YOUTUBE_API_BASE}/videos?${videoParams.toString()}`
                    );

                let videoData = {};

                try {
                    videoData =
                        await videoResponse.json();
                } catch {
                    videoData = {};
                }

                if (videoResponse.ok) {
                    const normalizedSongs =
                        (
                            videoData.items ||
                            []
                        ).map(video => ({
                            videoId:
                                video.id,

                            title:
                                video?.snippet?.title ||
                                "",

                            description:
                                video?.snippet?.description ||
                                "",

                            channelTitle:
                                video?.snippet?.channelTitle ||
                                "",

                            channelId:
                                video?.snippet?.channelId ||
                                null,

                            publishedAt:
                                video?.snippet?.publishedAt ||
                                null,

                            duration:
                                video?.contentDetails?.duration ||
                                null,

                            language:
                                video?.snippet?.defaultAudioLanguage ||
                                null,

                            thumbnails: {
                                default:
                                    video?.snippet?.thumbnails?.default?.url ||
                                    null,

                                medium:
                                    video?.snippet?.thumbnails?.medium?.url ||
                                    null,

                                high:
                                    video?.snippet?.thumbnails?.high?.url ||
                                    null
                            }
                        }));

                    await saveSongsToMusicDB(
                        normalizedSongs
                    );

                    if (
                        normalizedSongs.length
                    ) {
                        return {
                            items:
                                normalizedSongs,

                            nextPageToken:
                                null,

                            prevPageToken:
                                null,

                            totalResults:
                                normalizedSongs.length
                        };
                    }
                }
            }
        } else {
    console.error(
        "BRAVE SEARCH FAILED:",
        braveResponse?.status || "NO_RESPONSE",
        braveData
    );
        }
    } catch (error) {
        console.warn(
            "Brave YouTube search failed:",
            error.message
        );
    }
}

    const response =
        await fetch(
            `${YOUTUBE_API_BASE}/search?${params.toString()}`
        );

    let data = {};

    try {
        data = await response.json();
    }
    catch {
        throw new Error(
            "Invalid response from YouTube API."
        );
    }

    if (!response.ok) {
    console.error(
        "YouTube API error:",
        data
    );

    const reason =
        data?.error?.errors?.[0]?.reason || "";

    const isQuotaExceeded =
        reason === "quotaExceeded" ||
        reason === "dailyLimitExceeded" ||
        String(data?.error?.message || "")
            .toLowerCase()
            .includes("quota exceeded");

    if (isQuotaExceeded) {
        console.warn(
            "YouTube quota exceeded. Falling back to music_songs DB."
        );

        const searchTerm =
            cleanQuery.toLowerCase().trim();

        const likeTerm = `%${searchTerm}%`;

        const dbSongs =
            await db.allAsync(
                `
                SELECT
                    youtube_video_id AS videoId,
                    title,
                    artist,
                    channel_title AS channelTitle,
                    channel_id AS channelId,
                    thumbnail_url AS thumbnailUrl,
                    duration,
                    language,
                    updated_at
                FROM music_songs
                WHERE
                    LOWER(title) LIKE ?
                    OR LOWER(artist) LIKE ?
                    OR LOWER(channel_title) LIKE ?
                ORDER BY updated_at DESC
                LIMIT ?
                `,
                [
                    likeTerm,
                    likeTerm,
                    likeTerm,
                    Math.min(
                        Math.max(
                            Number(maxResults) || 12,
                            1
                        ),
                        50
                    )
                ]
            );

        return {
            items: (dbSongs || []).map(song => ({
                videoId:
                    song.videoId || null,

                title:
                    song.title || "",

                description:
                    "",

                channelTitle:
                    song.channelTitle || "",

                channelId:
                    null,

                publishedAt:
                    song.updated_at || null,

                duration:
                    song.duration || null,

                language:
                    song.language || null,

                thumbnails: {
                    default:
                        song.thumbnailUrl || null,

                    medium:
                        song.thumbnailUrl || null,

                    high:
                        song.thumbnailUrl || null
                }
            })),

            nextPageToken: null,
            prevPageToken: null,
            totalResults:
                Array.isArray(dbSongs)
                    ? dbSongs.length
                    : 0
        };
    }

    throw new Error(
        data?.error?.message ||
        "Unable to search YouTube."
    );
}
    const items =
        Array.isArray(data.items)
            ? data.items
            : [];
    const videoIds =
    items
        .map(item => item?.id?.videoId)
        .filter(Boolean);

let durationMap = {};
let languageMap = {};
if (videoIds.length) {
    const durationParams =
        new URLSearchParams({
            part: "snippet,contentDetails",
            id: videoIds.join(","),
            key: getYouTubeApiKey()
        });

    const durationResponse =
        await fetch(
            `${YOUTUBE_API_BASE}/videos?${durationParams.toString()}`
        );

    let durationData = {};

    try {
        durationData =
            await durationResponse.json();
    }
    catch {
        durationData = {};
    }

    if (durationResponse.ok) {
    durationMap =
        Object.fromEntries(
            (durationData.items || []).map(video => [
                video.id,
                video?.contentDetails?.duration || null
            ])
        );

    languageMap =
        Object.fromEntries(
            (durationData.items || []).map(video => [
                video.id,
                video?.snippet?.defaultAudioLanguage || null
            ])
        );
}
}
const filteredItems =
    items.filter(item => {
        const duration =
            durationMap[item?.id?.videoId];

        if (!duration) {
            return true;
        }
        

        return !/^PT([0-5]?[0-9]S|[0-5]?[0-9]M)$/.test(duration);
    });  
    const normalizedSongs =
    filteredItems.map(item => ({
        videoId:
            item?.id?.videoId || null,

        title:
            item?.snippet?.title || "",

        description:
            item?.snippet?.description || "",

        channelTitle:
            item?.snippet?.channelTitle || "",

        channelId:
            item?.snippet?.channelId || null,

        publishedAt:
            item?.snippet?.publishedAt || null,

        duration:
            durationMap[item?.id?.videoId] || null,

        language:
            languageMap[item?.id?.videoId] || null,

        thumbnails: {
            default:
                item?.snippet?.thumbnails?.default?.url ||
                null,

            medium:
                item?.snippet?.thumbnails?.medium?.url ||
                null,

            high:
                item?.snippet?.thumbnails?.high?.url ||
                null
        }
    }));

await saveSongsToMusicDB(normalizedSongs);      

    return {
    items: normalizedSongs,

        nextPageToken:
            data.nextPageToken || null,

        prevPageToken:
            data.prevPageToken || null,

        totalResults:
            data?.pageInfo?.totalResults || 0
    };
};
const getCatalogueSongsForArtists = async ({
  artists = [],
  channels = [],
  channelIds = [],
  languages = [],
  limit = 12
} = {}) => {
  const cleanArtists = [...new Set(
    (artists || [])
      .map(v => String(v || "").trim().toLowerCase())
      .filter(Boolean)
  )];

  const cleanChannels = [...new Set(
    (channels || [])
      .map(v => String(v || "").trim().toLowerCase())
      .filter(Boolean)
  )];

  const cleanChannelIds = [...new Set(
    (channelIds || [])
      .map(v => String(v || "").trim())
      .filter(Boolean)
  )];

  const cleanLanguages = [...new Set(
    (languages || [])
      .map(v => String(v || "").trim().toLowerCase())
      .filter(Boolean)
  )];

  if (
    !cleanArtists.length &&
    !cleanChannels.length &&
    !cleanChannelIds.length &&
    !cleanLanguages.length
  ) {
    return [];
  }

  const conditions = [];
  const params = [];

  if (cleanArtists.length) {
    conditions.push(
      `LOWER(TRIM(COALESCE(s.artist, ''))) IN (${cleanArtists
        .map(() => "?")
        .join(",")})`
    );

    params.push(...cleanArtists);
  }

  if (cleanChannels.length) {
    conditions.push(
      `LOWER(TRIM(COALESCE(s.channel_title, ''))) IN (${cleanChannels
        .map(() => "?")
        .join(",")})`
    );

    params.push(...cleanChannels);
  }

  if (cleanChannelIds.length) {
    conditions.push(
      `s.channel_id IN (${cleanChannelIds
        .map(() => "?")
        .join(",")})`
    );

    params.push(...cleanChannelIds);
  }

  if (cleanLanguages.length) {
    conditions.push(
      `LOWER(TRIM(COALESCE(s.language, ''))) IN (${cleanLanguages
        .map(() => "?")
        .join(",")})`
    );

    params.push(...cleanLanguages);
  }

  const safeLimit = Math.min(
    Math.max(Number(limit) || 12, 1),
    100
  );

  const rows = await db.allAsync(
    `
    SELECT
      s.youtube_video_id AS videoId,
      s.title,
      s.artist,
      s.channel_title AS channelTitle,
      s.channel_id AS channelId,
      s.thumbnail_url AS thumbnailUrl,
      s.duration,
      s.language,
      s.updated_at
    FROM music_songs s
    WHERE ${conditions.join(" OR ")}
    ORDER BY s.updated_at DESC
    LIMIT ?
    `,
    [
      ...params,
      safeLimit
    ]
  );

  const artistCatalogue = await db.allAsync(
  `
  SELECT
    artist,
    channel,
    channel_id AS channelId,
    language
  FROM music_artists
  WHERE status = 'active'
  `
);

const catalogueByChannel = new Map();
const catalogueByArtist = new Map();

for (const item of artistCatalogue || []) {
  const artistName = String(item?.artist || "")
    .trim()
    .toLowerCase();

  const channelName = String(item?.channel || "")
    .trim()
    .toLowerCase();

  if (artistName) {
    if (!catalogueByArtist.has(artistName)) {
      catalogueByArtist.set(artistName, []);
    }

    catalogueByArtist.get(artistName).push(item);
  }

  if (channelName) {
    if (!catalogueByChannel.has(channelName)) {
      catalogueByChannel.set(channelName, []);
    }

    catalogueByChannel.get(channelName).push(item);
  }
}

return (rows || [])
  .filter(song => {
    const songArtist = String(song?.artist || "")
      .trim()
      .toLowerCase();

    const songChannel = String(song?.channelTitle || "")
      .trim()
      .toLowerCase();

    const songChannelId = String(song?.channelId || "")
      .trim();

    const artistEntries =
      catalogueByArtist.get(songArtist) || [];

    const channelEntries =
      catalogueByChannel.get(songChannel) || [];

    const artistCatalogueMatch =
      artistEntries.some(item => {
        const catalogueChannel =
          String(item?.channel || "")
            .trim()
            .toLowerCase();

        const catalogueChannelId =
          String(item?.channelId || "")
            .trim();

        return (
          (
            catalogueChannel &&
            songChannel &&
            catalogueChannel === songChannel
          ) ||
          (
            catalogueChannelId &&
            songChannelId &&
            catalogueChannelId === songChannelId
          )
        );
      });

    const requestedArtistMatch =
      cleanArtists.includes(songArtist);

    const requestedChannelMatch =
      cleanChannels.includes(songChannel);

    const requestedChannelIdMatch =
      cleanChannelIds.includes(songChannelId);

    /*
     * Explicit channel/channel-id match is always valid.
     */
    if (
      requestedChannelMatch ||
      requestedChannelIdMatch
    ) {
      return true;
    }

    /*
     * Artist match is valid ONLY when the song also belongs
     * to one of that artist's catalogue channels.
     */
    if (requestedArtistMatch) {
      return artistCatalogueMatch;
    }

    /*
     * Non-artist channel discovery.
     */
    if (channelEntries.length > 0) {
      return cleanChannels.includes(songChannel);
    }

    return false;
  })
  .map(song => {
    const songArtist = String(song?.artist || "")
      .trim()
      .toLowerCase();

    const songChannel = String(song?.channelTitle || "")
      .trim()
      .toLowerCase();

    const artistEntries =
      catalogueByArtist.get(songArtist) || [];

    const channelEntries =
      catalogueByChannel.get(songChannel) || [];

    const matchingEntries = [
      ...artistEntries,
      ...channelEntries
    ];

    const catalogue =
      matchingEntries.find(item => {
        const itemLanguage =
          String(item?.language || "")
            .trim()
            .toLowerCase();

        return (
          !cleanLanguages.length ||
          !itemLanguage ||
          cleanLanguages.includes(itemLanguage)
        );
      }) ||
      matchingEntries[0] ||
      null;

    return {
      videoId: song.videoId || null,
      title: song.title || "",
      artist:
        song.artist ||
        catalogue?.artist ||
        song.channelTitle ||
        "",
      channelTitle:
        song.channelTitle ||
        catalogue?.channel ||
        "",
      channelId:
        song.channelId ||
        catalogue?.channelId ||
        null,
      description: "",
      publishedAt: song.updated_at || null,
      duration: song.duration || null,
      language:
        song.language ||
        catalogue?.language ||
        null,
      thumbnails: {
        default: song.thumbnailUrl || null,
        medium: song.thumbnailUrl || null,
        high: song.thumbnailUrl || null
      }
    };
  });

};
const getLatestCatalogueSongsForArtists = async ({
  artists = [],
  channels = [],
  channelIds = [],
  excludeVideoIds = [],
  limit = 12
} = {}) => {
  const cleanArtists = [...new Set(
    (artists || [])
      .map(v => String(v || "").trim().toLowerCase())
      .filter(Boolean)
  )];

  const cleanChannels = [...new Set(
    (channels || [])
      .map(v => String(v || "").trim().toLowerCase())
      .filter(Boolean)
  )];

  const cleanChannelIds = [...new Set(
    (channelIds || [])
      .map(v => String(v || "").trim())
      .filter(Boolean)
  )];

  const cleanExcludeIds = [...new Set(
    (excludeVideoIds || [])
      .map(v => String(v || "").trim())
      .filter(Boolean)
  )];

  const conditions = [];
  const params = [];

  if (cleanArtists.length) {
    conditions.push(
      `LOWER(TRIM(COALESCE(s.artist, ''))) IN (${cleanArtists
        .map(() => "?")
        .join(",")})`
    );

    params.push(...cleanArtists);
  }

  if (cleanChannels.length) {
    conditions.push(
      `LOWER(TRIM(COALESCE(s.channel_title, ''))) IN (${cleanChannels
        .map(() => "?")
        .join(",")})`
    );

    params.push(...cleanChannels);
  }

  if (cleanChannelIds.length) {
    conditions.push(
      `s.channel_id IN (${cleanChannelIds
        .map(() => "?")
        .join(",")})`
    );

    params.push(...cleanChannelIds);
  }

  if (!conditions.length) {
    return [];
  }

  if (cleanExcludeIds.length) {
    conditions.push(
      `s.youtube_video_id NOT IN (${cleanExcludeIds
        .map(() => "?")
        .join(",")})`
    );

    params.push(...cleanExcludeIds);
  }

  const safeLimit = Math.min(
    Math.max(Number(limit) || 12, 1),
    100
  );

  return await db.allAsync(
    `
    SELECT
      s.youtube_video_id AS videoId,
      s.title,
      s.artist,
      s.channel_title AS channelTitle,
      s.channel_id AS channelId,
      s.thumbnail_url AS thumbnailUrl,
      s.duration,
      s.language,
      s.updated_at
    FROM music_songs s
    WHERE ${conditions.join(" AND ")}
    ORDER BY s.updated_at DESC
    LIMIT ?
    `,
    [
      ...params,
      safeLimit
    ]
  );
};
const discoverMusic = async ({
    languages = [],
    artists = [],
    channels = [],
    channelIds = [],
    favoriteVideoIds = [],
    limit = 12
}) => {
    
const catalogueArtists = await db.allAsync(
  `
  SELECT
    artist,
    channel,
    channel_id AS channelId,
    language
  FROM music_artists
  WHERE status = 'active'
  `
);

const artistMap = new Map();

for (const item of catalogueArtists || []) {
  const artist = String(item?.artist || "").trim();

  if (!artist) continue;

  artistMap.set(artist.toLowerCase(), {
    artist,
    channel: item?.channel || null,
    channelId: item?.channelId || null,
    language: item?.language || null
  });
}
for (const artist of artists || []) {
  const key = String(artist || "").trim().toLowerCase();

  const catalogueArtist = artistMap.get(key);

  if (!catalogueArtist) continue;

  if (catalogueArtist.channel) {
    channels.push(catalogueArtist.channel);
  }

  if (catalogueArtist.channelId) {
    channelIds.push(catalogueArtist.channelId);
  }
}
const uniqueArtists = [...new Set(
  (artists || []).map(v => String(v || "").trim()).filter(Boolean)
)];

const uniqueChannels = [...new Set(
  (channels || []).map(v => String(v || "").trim()).filter(Boolean)
)];

const uniqueChannelIds = [...new Set(
  (channelIds || []).map(v => String(v || "").trim()).filter(Boolean)
)]; 
const catalogueSongs = await getCatalogueSongsForArtists({
  artists: uniqueArtists,
  channels: uniqueChannels,
  channelIds: uniqueChannelIds,
  languages: [],
  limit: 100
});
const artistCatalogueRows = await db.allAsync(
    `
    SELECT
        artist,
        channel,
        channel_id AS channelId,
        language
    FROM music_artists
    WHERE status = 'active'
    `
);

const catalogueByArtist = new Map();

for (const item of artistCatalogueRows || []) {
    const artistName =
        String(item?.artist || "")
            .trim()
            .toLowerCase();

    if (!artistName) {
        continue;
    }

    if (!catalogueByArtist.has(artistName)) {
        catalogueByArtist.set(artistName, []);
    }

    catalogueByArtist
        .get(artistName)
        .push(item);
}


    const cleanLanguages =
        Array.isArray(languages)
            ? languages
                .map(language =>
                    String(language || "")
                        .trim()
                        .toLowerCase()
                )
                .filter(Boolean)
            : [];

    const cleanArtists =
        Array.isArray(artists)
            ? artists
                .map(artist =>
                    String(artist || "").trim()
                )
                .filter(Boolean)
            : [];

    const cleanChannels =
        Array.isArray(channels)
            ? channels
                .map(channel =>
                    String(channel || "").trim()
                )
                .filter(Boolean)
            : [];
    const cleanChannelIds =
    Array.isArray(channelIds)
        ? channelIds
            .map(id =>
                String(id || "").trim()
            )
            .filter(Boolean)
        : [];        

    const cleanFavoriteVideoIds =
        Array.isArray(favoriteVideoIds)
            ? favoriteVideoIds
                .map(videoId =>
                    String(videoId || "").trim()
                )
                .filter(Boolean)
            : [];
           

    const safeLimit =
        Math.min(
            Math.max(
                Number(limit) || 12,
                1
            ),
            60
        );
        

    /*
     * =========================================================
     * NO FAVORITES
     * =========================================================
     *
     * Agar koi favorite artist/channel nahi hai,
     * to existing fallback behaviour use hoga.
     */

    if (
        !cleanArtists.length &&
        !cleanChannels.length &&
        !cleanFavoriteVideoIds.length
    ) {

        let songs;

        if (cleanLanguages.length) {

            const placeholders =
                cleanLanguages
                    .map(() => "?")
                    .join(", ");

            songs =
                await db.allAsync(
                    `
                    SELECT
                        youtube_video_id AS videoId,
                        title,
                        artist,
                        channel_title AS channelTitle,
                        channel_id AS channelId,
                        thumbnail_url AS thumbnailUrl,
                        duration,
                        language,
                        updated_at
                    FROM music_songs
                    WHERE LOWER(language) IN (${placeholders})
                    ORDER BY updated_at DESC
                    LIMIT ?
                    `,
                    [
                        ...cleanLanguages,
                        safeLimit
                    ]
                );

        } else {

            songs =
                await db.allAsync(
                    `
                    SELECT
                        youtube_video_id AS videoId,
                        title,
                        artist,
                        channel_title AS channelTitle,
                        channel_id AS channelId,
                        thumbnail_url AS thumbnailUrl,
                        duration,
                        language,
                        updated_at
                    FROM music_songs
                    ORDER BY RANDOM()
                    LIMIT ?
                    `,
                    [
                        safeLimit
                    ]
                );
        }

        return songs || [];
    }


const latestCatalogueRows =
  await getLatestCatalogueSongsForArtists({
    artists: uniqueArtists,
    channels: uniqueChannels,
    channelIds: uniqueChannelIds,
    excludeVideoIds: [
      ...cleanFavoriteVideoIds,
      ...catalogueSongs.map(song =>
        String(song?.videoId || "").trim()
      )
    ],
    limit: 100
  });

    const relatedResults = new Map();
const latestResults = new Map();
const relatedLimit = 30;
const latestLimit = 30;
for (const song of catalogueSongs) {
  const videoId = String(song?.videoId || "").trim();

  if (!videoId) continue;

  if (cleanFavoriteVideoIds.includes(videoId)) {
    continue;
  }

  const songArtist = String(song?.artist || "")
    .trim()
    .toLowerCase();

  const songChannel = String(song?.channelTitle || "")
    .trim()
    .toLowerCase();

  const songChannelId = String(song?.channelId || "")
    .trim();

  const artistRequested =
    cleanArtists.includes(songArtist);

  const channelRequested =
    cleanChannels.includes(songChannel);

  const channelIdRequested =
    cleanChannelIds.includes(songChannelId);

  /*
   * Artist is NOT enough by itself.
   * If artist is requested, channel/channelId must also
   * belong to the requested catalogue relationship.
   */
  if (artistRequested) {
    const artistCatalogue =
      (catalogueByArtist?.get(songArtist) || []);

    const validArtistChannel =
      artistCatalogue.some(item => {
        const catalogueChannel =
          String(item?.channel || "")
            .trim()
            .toLowerCase();

        const catalogueChannelId =
          String(item?.channelId || "")
            .trim();

        return (
          (
            catalogueChannel &&
            catalogueChannel === songChannel
          ) ||
          (
            catalogueChannelId &&
            catalogueChannelId === songChannelId
          )
        );
      });

    if (!validArtistChannel) {
      continue;
    }
  } else if (
    !channelRequested &&
    !channelIdRequested
  ) {
    continue;
  }

  relatedResults.set(videoId, song);

  if (relatedResults.size >= relatedLimit) {
    break;
  }
}
for (const song of catalogueSongs) {
  const videoId = String(song?.videoId || "").trim();

  if (!videoId) continue;

  if (cleanFavoriteVideoIds.includes(videoId)) {
    continue;
  }

  if (relatedResults.has(videoId)) {
    continue;
  }

  latestResults.set(videoId, song);

  if (latestResults.size >= latestLimit) {
    break;
  }
}
for (const song of latestCatalogueRows) {
  const videoId = String(song?.videoId || "").trim();

  if (!videoId) continue;

  if (cleanFavoriteVideoIds.includes(videoId)) {
    continue;
  }

  if (relatedResults.has(videoId)) {
    continue;
  }

  latestResults.set(videoId, {
    videoId: song.videoId || null,
    title: song.title || "",
    artist: song.artist || "",
    channelTitle: song.channelTitle || "",
    channelId: song.channelId || null,
    description: "",
    publishedAt: song.updated_at || null,
    duration: song.duration || null,
    language: song.language || null,
    thumbnails: {
      default: song.thumbnailUrl || null,
      medium: song.thumbnailUrl || null,
      high: song.thumbnailUrl || null
    }
  });

  if (latestResults.size >= latestLimit) {
    break;
  }
}

    /*
     * =========================================================
     * DATABASE MATCHES
     * =========================================================
     */

    const conditions = [];
const values = [];

if (cleanArtists.length) {

    const artistConditions =
        cleanArtists.map(() =>
            `LOWER(TRIM(artist)) = ?`
        );

    conditions.push(
        `(${artistConditions.join(" OR ")})`
    );

    values.push(
        ...cleanArtists.map(artist =>
            artist.toLowerCase().trim()
        )
    );
}

if (cleanChannels.length) {

    const channelConditions =
        cleanChannels.map(() =>
            `LOWER(TRIM(channel_title)) = ?`
        );

    conditions.push(
        `(${channelConditions.join(" OR ")})`
    );

    values.push(
        ...cleanChannels.map(channel =>
            channel.toLowerCase().trim()
        )
    );
}
if (cleanChannelIds.length) {
    const channelIdConditions =
        cleanChannelIds.map(() =>
            `TRIM(channel_id) = ?`
        );

    conditions.push(
        `(${channelIdConditions.join(" OR ")})`
    );

    values.push(
        ...cleanChannelIds
    );
}

if (conditions.length && !(cleanArtists.length && cleanChannelIds.length)) {

    let sql = `
        SELECT
            youtube_video_id AS videoId,
            title,
            artist,
            channel_title AS channelTitle,
            channel_id AS channelId,
            thumbnail_url AS thumbnailUrl,
            duration,
            language,
            updated_at
        FROM music_songs
        WHERE (${conditions.join(" OR ")})
    `;

    if (cleanLanguages.length) {

        const languagePlaceholders =
            cleanLanguages
                .map(() => "?")
                .join(", ");

        sql += `
            AND LOWER(TRIM(language))
            IN (${languagePlaceholders})
        `;

        values.push(
            ...cleanLanguages
        );
    }

    sql += `
        ORDER BY updated_at DESC
        LIMIT ?
    `;

    values.push(safeLimit);

    const dbSongs =
        await db.allAsync(
            sql,
            values
        );

    for (const song of dbSongs || []) {

        const videoId =
            String(song?.videoId || "").trim();

        if (!videoId) {
            continue;
        }

        if (
            cleanFavoriteVideoIds.includes(videoId)
        ) {
            continue;
        }

        /*
         * Final strict DB validation
         */

        const songArtist =
            String(song?.artist || "")
                .trim()
                .toLowerCase();

        const songChannel =
            String(song?.channelTitle || "")
                .trim()
                .toLowerCase();

        const artistMatch =
            cleanArtists.some(
                artist =>
                    songArtist ===
                    artist.toLowerCase().trim()
            );
        
        const songChannelId =
    String(song?.channelId || "").trim();

const channelMatch =
    cleanChannels.some(
        channel =>
            songChannel ===
            channel.toLowerCase().trim()
    );

const channelIdMatch =
    cleanChannelIds.length > 0 &&
    cleanChannelIds.includes(songChannelId);

if (artistMatch) {
    const artistCatalogue =
        catalogueByArtist.get(songArtist) || [];

    const validArtistChannel =
        artistCatalogue.some(item => {
            const catalogueChannel =
                String(item?.channel || "")
                    .trim()
                    .toLowerCase();

            const catalogueChannelId =
                String(item?.channelId || "")
                    .trim();

            return (
                (
                    catalogueChannel &&
                    catalogueChannel === songChannel
                ) ||
                (
                    catalogueChannelId &&
                    catalogueChannelId === songChannelId
                )
            );
        });

    if (!validArtistChannel) {
        continue;
    }
} else if (
    !channelMatch &&
    !channelIdMatch
) {
    continue;
}

relatedResults.set(
    videoId,
    song
);

if (
    relatedResults.size >= relatedLimit
) {
    break;
}
    }
}


    /*
     * =========================================================
     * YOUTUBE DISCOVERY
     * =========================================================
     */

    const relatedQueries = [];
    const latestQueries = [];


    /*
     * Artist queries
     */

    for (const artist of cleanArtists) {

        relatedQueries.push(
            `"${artist}" songs`
        );

        relatedQueries.push(
            `"${artist}" official`
        );

        latestQueries.push(
            `"${artist}"`
        );
    }


    /*
     * Channel queries
     */

    for (const channel of cleanChannels) {

        relatedQueries.push(
            `"${channel}" music`
        );

        latestQueries.push(
            `"${channel}"`
        );
    }


    const uniqueRelatedQueries = [
        ...new Set(
            relatedQueries
                .map(query =>
                    String(query || "").trim()
                )
                .filter(Boolean)
        )
    ];

    const uniqueLatestQueries = [
        ...new Set(
            latestQueries
                .map(query =>
                    String(query || "").trim()
                )
                .filter(Boolean)
        )
    ];


    /*
     * =========================================================
     * RUN YOUTUBE SEARCH
     * =========================================================
     */

   const runDiscoverySearch = async (
    query,
    order,
    targetResults,
    targetLimit
) => {

        try {

            const searchResult =
                await searchMusic({
                    query,
                    maxResults: 15,
                    order
                });

            const items =
                Array.isArray(searchResult?.items)
                    ? searchResult.items
                    : [];

            for (const song of items) {

                const videoId =
                    String(
                        song?.videoId || ""
                    ).trim();

                if (!videoId) {
                    continue;
                }


                /*
                 * Favorite video dobara nahi dikhana
                 */

                if (
                    cleanFavoriteVideoIds.includes(
                        videoId
                    )
                ) {
                    continue;
                }


                /*
                 * Language filter
                 */

                if (cleanLanguages.length) {
    const songLanguage =
        String(song?.language || "")
            .trim()
            .toLowerCase();

    if (
        !songLanguage ||
        !cleanLanguages.includes(songLanguage)
    ) {
        continue;
    }
}


                const songChannel =
                    String(
                        song?.channelTitle || ""
                    )
                        .trim()
                        .toLowerCase();


                const songTitle =
                    String(
                        song?.title || ""
                    )
                        .trim()
                        .toLowerCase();


                /*
                 * =================================================
                 * CHANNEL MATCH
                 * =================================================
                 *
                 * Channel ko strict rakhenge.
                 *
                 * Example:
                 * Favorite = T-Series
                 *
                 * T-Series -> ACCEPT
                 * T-Series Bollywood -> ACCEPT only if exact
                 * Random fan channel -> REJECT
                 */

                const channelMatch =
                    cleanChannels.some(channel => {

                        const favoriteChannel =
                            channel
                                .toLowerCase()
                                .trim();

                        return (
                            songChannel ===
                            favoriteChannel
                        );
                    });
                const channelIdMatch =
    cleanChannelIds.length > 0 &&
    cleanChannelIds.includes(
        String(song?.channelId || "").trim()
    );    


                /*
                 * =================================================
                 * ARTIST MATCH
                 * =================================================
                 *
                 * Artist ko title/channel se identify karna
                 * sirf supporting signal hai.
                 *
                 * Exact artist name ko hi match karenge.
                 */

                const artistMatch =
                    cleanArtists.some(artist => {

                        const favoriteArtist =
                            artist
                                .toLowerCase()
                                .trim();

                        const titleWords =
                            songTitle
                                .replace(
                                    /[^a-z0-9]+/gi,
                                    " "
                                )
                                .split(/\s+/)
                                .filter(Boolean);

                        const channelWords =
                            songChannel
                                .replace(
                                    /[^a-z0-9]+/gi,
                                    " "
                                )
                                .split(/\s+/)
                                .filter(Boolean);

                        const artistWords =
                            favoriteArtist
                                .replace(
                                    /[^a-z0-9]+/gi,
                                    " "
                                )
                                .split(/\s+/)
                                .filter(Boolean);

                        const titleContainsArtist =
                            artistWords.length > 0 &&
                            artistWords.every(word =>
                                titleWords.includes(word)
                            );

                        const channelContainsArtist =
                            artistWords.length > 0 &&
                            artistWords.every(word =>
                                channelWords.includes(word)
                            );

                        return (
                            titleContainsArtist ||
                            channelContainsArtist
                        );
                    });
                    if (artistMatch) {
    console.log("DISCOVER ARTIST DEBUG:", {
        songTitle,
        songArtist: song?.artist,
        songChannel,
        songChannelId: String(song?.channelId || "").trim(),
        cleanArtists,
        cleanChannels,
        cleanChannelIds,
        catalogueEntries:
            catalogueByArtist.get(
                String(song?.artist || "").trim().toLowerCase()
            ) || []
    });
}


                /*
                 * =================================================
                 * FINAL RELEVANCE CHECK
                 * =================================================
                 *
                 * Favorite artist OR exact favorite channel
                 * hona compulsory hai.
                 */

            if (cleanArtists.length) {
    if (!artistMatch) {
        continue;
    }

    /*
     * Artist name alone is NOT sufficient.
     * The song must also belong to the artist's
     * catalogue channel/channel ID.
     */
    const artistCatalogue =
        catalogueByArtist.get(songArtist) || [];

    const validArtistChannel =
        artistCatalogue.some(item => {
            const catalogueChannel =
                String(item?.channel || "")
                    .trim()
                    .toLowerCase();

            const catalogueChannelId =
                String(item?.channelId || "")
                    .trim();

            return (
                (
                    catalogueChannel &&
                    catalogueChannel === songChannel
                ) ||
                (
                    catalogueChannelId &&
                    catalogueChannelId === songChannelId
                )
            );
        });
        if (artistMatch) {
    console.log("ARTIST DEBUG:", {
        songArtist,
        songChannel,
        songChannelId,
        artistEntries: catalogueByArtist.get(songArtist)
    });
}

    if (!validArtistChannel) {
        continue;
    }
}

if (cleanChannelIds.length) {
    if (!channelIdMatch) {
        continue;
    }
}

if (cleanChannels.length) {
    if (!channelMatch) {
        continue;
    }
}
                if (artistMatch && !channelMatch) {

    const blockedChannelPatterns = [
        "lyrics",
        "lyrical",
        "jukebox",
        "compilation",
        "collection",
        "mix",
        "lofi",
        "slowed",
        "reverb",
        "status",
        "fan",
        "fans",
        "tribute",
        "cover",
        "covers",
        "remix",
        "remixes",
        "8d",
        "sad songs",
        "hit songs",
        "top songs",
        "music world",
        "music channel"
    ];

    const lowerChannel =
        songChannel.toLowerCase();

    const looksLikeFanOrCompilation =
        blockedChannelPatterns.some(pattern =>
            lowerChannel.includes(pattern)
        );

    if (looksLikeFanOrCompilation) {
        continue;
    }
}


                /*
                 * Duplicate remove
                 */

                if (!targetResults.has(videoId)) {
    targetResults.set(
        videoId,
        {
            ...song
        }
    );

    try {
        await saveSongsToMusicDB([song]);
    } catch (saveError) {
        console.warn(
            "Music discovery DB save failed:",
            saveError.message
        );
    }
}

if (
    targetResults.size >= targetLimit
) {
    break;
}
            }

        } catch (error) {

            console.warn(
                "Music discover search failed:",
                query,
                error.message
            );
        }
    };


      /*
     * =========================================================
     * YOUTUBE DISCOVERY
     * =========================================================
     *
     * DB catalogue ko priority.
     * Sirf tab YouTube search chalegi jab DB se
     * sufficient results nahi mile.
     */

    const hasCatalogueResults =
        relatedResults.size > 0 ||
        latestResults.size > 0;

    if (!hasCatalogueResults) {

        /*
         * =====================================================
         * RELATED MUSIC
         * =====================================================
         */

        for (
            const query of
            uniqueRelatedQueries.slice(0, 6)
        ) {
            await runDiscoverySearch(
                query,
                "relevance",
                relatedResults,
                relatedLimit
            );

            if (
                relatedResults.size >= relatedLimit
            ) {
                break;
            }
        }


        /*
         * =====================================================
         * LATEST MUSIC
         * =====================================================
         */

        for (
            const query of
            uniqueLatestQueries.slice(0, 6)
        ) {
            await runDiscoverySearch(
                query,
                "date",
                latestResults,
                latestLimit
            );

            if (
                latestResults.size >= latestLimit
            ) {
                break;
            }
        }
    }

    /*
     * =========================================================
     * FINAL RESULT
     * =========================================================
     */

   const related = [
    ...relatedResults.values()
].slice(
    0,
    relatedLimit
);

const latest = [
    ...latestResults.values()
]
    .filter(song =>
        !relatedResults.has(
            String(song?.videoId || "").trim()
        )
    )
    .slice(
        0,
        latestLimit
    );

return {
    related,
    latest,
    songs: [
        ...related,
        ...latest
    ]
};

};
module.exports = {
    searchMusic,
    getVideoDuration,
    discoverMusic,
    discoverAndSaveArtist,
    addYouTubeVideo
};