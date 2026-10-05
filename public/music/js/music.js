(async () => {
    "use strict";

    /* =========================================================
       CONFIG
    ========================================================= */

    const API_BASE = "/api/music/search";

    const STORAGE_KEYS = {
    favorites: "alignMusicFavorites",
    recent: "alignMusicRecent",
    playback: "alignMusicPlayback",
    searchHistory: "alignMusicSearchHistory",
    video: "alignMusicVideo"
};
window.MUSIC_AUTH = {
    getToken() {
        return localStorage.getItem("propertyToken") || "";
    },

    getUser() {
        try {
            return JSON.parse(
                localStorage.getItem("propertyUser") || "null"
            );
        } catch {
            return null;
        }
    },

    isLoggedIn() {
        return !!this.getToken();
    },

    headers() {
        const token = this.getToken();

        return token
            ? {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`
            }
            : {
                "Content-Type": "application/json"
            };
    },

    async request(url, options = {}) {
        const token = this.getToken();

        const response = await fetch(url, {
            ...options,
            headers: {
                ...(options.headers || {}),
                ...(token
                    ? {
                        Authorization: `Bearer ${token}`
                    }
                    : {})
            }
        });

        let data = {};

        try {
            data = await response.json();
        } catch {
            data = {};
        }

        if (response.status === 401) {
            throw new Error("LOGIN_REQUIRED");
        }

        if (!response.ok || data.success === false) {
            throw new Error(
                data.message || "Music account request failed."
            );
        }

        return data;
    }
};

const MAX_SEARCH_HISTORY = 30;

    const MAX_RECENT = 20;


    /* =========================================================
       DOM
    ========================================================= */

    const searchForm =
        document.getElementById(
            "musicSearchForm"
        );    

    const searchInput =
        document.getElementById(
            "musicSearchInput"
        );
    const musicYouTubeInput =
    document.getElementById("musicYouTubeInput");

    const musicYouTubeAddButton =
    document.getElementById("musicYouTubeAddButton");    

    const searchButton =
        document.getElementById(
            "musicSearchButton"
        );

    const clearButton =
        document.getElementById(
            "musicClearButton"
        );
        

    const resultsContainer =
        document.getElementById(
            "musicResults"
        );

    const resultsInfo =
        document.getElementById(
            "musicResultsInfo"
        );

    const loading =
        document.getElementById(
            "musicLoading"
        );

    const emptyState =
        document.getElementById(
            "musicEmpty"
        );

    const pagination =
        document.getElementById(
            "musicPagination"
        );

    const previousButton =
        document.getElementById(
            "musicPreviousButton"
        );

    const nextButton =
        document.getElementById(
            "musicNextButton"
        );

    const favoritesContainer =
        document.getElementById(
            "musicFavorites"
        );

    const recentContainer =
        document.getElementById(
            "musicRecent"
        );

    const playerTitle =
        document.getElementById(
            "musicPlayerTitle"
        );

    const playerArtist =
        document.getElementById(
            "musicPlayerArtist"
        );

    const playerArtwork =
        document.getElementById(
            "musicPlayerArtwork"
        );

    const playPauseButton =
        document.getElementById(
            "musicPlayPause"
        );

    const playerPreviousButton =
        document.getElementById(
            "musicPrevious"
        );

    const playerNextButton =
        document.getElementById(
            "musicNext"
        );
    const playerVideoButton =
        document.getElementById(
         "musicVideo"
        );    

    const musicPlayer =
        document.getElementById(
            "musicPlayer"
        );

    const musicPlayerClose =
        document.getElementById(
            "musicPlayerClose"
        );

    const progress =
        document.getElementById(
            "musicProgress"
        );

    const currentTime =
        document.getElementById(
            "musicCurrentTime"
        );

    const duration =
        document.getElementById(
            "musicDuration"
        );

    const volume =
        document.getElementById(
            "musicVolume"
        );

    const toast =
        document.getElementById(
            "musicToast"
        );
    const resultsSection =
    document.getElementById("musicResultsSection");   
    const searchHistoryButton =
    document.getElementById("musicSearchHistoryButton");

const searchHistoryPanel =
    document.getElementById("musicSearchHistoryPanel");

const searchHistoryList =
    document.getElementById("musicSearchHistoryList");

const clearSearchHistoryButton =
    document.getElementById("musicClearSearchHistory"); 


    /* =========================================================
       STATE
    ========================================================= */

    let results = [];
    let discoverResults = [];

    let currentIndex = -1;

    let currentSong = null;
    let activePlaybackList = [];
    let activePlaybackIndex = -1;
    let nextPageToken = null;

    let previousPageToken = null;

    let lastSearchQuery = "";

    let player = null;

    let playerReady = false;

    let playerState = -1;
    let playbackLoading = false;
    let playbackLoadingTimer = null;
    const PLAYBACK_LOADING_TIMEOUT = 10000;
    let musicVideoPlayer = null;

    let progressTimer = null;

    let toastTimer = null;

    let lastPlaybackSaveAt = 0;
    let discoverContainer =
    document.getElementById("musicDiscover");

    let favorites =
        loadStorage(
            STORAGE_KEYS.favorites,
            []
        );

    let recent =
        loadStorage(
            STORAGE_KEYS.recent,
            []
        );
    let searchHistory =
    loadStorage(
        STORAGE_KEYS.searchHistory,
        []
    );

if (!Array.isArray(searchHistory)) {
    searchHistory = [];
}    


    /* =========================================================
       STORAGE
    ========================================================= */

    function loadStorage(
        key,
        fallback
    ) {
        try {

            const value =
                localStorage.getItem(key);

            if (!value) {
                return fallback;
            }

            const parsed =
                JSON.parse(value);

            return parsed ?? fallback;

        }
        catch (error) {

            console.warn(
                `Unable to read ${key}`,
                error
            );

            return fallback;
        }
    }


    function saveStorage(
        key,
        value
    ) {
        try {

            localStorage.setItem(
                key,
                JSON.stringify(value)
            );

        }
        catch (error) {

            console.warn(
                `Unable to save ${key}`,
                error
            );
        }
    }


    /* =========================================================
       PLAYBACK PERSISTENCE / SPA VIEW
    ========================================================= */

    function savePlaybackState(force = false) {

        if (!currentSong?.videoId) {
            return;
        }

        const now = Date.now();

        if (!force && now - lastPlaybackSaveAt < 1800) {
            return;
        }

        let currentTime = 0;

        try {
            if (playerReady && player) {
                currentTime =
                    Number(
                        player.getCurrentTime()
                    ) || 0;
            }
        }
        catch {
            currentTime = 0;
        }

        try {
            localStorage.setItem(
                STORAGE_KEYS.playback,
                JSON.stringify({
    song: currentSong,
    currentTime,
    isPlaying:
        playerState ===
        YT.PlayerState.PLAYING,

    playbackList:
        Array.isArray(activePlaybackList)
            ? activePlaybackList
            : [],

    playbackIndex:
        Number.isInteger(activePlaybackIndex)
            ? activePlaybackIndex
            : -1,

    savedAt: now
})
            );

            lastPlaybackSaveAt = now;
        }
        catch (error) {
            console.warn(
                "Unable to save playback state",
                error
            );
        }
    }


    function loadPlaybackState() {

        try {
            const raw =
                localStorage.getItem(
                    STORAGE_KEYS.playback
                );

            if (!raw) {
                return null;
            }

            const saved = JSON.parse(raw);

            if (!saved?.song?.videoId) {
                return null;
            }

            return saved;
        }
        catch {
            return null;
        }
    }
    function saveMusicVideoState() {
    try {
        if (!musicVideoPlayer || !currentSong?.videoId) {
            return;
        }

        let currentTime = 0;
        let videoState = YT?.PlayerState?.PAUSED;

        if (
            typeof musicVideoPlayer.getCurrentTime === "function"
        ) {
            currentTime =
                Number(
                    musicVideoPlayer.getCurrentTime()
                ) || 0;
        }

        if (
            typeof musicVideoPlayer.getPlayerState === "function"
        ) {
            videoState =
                musicVideoPlayer.getPlayerState();
        }

        localStorage.setItem(
            STORAGE_KEYS.video,
            JSON.stringify({
                isOpen: true,
                song: currentSong,
                currentTime,
                isPlaying:
                    videoState ===
                    YT.PlayerState.PLAYING,
                savedAt: Date.now()
            })
        );

    }
    catch (error) {
        console.warn(
            "Unable to save music video state:",
            error
        );
    }
}


function loadMusicVideoState() {
    try {
        const raw =
            localStorage.getItem(
                STORAGE_KEYS.video
            );

        if (!raw) {
            return null;
        }

        const saved =
            JSON.parse(raw);

        if (
            !saved?.isOpen ||
            !saved?.song?.videoId
        ) {
            return null;
        }

        return saved;

    }
    catch {
        return null;
    }
}
function restoreMusicVideoState() {
    try {
        const saved = loadMusicVideoState();

        if (
            !saved?.isOpen ||
            !saved?.song?.videoId
        ) {
            return;
        }

        if (
            typeof openMusicVideo !== "function"
        ) {
            console.warn(
                "openMusicVideo() is not available yet."
            );
            return;
        }

        openMusicVideo(
            saved.song,
            Number(saved.currentTime) || 0,
            saved.isPlaying === true
        );

    } catch (error) {
        console.warn(
            "Unable to restore music video state:",
            error
        );
    }
}
function openMusicVideo(song, startSeconds = 0, shouldPlay = true) {
    if (!song?.videoId) {
        return;
    }

    try {
        // Close any existing video overlay
        const existingOverlay =
            document.querySelector(".music-video-overlay");

        if (existingOverlay) {
            existingOverlay.remove();
        }

        if (musicVideoPlayer) {
            try {
                musicVideoPlayer.destroy();
            } catch {}
            musicVideoPlayer = null;
        }

        // Keep main music paused while video is open
        if (player && typeof player.pauseVideo === "function") {
            player.pauseVideo();
        }

        const overlay =
            document.createElement("div");

        overlay.className =
            "music-video-overlay";

        overlay.innerHTML = `
            <div class="music-video-container">
                <button
                    type="button"
                    class="music-video-close"
                    aria-label="Close video"
                >
                    ×
                </button>

                <div class="music-video-frame-wrap">
                    <div class="music-video-frame"></div>
                </div>
            </div>
        `;

        document.body.appendChild(overlay);

        const closeButton =
            overlay.querySelector(
                ".music-video-close"
            );
        const closeVideo = () => {

    if (
        musicVideoPlayer &&
        typeof musicVideoPlayer.destroy === "function"
    ) {
        musicVideoPlayer.destroy();
        musicVideoPlayer = null;
    }

    overlay.remove();

    localStorage.removeItem(
        STORAGE_KEYS.video
    );
};    

        closeButton.addEventListener(
            "click",
            closeVideo
        );

        const frame =
            overlay.querySelector(
                ".music-video-frame"
            );

        if (
            typeof YT === "undefined" ||
            typeof YT.Player !== "function"
        ) {
            console.warn(
                "YouTube API is not ready."
            );
            return;
        }

        musicVideoPlayer =
            new YT.Player(frame, {
                videoId: song.videoId,

                playerVars: {
    autoplay: shouldPlay ? 1 : 0,
    controls: 1,
    fs: 1,
    playsinline: 1,
    rel: 0,
    modestbranding: 1
},

                events: {
                    onReady: function (event) {

    const iframe =
        event.target.getIframe();

    if (iframe) {

        iframe.setAttribute(
            "allowfullscreen",
            ""
        );

        iframe.setAttribute(
            "webkitallowfullscreen",
            ""
        );

        iframe.setAttribute(
            "mozallowfullscreen",
            ""
        );

        iframe.setAttribute(
            "allow",
            "autoplay; encrypted-media; picture-in-picture; fullscreen"
        );

        iframe.style.pointerEvents = "auto";
        
    }

    const seconds =
        Number(startSeconds) || 0;

    if (seconds > 0) {
        event.target.seekTo(
            seconds,
            true
        );
    }

    if (shouldPlay) {
        event.target.playVideo();
    } else {
        event.target.pauseVideo();
    }

    saveMusicVideoState();
},

                    onStateChange: function (event) {

                        if (
                            event.data ===
                            YT.PlayerState.ENDED
                        ) {
                            playNextMusicVideo();
                            return;
                        }

                        saveMusicVideoState();
                    }
                }
            });

    } catch (error) {
        console.warn(
            "Unable to open music video:",
            error
        );
    }
}


    function expandMusicPlayer() {

        if (!musicPlayer || !currentSong) {
            return;
        }

        musicPlayer.classList.add(
            "is-expanded"
        );

        if (musicPlayerClose) {
            musicPlayerClose.classList.remove(
                "hidden"
            );
        }

        if (location.hash !== "#player") {
            history.pushState(
                { musicPlayer: true },
                "",
                "#player"
            );
        }
    }


    function collapseMusicPlayer(restoreHistory = true) {

        if (!musicPlayer) {
            return;
        }

        musicPlayer.classList.remove(
            "is-expanded"
        );

        if (musicPlayerClose) {
            musicPlayerClose.classList.add(
                "hidden"
            );
        }

        if (restoreHistory && location.hash === "#player") {
            history.back();
        }
    }


    function restorePlaybackState() {

        const saved =
            loadPlaybackState();

        if (!saved?.song?.videoId || !playerReady || !player) {
            return;
        }

       currentSong =
    normalizeSong(
        saved.song
    );

activePlaybackList =
    Array.isArray(saved.playbackList)
        ? saved.playbackList
            .map(normalizeSong)
            .filter(song => song.videoId)
        : [];

activePlaybackIndex =
    Number.isInteger(saved.playbackIndex)
        ? saved.playbackIndex
        : -1;

currentIndex =
    activePlaybackIndex;

        updatePlayerUI();
        renderRecent();

        const startSeconds =
            Math.max(
                0,
                Number(saved.currentTime) || 0
            );

        try {
            if (saved.isPlaying) {

    player.loadVideoById({
        videoId: currentSong.videoId,
        startSeconds
    });

} else {

    player.cueVideoById({
        videoId: currentSong.videoId,
        startSeconds
    });

    player.pauseVideo();

}

            if (location.hash === "#player") {
                expandMusicPlayer();
            }

        }
        catch (error) {
            console.error(
                "Unable to restore playback:",
                error
            );
        }
    }


    window.addEventListener(
    "beforeunload",
    () => {
        savePlaybackState(true);
        saveMusicVideoState();
    }
);

    document.addEventListener(
    "visibilitychange",
    () => {
        if (document.visibilityState === "hidden") {
            savePlaybackState(true);
            saveMusicVideoState();
        }
    }
);


        /* =========================================================
       HELPERS
    ========================================================= */

    function escapeHtml(
        value
    ) {

        return String(
            value ?? ""
        )
            .replaceAll(
                "&",
                "&amp;"
            )
            .replaceAll(
                "<",
                "&lt;"
            )
            .replaceAll(
                ">",
                "&gt;"
            )
            .replaceAll(
                '"',
                "&quot;"
            )
            .replaceAll(
                "'",
                "&#039;"
            );
    }


    function formatTime(
        seconds
    ) {

        const value =
            Math.max(
                0,
                Math.floor(
                    Number(seconds) || 0
                )
            );

        const minutes =
            Math.floor(
                value / 60
            );

        const remainingSeconds =
            value % 60;

        return `${minutes}:${String(
            remainingSeconds
        ).padStart(2, "0")}`;
    }


    function showToast(
        message
    ) {

        if (!toast) {
            return;
        }

        toast.textContent =
            message;

        toast.classList.add(
            "show"
        );

        clearTimeout(
            toastTimer
        );

        toastTimer =
            setTimeout(() => {

                toast.classList.remove(
                    "show"
                );

            }, 2400);
    }


       function normalizeSong(
        song
    ) {

        return {
            videoId:
                song?.videoId ||
                null,

            title:
                song?.title ||
                "Unknown title",

            artist:
                song?.artist ||
                null,

            description:
                song?.description ||
                "",

            channelTitle:
                song?.channelTitle ||
                "YouTube",
            channelId:
                song?.channelId ||
                null,    

            publishedAt:
                song?.publishedAt ||
                null,

            duration:
                song?.duration ||
                null,

            language:
                song?.language ||
                null,

            thumbnails: {
    default:
        song?.thumbnails?.default ||
        song?.thumbnailUrl ||
        null,

    medium:
        song?.thumbnails?.medium ||
        song?.thumbnailUrl ||
        null,

    high:
        song?.thumbnails?.high ||
        song?.thumbnailUrl ||
        null

            }
        };
    }


    function getFavoriteLanguages() {

        return [
            ...new Set(
                favorites
                    .map(song =>
                        String(
                            song?.language || ""
                        ).trim().toLowerCase()
                    )
                    .filter(Boolean)
            )
        ];
    }
    function getFavoriteArtists() {
    return [
        ...new Set(
            favorites
                .map(song =>
                    String(song?.artist || "").trim()
                )
                .filter(Boolean)
        )
    ];
}


function getFavoriteChannels() {
    return [
        ...new Set(
            favorites
                .map(song =>
                    String(song?.channelTitle || "").trim()
                )
                .filter(Boolean)
        )
    ];
}
function getFavoriteChannelIds() {
    return [
        ...new Set(
            favorites
                .map(song =>
                    String(song?.channelId || "").trim()
                )
                .filter(Boolean)
        )
    ];
}

function getFavoriteVideoIds() {
    return [
        ...new Set(
            favorites
                .map(song =>
                    String(song?.videoId || "").trim()
                )
                .filter(Boolean)
        )
    ];
}


  async function loadMusicDiscover() {
    if (!discoverContainer) return;

    discoverContainer.innerHTML = `
        
    `;

    try {
        const languages = getFavoriteLanguages();
        const artists = getFavoriteArtists();
        const channels = getFavoriteChannels();
        const favoriteVideoIds = getFavoriteVideoIds();

        const params = new URLSearchParams();

        if (languages.length) {
            params.set("languages", languages.join(","));
        }

        if (artists.length) {
            params.set("artists", artists.join(","));
        }

        if (channels.length) {
            params.set("channels", channels.join(","));
        }

        if (favoriteVideoIds.length) {
            params.set(
                "favoriteVideoIds",
                favoriteVideoIds.join(",")
            );
        }

        params.set("limit", "60");

        const response = await fetch(
            `/api/music/discover?${params.toString()}`
        );

        if (!response.ok) {
            throw new Error(
                `Discover API failed: ${response.status}`
            );
        }

        const data = await response.json();

        const relatedSongs =
            Array.isArray(data?.songs?.related)
                ? data.songs.related
                    .map(normalizeSong)
                    .filter(song => song.videoId)
                : [];

        const latestSongs =
            Array.isArray(data?.songs?.latest)
                ? data.songs.latest
                    .map(normalizeSong)
                    .filter(song => song.videoId)
                : [];

        /*
         * IMPORTANT:
         * Ye hi actual Discover playback/click list hogi.
         */
        discoverResults = [
            ...relatedSongs,
            ...latestSongs
        ];

        if (!discoverResults.length) {
            discoverContainer.innerHTML = `
                
            `;
            return;
        }

        let html = "";

        if (relatedSongs.length) {
            html += `
                <div class="music-discover-group">

                    <div class="music-discover-grid">
                        ${relatedSongs
                            .map((song, index) =>
                                renderResultCard(
                                    song,
                                    index
                                )
                            )
                            .join("")}
                    </div>
                </div>
            `;
        }

        if (latestSongs.length) {
            html += `
                <div class="music-discover-group">

                    <div class="music-discover-grid">
                        ${latestSongs
                            .map((song, index) =>
                                renderResultCard(
                                    song,
                                    relatedSongs.length + index
                                )
                            )
                            .join("")}
                    </div>
                </div>
            `;
        }

        discoverContainer.innerHTML = html;

    } catch (error) {
        console.error(
            "Discover music error:",
            error
        );

        discoverResults = [];

        discoverContainer.innerHTML = `
            <div class="music-empty">
                <i class="fas fa-exclamation-circle"></i>
                <span>Unable to load discover music.</span>
            </div>
        `;
    }
}
    function getSongImage(
        song
    ) {

        return (
            song?.thumbnails?.high ||
            song?.thumbnails?.medium ||
            song?.thumbnails?.default ||
            ""
        );
    }


    function isFavorite(
        videoId
    ) {

        return favorites.some(
            song =>
                song.videoId === videoId
        );
    }

/* =========================================================
   SEARCH HISTORY
========================================================= */

function normalizeSearchHistoryEntry(entry) {

    if (!entry || !entry.query) {
        return null;
    }

    return {
        id:
            entry.id ||
            `${Date.now()}-${Math.random()
                .toString(36)
                .slice(2, 9)}`,

        query:
            String(entry.query).trim(),

        results:
            Array.isArray(entry.results)
                ? entry.results
                    .map(normalizeSong)
                    .filter(song => song.videoId)
                : [],

        nextPageToken:
            entry.nextPageToken || null,

        previousPageToken:
            entry.previousPageToken || null,

        savedAt:
            Number(entry.savedAt) || Date.now()
    };
}


function saveSearchHistory() {

    try {

        localStorage.setItem(
            STORAGE_KEYS.searchHistory,
            JSON.stringify(searchHistory)
        );

    }
    catch (error) {

        console.warn(
            "Unable to save search history",
            error
        );

    }
}


function saveCurrentSearchToHistory(query) {

    const cleanQuery =
        String(query || "").trim();

    if (!cleanQuery) {
        return;
    }

    const existingIndex =
        searchHistory.findIndex(
            item =>
                String(item?.query || "")
                    .toLowerCase() ===
                cleanQuery.toLowerCase()
        );

    /*
       Same search dobara ki gayi hai.
       Purani entry remove karke latest ko top par rakhenge.
    */
    if (existingIndex !== -1) {

        searchHistory.splice(
            existingIndex,
            1
        );

    }

    const historyEntry = {

        id:
            `${Date.now()}-${Math.random()
                .toString(36)
                .slice(2, 9)}`,

        query:
            cleanQuery,

        /*
           IMPORTANT:
           Search ke actual results save honge.
           Isliye baad mein API call ke bina
           search result screen restore ho sakti hai.
        */
        results:
            Array.isArray(results)
                ? results.map(normalizeSong)
                : [],

        nextPageToken:
            nextPageToken || null,

        previousPageToken:
            previousPageToken || null,

        savedAt:
            Date.now()
    };

    searchHistory.unshift(
        historyEntry
    );

    searchHistory =
        searchHistory
            .map(normalizeSearchHistoryEntry)
            .filter(Boolean)
            .slice(
                0,
                MAX_SEARCH_HISTORY
            );

    saveSearchHistory();

    renderSearchHistory();
}


function updateCurrentSearchHistory() {

    if (!lastSearchQuery) {
        return;
    }

    const index =
        searchHistory.findIndex(
            item =>
                String(item?.query || "")
                    .toLowerCase() ===
                lastSearchQuery.toLowerCase()
        );

    if (index === -1) {

        saveCurrentSearchToHistory(
            lastSearchQuery
        );

        return;
    }

    searchHistory[index] = {

        ...searchHistory[index],

        results:
            Array.isArray(results)
                ? results.map(normalizeSong)
                : [],

        nextPageToken:
            nextPageToken || null,

        previousPageToken:
            previousPageToken || null,

        savedAt:
            Date.now()
    };

    saveSearchHistory();

    renderSearchHistory();
}


function formatSearchHistoryTime(timestamp) {

    const date =
        new Date(timestamp);

    if (Number.isNaN(date.getTime())) {
        return "";
    }

    return date.toLocaleString(
        undefined,
        {
            day: "2-digit",
            month: "short",
            hour: "2-digit",
            minute: "2-digit"
        }
    );
}


function renderSearchHistory() {

    if (!searchHistoryList) {
        return;
    }

    if (!searchHistory.length) {

        searchHistoryList.innerHTML = `
            <div class="music-search-history-empty">
                No search history yet.
            </div>
        `;

        return;
    }

    searchHistoryList.innerHTML =
        searchHistory
            .map(entry => {

                const safeQuery =
                    escapeHtml(
                        entry.query
                    );

                const resultCount =
                    Array.isArray(entry.results)
                        ? entry.results.length
                        : 0;

                return `
                    <div
                        class="music-search-history-item">

                        <button
                            type="button"
                            class="music-search-history-open"
                            data-search-history-id="${escapeHtml(entry.id)}">

                            <span
                                class="music-search-history-query">
                                ${safeQuery}
                            </span>

                            <span
                                class="music-search-history-meta">
                                ${resultCount} results
                                ·
                                ${escapeHtml(
                                    formatSearchHistoryTime(
                                        entry.savedAt
                                    )
                                )}
                            </span>

                        </button>

                        <button
                            type="button"
                            class="music-search-history-delete"
                            data-delete-search-history-id="${escapeHtml(entry.id)}"
                            aria-label="Delete ${safeQuery}"
                            title="Delete">

                            ×

                        </button>

                    </div>
                `;

            })
            .join("");
}


function openSavedSearchHistory(id) {

    const entry =
        searchHistory.find(
            item =>
                String(item.id) ===
                String(id)
        );

    if (!entry) {
        return;
    }

    /*
       Restore the complete saved search.
    */

    lastSearchQuery =
        entry.query;

    results =
        Array.isArray(entry.results)
            ? entry.results.map(normalizeSong)
            : [];

    nextPageToken =
        entry.nextPageToken || null;

    previousPageToken =
        entry.previousPageToken || null;


    /*
       Search input mein query wapas.
    */

    if (searchInput) {

        searchInput.value =
            entry.query;

    }


    /*
       Hidden results section ko open karo.
    */

    if (resultsSection) {

        resultsSection.classList.remove(
            "hidden"
        );

    }


    /*
       Saved results render karo.
       API call nahi hogi.
    */

    renderResults();

    updatePagination();
    


    if (results.length) {

        resultsInfo.textContent =
            `${results.length} saved results for "${entry.query}"`;

        emptyState.classList.add(
            "hidden"
        );

    }
    else {

        resultsInfo.textContent =
            `No saved results for "${entry.query}"`;

        emptyState.classList.remove(
            "hidden"
        );

    }


    /*
       Existing clear button show karo.
    */

    clearButton.classList.remove(
        "hidden"
    );


    closeSearchHistoryPanel();


    /*
       Result section tak scroll.
    */

    if (resultsSection) {

        window.scrollTo({

            top:
                resultsSection
                    .getBoundingClientRect()
                    .top +
                window.scrollY -
                20,

            behavior: "smooth"

        });

    }
}


function deleteSearchHistory(id) {

    searchHistory =
        searchHistory.filter(
            item =>
                String(item.id) !==
                String(id)
        );

    saveSearchHistory();

    renderSearchHistory();
}


function clearAllSearchHistory() {

    searchHistory = [];

    saveSearchHistory();

    renderSearchHistory();
}


function openSearchHistoryPanel() {

    if (!searchHistoryPanel) {
        return;
    }

    searchHistoryPanel.classList.remove(
        "hidden"
    );

    if (searchHistoryButton) {

        searchHistoryButton.setAttribute(
            "aria-expanded",
            "true"
        );

    }

    renderSearchHistory();
}


function closeSearchHistoryPanel() {

    if (!searchHistoryPanel) {
        return;
    }

    searchHistoryPanel.classList.add(
        "hidden"
    );

    if (searchHistoryButton) {

        searchHistoryButton.setAttribute(
            "aria-expanded",
            "false"
        );

    }
}
    /* =========================================================
       YOUTUBE PLAYER
    ========================================================= */

    window.onYouTubeIframeAPIReady =
        () => {

            createYouTubePlayer();

        };


    function createYouTubePlayer() {

        if (
            typeof YT === "undefined" ||
            !YT.Player
        ) {
            return;
        }

        if (player) {
            return;
        }

        player =
            new YT.Player(
                "youtubePlayer",
                {
                    width: "2",
                    height: "2",

                    playerVars: {
    autoplay: 0,
    controls: 0,
    disablekb: 1,
    fs: 0,
    iv_load_policy: 3,
    modestbranding: 1,
    playsinline: 1,
    rel: 0
},

origin: window.location.origin,

                    events: {

                        onReady:
                            handlePlayerReady,

                        onStateChange:
                            handlePlayerStateChange,

                        onError:
                            handlePlayerError
                    }
                }
            );
    }


    function handlePlayerReady(
        event
    ) {

        playerReady = true;
        restoreMusicVideoState();

        const savedVolume =
            Number(
                localStorage.getItem(
                    STORAGE_KEYS.volume
                )
            );

        const initialVolume =
    Number.isFinite(savedVolume) && savedVolume > 0
        ? Math.max(
            0,
            Math.min(
                100,
                savedVolume
            )
        )
        : 100;

        if (volume) {
            volume.value =
                String(
                    initialVolume
                );
        }

        event.target.setVolume(
            initialVolume
        );

        restorePlaybackState();
    }


    function handlePlayerStateChange(
        event
    ) {
        playerState =
            event.data;

        if (
            event.data ===
            YT.PlayerState.BUFFERING
        ) {
            lockPlaybackControls();
        }

        if (
            event.data ===
            YT.PlayerState.PLAYING
        ) {
            unlockPlaybackControls();
        }

        savePlaybackState(true);

        if (
            event.data ===
            YT.PlayerState.PLAYING
        ) {

            updatePlayButton(
                true
            );

            startProgressTimer();

        }
        else if (
            event.data ===
            YT.PlayerState.PAUSED
        ) {

            updatePlayButton(
                false
            );

            stopProgressTimer();

        }
        else if (
            event.data ===
            YT.PlayerState.ENDED
        ) {

            updatePlayButton(
                false
            );

            stopProgressTimer();

            if (window.AlignMusicExtras?.repeat && currentSong) {
    playSong(
        currentSong,
        activePlaybackIndex,
        activePlaybackList
    );
} else {
    playNext();
}

        }
        else {

            updatePlayButton(
                false
            );
        }
    }


    function handlePlayerError(
        event
    ) {

        stopProgressTimer();

        updatePlayButton(
            false
        );

        const errorMessages = {
            2: "Invalid YouTube video.",
            5: "This video cannot be played.",
            100: "This video is unavailable.",
            101: "Playback is not allowed for this video.",
            150: "Playback is not allowed for this video."
        };

        showToast(
            errorMessages[event.data] ||
            "Unable to play this video."
        );
    }


    /* =========================================================
       SEARCH
    ========================================================= */

    async function searchMusic(
        query,
        pageToken = ""
    ) {

        const cleanQuery =
            String(
                query || ""
            ).trim();

        if (!cleanQuery) {
            showToast(
                "Enter a song or artist to search."
            );
            return;
        }

        setLoading(
            true
        );

        try {

            const params =
                new URLSearchParams({
                    q: cleanQuery,
                    limit: "12"
                });

            if (pageToken) {
                params.set(
                    "pageToken",
                    pageToken
                );
            }

            const response =
                await fetch(
                    `${API_BASE}?${params.toString()}`,
                    {
                        method: "GET",
                        headers: {
                            Accept:
                                "application/json"
                        }
                    }
                );

            let data = {};

            try {

                data =
                    await response.json();

            }
            catch {

                throw new Error(
                    "Invalid server response."
                );
            }

            if (!response.ok ||
                !data.success
            ) {

                throw new Error(
                    data.message ||
                    "Unable to search music."
                );
            }

            results =
                Array.isArray(
                    data.items
                )
                    ? data.items
                        .map(
                            normalizeSong
                        )
                        .filter(
                            song =>
                                song.videoId
                        )
                    : [];
                    

            nextPageToken =
                data.nextPageToken ||
                null;

            previousPageToken =
                data.prevPageToken ||
                null;

            lastSearchQuery =
                cleanQuery;
            saveCurrentSearchToHistory(
    cleanQuery
);    
            if (resultsSection) {
    resultsSection.classList.remove("hidden");
}    

            renderResults();

            updatePagination();
            updateCurrentSearchHistory();


            if (results.length) {

                resultsInfo.textContent =
                    `${results.length} results for "${cleanQuery}"`;

                emptyState.classList.add(
                    "hidden"
                );

                clearButton.classList.remove(
                    "hidden"
                );

            }
            else {

                resultsInfo.textContent =
                    `No results found for "${cleanQuery}"`;

                emptyState.classList.remove(
                    "hidden"
                );

                clearButton.classList.remove(
                    "hidden"
                );
            }

        }
        catch (error) {

            console.error(
                "Music search error:",
                error
            );

            results = [];

            nextPageToken = null;

            previousPageToken = null;

            resultsContainer.innerHTML = "";

            emptyState.classList.remove(
                "hidden"
            );

            resultsInfo.textContent =
                error.message ||
                "Unable to search music.";

            showToast(
                error.message ||
                "Unable to search music."
            );

        }
        finally {

            setLoading(
                false
            );
        }
    }


    function setLoading(
        state
    ) {

        if (!loading) {
            return;
        }

        loading.classList.toggle(
            "hidden",
            !state
        );

        if (state) {

            emptyState.classList.add(
                "hidden"
            );

            searchButton.disabled =
                true;

        }
        else {

            searchButton.disabled =
                false;
        }
    }


    /* =========================================================
       RENDER SEARCH RESULTS
    ========================================================= */

    function renderResults() {

        if (!resultsContainer) {
            return;
        }

        if (!results.length) {

            resultsContainer.innerHTML =
                "";

            return;
        }

        resultsContainer.innerHTML =
            results
                .map(
                    (
                        song,
                        index
                    ) =>
                        renderResultCard(
                            song,
                            index
                        )
                )
                .join("");
    }


    function renderResultCard(
        song,
        index
    ) {

        const image =
            getSongImage(
                song
            );

        const favorite =
            isFavorite(
                song.videoId
            );

        return `
            <article
                class="music-result-card"
                data-index="${index}">

                <div class="music-result-image-wrap">

                    ${
                        image
                            ? `
                                <img
                                    src="${escapeHtml(image)}"
                                    alt="${escapeHtml(song.title)}"
                                    class="music-result-image"
                                    loading="lazy">
                              `
                            : `
                                <div
                                    class="music-result-image"
                                    style="
                                        display:grid;
                                        place-items:center;
                                        color:#8b5cf6;
                                        font-size:32px;
                                    ">
                                    ♪
                                </div>
                              `
                    }

                    <button
                        type="button"
                        class="music-result-play"
                        data-action="play"
                        data-index="${index}"
                        aria-label="Play ${escapeHtml(song.title)}">

                        ▶

                    </button>

                </div>

                <div class="music-result-body">

                    <button
    type="button"
    class="music-result-title"
    data-action="play"
    data-index="${index}"
>
    ${escapeHtml(song.title)}
</button>

                    <div class="music-result-channel">
                        ${escapeHtml(song.channelTitle)}
                    </div>

                    <div class="music-result-actions">

                        <button
                            type="button"
                            class="music-result-action"
                            data-action="play"
                            data-index="${index}">

                            ▶ Play

                        </button>

                        <button
                            type="button"
                            class="music-result-action ${
                                favorite
                                    ? "favorite-active"
                                    : ""
                            }"
                            data-action="favorite"
                            data-index="${index}">

                            ${
                                favorite
                                    ? "♥ Saved"
                                    : "♡ Save"
                            }

                        </button>

                    </div>

                </div>

            </article>
        `;
    }
    function lockPlaybackControls() {
            playbackLoading = true;

        if (playbackLoadingTimer) {
            clearTimeout(playbackLoadingTimer);
        }

        if (playPauseButton) {
            playPauseButton.disabled = true;
            playPauseButton.setAttribute(
                "aria-busy",
                "true"
            );
        }

        playbackLoadingTimer = setTimeout(() => {
            unlockPlaybackControls();
        }, PLAYBACK_LOADING_TIMEOUT);
    }

    function unlockPlaybackControls() {
        playbackLoading = false;

        if (playbackLoadingTimer) {
            clearTimeout(playbackLoadingTimer);
            playbackLoadingTimer = null;
        }

        if (playPauseButton) {
            playPauseButton.disabled = false;
            playPauseButton.removeAttribute(
                "aria-busy"
            );
        }
    }



    /* =========================================================
       PLAYBACK
    ========================================================= */

    function playSong(
    song,
    index = -1,
    playbackList = null
) {
    if (!song?.videoId) {
        return;
    }

    if (playbackLoading) {
        return;
    }

        currentSong =
            normalizeSong(
                song
            );
          

        currentIndex =
            Number.isInteger(index)
                ? index
                : -1;
       if (Array.isArray(playbackList) && playbackList.length) {
    activePlaybackList = playbackList
        .filter(song => song?.videoId)
        .map(normalizeSong);

    activePlaybackIndex =
        Number.isInteger(index)
            ? index
            : 0;
} else {
    activePlaybackList = results;
    activePlaybackIndex = currentIndex;
}       

        try {
            localStorage.setItem(
    STORAGE_KEYS.playback,
    JSON.stringify({
        song: currentSong,
        currentTime: 0,
        isPlaying: true,

        playbackList:
            Array.isArray(activePlaybackList)
                ? activePlaybackList
                : [],

        playbackIndex:
            Number.isInteger(activePlaybackIndex)
                ? activePlaybackIndex
                : -1,

        savedAt: Date.now()
    })
            );
            lastPlaybackSaveAt = Date.now();
        }
        catch (error) {
            console.warn(
                "Unable to save playback state",
                error
            );
        }

        updatePlayerUI();
        

        addToRecent(
            currentSong
        );

        renderRecent();

        if (!playerReady || !player) {

            showToast(
                "Music player is still loading..."
            );

            return;
        }

        try {
            lockPlaybackControls();

            player.loadVideoById(
    currentSong.videoId
);


if (typeof window.AlignMusicExtras?.saveMusicSong === "function") {
    window.AlignMusicExtras.saveMusicSong(currentSong);
}

player.playVideo();

updatePlayButton(true);

        }
        catch (error) {
            unlockPlaybackControls();

            console.error(
                "Playback error:",
                error
            );

            showToast(
                "Unable to start playback."
            );
        }
    }


    function togglePlayPause() {

        if (!currentSong) {

            if (results.length) {

                playSong(
                    results[0],
                    0
                );

            }
            else {

                showToast(
                    "Search for a song first."
                );
            }

            return;
        }

        if (
            !playerReady ||
            !player
        ) {

            showToast(
                "Music player is still loading..."
            );

            return;
        }

        if (
            playerState ===
            YT.PlayerState.PLAYING
        ) {

            player.pauseVideo();

        }
        else {

            player.playVideo();

        }
    }


    function playPrevious() {

        if (!activePlaybackList.length) {
    return;
}

        if (activePlaybackIndex <= 0) {
    playSong(
        activePlaybackList[activePlaybackList.length - 1],
        activePlaybackList.length - 1,
        activePlaybackList
    );
    return;
}

        const index =
    activePlaybackIndex - 1;

playSong(
    activePlaybackList[index],
    index,
    activePlaybackList
);
    }


    function playNext() {

        if (!activePlaybackList.length) {
    return;
}

        if (activePlaybackIndex < 0) {

            playSong(
    activePlaybackList[0],
    0,
    activePlaybackList
);

            return;
        }

        let nextIndex;
        

if (window.AlignMusicExtras?.shuffle) {
    do {
        nextIndex =
            Math.floor(
                Math.random() * activePlaybackList.length
            );
    } while (
        activePlaybackList.length > 1 &&
nextIndex === activePlaybackIndex
    );
} else {
    nextIndex = activePlaybackIndex + 1;
}

        if (nextIndex >= activePlaybackList.length) {
    if (nextPageToken) {
        searchMusic(
            lastSearchQuery,
            nextPageToken
        );
        return;
    }

    playSong(
        activePlaybackList[0],
        0,
        activePlaybackList
    );
    return;
}
activePlaybackIndex = nextIndex;
        playSong(
    activePlaybackList[nextIndex],
    nextIndex,
    activePlaybackList
);
    }
    function playNextMusicVideo() {

    if (
        !Array.isArray(activePlaybackList) ||
        !activePlaybackList.length
    ) {
        return;
    }

    let nextIndex;

    if (window.AlignMusicExtras?.shuffle) {

        do {
            nextIndex =
                Math.floor(
                    Math.random() *
                    activePlaybackList.length
                );

        } while (
            activePlaybackList.length > 1 &&
            nextIndex === activePlaybackIndex
        );

    } else {

        nextIndex =
            activePlaybackIndex + 1;
    }

    if (
        nextIndex >=
        activePlaybackList.length
    ) {
        return;
    }

    const nextSong =
        activePlaybackList[nextIndex];

    if (
        !nextSong ||
        !nextSong.videoId
    ) {
        return;
    }

    activePlaybackIndex =
        nextIndex;

    currentIndex =
        nextIndex;

    currentSong =
        normalizeSong(nextSong);

    updatePlayerUI();

    const overlay =
        document.querySelector(
            ".music-video-overlay"
        );

    if (!overlay) {
        return;
    }

    if (
    !musicVideoPlayer ||
    typeof musicVideoPlayer.loadVideoById !== "function"
) {
    return;
}

musicVideoPlayer.loadVideoById(
    currentSong.videoId
);
}


    /* =========================================================
       PLAYER UI
    ========================================================= */

    function updatePlayerUI() {

        if (!currentSong) {
            return;
        }

        if (playerTitle) {

            playerTitle.textContent =
                currentSong.title;
        }

        if (playerArtist) {

            playerArtist.textContent =
                currentSong.channelTitle;
        }

        if (playerArtwork) {

            const image =
                getSongImage(
                    currentSong
                );

            if (image) {

                playerArtwork.innerHTML =
                    `
                        <img
                            src="${escapeHtml(image)}"
                            alt="">
                    `;

            }
            else {

                playerArtwork.innerHTML =
                    "<span>♪</span>";
            }
        }

        if (currentTime) {
            currentTime.textContent =
                "0:00";
        }

        if (duration) {
            duration.textContent =
                "0:00";
        }

        if (progress) {
            progress.value =
                "0";
        }
        const likeButton = document.getElementById("musicLike");
const likeIcon = likeButton?.querySelector(".music-action-icon");

if (likeButton && likeIcon && currentSong) {
    const likes = window.AlignMusicExtras?.likes || [];

    const liked = likes.some(
        item => item.videoId === currentSong.videoId
    );

    likeIcon.textContent = liked ? "♥" : "♡";
    likeButton.classList.toggle("active", liked);
}
    }


    function updatePlayButton(
        playing
    ) {

        if (!playPauseButton) {
            return;
        }

        playPauseButton.textContent =
            playing
                ? "❚❚"
                : "▶";

        playPauseButton.setAttribute(
            "aria-label",
            playing
                ? "Pause"
                : "Play"
        );
    }


    /* =========================================================
       PROGRESS
    ========================================================= */

    function startProgressTimer() {

        stopProgressTimer();

        progressTimer =
            setInterval(() => {

                updateProgress();
                savePlaybackState();

            }, 100);
    }


    function stopProgressTimer() {

        if (progressTimer) {

            clearInterval(
                progressTimer
            );

            progressTimer =
                null;
        }
    }


    function updateProgress() {

        if (
            !playerReady ||
            !player ||
            !currentSong
        ) {
            return;
        }

        try {

            const current =
                player.getCurrentTime();

            const total =
                player.getDuration();

            if (
                !Number.isFinite(current) ||
                !Number.isFinite(total) ||
                total <= 0
            ) {
                return;
            }

            if (currentTime) {

                currentTime.textContent =
                    formatTime(
                        current
                    );
            }

            if (duration) {

                duration.textContent =
                    formatTime(
                        total
                    );
            }

            if (progress) {
    const percent =
        Math.min(
            100,
            Math.max(
                0,
                (current / total) * 100
            )
        );

    progress.value =
        String(percent);

    progress.style.background =
        `linear-gradient(
            to right,
            #8b5cf6 0%,
            #8b5cf6 ${percent}%,
            #e2e8f0 ${percent}%,
            #e2e8f0 100%
        )`;
}

        }
        catch {
            // Player may not be ready.
        }
    }


    function seekToPercent(
        value
    ) {

        if (
            !playerReady ||
            !player ||
            !currentSong
        ) {
            return;
        }

        try {

            const total =
                player.getDuration();

            if (
                !Number.isFinite(total) ||
                total <= 0
            ) {
                return;
            }

            const target =
                (
                    Number(value) /
                    100
                ) * total;

            player.seekTo(
                target,
                true
            );

        }
        catch {
            // Ignore seek errors.
        }
    }


    /* =========================================================
       VOLUME
    ========================================================= */

    function setVolume(
        value
    ) {

        const numericValue =
            Math.max(
                0,
                Math.min(
                    100,
                    Number(value) || 0
                )
            );

        if (volume) {

            volume.value =
                String(
                    numericValue
                );
        }

        localStorage.setItem(
            STORAGE_KEYS.volume,
            String(
                numericValue
            )
        );

        if (
            playerReady &&
            player
        ) {

            player.setVolume(
                numericValue
            );
        }

    }
    
async function loadAccountFavorites() {
    if (!window.MUSIC_AUTH.isLoggedIn()) {
        return;
    }

    try {
        const localFavorites = Array.isArray(favorites)
            ? favorites
            : [];

        const data = await window.MUSIC_AUTH.request(
            "/api/music/favorites"
        );

        const accountFavorites =
            Array.isArray(data?.favorites)
                ? data.favorites
                : [];

        const accountSongs = accountFavorites
            .filter(song => song?.videoId)
            .map(song =>
                normalizeSong({
                    videoId: song.videoId,
                    title: song.title || "",
                    artist: song.artist || "",
                    channelTitle:
                        song.channelTitle || "",
                    channelId:
                        song.channelId || null,
                    thumbnailUrl:
                        song.thumbnailUrl || null,
                    duration:
                        song.duration || null,
                    language:
                        song.language || null
                })
            );

        const merged = [
            ...accountSongs,
            ...localFavorites.filter(localSong =>
                localSong?.videoId &&
                !accountSongs.some(
                    accountSong =>
                        accountSong.videoId ===
                        localSong.videoId
                )
            )
        ];

        favorites = merged;

        saveStorage(
            STORAGE_KEYS.favorites,
            favorites
        );

        renderResults();
        renderFavorites();

        if (
            typeof refreshLikeButtons ===
            "function"
        ) {
            refreshLikeButtons();
        }

    } catch (error) {
        console.error(
            "Music account favorites load error:",
            error
        );
    }
}
    
    /* =========================================================
       FAVORITES
    ========================================================= */

   async function toggleFavorite(song) {
    if (!window.MUSIC_AUTH.isLoggedIn()) {
        window.location.href = "/music-auth/login.html";
        return;
    }

    if (!song?.videoId) {
        return;
    }

    const existingIndex =
        favorites.findIndex(
            item =>
                item.videoId ===
                song.videoId
        );

    try {
        if (existingIndex >= 0) {
            await window.MUSIC_AUTH.request(
                "/api/music/favorites/remove",
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        videoId: song.videoId
                    })
                }
            );

            favorites.splice(existingIndex, 1);
            showToast("Removed from favorites.");
        } else {
            await window.MUSIC_AUTH.request(
                "/api/music/favorites/add",
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        videoId: song.videoId
                    })
                }
            );

            favorites.unshift(normalizeSong(song));
            showToast("Added to favorites.");
        }

        saveStorage(
            STORAGE_KEYS.favorites,
            favorites
        );

        renderResults();
        renderFavorites();

        if (typeof refreshLikeButtons === "function") {
            refreshLikeButtons();
        }

        if (discoverContainer) {
            const relatedCount =
                discoverResults.filter(
                    song => song?.discoverType === "related"
                ).length;

            let html = "";

            const relatedSongs =
                discoverResults.slice(0, relatedCount);

            const latestSongs =
                discoverResults.slice(relatedCount);

            if (relatedSongs.length) {
                html += `<div class="music-discover-group">
                    <h3>Related Music</h3>
                    <div class="music-discover-grid">
                        ${relatedSongs
                            .map((song, index) =>
                                renderResultCard(song, index)
                            )
                            .join("")}
                    </div>
                </div>`;
            }

            if (latestSongs.length) {
                html += `<div class="music-discover-group">
                    <h3>Latest Music</h3>
                    <div class="music-discover-grid">
                        ${latestSongs
                            .map((song, index) =>
                                renderResultCard(
                                    song,
                                    relatedSongs.length + index
                                )
                            )
                            .join("")}
                    </div>
                </div>`;
            }

            discoverContainer.innerHTML = html;
        }

    } catch (error) {
        console.error(
            "Favorite update error:",
            error
        );

        showToast(
            error.message === "LOGIN_REQUIRED"
                ? "Please login to use favorites."
                : "Unable to update favorite."
        );
    }
}

    function renderFavorites() {

        if (!favoritesContainer) {
            return;
        }

        if (!favorites.length) {

            favoritesContainer.innerHTML =
                `
                    <div class="music-mini-empty">
                        No favorites yet.
                    </div>
                `;

            return;
        }

        favoritesContainer.innerHTML =
            favorites
                .slice(
                    0,
                    20
                )
                .map(
                    song =>
                        renderMiniSong(
                            song,
                            "favorite"
                        )
                )
                .join("");
    }

        const playAllFavoritesButton =
        document.getElementById("musicPlayAllFavorites");

    if (playAllFavoritesButton) {
        playAllFavoritesButton.addEventListener(
            "click",
            () => {
                if (!favorites.length) {
                    showToast("No favorites to play.");
                    return;
                }

                playSong(
    favorites[0],
    0,
    favorites
);
            }
        );
    }


    /* =========================================================
       RECENT
    ========================================================= */

    function addToRecent(
        song
    ) {

        if (!song?.videoId) {
            return;
        }

        recent =
            recent.filter(
                item =>
                    item.videoId !==
                    song.videoId
            );

        recent.unshift(
            normalizeSong(
                song
            )
        );

        recent =
            recent.slice(
                0,
                MAX_RECENT
            );

        saveStorage(
            STORAGE_KEYS.recent,
            recent
        );
    }


    function renderRecent() {

        if (!recentContainer) {
            return;
        }

        if (!recent.length) {

            recentContainer.innerHTML =
                `
                    <div class="music-mini-empty">
                        Nothing played yet.
                    </div>
                `;

            return;
        }

        recentContainer.innerHTML =
            recent
                .slice(
                    0,
                    20
                )
                .map(
                    song =>
                        renderMiniSong(
                            song,
                            "recent"
                        )
                )
                .join("");
    }
    const playAllRecentButton =
    document.getElementById("musicPlayAllRecent");

if (playAllRecentButton) {
    playAllRecentButton.addEventListener(
        "click",
        () => {
            if (!recent.length) {
                showToast("No recently played songs.");
                return;
            }

            playSong(
                recent[0],
                0,
                recent
            );
        }
    );
}


    function renderMiniSong(
        song,
        type
    ) {

        const image =
            getSongImage(
                song
            );

        return `
            <div
                class="music-mini-item"
                data-video-id="${escapeHtml(song.videoId)}"
                data-mini-type="${type}">

                ${
                    image
                        ? `
                            <img
                                class="music-mini-image"
                                src="${escapeHtml(image)}"
                                alt=""
                                loading="lazy">
                          `
                        : `
                            <div
                                class="music-mini-image"
                                style="
                                    display:grid;
                                    place-items:center;
                                    color:#8b5cf6;
                                    font-size:20px;
                                ">
                                ♪
                            </div>
                          `
                }

                <div class="music-mini-info">

                    <div class="music-mini-title">
                        ${escapeHtml(song.title)}
                    </div>

                    <div class="music-mini-channel">
                        ${escapeHtml(song.channelTitle)}
                    </div>

                </div>
                ${
    type === "favorite"
        ? `
            <button
                type="button"
                class="music-playlist-delete"
                data-remove-favorite="${escapeHtml(song.videoId)}"
                aria-label="Remove from favorites">
                ×
            </button>
          `
        : ""
}

            </div>
        `;
    }


    /* =========================================================
       PAGINATION
    ========================================================= */

    function updatePagination() {

        if (!pagination) {
            return;
        }

        const show =
            Boolean(
                nextPageToken ||
                previousPageToken
            );

        pagination.classList.toggle(
            "hidden",
            !show
        );

        if (previousButton) {

            previousButton.disabled =
                !previousPageToken;
        }

        if (nextButton) {

            nextButton.disabled =
                !nextPageToken;
        }
    }


    /* =========================================================
       CLEAR
    ========================================================= */

    function clearSearch() {

        if (searchInput) {
            searchInput.value =
                "";
        }

        results = [];

        nextPageToken = null;

        previousPageToken = null;

        lastSearchQuery = "";

        resultsContainer.innerHTML =
            "";

        resultsInfo.textContent =
            "Search for a song to get started.";

        emptyState.classList.remove(
            "hidden"
        );

        clearButton.classList.add(
            "hidden"
        );
        if (resultsSection) {
    resultsSection.classList.add("hidden");
}

        pagination.classList.add(
            "hidden"
        );
    }


    /* =========================================================
       EVENTS
    ========================================================= */

    function extractYouTubeVideoId(url) {
    try {
        const parsed = new URL(String(url || "").trim());

        if (
            parsed.hostname === "youtube.com" ||
            parsed.hostname === "www.youtube.com" ||
            parsed.hostname === "m.youtube.com"
        ) {
            return parsed.searchParams.get("v") || null;
        }

        if (
            parsed.hostname === "youtu.be" ||
            parsed.hostname === "www.youtu.be"
        ) {
            return parsed.pathname.split("/")[1] || null;
        }

        return null;
    } catch {
        return null;
    }
}
   if (searchForm) {

    searchForm.addEventListener(
        "submit",
        async event => {

            event.preventDefault();

            const query =
                String(searchInput?.value || "").trim();

            if (!query) {
                return;
            }

            const videoId =
                extractYouTubeVideoId(query);

            // YouTube link pasted in search box
            if (videoId) {

                try {

                    searchButton.disabled = true;

                    const response =
                        await fetch(
                            "/api/music/add-youtube",
                            {
                                method: "POST",
                                headers: {
                                    "Content-Type":
                                        "application/json"
                                },
                                body: JSON.stringify({
                                    videoId
                                })
                            }
                        );

                    const data =
                        await response.json();

                    if (
                        !response.ok ||
                        !data.success ||
                        !data.song
                    ) {
                        throw new Error(
                            data.message ||
                            "Unable to add YouTube video."
                        );
                    }

                    const song = {
                        ...data.song,
                        videoId:
                            data.song.videoId ||
                            videoId
                    };

                    // Existing player
                    playSong(
                        song,
                        -1,
                        [song]
                    );

                    // Existing video overlay
                    setTimeout(() => {
                        openMusicVideo(
                            song,
                            0,
                            true
                        );
                    }, 300);

                    if (
                        typeof showToast ===
                        "function"
                    ) {
                        showToast(
                            data.alreadyExists
                                ? "Video already in Align Music."
                                : "YouTube video added to Align Music."
                        );
                    }

                } catch (error) {

                    console.error(
                        "YouTube search error:",
                        error
                    );

                    if (
                        typeof showToast ===
                        "function"
                    ) {
                        showToast(
                            error.message ||
                            "Unable to play YouTube video."
                        );
                    }

                } finally {

                    searchButton.disabled = false;

                }

                return;
            }

            // Normal music search
            searchMusic(query);
        }
    );
}


    if (clearButton) {

        clearButton.addEventListener(
            "click",
            clearSearch
        );
    }
    if (searchInput && searchHistoryPanel) {

    searchInput.addEventListener(
        "focus",
        () => {
            openSearchHistoryPanel();
        }
    );

    searchInput.addEventListener(
        "click",
        event => {
            event.stopPropagation();
            openSearchHistoryPanel();
        }
    );

}
if (searchHistoryList) {

    searchHistoryList.addEventListener(
        "click",
        event => {

            const openButton =
                event.target.closest(
                    "[data-search-history-id]"
                );

            if (openButton) {

                openSavedSearchHistory(
                    openButton.dataset
                        .searchHistoryId
                );

                return;
            }


            const deleteButton =
                event.target.closest(
                    "[data-delete-search-history-id]"
                );

            if (deleteButton) {

                event.stopPropagation();

                deleteSearchHistory(
                    deleteButton.dataset
                        .deleteSearchHistoryId
                );

            }

        }
    );

}
if (clearSearchHistoryButton) {

    clearSearchHistoryButton.addEventListener(
        "click",
        event => {

            event.stopPropagation();

            clearAllSearchHistory();

        }
    );

}
document.addEventListener(
    "click",
    event => {

        if (
            !searchHistoryPanel ||
            searchHistoryPanel.classList.contains(
                "hidden"
            )
        ) {
            return;
        }

        if (
            !event.target.closest(
                ".music-search-history-wrap"
            )
        ) {

            closeSearchHistoryPanel();

        }

    }
);
searchHistory =
    searchHistory
        .map(normalizeSearchHistoryEntry)
        .filter(Boolean)
        .slice(
            0,
            MAX_SEARCH_HISTORY
        );

renderSearchHistory();


    if (previousButton) {

        previousButton.addEventListener(
            "click",
            () => {

                if (
                    previousPageToken
                ) {

                    searchMusic(
                        lastSearchQuery,
                        previousPageToken
                    );
                }
            }
        );
    }


    if (nextButton) {

        nextButton.addEventListener(
            "click",
            () => {

                if (
                    nextPageToken
                ) {

                    searchMusic(
                        lastSearchQuery,
                        nextPageToken
                    );
                }
            }
        );
    }


   if (resultsContainer) {
    resultsContainer.addEventListener(
        "click",
        event => {

            const button =
                event.target.closest(
                    "[data-action]"
                );

            if (!button) {
                return;
            }

            const index =
                Number(
                    button.dataset.index
                );

            const song =
                results[index];

            if (!song) {
                return;
            }

            const action =
                button.dataset.action;

            if (action === "play") {

                playSong(
                    song,
                    index
                );
                clearSearch();
                window.scrollTo({
    top: 0,
    behavior: "smooth"
});

                expandMusicPlayer();

                return;
            }
            if (action === "favorite") {
    toggleFavorite(song);
    return;
}

        }
    );
}

if (discoverContainer) {

    discoverContainer.addEventListener(
        "click",
        event => {

            const button =
                event.target.closest(
                    "[data-action]"
                );

            if (!button) {
                return;
            }

            const index =
                Number(
                    button.dataset.index
                );

            const song =
                discoverResults[index];

            if (!song) {
                return;
            }

            const action =
                button.dataset.action;

            if (action === "play") {

                playSong(
                    song,
                    index,
                    discoverResults
                );

                expandMusicPlayer();

                return;
            }

            if (action === "favorite") {

                toggleFavorite(
                    song
                );

                return;
            }
        }
    );
}

    if (favoritesContainer) {

        favoritesContainer.addEventListener(
            "click",
           async event => {
                const removeButton =
    event.target.closest(
        "[data-remove-favorite]"
    );

if (removeButton) {
    const videoId =
        removeButton.dataset.removeFavorite;

    if (!window.MUSIC_AUTH.isLoggedIn()) {
        window.location.href = "/music-auth/login.html";
        return;
    }

    try {
        await window.MUSIC_AUTH.request(
            "/api/music/favorites/remove",
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    videoId
                })
            }
        );

        favorites =
            favorites.filter(
                song =>
                    song.videoId !== videoId
            );

        saveStorage(
            STORAGE_KEYS.favorites,
            favorites
        );

        renderFavorites();
        renderResults();

        showToast(
            "Removed from favorites."
        );
    } catch (error) {
        console.error(
            "Remove favorite error:",
            error
        );

        showToast(
            "Failed to remove from favorites."
        );
    }

    return;

}

                const item =
                    event.target.closest(
                        ".music-mini-item"
                    );

                if (!item) {
                    return;
                }

                const videoId =
                    item.dataset.videoId;

                const song =
                    favorites.find(
                        favorite =>
                            favorite.videoId ===
                            videoId
                    );

                if (song) {
    const index =
        favorites.findIndex(
            favorite =>
                favorite.videoId ===
                song.videoId
        );

    playSong(
        song,
        index,
        favorites
    );
}
            }
        );
    }


    if (recentContainer) {

        recentContainer.addEventListener(
            "click",
            event => {
                

                const item =
                    event.target.closest(
                        ".music-mini-item"
                    );

                if (!item) {
                    return;
                }

                const videoId =
                    item.dataset.videoId;

                const song =
                    recent.find(
                        recentSong =>
                            recentSong.videoId ===
                            videoId
                    );

                if (song) {
    const index =
        recent.findIndex(
            recentSong =>
                recentSong.videoId ===
                song.videoId
        );

    playSong(
        song,
        index,
        recent
    );
}
            }
        );
    }


    if (playPauseButton) {

        playPauseButton.addEventListener(
            "click",
            togglePlayPause
        );
    }


    if (playerPreviousButton) {

        playerPreviousButton.addEventListener(
            "click",
            playPrevious
        );
    }


    if (playerNextButton) {

        playerNextButton.addEventListener(
            "click",
            playNext
        );
    }
    if (playerVideoButton) {
    playerVideoButton.addEventListener(
        "click",
        () => {

            if (
                !currentSong ||
                !currentSong.videoId
            ) {
                notify("No song is currently playing.");
                return;
            }

            openMusicVideo(
                currentSong,
                0,
                true
            );
        }
    );
}


    if (progress) {

        progress.addEventListener(
            "input",
            event => {

                seekToPercent(
                    event.target.value
                );
            }
        );
    }


    if (volume) {

        volume.addEventListener(
            "input",
            event => {

                setVolume(
                    event.target.value
                );
            }
        );
    }


    if (musicPlayerClose) {
        musicPlayerClose.addEventListener(
            "click",
            () => collapseMusicPlayer(true)
        );
    }

    window.addEventListener(
        "popstate",
        () => {
            if (location.hash !== "#player") {
                collapseMusicPlayer(false);
            }
        }
    );

        /* =========================================================
       INITIALIZE
    ========================================================= */

    renderFavorites();

    renderRecent();

    /*
     * If the YouTube API has already loaded
     * before this script executes.
     */

    if (
        window.YT &&
        window.YT.Player
    ) {

        createYouTubePlayer();
    }


    /* Public bridge for player extras */
        window.AlignMusicPlayer = {

    playSong: (song, index = -1, playbackList = null) =>
        playSong(song, index, playbackList),

    getCurrentSong: () => currentSong

};
window.AlignMusicFavoritesToggle = song =>
    toggleFavorite(song);
window.AlignMusicIsFavorite = videoId =>
    favorites.some(song => song.videoId === videoId);

await loadAccountFavorites();
await loadMusicDiscover();

})();



(async () => {
    "use strict";

   const EXTRA_KEYS = {
    playlists: "alignMusicPlaylists"
};

    const extraLoad = (key, fallback) => {
        try {
            const raw = localStorage.getItem(key);
            return raw ? JSON.parse(raw) : fallback;
        } catch {
            return fallback;
        }
    };

    const extraSave = (key, value) => {
        try {
            localStorage.setItem(key, JSON.stringify(value));
        } catch (error) {
            console.warn(`Unable to save ${key}`, error);
        }
    };

    const extraEscapeHtml = value => String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");

    const extraSong = song => ({
    videoId: song?.videoId || null,
    title: song?.title || "Unknown title",
    artist: song?.artist || null,
    channelTitle: song?.channelTitle || "YouTube",
    duration: song?.duration || null,
    thumbnails: song?.thumbnails || {}
});

    let extraPlaylists = [];
    async function loadAccountPlaylists() {
    if (!window.MUSIC_AUTH.isLoggedIn()) {
        return;
    }

    try {
        const data = await window.MUSIC_AUTH.request(
            "/api/music/playlists"
        );

        extraPlaylists = Array.isArray(data?.playlists)
            ? data.playlists.map(playlist => ({
                id: String(playlist.id),
                name: playlist.name || "Untitled Playlist",
                songs: Array.isArray(playlist.songs)
                    ? playlist.songs.map(extraSong)
                    : []
            }))
            : [];

        renderPlaylists();

    } catch (error) {
        console.error(
            "Music account playlists load error:",
            error
        );
    }
}
    let extraShuffle = false;
    let extraRepeat = false;

    const extraPlayer = document.getElementById("musicPlayer");
    const extraPlay = document.getElementById("musicPlayPause");
    const extraPrev = document.getElementById("musicPrevious");
    const extraNext = document.getElementById("musicNext");
    const extraVolume = document.getElementById("musicVolume");
    const extraToast = document.getElementById("musicToast");

    // Keep the feature state accessible to the existing music.js where possible.
    window.AlignMusicExtras = {
        get shuffle() { return extraShuffle; },
        get repeat() { return extraRepeat; },
        get likes() {
    return JSON.parse(
        localStorage.getItem("alignMusicFavorites") || "[]"
    );
},
        get playlists() { return extraPlaylists; },
        saveMusicSong,
        
        toggleLike(song) {
    if (!song?.videoId) return;

    if (
        typeof window.AlignMusicFavoritesToggle ===
        "function"
    ) {
        window.AlignMusicFavoritesToggle(song);
    }
}
    };

    function notify(message) {
        if (!extraToast) return;
        extraToast.textContent = message;
        extraToast.classList.add("show");
        clearTimeout(notify.timer);
        notify.timer = setTimeout(() => extraToast.classList.remove("show"), 2200);
    }

    function current() {
        return window.AlignMusicPlayer?.getCurrentSong?.() || null;
    }
    async function saveMusicSong(song) {
    if (!song?.videoId) {
        return;
    }

    try {
       
        await fetch("/api/music/save", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                videoId: song.videoId,
                title: song.title || "Unknown title",
                artist: song.artist || null,
                channelTitle: song.channelTitle || null,
                thumbnailUrl:
                                song.thumbnails?.high ||
                                song.thumbnails?.medium ||
                                song.thumbnails?.default ||
                                          null,
                duration: song.duration || null,
                language: song.language || null
            })
        });
    }
    catch (error) {
        console.error(
            "Music save error:",
            error
        );
    }
}

    function playSong(song, index = -1, playbackList = null) {
    if (!song?.videoId) return;

    if (typeof window.AlignMusicPlayer?.playSong === "function") {
        window.AlignMusicPlayer.playSong(
            song,
            index,
            playbackList
        );
        return;
    }
        // Fallback: click matching result card if the main script exposes no API.
        const button = [...document.querySelectorAll('[data-action="play"]')]
    .find(el => el.dataset.index === String(song.index));

if (button) button.click();
    }

    function refreshLikeButtons() {
    document
        .querySelectorAll("#musicLike")
        .forEach(button => {

            const song =
                current();

            if (!song?.videoId) {
                button.textContent = "♡";
                button.classList.remove(
                    "active"
                );
                return;
            }

            const favorites =
                JSON.parse(
                    localStorage.getItem(
                        "alignMusicFavorites"
                    ) || "[]"
                );

            const liked =
                favorites.some(
                    item =>
                        item.videoId ===
                        song.videoId
                );

            button.textContent =
                liked ? "♥" : "♡";

            button.classList.toggle(
                "active",
                liked
            );
        });
}


   function renderPlaylists() {
    const container =
        document.getElementById("musicPlaylists");

    if (!container) {
        return;
    }

    if (!extraPlaylists.length) {
        container.innerHTML = `
            <div class="music-mini-empty">
                No playlists yet.
            </div>
        `;
        return;
    }

    container.innerHTML =
    extraPlaylists.map(playlist => `
        <div
            class="music-mini-item music-playlist-item"
            data-playlist-id="${playlist.id}"
        >
            <button
                type="button"
                class="music-playlist-open"
                data-playlist-id="${playlist.id}"
            >
                <div class="music-mini-art music-playlist-art">
                    🎵
                </div>

                <div class="music-mini-content">
                    <div class="music-mini-title">
                        ${extraEscapeHtml(playlist.name)}
                    </div>

                    <div class="music-mini-meta">
                        ${playlist.songs.length}
                        ${playlist.songs.length === 1 ? "song" : "songs"}
                    </div>
                </div>
            </button>

            <div class="music-playlist-actions">

    <button
        type="button"
        class="music-playlist-delete"
        data-playlist-rename-id="${playlist.id}"
        aria-label="Rename playlist"
        title="Rename playlist">
        ✎
    </button>

    <button
        type="button"
        class="music-playlist-delete"
        data-playlist-delete-id="${playlist.id}"
        aria-label="Delete playlist"
        title="Delete playlist">
        ×
    </button>

</div>
        </div>
    `).join("");
}
function showPlaylistSongs(playlist) {
    if (!playlist) {
        return;
    }

    const existing =
        document.getElementById("musicPlaylistSongsModal");

    if (existing) {
        existing.remove();
    }

    const overlay = document.createElement("div");

    overlay.id = "musicPlaylistSongsModal";
    overlay.className = "music-playlist-songs-overlay";

    overlay.innerHTML = `
        <div class="music-standalone-modal-box">

            <div class="music-standalone-modal-header">
    <div>
        <h3>${extraEscapeHtml(playlist.name)}</h3>
        <p>
            ${playlist.songs.length}
            ${playlist.songs.length === 1 ? "song" : "songs"}
        </p>
    </div>

    <div style="
        display:flex;
        align-items:center;
        gap:8px;
    ">
        ${
            playlist.songs.length
                ? `
                    <button
                        type="button"
                        id="musicPlaylistPlayAll"
                        class="music-play-all-button">
                        ▶ Play All
                    </button>
                `
                : ""
        }

        <button
            type="button"
            class="music-standalone-modal-close"
            id="musicPlaylistSongsClose">
            ×
        </button>
    </div>
</div>

            <div class="music-playlist-view-list">
    ${
        playlist.songs.length
            ? playlist.songs.map((song, index) => `
                <div class="music-playlist-song-row">

    <button
        type="button"
        class="music-playlist-view-song"
        data-playlist-song-index="${index}">

        <div class="music-playlist-view-number">
            ${index + 1}
        </div>

        <div class="music-playlist-view-info">
            <strong>
                ${extraEscapeHtml(song.title)}
            </strong>

            <small>
                ${extraEscapeHtml(
                    song.artist || song.channelTitle || "Unknown artist"
                )}
            </small>
        </div>
    </button>

    <button
        type="button"
        class="music-playlist-song-delete"
        data-playlist-delete-index="${index}"
        aria-label="Remove song">
        ×
    </button>

</div>
            `).join("")
            : `
                <div class="music-playlist-modal-empty">
                    This playlist is empty.
                </div>
            `
    }
</div>
        </div>
    `;

    document.body.appendChild(overlay);

    const closeButton =
        document.getElementById("musicPlaylistSongsClose");

    if (closeButton) {
        closeButton.addEventListener("click", () => {
            overlay.remove();
        });
    }
    const playAllButton =
    document.getElementById("musicPlaylistPlayAll");

if (playAllButton) {
    playAllButton.addEventListener("click", () => {
        if (!playlist.songs.length) {
            notify("This playlist is empty.");
            return;
        }

        playSong(
            playlist.songs[0],
            0,
            playlist.songs
        );

        overlay.remove();

        window.scrollTo({
            top: 0,
            behavior: "smooth"
        });
    });
}
overlay
    .querySelectorAll("[data-playlist-delete-index]")
    .forEach(button => {
        button.addEventListener("click", async event => {
            event.stopPropagation();

            if (!window.MUSIC_AUTH.isLoggedIn()) {
                window.location.href = "/music-auth/login.html";
                return;
            }

            const index = Number(
                button.dataset.playlistDeleteIndex
            );

            if (
                !Number.isInteger(index) ||
                !playlist.songs[index]
            ) {
                return;
            }

            const song = playlist.songs[index];

            try {
                await window.MUSIC_AUTH.request(
                    "/api/music/playlists/songs/remove",
                    {
                        method: "POST",
                        headers: {
                            "Content-Type": "application/json"
                        },
                        body: JSON.stringify({
                            playlistId: playlist.id,
                            videoId: song.videoId
                        })
                    }
                );

                playlist.songs.splice(index, 1);

                renderPlaylists();

                notify("Song removed from playlist.");

                overlay.remove();
                showPlaylistSongs(playlist);

            } catch (error) {
                console.error(
                    "Remove song from playlist error:",
                    error
                );

                notify(
                    error.message === "LOGIN_REQUIRED"
                        ? "Please login to use playlists."
                        : error.message ||
                          "Unable to remove song from playlist."
                );
            }
        });
    });

    overlay
        .querySelectorAll("[data-playlist-song-index]")
        .forEach(button => {
            button.addEventListener("click", () => {
                const index =
                    Number(
                        button.dataset.playlistSongIndex
                    );

                playSong(
                    playlist.songs[index],
                    index,
                    playlist.songs
                );

                overlay.remove();

                window.scrollTo({
                    top: 0,
                    behavior: "smooth"
                });
            });
        });
}
function openMusicPlaylistModal(song) {
    if (!song) {
        return;
    }

    const existing =
        document.getElementById("musicPlaylistStandaloneModal");

    if (existing) {
        existing.remove();
    }

    const playlistOptions = extraPlaylists.length
        ? extraPlaylists.map(playlist => `
            <button
                type="button"
                class="music-playlist-modal-item"
                data-standalone-playlist-id="${playlist.id}">
                <div class="music-playlist-modal-icon">🎵</div>

                <div class="music-playlist-modal-info">
                    <strong>${extraEscapeHtml(playlist.name)}</strong>
                    <small>
                        ${playlist.songs.length}
                        ${playlist.songs.length === 1 ? "song" : "songs"}
                    </small>
                </div>

                <span class="music-playlist-modal-arrow">›</span>
            </button>
        `).join("")
        : `
            <div class="music-playlist-modal-empty">
                No playlists yet.
            </div>
        `;

    const overlay = document.createElement("div");

    overlay.id = "musicPlaylistStandaloneModal";

    overlay.innerHTML = `
        <div class="music-standalone-modal-box">

            <div class="music-standalone-modal-header">
                <div>
                    <h3>Add to Playlist</h3>
                    <p>${extraEscapeHtml(song.title)}</p>
                </div>

                <button
                    type="button"
                    class="music-standalone-modal-close"
                    id="musicPlaylistModalClose">
                    ×
                </button>
            </div>

            <div class="music-standalone-playlist-list">
                ${playlistOptions}
            </div>

            <button
                type="button"
                id="musicStandaloneNewPlaylist"
                class="music-standalone-new-playlist">
                ＋ New Playlist
            </button>

        </div>
    `;

    document.body.appendChild(overlay);
    const closeButton =
    document.getElementById("musicPlaylistModalClose");

if (closeButton) {
    closeButton.addEventListener("click", () => {
        overlay.remove();
    });
}
overlay.querySelectorAll("[data-standalone-playlist-id]").forEach(button => {
    button.addEventListener("click", async () => {
        if (!window.MUSIC_AUTH.isLoggedIn()) {
            window.location.href = "/music-auth/login.html";
            return;
        }

        const playlist = extraPlaylists.find(
            item =>
                String(item.id) ===
                String(button.dataset.standalonePlaylistId)
        );

        if (!playlist) {
            return;
        }

        if (
            playlist.songs.some(
                item => item.videoId === song.videoId
            )
        ) {
            notify("Song is already in this playlist.");
            return;
        }

        try {
            await window.MUSIC_AUTH.request(
                "/api/music/playlists/songs/add",
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        playlistId: playlist.id,
                        videoId: song.videoId
                    })
                }
            );

            playlist.songs.push(extraSong(song));

            renderPlaylists();

            notify(`Added to ${playlist.name}.`);

            overlay.remove();

        } catch (error) {
            console.error(
                "Add song to playlist error:",
                error
            );

            notify(
                error.message === "LOGIN_REQUIRED"
                    ? "Please login to use playlists."
                    : error.message ||
                      "Unable to add song to playlist."
            );
        }
    });
});
const newPlaylistButton =
    document.getElementById("musicStandaloneNewPlaylist");

if (newPlaylistButton) {
    newPlaylistButton.addEventListener("click", () => {
        overlay.style.display = "none";

        openMusicCreatePlaylistModal(song);
    });
}
}
function openMusicCreatePlaylistModal(song) {
    if (!song) {
        return;
    }

    const existing =
        document.getElementById("musicCreatePlaylistStandaloneModal");

    if (existing) {
        existing.remove();
    }

    const overlay = document.createElement("div");

    overlay.id = "musicCreatePlaylistStandaloneModal";

    overlay.innerHTML = `
        <div class="music-standalone-modal-box">

            <div class="music-standalone-modal-header">
                <div>
                    <h3>New Playlist</h3>
                    <p>Create a playlist for your music</p>
                </div>

                <button
                    type="button"
                    class="music-standalone-modal-close"
                    id="musicCreatePlaylistModalClose">
                    ×
                </button>
            </div>

            <input
                id="musicStandalonePlaylistName"
                type="text"
                maxlength="60"
                placeholder="Enter playlist name"
                autocomplete="off"
                style="
                    width:100%;
                    box-sizing:border-box;
                    padding:12px 14px;
                    border:1px solid #cbd5e1;
                    border-radius:12px;
                    outline:none;
                    font-size:14px;
                ">

            <div style="
                display:flex;
                justify-content:flex-end;
                gap:10px;
                margin-top:16px;
            ">
                <button
                    type="button"
                    id="musicCreatePlaylistCancel"
                    class="music-secondary-button">
                    Cancel
                </button>

                <button
                    type="button"
                    id="musicCreatePlaylistConfirm"
                    class="music-primary-button">
                    Create Playlist
                </button>
            </div>

        </div>
    `;

    document.body.appendChild(overlay);
    overlay.style.position = "fixed";
overlay.style.inset = "0";
overlay.style.zIndex = "100000";
overlay.style.display = "flex";
overlay.style.alignItems = "center";
overlay.style.justifyContent = "center";
overlay.style.padding = "20px";
overlay.style.boxSizing = "border-box";
    const input =
    document.getElementById("musicStandalonePlaylistName");

const closeButton =
    document.getElementById("musicCreatePlaylistModalClose");

const cancelButton =
    document.getElementById("musicCreatePlaylistCancel");

const confirmButton =
    document.getElementById("musicCreatePlaylistConfirm");

if (input) {
    input.focus();
}

if (closeButton) {
    closeButton.addEventListener("click", () => {
        overlay.remove();
    });
}

if (cancelButton) {
    cancelButton.addEventListener("click", () => {
        overlay.remove();
    });
}
if (confirmButton) {
    confirmButton.addEventListener("click", async () => {
        if (!window.MUSIC_AUTH.isLoggedIn()) {
            window.location.href = "/music-auth/login.html";
            return;
        }

        const name = input?.value.trim();

        if (!name) {
            notify("Enter a playlist name.");
            return;
        }

        const exists = extraPlaylists.some(
            playlist =>
                playlist.name.toLowerCase() ===
                name.toLowerCase()
        );

        if (exists) {
            notify("Playlist already exists.");
            return;
        }

        try {
            const data = await window.MUSIC_AUTH.request(
                "/api/music/playlists/create",
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        name
                    })
                }
            );

            const createdPlaylist = data?.playlist;

            if (!createdPlaylist?.id) {
                throw new Error(
                    "Playlist was not created."
                );
            }

            const playlist = {
                id: String(createdPlaylist.id),
                name:
                    createdPlaylist.name ||
                    name,
                songs: []
            };

            extraPlaylists.push(playlist);

            if (song?.videoId) {
                await window.MUSIC_AUTH.request(
                    "/api/music/playlists/songs/add",
                    {
                        method: "POST",
                        headers: {
                            "Content-Type": "application/json"
                        },
                        body: JSON.stringify({
                            playlistId: playlist.id,
                            videoId: song.videoId
                        })
                    }
                );

                playlist.songs.push(
                    extraSong(song)
                );
            }

            renderPlaylists();

            notify(`Created ${playlist.name}.`);

            overlay.remove();

        } catch (error) {
            console.error(
                "Playlist creation error:",
                error
            );

            notify(
                error.message === "LOGIN_REQUIRED"
                    ? "Please login to use playlists."
                    : error.message ||
                      "Unable to create playlist."
            );
        }
    });
}
}
    function addPlayerActionButtons() {
        if (!extraPlayer) return;
        let actions = document.getElementById("musicExtraActions");
        if (actions) return;

        actions = document.createElement("div");
        actions.id = "musicExtraActions";
        actions.className = "music-extra-actions";
        actions.innerHTML = `
            <button
    type="button"
    id="musicShuffle"
    class="music-player-action"
    aria-label="Shuffle"
    title="Shuffle">
    <span class="music-action-icon">⤨</span>
    
</button>

<button
    type="button"
    id="musicRepeat"
    class="music-player-action"
    aria-label="Repeat"
    title="Repeat">
    <span class="music-action-icon">↻</span>
   
</button>

<button
    type="button"
    id="musicLike"
    class="music-player-action"
    aria-label="Like"
    title="Like">
    <span class="music-action-icon">♡</span>
    
</button>

<button
    type="button"
    id="musicPlaylist"
    class="music-player-action"
    aria-label="Add to playlist"
    title="Add to playlist">
    <span class="music-action-icon">＋</span>
    <span>Playlist</span>
</button>
        `;
        const controls = extraPlayer.querySelector(".music-player-controls");
        if (controls) controls.parentNode.insertBefore(actions, controls);

        actions.querySelector("#musicShuffle").addEventListener("click", () => {
            extraShuffle = !extraShuffle;
            actions.querySelector("#musicShuffle").classList.toggle("active", extraShuffle);
            notify(extraShuffle ? "Shuffle on." : "Shuffle off.");
        });

        actions.querySelector("#musicRepeat").addEventListener("click", () => {
            extraRepeat = !extraRepeat;
            actions.querySelector("#musicRepeat").classList.toggle("active", extraRepeat);
            notify(extraRepeat ? "Repeat on." : "Repeat off.");
        });
        
       actions.querySelector("#musicLike").addEventListener("click", async () => {
if (!window.MUSIC_AUTH.isLoggedIn()) {
    window.location.href = "/music-auth/login.html";
    return;
}
    const song = current();

    if (!song) {
        return;
    }

    await window.AlignMusicFavoritesToggle(song);

    const likeButton = actions.querySelector("#musicLike");
    const icon = likeButton.querySelector(".music-action-icon");

    if (icon) {
        const liked = window.AlignMusicIsFavorite
            ? window.AlignMusicIsFavorite(song.videoId)
            : false;

        icon.textContent = liked ? "♥" : "♡";
        likeButton.classList.toggle("active", liked);
    }
});

       actions.querySelector("#musicPlaylist").addEventListener("click", () => {
    if (!window.MUSIC_AUTH.isLoggedIn()) {
        window.location.href = "/music-auth/login.html";
        return;
    }
        const song = current();

    if (!song) {
        notify("Choose a song first.");
        return;
    }

    openMusicPlaylistModal(song);
});
    }
    const playlistsContainer =
    document.getElementById("musicPlaylists");
    

if (playlistsContainer) {
    playlistsContainer.addEventListener(
        "click",
        event => {
const renameButton =
    event.target.closest("[data-playlist-rename-id]");

if (renameButton) {
    event.stopPropagation();

    const playlistId =
        renameButton.dataset.playlistRenameId;

    const playlist =
        extraPlaylists.find(
            item => item.id === playlistId
        );

    if (!playlist) {
        return;
    }

    const existingModal =
        document.getElementById(
            "musicRenamePlaylistModal"
        );

    if (existingModal) {
        existingModal.remove();
    }

    const modal =
        document.createElement("div");

    modal.id =
        "musicRenamePlaylistModal";

    modal.className =
        "music-playlist-songs-overlay";

    modal.innerHTML = `
        <div class="music-standalone-modal-box">

            <div class="music-standalone-modal-header">
                <div>
                    <h3>Rename Playlist</h3>

                    <p>
                        Enter a new name for
                        <strong>
                            ${extraEscapeHtml(playlist.name)}
                        </strong>.
                    </p>
                </div>

                <button
                    type="button"
                    class="music-standalone-modal-close"
                    id="musicRenamePlaylistClose">
                    ×
                </button>
            </div>

            <div style="margin-top:20px;">

                <input
                    type="text"
                    id="musicRenamePlaylistInput"
                    value="${extraEscapeHtml(playlist.name)}"
                    maxlength="100"
                    style="
                        width:100%;
                        box-sizing:border-box;
                    "
                >

            </div>

            <div style="
                display:flex;
                justify-content:flex-end;
                gap:10px;
                margin-top:20px;
            ">

                <button
                    type="button"
                    id="musicRenamePlaylistCancel"
                    class="music-secondary-button">
                    Cancel
                </button>

                <button
                    type="button"
                    id="musicRenamePlaylistConfirm"
                    class="music-primary-button">
                    Rename
                </button>

            </div>

        </div>
    `;

    document.body.appendChild(modal);

    const input =
        document.getElementById(
            "musicRenamePlaylistInput"
        );

    const closeModal = () => {
        modal.remove();
    };

    document
        .getElementById(
            "musicRenamePlaylistClose"
        )
        ?.addEventListener(
            "click",
            closeModal
        );

    document
        .getElementById(
            "musicRenamePlaylistCancel"
        )
        ?.addEventListener(
            "click",
            closeModal
        );

    document
        .getElementById(
            "musicRenamePlaylistConfirm"
        )
        ?.addEventListener(
    "click",
    async () => {
        if (!window.MUSIC_AUTH.isLoggedIn()) {
            window.location.href = "/music-auth/login.html";
            return;
        }

        const newName =
            input.value.trim();

        if (!newName) {
            notify(
                "Playlist name cannot be empty."
            );
            input.focus();
            return;
        }

        if (
            newName.toLowerCase() !==
            playlist.name.toLowerCase() &&
            extraPlaylists.some(
                item =>
                    item.id !== playlist.id &&
                    item.name.toLowerCase() ===
                        newName.toLowerCase()
            )
        ) {
            notify("Playlist already exists.");
            return;
        }

        try {
            await window.MUSIC_AUTH.request(
                "/api/music/playlists/rename",
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        playlistId: playlist.id,
                        name: newName
                    })
                }
            );

            playlist.name = newName;

            renderPlaylists();
            closeModal();

            notify(
                `Renamed playlist to ${newName}.`
            );

        } catch (error) {
            console.error(
                "Playlist rename error:",
                error
            );

            notify(
                error.message === "LOGIN_REQUIRED"
                    ? "Please login to use playlists."
                    : error.message ||
                      "Unable to rename playlist."
            );
        }
    }
);

    input?.focus();
    input?.select();

    return;
}

    const deleteButton =
    event.target.closest("[data-playlist-delete-id]");

if (deleteButton) {
    event.stopPropagation();

    const playlistId =
        deleteButton.dataset.playlistDeleteId;

    const playlist =
        extraPlaylists.find(
            item => item.id === playlistId
        );

    if (!playlist) {
        return;
    }

    const existingModal =
        document.getElementById(
            "musicDeletePlaylistModal"
        );

    if (existingModal) {
        existingModal.remove();
    }

    const modal =
        document.createElement("div");

    modal.id =
        "musicDeletePlaylistModal";

    modal.className =
        "music-playlist-songs-overlay";

    modal.innerHTML = `
        <div class="music-standalone-modal-box">

            <div class="music-standalone-modal-header">
                <div>
                    <h3>Delete Playlist</h3>

                    <p>
                        Are you sure you want to delete
                        <strong>
                            ${extraEscapeHtml(playlist.name)}
                        </strong>?
                    </p>
                </div>

                <button
                    type="button"
                    class="music-standalone-modal-close"
                    id="musicDeletePlaylistClose">
                    ×
                </button>
            </div>

            <div style="
                display:flex;
                justify-content:flex-end;
                gap:10px;
                margin-top:20px;
            ">

                <button
                    type="button"
                    id="musicDeletePlaylistCancel"
                    class="music-secondary-button">
                    Cancel
                </button>

                <button
                    type="button"
                    id="musicDeletePlaylistConfirm"
                    class="music-primary-button"
                    style="
                        background:#dc2626;
                        border-color:#dc2626;
                    ">
                    Delete
                </button>

            </div>

        </div>
    `;
    

    document.body.appendChild(modal);

    const closeModal = () => {
        modal.remove();
    };

    document
        .getElementById(
            "musicDeletePlaylistClose"
        )
        ?.addEventListener(
            "click",
            closeModal
        );

    document
        .getElementById(
            "musicDeletePlaylistCancel"
        )
        ?.addEventListener(
            "click",
            closeModal
        );

    document
        .getElementById(
            "musicDeletePlaylistConfirm"
        )
        ?.addEventListener(
    "click",
    async () => {
        if (!window.MUSIC_AUTH.isLoggedIn()) {
            window.location.href = "/music-auth/login.html";
            return;
        }

        try {
            await window.MUSIC_AUTH.request(
                "/api/music/playlists/delete",
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        playlistId: playlistId
                    })
                }
            );

            const index =
                extraPlaylists.findIndex(
                    item =>
                        String(item.id) ===
                        String(playlistId)
                );

            if (index !== -1) {
                extraPlaylists.splice(index, 1);
            }

            renderPlaylists();
            closeModal();

            notify(
                `Deleted ${playlist.name}.`
            );

        } catch (error) {
            console.error(
                "Playlist delete error:",
                error
            );

            notify(
                error.message === "LOGIN_REQUIRED"
                    ? "Please login to use playlists."
                    : error.message ||
                      "Unable to delete playlist."
            );
        }
    }
);

    return;
}
            const item =
                event.target.closest(
                    "[data-playlist-id]"
                );

            if (!item) {
                return;
            }

            const playlist =
                extraPlaylists.find(
                    p =>
                        p.id ===
                        item.dataset.playlistId
                );

            if (!playlist) {
                return;
            }

            showPlaylistSongs(playlist);
        }
    );
}


loadAccountPlaylists();
addPlayerActionButtons();

})();

