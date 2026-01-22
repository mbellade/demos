package org.hibernate.demo.assistant;

import org.hibernate.StatelessSession;
import org.hibernate.query.SelectionQuery;
import org.hibernate.tool.language.internal.MetamodelJsonSerializerImpl;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Hibernate Assistant MCP Server implementation.
 */
@Singleton
public class HibernateAssistantMCP {
	private final StatelessSession session;
	private final HibernateAssistantLC4J assistant;

	@Inject
	public HibernateAssistantMCP(StatelessSession session, HibernateAssistantLC4J assistant) {
		this.session = session;
		this.assistant = assistant;
	}

	@Tool(name = "hibernate_get_metamodel", description = "Retrieve a textual (JSON) representation of the Hibernate Metamodel, i.e. the entities, " +
			"properties and relationships defined in the persistence layer, that can be used to access the database.")
	public String getMetamodel() {
		return MetamodelJsonSerializerImpl.INSTANCE.toString( session.getFactory().getMetamodel() );
	}

	@Tool(name = "hibernate_query", description = "Execute a query against the database using the Hibernate Assistant. " +
			"Pass a natural language message and get a natural language response based on the results of the query.")
	public String executeQuery(@ToolArg(description = "Natural language query to execute") String query) {
		return assistant.executeQuery( query, session );
	}

	/**
	 * Creates a {@link SelectionQuery} by providing the specified natural language {@code message} to the LLM
	 * and executes it, returning a JSON representation of the resulting data.
	 *
	 * @param message the natural language prompt
	 *
	 * @return a JSON representation of the query results, or an error message if the query could not be created
	 */
	@Tool(name = "hibernate_query_json", description = "Execute a query against the database using the Hibernate Assistant. " +
			"Pass a natural language message and get a JSON representation of the resulting data.")
	public String executeQueryToJson(@ToolArg(description = "Natural language query to execute") String message) {
		try {
			final SelectionQuery<?> query = assistant.createAiQuery( message, session );
			return assistant.executeQueryToJson( query, session );
		}
		catch (Exception e) {
			return "Error executing query: " + e.getMessage();
		}
	}
}
