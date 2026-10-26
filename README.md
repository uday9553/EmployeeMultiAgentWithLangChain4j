# Employee multi-agent assistant

This extends the existing Spring Boot application. The original `EmployeeWorkflowAgent` sequence (`EmployeeAgent` →
`PromotionAgent` → `EmployeeResponseAgent`) and `/api/ai-support/employee/promotion` remain available. Promotion rules
still come from `promotion_policy` through `PromotionService`; no language model calculates eligibility.

The supplied repository contained no Angular application, authentication setup, department field, or salary field. The
new standalone Angular application is located in the sibling directory
`E:\Uday practise springboot\EmployeeManagementUIWithLangChain4j`.

## Architecture and dependency decisions

The existing versions remain unchanged: Spring Boot 4.1.1, Java target 21, LangChain4j Spring Boot/Ollama starters
1.22.0-beta32, agentic 1.21.0-beta31. No Spring Security dependency is installed; APIs currently require no login. APIs
were inspected directly in the installed agentic JAR, and verified by compilation and Spring context tests. No
speculative supervisor API or dependency upgrade is required.

Exact commands `show all employees`, `list all employees`, and `get all employees` (case insensitive, with optional
final punctuation) use a deterministic LIST plan without calling Ollama. Other requests use
`AgenticServices.sequenceBuilder` with the structured `IntentAgent`. Its explicit Ollama JSON schema bounds arrays
to six steps and restricts field names. Schema constraints ensure shape, not correct interpretation. The default
model is `qwen2.5:3b`, installed using `ollama pull qwen2.5:3b`; set `OLLAMA_MODEL` to override it.
The earlier `llama3.2:1b` model misclassified listing and creation requests in live checks. The planner produces at most six
allowlisted steps. `ConversationService` validates the plan and invokes deterministic `ManagementAgent`,
`AnalyticsAgent`, and the existing promotion service. Dependent operations receive the actual generated employee ID.
Writes are outside LLM tool execution, so model retries cannot repeat them. All updates are reviewed through
server-issued forms; salary updates require explicit confirmation. Deletion and promotion approval are unsupported and
cannot be executed through tools or actions.

The model has no employee write tools. Plans, field names, employee references, action IDs, submitted values, and
allowed operations are checked in Java. There is no arbitrary method/URL execution. Employee IDs remain strings such as
`EMP-1001`, preserving the existing convention; numeric database IDs are not employee IDs. Unique partial-name lookup is
supported; ambiguous names require an exact ID.

New employees default to zero months in role and a performance rating of zero. Zero is treated as unrated in
performance averages. Employee IDs use `EMP-1000`, `EMP-1001`, and so on. A persistent singleton row in
`employee_id_counter` is read with `PESSIMISTIC_WRITE` during creation and held until the employee transaction
commits or rolls back. The first allocation starts above existing numeric employee IDs (after the demo seed,
`EMP-1006`); subsequent allocations use the stored counter. The database's employee-ID unique constraint remains
in place. Bootstrap never resets a persistent counter. Existing UUID IDs and existing null fields are not rewritten.

Before executing any planned CREATE or UPDATE, the service checks that proposed field values occur in the user's
current request or stored prior clarification request. Values appearing only in a model-generated question do not
qualify. Unmatched values cause a follow-up without writes. Copy values as supplied, including numeric formatting.
The default demo seed includes illustrative department and salary values for all five employees; these are not real
compensation data. The `local` profile skips seeds, so existing persistent records are not backfilled.

`UiPlanner` is deliberately deterministic: verified results become schema-version-1 nodes. The recursive Angular
renderer uses a registry of compiled components and `NgComponentOutlet`, with template interpolation. No HTML,
JavaScript, or Angular templates are generated or compiled at runtime. The registered renderer handles layouts,
employee/promotion cards, tables, native meter charts, forms, status, errors and safe presentation fallbacks. No Angular
Material or external chart dependency is introduced. Buttons without a server-issued supported action are disabled.

Angular 21 uses standalone components, signals, FormsModule and built-in control flow. Use Node 24 LTS and TypeScript
5.9, per the [official Angular compatibility table](https://angular.dev/reference/versions). A package lock is included.

## Run locally

Use JDK 21 or newer. This machine's default Java was 11 and its `JAVA_HOME` pointed to a missing Android JBR.
Verification used `C:\Users\PC\.jdks\openjdk-26.0.1`; Maven still compiles to the existing Java 21 target.

Start Ollama separately and ensure the configured `qwen2.5:3b` model is installed:

```powershell
ollama pull qwen2.5:3b
# If Ollama is not already running:
ollama serve
```

In another terminal at the repository root:

```powershell
$env:JAVA_HOME='C:\path\to\jdk-21'
mvn spring-boot:run
```

Backend: `http://localhost:9292`. The existing default H2 configuration resets and seeds data on startup. The H2 browser
console is disabled. For retained employee and conversation data, run:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

Both the default settings and optional `local` profile are defined in the single `src/main/resources/application.yaml`
file. The `local` profile stores data under ignored `local-data/`, uses schema update, and does **not** run sample seed
SQL. On a fresh local database, add your real promotion policies before expecting eligibility decisions. The existing
default profile remains the seeded demonstration environment.

Start the Angular development server in another terminal:

```powershell
cd 'E:\Uday practise springboot\EmployeeManagementUIWithLangChain4j'
npm ci
npm start
```

Open `http://localhost:4200` and send messages directly. No login, role checks, authentication headers, or CSRF tokens
are required. The Angular proxy forwards `/api` to port 9292.

The `serve` target in `angular.json` loads `proxy.json` for both `ng serve` and `npm start`. Its `/api/**` pattern
forwards nested endpoints such as `/api/ai/chat` and `/api/ai/actions`. Restart the Angular dev server after changing
proxy configuration. Browser requests still show port 4200 because forwarding happens inside the dev server.

## API examples

Send requests directly with `Content-Type: application/json`. Authentication headers and CSRF tokens are not needed.

`POST /api/ai/chat`, `Accept: application/json`:

```json
{"conversationId":"conv-123","requestId":"request-001","message":"Create employee Rahul in Engineering with salary 60000, then check his promotion eligibility"}
```

Use a new request ID for each new message. Reuse the **same** request ID when retrying the same request. An illustrative
response (the generated ID will differ):

```json
{
  "conversationId": "conv-123",
  "message": "Completed.",
  "status": "SUCCESS",
  "agents": [
    "Employee Management",
    "Promotion"
  ],
  "results": [
    {
      "employeeId": "EMP-generated-uuid",
      "name": "Rahul",
      "department": "Engineering",
      "salary": 60000,
      "designation": null,
      "monthsInRole": null,
      "performanceRating": null
    },
    {
      "employeeId": "EMP-generated-uuid",
      "employeeName": "Rahul",
      "eligible": null,
      "nextDesignation": null,
      "explanation": "Performance or experience data is missing; eligibility cannot be determined."
    }
  ],
  "schemaVersion": 1,
  "ui": {
    "type": "column",
    "props": {},
    "children": [
      {
        "type": "employee-card",
        "props": {
          "data": {
            "employeeId": "EMP-generated-uuid",
            "name": "Rahul",
            "department": "Engineering",
            "salary": 60000
          }
        },
        "children": []
      },
      {
        "type": "promotion-card",
        "props": {
          "data": {
            "employeeName": "Rahul",
            "eligible": null,
            "explanation": "Eligibility cannot be determined."
          }
        },
        "children": []
      }
    ]
  }
}
```

New employee performance/experience is deliberately unknown, rather than fabricated. The original seeded employee
`EMP-1001` has stored performance and experience and can demonstrate an eligible assessment. The conversational
interface cannot modify scores or experience.

Send `Update his salary to 75000` in the same conversation. The response includes a `form` node containing a
server-issued `actionId`, employee ID, allowed fields and `requiresConfirmation`. To submit it, `POST /api/ai/actions`:

```json
{"conversationId":"conv-123","actionId":"copy-the-issued-action-id","input":{"salary":"75000"},"confirmed":true}
```

Field values in actions are strings. The action must belong to the same conversation, be unexpired, and accept exactly
the issued field set. A generic edit form allows any of the four supported employee fields. Action IDs expire in ten
minutes. Completed actions replay their stored result rather than reapplying a change.

## Streaming

Use the same `POST /api/ai/chat` with `Accept: text/event-stream`. Angular uses fetch streaming to send the POST body
and receive workflow events. Events are `agent_started`, `agent_completed`, `ui_ready`, `workflow_completed`, and
`workflow_failed`. The UI tree arrives in `ui_ready`. These are real workflow progress events, not simulated token
deltas. The model's intent planning remains synchronous; there are no `message_delta` or tool events because this path
has no LLM tools. The installed agentic builder exposes `streamingChatModel` and `listener`; a future token-streaming
planner would additionally need a streaming Ollama bean and compatible structured-output handling.

SSE is bounded to 16 active requests with a configurable 210-second emitter timeout. Ollama uses a 180-second timeout,
768 output tokens, and no automatic retries. Run `mvn test -Dollama.live=true` to include the optional live decoding test;
it requires Ollama with `qwen2.5:3b` installed. The live tests check listing intent, exact CREATE field extraction, and
follow-up without writes for an incomplete CREATE. Set `-Dollama.test-model=...` to test a different model.
Client disconnection does not cancel or retry a write. A disconnected client can obtain the saved result using the same
request ID over synchronous JSON.

## Conversation and reliability boundaries

Conversation snapshots and execution reservations are stored in `ai_conversations`, keyed by conversation ID. They
include the selected employee, clarification context, issued actions and cached responses. The selected employee is
passed explicitly to the planning workflow rather than using shared LLM chat memory. The `local` profile retains this
data across restarts; the default in-memory demonstration database does not.

Execution reservations are persisted **before** mutations. An interrupted request returns a failed/reserved result on
replay, rather than guessing whether a write committed and repeating it. Inspect current records before initiating a new
write after interruption. Workflows commit individual service transactions; a later step failure does not roll back a
previously committed creation. Dependent steps after an update form require a follow-up message once the form is
submitted.

This implementation supports one application instance. Per-conversation synchronization does not provide distributed
locking: add database row locking or a shared execution queue before running multiple replicas. Conversations expire
after one hour; request replay protection has the same retention window. Limits are six workflow steps, 100 chat
requests per conversation, 1,000 cached active conversations, eight UI nesting levels, 100 UI nodes, 100,000 characters
per UI tree, and two million characters per persisted conversation. The frontend also validates schema version, known
types, tree size and response size. Large result sets are rejected rather than silently truncated; database
pagination/search beyond unique name resolution is a future extension. Persisted expired records require an operational
cleanup policy.

## Verify

```powershell
mvn test
cd 'E:\Uday practise springboot\EmployeeManagementUIWithLangChain4j'
npm run build
npm test
```

Backend tests cover routing plans, dependent actual-ID propagation, conversation isolation, replay, confirmation,
employee validation, unsupported/deep UI, and original promotion boundaries. `ChatIntegrationTests` starts real
HTTP/Tomcat, H2, unauthenticated requests, action validation and SSE with a mocked planning workflow. Angular tests
cover rendering without HTML execution, unsupported types and confirmation. Tests deliberately do not require Ollama, so
these standard checks validate the application contract. The optional live tests additionally check model intent and extraction.

Manual walkthrough with Ollama:

1. `Create an employee named Rahul in Engineering with salary 60000` → employee card.
2. `Update his department to Finance` → editable form → submit → updated card.
3. `Update his salary to 75000` → form → check confirmation → submit.
4. `Is he eligible for promotion?` → unknown until actual experience and performance exist.
5. `Check promotion eligibility for EMP-1001` → policy-backed promotion card in the default seeded profile.
6. `Show all employees` → table.
7. `Show a dashboard comparing employee performance across departments` → counts, rated population and average rating
   charts.
8. `Create employee Meera in Engineering with salary 60000 and check her promotion eligibility` → ordered management and
   promotion results.
9. `Show Rahul` then `Show his details` → remembered employee context, or a clarification if the name is ambiguous.

The configured small local model can produce malformed or incomplete plans. Such plans fail validation without an
unrestricted tool fallback. Evaluate your local model against these prompts before deployment; model choice and the
original Ollama settings are unchanged.

Verified in this workspace: 12 backend tests passed, four Angular tests passed, and the Angular production build passed.
This includes actual `AgenticServices` plan parsing with a mocked chat model, plus the HTTP/SSE integration scenario.
Live Ollama intent accuracy and browser automation were not exercised. The installed Node 25 emits an LTS warning; Node
24 is the documented runtime.
