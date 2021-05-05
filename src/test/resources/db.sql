DROP TABLE IF EXISTS employee;

CREATE TABLE employee
(
    employee_id         INT,
    name                VARCHAR(255) NOT NULL,
    job                 VARCHAR(255) NOT NULL,
    temporal_id         SERIAL PRIMARY KEY,
    from_date           TIMESTAMP WITH TIME ZONE NOT NULL,
    to_date             TIMESTAMP WITH TIME ZONE NOT NULL
);

create unique index employee_id_to_date_index on employee (employee_id, to_date);
