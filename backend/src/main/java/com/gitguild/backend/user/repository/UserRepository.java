package com.gitguild.backend.user.repository;

import com.gitguild.backend.user.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<User> findByEmail(String email);

    /** 登录用：同一标识既可匹配邮箱也可匹配用户名（两个参数传入相同值）。 */
    Optional<User> findByEmailOrUsername(String email, String username);
}
