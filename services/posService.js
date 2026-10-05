const fs =
    require("fs").promises;

const path =
    require("path");

exports.getLatest =
    async () => {

        const filePath =
            path.join(
                __dirname,
                "..",
                "storage",
                "pos",
                "latest.json"
            );

        const content =
            await fs.readFile(
                filePath,
                "utf8"
            );

        return JSON.parse(
            content
        );

    };
exports.getDownloadPath =
    () => {

        return path.join(
            __dirname,
            "..",
            "storage",
            "pos",
            "AlignPOS.zip"
        );

    };    
exports.getLatestFromGitHub =
    async () => {

        const response =
            await fetch(
                "https://api.github.com/repos/hansraj251/Align-POS/releases/latest",
                {
                    headers: {
                        "Accept":
                            "application/vnd.github+json"
                    }
                }
            );

       if (!response.ok) {

    const body =
        await response.text();

    throw new Error(
        `GitHub API ${response.status}: ${body}`
    );

}

        const release =
            await response.json();

        const asset =
            release.assets.find(
                item =>
                    item.name.endsWith(".zip")
            );

        return {

            success: true,

            version:
                release.tag_name.replace(
                    /^v/,
                    ""
                ),

            mandatory: false,

            releaseDate:
                release.published_at,

            notes:
                release.body
                    ? release.body
                          .split("\n")
                          .filter(Boolean)
                    : [],

            downloadUrl:
                asset
                    ? asset.browser_download_url
                    : null

        };

    }; 