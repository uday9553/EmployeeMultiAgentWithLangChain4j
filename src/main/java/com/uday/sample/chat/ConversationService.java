package com.uday.sample.chat;

import com.uday.sample.service.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.uday.sample.chat.ChatTypes.*;

import com.uday.sample.chat.ConversationStore.Pending;

@Service
public class ConversationService {
    private final PlanningWorkflow planner;
    private final EmployeeService employees;
    private final PromotionService promotions;
    private final UiPlanner ui;
    private final AnalyticsAgent analytics;
    private final ManagementAgent management;
    private final ConversationStore store;
    private final Map<String, State> conversations = new ConcurrentHashMap<>();

    private static class State {
        String employee;
        String question = "";
        long touched = System.currentTimeMillis();
        final Map<String, Response> requests = new HashMap<>();
        final Map<String, Pending> actions = new HashMap<>();
    }

    public ConversationService(PlanningWorkflow planner, EmployeeService employees, PromotionService promotions, UiPlanner ui,
                               AnalyticsAgent analytics, ManagementAgent management, ConversationStore store) {
        this.planner = planner;
        this.employees = employees;
        this.promotions = promotions;
        this.ui = ui;
        this.analytics = analytics;
        this.management = management;
        this.store = store;
    }

    private State state(String id) {
        if (id == null || !id.matches("[a-zA-Z0-9_-]{1,80}"))
            throw new IllegalArgumentException("Invalid conversation ID");
        conversations.entrySet().removeIf(e -> System.currentTimeMillis() - e.getValue().touched > 3600000);
        if (conversations.size() > 1000) throw new IllegalArgumentException("Conversation capacity reached");
        State state = conversations.computeIfAbsent(id, k -> {
            State fresh = new State();
            var saved = store.load(k);
            if (saved != null) {
                fresh.employee = saved.employee();
                fresh.question = saved.question();
                fresh.requests.putAll(saved.requests());
                fresh.actions.putAll(saved.actions());
            }
            return fresh;
        });
        state.touched = System.currentTimeMillis();
        return state;
    }

    public Response chat(Request request) {
        return chat(request, (event, data) -> {
        });
    }

    public Response chat(Request request, java.util.function.BiConsumer<String, Object> events) {
        var state = state(request.conversationId());
        if (request.message() == null || request.message().isBlank() || request.message().length() > 4000
                || request.requestId() == null || !request.requestId().matches("[a-zA-Z0-9_-]{1,80}"))
            throw new IllegalArgumentException("Message and unique requestId required");
        synchronized (state) {
            if (state.requests.containsKey(request.requestId())) return state.requests.get(request.requestId());
            if (state.requests.size() >= 100) throw new IllegalArgumentException("Start a new conversation");
            var plan = planner.plan(request.message(), Objects.toString(state.employee, "none"), state.question);
            if (plan == null || plan.steps() == null || plan.steps().isEmpty() || plan.steps().size() > 6)
                throw new IllegalArgumentException("Invalid agent plan");
            // Validate the plan before executing any write.
            for (var step : plan.steps()) {
                if (step == null || step.operation() == null) throw new IllegalArgumentException("Invalid operation");
                if (step.operation() == Operation.CREATE && (step.fields() == null || !step.fields().keySet().containsAll(Set.of("name", "department", "salary"))))
                    return followUp(request, state, "Please provide the employee's name, department and salary.");
                if ((step.operation() == Operation.CREATE || step.operation() == Operation.UPDATE)
                        && step.fields() != null && !suppliedFields(step.fields(), request.message(), state.question))
                    return followUp(request, state, "Please provide the exact employee field values to use: name, department and salary for creation, or the fields to update.");
            }
            // Reserve the key before execution. A failed/partial write cannot be replayed automatically.
            state.requests.put(request.requestId(), response(request.conversationId(), "Request interrupted; inspect current data before retrying.", "FAILED", List.of(), List.of(), List.of()));
            save(request.conversationId(), state);
            List<Object> results = new ArrayList<>();
            List<Node> nodes = new ArrayList<>();
            List<String> agents = new ArrayList<>();
            String message = "Completed.";
            String status = "SUCCESS";
            for (var step : plan.steps()) {
                org.slf4j.LoggerFactory.getLogger(getClass()).info("Employee workflow operation={}", step.operation());
                events.accept("agent_started", Map.of("agent", step.operation().name()));
                switch (step.operation()) {
                    case CLARIFY -> {
                        message = Objects.toString(plan.question(), "Please clarify your request.");
                        state.question = "Previous request: " + request.message() + ". Follow-up: " + message;
                        status = "FOLLOW_UP";
                    }
                    case CREATE -> {
                        var employee = management.create(step.fields());
                        state.employee = employee.getEmployeeId();
                        results.add(employee);
                        nodes.add(ui.node("employee-card", employee));
                        agents.add("Employee Management");
                    }
                    case GET, PROMOTION, UPDATE -> {
                        String ref = step.employee() == null || step.employee().isBlank() ? state.employee : step.employee();
                        if (ref == null)
                            return followUp(request, state, "Which employee? Provide an employee ID or full name.");
                        com.uday.sample.model.Employee employee;
                        try {
                            employee = management.get(ref);
                        } catch (IllegalArgumentException missing) {
                            return followUp(request, state, "Employee lookup is missing or ambiguous. Please provide an exact employee ID.");
                        }
                        state.employee = employee.getEmployeeId();
                        if (step.operation() == Operation.GET) {
                            results.add(employee);
                            nodes.add(ui.node("employee-card", employee));
                            agents.add("Employee Management");
                        } else if (step.operation() == Operation.PROMOTION) {
                            var result = promotions.assess(state.employee);
                            results.add(result);
                            nodes.add(ui.node("promotion-card", result));
                            agents.add("Promotion");
                        } else {
                            String actionId = UUID.randomUUID().toString();
                            Map<String, String> fields = step.fields() == null ? Map.of() : Map.copyOf(step.fields());
                            if (!Set.of("name", "department", "designation", "salary").containsAll(fields.keySet()))
                                throw new IllegalArgumentException("Invalid update fields");
                            state.actions.put(actionId, new Pending(state.employee, fields, fields.isEmpty() || fields.containsKey("salary"), System.currentTimeMillis() + 600000));
                            nodes.add(ui.node("form", Map.of("actionId", actionId, "employeeId", state.employee, "fields", fields,
                                    "requiresConfirmation", fields.isEmpty() || fields.containsKey("salary"))));
                            message = "Review and submit the employee changes.";
                            status = "FOLLOW_UP";
                            agents.add("Employee Management");
                        }
                    }
                    case LIST -> {
                        var all = management.list();
                        results.add(all);
                        nodes.add(ui.node("employee-table", all));
                        agents.add("Employee Management");
                    }
                    case ELIGIBLE, ANALYTICS -> {
                        var eligible = analytics.eligible();
                        if (step.operation() == Operation.ELIGIBLE) {
                            results.add(eligible);
                            nodes.add(ui.node("employee-table", eligible));
                        } else {
                            var data = analytics.dashboard();
                            results.add(data);
                            nodes.add(ui.node("chart", data));
                        }
                        agents.add("Analytics");
                    }
                }
                events.accept("agent_completed", Map.of("agent", step.operation().name()));
                if (status.equals("FOLLOW_UP")) break;
            }
            var response = response(request.conversationId(), message, status, agents, results, nodes);
            state.requests.put(request.requestId(), response);
            save(request.conversationId(), state);
            return response;
        }
    }

    public Response action(ActionRequest request) {
        var state = state(request.conversationId());
        synchronized (state) {
            String key = "action-" + request.actionId();
            if (state.requests.containsKey(key)) return state.requests.get(key);
            var pending = state.actions.get(request.actionId());
            if (pending == null || pending.expires() < System.currentTimeMillis())
                throw new IllegalArgumentException("Unknown or expired action");
            if (pending.sensitive() && !request.confirmed())
                throw new IllegalArgumentException("Explicit confirmation required");
            var fields = request.input();
            if (fields == null || (!pending.fields().isEmpty() && !pending.fields().keySet().equals(fields.keySet())))
                throw new IllegalArgumentException("Action fields do not match");
            employees.validateChanges(fields);
            // Persist an execution reservation before committing any mutation.
            state.requests.put(key, response(request.conversationId(), "Action interrupted; inspect current data before retrying.", "FAILED", List.of(), List.of(), List.of()));
            save(request.conversationId(), state);
            var employee = management.update(pending.employee(), fields);
            var result = response(request.conversationId(), "Employee updated.", "SUCCESS", List.of("Employee Management"), List.of(employee), List.of(ui.node("employee-card", employee)));
            state.requests.put(key, result);
            state.actions.remove(request.actionId());
            save(request.conversationId(), state);
            return result;
        }
    }

    private Response response(String id, String message, String status, List<String> agents, List<Object> results, List<Node> nodes) {
        return new Response(id, message, status, List.copyOf(agents), List.copyOf(results), 1, ui.layout(nodes));
    }

    private boolean suppliedFields(Map<String, String> fields, String message, String previousQuestion) {
        String context = message;
        // Only include the user's prior request, not text generated by the model's question.
        if (previousQuestion != null && previousQuestion.startsWith("Previous request: ")) {
            int boundary = previousQuestion.indexOf(". Follow-up: ");
            if (boundary >= 0) context += " " + previousQuestion.substring("Previous request: ".length(), boundary);
        }
        for (String value : fields.values()) {
            if (value == null || value.isBlank()) return false;
            var pattern = java.util.regex.Pattern.compile("(?<![\\p{L}\\p{N}_])"
                    + java.util.regex.Pattern.quote(value.strip()) + "(?![\\p{L}\\p{N}_])",
                    java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE);
            if (!pattern.matcher(context).find()) return false;
        }
        return true;
    }

    private void save(String id, State state) {
        store.save(id, new ConversationStore.Snapshot(state.employee, state.question, state.requests, state.actions));
    }

    private Response followUp(Request request, State state, String question) {
        state.question = "Previous request: " + request.message() + ". Follow-up: " + question;
        var result = response(request.conversationId(), question, "FOLLOW_UP", List.of(), List.of(), List.of(ui.node("text", question)));
        state.requests.put(request.requestId(), result);
        save(request.conversationId(), state);
        return result;
    }
}
