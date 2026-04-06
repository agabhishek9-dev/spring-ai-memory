package com.letslearn.springai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatAdvisorServiceImpl implements ChatAdvisorService {

    /*
     * ChatClient is the main Spring AI entry point for:
     * - building prompts
     * - attaching runtime advisor parameters
     * - executing synchronous or streaming AI calls
     *
     * This client is already configured in AiConfig with:
     * - MessageChatMemoryAdvisor
     * - SimpleLoggerAdvisor
     * - optional custom or safeguard advisors
     *
     * So this service does not call advisors manually.
     * Advisors run automatically when .call() or .stream() is executed. [web:48][web:20]
     */
    private final ChatClient chatClient;

    /*
     * External system prompt file.
     *
     * This usually contains high-level instruction text such as:
     * - assistant behavior
     * - response style
     * - domain guidance
     *
     * Resource-based prompt files help keep Java code clean
     * and make prompt tuning easier. [web:14]
     */
    @Value("classpath:prompts/system-message.st")
    private Resource systemMessage;

    /*
     * External user prompt file.
     *
     * This file normally contains the user-facing prompt template
     * with placeholders such as {concept}.
     */
    @Value("classpath:prompts/user-message.st")
    private Resource userMessage;

    public ChatAdvisorServiceImpl(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /*
     * --------------------------------------------------------------
     * CONCEPT 1: Synchronous chat with conversation memory
     * --------------------------------------------------------------
     *
     * This method executes a normal non-streaming chat request and returns
     * the final response as a plain String.
     *
     * Flow:
     * 1. Start building a prompt
     * 2. Set runtime advisor parameter: conversation ID
     * 3. Load system prompt from external file
     * 4. Load user prompt template from external file
     * 5. Replace {concept} with incoming query
     * 6. Execute the request
     * 7. Return only the response text
     *
     * Important learning point:
     * Chat memory works per conversation ID.
     * By passing ChatMemory.CONVERSATION_ID = userId, the memory advisor
     * knows which conversation history should be loaded and updated for
     * this request. Spring AI shows this exact pattern in chat memory and
     * advisor examples. [web:71][web:98][web:99]
     *
     * In simple terms:
     * - same userId -> same conversation context
     * - different userId -> separate conversation history
     */
    @Override
    public String chatTemplate(String query, String userId) {

        return chatClient
                .prompt()
                // Start a new prompt request using the configured ChatClient

                .advisors(advisorSpec ->
                        advisorSpec.param(ChatMemory.CONVERSATION_ID, userId))
                /*
                 * Attach a runtime advisor parameter for this request.
                 *
                 * Why this matters:
                 * MessageChatMemoryAdvisor uses this conversation ID to
                 * retrieve and update the correct conversation history.
                 *
                 * Without a stable conversation ID, memory-based chat would
                 * not know which previous messages belong to this user/session. [web:71][web:48]
                 */

                .system(system -> system.text(this.systemMessage))
                /*
                 * Load the system prompt from an external file.
                 *
                 * This provides the assistant's instruction-level context,
                 * such as tone, role, or rules for answering.
                 */

                .user(user -> user
                        .text(this.userMessage)
                        .param("concept", query))
                /*
                 * Load the user prompt template from an external file and
                 * replace the {concept} placeholder with the runtime query.
                 *
                 * Example:
                 * if query = "Java Optional class"
                 * then {concept} becomes "Java Optional class"
                 */

                .call()
                /*
                 * Execute the synchronous chat request.
                 *
                 * At this point, configured advisors become active:
                 * - memory advisor injects previous conversation history
                 * - logger advisor logs request/response
                 * - custom advisors can inspect tokens or response metadata
                 */

                .content();
        /*
         * Return only the generated text content.
         *
         * This is the simplest return style when you do not need
         * extra metadata such as token usage or raw chat response.
         */
    }

    /*
     * --------------------------------------------------------------
     * CONCEPT 2: Streaming chat response
     * --------------------------------------------------------------
     *
     * This method demonstrates streaming output instead of waiting for
     * the full response to complete.
     *
     * Return type:
     * Flux<String>
     *
     * Meaning:
     * The response text is emitted in chunks over time as the model
     * generates it, rather than returning one final String at the end.
     *
     * Spring AI documents .stream().content() as returning Flux<String>,
     * which is useful for real-time UI updates or server-sent events. [web:20][web:100][web:102]
     */
    @Override
    public Flux<String> streamChat(String query, String userId) {

        return chatClient
                .prompt()
                // Start a new streaming prompt request
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, userId))

                .system(system -> system.text(systemMessage))
                /*
                 * Use the external system prompt file for base instructions.
                 */

                .user(user -> user
                        .text(userMessage)
                        .param("concept", query))
                /*
                 * Use the external user prompt template and inject the
                 * runtime query into the {concept} placeholder.
                 */

                .stream()
                /*
                 * Switch from normal one-shot execution to streaming mode.
                 *
                 * Instead of receiving one full response object,
                 * the model output is produced incrementally. [web:20]
                 */

                .content();
        /*
         * Return only the generated text stream as Flux<String>.
         *
         * Each emitted item is a chunk/token segment of the final
         * answer, which is useful for live rendering in clients. [web:20][web:102]
         */
    }
}

/*
        | Part                                                                            | Meaning                                                                                                                     |
        | ------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
        | .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, userId)) | Sets the conversation ID at runtime so memory advisors can fetch and update the correct conversation history. docs.spring+2 |
        | .system(system -> system.text(systemMessage))                                   | Loads the system prompt from an external file. spring                                                                       |
        | .user(user -> user.text(userMessage).param("concept", query))                   | Loads the user prompt template and replaces {concept} with the incoming query. spring                                       |
        | .call().content()                                                               | Executes a normal synchronous request and returns only plain text. spring                                                   |
        | .stream().content()                                                             | Executes a streaming request and returns Flux<String> chunks as text is generated. spring+1                                 |*/

// ChatMemory.CONVERSATION_ID links a request to the correct stored conversation history, while .stream().content() returns the response incrementally as Flux<String>.
