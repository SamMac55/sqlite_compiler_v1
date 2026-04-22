-- CREATE TABLE COMMAND --
DROP TABLE IF EXISTS employees;
CREATE TABLE employees (
	e_id INTEGER (null), 
	first_name TEXT (null), 
	last_name TEXT (null), 
	salary REAL, 
	department_id INTEGER,
	FOREIGN KEY (department_id) REFERENCES departments (department_id)
);

-- DELETE TABLE --
DROP TABLE IF EXISTS employees;

--INSERT ROW --
INSERT INTO employees(first_name,last_name,dob,department_id)
VALUES ('Jane','Doe','2026-03-21',3);

-- SELECT STMT --
SELECT *
FROM employees;

-- SELECT STMT --
SELECT first_name, last_name
FROM employees
WHERE e_id = 1;

-- SELECT STMT --
SELECT name
FROM departments
GROUP BY name
HAVING department_id < 5;

-- SELECT STMT --
SELECT *
FROM employees
ORDER BY salary DESC;

-- SELECT STMT --
SELECT *
FROM employees
ORDER BY salary;

-- SELECT STMT --
SELECT *
FROM employees
LIMIT 1;

-- SELECT STMT --
SELECT employees.*, departments.department_name
FROM employees
JOIN departments ON employees.department_id = departments.department_id;

-- DELETE ROW --
DELETE FROM employees WHERE e_id = 5 AND name = 'Stephaine';

-- UPDATE ROW --
UPDATE employees
SET salary=75000
WHERE e_id = 1;

.fullschema

.tables


