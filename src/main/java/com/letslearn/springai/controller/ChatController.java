package com.letslearn.springai.controller;

import com.letslearn.springai.entity.Tut;
import com.letslearn.springai.service.ChatAdvisorService;
import com.letslearn.springai.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/v1")
public class ChatController {

    private static final Logger logger = Logger.getLogger(ChatController.class.getName());

    /*
     * ChatService is injected into the controller so the controller only deals with:
     * - receiving HTTP requests
     * - calling the service layer
     * - returning HTTP responses
     *
     * This keeps responsibilities separate:
     * controller -> web layer
     * service    -> Spring AI/business logic layer
     */

    @Autowired
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }


    /*
     * --------------------------------------------------------------
     * CONCEPT 1: Map AI response into a single entity object
     * --------------------------------------------------------------
     *
     * This endpoint is used when the AI is expected to return one
     * structured object that can be mapped into the Tut class.
     *
     * Example:
     * query = "Create one tutorial object for Spring Boot"
     *
     * Expected flow:
     * controller -> service -> AI response -> map to Tut -> return JSON
     */
    @GetMapping("/chat1")
    public ResponseEntity<Tut> chatMapResponseToSingleEntityObject(
            @RequestParam(value = "query", required = true) String query) {

        logger.info("Controller handling single entity response...");

        // Call service method that asks the model for one structured object.
        Tut tut = chatService.chatMapResponseToSingleEntityObject(query);

        // Return mapped object as HTTP 200 OK response.
        return ResponseEntity.ok(tut);
    }

    /*
     * --------------------------------------------------------------
     * CONCEPT 2: Map AI response into a list of entity objects
     * --------------------------------------------------------------
     *
     * This endpoint is used when the AI is expected to return multiple
     * structured objects and Spring AI maps them into List<Tut>.
     *
     * Important:
     * This must use a different path from the previous method.
     * If both methods use @GetMapping("/chat1"), Spring throws
     * an ambiguous mapping error at application startup. [web:39][web:45]
     *
     * Example:
     * query = "Generate 5 tutorials on Java collections"
     */
    @GetMapping("/chat2")
    public ResponseEntity<List<Tut>> chatMapResponseToList(
            @RequestParam(name = "query", required = true) String query) {

        logger.info("Controller handling list of entities response...");

        // Call service method that asks the model for multiple structured objects.
        List<Tut> tutorials = chatService.chatMapResponseToList(query);

        // Return list as HTTP 200 OK response.
        return ResponseEntity.ok(tutorials);
    }

    /*
     * --------------------------------------------------------------
     * CONCEPT 3: Return plain text response from Spring AI
     * --------------------------------------------------------------
     *
     * This is the simplest controller flow in your application.
     *
     * It accepts a query string, forwards it to the service,
     * and returns the generated text response as plain String.
     *
     * In your current code, this method calls:
     * chatUsingExternalPromptFile(query)
     *
     * That means the service will:
     * - load prompt text from external files
     * - inject runtime values into placeholders
     * - send the final prompt to the model
     * - return generated text
     */
    @GetMapping("/chat3")
    public ResponseEntity<String> chatUsingExternalPromptFile(
            @RequestParam(name = "query", required = true) String query) {

        logger.info("Received request in controller for query: " + query);

        // Delegate the actual AI interaction to the service layer.
        String response = chatService.chatUsingExternalPromptFile(query);

        // Wrap the generated text in an HTTP 200 OK response.
        return ResponseEntity.ok(response);
    }

}