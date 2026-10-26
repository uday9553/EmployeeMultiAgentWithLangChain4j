package com.uday.sample.chat;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

@Component
public class ConversationStore {
    public record Pending(String employee, Map<String, String> fields, boolean sensitive, long expires) {
    }

    public record Snapshot(String employee, String question, Map<String, ChatTypes.Response> requests,
                           Map<String, Pending> actions) {
    }

    private final ConversationRepository repository;
    private final ObjectMapper mapper;

    public ConversationStore(ConversationRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public Snapshot load(String key) {
        return repository.findById(key).filter(r -> System.currentTimeMillis() - r.getTouched() < 3600000)
                .map(r -> mapper.readValue(r.getStateJson(), Snapshot.class)).orElse(null);
    }

    public void save(String key, Snapshot snapshot) {
        String json = mapper.writeValueAsString(snapshot);
        if (json.length() > 2000000)
            throw new IllegalArgumentException("Conversation size limit reached; start a new conversation");
        repository.saveAndFlush(new ConversationRecord(key, json, System.currentTimeMillis()));
    }
}
