package org.hibernate.demo;

import org.hibernate.query.MutationQuery;
import org.hibernate.query.Order;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.range.Range;
import org.hibernate.query.restriction.Restriction;
import org.hibernate.query.specification.MutationSpecification;
import org.hibernate.query.specification.SelectionSpecification;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.List;

import static org.hibernate.query.restriction.Restriction.restrict;

/* The QuerySpecification API is considered incubating
The idea is similar in concept to criteria queries, but focused on ease-of-use and less verbosity.
Generally the JPA static metamodel is a convenient and type-safe way to help build these sorting and restriction references.
( with gradle add :  testAnnotationProcessor "org.hibernate:hibernate-jpamodelgen:${hibernateVersion})
 */
@DomainModel(
		annotatedClasses = {
				Person.class
		}
)
@SessionFactory
public class QuerySpecification {

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Person andrea = new Person( "Andrea", 53 );
					Person marco = new Person( "Marco", 25 );
					session.persist( andrea );
					session.persist( marco );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testSelectionSpecificationFromHQL(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SelectionSpecification<Person> spec = SelectionSpecification.create(
									Person.class,
									"from Person"
							)
							//add restriction
							.restrict(
									Restriction.restrict(
											Person_.age,
											Range.closed( 10, 50 )
									)
							)
							// sorting
							.sort(
									Order.asc( Person_.name )
							);
					SelectionQuery<Person> query = spec.createQuery( session );
					List<Person> list = query.list();
				}
		);
	}

	@Test
	public void testSelectionSpecificationFrom(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SelectionSpecification<Person> spec = SelectionSpecification.create(
									Person.class
							)
							//add restriction
							.restrict(
									restrict(
											Person.class,
											"age",
											Range.closed( 10, 50 )
									)
							)
							// sorting
							.sort(
									Order.asc(
											Person.class,
											"name"
									)
							);
					SelectionQuery<Person> query = spec.createQuery( session );
					List<Person> list = query.list();
				}
		);
	}

	@Test
	public void testMutationSpecification(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					MutationQuery qry = MutationSpecification.create(
							Person.class,
							"delete Person"
					).restrict(
							Restriction.restrict(
									Person_.age,
									Range.closed( 10, 50 )
							)
					).createQuery( session );
					qry.executeUpdate();
				}
		);
	}


}
