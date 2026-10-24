package com.uday.sample.service;


import dev.langchain4j.service.spring.AiService;

@AiService
public interface SampleAIAgent {

    String chat(String message);
}