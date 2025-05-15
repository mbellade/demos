package org.hibernate.demo;

import org.hibernate.Hibernate;
import org.hibernate.annotations.ConcreteProxy;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


@SessionFactory
@DomainModel(annotatedClasses = {
		ConcreteProxies.Owner.class,
		ConcreteProxies.Animal.class,
		ConcreteProxies.Mammal.class,
		ConcreteProxies.Cat.class,
		ConcreteProxies.Fish.class,
})
class ConcreteProxies {
	@Test
	public void testFind(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final Owner owner1 = session.find( Owner.class, 1L );

			// The retrieved association has the correct type and is lazy
			assertThat( owner1.getAnimal() ).isInstanceOf( Cat.class );
			assertThat( Hibernate.isInitialized( owner1.getAnimal() ) ).isFalse();

			// Laziness is maintained even when casting or accessing the ID
			final Cat cat = (Cat) owner1.getAnimal();
			assertThat( cat.getId() ).isEqualTo( 1L );
			assertThat( Hibernate.isInitialized( cat ) ).isFalse();
		} );
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final Owner owner2 = session.createQuery(
					"from Owner where id = 2",
					Owner.class
			).getSingleResult();

			// The retrieved association has the correct type and is lazy
			assertThat( owner2.getAnimal() ).isInstanceOf( Fish.class );
			assertThat( Hibernate.isInitialized( owner2.getAnimal() ) ).isFalse();

			if ( owner2.getAnimal() instanceof Fish fish ) {
				// Laziness is maintained even when casting or accessing the ID
				assertThat( fish.getId() ).isEqualTo( 2L );
				assertThat( Hibernate.isInitialized( fish ) ).isFalse();
			}
		} );
	}

	@Test
	public void testGetReference(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final Mammal mammal = session.getReference( Mammal.class, 1L );
			assertThat( mammal ).isInstanceOf( Cat.class );
			assertThat( Hibernate.isInitialized( mammal ) ).isFalse();

			final Cat cat = (Cat) mammal;
			assertThat( Hibernate.isInitialized( cat ) ).isFalse();

			System.out.println( "\nRetrieving mammal: " + mammal.getId() + "\n" );

			assertThat( cat.getClaws() ).isEqualTo( "sharp" );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Owner( 1L, new Cat( 1L, "Gatta", "sharp" ) ) );
			session.persist( new Owner( 2L, new Fish( 2L, 2 ) ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "Owner")
	static class Owner {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		private Animal animal;

		public Owner() {
		}

		public Owner(Long id, Animal animal) {
			this.id = id;
			this.animal = animal;
		}

		public Animal getAnimal() {
			return animal;
		}
	}

	@Entity(name = "Animal")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "disc_col")
	@ConcreteProxy
	static abstract class Animal {
		@Id
		private Long id;

		public Animal() {
		}

		public Animal(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}
	}

	@Entity(name = "Mammal")
	static class Mammal extends Animal {
		private String name;

		public Mammal() {
		}

		public Mammal(Long id, String name) {
			super( id );
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	@Entity(name = "Cat")
	static class Cat extends Mammal {
		private String claws;

		public Cat() {
		}

		public Cat(Long id, String child1Prop, String claws) {
			super( id, child1Prop );
			this.claws = claws;
		}

		public String getClaws() {
			return claws;
		}
	}

	@Entity(name = "SingleChild2")
	static class Fish extends Animal {
		private Integer fins;

		public Fish() {
		}

		public Fish(Long id, Integer fins) {
			super( id );
			this.fins = fins;
		}

		public Integer getFins() {
			return fins;
		}
	}
}
