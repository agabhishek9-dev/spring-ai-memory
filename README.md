Concept	                    What it does
builder.build()	            Creates a ChatClient, optionally with defaults like system prompt or model options.
.prompt().user().system()	Builds a request inline using fluent API style.
new Prompt(query)	        Creates a Prompt object explicitly, useful when prompt structure becomes richer.
.call().content()	        Executes the request and returns only plain text.
.call().chatResponse()	    Executes the request and gives the full response object for deeper inspection.
.entity(Tut.class)	        Maps structured model output into one Java object.
.entity(new ParameterizedTypeReference<List<Tut>>() {})	Maps structured model output into a generic list while preserving element type information. 


# Spring AI Demo – Detailed Guide

An example Spring Boot project that demonstrates Spring AI’s `ChatClient` API, structured outputs, prompt templating, and different wiring patterns.  
Goal: help you **revise and understand** Spring AI by making each method and API chain self‑documenting.

---

## 1. Project Goals

This project helps you learn:

- How to integrate Spring AI with OpenAI using `ChatClient`.
- Different ways to send prompts:
    - Simple strings
    - `Prompt` and `PromptTemplate`
    - External prompt files
- How to return responses:
    - Plain text
    - One Java object (`Tut.class`)
    - List of objects (`List<Tut>`)
- How to keep controllers thin and services rich with AI logic.
- How to layout and comment code so it’s easy to revise later.

---

## 2. Prerequisites

- Java 17+
- Maven (or Gradle if you adapt)
- OpenAI API key
- `spring-boot-starter-web` + Spring AI dependencies already in `pom.xml`

Set your API key as an environment variable:

```bash
export OPENAI_API_KEY="your-openai-api-key"
```

or in your IDE run configuration as:

```bash
-DOPENAI_API_KEY=your-openai-api-key
```

---

## 3. Project Structure
src/main/java
├── com.letslearn.springai
│ ├── controller
│ │ └── ChatController.java # REST endpoints (GET requests)
│ ├── entity
│ │ └── Tut.java # DTO for structured output
│ ├── service
│ │ ├── ChatService.java # Service interface
│ │ └── ChatServiceImpl.java # All AI logic (ChatClient calls)
│ └── config
│ └── AiConfig.java # ChatClient bean with default options
src/main/resources
├── application.properties # Port, OpenAI key, defaults
├── prompts
│ ├── user-message.st # External user prompt template
│ └── system-message.st # External system prompt template


---

## 4. Core Concepts Demystified

### 4.1. `ChatClient` vs `ChatModel`

- `ChatModel` is the underlying LLM abstraction (e.g., OpenAI GPT‑4o‑mini).
- `ChatClient` is a higher‑level API that:
    - Accepts prompts
    - Sends them to the model
    - Returns responses (text, metadata, or mapped objects)
- You usually inject `ChatClient` into services instead of talking to `ChatModel` directly.

### 4.2. `defaultSystem` and `defaultOptions`

In `AiConfig.java`:

```java
@Bean
public ChatClient chatClient(ChatClient.Builder builder) {
    return builder
        .defaultSystem("You are helpful assistant")
        .defaultOptions(OpenAiChatOptions.builder()
            .model("gpt-4o-mini")
            .temperature(0.3)
            .maxTokens(100)
            .build())
        .build();
}
```

- `defaultSystem` gives global behavior rules (model identity).
- `defaultOptions` sets default model parameters (model name, temperature, max tokens).
- These defaults apply to all requests unless you override them at request level.

---

## 5. Structured Output Concepts

### 5.1. Single object mapping (`Tut.class`)

- Flow:
    - Prompt asks for JSON‑like structured tutorial.
    - Spring AI maps fields `"title"`, `"content"`, `"createdYear"` into `Tut` fields.
    - Service returns `Tut` → controller returns `ResponseEntity<Tut>`.
- Spring AI uses structured output support to deserialize the model’s response into the target class.

### 5.2. List mapping (`List<Tut>`)

- Same idea, but the model returns multiple objects.
- Java erases generic types at runtime, so Spring AI cannot know `List<Tut>.class`.
- `ParameterizedTypeReference<List<Tut>>()` preserves the generic type, letting Spring AI deserialize into a proper `List<Tut>`.

---

## 6. Prompt Templating Styles

### 6.1. Inline templating in fluent API

```java
chatClient
    .prompt()
    .user(user -> user
        .text("Reply to this question : {query}")
        .param("query", query)
    )
    .call()
    .content();
```

- `{query}` is a placeholder.
- `.param("query", query)` replaces it at runtime.
- This is compact and readable for simple prompts.

### 6.2. `PromptTemplate` and `SystemPromptTemplate`

```java
PromptTemplate userTemplate = PromptTemplate
    .builder()
    .template("What is {techName} ? Example of {exampleName}")
    .build();

String rendered = userTemplate.render(Map.of(
    "techName", "Spring",
    "exampleName", "Spring Boot"
));
```

- `PromptTemplate` is a reusable template object.
- `render(...)` replaces placeholders with actual values.
- Useful when you want explicit separation between template and rendering.

### 6.3. System + user prompt separation

- `SystemPromptTemplate` for instructions (e.g., behavior rules).
- `PromptTemplate` for user message.
- Combine into `Prompt(systemMessage, userMessage)` so the model sees distinct roles.

### 6.4. External prompt files (`/prompts/*.st`)

```java
@Value("classpath:/prompts/user-message.st")
private Resource userMessage;

@Value("classpath:/prompts/system-message.st")
private Resource systemMessage;
```

- Store long or reusable prompt text in files.
- Inject them as `Resource` and pass to `.text(Resource)` in the fluent API.
- Keeps Java code clean and makes prompt wording easier to tweak.

---

## 7. REST Endpoints Summary

### 7.1. Plain text response

- Path: `GET /v1/chat?query=...`
- Service: `chatUsingExternalPromptFile(query)`
- Flow:
    - Controller forwards `query`.
    - Service:
        - Loads system/user templates from files.
        - Fills placeholders (e.g., `{concept}` with `"CAP theorem"`).
        - Sends prompt → gets text → returns `String`.
    - Controller wraps string in `ResponseEntity.ok(...)`.

### 7.2. Single `Tut` object

- Path: `GET /v1/chat1?query=...`
- Service: `chatMapResponseToSingleEntityObject(query)`
- Expectation:
    - Prompt: "Generate one tutorial object for ...".
    - Response mapped into `Tut.class`.

### 7.3. `List<Tut>` objects

- Path: `GET /v1/chat2?query=...`
- Service: `chatMapResponseToList(query)`
- Expectation:
    - Prompt: "Generate 5 tutorials on ...".
    - Response mapped into `List<Tut>` using `ParameterizedTypeReference`.

---

## 8. How to Run the Project

### 8.1. Build

```bash
./mvnw clean install
```

### 8.2. Run

```bash
java -jar target/springai.jar
```

The server starts at:

```bash
http://localhost:8081
```

From `application.properties`:

```properties
server.port=8081
spring.ai.openai.api-key=${OPENAI_API_KEY}
```

---

## 9. Example API Calls

### 9.1. Plain text response

```bash
curl 'http://localhost:8081/v1/chat?query=Explain%20Spring%20AI'
```

### 9.2. Single `Tut` object

```bash
curl 'http://localhost:8081/v1/chat1?query=Create%20a%20tutorial%20on%20Spring%20Boot'
```

### 9.3. List of `Tut` objects

```bash
curl 'http://localhost:8081/v1/chat2?query=Generate%203%20tutorials%20on%20Java%20collections'
```

---

## 10. Project‑Level Best Practices

### 10.1. Separation of Concerns

- Controller:
    - HTTP request/response handling.
    - Validation (via `@RequestParam`, etc.).
- Service:
    - Spring AI interaction (`ChatClient`, prompts, mappings).
    - Mapping logic (`entity(Tut.class)`, `entity(new ParameterizedTypeReference...`)).
- Config:
    - `ChatClient` wiring and defaults.

### 10.2. Dependency Injection Style

- Prefer **constructor injection** (e.g., `public ChatController(ChatService chatService)`).
- Avoid mixing `@Autowired` field injection and constructor injection in the same class.
- Constructor injection makes dependencies explicit and easier to test.

### 10.3. Naming and Commenting

- Use meaningful method names like:
    - `chatMapResponseToSingleEntityObject`
    - `chatMapResponseToList`
    - `chatUsingExternalPromptFile`
- Use block comments above methods to explain:
    - intent
    - input/output
    - example usage
- Use inline comments only for:
    - `Prompt` vs `PromptTemplate` chains
    - `.content()` vs `.chatResponse()` explanation
    - why `ParameterizedTypeReference` is needed

### 10.4. Prompt Design Tips

- Use external prompt files for long or reusable instructions.
- Use placeholders (`{concept}`, `{techName}`) and `param(...)` for dynamic values.
- Separate system and user messages when behavior rules differ from content.
- Test with `ResponseEntity` status codes (e.g., `200 OK`) and ensure your controller always returns something meaningful.

---

## 11. Revision‑Friendly Notes

Copy these into your notebook or into a separate `NOTES.md` for interviews:

- `ChatClient.prompt(...).call().content()` → simplest flow for plain text.
- `Prompt` object → explicit prompt representation.
- `PromptTemplate` → reusable template with placeholders.
- `SystemPromptTemplate` → separate system instructions.
- `entity(Tut.class)` → map AI JSON to one Java object.
- `entity(new ParameterizedTypeReference<List<Tut>>() {})` → map to generic list.
- External prompt files → keep Java code clean.
- Keep controllers thin, services rich with AI logic.
- Constructor injection good; mixed field + constructor injection bad.