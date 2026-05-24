package com.gitguild.backend.quest.repository;

import com.gitguild.backend.codehost.domain.CodeIssue;
import com.gitguild.backend.quest.domain.Quest;
import com.gitguild.backend.quest.domain.QuestStatus;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface QuestRepository extends JpaRepository<Quest, Long>, JpaSpecificationExecutor<Quest> {

    boolean existsByIssueAndStatusIn(CodeIssue issue, Collection<QuestStatus> statuses);
}
