package org.hibernate.demo.assistant;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.hibernate.StatelessSession;
import org.hibernate.query.SelectionQuery;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;

@Singleton
public class HibernateContentRetriever implements ContentRetriever {
	private static final Logger log = Logger.getLogger(HibernateContentRetriever.class);

	private final HibernateAssistantLC4J assistant;
	private final StatelessSession session;

	/**
	 * Recommended default prompt template to use in conjunction with {@link HibernateContentRetriever},
	 * simply provide this to {@link DefaultContentInjector}.
	 */
	public static final PromptTemplate INJECTOR_PROMPT_TEMPLATE = PromptTemplate.from(
			"""
					Answer the original question:
					{{userMessage}}
					
					Based strictly on the following data:
					{{contents}}
					
					Do not create an HQL query, nor suggest any further steps to take, just answer the original question in natural language."""
	);

	@Inject
	public HibernateContentRetriever(HibernateAssistantLC4J assistant, StatelessSession session) {
		this.assistant = assistant;
		this.session = session;
	}

	@Override
	public List<Content> retrieve(Query naturalLanguageQuery) {
		String hql = null;
		TextSegment result;
		try {
			final String response = assistant.queryPrompt(naturalLanguageQuery.text(), null);
			hql = assistant.extractHql(response);
			if (hql != null) {
				log.infof("Generated HQL: %s", hql);
				final SelectionQuery<Object> aiQuery = session.createSelectionQuery(hql, Object.class);
				final String json = assistant.executeQueryToJson(aiQuery, session);
				result = TextSegment.from(json, Metadata.from(Map.of("HQL", hql)));
			} else {
				log.debugf("No HQL extracted from model response");
				result = TextSegment.from(response);
			}
		} catch (Exception e) {
			final String message = e.getMessage();
			log.errorf(e, "Error executing query: ", message);
			result = TextSegment.from(String.format(
					"""
							An error occurred while executing the query.
							HQL: %s
							Error message: %s
							""", hql, message
			));
		}

		return singletonList(Content.from(result));
	}
}
