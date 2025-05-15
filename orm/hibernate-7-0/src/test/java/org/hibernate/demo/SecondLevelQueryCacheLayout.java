package org.hibernate.demo;

import org.hibernate.annotations.CacheLayout;
import org.hibernate.annotations.QueryCacheLayout;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Cacheable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Most queries do not benefit from caching or their results,
 * so by default, individual queries are not cached even after enabling query caching.
 * Each particular query that needs to be cached must be manually set as cacheable.
 * JPA: .setHint("org.hibernate.cacheable", "true")
 * Hibernate : .setCacheable(true)
 */
@DomainModel(
		annotatedClasses = {
				SecondLevelQueryCacheLayout.Person.class,
				SecondLevelQueryCacheLayout.Address.class,
		}
)
@SessionFactory(
		statementInspectorClass = SQLStatementInspector.class,
		generateStatistics = true
)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
//				@Setting(name = AvailableSettings.QUERY_CACHE_LAYOUT, value = "SHALLOW"),
		}
)
public class SecondLevelQueryCacheLayout {

	private static final Long ADDRESS_ID = 1L;
	private static final Long PERSON_ID = 2L;

	@BeforeAll
	public static void init(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();

		scope.inTransaction(
				session -> {
					Address address = new Address( ADDRESS_ID, "Pomaia" );
					Person person = new Person( PERSON_ID, "Andrea", address );
					session.persist( person );
				}
		);
		assertThat( statistics.getSecondLevelCachePutCount() ).isEqualTo( 2 );
	}

	@Test
	public void testCache(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		scope.inTransaction(
				session -> {
					Person person = session.find( Person.class, PERSON_ID );
					assertThat( person ).isNotNull();
					assertThat( statementInspector.getSqlQueries().size() ).isEqualTo( 0 );
					assertThat( statistics.getSecondLevelCacheHitCount() ).isEqualTo( 2L );
					person.getAddress().getStreet();

					assertThat( statementInspector.getSqlQueries().size() ).isEqualTo( 0 );
					statistics.clear();
				}
		);
	}

	@Test
	public void testQueryCache(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		scope.inTransaction(
				session -> {
					List<Person> people = session.createQuery( "from Person p join fetch p.address where p.id = :id", Person.class )
							.setParameter( "id", PERSON_ID )
							.setCacheable( true )
							.list();
					assertThat( statistics.getQueryCacheHitCount() ).isEqualTo( 0 );
					assertThat( statistics.getQueryCachePutCount() ).isEqualTo( 1 );
					assertThat( statistics.getSecondLevelCacheHitCount() ).isEqualTo( 0 );
					assertThat( people.size() ).isEqualTo( 1 );
					assertThat( statementInspector.getSqlQueries().size() ).isEqualTo( 1 );
				}
		);
		statistics.clear();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					List<Person> people =  session.createQuery( "from Person p join fetch p.address where p.id = :id", Person.class )
							// without a query is executed
							.setCacheable( true )
							.setParameter( "id", PERSON_ID )
							.list();
					assertThat( statistics.getQueryCacheHitCount() ).isEqualTo( 1 );
					assertThat( statistics.getQueryCachePutCount() ).isEqualTo( 0 );
					// Because the Address entity annotated with @QueryCacheLayout(layout = CacheLayout.SHALLOW)
					// the query second level cache contains only the Address id, the address is then retrieved from the Entity cache
					assertThat( statistics.getSecondLevelCacheHitCount() ).isEqualTo( 1 );
					assertThat( people.size() ).isEqualTo( 1 );
					Person person = people.get( 0 );
					assertThat( statementInspector.getSqlQueries().size() ).isEqualTo( 0 );
				}
		);
	}

	@Entity(name = "Person")
	@Cacheable
	public static class Person {
		@Id
		private Long id;

		private String name;

		@ManyToOne(cascade = CascadeType.PERSIST)
		private Address address;

		public Person() {
		}

		public Person(Long id, String name, Address address) {
			this.id = id;
			this.name = name;
			this.address = address;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Address getAddress() {
			return address;
		}
	}

	@Entity(name = "Address")
	@Cacheable
	@QueryCacheLayout(layout = CacheLayout.SHALLOW)
	public static class Address {
		@Id
		private Long id;

		private String street;

		public Address() {
		}

		public Address(Long id, String street) {
			this.id = id;
			this.street = street;
		}

		public Long getId() {
			return id;
		}

		public String getStreet() {
			return street;
		}
	}
}
