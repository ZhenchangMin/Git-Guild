
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `admin_review_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_review_records` (
  `admin_review_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `decision` enum('APPROVE_PUBLISH','REJECT_PUBLISH','TAKE_DOWN') NOT NULL,
  `reason` varchar(500) NOT NULL,
  `reviewed_at` datetime(6) NOT NULL,
  `visible_to_publisher` bit(1) NOT NULL,
  `admin_id` bigint NOT NULL,
  `quest_id` bigint NOT NULL,
  PRIMARY KEY (`admin_review_id`),
  KEY `FKqpto5guc93e6sq1ypp6s0s5ab` (`admin_id`),
  KEY `FKg8i4e805fpk117dj10walbp1n` (`quest_id`),
  CONSTRAINT `FKg8i4e805fpk117dj10walbp1n` FOREIGN KEY (`quest_id`) REFERENCES `quests` (`quest_id`),
  CONSTRAINT `FKqpto5guc93e6sq1ypp6s0s5ab` FOREIGN KEY (`admin_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `admin_review_records` WRITE;
/*!40000 ALTER TABLE `admin_review_records` DISABLE KEYS */;
/*!40000 ALTER TABLE `admin_review_records` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `code_host_account_bindings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `code_host_account_bindings` (
  `binding_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `external_account_id` varchar(128) NOT NULL,
  `external_username` varchar(128) NOT NULL,
  `host_type` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`binding_id`),
  KEY `FK8dhw0klyhjxcxhgm78xhy9fhj` (`user_id`),
  CONSTRAINT `FK8dhw0klyhjxcxhgm78xhy9fhj` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `code_host_account_bindings` WRITE;
/*!40000 ALTER TABLE `code_host_account_bindings` DISABLE KEYS */;
/*!40000 ALTER TABLE `code_host_account_bindings` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `contribution_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `contribution_records` (
  `record_id` bigint NOT NULL AUTO_INCREMENT,
  `completed_at` datetime(6) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `summary` varchar(500) NOT NULL,
  `quest_id` bigint NOT NULL,
  `repository_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`record_id`),
  KEY `FKne1vfskpkvglsbpogd6l5nlo` (`quest_id`),
  KEY `FK5bn6gxqhwu8f25rhd3878ykib` (`repository_id`),
  KEY `FKbuxxbpbyuep3tai88ynwibqfq` (`user_id`),
  CONSTRAINT `FK5bn6gxqhwu8f25rhd3878ykib` FOREIGN KEY (`repository_id`) REFERENCES `repositories` (`repository_id`),
  CONSTRAINT `FKbuxxbpbyuep3tai88ynwibqfq` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKne1vfskpkvglsbpogd6l5nlo` FOREIGN KEY (`quest_id`) REFERENCES `quests` (`quest_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `contribution_records` WRITE;
/*!40000 ALTER TABLE `contribution_records` DISABLE KEYS */;
/*!40000 ALTER TABLE `contribution_records` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `growth_profiles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `growth_profiles` (
  `profile_id` bigint NOT NULL AUTO_INCREMENT,
  `completed_quest_count` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `level` int NOT NULL,
  `total_xp` int NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`profile_id`),
  UNIQUE KEY `UKidojqqsge19e09q8pkx7m50vh` (`user_id`),
  CONSTRAINT `FKmps25oroq66nsm662q0m1p8b` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `growth_profiles` WRITE;
/*!40000 ALTER TABLE `growth_profiles` DISABLE KEYS */;
/*!40000 ALTER TABLE `growth_profiles` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `issues`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `issues` (
  `issue_id` bigint NOT NULL AUTO_INCREMENT,
  `body` text,
  `created_at` datetime(6) NOT NULL,
  `external_issue_id` varchar(128) NOT NULL,
  `external_url` varchar(512) DEFAULT NULL,
  `status` varchar(32) NOT NULL,
  `synced_at` datetime(6) DEFAULT NULL,
  `title` varchar(200) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `repository_id` bigint NOT NULL,
  PRIMARY KEY (`issue_id`),
  KEY `FK4bjkv5wxbc6gcsd28wn6bfni8` (`repository_id`),
  CONSTRAINT `FK4bjkv5wxbc6gcsd28wn6bfni8` FOREIGN KEY (`repository_id`) REFERENCES `repositories` (`repository_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `issues` WRITE;
/*!40000 ALTER TABLE `issues` DISABLE KEYS */;
INSERT INTO `issues` VALUES (1,NULL,'2026-06-06 12:50:12.547997','1','http://localhost:3000/spike-admin/hello-world-mvp/issues/1','OPEN','2026-06-06 12:50:12.545273','Add hello world file','2026-06-06 12:50:12.547997',1);
/*!40000 ALTER TABLE `issues` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `notification_id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(1000) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `read_at` datetime(6) DEFAULT NULL,
  `related_id` bigint DEFAULT NULL,
  `related_type` varchar(64) DEFAULT NULL,
  `status` enum('READ','UNREAD') NOT NULL,
  `type` enum('REVIEW_APPROVED','REVIEW_CHANGES_REQUESTED','REVIEW_REJECTED','SUBMISSION_RECEIVED') NOT NULL,
  `receiver_id` bigint NOT NULL,
  PRIMARY KEY (`notification_id`),
  KEY `FK9kxl0whvhifo6gw4tjq36v53k` (`receiver_id`),
  CONSTRAINT `FK9kxl0whvhifo6gw4tjq36v53k` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `notifications` WRITE;
/*!40000 ALTER TABLE `notifications` DISABLE KEYS */;
/*!40000 ALTER TABLE `notifications` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `pull_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pull_requests` (
  `pull_request_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `external_pr_id` varchar(128) NOT NULL,
  `external_url` varchar(512) DEFAULT NULL,
  `merged_at` datetime(6) DEFAULT NULL,
  `source_branch` varchar(128) NOT NULL,
  `status` varchar(32) NOT NULL,
  `target_branch` varchar(128) NOT NULL,
  `title` varchar(200) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `repository_id` bigint NOT NULL,
  PRIMARY KEY (`pull_request_id`),
  KEY `FK4xy4k2hxqpbt6qc7q4ruaylkg` (`repository_id`),
  CONSTRAINT `FK4xy4k2hxqpbt6qc7q4ruaylkg` FOREIGN KEY (`repository_id`) REFERENCES `repositories` (`repository_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `pull_requests` WRITE;
/*!40000 ALTER TABLE `pull_requests` DISABLE KEYS */;
/*!40000 ALTER TABLE `pull_requests` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `quest_assignments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quest_assignments` (
  `assignment_id` bigint NOT NULL AUTO_INCREMENT,
  `accepted_at` datetime(6) NOT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `status` enum('ABANDONED','ACTIVE','CANCELLED','COMPLETED') NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `assignee_id` bigint NOT NULL,
  `quest_id` bigint NOT NULL,
  `task_branch` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`assignment_id`),
  KEY `FKi29ons887iftuudjqojxojswm` (`assignee_id`),
  KEY `FK3mwv32a87i9s9pulhblqxdlsc` (`quest_id`),
  CONSTRAINT `FK3mwv32a87i9s9pulhblqxdlsc` FOREIGN KEY (`quest_id`) REFERENCES `quests` (`quest_id`),
  CONSTRAINT `FKi29ons887iftuudjqojxojswm` FOREIGN KEY (`assignee_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `quest_assignments` WRITE;
/*!40000 ALTER TABLE `quest_assignments` DISABLE KEYS */;
/*!40000 ALTER TABLE `quest_assignments` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `quest_categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quest_categories` (
  `category_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `enabled` bit(1) NOT NULL,
  `name` varchar(64) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`category_id`),
  UNIQUE KEY `UKki7xiviisykal530gfq256rtt` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `quest_categories` WRITE;
/*!40000 ALTER TABLE `quest_categories` DISABLE KEYS */;
INSERT INTO `quest_categories` VALUES (1,'2026-06-04 06:11:55.000000','æœ¬åœ° Gitea MVP æ¼”ç¤ºåˆ†ç±»',_binary '','MVP','2026-06-04 06:11:55.000000');
/*!40000 ALTER TABLE `quest_categories` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `quest_tag_relations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quest_tag_relations` (
  `quest_id` bigint NOT NULL,
  `tag_id` bigint NOT NULL,
  PRIMARY KEY (`quest_id`,`tag_id`),
  KEY `FKcbidmdp3fkqa9gn5vcfeqfqbh` (`tag_id`),
  CONSTRAINT `FKcbidmdp3fkqa9gn5vcfeqfqbh` FOREIGN KEY (`tag_id`) REFERENCES `quest_tags` (`tag_id`),
  CONSTRAINT `FKsirxxdqn48fajochdp4kfkfl7` FOREIGN KEY (`quest_id`) REFERENCES `quests` (`quest_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `quest_tag_relations` WRITE;
/*!40000 ALTER TABLE `quest_tag_relations` DISABLE KEYS */;
/*!40000 ALTER TABLE `quest_tag_relations` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `quest_tags`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quest_tags` (
  `tag_id` bigint NOT NULL AUTO_INCREMENT,
  `color` varchar(32) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `enabled` bit(1) NOT NULL,
  `name` varchar(64) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`tag_id`),
  UNIQUE KEY `UK6xshqv9b292my62lbj74vpynf` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `quest_tags` WRITE;
/*!40000 ALTER TABLE `quest_tags` DISABLE KEYS */;
/*!40000 ALTER TABLE `quest_tags` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `quests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quests` (
  `quest_id` bigint NOT NULL AUTO_INCREMENT,
  `completion_criteria` text NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` text NOT NULL,
  `difficulty` enum('A','B','C','D') NOT NULL,
  `estimated_hours` int NOT NULL,
  `published_at` datetime(6) DEFAULT NULL,
  `reward_xp` int NOT NULL,
  `status` enum('CLOSED','COMPLETED','DRAFT','IN_PROGRESS','IN_REVIEW','PENDING_ADMIN_REVIEW','PUBLISHED','REJECTED') NOT NULL,
  `tech_stack` json NOT NULL,
  `title` varchar(200) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `category_id` bigint NOT NULL,
  `issue_id` bigint DEFAULT NULL,
  `publisher_id` bigint NOT NULL,
  `repository_id` bigint NOT NULL,
  PRIMARY KEY (`quest_id`),
  KEY `FKhdf4p58y37laeo7o2jlxtxsiu` (`category_id`),
  KEY `FK1tvfixe5wedmsmhs6u739i6cs` (`issue_id`),
  KEY `FKmbnbslok96ctxtujuhqdr74lk` (`publisher_id`),
  KEY `FK5f9roosknqw2wwqsb3lh1jfau` (`repository_id`),
  CONSTRAINT `FK1tvfixe5wedmsmhs6u739i6cs` FOREIGN KEY (`issue_id`) REFERENCES `issues` (`issue_id`),
  CONSTRAINT `FK5f9roosknqw2wwqsb3lh1jfau` FOREIGN KEY (`repository_id`) REFERENCES `repositories` (`repository_id`),
  CONSTRAINT `FKhdf4p58y37laeo7o2jlxtxsiu` FOREIGN KEY (`category_id`) REFERENCES `quest_categories` (`category_id`),
  CONSTRAINT `FKmbnbslok96ctxtujuhqdr74lk` FOREIGN KEY (`publisher_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `quests` WRITE;
/*!40000 ALTER TABLE `quests` DISABLE KEYS */;
/*!40000 ALTER TABLE `quests` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `repositories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `repositories` (
  `repository_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `default_branch` varchar(128) NOT NULL,
  `external_repository_id` varchar(128) DEFAULT NULL,
  `host_type` varchar(32) NOT NULL,
  `last_synced_at` datetime(6) DEFAULT NULL,
  `name` varchar(128) NOT NULL,
  `source_url` varchar(512) NOT NULL,
  `sync_error_message` varchar(512) DEFAULT NULL,
  `sync_status` varchar(32) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `owner_id` bigint NOT NULL,
  `description` varchar(512) DEFAULT NULL,
  PRIMARY KEY (`repository_id`),
  KEY `FKm4pu2ee4b1or1y004uiejg794` (`owner_id`),
  CONSTRAINT `FKm4pu2ee4b1or1y004uiejg794` FOREIGN KEY (`owner_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `repositories` WRITE;
/*!40000 ALTER TABLE `repositories` DISABLE KEYS */;
INSERT INTO `repositories` VALUES (1,'2026-06-06 12:50:12.388965','main',NULL,'GITEA','2026-06-06 12:50:12.557580','hello-world-mvp','http://localhost:3000/spike-admin/hello-world-mvp',NULL,'SYNCED','2026-06-06 12:50:12.560024',5,NULL);
/*!40000 ALTER TABLE `repositories` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `review_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review_items` (
  `item_id` bigint NOT NULL AUTO_INCREMENT,
  `checkpoint` varchar(128) NOT NULL,
  `comment` varchar(500) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `passed` bit(1) NOT NULL,
  `review_id` bigint NOT NULL,
  PRIMARY KEY (`item_id`),
  KEY `FKnge6cmhkhfk9q1qvqwbjh03e` (`review_id`),
  CONSTRAINT `FKnge6cmhkhfk9q1qvqwbjh03e` FOREIGN KEY (`review_id`) REFERENCES `review_records` (`review_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `review_items` WRITE;
/*!40000 ALTER TABLE `review_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `review_items` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `review_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review_records` (
  `review_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `decision` enum('APPROVED','CHANGES_REQUESTED','REJECTED') NOT NULL,
  `reviewed_at` datetime(6) NOT NULL,
  `summary` varchar(500) NOT NULL,
  `reviewer_id` bigint NOT NULL,
  `submission_id` bigint NOT NULL,
  PRIMARY KEY (`review_id`),
  KEY `FK9y48qhx4axwmmblpojpkem83r` (`reviewer_id`),
  KEY `FKte1q6gpse8q8tgu0a8u2pgn3s` (`submission_id`),
  CONSTRAINT `FK9y48qhx4axwmmblpojpkem83r` FOREIGN KEY (`reviewer_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKte1q6gpse8q8tgu0a8u2pgn3s` FOREIGN KEY (`submission_id`) REFERENCES `submissions` (`submission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `review_records` WRITE;
/*!40000 ALTER TABLE `review_records` DISABLE KEYS */;
/*!40000 ALTER TABLE `review_records` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `submissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `submissions` (
  `submission_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `description` text NOT NULL,
  `status` enum('APPROVED','CHANGES_REQUESTED','PENDING_REVIEW','REJECTED') NOT NULL,
  `submitted_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `pull_request_id` bigint NOT NULL,
  `quest_id` bigint NOT NULL,
  `submitter_id` bigint NOT NULL,
  PRIMARY KEY (`submission_id`),
  KEY `FKkdf11d3u4rsa761k87i7ecwj9` (`pull_request_id`),
  KEY `FK7k48utjjd7t8wwvftdm95oacj` (`quest_id`),
  KEY `FKjnorec1kmqhxwisxx065bp7it` (`submitter_id`),
  CONSTRAINT `FK7k48utjjd7t8wwvftdm95oacj` FOREIGN KEY (`quest_id`) REFERENCES `quests` (`quest_id`),
  CONSTRAINT `FKjnorec1kmqhxwisxx065bp7it` FOREIGN KEY (`submitter_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKkdf11d3u4rsa761k87i7ecwj9` FOREIGN KEY (`pull_request_id`) REFERENCES `pull_requests` (`pull_request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `submissions` WRITE;
/*!40000 ALTER TABLE `submissions` DISABLE KEYS */;
/*!40000 ALTER TABLE `submissions` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `user_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(128) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `role` enum('ADMIN','BEGINNER','MAINTAINER') NOT NULL,
  `status` enum('ACTIVE','DISABLED') NOT NULL,
  `token_version` int NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `username` varchar(32) NOT NULL,
  `avatar_url` varchar(512) DEFAULT NULL,
  `display_badge_id` varchar(64) DEFAULT NULL,
  `motto` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (3,'2026-06-03 13:09:49.272054','admin@gg.local','$2a$10$Qd5H2WYJeBvIPgSo9tVf9eoIglSPUzsAnJH8Be/tDVUX/JNyD4XyC','ADMIN','ACTIVE',0,'2026-06-06 12:50:12.099071','admin',NULL,NULL,NULL),(5,'2026-06-04 06:11:55.000000','guild@gg.local','$2a$10$Qd5H2WYJeBvIPgSo9tVf9eoIglSPUzsAnJH8Be/tDVUX/JNyD4XyC','MAINTAINER','ACTIVE',0,'2026-06-06 12:50:12.099071','guild',NULL,NULL,NULL),(6,'2026-06-04 06:11:55.000000','advent@gg.local','$2a$10$Qd5H2WYJeBvIPgSo9tVf9eoIglSPUzsAnJH8Be/tDVUX/JNyD4XyC','BEGINNER','ACTIVE',0,'2026-06-06 12:50:12.099071','advent',NULL,NULL,NULL);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `xp_transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `xp_transactions` (
  `transaction_id` bigint NOT NULL AUTO_INCREMENT,
  `amount` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `reason` varchar(128) NOT NULL,
  `quest_id` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`transaction_id`),
  KEY `FK41xsvi3uf56v3h4miq8artspf` (`quest_id`),
  KEY `FKnow46h8nu701itd5e03slgeyq` (`user_id`),
  CONSTRAINT `FK41xsvi3uf56v3h4miq8artspf` FOREIGN KEY (`quest_id`) REFERENCES `quests` (`quest_id`),
  CONSTRAINT `FKnow46h8nu701itd5e03slgeyq` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `xp_transactions` WRITE;
/*!40000 ALTER TABLE `xp_transactions` DISABLE KEYS */;
/*!40000 ALTER TABLE `xp_transactions` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

