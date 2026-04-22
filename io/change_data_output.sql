-- UPDATE ROW --
UPDATE animals
SET animal_name='Perry the Platypus',animal_pedality='biped'
WHERE animal_name = 'Platypus';

-- DELETE ROW --
DELETE FROM animals WHERE animal_name = 'penguin';

--INSERT ROW --
INSERT INTO animals(animal_name,habitat,land_type)
VALUES ('capybara','forests','land');


