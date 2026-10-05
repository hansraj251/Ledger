const db =
    require("../db");

async function runMigrations() {

    console.log(
        "🔄 Running database migrations..."
    );

    /*
     * Migration 1:
     * subscription_orders.school_id
     */

    const subscriptionColumns =
        await db.allAsync(
            `
            PRAGMA table_info(
                subscription_orders
            )
            `
        );

    const hasSchoolId =
        subscriptionColumns.some(
            column =>
                column.name ===
                "school_id"
        );

    if (
        !hasSchoolId
    ) {

        console.log(
            "⚠️ Migrating subscription_orders..."
        );

        await db.runAsync(
            `
            PRAGMA foreign_keys = OFF
            `
        );

        await db.runAsync(
            `
            BEGIN TRANSACTION
            `
        );

        try {

            await db.runAsync(
                `
                CREATE TABLE subscription_orders_new (

                    id INTEGER PRIMARY KEY AUTOINCREMENT,

                    restaurant_id INTEGER,

                    school_id INTEGER,

                    plan_id INTEGER NOT NULL,

                    plan_pricing_id INTEGER NOT NULL,

                    razorpay_order_id TEXT NOT NULL UNIQUE,

                    razorpay_payment_id TEXT,

                    amount REAL NOT NULL,

                    currency TEXT NOT NULL,

                    duration_days INTEGER NOT NULL,

                    payment_method TEXT,

                    status TEXT DEFAULT 'pending',

                    paid_at DATETIME,

                    created_at DATETIME
                        DEFAULT CURRENT_TIMESTAMP,

                    FOREIGN KEY (restaurant_id)
                        REFERENCES restaurants(id),

                    FOREIGN KEY (school_id)
                        REFERENCES schools(id),

                    FOREIGN KEY (plan_id)
                        REFERENCES plans(id),

                    FOREIGN KEY (plan_pricing_id)
                        REFERENCES plan_pricing(id)

                )
                `
            );

            await db.runAsync(
                `
                INSERT INTO
                    subscription_orders_new
                SELECT
                    id,
                    restaurant_id,
                    NULL,
                    plan_id,
                    plan_pricing_id,
                    razorpay_order_id,
                    razorpay_payment_id,
                    amount,
                    currency,
                    duration_days,
                    payment_method,
                    status,
                    paid_at,
                    created_at
                FROM
                    subscription_orders
                `
            );

            await db.runAsync(
                `
                DROP TABLE
                    subscription_orders
                `
            );

            await db.runAsync(
                `
                ALTER TABLE
                    subscription_orders_new
                RENAME TO
                    subscription_orders
                `
            );

            await db.runAsync(
                `
                COMMIT
                `
            );

            console.log(
                "✅ subscription_orders migrated successfully."
            );

        }
        catch (err) {

            await db.runAsync(
                `
                ROLLBACK
                `
            );

            console.error(
                "❌ subscription_orders migration failed:",
                err.message
            );

            throw err;

        }
        finally {

            await db.runAsync(
                `
                PRAGMA foreign_keys = ON
                `
            );

        }

    }
    else {

        console.log(
            "✅ subscription_orders schema is up to date."
        );

    }


        /*
     * Migration 2:
     * users.username and users.school_id
     */

    const userColumns =
        await db.allAsync(
            `
            PRAGMA table_info(
                users
            )
            `
        );

    const hasUsername =
        userColumns.some(
            column =>
                column.name ===
                "username"
        );

    const hasUserSchoolId =
        userColumns.some(
            column =>
                column.name ===
                "school_id"
        );

    const userForeignKeys =
        await db.allAsync(
            `
            PRAGMA foreign_key_list(
                users
            )
            `
        );

    const hasSchoolForeignKey =
        userForeignKeys.some(
            foreignKey =>
                foreignKey.table ===
                "schools" &&
                foreignKey.from ===
                "school_id"
        );

    if (
        hasUsername &&
        hasUserSchoolId &&
        hasSchoolForeignKey
    ) {

        await db.runAsync(
            `
            CREATE UNIQUE INDEX
                IF NOT EXISTS
                idx_users_username
            ON users (
                username
            )
            `
        );

        console.log(
            "✅ users schema is up to date."
        );

    }
    else {

        console.log(
            "⚠️ Migrating users schema..."
        );

        await db.runAsync(
            `
            PRAGMA foreign_keys = OFF
            `
        );

        await db.runAsync(
            `
            BEGIN TRANSACTION
            `
        );

        try {

            const usernameExpression =
                hasUsername
                    ? "username"
                    : "NULL";

            const schoolIdExpression =
                hasUserSchoolId
                    ? "school_id"
                    : "NULL";

            await db.runAsync(
                `
                CREATE TABLE users_new (

                    id INTEGER PRIMARY KEY AUTOINCREMENT,

                    restaurant_id INTEGER,

                    school_id INTEGER,

                    name TEXT NOT NULL,

                    email TEXT UNIQUE,

                    mobile TEXT UNIQUE,

                    password TEXT NOT NULL,

                    role TEXT NOT NULL
                        DEFAULT 'owner',

                    status TEXT NOT NULL
                        DEFAULT 'active',

                    last_login DATETIME,

                    created_at DATETIME
                        DEFAULT CURRENT_TIMESTAMP,

                    updated_at DATETIME
                        DEFAULT CURRENT_TIMESTAMP,

                    username TEXT,

                    FOREIGN KEY (restaurant_id)
                        REFERENCES restaurants(id),

                    FOREIGN KEY (school_id)
                        REFERENCES schools(id)

                )
                `
            );

            await db.runAsync(
                `
                INSERT INTO
                    users_new (
                        id,
                        restaurant_id,
                        school_id,
                        name,
                        email,
                        mobile,
                        password,
                        role,
                        status,
                        last_login,
                        created_at,
                        updated_at,
                        username
                    )
                SELECT
                    id,
                    restaurant_id,
                    ${schoolIdExpression},
                    name,
                    email,
                    mobile,
                    password,
                    role,
                    status,
                    last_login,
                    created_at,
                    updated_at,
                    ${usernameExpression}
                FROM
                    users
                `
            );

            await db.runAsync(
                `
                DROP TABLE
                    users
                `
            );

            await db.runAsync(
                `
                ALTER TABLE
                    users_new
                RENAME TO
                    users
                `
            );

            await db.runAsync(
                `
                CREATE UNIQUE INDEX
                    IF NOT EXISTS
                    idx_users_username
                ON users (
                    username
                )
                `
            );

            await db.runAsync(
                `
                COMMIT
                `
            );

            console.log(
                "✅ users schema migrated successfully."
            );

        }
        catch (err) {

            await db.runAsync(
                `
                ROLLBACK
                `
            );

            console.error(
                "❌ users migration failed:",
                err.message
            );

            throw err;

        }
        finally {

            await db.runAsync(
                `
                PRAGMA foreign_keys = ON
                `
            );

        }

       }

    /*
     * Migration 3:
     * email_otps.business_type
     */

    const emailOtpColumns =
        await db.allAsync(
            `
            PRAGMA table_info(
                email_otps
            )
            `
        );

    const hasBusinessType =
        emailOtpColumns.some(
            column =>
                column.name ===
                "business_type"
        );

    if (
        hasBusinessType
    ) {

        console.log(
            "✅ email_otps schema is up to date."
        );

    }
    else {

        console.log(
            "⚠️ Adding email_otps.business_type..."
        );

        await db.runAsync(
            `
            ALTER TABLE
                email_otps
            ADD COLUMN
                business_type TEXT
            `
        );

        console.log(
            "✅ email_otps.business_type added successfully."
        );

    }
        /*
     * Migration 4:
     * plans.plan_type
     */

    const planColumns =
        await db.allAsync(
            `
            PRAGMA table_info(
                plans
            )
            `
        );

    const hasPlanType =
        planColumns.some(
            column =>
                column.name ===
                "plan_type"
        );

    if (
        hasPlanType
    ) {

        console.log(
            "✅ plans schema is up to date."
        );

    }
    else {

        console.log(
            "⚠️ Adding plans.plan_type..."
        );

        await db.runAsync(
            `
            ALTER TABLE
                plans
            ADD COLUMN
                plan_type TEXT NOT NULL
                DEFAULT 'food'
            `
        );

        console.log(
            "✅ plans.plan_type added successfully."
        );

    }
        /*
     * Migration 5:
     * salary_payments
     */

    await db.runAsync(
        `
        CREATE TABLE IF NOT EXISTS salary_payments (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            school_id INTEGER NOT NULL,

            teacher_id INTEGER NOT NULL,

            salary_month TEXT NOT NULL,

            salary_amount REAL NOT NULL,

            amount_paid REAL NOT NULL,

            payment_date DATE NOT NULL,

            payment_mode TEXT,

            reference_no TEXT,

            remarks TEXT,

            created_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            updated_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            FOREIGN KEY (
                school_id
            )
                REFERENCES schools(id),

            FOREIGN KEY (
                teacher_id
            )
                REFERENCES teachers(id)

        )
        `
    );

    console.log(
        "✅ salary_payments schema is up to date."
    );
        /*
     * Migration 6:
     * fee_payments.fee_head
     */

    const feePaymentColumns =

        await db.allAsync(

            `
            PRAGMA table_info(
                fee_payments
            )
            `

        );

    const hasFeeHead =

        feePaymentColumns.some(

            column =>

                column.name ===
                "fee_head"

        );

    if (
        hasFeeHead
    ) {

        console.log(
            "✅ fee_payments schema is up to date."
        );

    }
    else {

        console.log(
            "⚠️ Adding fee_payments.fee_head..."
        );

        await db.runAsync(

            `
            ALTER TABLE
                fee_payments
            ADD COLUMN
                fee_head TEXT
            `

        );

        console.log(
            "✅ fee_payments.fee_head added successfully."
        );

    }
    /*
 * Migration 7:
 * fee_payment_items
 */

await db.runAsync(

    `
    CREATE TABLE IF NOT EXISTS fee_payment_items (

        id INTEGER PRIMARY KEY AUTOINCREMENT,

        payment_id INTEGER NOT NULL,

        fee_structure_id INTEGER NOT NULL,

        fee_head TEXT NOT NULL,

        amount REAL NOT NULL DEFAULT 0,

        created_at DATETIME
            DEFAULT CURRENT_TIMESTAMP,

        FOREIGN KEY (payment_id)
            REFERENCES fee_payments(id)
            ON DELETE CASCADE,

        FOREIGN KEY (fee_structure_id)
            REFERENCES fee_structures(id)

    )
    `

);

console.log(
    "✅ fee_payment_items table ready."
);
    /*
     * Migration 8:
     * schools.receipt_footer_message
     */

    const schoolColumns =
        await db.allAsync(
            `
            PRAGMA table_info(
                schools
            )
            `
        );

    const hasReceiptFooterMessage =
        schoolColumns.some(
            column =>
                column.name ===
                "receipt_footer_message"
        );

    if (
        hasReceiptFooterMessage
    ) {

        console.log(
            "✅ schools.receipt_footer_message schema is up to date."
        );

    }
    else {

        console.log(
            "⚠️ Adding schools.receipt_footer_message..."
        );

        await db.runAsync(
            `
            ALTER TABLE
                schools
            ADD COLUMN
                receipt_footer_message TEXT
            `
        );

        console.log(
            "✅ schools.receipt_footer_message added successfully."
        );

    }


    const contactRequestColumns =
        await db.allAsync(
            `
            PRAGMA table_info(
                property_contact_requests
            )
            `
        );

    const hasContactShared =
        contactRequestColumns.some(
            column =>
                column.name ===
                "contact_shared"
        );

    if (
        !hasContactShared
    ) {

        console.log(
            "⚠️ Adding property_contact_requests.contact_shared..."
        );

        await db.runAsync(
            `
            ALTER TABLE
                property_contact_requests
            ADD COLUMN
                contact_shared INTEGER
                NOT NULL DEFAULT 0
            `
        );

        console.log(
            "✅ contact_shared added successfully."
        );

    }

    const hasContactSharedAt =
        contactRequestColumns.some(
            column =>
                column.name ===
                "contact_shared_at"
        );

    if (
        !hasContactSharedAt
    ) {

        console.log(
            "⚠️ Adding property_contact_requests.contact_shared_at..."
        );

        await db.runAsync(
            `
            ALTER TABLE
                property_contact_requests
            ADD COLUMN
                contact_shared_at DATETIME
            `
        );

        console.log(
            "✅ contact_shared_at added successfully."
        );

    }
        const contactRequestTokenColumns =
        await db.allAsync(
            `
            PRAGMA table_info(
                property_contact_requests
            )
            `
        );

    const hasBuyerAccessTokenHash =
        contactRequestTokenColumns.some(
            column =>
                column.name ===
                "buyer_access_token_hash"
        );

    if (
        !hasBuyerAccessTokenHash
    ) {

        console.log(
            "⚠️ Adding property_contact_requests.buyer_access_token_hash..."
        );

        await db.runAsync(
            `
            ALTER TABLE
                property_contact_requests
            ADD COLUMN
                buyer_access_token_hash TEXT
            `
        );

        console.log(
            "✅ buyer_access_token_hash added successfully."
        );

    }

    const hasBuyerAccessTokenExpiresAt =
        contactRequestTokenColumns.some(
            column =>
                column.name ===
                "buyer_access_token_expires_at"
        );

    if (
        !hasBuyerAccessTokenExpiresAt
    ) {

        console.log(
            "⚠️ Adding property_contact_requests.buyer_access_token_expires_at..."
        );

        await db.runAsync(
            `
            ALTER TABLE
                property_contact_requests
            ADD COLUMN
                buyer_access_token_expires_at DATETIME
            `
        );

        console.log(
            "✅ buyer_access_token_expires_at added successfully."
        );

    }
        /*
     * Migration 9:
     * Add property listing contact/rent fields
     */

    const propertyListingColumns =
        await db.allAsync(
            `
                PRAGMA table_info(
                    property_listings
                )
            `
        );

    const propertyListingColumnNames =
        new Set(
            propertyListingColumns.map(
                column => column.name
            )
        );

    if (
        !propertyListingColumnNames.has(
            "contact_preference"
        )
    ) {

        console.log(
            "⚠️ Adding property_listings.contact_preference..."
        );

        await db.runAsync(
            `
                ALTER TABLE
                    property_listings
                ADD COLUMN
                    contact_preference TEXT
                    NOT NULL
                    DEFAULT 'show'
            `
        );

        console.log(
            "✅ contact_preference added."
        );

    }

    if (
        !propertyListingColumnNames.has(
            "rent_amount"
        )
    ) {

        console.log(
            "⚠️ Adding property_listings.rent_amount..."
        );

        await db.runAsync(
            `
                ALTER TABLE
                    property_listings
                ADD COLUMN
                    rent_amount REAL
            `
        );

        console.log(
            "✅ rent_amount added."
        );

    }
        /*
     * Migration 10:
     * music_songs
     */

    await db.runAsync(
        `
        CREATE TABLE IF NOT EXISTS music_songs (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            youtube_video_id TEXT NOT NULL UNIQUE,

            title TEXT NOT NULL,

            artist TEXT,

            channel_title TEXT,

            thumbnail_url TEXT,

            duration TEXT,
            language TEXT,

            created_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            updated_at DATETIME
                DEFAULT CURRENT_TIMESTAMP

        )
        `
    );

    console.log(
        "✅ music_songs table is ready."
    );
    /*
 * Migration 10.1:
 * music_songs.channel_id
 */

const musicSongColumns = await db.allAsync(
    `
    PRAGMA table_info(music_songs)
    `
);

const hasMusicSongChannelId =
    musicSongColumns.some(
        column =>
            column.name === "channel_id"
    );

if (!hasMusicSongChannelId) {

    await db.runAsync(
        `
        ALTER TABLE music_songs
        ADD COLUMN channel_id TEXT
        `
    );

    console.log(
        "✅ music_songs.channel_id added."
    );
}
        /*
     * Migration 11:
     * music_artists
     */

    await db.runAsync(
        `
        CREATE TABLE IF NOT EXISTS music_artists (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            artist TEXT NOT NULL,

            channel TEXT,

            channel_id TEXT,

            language TEXT,

            status TEXT NOT NULL
                DEFAULT 'active',

            last_fetched_at DATETIME,

            created_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            updated_at DATETIME
                DEFAULT CURRENT_TIMESTAMP

        )
        `
    );

    await db.runAsync(
        `
        CREATE INDEX IF NOT EXISTS
            idx_music_artists_artist
        ON music_artists (
            artist
        )
        `
    );

    await db.runAsync(
        `
        CREATE INDEX IF NOT EXISTS
            idx_music_artists_channel_id
        ON music_artists (
            channel_id
        )
        `
    );

    console.log(
        "✅ music_artists table is ready."
    );
    /*
 * Migration 12:
 * Seed music_artists master catalogue
 */

const musicArtistsSeed = [
  // Hindi / Bollywood
  ["Arijit Singh", "Arijit Singh - Topic", null, "Hindi"],
  ["Shreya Ghoshal", "Shreya Ghoshal - Topic", null, "Hindi"],
  ["Sonu Nigam", "Sonu Nigam - Topic", null, "Hindi"],
  ["KK", "K.K. - Topic", null, "Hindi"],
  ["Atif Aslam", "Atif Aslam - Topic", null, "Hindi"],
  ["Jubin Nautiyal", "Jubin Nautiyal - Topic", null, "Hindi"],
  ["Armaan Malik", "Armaan Malik - Topic", null, "Hindi"],
  ["Darshan Raval", "Darshan Raval - Topic", null, "Hindi"],
  ["Vishal Mishra", "Vishal Mishra - Topic", null, "Hindi"],
  ["Amit Trivedi", "Amit Trivedi - Topic", null, "Hindi"],
  ["Mohit Chauhan", "Mohit Chauhan - Topic", null, "Hindi"],
  ["Shaan", "Shaan - Topic", null, "Hindi"],
  ["Sunidhi Chauhan", "Sunidhi Chauhan - Topic", null, "Hindi"],
  ["Neha Kakkar", "Neha Kakkar - Topic", null, "Hindi"],
  ["Palak Muchhal", "Palak Muchhal - Topic", null, "Hindi"],
  ["Tulsi Kumar", "Tulsi Kumar - Topic", null, "Hindi"],
  ["B Praak", "B Praak - Topic", null, "Hindi"],
  ["Badshah", "Badshah - Topic", null, "Hindi"],
  ["Yo Yo Honey Singh", "Yo Yo Honey Singh", null, "Hindi"],
  ["Raftaar", "Raftaar - Topic", null, "Hindi"],
  ["Divine", "DIVINE - Topic", null, "Hindi"],
  ["King", "King - Topic", null, "Hindi"],
  ["Karan Aujla", "Karan Aujla - Topic", null, "Punjabi"],
  ["Diljit Dosanjh", "Diljit Dosanjh - Topic", null, "Punjabi"],
  ["Sidhu Moose Wala", "Sidhu Moose Wala - Topic", null, "Punjabi"],
  ["AP Dhillon", "AP Dhillon - Topic", null, "Punjabi"],
  ["Shubh", "Shubh - Topic", null, "Punjabi"],
  ["Kaka", "Kaka - Topic", null, "Punjabi"],
  ["Gurnam Bhullar", "Gurnam Bhullar - Topic", null, "Punjabi"],
  ["Ammy Virk", "Ammy Virk - Topic", null, "Punjabi"],
  ["Nimrat Khaira", "Nimrat Khaira - Topic", null, "Punjabi"],

  // Major labels / catalogue channels
  ["T-Series", "T-Series", null, "Hindi"],
  ["Sony Music India", "Sony Music India", null, "Hindi"],
  ["Zee Music Company", "Zee Music Company", null, "Hindi"],
  ["Tips Official", "Tips Official", null, "Hindi"],
  ["Saregama Music", "Saregama Music", null, "Hindi"],
  ["Speed Records", "Speed Records", null, "Punjabi"],
  ["White Hill Music", "White Hill Music", null, "Punjabi"],
  ["Desi Music Factory", "Desi Music Factory", null, "Hindi"],
  ["Desi Melodies", "Desi Melodies", null, "Punjabi"],
  ["DM - Desi Melodies", "DM - Desi Melodies", null, "Punjabi"],

  // English / International
  ["Taylor Swift", "Taylor Swift", null, "English"],
  ["Ed Sheeran", "Ed Sheeran", null, "English"],
  ["The Weeknd", "The Weeknd", null, "English"],
  ["Justin Bieber", "Justin Bieber", null, "English"],
  ["Billie Eilish", "Billie Eilish", null, "English"],
  ["Ariana Grande", "Ariana Grande", null, "English"],
  ["Bruno Mars", "Bruno Mars", null, "English"],
  ["Dua Lipa", "Dua Lipa", null, "English"],
  ["Selena Gomez", "Selena Gomez", null, "English"],
  ["OneRepublic", "OneRepublic", null, "English"],
  ["Maroon 5", "Maroon 5", null, "English"],
  ["Imagine Dragons", "ImagineDragons", null, "English"],
  ["Coldplay", "Coldplay", null, "English"],
  ["Eminem", "EminemMusic", null, "English"],
  ["Drake", "Drake", null, "English"],
  ["Post Malone", "Post Malone", null, "English"],
  ["Sia", "Sia", null, "English"],
  ["Adele", "Adele", null, "English"],

  // Punjabi / regional
  ["Guru Randhawa", "Guru Randhawa", null, "Punjabi"],
  ["Jass Manak", "Jass Manak", null, "Punjabi"],
  ["Parmish Verma", "Parmish Verma", null, "Punjabi"],
  ["Mankirt Aulakh", "Mankirt Aulakh", null, "Punjabi"],
  ["Jordan Sandhu", "Jordan Sandhu", null, "Punjabi"],
  ["Prem Dhillon", "Prem Dhillon", null, "Punjabi"],
  ["Ranjit Bawa", "Ranjit Bawa", null, "Punjabi"],
  ["Gippy Grewal", "Gippy Grewal", null, "Punjabi"],
  ["Babbu Maan", "Babbu Maan", null, "Punjabi"],
  ["Amrit Maan", "Amrit Maan", null, "Punjabi"]
];

for (const [artist, channel, channelId, language] of musicArtistsSeed) {
  try {
    const existing = await db.getAsync(
      `
      SELECT id
      FROM music_artists
      WHERE LOWER(TRIM(artist)) = LOWER(TRIM(?))
      LIMIT 1
      `,
      [artist]
    );

    if (existing?.id) {
      await db.runAsync(
        `
        UPDATE music_artists
        SET
          channel = ?,
          channel_id = COALESCE(channel_id, ?),
          language = ?,
          status = 'active',
          updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
        `,
        [channel, channelId, language, existing.id]
      );
    } else {
      await db.runAsync(
        `
        INSERT INTO music_artists
          (artist, channel, channel_id, language, status)
        VALUES (?, ?, ?, ?, 'active')
        `,
        [artist, channel, channelId, language]
      );
    }
  } catch (error) {
    console.error("Failed to seed artist:", artist, error);
  }
}

console.log("✅ Music artist catalogue seeded.");

    /*
     * Migration 13:
     * Music user favorites and playlists
     */

    await db.runAsync(
        `
        CREATE TABLE IF NOT EXISTS music_favorites (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            user_id INTEGER NOT NULL,

            youtube_video_id TEXT NOT NULL,

            created_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            UNIQUE (
                user_id,
                youtube_video_id
            ),

            FOREIGN KEY (user_id)
                REFERENCES users(id)
                ON DELETE CASCADE
        )
        `
    );

    await db.runAsync(
        `
        CREATE INDEX IF NOT EXISTS
            idx_music_favorites_user
        ON music_favorites (
            user_id
        )
        `
    );

    await db.runAsync(
        `
        CREATE TABLE IF NOT EXISTS music_playlists (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            user_id INTEGER NOT NULL,

            name TEXT NOT NULL,

            created_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            updated_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            FOREIGN KEY (user_id)
                REFERENCES users(id)
                ON DELETE CASCADE
        )
        `
    );

    await db.runAsync(
        `
        CREATE INDEX IF NOT EXISTS
            idx_music_playlists_user
        ON music_playlists (
            user_id
        )
        `
    );

    await db.runAsync(
        `
        CREATE TABLE IF NOT EXISTS music_playlist_songs (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            playlist_id INTEGER NOT NULL,

            youtube_video_id TEXT NOT NULL,

            position INTEGER NOT NULL
                DEFAULT 0,

            added_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            UNIQUE (
                playlist_id,
                youtube_video_id
            ),

            FOREIGN KEY (playlist_id)
                REFERENCES music_playlists(id)
                ON DELETE CASCADE
        )
        `
    );

    await db.runAsync(
        `
        CREATE INDEX IF NOT EXISTS
            idx_music_playlist_songs_playlist
        ON music_playlist_songs (
            playlist_id
        )
        `
    );

    console.log(
        "✅ Music user favorites and playlists tables are ready."
    );
}

module.exports =
    runMigrations;