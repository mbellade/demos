package org.hibernate.demo.assistant;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.SelectionQuery;

import org.jboss.logging.Logger;

import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hibernate.demo.assistant.HibernateAssistantLC4J.extractHql;

@Singleton
public class HibernateContentRetriever implements ContentRetriever {
	private static final Logger log = Logger.getLogger( HibernateContentRetriever.class );

	@Inject
	HibernateAssistantLC4J assistant;

	@Inject
	Session session;

	/**
	 * Recommended default prompt template to use in conjuction with {@link HibernateContentRetriever},
	 * simply provide this to {@link dev.langchain4j.rag.content.injector.DefaultContentInjector}.
	 */
	public static final PromptTemplate INJECTOR_PROMPT_TEMPLATE = PromptTemplate.from(
			"""
					Answer the original question:
					{{userMessage}}
					
					Based strictly on the following data information:
					{{contents}}
					
					Do not create an HQL query, nor suggest any further steps to take, just answer the original question in natural language."""
	);

	public HibernateContentRetriever(HibernateAssistantLC4J assistant) {
		this.assistant = ensureNotNull( assistant, "Metamodel" );
	}

	@Override
	public List<Content> retrieve(Query naturalLanguageQuery) {
		final String result;
		try {
			final String response = assistant.queryPrompt( naturalLanguageQuery.text(), session, null );
			final String hql = extractHql( response );
			if ( hql != null ) {
				log.debugf( "Extracted HQL: %s", hql );
				final SelectionQuery<Object> aiQuery = session.createSelectionQuery( hql, Object.class );
				result = assistant.executeQueryToJson( aiQuery, session );
			}
			else {
				log.debugf( "No HQL extracted from model response" );
				result = response;
			}

		}
		catch (Exception e) {
			log.errorf( e, "Error executing query: ", e.getMessage() );
			return emptyList();
		}

		return result == null ? emptyList() : singletonList( Content.from( result ) );
	}
}
