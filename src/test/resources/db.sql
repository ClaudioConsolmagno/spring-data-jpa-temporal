DROP TABLE IF EXISTS employee;

CREATE TABLE employee
(
    employee_id         INT,
    name                VARCHAR(255) NOT NULL,
    job                 VARCHAR(255) NOT NULL,
    temporal_id         INT AUTO_INCREMENT PRIMARY KEY,
    from_date           TIMESTAMP(9)    NOT NULL,
    to_date             TIMESTAMP(9)    NOT NULL,
    address_temporal_id INT NULL
);

create unique index employee_id_to_date_index on employee (employee_id, to_date);
