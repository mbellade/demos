package org.hibernate.bugs;

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
			final Owner parent1 = session.find( Owner.class, 1L );
			assertThat( parent1.getSingle() ).isInstanceOf( Cat.class );
			assertThat( Hibernate.isInitialized( parent1.getSingle() ) ).isFalse();
			final Cat proxy = (Cat) parent1.getSingle();
			assertThat( proxy.getId() ).isEqualTo( 1L );
			assertThat( Hibernate.isInitialized( proxy ) ).isFalse();
		} );
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final Owner parent2 = session.createQuery(
					"from Owner where id = 2",
					Owner.class
			).getSingleResult();
			assertThat( parent2.getSingle() ).isInstanceOf( Fish.class );
			assertThat( Hibernate.isInitialized( parent2.getSingle() ) ).isFalse();
			final Fish proxy = (Fish) parent2.getSingle();
			assertThat( proxy.getId() ).isEqualTo( 2L );
			assertThat( Hibernate.isInitialized( proxy ) ).isFalse();
		} );
	}

	@Test
	public void testGetReference(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final Mammal proxy1 = session.getReference( Mammal.class, 1L );
			assertThat( proxy1 ).isInstanceOf( Cat.class );
			assertThat( Hibernate.isInitialized( proxy1 ) ).isFalse();
			final Cat subChild1 = (Cat) proxy1;
			assertThat( Hibernate.isInitialized( subChild1 ) ).isFalse();

			System.out.println( "\nRetrieving mammal: " + proxy1.getId() + "\n" );

			assertThat( subChild1.getClaws() ).isEqualTo( "sharp" );
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
		private Animal single;

		public Owner() {
		}

		public Owner(Long id, Animal single) {
			this.id = id;
			this.single = single;
		}

		public Animal getSingle() {
			return single;
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
