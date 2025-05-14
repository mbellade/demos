package org.hibernate.demo;

import org.hibernate.HibernateException;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/*
 @Filter allows to filter out entities or collection using custom SQL criteria,
 the difference with @SQLRestrictions is the possibility to parametrize the filter clause at runtime
 */

@DomainModel(
		annotatedClasses = {
				Filters.Person.class,
		}
)
@SessionFactory
public class Filters {

	@BeforeAll
	public static void beforeAll(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Person andrea = new Person("Andrea", 53, false);
					Person marco = new Person("Marco", 25, true);
					session.persist( andrea );
					session.persist( marco );
				}
		);
	}

	@Test
	public void testUsingAutoEnabledFilter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// we need to set the parameter value of the auto enabled filer
			session.getEnabledFilter( "filterByAge" ).setParameter( "age", 53 );
			List<Person> people = session.createQuery( "from Person", Person.class ).list();
			assertThat(people.size()).isEqualTo( 1 );
		} );
	}

	@Test
	public void testNotAutoEnabledFilter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// need to enable it and set its parameter
			session.enableFilter( "filterYesNo" ).setParameter( "yesNo", true );
			// we need to set the parameter value of the auto enabled filer
			session.getEnabledFilter( "filterByAge" ).setParameter( "age", 53 );
			List<Person> people = session.createQuery( "from Person", Person.class ).list();
			assertThat(people.size()).isEqualTo( 0 );
		} );
	}

	@Test
	public void testActiveFilterNoSpecifyingParameterOrUsigResolver(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						/*
		 					An exception is thrown because Filter parameter 'filterByAge' has neither an argument nor a resolver.
		 					If we add a resolver this will not happen
		 				*/
						assertThrows(
								HibernateException.class, () -> {
									session.createQuery( "from Person", Person.class ).list();
								}
						)
		);
	}

	@FilterDef(
			name = "filterByAge",
			defaultCondition = "age = :age",
			parameters = @ParamDef( name = "age", type = Integer.class),
//			parameters = @ParamDef( name = "age", type = Integer.class, resolver = AgeResolver.class),
			autoEnabled = true
	)
	@FilterDef(
			name = "filterYesNo",
			defaultCondition = "yesNo = :yesNo",
			parameters = @ParamDef( name = "yesNo", type = Boolean.class )
	)
	@Filter( name = "filterByAge" )
	@Filter( name = "filterYesNo" )
	@Entity(name = "Person")
	public static class Person{
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		private int age;

		private boolean yesNo;

		public Person() {
		}

		public Person(String name, int age, boolean yesNo) {
			this.name = name;
			this.age = age;
			this.yesNo = yesNo;
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

		public boolean isYesNo() {
			return yesNo;
		}
	}

	public static class AgeResolver implements Supplier<Integer> {
		@Override
		public Integer get() {
			return 25;
		}
	}

}
