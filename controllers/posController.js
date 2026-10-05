const posService =
    require("../services/posService");

exports.getLatest =
    async (req, res) => {

        try {

            const data =
                await posService.getLatest();

            return res.json({
                success: true,
                ...data
            });

        } catch (err) {

            console.error(err);

            return res.status(500).json({

                success: false,

                message:
                    err.message

            });

        }

    };
exports.download =
    async (req, res) => {

        try {

            const filePath =
                posService.getDownloadPath();

            return res.download(
                filePath,
                "AlignPOS.zip"
            );

        } catch (err) {

            console.error(err);

            return res.status(500).json({

                success: false,

                message: err.message

            });

        }

    };    