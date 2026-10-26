package com.uday.sample.chat;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.request.*;
import dev.langchain4j.model.chat.request.json.*;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.Set;

/** Applies decoding constraints that cannot be expressed by the generated List/Map schema. */
public final class PlanChatModel implements ChatModel {
    private static final ResponseFormat FORMAT = ResponseFormat.builder()
            .type(ResponseFormatType.JSON)
            .jsonSchema(JsonSchema.builder().name("Plan").rootElement(JsonRawSchema.from("""
                    {
                      "type":"object",
                      "properties":{
                        "steps":{"type":"array","minItems":1,"maxItems":6,"items":{
                          "type":"object",
                          "properties":{
                            "operation":{"type":"string","enum":["CREATE","GET","LIST","UPDATE","PROMOTION","ELIGIBLE","ANALYTICS","CLARIFY"]},
                            "employee":{"type":["string","null"],"maxLength":100},
                            "fields":{"type":"object","properties":{
                              "name":{"type":"string","maxLength":100},
                              "department":{"type":"string","maxLength":100},
                              "designation":{"type":"string","maxLength":100},
                              "salary":{"type":"string","maxLength":30}
                            },"additionalProperties":false}
                          },
                          "required":["operation","employee","fields"],"additionalProperties":false
                        }},
                        "question":{"type":["string","null"],"maxLength":300}
                      },
                      "required":["steps","question"],"additionalProperties":false
                    }
                    """)).build()).build();

    private final ChatModel delegate;

    public PlanChatModel(ChatModel delegate) {
        this.delegate = delegate;
    }

    @Override
    public Set<Capability> supportedCapabilities() {
        return Set.of(Capability.RESPONSE_FORMAT_JSON_SCHEMA);
    }

    @Override
    public ChatResponse doChat(ChatRequest request) {
        var parameters = request.parameters().overrideWith(ChatRequestParameters.builder()
                .responseFormat(FORMAT).build());
        return delegate.chat(request.toBuilder().parameters(parameters).build());
    }
}
