const db =
    require("../db");

async function createRestaurantAccount(
    signupData
) {

    return new Promise(
        (
            resolve,
            reject
        ) => {

           db.run(
               `INSERT INTO restaurants
           (
               name,
               owner_name,
               email,
               mobile,
               status,
               plan_id,
               subscription_status,
               plan_start,
               plan_end,
               trial_used
           )
           VALUES
           (
               ?, ?, ?, ?, ?,
               (
                   SELECT id
                   FROM plans
                   WHERE slug = '100' AND plan_type = 'school'
               ),
               ?,
               DATE('now'),
               DATE('now', '+30 days'),
               ?
           )`,
               [
               signupData.restaurantName,
               signupData.ownerName,
              signupData.email,
              signupData.mobile,
               "active",
               "trial",
               1
           ],
               function (err) {

                   if (err) {
                       return reject(err);
                   }

           const restaurantId = this.lastID;
           const restaurantCode =
               `ALN${String(restaurantId).padStart(6, "0")}`;

           db.run(
               `
               UPDATE restaurants
               SET restaurant_code = ?
               WHERE id = ?
               `,
               [
                   restaurantCode,
                   restaurantId
               ],
               function (err) {

                   if (err) {

                       return reject(err);

                   }

               }
           );
           db.run(
               `INSERT INTO users
               (
                   restaurant_id,
                   name,
                   email,
                   mobile,
                   password,
                   role,
                   status
               )
               VALUES (?, ?, ?, ?, ?, ?, ?)`,
               [
                   restaurantId,
                   signupData.ownerName,
                   signupData.email,
                   signupData.mobile,
                   signupData.passwordHash,
                   "owner",
                   "active"
               ],
               function (err) {

                   if (err) {
                       return reject(err);
                   }

                   const userId = this.lastID;

                  resolve({

    restaurantId,

    userId

});

               }
           );
               }
           );

        }
    );

}
async function createSchoolAccount(
    signupData
) {

    return new Promise(
        (
            resolve,
            reject
        ) => {

            db.run(
                `INSERT INTO schools
                (
                    name,
                    owner_name,
                    email,
                    mobile,
                    status,
                    plan_id,
                    subscription_status,
                    plan_start,
                    plan_end
                )
                VALUES
                (
                    ?, ?, ?, ?, ?,
                    (
                        SELECT id
                        FROM plans
                        WHERE slug = '100' AND plan_type = 'school'
                    ),
                    ?,
                    DATE('now'),
                    DATE('now', '+30 days')
                )`,
                [
                    signupData.schoolName,
                    signupData.ownerName,
                    signupData.email,
                    signupData.mobile,
                    "active",
                    "trial"
                ],
                function (err) {

                    if (err) {

                        return reject(err);

                    }

                    const schoolId =
                        this.lastID;

                    const schoolCode =
                        `SCH${String(schoolId).padStart(6, "0")}`;

                    db.run(
                        `
                        UPDATE schools
                        SET school_code = ?
                        WHERE id = ?
                        `,
                        [
                            schoolCode,
                            schoolId
                        ],
                        function (err) {

                            if (err) {

                                return reject(err);

                            }

                        }
                    );

                    db.run(
                        `INSERT INTO users
                        (
                            school_id,
                            name,
                            email,
                            mobile,
                            password,
                            role,
                            status
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?)`,
                        [
                            schoolId,
                            signupData.ownerName,
                            signupData.email,
                            signupData.mobile,
                            signupData.passwordHash,
                            "owner",
                            "active"
                        ],
                        function (err) {

                            if (err) {

                                return reject(err);

                            }

                            const userId =
                                this.lastID;

                            resolve({

                                schoolId,

                                userId

                            });

                        }
                    );

                }
            );

        }
    );

}

module.exports = {

    createRestaurantAccount,

    createSchoolAccount

};