package com.uday.sample.chat;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<ConversationRecord, String> {
}
