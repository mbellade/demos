package org.hibernate.demo;

import org.hibernate.query.spi.QueryImplementor;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				GetResultCount.Person.class,
		}
)
@SessionFactory(
		statementInspectorClass = SQLStatementInspector.class
)
public class GetResultCount {

	@BeforeAll
	public static void beforeAll(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Person andrea = new Person("Andrea", 53);
			Person marco = new Person("Marco", 25);
			session.persist( andrea );
			session.persist( marco );
		} );
	}

	@Test
	public void testGetResultCount(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			QueryImplementor<Person> fromPerson = session.createQuery( "from Person", Person.class );
			long resultCount = fromPerson.getResultCount();
			// it triggers a `select count(*) from Person p1_0
			assertThat(resultCount).isEqualTo( 2 );
			assertThat( statementInspector.getSqlQueries().size() ).isEqualTo( 1 );
			statementInspector.clear();
			fromPerson.getResultCount();
			// it triggers another `select count(*) from Person p1_0
			assertThat( statementInspector.getSqlQueries().size() ).isEqualTo( 1 );
		});
	}

	@Entity(name = "Person")
	public static class Person{
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		private int age;

		public Person() {
		}

		public Person(String name, int age) {
			this.name = name;
			this.age = age;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public int getAge() {
			return age;
		}
	}
}
