package org.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Set;

@Entity
@Table(name = "project_table")
public class Project {
	@Id
	private String name;

	@Column(name = "start_date")
	private LocalDate startDate;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "project_employee_table",
			joinColumns = @JoinColumn(name = "project_name"),
			inverseJoinColumns = @JoinColumn(name = "employee_id"))
	private Set<Employee> employees;
}
