package com.uday.sample.chat;

import java.util.*;

public final class ChatTypes {
    private ChatTypes() {
    }

    public record Request(String conversationId, String requestId, String message) {
    }

    public record ActionRequest(String conversationId, String actionId, Map<String, String> input, boolean confirmed) {
    }

    public enum Operation {CREATE, GET, LIST, UPDATE, PROMOTION, ELIGIBLE, ANALYTICS, CLARIFY}

    public record Step(Operation operation, String employee, Map<String, String> fields) {
    }

    public record Plan(List<Step> steps, String question) {
    }

    public record Node(String type, Map<String, Object> props, List<Node> children) {
    }

    public record Response(String conversationId, String message, String status,
                           List<String> agents, List<Object> results, int schemaVersion, Node ui) {
    }
}
