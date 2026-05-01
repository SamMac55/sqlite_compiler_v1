SELECT animals.animal_name, animals.age FROM animals ORDER BY animal_name ASC;
SELECT animals.animal_name, animals.age FROM animals ORDER BY animal_name ASC;
SELECT animals.* FROM animals WHERE type_id = 1 AND age < 10;
SELECT animals.* FROM animals WHERE type_id >= 2 OR animal_name = 'Lion';
SELECT animals.animal_name, animal_caretakers.assignment_id, caretakers.first_name, caretakers.last_name FROM animals JOIN animal_caretakers ON animal_caretakers.animal_id = animals.animal_id JOIN caretakers ON caretakers.caretaker_id = animal_caretakers.caretaker_id ;
SELECT animals.habitat_id, COUNT(animals.animal_id) FROM animals GROUP BY habitat_id;
