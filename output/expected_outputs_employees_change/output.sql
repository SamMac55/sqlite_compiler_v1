CREATE TABLE test_table (
	id INTEGER PRIMARY KEY,
 	name TEXT);
INSERT INTO employees (employee_id, first_name, last_name, email, salary, department_id) VALUES (900, 'John', 'Doe', 'john@example.com', 50000, 1);
UPDATE employees SET salary = 60000 WHERE employee_id = 1;
DELETE FROM employees WHERE employee_id = 1;
DROP TABLE test_table;
SELECT * FROM employees;
