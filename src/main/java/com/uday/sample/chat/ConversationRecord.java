package com.uday.sample.chat;

import jakarta.persistence.*;

@Entity
@Table(name = "ai_conversations")
public class ConversationRecord {
    @Id
    @Column(length = 300)
    private String id;
    @Lob
    private String stateJson;
    private long touched;

    protected ConversationRecord() {
    }

    public ConversationRecord(String id, String stateJson, long touched) {
        this.id = id;
        this.stateJson = stateJson;
        this.touched = touched;
    }

    public String getStateJson() {
        return stateJson;
    }

    public long getTouched() {
        return touched;
    }
}
