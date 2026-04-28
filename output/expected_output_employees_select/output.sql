SELECT * FROM employees;
SELECT employees.first_name, employees.last_name FROM employees;
SELECT * FROM employees WHERE salary > 50000;
SELECT * FROM employees WHERE salary > 50000 AND department_id = 1;
SELECT employees.first_name, employees.last_name, departments.dept_name FROM employees JOIN departments ON departments.department_id = employees.department_id;
SELECT employees.first_name, employees.last_name, departments.dept_name FROM employees JOIN departments ON departments.department_id = employees.department_id WHERE dept_name = 'Human Resources';
SELECT * FROM employees ORDER BY salary DESC;
SELECT employees.department_id FROM employees GROUP BY department_id HAVING department_id > 1;
SELECT employees.first_name, employees.last_name, departments.dept_name FROM employees JOIN departments ON departments.department_id = employees.department_id WHERE salary > 50000 ORDER BY last_name ASC LIMIT 5;
