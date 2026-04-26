CREATE TABLE test_table (
	id INTEGER PRIMARY KEY,
 	name TEXT,
 	value REAL);

CREATE TABLE simple (
	num INTEGER,
 	text TEXT);

CREATE TABLE fk_example (
	department_id INTEGER REFERENCES departments(department_id),
 	note TEXT);

SELECT * FROM test_table;

SELECT simple.num, simple.text FROM simple;

SELECT fk_example.note, departments.department_id FROM fk_example JOIN departments ON departments.department_id = fk_example.department_id;

