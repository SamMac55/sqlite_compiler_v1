-- SELECT STMT --
SELECT *
FROM songs;

-- SELECT STMT --
SELECT song_id, name
FROM songs;

-- SELECT STMT --
SELECT *
FROM songs
WHERE song_id = 50;

-- SELECT STMT --
SELECT *
FROM songs
ORDER BY duration
LIMIT 10;

-- SELECT STMT --
SELECT songs.name, artists.name
FROM songs
JOIN artists ON songs.artist_id = artists.artist_id
LIMIT 3;

-- SELECT STMT --
SELECT *
FROM artists
ORDER BY artist_name, artist_country
LIMIT 5;

-- SELECT STMT --
SELECT *
FROM artists
GROUP BY artist_country
HAVING artist_country = 'US' OR artist_country != 'Panama';


