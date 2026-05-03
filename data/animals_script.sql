CREATE TABLE animal_types (
    type_id INTEGER PRIMARY KEY,
    type_name TEXT NOT NULL
);

CREATE TABLE habitats (
    habitat_id INTEGER PRIMARY KEY,
    habitat_name TEXT NOT NULL
);

CREATE TABLE animals (
    animal_id INTEGER PRIMARY KEY,
    animal_name TEXT NOT NULL,
    type_id INTEGER NOT NULL REFERENCES animal_types(type_id),
    habitat_id INTEGER NOT NULL REFERENCES habitats(habitat_id),
    age INTEGER NOT NULL
);

CREATE TABLE caretakers (
    caretaker_id INTEGER PRIMARY KEY,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL
);

CREATE TABLE animal_caretakers (
    assignment_id INTEGER PRIMARY KEY,
    animal_id INTEGER NOT NULL REFERENCES animals(animal_id),
    caretaker_id INTEGER NOT NULL REFERENCES caretakers(caretaker_id)
);

-- Animal Types
INSERT INTO animal_types (type_id, type_name) VALUES
(1, 'Mammal'),
(2, 'Bird'),
(3, 'Fish'),
(4, 'Reptile');

-- Habitats
INSERT INTO habitats (habitat_id, habitat_name) VALUES
(1, 'Savannah'),
(2, 'Jungle'),
(3, 'Ocean'),
(4, 'Desert');

--  Animals
INSERT INTO animals (animal_id, animal_name, type_id, habitat_id, age) VALUES
(1, 'Lion', 1, 1, 8),
(2, 'Elephant', 1, 1, 25),
(3, 'Eagle', 2, 2, 5),
(4, 'Shark', 3, 3, 12),
(5, 'Snake', 4, 4, 4);

--  Caretakers
INSERT INTO caretakers (caretaker_id, first_name, last_name) VALUES
(1, 'John', 'Miller'),
(2, 'Sarah', 'Wilson'),
(3, 'David', 'Clark');

-- Animal-Caretaker assignments
INSERT INTO animal_caretakers (assignment_id, animal_id, caretaker_id) VALUES
(1, 1, 1),
(2, 2, 1),
(3, 3, 2),
(4, 4, 3),
(5, 5, 2);