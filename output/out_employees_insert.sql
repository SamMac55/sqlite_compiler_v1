INSERT INTO employees (employee_id, first_name, last_name, email, salary, department_id) VALUES (1, 'John', 'Doe', 'john@example.com', 50000, 1);

INSERT INTO departments (department_id, dept_name) VALUES (10, 'HR');

INSERT INTO projects (project_id, project_name, budget) VALUES (100, 'Apollo', 1000000);

INSERT INTO employee_projects (project_employee_id, employee_id, project_id, emp_role) VALUES (1, 1, 100, 'Developer');

INSERT INTO employees (employee_id, first_name, last_name, email, salary, department_id) VALUES (2, 'Jane', 'Smith', 'jane@example.com', 60000, 2);

