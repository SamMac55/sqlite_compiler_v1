CREATE TABLE artists (
    artist_id INTEGER PRIMARY KEY,
    artist_name TEXT NOT NULL
);

CREATE TABLE albums (
    album_id INTEGER PRIMARY KEY,
    album_name TEXT NOT NULL,
    artist_id INTEGER NOT NULL REFERENCES artists(artist_id)
);

CREATE TABLE songs (
    song_id INTEGER PRIMARY KEY,
    song_title TEXT NOT NULL,
    duration INTEGER NOT NULL,
    album_id INTEGER NOT NULL REFERENCES albums(album_id)
);

CREATE TABLE users (
    user_id INTEGER PRIMARY KEY,
    username TEXT NOT NULL
);

CREATE TABLE playlists (
    playlist_id INTEGER PRIMARY KEY,
    playlist_name TEXT NOT NULL,
    user_id INTEGER NOT NULL REFERENCES users(user_id)
);

CREATE TABLE playlist_songs (
    playlist_song_id INTEGER PRIMARY KEY,
    playlist_id INTEGER NOT NULL REFERENCES playlists(playlist_id),
    song_id INTEGER NOT NULL REFERENCES songs(song_id)
);

-- Artists
INSERT INTO artists (artist_id, artist_name) VALUES
(1, 'The Echoes'),
(2, 'Neon Nights'),
(3, 'Acoustic Souls');

-- Albums
INSERT INTO albums (album_id, album_name, artist_id) VALUES
(1, 'First Light', 1),
(2, 'Midnight Dreams', 2),
(3, 'Unplugged', 3);

-- Songs
INSERT INTO songs (song_id, song_title, duration, album_id) VALUES
(1, 'Rising Sun', 210, 1),
(2, 'Shadows', 180, 1),
(3, 'City Lights', 200, 2),
(4, 'Moonwalk', 240, 2),
(5, 'Silent Strings', 190, 3);

-- Users
INSERT INTO users (user_id, username) VALUES
(1, 'alice123'),
(2, 'bob_the_listener');

-- Playlists
INSERT INTO playlists (playlist_id, playlist_name, user_id) VALUES
(1, 'Favorites', 1),
(2, 'Chill Vibes', 2);

-- Playlist-Song assignments
INSERT INTO playlist_songs (playlist_song_id, playlist_id, song_id) VALUES
(1, 1, 1),
(2, 1, 3),
(3, 1, 5),
(4, 2, 2),
(5, 2, 4);