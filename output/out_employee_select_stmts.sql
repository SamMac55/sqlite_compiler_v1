SELECT * FROM employees;

SELECT employees.first_name, employees.last_name, employees.email FROM employees;

SELECT * FROM employees LIMIT 5;

SELECT employees.first_name, employees.last_name FROM employees LIMIT 10;

SELECT * FROM employees WHERE salary > 50000;

SELECT employees.first_name, employees.last_name FROM employees WHERE department_id = 2;

SELECT * FROM employees WHERE salary > 50000 AND department_id = 1;

SELECT * FROM employees WHERE salary > 70000 OR department_id = 3;

SELECT * FROM employees ORDER BY salary ASC;

SELECT * FROM employees ORDER BY salary DESC;

SELECT employees.first_name, employees.last_name FROM employees ORDER BY last_name ASC;

SELECT * FROM employees ORDER BY department_id, salary DESC;

