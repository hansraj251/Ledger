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

                const uniqueName =
                    `property-listing-${req.propertyUserId}-${Date.now()}-${Math.random().toString(36).slice(2, 10)}${extension}`;

                cb(
                    null,
                    uniqueName
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


const uploadPropertyImages =
    multer({

        storage,

        fileFilter,

        limits: {

            fileSize:
                5 * 1024 * 1024,

            files: 8

        }

    }).array(

        "photos",

        8

    );


module.exports =
    uploadPropertyImages;
