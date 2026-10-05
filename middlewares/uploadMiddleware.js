const multer =
    require("multer");

const path =
    require("path");

const fs =
    require("fs");

const uploadsPath =
    process.env.RENDER
        ? "/var/data/uploads"
        : path.join(
            __dirname,
            "../uploads"
        );

fs.mkdirSync(
    uploadsPath,
    {
        recursive: true
    }
);

const storage =
    multer.diskStorage({

        destination:
            (
                req,
                file,
                cb
            ) => {

                cb(
                    null,
                    uploadsPath
                );

            },

        filename:
            (
                req,
                file,
                cb
            ) => {

                const extension =
                    path.extname(
                        file.originalname
                    ).toLowerCase();

                cb(
                    null,
                    `school-logo-${req.user.schoolId}-${Date.now()}${extension}`
                );

            }

    });

const fileFilter =
    (
        req,
        file,
        cb
    ) => {

        const allowedTypes = [
            "image/jpeg",
            "image/png",
            "image/webp"
        ];

        if (
            allowedTypes.includes(
                file.mimetype
            )
        ) {

            cb(
                null,
                true
            );

        }
        else {

            cb(
                new Error(
                    "Only JPG, PNG and WebP images are allowed"
                )
            );

        }

    };

const uploadSchoolLogo =
    multer({

        storage,

        fileFilter,

        limits: {
            fileSize:
                2 * 1024 * 1024
        }

    }).single(
        "logo"
    );

module.exports =
    uploadSchoolLogo;