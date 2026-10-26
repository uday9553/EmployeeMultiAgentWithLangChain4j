-- Sample promotion rules: performance ratings use a scale of 1 to 5.
INSERT INTO promotion_policy
(designation, nextDesignation, minimumMonthsInRole, minimumPerformanceRating)
VALUES ('Junior Developer', 'Developer', 12, 3),
       ('Developer', 'Senior Developer', 24, 4),
       ('Senior Developer', 'Team Lead', 24, 4),
       ('Team Lead', 'Engineering Manager', 36, 4),
       ('Engineering Manager', 'Senior Engineering Manager', 36, 5);

-- Illustrative demo departments and salaries, not real employee compensation.
INSERT INTO employees (employeeId, name, department, designation, salary, monthsInRole, performanceRating)
VALUES ('EMP-1001', 'Uday Kumar', 'Engineering', 'Junior Developer', 50000, 18, 4),
       ('EMP-1002', 'Priya Sharma', 'Engineering', 'Developer', 75000, 12, 5),
       ('EMP-1003', 'Rahul Verma', 'Engineering', 'Senior Developer', 100000, 30, 3),
       ('EMP-1004', 'Ananya Reddy', 'Engineering', 'Team Lead', 130000, 40, 5),
       ('EMP-1005', 'Arjun Patel', 'Engineering', 'Engineering Manager', 180000, 24, 4);
