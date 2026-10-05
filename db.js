const sqlite3 = require("sqlite3").verbose();
const path = require("path");

const dbPath =
    process.env.RENDER
        ? "/var/data/align.db"
        : path.join(__dirname, "database", "align.db");       


const db = new sqlite3.Database(dbPath, (err) => {
    if (err) {
        console.error("❌ Database connection failed:", err.message);
    } else {
        console.log("✅ SQLite Connected");
        db.run("PRAGMA foreign_keys = ON;");
    }
});

db.runAsync = (sql, params = []) => {

    return new Promise((resolve, reject) => {

        db.run(sql, params, function (err) {

            if (err) {
                return reject(err);
            }

            resolve({
                lastID: this.lastID,
                changes: this.changes
            });

        });

    });

};

db.getAsync = (sql, params = []) => {

    return new Promise((resolve, reject) => {

        db.get(sql, params, (err, row) => {

            if (err) {
                return reject(err);
            }

            resolve(row);

        });

    });

};

db.allAsync = (sql, params = []) => {

    return new Promise((resolve, reject) => {

        db.all(sql, params, (err, rows) => {

            if (err) {
                return reject(err);
            }

            resolve(rows);

        });

    });

};
db.execAsync = (sql) => {

    return new Promise((resolve, reject) => {

        db.exec(sql, err => {

            if (err) {

                return reject(err);

            }

            resolve();

        });

    });

};
db.transaction = async (callback) => {

    try {

        await db.runAsync("BEGIN TRANSACTION");

        const result = await callback(db);

        await db.runAsync("COMMIT");

        return result;

    } catch (err) {

        try {
            await db.runAsync("ROLLBACK");
        } catch (_) {}

        throw err;

    }

};
module.exports = db;