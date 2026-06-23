-- GitGuild MySQL Schema Initialization Script
-- Target: MySQL 8.0+
-- Description: Create database and core tables for GitGuild P4 implementation.
-- Note: Table creation uses IF NOT EXISTS so the script is safer to rerun during local development.

CREATE DATABASE IF NOT EXISTS gitguild
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

USE gitguild;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS users (
                       user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       username VARCHAR(32) NOT NULL,
                       email VARCHAR(128) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       role VARCHAR(32) NOT NULL,
                       status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
                       token_version INT NOT NULL DEFAULT 0,
                       created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       UNIQUE KEY uk_users_username (username),
                       UNIQUE KEY uk_users_email (email),
                       KEY idx_users_role_status (role, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS code_host_account_bindings (
                                            binding_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                            user_id BIGINT NOT NULL,
                                            host_type VARCHAR(32) NOT NULL,
                                            external_account_id VARCHAR(128) NOT NULL,
                                            external_username VARCHAR(128) NOT NULL,
                                            status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
                                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                            updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                            CONSTRAINT fk_bindings_user FOREIGN KEY (user_id) REFERENCES users(user_id),
                                            UNIQUE KEY uk_bindings_host_account (host_type, external_account_id),
                                            KEY idx_bindings_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS repositories (
                              repository_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                              owner_id BIGINT NOT NULL,
                              name VARCHAR(128) NOT NULL,
                              host_type VARCHAR(32) NOT NULL,
                              source_url VARCHAR(512) NOT NULL,
                              external_repository_id VARCHAR(128) NULL,
                              default_branch VARCHAR(128) NOT NULL DEFAULT 'main',
                              sync_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
                              last_synced_at DATETIME NULL,
                              sync_error_message VARCHAR(512) NULL,
                              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              CONSTRAINT fk_repositories_owner FOREIGN KEY (owner_id) REFERENCES users(user_id),
                              UNIQUE KEY uk_repositories_host_external (host_type, external_repository_id),
                              KEY idx_repositories_owner (owner_id),
                              KEY idx_repositories_sync_status (sync_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS issues (
                        issue_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        repository_id BIGINT NOT NULL,
                        external_issue_id VARCHAR(128) NOT NULL,
                        title VARCHAR(200) NOT NULL,
                        body TEXT NULL,
                        status VARCHAR(32) NOT NULL,
                        external_url VARCHAR(512) NULL,
                        synced_at DATETIME NULL,
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        CONSTRAINT fk_issues_repository FOREIGN KEY (repository_id) REFERENCES repositories(repository_id),
                        UNIQUE KEY uk_issues_repository_external (repository_id, external_issue_id),
                        KEY idx_issues_repository_status (repository_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pull_requests (
                               pull_request_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                               repository_id BIGINT NOT NULL,
                               external_pr_id VARCHAR(128) NOT NULL,
                               title VARCHAR(200) NOT NULL,
                               source_branch VARCHAR(128) NOT NULL,
                               target_branch VARCHAR(128) NOT NULL,
                               status VARCHAR(32) NOT NULL,
                               external_url VARCHAR(512) NULL,
                               merged_at DATETIME NULL,
                               created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                               CONSTRAINT fk_pr_repository FOREIGN KEY (repository_id) REFERENCES repositories(repository_id),
                               UNIQUE KEY uk_pr_repository_external (repository_id, external_pr_id),
                               KEY idx_pr_repository_status (repository_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS project_guides (
                                guide_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                repository_id BIGINT NOT NULL,
                                project_structure TEXT NOT NULL,
                                run_instructions TEXT NOT NULL,
                                contribution_steps TEXT NOT NULL,
                                example_pr_url VARCHAR(512) NULL,
                                reference_links JSON NULL,
                                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                CONSTRAINT fk_guides_repository FOREIGN KEY (repository_id) REFERENCES repositories(repository_id),
                                UNIQUE KEY uk_guides_repository (repository_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS quest_categories (
                                  category_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                  name VARCHAR(64) NOT NULL,
                                  description VARCHAR(255) NULL,
                                  enabled BOOLEAN NOT NULL DEFAULT TRUE,
                                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  UNIQUE KEY uk_categories_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS quest_tags (
                            tag_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                            name VARCHAR(64) NOT NULL,
                            color VARCHAR(32) NULL,
                            enabled BOOLEAN NOT NULL DEFAULT TRUE,
                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            UNIQUE KEY uk_tags_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS quest_tech_stacks (
                                   tech_stack_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                   name VARCHAR(64) NOT NULL,
                                   enabled BOOLEAN NOT NULL DEFAULT TRUE,
                                   created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                   UNIQUE KEY uk_tech_stacks_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS quests (
                        quest_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        publisher_id BIGINT NOT NULL,
                        repository_id BIGINT NOT NULL,
                        issue_id BIGINT NULL,
                        category_id BIGINT NOT NULL,
                        title VARCHAR(200) NOT NULL,
                        description TEXT NOT NULL,
                        completion_criteria TEXT NOT NULL,
                        difficulty VARCHAR(16) NOT NULL,
                        tech_stack JSON NOT NULL,
                        reward_xp INT NOT NULL,
                        estimated_hours INT NOT NULL,
                        status VARCHAR(32) NOT NULL,
                        published_at DATETIME NULL,
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        CONSTRAINT fk_quests_publisher FOREIGN KEY (publisher_id) REFERENCES users(user_id),
                        CONSTRAINT fk_quests_repository FOREIGN KEY (repository_id) REFERENCES repositories(repository_id),
                        CONSTRAINT fk_quests_issue FOREIGN KEY (issue_id) REFERENCES issues(issue_id),
                        CONSTRAINT fk_quests_category FOREIGN KEY (category_id) REFERENCES quest_categories(category_id),
                        KEY idx_quests_status_created (status, created_at),
                        KEY idx_quests_category_status (category_id, status),
                        KEY idx_quests_difficulty_status (difficulty, status),
                        KEY idx_quests_publisher (publisher_id),
                        FULLTEXT KEY ft_quests_title_description (title, description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS quest_tag_relations (
                                     quest_id BIGINT NOT NULL,
                                     tag_id BIGINT NOT NULL,
                                     PRIMARY KEY (quest_id, tag_id),
                                     CONSTRAINT fk_quest_tags_quest FOREIGN KEY (quest_id) REFERENCES quests(quest_id),
                                     CONSTRAINT fk_quest_tags_tag FOREIGN KEY (tag_id) REFERENCES quest_tags(tag_id),
                                     KEY idx_quest_tags_tag (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS quest_assignments (
                                   assignment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                   quest_id BIGINT NOT NULL,
                                   assignee_id BIGINT NOT NULL,
                                   status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
                                   accepted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   completed_at DATETIME NULL,
                                   created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                   CONSTRAINT fk_assignments_quest FOREIGN KEY (quest_id) REFERENCES quests(quest_id),
                                   CONSTRAINT fk_assignments_assignee FOREIGN KEY (assignee_id) REFERENCES users(user_id),
                                   KEY idx_assignments_assignee_status (assignee_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS submissions (
                             submission_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                             quest_id BIGINT NOT NULL,
                             submitter_id BIGINT NOT NULL,
                             pull_request_id BIGINT NOT NULL,
                             description TEXT NOT NULL,
                             evidence LONGTEXT NULL,
                             status VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW',
                             submitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             CONSTRAINT fk_submissions_quest FOREIGN KEY (quest_id) REFERENCES quests(quest_id),
                             CONSTRAINT fk_submissions_submitter FOREIGN KEY (submitter_id) REFERENCES users(user_id),
                             CONSTRAINT fk_submissions_pr FOREIGN KEY (pull_request_id) REFERENCES pull_requests(pull_request_id),
                             KEY idx_submissions_quest_status (quest_id, status),
                             KEY idx_submissions_submitter_status (submitter_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS review_records (
                                review_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                submission_id BIGINT NOT NULL,
                                reviewer_id BIGINT NOT NULL,
                                decision VARCHAR(32) NOT NULL,
                                summary VARCHAR(500) NOT NULL,
                                reviewed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                CONSTRAINT fk_reviews_submission FOREIGN KEY (submission_id) REFERENCES submissions(submission_id),
                                CONSTRAINT fk_reviews_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(user_id),
                                KEY idx_reviews_submission (submission_id),
                                KEY idx_reviews_reviewer (reviewer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS review_items (
                              item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                              review_id BIGINT NOT NULL,
                              checkpoint VARCHAR(128) NOT NULL,
                              comment VARCHAR(500) NULL,
                              passed BOOLEAN NOT NULL,
                              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              CONSTRAINT fk_review_items_review FOREIGN KEY (review_id) REFERENCES review_records(review_id),
                              KEY idx_review_items_review (review_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin_review_records (
                                      admin_review_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                      quest_id BIGINT NOT NULL,
                                      admin_id BIGINT NOT NULL,
                                      decision VARCHAR(32) NOT NULL,
                                      reason VARCHAR(500) NOT NULL,
                                      visible_to_publisher BOOLEAN NOT NULL DEFAULT TRUE,
                                      checklist_json TEXT NULL,
                                      reviewed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      CONSTRAINT fk_admin_reviews_quest FOREIGN KEY (quest_id) REFERENCES quests(quest_id),
                                      CONSTRAINT fk_admin_reviews_admin FOREIGN KEY (admin_id) REFERENCES users(user_id),
                                      KEY idx_admin_reviews_quest (quest_id),
                                      KEY idx_admin_reviews_admin (admin_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS recommendation_results (
                                        result_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                        user_id BIGINT NOT NULL,
                                        quest_id BIGINT NOT NULL,
                                        score DECIMAL(6, 2) NOT NULL,
                                        reason_text VARCHAR(500) NOT NULL,
                                        strategy_snapshot JSON NULL,
                                        generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        expires_at DATETIME NULL,
                                        CONSTRAINT fk_recommendations_user FOREIGN KEY (user_id) REFERENCES users(user_id),
                                        CONSTRAINT fk_recommendations_quest FOREIGN KEY (quest_id) REFERENCES quests(quest_id),
                                        UNIQUE KEY uk_recommendations_user_quest_generated (user_id, quest_id, generated_at),
                                        KEY idx_recommendations_user_score (user_id, score),
                                        KEY idx_recommendations_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS growth_profiles (
                                 profile_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                 user_id BIGINT NOT NULL,
                                 total_xp INT NOT NULL DEFAULT 0,
                                 level INT NOT NULL DEFAULT 1,
                                 completed_quest_count INT NOT NULL DEFAULT 0,
                                 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 CONSTRAINT fk_growth_profiles_user FOREIGN KEY (user_id) REFERENCES users(user_id),
                                 UNIQUE KEY uk_growth_profiles_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS xp_transactions (
                                 transaction_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                 user_id BIGINT NOT NULL,
                                 quest_id BIGINT NULL,
                                 amount INT NOT NULL,
                                 reason VARCHAR(128) NOT NULL,
                                 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 CONSTRAINT fk_xp_user FOREIGN KEY (user_id) REFERENCES users(user_id),
                                 CONSTRAINT fk_xp_quest FOREIGN KEY (quest_id) REFERENCES quests(quest_id),
                                 KEY idx_xp_user_created (user_id, created_at),
                                 KEY idx_xp_quest (quest_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS contribution_records (
                                      record_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                      user_id BIGINT NOT NULL,
                                      quest_id BIGINT NOT NULL,
                                      repository_id BIGINT NOT NULL,
                                      summary VARCHAR(500) NOT NULL,
                                      completed_at DATETIME NOT NULL,
                                      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      CONSTRAINT fk_contributions_user FOREIGN KEY (user_id) REFERENCES users(user_id),
                                      CONSTRAINT fk_contributions_quest FOREIGN KEY (quest_id) REFERENCES quests(quest_id),
                                      CONSTRAINT fk_contributions_repository FOREIGN KEY (repository_id) REFERENCES repositories(repository_id),
                                      UNIQUE KEY uk_contributions_user_quest (user_id, quest_id),
                                      KEY idx_contributions_user_completed (user_id, completed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notification_preferences (
                                          preference_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                          user_id BIGINT NOT NULL,
                                          enable_email BOOLEAN NOT NULL DEFAULT TRUE,
                                          enable_digest BOOLEAN NOT NULL DEFAULT TRUE,
                                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                          CONSTRAINT fk_preferences_user FOREIGN KEY (user_id) REFERENCES users(user_id),
                                          UNIQUE KEY uk_preferences_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notifications (
                               notification_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                               receiver_id BIGINT NOT NULL,
                               type VARCHAR(64) NOT NULL,
                               content VARCHAR(1000) NOT NULL,
                               status VARCHAR(32) NOT NULL DEFAULT 'UNREAD',
                               related_type VARCHAR(64) NULL,
                               related_id BIGINT NULL,
                               created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               read_at DATETIME NULL,
                               CONSTRAINT fk_notifications_receiver FOREIGN KEY (receiver_id) REFERENCES users(user_id),
                               KEY idx_notifications_receiver_status_created (receiver_id, status, created_at),
                               KEY idx_notifications_related (related_type, related_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS message_threads (
                               thread_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                               quest_id BIGINT NOT NULL,
                               publisher_id BIGINT NOT NULL,
                               assignee_id BIGINT NOT NULL,
                               status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
                               last_message_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                               CONSTRAINT fk_message_threads_quest FOREIGN KEY (quest_id) REFERENCES quests(quest_id),
                               CONSTRAINT fk_message_threads_publisher FOREIGN KEY (publisher_id) REFERENCES users(user_id),
                               CONSTRAINT fk_message_threads_assignee FOREIGN KEY (assignee_id) REFERENCES users(user_id),
                               UNIQUE KEY uk_message_threads_quest (quest_id),
                               KEY idx_message_threads_publisher_last (publisher_id, last_message_at),
                               KEY idx_message_threads_assignee_last (assignee_id, last_message_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS messages (
                               message_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                               thread_id BIGINT NOT NULL,
                               sender_id BIGINT NOT NULL,
                               content TEXT NOT NULL,
                               created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               CONSTRAINT fk_messages_thread FOREIGN KEY (thread_id) REFERENCES message_threads(thread_id),
                               CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users(user_id),
                               KEY idx_messages_thread_created (thread_id, created_at),
                               KEY idx_messages_sender (sender_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS message_read_states (
                               read_state_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                               thread_id BIGINT NOT NULL,
                               user_id BIGINT NOT NULL,
                               last_read_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                               CONSTRAINT fk_message_read_states_thread FOREIGN KEY (thread_id) REFERENCES message_threads(thread_id),
                               CONSTRAINT fk_message_read_states_user FOREIGN KEY (user_id) REFERENCES users(user_id),
                               UNIQUE KEY uk_message_read_states_thread_user (thread_id, user_id),
                               KEY idx_message_read_states_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 异常处理中心：平台自动检测到的运维异常（当前主要为仓库同步失败）。
-- repository_id 为轻引用（不建外键，避免与仓库生命周期强耦合）。
CREATE TABLE IF NOT EXISTS platform_exceptions (
                               exception_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                               category VARCHAR(32) NOT NULL,
                               type VARCHAR(64) NOT NULL,
                               title VARCHAR(255) NOT NULL,
                               status VARCHAR(32) NOT NULL DEFAULT 'UNRESOLVED',
                               repository_id BIGINT NULL,
                               repository_name VARCHAR(255) NULL,
                               related_quest VARCHAR(64) NULL,
                               reason VARCHAR(1000) NOT NULL,
                               impact VARCHAR(1000) NULL,
                               suggestion VARCHAR(1000) NULL,
                               retryable BIT(1) NOT NULL DEFAULT b'0',
                               resolution_action VARCHAR(64) NULL,
                               resolution_comment VARCHAR(1000) NULL,
                               resolved_by BIGINT NULL,
                               detected_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               resolved_at DATETIME NULL,
                               KEY idx_platform_exceptions_category_status (category, status, detected_at),
                               KEY idx_platform_exceptions_repo (repository_id, category, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 异常日志（只追加），按 seq 保序，随异常级联。
CREATE TABLE IF NOT EXISTS platform_exception_logs (
                               exception_id BIGINT NOT NULL,
                               seq INT NOT NULL,
                               line VARCHAR(1000) NOT NULL,
                               PRIMARY KEY (exception_id, seq),
                               CONSTRAINT fk_platform_exception_logs_exception
                                   FOREIGN KEY (exception_id) REFERENCES platform_exceptions(exception_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
