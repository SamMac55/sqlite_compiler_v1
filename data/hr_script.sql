-- departments table
CREATE TABLE departments (
    department_id INTEGER PRIMARY KEY,
    dept_name TEXT NOT NULL
);
-- employees talbe
CREATE TABLE employees (
    employee_id INTEGER PRIMARY KEY,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    email TEXT NOT NULL,
    salary REAL NOT NULL,
    department_id INTEGER REFERENCES departments(department_id)
);
-- projecs table
CREATE TABLE projects (
    project_id INTEGER PRIMARY KEY,
    project_name TEXT NOT NULL,
    budget REAL NOT NULL
);
-- employee projects table
CREATE TABLE employee_projects (
    project_employee_id INTEGER PRIMARY KEY,
    employee_id INTEGER NOT NULL REFERENCES employees(employee_id),
    project_id INTEGER NOT NULL REFERENCES projects(project_id),
    emp_role TEXT NOT NULL
);


-- Departments
INSERT INTO departments (department_id, dept_name) VALUES
(1, 'Human Resources'),
(2, 'Engineering'),
(3, 'Marketing');

-- Employees
INSERT INTO employees (employee_id, first_name, last_name, email, salary, department_id) VALUES
(1, 'Alice', 'Johnson', 'alice.johnson@example.com', 60000, 1),
(2, 'Bob', 'Smith', 'bob.smith@example.com', 85000, 2),
(3, 'Carol', 'Davis', 'carol.davis@example.com', 90000, 2),
(4, 'David', 'Lee', 'david.lee@example.com', 70000, 3),
(5, 'Eva', 'Brown', 'eva.brown@example.com', 65000, 1);

-- Projects
INSERT INTO projects (project_id, project_name, budget) VALUES
(1, 'Website Redesign', 50000),
(2, 'Mobile App Development', 120000),
(3, 'Marketing Campaign', 40000);

-- Employee-Project assignments
INSERT INTO employee_projects (project_employee_id, employee_id, project_id, emp_role) VALUES
(1, 1, 1, 'Coordinator'),
(2, 2, 1, 'Developer'),
(3, 2, 2, 'Lead Developer'),
(4, 3, 2, 'Developer'),
(5, 4, 3, 'Marketing Specialist'),
(6, 5, 1, 'HR Support');