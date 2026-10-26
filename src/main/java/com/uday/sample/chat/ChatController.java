package com.uday.sample.chat;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class ChatController {
    private final ConversationService service;

    @org.springframework.beans.factory.annotation.Value("${employee.chat.stream-timeout:PT210S}")
    private java.time.Duration streamTimeout;

    public ChatController(ConversationService service) {
        this.service = service;
    }

    @PostMapping(value = "/chat", produces = "application/json")
    public ChatTypes.Response chat(@RequestBody ChatTypes.Request request) {
        return service.chat(request);
    }

    private final java.util.concurrent.Semaphore streams = new java.util.concurrent.Semaphore(16);

    @PostMapping(value = "/chat", produces = "text/event-stream")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter stream(@RequestBody ChatTypes.Request request) {
        if (!streams.tryAcquire())
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS);
        var emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(streamTimeout.toMillis());
        Thread.ofVirtual().start(() -> {
            try {
                var result = service.chat(request, (event, data) -> send(emitter, event, data));
                send(emitter, "ui_ready", result);
                send(emitter, "workflow_completed", Map.of("status", result.status()));
                emitter.complete();
            } catch (RuntimeException failure) {
                org.slf4j.LoggerFactory.getLogger(getClass()).warn("Streaming workflow failed", failure);
                send(emitter, "workflow_failed", Map.of("message", "Workflow failed. Check employee data before retrying a write."));
                emitter.complete();
            } finally {
                streams.release();
            }
        });
        return emitter;
    }

    private void send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter, String event, Object data) {
        // A disconnected client must not interrupt or trigger a retry of an executing write.
        try {
            emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().name(event).data(data));
        } catch (java.io.IOException |
                 IllegalStateException disconnected) { /* Result remains available through request replay. */ }
    }

    @PostMapping("/actions")
    public ChatTypes.Response action(@RequestBody ChatTypes.ActionRequest request) {
        return service.action(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public Map<String, String> invalid(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public Map<String, String> malformed() {
        return Map.of("message", "A valid JSON request body is required.");
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<Map<String, String>> capacity(org.springframework.web.server.ResponseStatusException exception) {
        return org.springframework.http.ResponseEntity.status(exception.getStatusCode()).body(Map.of("message", "Workflow capacity reached. Retry later."));
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_GATEWAY)
    public Map<String, String> failed(RuntimeException exception) {
        org.slf4j.LoggerFactory.getLogger(getClass()).error("Employee workflow failed", exception);
        return Map.of("message", "The workflow failed. Check current employee data before submitting a new write request.");
    }
}
