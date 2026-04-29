CREATE TABLE animal_diet (
	diet_id INTEGER PRIMARY KEY,
 	diet_type TEXT NOT NULL,
 	num_feed_days INTEGER);
INSERT INTO animal_diet (diet_id, diet_type) VALUES (1, 'vegetarian');
INSERT INTO animal_diet (diet_id, diet_type, num_feed_days) VALUES (2, 'omnivore', 3);
INSERT INTO animal_diet (diet_id, diet_type, num_feed_days) VALUES (3, 'carnivore', 5);

SELECT * FROM animal_diet;
UPDATE animal_diet SET num_feed_days = 4 WHERE diet_type = 'vegetarian';
SELECT * FROM animal_diet WHERE diet_id = 1;
DROP TABLE animal_diet;
