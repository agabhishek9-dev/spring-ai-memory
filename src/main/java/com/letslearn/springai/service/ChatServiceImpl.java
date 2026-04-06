package com.letslearn.springai.service;

import com.letslearn.springai.entity.Tut;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger logger = Logger.getLogger(ChatServiceImpl.class.getName());

    /*
     * ChatClient is the main Spring AI abstraction used to send prompts
     * to the configured LLM and read the response.
     *
     * Think of it as the entry point for all chat-based AI interactions.
     */
    private final ChatClient chatClient;

    /*
     * These are sample hardcoded values used to demonstrate static prompt usage.
     *
     * They are useful for learning the API, but in real applications
     * most prompt data usually comes dynamically from the user request.
     */
    private final String prompt = "Who is Sachin Tendulkar ?";
    private final String systemInstruction = "As a cricket expert, answer the question.";

    /*
     * These Resource fields point to external prompt template files
     * stored under src/main/resources/prompts/.
     *
     * Spring injects them as Resource objects, and Spring AI can use them
     * directly inside prompt-building APIs. Spring AI supports using
     * Resource-backed prompt templates instead of hardcoded strings. [web:14][web:29][web:35]
     */
    @Value("classpath:/prompts/user-message.st")
    private Resource userMessage;

    @Value("classpath:/prompts/system-message.st")
    private Resource systemMessage;

    public ChatServiceImpl(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /*
     * --------------------------------------------------------------
     * CONCEPT 1: Build ChatClient locally from ChatClient.Builder
     * --------------------------------------------------------------
     *
     * This is an alternative constructor style.
     *
     * If Spring injects ChatClient.Builder, you can build the ChatClient
     * manually inside the service.
     *
     * Use this approach when:
     * - you want service-specific ChatClient configuration
     * - you do not want to share one common ChatClient bean
     *
     * If you already define a ChatClient bean in a @Configuration class,
     * injecting ChatClient directly is cleaner and more reusable.
     */
    // public ChatServiceImpl(ChatClient.Builder builder) {
    //     this.chatClient = builder.build();
    // }

    /*
     * --------------------------------------------------------------
     * CONCEPT 2: Passing static user + system prompt
     * --------------------------------------------------------------
     *
     * This method demonstrates the fluent API style where both the user
     * message and the system instruction are supplied directly in code.
     *
     * Useful when:
     * - you are learning Spring AI
     * - the prompt is fixed
     * - you want a short and readable chain
     *
     * Important note:
     * This method currently ignores the incoming 'query' parameter and uses
     * the hardcoded field 'prompt' instead. That is okay for demonstration,
     * but in a real project you would usually use the method parameter.
     */
    @Override
    public String chatPassingStaticUserAndSystemPrompt(String query) {

        String content = chatClient
                .prompt()                    // Start building a new chat request
                .user(prompt)                // Add the user message sent to the model
                .system(systemInstruction)   // Add system instruction that guides model behavior
                .call()                      // Execute the request against the configured model
                .content();                  // Extract only the generated text response

        return content;
    }

    /*
     * --------------------------------------------------------------
     * CONCEPT 3: Passing a Prompt object
     * --------------------------------------------------------------
     *
     * Prompt is a richer abstraction than a raw String.
     * It can hold one or more messages and can later be extended with
     * additional prompt-related configuration.
     *
     * Here, a Prompt object is created once using the hardcoded prompt text.
     */
    private final Prompt prompt1 = new Prompt(prompt);

    /*
     * This method demonstrates how to pass a pre-built Prompt object.
     *
     * It also shows the longer response extraction path:
     * .chatResponse() -> .getResult() -> .getOutput() -> .getText()
     *
     * Use this style when you want to understand the structure of the
     * full response object instead of using the shorter .content() method.
     */
    @Override
    public String chatPassingPromptObject(String query) {

        String text = chatClient
                .prompt(prompt1)     // Send an already-created Prompt object
                .call()              // Invoke the AI model
                .chatResponse()      // Get the complete response wrapper, not just plain text
                .getResult()         // Extract the main result from the response
                .getOutput()         // Extract the generated output message
                .getText();          // Read only the text content from that output

        return text;
    }

    /*
     * Revision note:
     *
     * .content()
     * - shortcut method
     * - best when you only need plain generated text
     *
     * .chatResponse()
     * - detailed method
     * - useful when you need metadata or want to inspect the complete response
     */

    /*
     * --------------------------------------------------------------
     * CONCEPT 4: Map AI response into one Java object
     * --------------------------------------------------------------
     *
     * This pattern is used when the model returns structured output
     * that matches the fields of a Java class such as Tut.
     *
     * Spring AI structured output can map the response directly into
     * a target class using .entity(Tut.class). [web:22][web:27]
     */
    @Override
    public Tut chatMapResponseToSingleEntityObject(String query) {
        logger.info("Service handling single entity response...");

        // Wrap the incoming user text inside a Prompt object.
        Prompt prompt1 = new Prompt(query);

        Tut tutorial = chatClient
                .prompt(prompt1)      // Attach prompt to the request
                .call()               // Execute the request
                .entity(Tut.class);   // Map structured response directly into Tut object

        return tutorial;
    }

    /*
     * --------------------------------------------------------------
     * CONCEPT 5: Map AI response into List<Tut>
     * --------------------------------------------------------------
     *
     * This pattern is used when the model should return multiple
     * structured objects instead of one object.
     *
     * We use ParameterizedTypeReference<List<Tut>>() {}
     * because Java erases generic type information at runtime.
     *
     * This helper preserves the full generic type so Spring AI knows
     * it must deserialize the response into a List of Tut objects,
     * not just a raw List. [web:12][web:22][web:27]
     */
    @Override
    public List<Tut> chatMapResponseToList(String query) {
        logger.info("Service handling list of entity response...");

        Prompt prompt1 = new Prompt(query);

        List<Tut> tutorials = chatClient
                .prompt(prompt1)   // Prepare the request using a Prompt object
                .call()            // Send request to the LLM
                .entity(new ParameterizedTypeReference<List<Tut>>() {
                });                // Map structured output into a generic List<Tut>

        return tutorials;
    }

    /*
     * --------------------------------------------------------------
     * CONCEPT 6: Simple fluent API chain
     * --------------------------------------------------------------
     *
     * This is one of the most common and cleanest Spring AI patterns.
     *
     * Flow:
     * 1. Convert user input into Prompt
     * 2. Send it to the model
     * 3. Read plain text response
     */
    @Override
    public String chatFluentAPIChain(String query) {
        logger.info("Processing query in service layer...");

        Prompt prompt = new Prompt(query);

        String content = chatClient
                .prompt(prompt)   // Attach Prompt object to the request
                .call()           // Invoke the model
                .content();       // Return only the generated text

        return content;
    }

    /*
     * --------------------------------------------------------------
     * CONCEPT 7: Simple fluent API with placeholders and params
     * --------------------------------------------------------------
     *
     * This method demonstrates inline prompt templating inside the fluent API.
     *
     * The user message text contains a placeholder: {query}
     * Then .param("query", query) replaces that placeholder dynamically.
     *
     * This is easier to maintain than string concatenation because the
     * template remains readable and the dynamic value is injected separately.
     * Spring AI prompt templates support placeholder substitution for this use case. [web:14][web:30]
     */
    @Override
    public String chatUsingSimpleFluentAPI(String query) {

        String queryTemplate =
                "As an expert coder, always write program in java. Reply to this question : {query}";

        String content = chatClient
                .prompt()                                           // Start building a new prompt request
                .user(user -> user
                        .text(queryTemplate)                        // Define user prompt template text
                        .param("query", query))                    // Replace {query} with actual runtime value
                .call()                                             // Send request to the model
                .content();                                         // Extract plain text response

        return content;
    }

    /*
     * --------------------------------------------------------------
     * CONCEPT 8: Prompt templating using PromptTemplate
     * --------------------------------------------------------------
     *
     * Here, prompt construction happens in two explicit steps:
     * 1. Create a PromptTemplate
     * 2. Render the template into a final String using a map
     *
     * This is useful when you want clearer separation between:
     * - template definition
     * - template rendering
     * - prompt execution
     *
     * Spring AI uses template rendering for prompt creation and supports
     * rendering via render(Map<String, Object>). [web:14][web:30]
     */
    @Override
    public String chatTemplateUsingPromptTemplating1(String query) {

        // Step 1: Define the user prompt template with placeholders.
        String queryTemplate = "What is {techName} ? Tell me example of {exampleName}";

        PromptTemplate promptTemplate = PromptTemplate
                .builder()
                .template(queryTemplate)
                .build();

        // Step 2: Replace placeholders with actual values.
        String renderedMessage = promptTemplate.render(Map.of(
                "techName", "Spring",
                "exampleName", "Spring Boot"
        ));

        // Step 3: Wrap the rendered text into a Prompt object.
        Prompt prompt = new Prompt(renderedMessage);

        // Step 4: Send the final prompt to the model and read plain text output.
        return chatClient
                .prompt(prompt)
                .call()
                .content();
    }

    /*
     * --------------------------------------------------------------
     * CONCEPT 9: Prompt templating with separate system + user messages
     * --------------------------------------------------------------
     *
     * This concept is more structured than the previous one.
     *
     * Instead of creating one final String, it creates:
     * - one system message
     * - one user message
     *
     * Then both are combined into a Prompt.
     *
     * Use this style when you want clean role separation:
     * - system message defines behavior/instructions
     * - user message contains the actual question
     *
     * Spring AI supports SystemPromptTemplate and PromptTemplate for
     * building role-specific messages before combining them into a Prompt. [web:14][web:29]
     */
    @Override
    public String chatTemplateUsingPromptTemplating2(String query) {

        // Step 1: Create a system prompt template for instruction/behavior.
        SystemPromptTemplate systemPromptTemplate = SystemPromptTemplate
                .builder()
                .template("You are helpful coding assistant. Expert in coding")
                .build();

        // Step 2: Convert system template into a system message object.
        var systemMessage = systemPromptTemplate.createMessage();

        // Step 3: Create a user prompt template with placeholders.
        PromptTemplate userPromptTemplate = PromptTemplate
                .builder()
                .template("What is {techName} ? Tell me example of {exampleName}")
                .build();

        // Step 4: Render the user template into a user message object using parameters.
        var userMessage = userPromptTemplate.createMessage(Map.of(
                "techName", "Spring",
                "exampleName", "Spring Boot"
        ));

        /*
         * Step 5: Combine both messages into one Prompt.
         *
         * Why createMessage()?
         * Because Prompt expects Message objects here, not raw strings.
         */
        Prompt prompt = new Prompt(systemMessage, userMessage);

        return chatClient
                .prompt(prompt)
                .call()
                .content();
    }

    /*
     * --------------------------------------------------------------
     * CONCEPT 10: Prompt templating directly in ChatClient fluent API
     * --------------------------------------------------------------
     *
     * This combines the benefits of:
     * - separate system and user roles
     * - parameter substitution
     * - compact fluent API style
     *
     * Compared to PromptTemplate-based code, this is shorter and often
     * easier to read for small prompt-building scenarios.
     */
    @Override
    public String chatTemplateUsingFluentAPI(String query) {

        return chatClient
                .prompt()   // Start building a new request
                .system(system -> system
                        .text("You are helpful coding assistant. Expert in coding")) // Define system instruction
                .user(user -> user
                        .text("What is {techName} ? Tell me example of {exampleName}") // User template with placeholders
                        .param("techName", "Java")                                     // Replace first placeholder
                        .param("exampleName", "Collection framework in java"))         // Replace second placeholder
                .call()    // Execute the request
                .content();// Read plain text output
    }

    /*
     * --------------------------------------------------------------
     * CONCEPT 11: Load prompt templates from external files
     * --------------------------------------------------------------
     *
     * This is one of the best approaches when prompts become large,
     * reusable, or hard to maintain inside Java code.
     *
     * Here, the prompt text is stored in:
     * - classpath:/prompts/user-message.st
     * - classpath:/prompts/system-message.st
     *
     * Spring injects them as Resource objects, and the fluent API can
     * read them directly via text(Resource). Spring AI supports
     * Resource-based prompt text for this purpose. [web:14][web:35]
     *
     * Benefit:
     * Your Java code stays clean while prompt wording lives in dedicated files.
     */
    @Override
    public String chatUsingExternalPromptFile(String query) {

        return chatClient
                .prompt()   // Start a new request
                .system(system -> system
                        .text(systemMessage))                    // Load system prompt text from external file
                .user(user -> user
                        .text(userMessage)                       // Load user prompt text from external file
                        .param("concept", "CAP theorem"))       // Fill placeholder inside the external template
                .call()                                         // Execute the request
                .content();                                     // Extract plain text response
    }
}