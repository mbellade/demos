package org.hibernate.demo;

import org.hibernate.annotations.Struct;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/*
  Hibernate embeddables are value types (does not have an identity) that do not directly correspond to database tables
  but are instead used within entities to group multiple basic type mappings
  and reuse them across several entities.

  There's a couple of alternative ways to represent an embeddable type on the database side.

  as UDTs for dbms supporting user define types
  	Just annotate the embeddable, or the attribute which holds a reference to it, with the new `@Struct` annotation

  as JSON
  	we must annotate the attribute `@JdbcTypeCode(SqlTypes.JSON)`, instead of annotating the embeddable type.
 	We also need to add Jackson or an implementation of JSONB
 	runtimeOnly 'com.fasterxml.jackson.core:jackson-databind:{jacksonVersion}'
 */
@DomainModel(
		annotatedClasses = {
				EmbeddableAggregateTypes.Person.class,
		}
)
@SessionFactory
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(OracleDialect.class)
public class EmbeddableAggregateTypes {

	@Test
	public void testPersist(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Name name = new Name( "Richard", "Phillips", "Feynman" );
					Person person = new Person( name );
					session.persist( person );
				}
		);
	}

	@Entity(name = "MyEntity")
	public static class Person {
		@Id
		@GeneratedValue
		private Long id;

		//@JdbcTypeCode(SqlTypes.JSON)
		private Name name;

		public Person() {
		}

		public Person(Name name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public Name getName() {
			return name;
		}
	}

	@Embeddable
	/**
	 * Specifies the UDT (user defined type) name for the annotated embeddable.
	 * 	This results in the following UDT:
	 * 		create type PersonName as (firstName varchar(255), middleName varchar(255), lastName varchar(255))
	 * 	And the `name` column of the `Author` table will have the type `PersonName`.
	 */
	@Struct(name = "PersonName")
	record Name(String firstName, String middleName, String lastName) {
	}
}
