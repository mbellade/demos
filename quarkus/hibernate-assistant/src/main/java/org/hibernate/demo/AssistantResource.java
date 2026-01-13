package org.hibernate.demo;

import org.hibernate.Session;
import org.hibernate.demo.assistant.HibernateAssistantLC4J;
import org.hibernate.query.SelectionQuery;

import org.jboss.logging.Logger;

import dev.langchain4j.data.message.UserMessage;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Objects;

@Path("/assistant")
public class AssistantResource {
	private static final Logger LOG = Logger.getLogger( AssistantResource.class);

	@Inject
	Session session;

	@Inject
	HibernateAssistantLC4J assistant;

	/**
	 * Executes a natural language query and returns data in JSON format.
	 */
	@GET
	@Path("/json")
	@Produces(MediaType.TEXT_PLAIN)
	public Response queryToJson(@QueryParam("query") String query) {
		LOG.debugf( "Query: %s", query );

		try {
			Objects.requireNonNull( query, "Query parameter must not be null" );
			final SelectionQuery<?> select = assistant.createAiQuery(
					query,
					session
			);
			final String json = assistant.executeQueryToJson( select, session );

			LOG.debugf( "Assistant response: %s", json );

			assistant.getChatMemory().add( UserMessage.from( "The query returned the following data (in JSON format):\n" + json ) );

			return Response.ok( json ).build();
		}
		catch (Exception e) {
			LOG.errorf( e, "Error executing query: %s", e.getMessage() );
			return Response.status( Response.Status.INTERNAL_SERVER_ERROR )
					.entity( "Error executing query: " + e.getMessage() ).build();
		}
	}

	/**
	 * Executes a natural language query and feeds back data into the LLM to provide a natural language response.
	 */
	@GET
	@Path("/ask")
	@Produces(MediaType.TEXT_PLAIN)
	public Response naturalLanguage(@QueryParam("query") String query) {
		LOG.debugf( "Ask: %s", query );

		try {
			Objects.requireNonNull( query, "Query parameter must not be null" );
			final String response = assistant.executeQuery( query, session );

			LOG.debugf( "Assistant response: %s", response );

			return Response.ok( response ).build();
		}
		catch (Exception e) {
			LOG.errorf( e, "Error executing query: %s", e.getMessage() );
			return Response.status( Response.Status.INTERNAL_SERVER_ERROR )
					.entity( "Error executing query: " + e.getMessage() ).build();
		}
	}

	@GET
	@Path("/clear")
	@Produces(MediaType.TEXT_PLAIN)
	public Response clear() {
		LOG.debugf( "Clearing assistant context" );
		assistant.clear();
		return Response.ok( "Assistant context cleared" ).build();
	}
}
