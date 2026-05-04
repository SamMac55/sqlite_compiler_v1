SELECT employees.* FROM employees;
SELECT employees.first_name FROM employees;
SELECT employees.first_name, employees.last_name, employees.salary FROM employees ORDER BY salary DESC;
SELECT employees.first_name, employees.last_name, employee_projects.emp_role FROM employees JOIN employee_projects ON employee_projects.employee_id = employees.employee_id  WHERE project_id <= 2;
SELECT employees.first_name, employees.last_name, employee_projects.emp_role, projects.project_name FROM employees JOIN employee_projects ON employee_projects.employee_id = employees.employee_id JOIN projects ON projects.project_id = employee_projects.project_id ;
SELECT projects.project_name FROM projects WHERE budget <= 45000;
SELECT employees.first_name, employees.last_name, employees.salary FROM employees WHERE department_id > 1 ORDER BY salary ASC LIMIT 1;
SELECT employees.department_id, COUNT(*), departments.dept_name FROM employees JOIN departments ON departments.department_id = employees.department_id  GROUP BY employees.department_id, dept_name;
