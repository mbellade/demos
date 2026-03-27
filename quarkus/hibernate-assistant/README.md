# Hibernate Assistant and Quarkus LangChain4j demo

This project contains a simple demo Quarkus application that demonstrates how to use Hibernate + LangChain4j to interact with a database through natural language.

## Project structure

- [src/](src/) — the Quarkus application source code
  - [org/domain/](src/main/java/org/domain/) — entity classes (Company, Employee, Project, Address)
  - [org/hibernate/demo/assistant/](src/main/java/org/hibernate/demo/assistant/) — HibernateAssistantLC4J, HibernateContentRetriever (RAG), HibernateAssistantMCP
  - [AssistantResource.java](src/main/java/org/hibernate/demo/AssistantResource.java) — REST endpoints (`/assistant/json`, `/assistant/ask`)

## References

- [Hibernate ORM](https://hibernate.org/orm/) — [GitHub](https://github.com/hibernate/hibernate-orm)
- [Quarkus](https://quarkus.io/) — [GitHub](https://github.com/quarkusio/quarkus)
- [LangChain4j](https://docs.langchain4j.dev/) — [GitHub](https://github.com/langchain4j/langchain4j)

- ![Chatbot screenshot](src/main/resources/META-INF/resources/img/chatbot_screenshot.png)
