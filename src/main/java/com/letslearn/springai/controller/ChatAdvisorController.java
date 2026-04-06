package com.letslearn.springai.controller;

import com.letslearn.springai.service.ChatAdvisorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/advisor")
public class ChatAdvisorController {

    /*
     * ChatAdvisorService contains the actual Spring AI advisor-related logic.
     *
     * The controller should stay lightweight and only:
     * - accept HTTP input
     * - delegate to service layer
     * - return HTTP response
     *
     * Since we are using constructor injection, this dependency should
     * ideally be marked final.
     */
    private final ChatAdvisorService chatAdvisorService;

    public ChatAdvisorController(ChatAdvisorService chatAdvisorService) {
        this.chatAdvisorService = chatAdvisorService;
    }

    /*
     * --------------------------------------------------------------
     * CONCEPT 1: Advisor-based chat endpoint
     * --------------------------------------------------------------
     *
     * This endpoint demonstrates the advisor concept in Spring AI.
     *
     * Flow:
     * 1. Client sends HTTP GET request with query parameter
     * 2. Controller forwards that query to service layer
     * 3. Service calls Spring AI ChatClient
     * 4. Configured advisors intercept/log/guard the request-response flow
     * 5. Final AI response is returned to the client
     *
     * Why this endpoint matters:
     * It shows that advisors are not called directly from the controller.
     * Instead, they are applied automatically by the ChatClient pipeline
     * configured in AiConfig. Spring AI documents advisors as interceptors
     * that can inspect, modify, enrich, or guard prompt execution. [web:48][web:64]
     *
     * Note:
     * required = false means query may be absent, so the service method
     * should be able to handle null safely if this is intentional. [web:58][web:63]
     */
    @GetMapping("/chat")
    public ResponseEntity<String> chat(
            @RequestParam(name = "query", required = false) String query,
            @RequestHeader(name="userId") String userID) {

        // Delegate the request to the advisor-based service method.
        String response = chatAdvisorService.chatTemplate(query, userID);

        // Return the generated response as HTTP 200 OK.
        return ResponseEntity.ok(response);
    }


    @GetMapping("/stream-chat")
    public ResponseEntity<Flux<String>> streamChat(@RequestParam(name="query", required = false) String query,
                                                   @RequestHeader(name="userId") String userID){
        return ResponseEntity.ok(chatAdvisorService.streamChat(query, userID));

    }
}