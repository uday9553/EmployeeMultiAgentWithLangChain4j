package com.uday.sample.chat;

import org.springframework.stereotype.Component;

import java.util.*;

import static com.uday.sample.chat.ChatTypes.*;

/**
 * Deterministic UI planner: only verified Java results become component props.
 */
@Component
public class UiPlanner {
    public static final Set<String> TYPES = Set.of("column", "row", "heading", "text", "employee-card",
            "promotion-card", "employee-table", "chart", "form", "button", "status", "error", "card", "confirmation");

    public Node node(String type, Object data) {
        return new Node(type, Map.of("data", data), List.of());
    }

    @dev.langchain4j.agentic.Agent(description = "Plans a bounded schema tree from verified business result components")
    public Node layout(List<Node> children) {
        Node root = new Node("column", Map.of(), List.copyOf(children));
        validate(root, 0, new int[]{0});
        if (tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(root).length() > 100000)
            throw new IllegalArgumentException("UI response too large; narrow the employee query");
        return root;
    }

    public void validate(Node node, int depth, int[] count) {
        if (node == null || !TYPES.contains(node.type()) || depth > 8 || ++count[0] > 100)
            throw new IllegalArgumentException("Invalid UI schema");
        if (node.children() == null || node.props() == null) throw new IllegalArgumentException("Invalid UI node");
        for (Node child : node.children()) validate(child, depth + 1, count);
    }
}
