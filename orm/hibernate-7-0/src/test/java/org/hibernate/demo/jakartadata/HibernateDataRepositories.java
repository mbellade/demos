package org.hibernate.demo.jakartadata;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				Fruit.class,
				Vegetable.class
		}
)
@SessionFactory
public class HibernateDataRepositories {
//	@Inject
//	Grocery grocery;

	@Test
	public void testAddAFruit(SessionFactoryScope scope) {
		String fruitName = "Apple";
		scope.inStatelessSession( session -> {
			Grocery grocery = new Grocery_( session );
			grocery.addFruit( new Fruit( fruitName ) );
		} );

		scope.inStatelessSession( session -> {
			Grocery grocery = new Grocery_( session );
			Fruit fruit = grocery.fruitByName( fruitName );
			assertThat(fruit).isNotNull();
		} );
	}

}
