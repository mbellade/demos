package org.hibernate.demo.assistant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.service.AiServices;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.metamodel.Metamodel;
import org.hibernate.SharedSessionContract;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.query.SelectionQuery;
import org.hibernate.tool.language.HibernateAssistant;
import org.hibernate.tool.language.internal.MetamodelJsonSerializerImpl;
import org.hibernate.tool.language.internal.ResultsJsonSerializerImpl;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.langchain4j.model.chat.request.ResponseFormatType.JSON;
import static org.hibernate.demo.assistant.HibernateContentRetriever.INJECTOR_PROMPT_TEMPLATE;

/**
 * Implementation of {@link HibernateAssistant} based on <a href="https://docs.langchain4j.dev/">LangChain4j</a> APIs,
 * designed to be available as a CDI bean within a Quarkus application.
 * The CDI context must contain a {@link ChatModel} instance that will be used to interact with the LLMs.
 * Also, a {@link ChatMemoryProvider} should be present as well to provide a {@link ChatMemory}
 * instance to store the conversation history.
 * <p>
 * It is highly recommended to use a {@link ChatModel} that supports
 * <a href="https://docs.langchain4j.dev/tutorials/structured-outputs#json-schema">JSON Schema</a>
 * to improve the chances of extracting a valid HQL query from the LLM's responses. Note that this requires
 * enabling <a href="https://docs.langchain4j.dev/tutorials/structured-outputs#json-schema">JSON Schema</a>
 * support on the provided chat model.
 */
@ApplicationScoped
public class HibernateAssistantLC4J implements HibernateAssistant {
	private static final Logger log = Logger.getLogger(HibernateAssistantLC4J.class);

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final Pattern HQL_PATTERN = Pattern.compile("(?i)\\bSELECT\\b.*?(?:;|\\n|$)");

	private static final PromptTemplate METAMODEL_PROMPT_TEMPLATE = PromptTemplate.from(
			"""
					You are an expert in writing Hibernate Query Language (HQL) queries.
					You have access to a entity model with the following structure:
					
					{{it}}
					
					If a user asks a question that can be answered by querying this model, generate an HQL SELECT query.
					The query must not include any input parameters.
					Do not output anything else aside from a valid HQL statement, no explanation, and do not put the query in backticks or code blocks.
					""");

	private final ChatModel chatModel;
	private final ChatMemory chatMemory;
	private final Metamodel metamodel;
	private final Instance<HibernateContentRetriever> contentRetrieverInstance;
	private final SystemMessage metamodelPrompt;
	private volatile HibernateAssistantRag ragAssistant;

	@Inject
	public HibernateAssistantLC4J(
			// Injected ChatModel and ChatMemoryProvider from the Quarkus LC4J extension
			ChatModel chatModel,
			ChatMemoryProvider memoryProvider,
			// Injected Metamodel from the Hibernate ORM extension
			Metamodel metamodel,
			// Lazy injection to break circular dependency
			Instance<HibernateContentRetriever> contentRetrieverInstance) {
		this.chatModel = chatModel;
		this.chatMemory = memoryProvider.get("hibernate-assistant-lc4j");
		this.metamodel = metamodel;
		this.contentRetrieverInstance = contentRetrieverInstance;

		this.metamodelPrompt = getMetamodelPrompt(metamodel);
		log.debugf("Metamodel prompt: %s", metamodelPrompt.text());
		chatMemory.add(metamodelPrompt);
	}

	private static SystemMessage getMetamodelPrompt(Metamodel metamodel) {
		// Serialize metamodel to JSON and create system prompt
		return METAMODEL_PROMPT_TEMPLATE.apply(MetamodelJsonSerializerImpl.INSTANCE.toString(metamodel))
				.toSystemMessage();
	}

	@Override
	public void clear() {
		this.chatMemory.clear();
		this.chatMemory.add(metamodelPrompt);
	}

	public ChatMemory getChatMemory() {
		return chatMemory;
	}

	public <T> String queryPrompt(String message, Class<T> resultType) {
		final ManagedDomainType<T> managedType = resultType != null && resultType != Object.class && !resultType.isInterface() ?
				((JpaMetamodel) metamodel).findManagedType(resultType) :
				null;
		if (managedType != null) {
			message += "\nThe query must return objects of type \"" + managedType.getTypeName() + "\".";
		}

		final UserMessage userMessage = UserMessage.from(message);
		chatMemory.add(userMessage);

		final ChatRequest chatRequest = ChatRequest.builder()
				.messages(chatMemory.messages())
				.responseFormat(hqlResponseFormat())
				.build();

		final ChatResponse chatResponse = chatModel.chat(chatRequest);

		return chatResponse.aiMessage().text();
	}

	@Override
	public <T> SelectionQuery<T> createAiQuery(String message, SharedSessionContract session, Class<T> resultType) {
		final String response = queryPrompt(message, resultType);
		final String hql = extractHql(response);

		log.infof("Generated HQL: %s", hql);

		return session.createSelectionQuery(hql, resultType);
	}

	/**
	 * Extracts an HQL query from the model response, using the configured extraction method.
	 *
	 * @param response the model response
	 * @return the extracted HQL query, or {@code null} if no query could be extracted
	 */
	public String extractHql(String response) {
		try {
			final HqlHolder hqlHolder = OBJECT_MAPPER.readValue(response, HqlHolder.class);
			return hqlHolder.hql();
		} catch (JsonProcessingException e) {
			log.warn("Failed to extract HQL from JSON format");
		}
		return extractHqlFromText(response);
	}

	private static String extractHqlFromText(String response) {
		// Try our best to extract valid HQL from text
		final Matcher matcher = HQL_PATTERN.matcher(response);
		if (matcher.find()) {
			return matcher.group().trim();
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Note that this requires the assistant's {@link ChatMemory} to be able to store at least 3 messages:
	 * the base mapping model system message, the initial request to create the query and the String
	 * representation of the query results.
	 * <p>
	 * You can also use this RAG-like (retrieval-augmented generation) functionality through the
	 * {@link HibernateContentRetriever} that directly plugs into LangChain4J's {@link RetrievalAugmentor} APIs.
	 *
	 * @param message the natural language request
	 * @return a natural language response based on the results of the query
	 */
	@Override
	public String executeQuery(String message, SharedSessionContract session) {
		return getRagAssistant().chat(message);
	}

	private HibernateAssistantRag getRagAssistant() {
		if (ragAssistant == null) {
			final RetrievalAugmentor rag = DefaultRetrievalAugmentor.builder()
					.contentRetriever(contentRetrieverInstance.get())
					.contentInjector(DefaultContentInjector.builder()
							.promptTemplate(INJECTOR_PROMPT_TEMPLATE)
							.build())
					.build();
			ragAssistant = AiServices.builder(HibernateAssistantRag.class)
					.chatModel(chatModel)
					.chatMemoryProvider(_ -> chatMemory)
					.retrievalAugmentor(rag)
					.build();
		}
		return ragAssistant;
	}

	interface HibernateAssistantRag {
		String chat(String userMessage);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Note that this requires the assistant's {@link ChatMemory} to be able to store at least 3 messages:
	 * the base mapping model system message, the initial request to create the query and the textual
	 * representation of the query results.
	 */
	@Override
	public String executeQuery(SelectionQuery<?> query, SharedSessionContract session) {
		final String result;
		try {
			result = executeQueryToJson(query, session);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		final String prompt = "The query returned the following data (in JSON format):\n" + result +
				// this seems to be needed, otherwise with some models we just get an HQL query
				"\nBased on the data above, answer the original question in plain natural language. " +
				"Do not create a query or suggest further steps to take!";

		log.debugf("Query result prompt: %s", prompt);

		final UserMessage userMessage = UserMessage.from(prompt);
		chatMemory.add(userMessage);

		final ChatRequest chatRequest = ChatRequest.builder()
				.messages(chatMemory.messages())
				.build();

		final ChatResponse chatResponse = chatModel.chat(chatRequest);
		return chatResponse.aiMessage().text();
	}

	/**
	 * Executes the given {@link SelectionQuery} as a {@link org.hibernate.query.SelectionQuery}, and provides
	 * a string representation of the response. The string will be created based on Hibernate's
	 * knowledge of the domain model, but it will not print the entire object tree since that
	 * would cause circularity problems. This is a best-effort attempt at providing a useful
	 * string-representation based on data, mainly used to pass it back to a {@link ChatModel}
	 * like in {@link #executeQuery(SelectionQuery, SharedSessionContract)}.
	 * <p>
	 * If you wish to execute the query manually and obtain the structured results yourself,
	 * you should use {@link SelectionQuery}'s direct execution methods, e.g. {@link SelectionQuery#getResultList()}
	 * or {@link SelectionQuery#getSingleResult()}.
	 *
	 * @param query   the AI query to execute
	 * @param session the session in which to execute the query
	 * @return a natural language response based on the results of the query
	 */
	public <T> String executeQueryToJson(SelectionQuery<T> query, SharedSessionContract session) throws IOException {
		final List<? extends T> resultList = query.getResultList();
		return new ResultsJsonSerializerImpl((SessionFactoryImplementor) session.getFactory()).toString(
				resultList,
				query
		);
	}

	/**
	 * Simple holder used for HQL extraction when using structured JSON responses.
	 */
	record HqlHolder(String hql) {
	}

	private static ResponseFormat hqlResponseFormat() {
		return ResponseFormat.builder().type(JSON) // type can be either TEXT (default) or JSON
				.jsonSchema(JsonSchema.builder().name("HQL")
						.rootElement(JsonObjectSchema.builder()
								.addStringProperty("hql")
								.required("hql")
								.build()).build()).build();
	}
}
