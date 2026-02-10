insert into company_table (id, name, city, street) values(1, 'Red Hat', 'Raleigh', 'Varsity Drive');
insert into company_table (id, name, city, street) values(2, 'IBM', 'Armonk', 'Orchard Road');
insert into company_table (id, name, city, street) values(3, 'Belladelli Giovanni Jewelry', 'Pegognaga', 'Via Roma');
insert into company_table (id, name, city, street) values(4, 'MB startup', null, null);
insert into company_table (id, name, city, street) values(7, 'Apple', 'Cupertino', 'Apple Park');
insert into company_table (id, name, city, street) values(5, 'Alphabet', 'Mountain View', 'Googleplex');
insert into company_table (id, name, city, street) values(6, 'American Express', 'New York City', 'Vesey Street');

insert into employee_table (id, first_name, last_name, salary, works_at) values(1, 'Marco', 'Belladelli', 500000, 1);
insert into employee_table (id, first_name, last_name, salary, works_at) values(2, 'Andrea', 'Boriero', 100000, 1);
insert into employee_table (id, first_name, last_name, salary, works_at) values(3, 'Luca', 'Molteni', 200000, 2);
insert into employee_table (id, first_name, last_name, salary, works_at) values(4, 'Flavia', 'Galeotti', 300000, 3);

insert into project_table (name, start_date) values('Quarkus', date '2019-03-20');
insert into project_table (name, start_date) values('Hibernate', date '2001-05-23');

insert into project_employee_table (project_name, employee_id) values('Quarkus', 1);
insert into project_employee_table (project_name, employee_id) values('Quarkus', 2);
insert into project_employee_table (project_name, employee_id) values('Quarkus', 3);
insert into project_employee_table (project_name, employee_id) values('Hibernate', 1);
insert into project_employee_table (project_name, employee_id) values('Hibernate', 2);