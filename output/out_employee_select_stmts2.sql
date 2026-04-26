SELECT * FROM employees;

SELECT employees.first_name, employees.last_name FROM employees;

SELECT * FROM employees WHERE salary > 50000;

SELECT employees.first_name, employees.last_name FROM employees WHERE salary > 40000 AND department_id = 1;

SELECT employees.first_name, departments.dept_name FROM employees JOIN departments ON departments.department_id = employees.department_id;

SELECT employees.first_name, employees.last_name, departments.dept_name FROM employees JOIN departments ON departments.department_id = employees.department_id WHERE dept_name = 'HR';

SELECT * FROM employees JOIN departments ON departments.department_id = employees.department_id ORDER BY salary DESC;

SELECT employees.first_name, departments.dept_name FROM employees JOIN departments ON departments.department_id = employees.department_id ORDER BY dept_name ASC;

SELECT employees.department_id FROM employees GROUP BY department_id;

SELECT employees.department_id, departments.dept_name FROM employees JOIN departments ON departments.department_id = employees.department_id GROUP BY employees.department_id;

SELECT employees.department_id, departments.dept_name FROM employees JOIN departments ON departments.department_id = employees.department_id GROUP BY employees.department_id HAVING employees.department_id > 1;

SELECT employees.first_name, employees.last_name, departments.dept_name FROM employees JOIN departments ON departments.department_id = employees.department_id WHERE salary > 50000 ORDER BY last_name ASC LIMIT 5;

SELECT employees.first_name, employee_projects.project_id FROM employees JOIN employee_projects ON employee_projects.employee_id = employees.employee_id;

SELECT * FROM employee_projects JOIN projects ON projects.project_id = employee_projects.project_id;

