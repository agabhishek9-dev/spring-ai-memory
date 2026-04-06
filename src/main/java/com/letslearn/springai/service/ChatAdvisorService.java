package com.letslearn.springai.service;

import reactor.core.publisher.Flux;

public interface ChatAdvisorService {
    String chatTemplate(String query, String userID);

    Flux<String> streamChat(String query, String userId);
}
