package com.letslearn.springai.entity;

public class Tut {

    /*
     * This POJO is used as a target class for structured output mapping.
     *
     * When the AI returns a response matching these fields,
     * Spring AI can map the output directly into this class.
     */
    private String title;
    private String content;
    private String createdYear;

    public Tut(String title, String content, String createdYear) {
        this.title = title;
        this.content = content;
        this.createdYear = createdYear;
    }

    public Tut() {
        // Required default constructor for object mapping frameworks.
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        // Set tutorial title.
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        // Set tutorial content/body.
        this.content = content;
    }

    public String getCreatedYear() {
        return createdYear;
    }

    public void setCreatedYear(String createdYear) {
        // Set tutorial creation year.
        this.createdYear = createdYear;
    }
}