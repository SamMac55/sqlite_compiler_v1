SELECT employees.department_id, COUNT(*), departments.dept_name FROM employees JOIN departments ON departments.department_id = employees.department_id  GROUP BY employees.department_id, dept_name;
