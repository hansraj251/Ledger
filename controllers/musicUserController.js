const musicUserService =
    require("../services/musicUserService");

exports.getFavorites = async (req, res) => {
    try {
        const favorites =
            await musicUserService.getFavorites(
                req.propertyUserId
            );

        return res.json({
            success: true,
            favorites
        });
    }
    catch (error) {
        console.error(
            "Music get favorites error:",
            error
        );

        return res.status(500).json({
            success: false,
            message:
                error.message ||
                "Unable to load favorites."
        });
    }
};

exports.addFavorite = async (req, res) => {
    try {
        await musicUserService.addFavorite(
            req.propertyUserId,
            req.body?.videoId
        );

        return res.json({
            success: true
        });
    }
    catch (error) {
        console.error(
            "Music add favorite error:",
            error
        );

        return res.status(400).json({
            success: false,
            message:
                error.message ||
                "Unable to add favorite."
        });
    }
};

exports.removeFavorite = async (req, res) => {
    try {
        await musicUserService.removeFavorite(
            req.propertyUserId,
            req.body?.videoId
        );

        return res.json({
            success: true
        });
    }
    catch (error) {
        console.error(
            "Music remove favorite error:",
            error
        );

        return res.status(400).json({
            success: false,
            message:
                error.message ||
                "Unable to remove favorite."
        });
    }
};

exports.getPlaylists = async (req, res) => {
    try {
        const playlists =
            await musicUserService.getPlaylists(
                req.propertyUserId
            );

        return res.json({
            success: true,
            playlists
        });
    }
    catch (error) {
        console.error(
            "Music get playlists error:",
            error
        );

        return res.status(500).json({
            success: false,
            message:
                error.message ||
                "Unable to load playlists."
        });
    }
};

exports.createPlaylist = async (req, res) => {
    try {
        const playlist =
            await musicUserService.createPlaylist(
                req.propertyUserId,
                req.body?.name
            );

        return res.json({
            success: true,
            playlist
        });
    }
    catch (error) {
        console.error(
            "Music create playlist error:",
            error
        );

        return res.status(400).json({
            success: false,
            message:
                error.message ||
                "Unable to create playlist."
        });
    }
};

exports.renamePlaylist = async (req, res) => {
    try {
        await musicUserService.renamePlaylist(
            req.propertyUserId,
            req.body?.playlistId,
            req.body?.name
        );

        return res.json({
            success: true
        });
    }
    catch (error) {
        console.error(
            "Music rename playlist error:",
            error
        );

        return res.status(400).json({
            success: false,
            message:
                error.message ||
                "Unable to rename playlist."
        });
    }
};

exports.deletePlaylist = async (req, res) => {
    try {
        await musicUserService.deletePlaylist(
            req.propertyUserId,
            req.body?.playlistId
        );

        return res.json({
            success: true
        });
    }
    catch (error) {
        console.error(
            "Music delete playlist error:",
            error
        );

        return res.status(400).json({
            success: false,
            message:
                error.message ||
                "Unable to delete playlist."
        });
    }
};

exports.addSongToPlaylist = async (req, res) => {
    try {
        await musicUserService.addSongToPlaylist(
            req.propertyUserId,
            req.body?.playlistId,
            req.body?.videoId
        );

        return res.json({
            success: true
        });
    }
    catch (error) {
        console.error(
            "Music add playlist song error:",
            error
        );

        return res.status(400).json({
            success: false,
            message:
                error.message ||
                "Unable to add song to playlist."
        });
    }
};

exports.removeSongFromPlaylist = async (
    req,
    res
) => {
    try {
        await musicUserService.removeSongFromPlaylist(
            req.propertyUserId,
            req.body?.playlistId,
            req.body?.videoId
        );

        return res.json({
            success: true
        });
    }
    catch (error) {
        console.error(
            "Music remove playlist song error:",
            error
        );

        return res.status(400).json({
            success: false,
            message:
                error.message ||
                "Unable to remove song from playlist."
        });
    }
};
