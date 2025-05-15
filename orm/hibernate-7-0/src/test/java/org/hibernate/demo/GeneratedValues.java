package org.hibernate.demo;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SessionFactory
@DomainModel(annotatedClasses = {
		GeneratedValues.Transaction.class
})
class GeneratedValues {
	@Test
	void transaction(SessionFactoryScope scope) {
		final Long id = scope.fromTransaction( session -> {
			final Transaction transaction = new Transaction();
			transaction.setAmount( BigDecimal.ONE );
			session.persist( transaction );

			// Flush data to database to trigger value generation
			session.flush();

			// Generated data has been retrieved from the database
			assertThat( transaction.getId() ).isNotNull();
			assertThat( transaction.getType() ).isEqualTo( "unknown" );
			assertThat( transaction.getUpdatedAt() ).isAfter( LocalDateTime.now().minusSeconds( 10 ) );
			assertThat( transaction.getAmount() ).isEqualByComparingTo( BigDecimal.ONE );

			return transaction.getId();
		} );

		System.out.println( "\nTransaction generated with id: " + id + "\n" );

		scope.inTransaction( session -> {
			final Transaction transaction = session.find( Transaction.class, id );
			transaction.setAmount( BigDecimal.ZERO );

			// Flush persistent Transaction to trigger update
			session.flush();

			// Update-generated data has been synchronized
			assertThat( transaction.getUpdatedAt() ).isAfter( LocalDateTime.now().minusSeconds( 10 ) );
			assertThat( transaction.getAmount() ).isEqualByComparingTo( BigDecimal.ZERO );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "Transaction")
	@Table(name = "transactions_table")
	static class Transaction {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@Generated(sql = "'unknown'")
		private String type;

		@UpdateTimestamp(source = SourceType.DB)
		private LocalDateTime updatedAt;

		private BigDecimal amount;

		public Long getId() {
			return id;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public LocalDateTime getUpdatedAt() {
			return updatedAt;
		}

		public BigDecimal getAmount() {
			return amount;
		}

		public void setAmount(BigDecimal amount) {
			this.amount = amount;
		}
	}
}
