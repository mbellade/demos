package org.hibernate.demo.jakartadata;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Vegetable {
	@Id
	@GeneratedValue
	private Integer id;

	private String name;

	public Vegetable() {
	}

	public Vegetable(String name) {
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
