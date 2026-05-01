CREATE TABLE animal_diet (
	diet_id INTEGER PRIMARY KEY NOT NULL,
 	diet_type TEXT NOT NULL,
 	num_feed_days INTEGER);
INSERT INTO animal_diet (diet_id, diet_type) VALUES (1, 'vegetarian');
INSERT INTO animal_diet (diet_id, diet_type, num_feed_days) VALUES (2, 'omnivore', 3);
INSERT INTO animal_diet (diet_id, diet_type, num_feed_days) VALUES (3, 'carnivore', 5);

SELECT animal_diet.* FROM animal_diet WHERE num_feed_days IS NULL;
SELECT animal_diet.* FROM animal_diet WHERE num_feed_days IS NOT NULL;
UPDATE animal_diet SET num_feed_days = 4 WHERE diet_type = 'vegetarian';
SELECT animal_diet.* FROM animal_diet WHERE diet_id = 1;
DROP TABLE animal_diet;
