insert into company_table (id, name, city, street, country) values
    (1, 'Red Hat', 'Raleigh', 'Varsity Drive', 'USA'),
    (2, 'IBM', 'Armonk', 'Orchard Road', 'USA'),
    (3, 'Belladelli Giovanni Jewelry', 'Pegognaga', 'Via Roma', 'Italy'),
    (4, 'MB startup', null, null, 'Italy'),
    (5, 'Alphabet', 'Mountain View', 'Googleplex', 'USA'),
    (6, 'American Express', 'New York City', 'Vesey Street', 'USA'),
    (7, 'Apple', 'Cupertino', 'Apple Park', 'USA');

insert into employee_table (id, first_name, last_name, salary, works_at) values
    (1, 'Marco', 'Belladelli', 500000, 1),
    (2, 'Andrea', 'Boriero', 100000, 1),
    (3, 'Luca', 'Molteni', 200000, 2),
    (4, 'Flavia', 'Galeotti', 300000, 3),
    (5, 'John', 'Smith', 150000, 2),
    (6, 'Sarah', 'Connor', 250000, 7),
    (7, 'James', 'Lee', 180000, 7),
    (8, 'Emily', 'Chen', 220000, 5),
    (9, 'David', 'Kumar', 190000, 5),
    (10, 'Lisa', 'Johnson', 170000, 6);

insert into project_table (name, start_date) values
    ('Quarkus', date '2019-03-20'),
    ('Hibernate', date '2001-05-23');

insert into project_employee_table (project_name, employee_id) values
    ('Quarkus', 1),
    ('Quarkus', 2),
    ('Quarkus', 3),
    ('Hibernate', 1),
    ('Hibernate', 2);