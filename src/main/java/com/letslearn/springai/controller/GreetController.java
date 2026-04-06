package com.letslearn.springai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class GreetController {


    @GetMapping("/greet")
    public String greetUser(){
        return "Hello Spring AI";
    }
}
