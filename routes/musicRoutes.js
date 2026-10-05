const express = require("express");

const router = express.Router();

const musicController =
    require("../controllers/musicController");

const musicUserController =
    require("../controllers/musicUserController");

const propertyAuthMiddleware =
    require("../middlewares/propertyAuthMiddleware");


router.get(
    "/search",
    musicController.search
);

router.post(
    "/save",
    musicController.save
);

router.post(
    "/add-youtube",
    musicController.addYouTubeSong
);

router.get(
    "/discover",
    musicController.discover
);


/*
 * Account Favorites
 */

router.get(
    "/favorites",
    propertyAuthMiddleware,
    musicUserController.getFavorites
);

router.post(
    "/favorites/add",
    propertyAuthMiddleware,
    musicUserController.addFavorite
);

router.post(
    "/favorites/remove",
    propertyAuthMiddleware,
    musicUserController.removeFavorite
);


/*
 * Account Playlists
 */

router.get(
    "/playlists",
    propertyAuthMiddleware,
    musicUserController.getPlaylists
);

router.post(
    "/playlists/create",
    propertyAuthMiddleware,
    musicUserController.createPlaylist
);

router.post(
    "/playlists/rename",
    propertyAuthMiddleware,
    musicUserController.renamePlaylist
);

router.post(
    "/playlists/delete",
    propertyAuthMiddleware,
    musicUserController.deletePlaylist
);

router.post(
    "/playlists/songs/add",
    propertyAuthMiddleware,
    musicUserController.addSongToPlaylist
);

router.post(
    "/playlists/songs/remove",
    propertyAuthMiddleware,
    musicUserController.removeSongFromPlaylist
);


module.exports = router;
