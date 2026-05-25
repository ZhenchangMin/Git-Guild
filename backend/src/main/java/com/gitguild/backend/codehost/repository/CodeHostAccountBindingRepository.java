package com.gitguild.backend.codehost.repository;

import com.gitguild.backend.codehost.domain.CodeHostAccountBinding;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodeHostAccountBindingRepository extends JpaRepository<CodeHostAccountBinding, Long> {

    /**
     * 查找 Adventurer 在指定代码托管平台上的有效账号绑定。
     *
     * <p>PR 归属校验的入口查询。调用方应传入 {@code status = "ACTIVE"}；
     * 传入其他 status 值将绕过不变量，允许已失效绑定通过校验。
     *
     * @param userId   Git-Guild 用户 ID
     * @param hostType 代码托管平台类型，当前固定传 {@code "GITEA"}
     * @param status   绑定状态，正常调用固定传 {@code "ACTIVE"}
     * @return 有效绑定，若不存在则为 {@code empty}
     */
    Optional<CodeHostAccountBinding> findByUserUserIdAndHostTypeAndStatus(Long userId, String hostType, String status);
}
