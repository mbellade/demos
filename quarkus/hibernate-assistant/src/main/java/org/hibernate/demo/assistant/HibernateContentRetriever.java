package org.hibernate.demo.assistant;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
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

	private static final int MAX_RETRIES = 1;

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
		String hqlQuery = null;
		String errorMessage = null;

		int attemptsLeft = MAX_RETRIES + 1;
		while (attemptsLeft > 0) {
			attemptsLeft--;

			try {
				hqlQuery = generateHqlQuery(naturalLanguageQuery, hqlQuery, errorMessage);

				log.infof("Generated HQL query: %s", hqlQuery);

				// Execute the query, extracting the results as JSON
				SelectionQuery<Object> aiQuery = session.createSelectionQuery(hqlQuery, Object.class);
				String json = assistant.executeQueryToJson(aiQuery, session);

				// Wrap the results and inject them into the RAG pipeline
				Content content = format(json, hqlQuery);
				return singletonList(content);
			} catch (Exception e) {
				errorMessage = e.getMessage();
				if (attemptsLeft > 0) {
					log.warnf("HQL execution failed, retrying (attempts left: %d): %s", attemptsLeft, errorMessage);
				} else {
					log.errorf("HQL execution failed, no retries left: %s", errorMessage);
				}
			}
		}

		String result = String.format(
				"""
						An error occurred while executing the query.
						HQL: %s
						Error message: %s
						""", hqlQuery, errorMessage
		);

		Content error = Content.from(result);
		return singletonList(error);
	}

	protected String generateHqlQuery(
			Query naturalLanguageQuery,
			String previousHqlQuery,
			String previousErrorMessage) {
		if (previousHqlQuery != null && previousErrorMessage != null) {
			assistant.getChatMemory().add(AiMessage.from(previousHqlQuery));
			assistant.getChatMemory().add(UserMessage.from(previousErrorMessage));
		}

		final String response = assistant.queryPrompt(naturalLanguageQuery.text(), null);
		return assistant.extractHql(response);
	}

	private static Content format(String result, String hqlQuery) {
		return Content.from(String.format("Result of executing '%s':\n%s", hqlQuery, result));
	}
}
