CREATE TABLE song_ratings (
	rating_id INTEGER PRIMARY KEY,
 	user_id INTEGER NOT NULL REFERENCES users(user_id),
 	song_id INTEGER NOT NULL REFERENCES songs(song_id),
 	rating REAL NOT NULL,
 	review TEXT);
INSERT INTO song_ratings (rating_id, user_id, song_id, rating, review) VALUES (1, 1, 2, 5, 'Amazing!');
INSERT INTO song_ratings (rating_id, user_id, song_id, rating, review) VALUES (2, 2, 3, 2.5, 'Not my favorite!');
INSERT INTO song_ratings (rating_id, user_id, song_id, rating) VALUES (3, 1, 1, 4);
INSERT INTO song_ratings (rating_id, user_id, song_id, rating) VALUES (4, 2, 4, 3.4);

SELECT song_ratings.* FROM song_ratings;
SELECT song_ratings.* FROM song_ratings WHERE rating = 2.5;
SELECT song_ratings.* FROM song_ratings WHERE user_id < song_id;
UPDATE song_ratings SET rating = 4, review = 'Great song!' WHERE rating_id = 4;
UPDATE song_ratings SET rating = song_id WHERE rating_id = 3;
SELECT song_ratings.* FROM song_ratings;
DELETE FROM song_ratings WHERE user_id = 2 AND song_id >= 4;
DELETE FROM song_ratings WHERE user_id = 1 OR song_id <= 2;
SELECT song_ratings.* FROM song_ratings;
DROP TABLE song_ratings;
