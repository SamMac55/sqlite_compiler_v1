SELECT songs.* FROM songs ORDER BY song_title, duration ASC LIMIT 3;
SELECT playlist_songs.playlist_id, COUNT(playlist_songs.song_id) FROM playlist_songs GROUP BY playlist_id HAVING playlist_id = 1;
SELECT SUM(songs.duration), albums.album_name FROM songs JOIN albums ON albums.album_id = songs.album_id  GROUP BY album_name ORDER BY SUM(duration) DESC LIMIT 2;
SELECT playlists.playlist_name, songs.song_title, songs.duration FROM playlist_songs JOIN playlists ON playlists.playlist_id = playlist_songs.playlist_id JOIN songs ON songs.song_id = playlist_songs.song_id  WHERE playlists.playlist_id != 2 AND songs.song_id > 2 OR duration >= 190;
SELECT SUM(songs.duration), albums.album_name FROM songs JOIN albums ON albums.album_id = songs.album_id  WHERE duration <= 250 GROUP BY album_name HAVING album_name != 'First Light' ORDER BY SUM(duration) DESC LIMIT 3;
