package org.hibernate.demo.assistant;

import org.hibernate.Session;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.spi.SqmQuery;

import org.jboss.logging.Logger;

import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@ApplicationScoped
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
					{{userMessage}}
					
					The query returned the following data (in JSON format):
					{{contents}}
					
					Answer the original question using natural language and do not create a query!"""
	);


	public HibernateContentRetriever(HibernateAssistantLC4J assistant) {
		this.assistant = ensureNotNull( assistant, "Metamodel" );
	}

	@Override
	public List<Content> retrieve(Query naturalLanguageQuery) {
		final SelectionQuery<?> aiQuery = assistant.createAiQuery( naturalLanguageQuery.text(), session );

		final String result;
		try {
			result = assistant.executeQuery( aiQuery, session );
		}
		catch (Exception e) {
			log.errorf( e, "Error executing query, hql: %s", ( (SqmQuery) aiQuery ).getQueryString() );
			return emptyList();
		}

		return result == null ? emptyList() : singletonList( Content.from( result ) );
	}
}
