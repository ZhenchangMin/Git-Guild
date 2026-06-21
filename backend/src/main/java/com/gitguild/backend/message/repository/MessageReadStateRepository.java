package com.gitguild.backend.message.repository;

import com.gitguild.backend.message.domain.MessageReadState;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageReadStateRepository extends JpaRepository<MessageReadState, Long> {

    Optional<MessageReadState> findByThreadThreadIdAndUserUserId(Long threadId, Long userId);
}
