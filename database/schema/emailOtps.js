const db =
    require("../../db");

async function createEmailOtpsTable() {

    return new Promise(
        (
            resolve,
            reject
        ) => {

            db.run(
                `
                CREATE TABLE IF NOT EXISTS email_otps
(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    email TEXT NOT NULL,
    otp TEXT NOT NULL,
    purpose TEXT NOT NULL,
    business_type TEXT,
    restaurant_name TEXT,
    owner_name TEXT,
    mobile TEXT,
    password_hash TEXT,
    expires_at TEXT NOT NULL,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP
)
                `,
                err => {

                    if (err) {

                        return reject(err);

                    }

                    resolve();

                }
            );

        }
    );

}

module.exports =
    createEmailOtpsTable;