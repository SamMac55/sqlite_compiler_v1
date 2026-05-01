CREATE TABLE test_table (
	id INTEGER PRIMARY KEY,
 	name TEXT);
INSERT INTO employees (employee_id, first_name, last_name, email, salary, department_id) VALUES (900, 'John', 'Doe', 'john@example.com', 50000, 1);

INSERT INTO test_table (id, name) VALUES (1, 'testing');
INSERT INTO test_table (id, name) VALUES (2, 'testing2');

SELECT test_table.* FROM test_table;
DROP TABLE test_table;
SELECT employees.* FROM employees WHERE employee_id = 900;
DELETE FROM employees WHERE employee_id = 900;
