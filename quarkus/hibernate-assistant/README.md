# Hibernate Assistant and Quarkus LangChain4j demo

This project contains a simple demo Quarkus application that demonstrates how to use Hibernate + LangChain4j to interact with a database through natural language.

## Project structure

- [src/](src/) — the Quarkus application source code
  - [org/domain/](src/main/java/org/domain/) — entity classes (Company, Employee, Project, Address)
  - [org/hibernate/demo/assistant/](src/main/java/org/hibernate/demo/assistant/) — HibernateAssistantLC4J, HibernateContentRetriever (RAG), HibernateAssistantMCP
  - [AssistantResource.java](src/main/java/org/hibernate/demo/AssistantResource.java) — REST endpoints (`/assistant/json`, `/assistant/ask`)
- [presentation/](src/main/resources/META-INF/resources/presentation/) — reveal.js slides for the talk "Talk to Your Data"
  - [slides.html](src/main/resources/META-INF/resources/presentation/slides.html) — the presentation
  - [images/](src/main/resources/META-INF/resources/presentation/images/) — diagrams and screenshots used in the slides
  - [vendor/](src/main/resources/META-INF/resources/presentation/vendor/) — offline reveal.js and highlight.js dependencies

When the Quarkus app is running, the slides are available at `http://localhost:8080/presentation/slides.html`.

![Chatbot screenshot](src/main/resources/META-INF/resources/img/chatbot_screenshot.png)