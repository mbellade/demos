package org.hibernate.demo;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = {
		EmbeddableInheritance.TestEntity.class,
		EmbeddableInheritance.Vehicle.class,
		EmbeddableInheritance.Bicycle.class,
		EmbeddableInheritance.MountainBike.class,
		EmbeddableInheritance.Car.class,
} )
@SessionFactory
public class EmbeddableInheritance {
	@Test
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 1L );
			assertThat( result.getVehicle().getWheels() ).isEqualTo( 2 );
			assertThat( result.getVehicle().getSpeed() ).isEqualTo( 25 );
			assertThat( result.getVehicle() ).isExactlyInstanceOf( Bicycle.class );
			assertThat( ( (Bicycle) result.getVehicle() ).getGears() ).isEqualTo( 8 );
		} );
	}

	@Test
	public void testQueryEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity result = session.createQuery(
					"from TestEntity where id = 2",
					TestEntity.class
			).getSingleResult();
			assertThat( result.getVehicle().getWheels() ).isEqualTo( 4 );
			assertThat( result.getVehicle().getSpeed() ).isEqualTo( 200 );
			assertThat( result.getVehicle() ).isExactlyInstanceOf( Car.class );
			assertThat( ( (Car) result.getVehicle() ).getHorsePower() ).isEqualTo( 150 );
		} );
	}

	@Test
	public void testQueryEmbeddable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Vehicle result = session.createQuery(
					"select vehicle from TestEntity where id = 3",
					Vehicle.class
			).getSingleResult();
			assertThat( result.getWheels() ).isEqualTo( 2 );
			assertThat( result.getSpeed() ).isEqualTo( 35 );
			assertThat( result ).isExactlyInstanceOf( MountainBike.class );

			final MountainBike mountainBike = (MountainBike) result;
			assertThat( ( mountainBike ).getGears() ).isEqualTo( 12 );
			assertThat( ( mountainBike ).getSuspensions() ).isEqualTo( SuspensionType.FULL );
		} );
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 4L );
			assertThat( result.getVehicle().getSpeed() ).isEqualTo( 20 );
			assertThat( result.getVehicle() ).isExactlyInstanceOf( Bicycle.class );

			// update values
			result.getVehicle().setSpeed( 22 );
			( (Bicycle) result.getVehicle() ).setGears( 7 );
		} );

		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 4L );
			assertThat( result.getVehicle().getSpeed() ).isEqualTo( 22 );
			assertThat( ( (Bicycle) result.getVehicle() ).getGears() ).isEqualTo( 7 );

			// change type of embeddable
			result.setVehicle( new MountainBike( 25, 8, SuspensionType.FRONT ) );
		} );

		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 4L );
			assertThat( result.getVehicle().getSpeed() ).isEqualTo( 25 );
			assertThat( result.getVehicle() ).isExactlyInstanceOf( MountainBike.class );
			assertThat( ( (MountainBike) result.getVehicle() ).getGears() ).isEqualTo( 8 );
			assertThat( ( (MountainBike) result.getVehicle() ).getSuspensions() ).isEqualTo( SuspensionType.FRONT );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new TestEntity( 1L, new Bicycle( 25, 8 ) ) );
			session.persist( new TestEntity( 2L, new Car( 200, 150 ) ) );
			session.persist( new TestEntity( 3L, new MountainBike( 35, 12, SuspensionType.FULL ) ) );
			session.persist( new TestEntity( 4L, new Bicycle( 20, 6 ) ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from TestEntity" ).executeUpdate() );
	}

	@Entity(name = "TestEntity")
	static class TestEntity {
		@Id
		private Long id;

		@Embedded
		private Vehicle vehicle;

		public TestEntity() {
		}

		public TestEntity(Long id, Vehicle vehicle) {
			this.id = id;
			this.vehicle = vehicle;
		}

		public Vehicle getVehicle() {
			return vehicle;
		}

		public void setVehicle(Vehicle embeddable) {
			this.vehicle = embeddable;
		}
	}

	@Embeddable
	@DiscriminatorValue("parent")
	@DiscriminatorColumn(name = "vehicle_type")
	static class Vehicle {
		private int wheels;
		private Integer speed;

		public Vehicle() {
		}

		public Vehicle(int wheels, Integer speed) {
			this.wheels = wheels;
			this.speed = speed;
		}

		public int getWheels() {
			return wheels;
		}

		public Integer getSpeed() {
			return speed;
		}

		public void setSpeed(Integer parentProp) {
			this.speed = parentProp;
		}
	}

	@Embeddable
	@DiscriminatorValue("bicycle")
	static class Bicycle extends Vehicle {
		private Integer gears;

		public Bicycle() {
		}

		public Bicycle(Integer speed, Integer gears) {
			super( 2, speed );
			this.gears = gears;
		}

		public Integer getGears() {
			return gears;
		}

		public void setGears(Integer spokes) {
			this.gears = spokes;
		}
	}

	@Embeddable
	@DiscriminatorValue("mountain_bike")
	static class MountainBike extends Bicycle {
		private SuspensionType suspensions;

		public MountainBike() {
		}

		public MountainBike(Integer speed, Integer gears, SuspensionType suspensions) {
			super( speed, gears );
			this.suspensions = suspensions;
		}

		public SuspensionType getSuspensions() {
			return suspensions;
		}

		public void setSuspensions(SuspensionType suspensions) {
			this.suspensions = suspensions;
		}
	}

	enum SuspensionType {
		FRONT,
		FULL
	}

	;

	@Embeddable
	static class Car extends Vehicle {
		private int horsePower;

		public Car() {
		}

		public Car(Integer speed, int horsePower) {
			super( 4, speed );
			this.horsePower = horsePower;
		}

		public int getHorsePower() {
			return horsePower;
		}

		public void setHorsePower(int horsePower) {
			this.horsePower = horsePower;
		}
	}
}
