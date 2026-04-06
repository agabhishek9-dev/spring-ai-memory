package com.letslearn.springai.config;

import java.util.logging.Logger;

import com.letslearn.springai.advisors.TokenPrintAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AiConfig {

    /*
     * --------------------------------------------------------------
     * CONCEPT 1: ChatMemory bean backed by JDBC
     * --------------------------------------------------------------
     *
     * This bean creates the ChatMemory implementation used by the application.
     *
     * Design here:
     * - JdbcChatMemoryRepository stores chat messages in a relational database
     * - MessageWindowChatMemory sits on top of that repository
     * - maxMessages(10) keeps only the latest 10 messages in the active window
     *
     * Important understanding:
     * The repository handles persistence, while MessageWindowChatMemory
     * controls how much history is supplied back to the model. Spring AI
     * documents JdbcChatMemoryRepository as a JDBC-based persistence layer
     * and MessageWindowChatMemory as a bounded conversation window. [web:90][web:92][web:71][web:97]
     */
    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository jdbcChatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .maxMessages(10)
                .build();
    }

    /*
     * Logger used to print startup-time diagnostic information.
     *
     * This is mainly helpful while learning or debugging configuration.
     */
    private final Logger logger = Logger.getLogger(AiConfig.class.getName());

    /**
     * Creates and configures a reusable ChatClient bean for the application.
     *
     * This bean centralizes:
     * - chat memory integration
     * - advisors
     * - system prompt defaults
     * - model defaults
     *
     * Why this is useful:
     * All services can share one ChatClient configuration instead of
     * re-declaring the same AI setup everywhere.
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {

        /*
         * Print the actual ChatMemory implementation being injected.
         *
         * Since ChatMemory is an interface, this log helps confirm the
         * concrete implementation used at runtime.
         */
        logger.info("--- ChatMemoryImplementation Class : " + chatMemory.getClass().getName());

        /*
         * Create an advisor that reads previous chat history from ChatMemory
         * and injects it into new prompts.
         *
         * This is what gives the assistant conversational continuity:
         * the model can respond based on prior turns because those turns are
         * supplied again as prompt messages. [web:71][web:92]
         */
        MessageChatMemoryAdvisor messageChatMemoryAdvisor =
                MessageChatMemoryAdvisor.builder(chatMemory).build();

        return builder

                /*
                 * ----------------------------------------------------------
                 * CONCEPT 2: defaultAdvisors(...)
                 * ----------------------------------------------------------
                 *
                 * Advisors are interceptors around the ChatClient request flow.
                 *
                 * They can:
                 * - add memory context
                 * - log request/response content
                 * - inspect usage/tokens
                 * - block unsafe input
                 *
                 * These advisors are applied automatically whenever .call()
                 * or another execution step is triggered. [web:48]
                 */
                .defaultAdvisors(

                        /*
                         * MessageChatMemoryAdvisor
                         *
                         * Adds recent conversation history from ChatMemory
                         * into the next prompt so the model can answer in context.
                         */
                        messageChatMemoryAdvisor,

//                      new TokenPrintAdvisor(),

                        /*
                         * TokenPrintAdvisor
                         *
                         * Optional custom advisor from your project.
                         *
                         * Intended use:
                         * inspect request/response text and token usage.
                         *
                         * It is commented out right now, so it is inactive.
                         */

                        /*
                         * SimpleLoggerAdvisor
                         *
                         * Built-in Spring AI advisor that logs request/response
                         * details for debugging and learning.
                         */
                        new SimpleLoggerAdvisor()

                        /*
                         * SafeGuardAdvisor
                         *
                         * Optional built-in advisor for basic keyword-based
                         * request blocking.
                         *
                         * It is currently disabled.
                         */
//                      , new SafeGuardAdvisor(List.of("cheat", "fraud", "hack"))
                )

                /*
                 * ----------------------------------------------------------
                 * CONCEPT 3: defaultSystem(...)
                 * ----------------------------------------------------------
                 *
                 * Sets the default system instruction for every request
                 * made through this ChatClient.
                 *
                 * This acts like the assistant's base behavior.
                 */
                .defaultSystem("You are helpful assistant")

                /*
                 * ----------------------------------------------------------
                 * CONCEPT 4: defaultOptions(...)
                 * ----------------------------------------------------------
                 *
                 * Defines the default OpenAI model configuration.
                 *
                 * These values apply globally unless overridden at request level.
                 */
                .defaultOptions(
                        OpenAiChatOptions.builder()

                                /*
                                 * model(...)
                                 *
                                 * Selects which OpenAI chat model handles
                                 * the request.
                                 */
                                .model("gpt-4o-mini")

                                /*
                                 * temperature(...)
                                 *
                                 * Controls output randomness.
                                 *
                                 * 1.0 means relatively more creative/varied
                                 * output compared to lower values.
                                 */
                                .temperature(1.0)

                                /*
                                 * maxTokens(...)
                                 *
                                 * Controls the maximum size of generated output.
                                 *
                                 * Larger values allow longer responses but may
                                 * increase cost and response size.
                                 */
                                .maxTokens(500)

                                // Finish building model options.
                                .build()
                )

                /*
                 * build()
                 *
                 * Creates the final ChatClient bean using all configured
                 * advisors, system prompt defaults, and model options.
                 */
                .build();
    }
}

/*
        | Part                                                 | Meaning                                                                                                                                        |
        | ---------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
        | JdbcChatMemoryRepository                             | Stores chat messages in a relational database using JDBC, giving memory persistence across restarts if the database is retained. docs.spring+1 |
        | MessageWindowChatMemory.builder()                    | Creates a bounded memory implementation that exposes only a recent window of messages to the model. docs.spring+1                              |
        | .chatMemoryRepository(jdbcChatMemoryRepository)      | Connects the message window layer to the JDBC persistence layer. baeldung+1                                                                    |
        | .maxMessages(10)                                     | Limits the active conversation window to 10 messages; older ones are evicted when the limit is exceeded. docs.spring+1                         |
        | MessageChatMemoryAdvisor.builder(chatMemory).build() | Creates an advisor that re-injects saved conversation history into new prompts. docs.spring+1                                                  |
        | messageChatMemoryAdvisor in defaultAdvisors(...)     | Enables context-aware multi-turn conversation automatically for every request made with this ChatClient. spring+1                              |
        | new SimpleLoggerAdvisor()                            | Logs request/response flow for inspection and debugging. spring+1                                                                              |
        | new TokenPrintAdvisor() commented                    | Shows where a custom token/usage advisor can be added, but it is not active right now. spring                                                  |
        | new SafeGuardAdvisor(...) commented                  | Shows optional request filtering is available but currently disabled. spring                                                                   |*/


/*
Important learning point

A very important distinction is this: JdbcChatMemoryRepository is the storage layer, while MessageWindowChatMemory is the memory policy layer. The repository persists messages in the database, but the memory
window decides how much of that history should be sent back to the model on the next interaction.*/

// JdbcChatMemoryRepository persists chat history, while MessageWindowChatMemory controls how much of that history is sent back to the model.
