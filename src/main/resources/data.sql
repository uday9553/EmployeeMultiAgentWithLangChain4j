-- Sample promotion rules: performance ratings use a scale of 1 to 5.
INSERT INTO promotion_policy
    (designation, nextDesignation, minimumMonthsInRole, minimumPerformanceRating)
VALUES
    ('Junior Developer', 'Developer', 12, 3),
    ('Developer', 'Senior Developer', 24, 4),
    ('Senior Developer', 'Team Lead', 24, 4),
    ('Team Lead', 'Engineering Manager', 36, 4),
    ('Engineering Manager', 'Senior Engineering Manager', 36, 5);

INSERT INTO employees (employeeId, name, designation, monthsInRole, performanceRating)
VALUES
    ('EMP-1001', 'Uday Kumar', 'Junior Developer', 18, 4),
    ('EMP-1002', 'Priya Sharma', 'Developer', 12, 5),
    ('EMP-1003', 'Rahul Verma', 'Senior Developer', 30, 3),
    ('EMP-1004', 'Ananya Reddy', 'Team Lead', 40, 5),
    ('EMP-1005', 'Arjun Patel', 'Engineering Manager', 24, 4);
