package com.letslearn.springai.advisors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import reactor.core.publisher.Flux;

public class TokenPrintAdvisor implements CallAdvisor, StreamAdvisor {

    /*
     * SLF4J logger used to print advisor-level diagnostics.
     *
     * Since this advisor is meant for learning/debugging, logging is the
     * main behavior it provides.
     */
    private static final Logger logger = LoggerFactory.getLogger(TokenPrintAdvisor.class);

    /*
     * --------------------------------------------------------------
     * CONCEPT 1: Non-streaming advisor flow
     * --------------------------------------------------------------
     *
     * This method runs for normal, non-streaming ChatClient calls.
     *
     * Advisor lifecycle here:
     * 1. Read/inspect the current request
     * 2. Forward the request to the next advisor or model
     * 3. Read/inspect the generated response
     * 4. Return the same response (or a modified one)
     *
     * Important:
     * callAdvisorChain.nextCall(...) returns ChatClientResponse, and this
     * method must return a ChatClientResponse too. Returning null breaks
     * the chain. [web:81][web:48]
     */
    @Override
    public ChatClientResponse adviseCall(
            ChatClientRequest chatClientRequest,
            CallAdvisorChain callAdvisorChain) {

        logger.info("--- TokenPrintAdvisor is called");

        /*
         * Log the user-visible contents of the current prompt request.
         *
         * Useful for understanding what prompt text is actually being sent
         * after earlier advisors/default configuration are applied.
         */
        logger.info("--- Request from TPA : {}", chatClientRequest.prompt().getContents());

        /*
         * Pass control to the next advisor in the chain.
         *
         * This is the most important line in an advisor implementation.
         * Without this call, the request never moves forward to the next
         * advisor or the underlying model execution. [web:81][web:48]
         */
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);

        logger.info("--- Response received from LLM");

        /*
         * Log the generated response text.
         *
         * chatClientResponse
         *   -> wraps the final ChatResponse
         *   -> which contains results
         *   -> whose output contains the generated text
         */
        logger.info("--- Response from TPA : {}",
                chatClientResponse.chatResponse()
                        .getResult()
                        .getOutput()
                        .getText());

        /*
         * Log usage metadata such as total token count.
         *
         * Spring AI exposes usage information through response metadata,
         * including prompt tokens, completion tokens, and total tokens
         * when supported by the provider. [web:87][web:83]
         */
        logger.info("--- Total tokens consumed: {}",
                chatClientResponse.chatResponse()
                        .getMetadata()
                        .getUsage()
                        .getTotalTokens());

        /*
         * IMPORTANT:
         * Always return the response so the ChatClient caller receives it.
         *
         * Your original code returned null, which is incorrect for an
         * advisor chain and would cause failures downstream. [web:81][web:48]
         */
        return chatClientResponse;
    }

    /*
     * --------------------------------------------------------------
     * CONCEPT 2: Streaming advisor flow
     * --------------------------------------------------------------
     *
     * This method runs for streaming ChatClient calls.
     *
     * Instead of a single ChatClientResponse, streaming returns a Flux
     * of response chunks over time.
     *
     * Right now this implementation simply forwards the stream unchanged.
     * That is valid if you only want pass-through behavior for streaming.
     */
    @Override
    public Flux<ChatClientResponse> adviseStream(
            ChatClientRequest chatClientRequest,
            StreamAdvisorChain streamAdvisorChain) {

        /*
         * Forward the streaming request to the next advisor in the chain.
         * A Flux is returned because streaming produces multiple chunks
         * asynchronously instead of one final response. [web:82][web:48]
         */
        Flux<ChatClientResponse> chatClientResponseFlux =
                streamAdvisorChain.nextStream(chatClientRequest);

        return chatClientResponseFlux;
    }

    /*
     * getName()
     *
     * Returns the advisor name.
     * Spring AI can use this for identification/debugging purposes.
     */
    @Override
    public String getName() {
        return this.getClass().getName();
    }

    /*
     * getOrder()
     *
     * Defines advisor execution priority.
     *
     * Lower order value means earlier execution in the chain.
     * Returning 0 places this advisor near the front unless other
     * advisors use even lower values. Advisor order determines how
     * request/response interception is sequenced. [web:48][web:86]
     */
    @Override
    public int getOrder() {
        return 0;
    }
}