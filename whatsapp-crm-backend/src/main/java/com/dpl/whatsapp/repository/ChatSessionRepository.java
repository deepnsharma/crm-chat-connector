package com.dpl.whatsapp.repository;

import com.dpl.whatsapp.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {
    Optional<ChatSession> findByPhoneNumber(String phoneNumber);
}
