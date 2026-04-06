package com.letslearn.springai.service;

import com.letslearn.springai.entity.Tut;

import java.util.List;

public interface ChatService {

        /*
         * CONCEPT 1:
         * Returns plain text response from the AI model.
         */
        // String chat(String query);

        /*
         * CONCEPT 2:
         * Returns a single structured object mapped from the AI response.
         */
        // Tut chat1(String query);

        /*
         * CONCEPT 3:
         * Returns a list of structured objects mapped from the AI response.
         */
        // List<Tut> chat2(String query);

        /*
         * CONCEPT 4:
         * Current active method for returning plain text response.
         */
        //  String chat3(String query);

        /*
         * CONCEPT :
         *
         */
//        String chat4(String query);
        String chatPassingStaticUserAndSystemPrompt(String query);
        String chatPassingPromptObject(String query);
        Tut chatMapResponseToSingleEntityObject(String query);
        List<Tut> chatMapResponseToList(String query);
        String chatFluentAPIChain(String query);
        String chatUsingSimpleFluentAPI(String query);
        String chatTemplateUsingPromptTemplating1(String query);
        String chatTemplateUsingPromptTemplating2(String query);
        String chatTemplateUsingFluentAPI(String query);
        String chatUsingExternalPromptFile(String query);
}