
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
  `decision` enum('APPROVE_PUBLISH','REJECT_PUBLISH','REOPEN','TAKE_DOWN') NOT NULL,
  `reason` varchar(500) NOT NULL,
  `reviewed_at` datetime(6) NOT NULL,
  `visible_to_publisher` bit(1) NOT NULL,
  `admin_id` bigint NOT NULL,
  `quest_id` bigint NOT NULL,
  `checklist_json` text,
  PRIMARY KEY (`admin_review_id`),
  KEY `FKqpto5guc93e6sq1ypp6s0s5ab` (`admin_id`),
  KEY `FKg8i4e805fpk117dj10walbp1n` (`quest_id`),
  CONSTRAINT `FKg8i4e805fpk117dj10walbp1n` FOREIGN KEY (`quest_id`) REFERENCES `quests` (`quest_id`),
  CONSTRAINT `FKqpto5guc93e6sq1ypp6s0s5ab` FOREIGN KEY (`admin_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `admin_review_records` WRITE;
/*!40000 ALTER TABLE `admin_review_records` DISABLE KEYS */;
INSERT INTO `admin_review_records` VALUES (1,'2026-06-07 05:54:09.614540','APPROVE_PUBLISH','通过','2026-06-07 05:54:09.613816',_binary '',3,3,NULL),(2,'2026-06-07 06:49:52.471771','APPROVE_PUBLISH','3','2026-06-07 06:49:52.471135',_binary '',3,4,NULL),(3,'2026-06-14 01:58:49.827528','APPROVE_PUBLISH','3','2026-06-14 01:58:49.824722',_binary '',3,5,NULL),(4,'2026-06-27 05:05:58.129825','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:05:58.129358',_binary '',3,7,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(5,'2026-06-27 05:06:00.103557','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:06:00.103344',_binary '',3,8,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(6,'2026-06-27 05:06:01.460400','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:06:01.460146',_binary '',3,9,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(7,'2026-06-27 05:06:02.661006','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:06:02.660773',_binary '',3,10,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(8,'2026-06-27 05:06:03.825249','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:06:03.825116',_binary '',3,11,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(9,'2026-06-27 05:06:05.395251','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:06:05.395002',_binary '',3,12,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(10,'2026-06-27 05:06:07.146906','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:06:07.146743',_binary '',3,13,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(11,'2026-06-27 05:06:08.431952','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:06:08.431647',_binary '',3,14,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(12,'2026-06-27 05:06:09.695369','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:06:09.695257',_binary '',3,15,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(13,'2026-06-27 05:06:09.811148','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:06:09.811060',_binary '',3,16,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(14,'2026-06-27 05:06:11.429981','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:06:11.429872',_binary '',3,17,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(15,'2026-06-27 05:06:11.551214','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:06:11.551102',_binary '',3,18,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(16,'2026-06-27 05:06:12.746243','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:06:12.746100',_binary '',3,19,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(17,'2026-06-27 05:06:13.189627','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:06:13.189349',_binary '',3,20,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(18,'2026-06-27 05:06:14.434917','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:06:14.434843',_binary '',3,21,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(19,'2026-06-27 05:06:14.549197','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:06:14.548940',_binary '',3,22,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(20,'2026-06-27 05:06:14.644139','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:06:14.644059',_binary '',3,23,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(21,'2026-06-27 05:35:49.325049','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:35:49.324918',_binary '',3,28,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(22,'2026-06-27 05:35:49.444686','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:35:49.444580',_binary '',3,29,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(23,'2026-06-27 05:35:49.564827','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:35:49.564727',_binary '',3,30,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(24,'2026-06-27 05:35:49.678501','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:35:49.678393',_binary '',3,31,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(25,'2026-06-27 05:35:49.793523','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:35:49.793404',_binary '',3,32,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(26,'2026-06-27 05:35:49.924367','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:35:49.924189',_binary '',3,33,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(27,'2026-06-27 05:35:50.056344','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:35:50.056158',_binary '',3,34,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(28,'2026-06-27 05:35:50.181595','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:35:50.181447',_binary '',3,35,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(29,'2026-06-27 05:35:50.292586','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:35:50.292486',_binary '',3,36,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(30,'2026-06-27 05:35:50.406618','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:35:50.406525',_binary '',3,37,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(31,'2026-06-27 05:35:50.518287','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:35:50.518169',_binary '',3,38,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(32,'2026-06-27 05:35:50.647278','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:35:50.647179',_binary '',3,39,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(33,'2026-06-27 05:35:50.774399','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:35:50.774301',_binary '',3,40,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(34,'2026-06-27 05:35:50.889793','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:35:50.889690',_binary '',3,41,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(35,'2026-06-27 05:35:51.007480','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:35:51.007372',_binary '',3,42,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(36,'2026-06-27 05:35:51.111347','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:35:51.111263',_binary '',3,43,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(37,'2026-06-27 05:35:51.214594','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:35:51.214504',_binary '',3,44,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(38,'2026-06-27 05:35:51.340610','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:35:51.340461',_binary '',3,45,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(39,'2026-06-27 05:35:51.473595','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:35:51.473469',_binary '',3,46,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(40,'2026-06-27 05:35:51.604040','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:35:51.603922',_binary '',3,47,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(41,'2026-06-27 05:35:51.724904','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:35:51.724717',_binary '',3,48,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(42,'2026-06-27 05:35:51.867205','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:35:51.867114',_binary '',3,49,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]'),(43,'2026-06-27 05:35:51.988502','APPROVE_PUBLISH','信息完整，技术栈合规，准予上架。','2026-06-27 05:35:51.988394',_binary '',3,50,'[{\"label\":\"标题清晰\",\"passed\":true},{\"label\":\"描述完整\",\"passed\":true},{\"label\":\"完成标准可验证\",\"passed\":true},{\"label\":\"技术栈合规\",\"passed\":true},{\"label\":\"奖励合理\",\"passed\":true},{\"label\":\"无敏感信息\",\"passed\":true}]');
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
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `contribution_records` WRITE;
/*!40000 ALTER TABLE `contribution_records` DISABLE KEYS */;
INSERT INTO `contribution_records` VALUES (1,'2026-06-07 06:03:45.380981','2026-06-07 06:03:45.381490','任务二',3,1,6),(2,'2026-06-07 06:50:58.340681','2026-06-07 06:50:58.340909','Hello World',4,1,6),(3,'2026-06-27 05:05:59.401617','2026-06-27 05:05:59.402297','任务板：增加难度筛选 / Quest board difficulty filter',7,6,22),(4,'2026-06-27 05:06:01.265325','2026-06-27 05:06:01.265472','工作台进度可视化 / Workbench progress bar',8,6,23),(5,'2026-06-27 05:06:02.475431','2026-06-27 05:06:02.475591','修复登录页回车不提交 / Fix Enter key on login',9,6,24),(6,'2026-06-27 05:06:03.653159','2026-06-27 05:06:03.653356','任务搜索增加按 XP 排序 / Sort quests by reward XP',10,7,25),(7,'2026-06-27 05:06:04.796359','2026-06-27 05:06:04.796503','任务详情接口缓存 / Cache quest detail with Redis',11,7,26),(8,'2026-06-27 05:06:06.586346','2026-06-27 05:06:06.586493','禁止接取自己发布的任务 / Block self-assignment',12,7,27),(9,'2026-06-27 05:06:08.242899','2026-06-27 05:06:08.243072','提交闭环集成测试 / Integration test for submission flow',13,7,28),(10,'2026-06-27 05:06:09.515209','2026-06-27 05:06:09.515299','CLI: quest list 命令 / Add `quest list` command',14,8,29),(11,'2026-06-27 05:06:10.868026','2026-06-27 05:06:10.868130','冒险者快速上手 / Adventurer quick start',16,9,31),(12,'2026-06-27 05:06:12.581385','2026-06-27 05:06:12.581468','打回通知 / Notify on changes requested',18,10,33),(13,'2026-06-27 05:06:14.258783','2026-06-27 05:06:14.258846','月度排行榜 / Monthly leaderboard',20,11,35);
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
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `growth_profiles` WRITE;
/*!40000 ALTER TABLE `growth_profiles` DISABLE KEYS */;
INSERT INTO `growth_profiles` VALUES (1,2,'2026-06-07 06:03:45.373428',2,100,'2026-06-07 06:50:58.361419',6),(2,1,'2026-06-27 05:05:59.393173',1,80,'2026-06-27 05:05:59.393173',22),(3,1,'2026-06-27 05:06:01.261831',2,140,'2026-06-27 05:06:01.261831',23),(4,1,'2026-06-27 05:06:02.471838',1,40,'2026-06-27 05:06:02.471838',24),(5,1,'2026-06-27 05:06:03.650404',2,160,'2026-06-27 05:06:03.650404',25),(6,1,'2026-06-27 05:06:04.793483',3,260,'2026-06-27 05:06:04.793483',26),(7,1,'2026-06-27 05:06:06.582696',1,90,'2026-06-27 05:06:06.582696',27),(8,1,'2026-06-27 05:06:08.238209',2,130,'2026-06-27 05:06:08.238209',28),(9,1,'2026-06-27 05:06:09.512600',2,100,'2026-06-27 05:06:09.512600',29),(10,1,'2026-06-27 05:06:10.864781',1,45,'2026-06-27 05:06:10.864781',31),(11,1,'2026-06-27 05:06:12.578692',2,150,'2026-06-27 05:06:12.578692',33),(12,1,'2026-06-27 05:06:14.256656',3,240,'2026-06-27 05:06:14.256656',35);
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
) ENGINE=InnoDB AUTO_INCREMENT=52 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `issues` WRITE;
/*!40000 ALTER TABLE `issues` DISABLE KEYS */;
INSERT INTO `issues` VALUES (1,NULL,'2026-06-06 12:50:12.547997','1','http://localhost:3000/spike-admin/hello-world-mvp/issues/1','OPEN','2026-06-06 12:50:12.545273','Add hello world file','2026-06-06 12:50:12.547997',1),(4,'任务二','2026-06-07 05:52:25.898334','4','http://localhost:3000/spike-admin/hello-world-mvp/issues/4','open','2026-06-07 05:52:25.894559','2-第二个任务','2026-06-07 05:52:25.898334',1),(5,'3','2026-06-07 06:48:49.662071','6','http://localhost:3000/spike-admin/hello-world-mvp/issues/6','open','2026-06-07 06:48:49.661581','3','2026-06-07 06:48:49.662071',1),(6,'3','2026-06-14 01:26:58.805464','8','http://localhost:3000/spike-admin/hello-world-mvp/issues/8','open','2026-06-14 01:26:58.802525','3','2026-06-14 01:26:58.805464',1),(7,'用于演示的issue','2026-06-18 07:14:23.801521','1','http://localhost:3000/spike-admin/gitguild-demo/issues/1','open','2026-06-18 07:14:23.800906','1','2026-06-18 07:14:23.801521',3),(8,'悬赏任务板目前只能按分类筛选。需要新增一个难度（A/B/C/D）下拉筛选器，并与现有筛选条件组合生效。\n\nThe quest board only filters by category. Add a difficulty (A/B/C/D) dropdown filter that composes with the existing filters.','2026-06-27 05:05:58.026781','1','http://localhost:3000/spike-admin/gitguild-web/issues/1','open','2026-06-27 05:05:58.026113','任务板增加难度筛选下拉框','2026-06-27 05:05:58.026781',6),(9,'在冒险者工作台为每个进行中的任务加上状态进度条（接取→分支→提交→审核→合并）。\n\nAdd a status progress bar to each active quest on the adventurer workbench.','2026-06-27 05:06:00.026589','3','http://localhost:3000/spike-admin/gitguild-web/issues/3','open','2026-06-27 05:06:00.026273','冒险者工作台显示任务进度条','2026-06-27 05:06:00.026589',6),(10,'登录表单在输入框按下回车不会触发提交，必须点击按钮。需修复键盘可达性。\n\nPressing Enter in the login form does not submit; fix keyboard accessibility.','2026-06-27 05:06:01.407694','5','http://localhost:3000/spike-admin/gitguild-web/issues/5','open','2026-06-27 05:06:01.407501','登录页回车键无法提交','2026-06-27 05:06:01.407694',6),(11,'GET /api/v1/quests 目前仅支持按创建时间排序。需要支持 sortBy=rewardXp 的升降序。\n\nAdd sortBy=rewardXp (asc/desc) to the quest search endpoint.','2026-06-27 05:06:02.612493','1','http://localhost:3000/spike-admin/gitguild-api/issues/1','open','2026-06-27 05:06:02.612332','任务搜索支持按奖励 XP 排序','2026-06-27 05:06:02.612493',7),(12,'热门任务详情被频繁读取。引入 Redis 缓存任务详情，并在任务状态变更时正确失效。\n\nCache quest detail in Redis and invalidate it on quest state transitions.','2026-06-27 05:06:03.780571','3','http://localhost:3000/spike-admin/gitguild-api/issues/3','open','2026-06-27 05:06:03.780420','为任务详情接口增加 Redis 缓存','2026-06-27 05:06:03.780571',7),(13,'发布者接取自己的任务时返回了 500。应返回 409 并给出业务错误码。\n\nReturn a clean 409 business error when a publisher accepts their own quest.','2026-06-27 05:06:05.337852','5','http://localhost:3000/spike-admin/gitguild-api/issues/5','open','2026-06-27 05:06:05.337460','接取自己发布的任务应被拒绝并返回明确错误','2026-06-27 05:06:05.337852',7),(14,'发布→接取→提交→审核→合并的全链路缺少端到端集成测试，回归风险高。\n\nAdd an end-to-end integration test for the full quest lifecycle.','2026-06-27 05:06:07.095555','7','http://localhost:3000/spike-admin/gitguild-api/issues/7','open','2026-06-27 05:06:07.095421','为提交-审核-合并闭环补集成测试','2026-06-27 05:06:07.095555',7),(15,'为 quest-cli 增加 `quest list` 子命令，调用平台 API 拉取并表格化展示可接取任务。\n\nAdd a `quest list` subcommand that fetches and tabulates open quests.','2026-06-27 05:06:08.382797','1','http://localhost:3000/spike-admin/quest-cli/issues/1','open','2026-06-27 05:06:08.382652','CLI 增加 quest list 命令','2026-06-27 05:06:08.382797',8),(16,'quest-cli 缺少安装与 token 配置说明，新人无法上手。\n\nThe CLI lacks install and token-config docs.','2026-06-27 05:06:09.653202','3','http://localhost:3000/spike-admin/quest-cli/issues/3','open','2026-06-27 05:06:09.653075','补充 CLI 安装与鉴权说明','2026-06-27 05:06:09.653202',8),(17,'为新冒险者撰写一页式快速上手指南：注册、浏览任务板、接取、提交。\n\nWrite a one-page quick start for new adventurers.','2026-06-27 05:06:09.772902','1','http://localhost:3000/spike-admin/guild-docs/issues/1','open','2026-06-27 05:06:09.772768','撰写冒险者快速上手指南','2026-06-27 05:06:09.772902',9),(18,'把分散在代码里的接口整理成一份对外 API 参考，按模块分章。\n\nConsolidate the scattered endpoints into a public API reference.','2026-06-27 05:06:11.388262','3','http://localhost:3000/spike-admin/guild-docs/issues/3','open','2026-06-27 05:06:11.388123','整理对外 REST API 参考','2026-06-27 05:06:11.388262',9),(19,'审核结论为“需修改”时，应给提交者发送站内通知并标记未读。\n\nSend an in-app notification to the submitter when a review requests changes.','2026-06-27 05:06:11.511022','1','http://localhost:3000/spike-admin/notify-service/issues/1','open','2026-06-27 05:06:11.510861','提交被打回时给冒险者发站内通知','2026-06-27 05:06:11.511022',10),(20,'并发标记已读时偶发状态回滚，导致红点不消失。\n\nConcurrent mark-as-read occasionally rolls back, leaving the unread badge.','2026-06-27 05:06:12.707648','3','http://localhost:3000/spike-admin/notify-service/issues/3','open','2026-06-27 05:06:12.707543','通知已读状态偶发不生效','2026-06-27 05:06:12.707648',10),(21,'现有排行榜只有总榜。需要增加按自然月聚合的 XP 排行，并支持月份切换。\n\nAdd a monthly XP leaderboard aggregated by calendar month.','2026-06-27 05:06:13.139202','1','http://localhost:3000/spike-admin/leaderboard-svc/issues/1','open','2026-06-27 05:06:13.139082','排行榜按月度维度统计','2026-06-27 05:06:13.139202',11),(22,'拉取成长档案时逐条查询 XP 流水，产生 N+1。需用一次聚合查询替代。\n\nFetching growth profiles triggers N+1 on XP transactions; replace with one aggregate query.','2026-06-27 05:06:14.398499','3','http://localhost:3000/spike-admin/leaderboard-svc/issues/3','open','2026-06-27 05:06:14.398401','成长档案查询消除 N+1','2026-06-27 05:06:14.398499',11),(23,'为后端补一个多阶段 Dockerfile，缩小镜像体积并加快构建。\n\nAdd a multi-stage Dockerfile for the backend to shrink the image.','2026-06-27 05:06:14.511052','9','http://localhost:3000/spike-admin/gitguild-api/issues/9','open','2026-06-27 05:06:14.510751','后端补充多阶段 Docker 构建','2026-06-27 05:06:14.511052',7),(24,'任务卡片当前不展示技术栈。需要把技术栈渲染成彩色小标签，便于快速识别。\n\nRender the quest tech stack as colored chips on the quest card.','2026-06-27 05:06:14.599053','7','http://localhost:3000/spike-admin/gitguild-web/issues/7','open','2026-06-27 05:06:14.598938','任务卡片展示技术栈标签','2026-06-27 05:06:14.599053',6),(25,'目前刷新令牌长期有效存在安全隐患。需实现一次性刷新令牌轮换与重放检测。\n\nImplement one-time refresh-token rotation with replay detection.','2026-06-27 05:06:14.693658','10','http://localhost:3000/spike-admin/gitguild-api/issues/10','open','2026-06-27 05:06:14.693560','接入刷新令牌轮换','2026-06-27 05:06:14.693658',7),(26,'用户希望一键清空未读。需要提供批量已读接口。\n\nProvide a bulk mark-as-read endpoint.','2026-06-27 05:06:14.756121','4','http://localhost:3000/spike-admin/notify-service/issues/4','open','2026-06-27 05:06:14.756028','通知支持批量已读','2026-06-27 05:06:14.756121',10),(27,'仓库缺少 CODE_OF_CONDUCT，社区协作无明确规范。\n\nThe repo lacks a CODE_OF_CONDUCT.','2026-06-27 05:06:14.816199','4','http://localhost:3000/spike-admin/guild-docs/issues/4','open','2026-06-27 05:06:14.816106','补充贡献者行为准则','2026-06-27 05:06:14.816199',9),(28,'CLI 命令缺测试，重构易回归。\n\nThe CLI commands lack tests, making refactors risky.','2026-06-27 05:06:14.874003','4','http://localhost:3000/spike-admin/quest-cli/issues/4','open','2026-06-27 05:06:14.873898','CLI 命令补充集成测试','2026-06-27 05:06:14.874003',8),(29,'任务卡片骨架屏加载 / Skeleton loading for quest cards\n\n本任务用于丰富悬赏板演示数据。\n首屏与翻页时展示骨架屏；数据到达后平滑替换；含组件测试。','2026-06-27 05:35:49.270643','8','http://localhost:3000/spike-admin/gitguild-web/issues/8','open','2026-06-27 05:35:49.270514','任务卡片骨架屏加载','2026-06-27 05:35:49.270643',6),(30,'任务列表接口加游标分页 / Cursor pagination\n\n本任务用于丰富悬赏板演示数据。\n大数据量下用游标分页替代 offset；兼容旧参数。','2026-06-27 05:35:49.388461','11','http://localhost:3000/spike-admin/gitguild-api/issues/11','open','2026-06-27 05:35:49.388260','任务列表接口加游标分页','2026-06-27 05:35:49.388461',7),(31,'CLI 增加 quest accept 命令 / `quest accept`\n\n本任务用于丰富悬赏板演示数据。\n按 questId 接取并打印任务分支信息；无 token 友好提示。','2026-06-27 05:35:49.509013','5','http://localhost:3000/spike-admin/quest-cli/issues/5','open','2026-06-27 05:35:49.508898','CLI 增加 quest accept 命令','2026-06-27 05:35:49.509013',8),(32,'撰写维护者发布指南 / Maintainer publishing guide\n\n本任务用于丰富悬赏板演示数据。\n覆盖建仓→建 Issue→发布→审核全流程；中英双语。','2026-06-27 05:35:49.626567','5','http://localhost:3000/spike-admin/guild-docs/issues/5','open','2026-06-27 05:35:49.626449','撰写维护者发布指南','2026-06-27 05:35:49.626567',9),(33,'任务被接取时通知发布者 / Notify on accept\n\n本任务用于丰富悬赏板演示数据。\n有人接取即给发布者发 UNREAD 通知；含任务标题。','2026-06-27 05:35:49.744937','5','http://localhost:3000/spike-admin/notify-service/issues/5','open','2026-06-27 05:35:49.744804','任务被接取时通知发布者','2026-06-27 05:35:49.744937',10),(34,'排行榜分页与我的排名 / Pagination & my rank\n\n本任务用于丰富悬赏板演示数据。\n排行榜分页；额外返回当前用户排名，即使不在本页。','2026-06-27 05:35:49.860193','4','http://localhost:3000/spike-admin/leaderboard-svc/issues/4','open','2026-06-27 05:35:49.860076','排行榜分页与我的排名','2026-06-27 05:35:49.860193',11),(35,'悬赏板支持关键字搜索 / Keyword search on board\n\n本任务用于丰富悬赏板演示数据。\n标题/描述模糊匹配；与现有筛选叠加；防抖输入。','2026-06-27 05:35:49.993378','9','http://localhost:3000/spike-admin/gitguild-web/issues/9','open','2026-06-27 05:35:49.993226','悬赏板支持关键字搜索','2026-06-27 05:35:49.993378',6),(36,'接取并发幂等保护 / Idempotent accept under race\n\n本任务用于丰富悬赏板演示数据。\n并发接取同一任务只产生一条有效接取；补并发测试。','2026-06-27 05:35:50.125916','12','http://localhost:3000/spike-admin/gitguild-api/issues/12','open','2026-06-27 05:35:50.125806','接取并发幂等保护','2026-06-27 05:35:50.125916',7),(37,'CLI 输出支持 JSON 格式 / JSON output\n\n本任务用于丰富悬赏板演示数据。\n所有列表命令加 --json；便于脚本消费。','2026-06-27 05:35:50.241082','6','http://localhost:3000/spike-admin/quest-cli/issues/6','open','2026-06-27 05:35:50.240981','CLI 输出支持 JSON 格式','2026-06-27 05:35:50.241082',8),(38,'整理常见问题 FAQ / FAQ page\n\n本任务用于丰富悬赏板演示数据。\n汇总登录、接取、提交常见问题与排查；通过文档站构建。','2026-06-27 05:35:50.355360','6','http://localhost:3000/spike-admin/guild-docs/issues/6','open','2026-06-27 05:35:50.355226','整理常见问题 FAQ','2026-06-27 05:35:50.355360',9),(39,'通知支持分页拉取 / Paginated notifications\n\n本任务用于丰富悬赏板演示数据。\n未读优先、按时间倒序、分页返回；含测试。','2026-06-27 05:35:50.469468','6','http://localhost:3000/spike-admin/notify-service/issues/6','open','2026-06-27 05:35:50.469355','通知支持分页拉取','2026-06-27 05:35:50.469468',10),(40,'成长等级曲线可配置 / Configurable level curve\n\n本任务用于丰富悬赏板演示数据。\n等级所需 XP 改为可配置表；现有数据平滑迁移。','2026-06-27 05:35:50.592976','5','http://localhost:3000/spike-admin/leaderboard-svc/issues/5','open','2026-06-27 05:35:50.592861','成长等级曲线可配置','2026-06-27 05:35:50.592976',11),(41,'任务详情页响应式适配 / Responsive quest detail\n\n本任务用于丰富悬赏板演示数据。\n移动端/平板布局自适应；图片与代码块不溢出。','2026-06-27 05:35:50.718187','10','http://localhost:3000/spike-admin/gitguild-web/issues/10','open','2026-06-27 05:35:50.718068','任务详情页响应式适配','2026-06-27 05:35:50.718187',6),(42,'发布任务参数校验增强 / Stronger create validation\n\n本任务用于丰富悬赏板演示数据。\n对 XP/工时上限、技术栈非空做服务层校验并返回明确错误码。','2026-06-27 05:35:50.836087','13','http://localhost:3000/spike-admin/gitguild-api/issues/13','open','2026-06-27 05:35:50.835977','发布任务参数校验增强','2026-06-27 05:35:50.836087',7),(43,'CLI 配置文件支持 / Config file support\n\n本任务用于丰富悬赏板演示数据。\n支持 ~/.questrc 存 token 与服务地址；环境变量覆盖。','2026-06-27 05:35:50.960746','7','http://localhost:3000/spike-admin/quest-cli/issues/7','open','2026-06-27 05:35:50.960617','CLI 配置文件支持','2026-06-27 05:35:50.960746',8),(44,'补充架构概览图 / Architecture overview\n\n本任务用于丰富悬赏板演示数据。\n前端/后端/Gitea/MySQL 关系图 + 文字说明。','2026-06-27 05:35:51.067899','7','http://localhost:3000/spike-admin/guild-docs/issues/7','open','2026-06-27 05:35:51.067804','补充架构概览图','2026-06-27 05:35:51.067899',9),(45,'邮件通知开关 / Email notification opt-in\n\n本任务用于丰富悬赏板演示数据。\n用户可开关邮件通知；关闭时仅站内通知。','2026-06-27 05:35:51.166560','7','http://localhost:3000/spike-admin/notify-service/issues/7','open','2026-06-27 05:35:51.166466','邮件通知开关','2026-06-27 05:35:51.166560',10),(46,'贡献热力统计 / Contribution heatmap data\n\n本任务用于丰富悬赏板演示数据。\n按日聚合贡献次数，提供热力图所需接口。','2026-06-27 05:35:51.281514','6','http://localhost:3000/spike-admin/leaderboard-svc/issues/6','open','2026-06-27 05:35:51.281382','贡献热力统计','2026-06-27 05:35:51.281514',11),(47,'暗色主题切换 / Dark mode toggle\n\n本任务用于丰富悬赏板演示数据。\n全局暗色主题；偏好持久化到本地存储。','2026-06-27 05:35:51.409160','11','http://localhost:3000/spike-admin/gitguild-web/issues/11','open','2026-06-27 05:35:51.409006','暗色主题切换','2026-06-27 05:35:51.409160',6),(48,'任务搜索支持多标签过滤 / Multi-tag filter\n\n本任务用于丰富悬赏板演示数据。\ntagIds 传多个时取交集；分页正确；新增 MVC 测试。','2026-06-27 05:35:51.542530','14','http://localhost:3000/spike-admin/gitguild-api/issues/14','open','2026-06-27 05:35:51.542399','任务搜索支持多标签过滤','2026-06-27 05:35:51.542530',7),(49,'排行榜缓存与定时刷新 / Cache & refresh\n\n本任务用于丰富悬赏板演示数据。\n排行榜结果缓存并定时刷新；缓存击穿保护。','2026-06-27 05:35:51.670750','7','http://localhost:3000/spike-admin/leaderboard-svc/issues/7','open','2026-06-27 05:35:51.670632','排行榜缓存与定时刷新','2026-06-27 05:35:51.670750',11),(50,'接取按钮加二次确认 / Confirm before accept\n\n本任务用于丰富悬赏板演示数据。\n点击接取弹确认框，避免误触；含交互测试。','2026-06-27 05:35:51.819583','12','http://localhost:3000/spike-admin/gitguild-web/issues/12','open','2026-06-27 05:35:51.819465','接取按钮加二次确认','2026-06-27 05:35:51.819583',6),(51,'操作审计日志 / Audit log for quest actions\n\n本任务用于丰富悬赏板演示数据。\n发布/审核/接取/合并写审计；可按用户与时间查询。','2026-06-27 05:35:51.930744','15','http://localhost:3000/spike-admin/gitguild-api/issues/15','open','2026-06-27 05:35:51.930586','操作审计日志','2026-06-27 05:35:51.930744',7);
/*!40000 ALTER TABLE `issues` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `message_read_states`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `message_read_states` (
  `read_state_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `last_read_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `thread_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`read_state_id`),
  KEY `FKc5oyh29w3vih4mk94vy3rv830` (`thread_id`),
  KEY `FKa62dtx1lpok7f4405h71t4ilm` (`user_id`),
  CONSTRAINT `FKa62dtx1lpok7f4405h71t4ilm` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKc5oyh29w3vih4mk94vy3rv830` FOREIGN KEY (`thread_id`) REFERENCES `message_threads` (`thread_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `message_read_states` WRITE;
/*!40000 ALTER TABLE `message_read_states` DISABLE KEYS */;
/*!40000 ALTER TABLE `message_read_states` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `message_threads`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `message_threads` (
  `thread_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `last_message_at` datetime(6) NOT NULL,
  `status` enum('ACTIVE','ARCHIVED') NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `assignee_id` bigint NOT NULL,
  `publisher_id` bigint NOT NULL,
  `quest_id` bigint NOT NULL,
  PRIMARY KEY (`thread_id`),
  UNIQUE KEY `UKg200vvv5afjetuxxjbyoltefl` (`quest_id`),
  KEY `FK4udsxigwdt7e4xsgjbvs08w95` (`assignee_id`),
  KEY `FK9e99ircc2yiftxs8mf9s77bws` (`publisher_id`),
  CONSTRAINT `FK4udsxigwdt7e4xsgjbvs08w95` FOREIGN KEY (`assignee_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FK691b4up5b1ydutu0a1xsp0sxe` FOREIGN KEY (`quest_id`) REFERENCES `quests` (`quest_id`),
  CONSTRAINT `FK9e99ircc2yiftxs8mf9s77bws` FOREIGN KEY (`publisher_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `message_threads` WRITE;
/*!40000 ALTER TABLE `message_threads` DISABLE KEYS */;
/*!40000 ALTER TABLE `message_threads` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `messages` (
  `message_id` bigint NOT NULL AUTO_INCREMENT,
  `content` text NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `sender_id` bigint NOT NULL,
  `thread_id` bigint NOT NULL,
  PRIMARY KEY (`message_id`),
  KEY `FK4ui4nnwntodh6wjvck53dbk9m` (`sender_id`),
  KEY `FKp50ts6rmerpigf6yqek16p6ay` (`thread_id`),
  CONSTRAINT `FK4ui4nnwntodh6wjvck53dbk9m` FOREIGN KEY (`sender_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKp50ts6rmerpigf6yqek16p6ay` FOREIGN KEY (`thread_id`) REFERENCES `message_threads` (`thread_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `messages` WRITE;
/*!40000 ALTER TABLE `messages` DISABLE KEYS */;
/*!40000 ALTER TABLE `messages` ENABLE KEYS */;
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
  `type` enum('BADGE_UNLOCKED','LEVEL_UP','MESSAGE_RECEIVED','PR_MERGED','QUEST_ACCEPTED','QUEST_APPROVED','QUEST_REJECTED','QUEST_REOPENED','QUEST_TAKEN_DOWN','REVIEW_APPROVED','REVIEW_CHANGES_REQUESTED','REVIEW_REJECTED','SUBMISSION_RECEIVED') NOT NULL,
  `receiver_id` bigint NOT NULL,
  PRIMARY KEY (`notification_id`),
  KEY `FK9kxl0whvhifo6gw4tjq36v53k` (`receiver_id`),
  CONSTRAINT `FK9kxl0whvhifo6gw4tjq36v53k` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=113 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `notifications` WRITE;
/*!40000 ALTER TABLE `notifications` DISABLE KEYS */;
INSERT INTO `notifications` VALUES (1,'冒险家 advent 提交了任务《任务二》的成果，等待你的审核。','2026-06-07 06:03:25.861287','2026-06-14 01:25:48.761052',1,'SUBMISSION','READ','SUBMISSION_RECEIVED',5),(2,'你的任务《任务二》已通过审核，获得 50 XP，等级与贡献记录已更新。','2026-06-07 06:03:45.404532','2026-06-20 11:01:46.457320',1,'SUBMISSION','READ','REVIEW_APPROVED',6),(3,'冒险家 advent 提交了任务《Hello World》的成果，等待你的审核。','2026-06-07 06:50:41.040652','2026-06-14 01:25:47.228951',2,'SUBMISSION','READ','SUBMISSION_RECEIVED',5),(4,'你的任务《Hello World》已通过审核，获得 50 XP，等级与贡献记录已更新。','2026-06-07 06:50:58.354715','2026-06-07 14:17:44.493442',2,'SUBMISSION','READ','REVIEW_APPROVED',6),(5,'你的委托《任务板：增加难度筛选 / Quest board difficulty filter》已通过管理员审核并上架到悬赏板。','2026-06-27 05:05:58.136845',NULL,7,'QUEST','UNREAD','QUEST_APPROVED',13),(6,'冒险家 novice_chen 接取了你的委托《任务板：增加难度筛选 / Quest board difficulty filter》。','2026-06-27 05:05:58.182311',NULL,7,'QUEST','UNREAD','QUEST_ACCEPTED',13),(7,'冒险家 novice_chen 提交了任务《任务板：增加难度筛选 / Quest board difficulty filter》的成果，等待你的审核。','2026-06-27 05:05:59.291567',NULL,3,'SUBMISSION','UNREAD','SUBMISSION_RECEIVED',13),(8,'解锁新徽章「首次贡献」：完成第一个任务后获得。','2026-06-27 05:05:59.407299',NULL,NULL,NULL,'UNREAD','BADGE_UNLOCKED',22),(9,'你的任务《任务板：增加难度筛选 / Quest board difficulty filter》已通过审核，获得 80 XP，等级与贡献记录已更新。','2026-06-27 05:05:59.431778',NULL,3,'SUBMISSION','UNREAD','REVIEW_APPROVED',22),(10,'你的任务《任务板：增加难度筛选 / Quest board difficulty filter》对应的 PR 已被合并到目标分支。','2026-06-27 05:05:59.944474',NULL,3,'SUBMISSION','UNREAD','PR_MERGED',22),(11,'你的委托《工作台进度可视化 / Workbench progress bar》已通过管理员审核并上架到悬赏板。','2026-06-27 05:06:00.107446',NULL,8,'QUEST','UNREAD','QUEST_APPROVED',13),(12,'冒险家 squire_liu 接取了你的委托《工作台进度可视化 / Workbench progress bar》。','2026-06-27 05:06:00.154041',NULL,8,'QUEST','UNREAD','QUEST_ACCEPTED',13),(13,'冒险家 squire_liu 提交了任务《工作台进度可视化 / Workbench progress bar》的成果，等待你的审核。','2026-06-27 05:06:01.197943',NULL,4,'SUBMISSION','UNREAD','SUBMISSION_RECEIVED',13),(14,'恭喜！你升到了 Lv.2，继续加油！','2026-06-27 05:06:01.267902',NULL,NULL,NULL,'UNREAD','LEVEL_UP',23),(15,'解锁新徽章「首次贡献」：完成第一个任务后获得。','2026-06-27 05:06:01.274572',NULL,NULL,NULL,'UNREAD','BADGE_UNLOCKED',23),(16,'解锁新徽章「XP 学徒」：累计 XP 达到 100 后获得。','2026-06-27 05:06:01.282521',NULL,NULL,NULL,'UNREAD','BADGE_UNLOCKED',23),(17,'你的任务《工作台进度可视化 / Workbench progress bar》已通过审核，获得 140 XP，等级与贡献记录已更新。','2026-06-27 05:06:01.295474',NULL,4,'SUBMISSION','UNREAD','REVIEW_APPROVED',23),(18,'你的委托《修复登录页回车不提交 / Fix Enter key on login》已通过管理员审核并上架到悬赏板。','2026-06-27 05:06:01.463965',NULL,9,'QUEST','UNREAD','QUEST_APPROVED',13),(19,'冒险家 rogue_tang 接取了你的委托《修复登录页回车不提交 / Fix Enter key on login》。','2026-06-27 05:06:01.492945',NULL,9,'QUEST','UNREAD','QUEST_ACCEPTED',13),(20,'冒险家 rogue_tang 提交了任务《修复登录页回车不提交 / Fix Enter key on login》的成果，等待你的审核。','2026-06-27 05:06:02.429858',NULL,5,'SUBMISSION','UNREAD','SUBMISSION_RECEIVED',13),(21,'解锁新徽章「首次贡献」：完成第一个任务后获得。','2026-06-27 05:06:02.478551',NULL,NULL,NULL,'UNREAD','BADGE_UNLOCKED',24),(22,'你的任务《修复登录页回车不提交 / Fix Enter key on login》已通过审核，获得 40 XP，等级与贡献记录已更新。','2026-06-27 05:06:02.491799',NULL,5,'SUBMISSION','UNREAD','REVIEW_APPROVED',24),(23,'你的委托《任务搜索增加按 XP 排序 / Sort quests by reward XP》已通过管理员审核并上架到悬赏板。','2026-06-27 05:06:02.666463',NULL,10,'QUEST','UNREAD','QUEST_APPROVED',14),(24,'冒险家 mage_apprentice_xu 接取了你的委托《任务搜索增加按 XP 排序 / Sort quests by reward XP》。','2026-06-27 05:06:02.692773',NULL,10,'QUEST','UNREAD','QUEST_ACCEPTED',14),(25,'冒险家 mage_apprentice_xu 提交了任务《任务搜索增加按 XP 排序 / Sort quests by reward XP》的成果，等待你的审核。','2026-06-27 05:06:03.610780',NULL,6,'SUBMISSION','UNREAD','SUBMISSION_RECEIVED',14),(26,'恭喜！你升到了 Lv.2，继续加油！','2026-06-27 05:06:03.655611',NULL,NULL,NULL,'UNREAD','LEVEL_UP',25),(27,'解锁新徽章「首次贡献」：完成第一个任务后获得。','2026-06-27 05:06:03.662774',NULL,NULL,NULL,'UNREAD','BADGE_UNLOCKED',25),(28,'解锁新徽章「XP 学徒」：累计 XP 达到 100 后获得。','2026-06-27 05:06:03.668982',NULL,NULL,NULL,'UNREAD','BADGE_UNLOCKED',25),(29,'你的任务《任务搜索增加按 XP 排序 / Sort quests by reward XP》已通过审核，获得 160 XP，等级与贡献记录已更新。','2026-06-27 05:06:03.680991',NULL,6,'SUBMISSION','UNREAD','REVIEW_APPROVED',25),(30,'你的委托《任务详情接口缓存 / Cache quest detail with Redis》已通过管理员审核并上架到悬赏板。','2026-06-27 05:06:03.828206',NULL,11,'QUEST','UNREAD','QUEST_APPROVED',14),(31,'冒险家 archer_deng 接取了你的委托《任务详情接口缓存 / Cache quest detail with Redis》。','2026-06-27 05:06:03.855220',NULL,11,'QUEST','UNREAD','QUEST_ACCEPTED',14),(32,'冒险家 archer_deng 提交了任务《任务详情接口缓存 / Cache quest detail with Redis》的成果，等待你的审核。','2026-06-27 05:06:04.757113',NULL,7,'SUBMISSION','UNREAD','SUBMISSION_RECEIVED',14),(33,'恭喜！你升到了 Lv.3，继续加油！','2026-06-27 05:06:04.799104',NULL,NULL,NULL,'UNREAD','LEVEL_UP',26),(34,'解锁新徽章「首次贡献」：完成第一个任务后获得。','2026-06-27 05:06:04.805327',NULL,NULL,NULL,'UNREAD','BADGE_UNLOCKED',26),(35,'解锁新徽章「XP 学徒」：累计 XP 达到 100 后获得。','2026-06-27 05:06:04.811792',NULL,NULL,NULL,'UNREAD','BADGE_UNLOCKED',26),(36,'解锁新徽章「等级新星」：用户等级达到 3 后获得。','2026-06-27 05:06:04.818526',NULL,NULL,NULL,'UNREAD','BADGE_UNLOCKED',26),(37,'你的任务《任务详情接口缓存 / Cache quest detail with Redis》已通过审核，获得 260 XP，等级与贡献记录已更新。','2026-06-27 05:06:04.832418',NULL,7,'SUBMISSION','UNREAD','REVIEW_APPROVED',26),(38,'你的任务《任务详情接口缓存 / Cache quest detail with Redis》对应的 PR 已被合并到目标分支。','2026-06-27 05:06:05.275611',NULL,7,'SUBMISSION','UNREAD','PR_MERGED',26),(39,'你的委托《禁止接取自己发布的任务 / Block self-assignment》已通过管理员审核并上架到悬赏板。','2026-06-27 05:06:05.399412',NULL,12,'QUEST','UNREAD','QUEST_APPROVED',14),(40,'冒险家 monk_pan 接取了你的委托《禁止接取自己发布的任务 / Block self-assignment》。','2026-06-27 05:06:05.435552',NULL,12,'QUEST','UNREAD','QUEST_ACCEPTED',14),(41,'冒险家 monk_pan 提交了任务《禁止接取自己发布的任务 / Block self-assignment》的成果，等待你的审核。','2026-06-27 05:06:06.528682',NULL,8,'SUBMISSION','UNREAD','SUBMISSION_RECEIVED',14),(42,'解锁新徽章「首次贡献」：完成第一个任务后获得。','2026-06-27 05:06:06.589532',NULL,NULL,NULL,'UNREAD','BADGE_UNLOCKED',27),(43,'你的任务《禁止接取自己发布的任务 / Block self-assignment》已通过审核，获得 90 XP，等级与贡献记录已更新。','2026-06-27 05:06:06.606543',NULL,8,'SUBMISSION','UNREAD','REVIEW_APPROVED',27),(44,'你的任务《禁止接取自己发布的任务 / Block self-assignment》对应的 PR 已被合并到目标分支。','2026-06-27 05:06:07.034088',NULL,8,'SUBMISSION','UNREAD','PR_MERGED',27),(45,'你的委托《提交闭环集成测试 / Integration test for submission flow》已通过管理员审核并上架到悬赏板。','2026-06-27 05:06:07.151546',NULL,13,'QUEST','UNREAD','QUEST_APPROVED',14),(46,'冒险家 bard_xie 接取了你的委托《提交闭环集成测试 / Integration test for submission flow》。','2026-06-27 05:06:07.186788',NULL,13,'QUEST','UNREAD','QUEST_ACCEPTED',14),(47,'冒险家 bard_xie 提交了任务《提交闭环集成测试 / Integration test for submission flow》的成果，等待你的审核。','2026-06-27 05:06:08.189181',NULL,9,'SUBMISSION','UNREAD','SUBMISSION_RECEIVED',14),(48,'恭喜！你升到了 Lv.2，继续加油！','2026-06-27 05:06:08.245984',NULL,NULL,NULL,'UNREAD','LEVEL_UP',28),(49,'解锁新徽章「首次贡献」：完成第一个任务后获得。','2026-06-27 05:06:08.254376',NULL,NULL,NULL,'UNREAD','BADGE_UNLOCKED',28),(50,'解锁新徽章「XP 学徒」：累计 XP 达到 100 后获得。','2026-06-27 05:06:08.261383',NULL,NULL,NULL,'UNREAD','BADGE_UNLOCKED',28),(51,'你的任务《提交闭环集成测试 / Integration test for submission flow》已通过审核，获得 130 XP，等级与贡献记录已更新。','2026-06-27 05:06:08.275500',NULL,9,'SUBMISSION','UNREAD','REVIEW_APPROVED',28),(52,'你的委托《CLI: quest list 命令 / Add `quest list` command》已通过管理员审核并上架到悬赏板。','2026-06-27 05:06:08.434808',NULL,14,'QUEST','UNREAD','QUEST_APPROVED',15),(53,'冒险家 knight_cao 接取了你的委托《CLI: quest list 命令 / Add `quest list` command》。','2026-06-27 05:06:08.464945',NULL,14,'QUEST','UNREAD','QUEST_ACCEPTED',15),(54,'冒险家 knight_cao 提交了任务《CLI: quest list 命令 / Add `quest list` command》的成果，等待你的审核。','2026-06-27 05:06:09.468931',NULL,10,'SUBMISSION','UNREAD','SUBMISSION_RECEIVED',15),(55,'恭喜！你升到了 Lv.2，继续加油！','2026-06-27 05:06:09.517551',NULL,NULL,NULL,'UNREAD','LEVEL_UP',29),(56,'解锁新徽章「首次贡献」：完成第一个任务后获得。','2026-06-27 05:06:09.525699',NULL,NULL,NULL,'UNREAD','BADGE_UNLOCKED',29),(57,'解锁新徽章「XP 学徒」：累计 XP 达到 100 后获得。','2026-06-27 05:06:09.533044',NULL,NULL,NULL,'UNREAD','BADGE_UNLOCKED',29),(58,'你的任务《CLI: quest list 命令 / Add `quest list` command》已通过审核，获得 100 XP，等级与贡献记录已更新。','2026-06-27 05:06:09.546907',NULL,10,'SUBMISSION','UNREAD','REVIEW_APPROVED',29),(59,'你的委托《CLI 安装文档 / CLI install & auth docs》已通过管理员审核并上架到悬赏板。','2026-06-27 05:06:09.697889',NULL,15,'QUEST','UNREAD','QUEST_APPROVED',15),(60,'冒险家 druid_lu 接取了你的委托《CLI 安装文档 / CLI install & auth docs》。','2026-06-27 05:06:09.724254',NULL,15,'QUEST','UNREAD','QUEST_ACCEPTED',15),(61,'你的委托《冒险者快速上手 / Adventurer quick start》已通过管理员审核并上架到悬赏板。','2026-06-27 05:06:09.813290',NULL,16,'QUEST','UNREAD','QUEST_APPROVED',16),(62,'冒险家 thief_meng 接取了你的委托《冒险者快速上手 / Adventurer quick start》。','2026-06-27 05:06:09.839794',NULL,16,'QUEST','UNREAD','QUEST_ACCEPTED',16),(63,'冒险家 thief_meng 提交了任务《冒险者快速上手 / Adventurer quick start》的成果，等待你的审核。','2026-06-27 05:06:10.830111',NULL,11,'SUBMISSION','UNREAD','SUBMISSION_RECEIVED',16),(64,'解锁新徽章「首次贡献」：完成第一个任务后获得。','2026-06-27 05:06:10.870314',NULL,NULL,NULL,'UNREAD','BADGE_UNLOCKED',31),(65,'你的任务《冒险者快速上手 / Adventurer quick start》已通过审核，获得 45 XP，等级与贡献记录已更新。','2026-06-27 05:06:10.882850',NULL,11,'SUBMISSION','UNREAD','REVIEW_APPROVED',31),(66,'你的任务《冒险者快速上手 / Adventurer quick start》对应的 PR 已被合并到目标分支。','2026-06-27 05:06:11.338907',NULL,11,'SUBMISSION','UNREAD','PR_MERGED',31),(67,'你的委托《REST API 参考文档 / REST API reference》已通过管理员审核并上架到悬赏板。','2026-06-27 05:06:11.432529',NULL,17,'QUEST','UNREAD','QUEST_APPROVED',16),(68,'冒险家 paladin_yao 接取了你的委托《REST API 参考文档 / REST API reference》。','2026-06-27 05:06:11.457086',NULL,17,'QUEST','UNREAD','QUEST_ACCEPTED',16),(69,'你的委托《打回通知 / Notify on changes requested》已通过管理员审核并上架到悬赏板。','2026-06-27 05:06:11.553366',NULL,18,'QUEST','UNREAD','QUEST_APPROVED',17),(70,'冒险家 scout_jiang 接取了你的委托《打回通知 / Notify on changes requested》。','2026-06-27 05:06:11.576055',NULL,18,'QUEST','UNREAD','QUEST_ACCEPTED',17),(71,'冒险家 scout_jiang 提交了任务《打回通知 / Notify on changes requested》的成果，等待你的审核。','2026-06-27 05:06:12.536687',NULL,12,'SUBMISSION','UNREAD','SUBMISSION_RECEIVED',17),(72,'恭喜！你升到了 Lv.2，继续加油！','2026-06-27 05:06:12.583567',NULL,NULL,NULL,'UNREAD','LEVEL_UP',33),(73,'解锁新徽章「首次贡献」：完成第一个任务后获得。','2026-06-27 05:06:12.589625',NULL,NULL,NULL,'UNREAD','BADGE_UNLOCKED',33),(74,'解锁新徽章「XP 学徒」：累计 XP 达到 100 后获得。','2026-06-27 05:06:12.595122',NULL,NULL,NULL,'UNREAD','BADGE_UNLOCKED',33),(75,'你的任务《打回通知 / Notify on changes requested》已通过审核，获得 150 XP，等级与贡献记录已更新。','2026-06-27 05:06:12.607795',NULL,12,'SUBMISSION','UNREAD','REVIEW_APPROVED',33),(76,'你的委托《修复通知已读不生效 / Fix mark-as-read flakiness》已通过管理员审核并上架到悬赏板。','2026-06-27 05:06:12.748233',NULL,19,'QUEST','UNREAD','QUEST_APPROVED',17),(77,'冒险家 hunter_fang 接取了你的委托《修复通知已读不生效 / Fix mark-as-read flakiness》。','2026-06-27 05:06:12.768722',NULL,19,'QUEST','UNREAD','QUEST_ACCEPTED',17),(78,'你的委托《月度排行榜 / Monthly leaderboard》已通过管理员审核并上架到悬赏板。','2026-06-27 05:06:13.192941',NULL,20,'QUEST','UNREAD','QUEST_APPROVED',18),(79,'冒险家 sorcerer_yu 接取了你的委托《月度排行榜 / Monthly leaderboard》。','2026-06-27 05:06:13.222417',NULL,20,'QUEST','UNREAD','QUEST_ACCEPTED',18),(80,'冒险家 sorcerer_yu 提交了任务《月度排行榜 / Monthly leaderboard》的成果，等待你的审核。','2026-06-27 05:06:14.219240',NULL,13,'SUBMISSION','UNREAD','SUBMISSION_RECEIVED',18),(81,'恭喜！你升到了 Lv.3，继续加油！','2026-06-27 05:06:14.260612',NULL,NULL,NULL,'UNREAD','LEVEL_UP',35),(82,'解锁新徽章「首次贡献」：完成第一个任务后获得。','2026-06-27 05:06:14.266906',NULL,NULL,NULL,'UNREAD','BADGE_UNLOCKED',35),(83,'解锁新徽章「XP 学徒」：累计 XP 达到 100 后获得。','2026-06-27 05:06:14.273618',NULL,NULL,NULL,'UNREAD','BADGE_UNLOCKED',35),(84,'解锁新徽章「等级新星」：用户等级达到 3 后获得。','2026-06-27 05:06:14.279861',NULL,NULL,NULL,'UNREAD','BADGE_UNLOCKED',35),(85,'你的任务《月度排行榜 / Monthly leaderboard》已通过审核，获得 240 XP，等级与贡献记录已更新。','2026-06-27 05:06:14.290925',NULL,13,'SUBMISSION','UNREAD','REVIEW_APPROVED',35),(86,'你的委托《成长档案 N+1 优化 / Fix N+1 in growth profile》已通过管理员审核并上架到悬赏板。','2026-06-27 05:06:14.436898',NULL,21,'QUEST','UNREAD','QUEST_APPROVED',18),(87,'冒险家 warrior_ma 接取了你的委托《成长档案 N+1 优化 / Fix N+1 in growth profile》。','2026-06-27 05:06:14.460255',NULL,21,'QUEST','UNREAD','QUEST_ACCEPTED',18),(88,'你的委托《后端多阶段 Docker 构建 / Multi-stage Docker build》已通过管理员审核并上架到悬赏板。','2026-06-27 05:06:14.551206',NULL,22,'QUEST','UNREAD','QUEST_APPROVED',14),(89,'你的委托《任务卡片技术栈标签 / Tech-stack chips on quest card》已通过管理员审核并上架到悬赏板。','2026-06-27 05:06:14.646175',NULL,23,'QUEST','UNREAD','QUEST_APPROVED',13),(90,'你的委托《任务卡片骨架屏加载 / Skeleton loading for quest cards》已通过管理员审核并上架到悬赏板。','2026-06-27 05:35:49.329263',NULL,28,'QUEST','UNREAD','QUEST_APPROVED',13),(91,'你的委托《任务列表接口加游标分页 / Cursor pagination》已通过管理员审核并上架到悬赏板。','2026-06-27 05:35:49.448506',NULL,29,'QUEST','UNREAD','QUEST_APPROVED',14),(92,'你的委托《CLI 增加 quest accept 命令 / `quest accept`》已通过管理员审核并上架到悬赏板。','2026-06-27 05:35:49.568672',NULL,30,'QUEST','UNREAD','QUEST_APPROVED',15),(93,'你的委托《撰写维护者发布指南 / Maintainer publishing guide》已通过管理员审核并上架到悬赏板。','2026-06-27 05:35:49.682451',NULL,31,'QUEST','UNREAD','QUEST_APPROVED',16),(94,'你的委托《任务被接取时通知发布者 / Notify on accept》已通过管理员审核并上架到悬赏板。','2026-06-27 05:35:49.797629',NULL,32,'QUEST','UNREAD','QUEST_APPROVED',17),(95,'你的委托《排行榜分页与我的排名 / Pagination & my rank》已通过管理员审核并上架到悬赏板。','2026-06-27 05:35:49.928264',NULL,33,'QUEST','UNREAD','QUEST_APPROVED',18),(96,'你的委托《悬赏板支持关键字搜索 / Keyword search on board》已通过管理员审核并上架到悬赏板。','2026-06-27 05:35:50.060479',NULL,34,'QUEST','UNREAD','QUEST_APPROVED',13),(97,'你的委托《接取并发幂等保护 / Idempotent accept under race》已通过管理员审核并上架到悬赏板。','2026-06-27 05:35:50.186017',NULL,35,'QUEST','UNREAD','QUEST_APPROVED',14),(98,'你的委托《CLI 输出支持 JSON 格式 / JSON output》已通过管理员审核并上架到悬赏板。','2026-06-27 05:35:50.296167',NULL,36,'QUEST','UNREAD','QUEST_APPROVED',15),(99,'你的委托《整理常见问题 FAQ / FAQ page》已通过管理员审核并上架到悬赏板。','2026-06-27 05:35:50.409527',NULL,37,'QUEST','UNREAD','QUEST_APPROVED',16),(100,'你的委托《通知支持分页拉取 / Paginated notifications》已通过管理员审核并上架到悬赏板。','2026-06-27 05:35:50.521753',NULL,38,'QUEST','UNREAD','QUEST_APPROVED',17),(101,'你的委托《成长等级曲线可配置 / Configurable level curve》已通过管理员审核并上架到悬赏板。','2026-06-27 05:35:50.650891',NULL,39,'QUEST','UNREAD','QUEST_APPROVED',18),(102,'你的委托《任务详情页响应式适配 / Responsive quest detail》已通过管理员审核并上架到悬赏板。','2026-06-27 05:35:50.778113',NULL,40,'QUEST','UNREAD','QUEST_APPROVED',13),(103,'你的委托《发布任务参数校验增强 / Stronger create validation》已通过管理员审核并上架到悬赏板。','2026-06-27 05:35:50.894085',NULL,41,'QUEST','UNREAD','QUEST_APPROVED',14),(104,'你的委托《CLI 配置文件支持 / Config file support》已通过管理员审核并上架到悬赏板。','2026-06-27 05:35:51.011063',NULL,42,'QUEST','UNREAD','QUEST_APPROVED',15),(105,'你的委托《补充架构概览图 / Architecture overview》已通过管理员审核并上架到悬赏板。','2026-06-27 05:35:51.114408',NULL,43,'QUEST','UNREAD','QUEST_APPROVED',16),(106,'你的委托《邮件通知开关 / Email notification opt-in》已通过管理员审核并上架到悬赏板。','2026-06-27 05:35:51.217735',NULL,44,'QUEST','UNREAD','QUEST_APPROVED',17),(107,'你的委托《贡献热力统计 / Contribution heatmap data》已通过管理员审核并上架到悬赏板。','2026-06-27 05:35:51.345658',NULL,45,'QUEST','UNREAD','QUEST_APPROVED',18),(108,'你的委托《暗色主题切换 / Dark mode toggle》已通过管理员审核并上架到悬赏板。','2026-06-27 05:35:51.477566',NULL,46,'QUEST','UNREAD','QUEST_APPROVED',13),(109,'你的委托《任务搜索支持多标签过滤 / Multi-tag filter》已通过管理员审核并上架到悬赏板。','2026-06-27 05:35:51.607776','2026-06-27 05:36:15.032179',47,'QUEST','READ','QUEST_APPROVED',14),(110,'你的委托《排行榜缓存与定时刷新 / Cache & refresh》已通过管理员审核并上架到悬赏板。','2026-06-27 05:35:51.729311',NULL,48,'QUEST','UNREAD','QUEST_APPROVED',18),(111,'你的委托《接取按钮加二次确认 / Confirm before accept》已通过管理员审核并上架到悬赏板。','2026-06-27 05:35:51.870784',NULL,49,'QUEST','UNREAD','QUEST_APPROVED',13),(112,'你的委托《操作审计日志 / Audit log for quest actions》已通过管理员审核并上架到悬赏板。','2026-06-27 05:35:51.992475',NULL,50,'QUEST','UNREAD','QUEST_APPROVED',14);
/*!40000 ALTER TABLE `notifications` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `platform_exception_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `platform_exception_logs` (
  `exception_id` bigint NOT NULL,
  `line` varchar(1000) NOT NULL,
  `seq` int NOT NULL,
  PRIMARY KEY (`exception_id`,`seq`),
  CONSTRAINT `FK8jrm9699alsemrp441laymigm` FOREIGN KEY (`exception_id`) REFERENCES `platform_exceptions` (`exception_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `platform_exception_logs` WRITE;
/*!40000 ALTER TABLE `platform_exception_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `platform_exception_logs` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `platform_exceptions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `platform_exceptions` (
  `exception_id` bigint NOT NULL AUTO_INCREMENT,
  `category` enum('PERMISSION','RELATION','SYNC') NOT NULL,
  `detected_at` datetime(6) NOT NULL,
  `impact` varchar(1000) DEFAULT NULL,
  `reason` varchar(1000) NOT NULL,
  `related_quest` varchar(64) DEFAULT NULL,
  `repository_id` bigint DEFAULT NULL,
  `repository_name` varchar(255) DEFAULT NULL,
  `resolution_action` varchar(64) DEFAULT NULL,
  `resolution_comment` varchar(1000) DEFAULT NULL,
  `resolved_at` datetime(6) DEFAULT NULL,
  `resolved_by` bigint DEFAULT NULL,
  `retryable` bit(1) NOT NULL,
  `status` enum('IN_REVIEW','RESOLVED','UNRESOLVED') NOT NULL,
  `suggestion` varchar(1000) DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `type` varchar(64) NOT NULL,
  PRIMARY KEY (`exception_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `platform_exceptions` WRITE;
/*!40000 ALTER TABLE `platform_exceptions` DISABLE KEYS */;
/*!40000 ALTER TABLE `platform_exceptions` ENABLE KEYS */;
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
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `pull_requests` WRITE;
/*!40000 ALTER TABLE `pull_requests` DISABLE KEYS */;
INSERT INTO `pull_requests` VALUES (1,'2026-06-07 06:03:25.849705','5','http://localhost:3000/spike-admin/hello-world-mvp/pulls/5',NULL,'task/quest-3-assignment-1-advent','OPEN','main','[Quest #3] 任务二','2026-06-07 06:03:25.849705',1),(2,'2026-06-07 06:50:41.032741','7','http://localhost:3000/spike-admin/hello-world-mvp/pulls/7',NULL,'task/quest-4-assignment-2-advent','OPEN','main','[Quest #4] Hello World','2026-06-07 06:50:41.032741',1),(3,'2026-06-27 05:05:59.281282','2','http://localhost:3000/spike-admin/gitguild-web/pulls/2','2026-06-27 05:05:59.941422','task/quest-7-assignment-4-novice_chen','MERGED','main','[Quest #7] 任务板：增加难度筛选 / Quest board difficulty filter','2026-06-27 05:05:59.954389',6),(4,'2026-06-27 05:06:01.192784','4','http://localhost:3000/spike-admin/gitguild-web/pulls/4','2026-06-27 05:07:59.871062','task/quest-8-assignment-5-squire_liu','MERGED','main','[Quest #8] 工作台进度可视化 / Workbench progress bar','2026-06-27 05:07:59.872422',6),(5,'2026-06-27 05:06:02.423990','6','http://localhost:3000/spike-admin/gitguild-web/pulls/6','2026-06-27 05:08:02.293394','task/quest-9-assignment-6-rogue_tang','MERGED','main','[Quest #9] 修复登录页回车不提交 / Fix Enter key on login','2026-06-27 05:08:02.294387',6),(6,'2026-06-27 05:06:03.606050','2','http://localhost:3000/spike-admin/gitguild-api/pulls/2','2026-06-27 05:08:02.758811','task/quest-10-assignment-7-mage_apprentice_xu','MERGED','main','[Quest #10] 任务搜索增加按 XP 排序 / Sort quests by reward XP','2026-06-27 05:08:02.760444',7),(7,'2026-06-27 05:06:04.752764','4','http://localhost:3000/spike-admin/gitguild-api/pulls/4','2026-06-27 05:06:05.274240','task/quest-11-assignment-8-archer_deng','MERGED','main','[Quest #11] 任务详情接口缓存 / Cache quest detail with Redis','2026-06-27 05:06:05.283458',7),(8,'2026-06-27 05:06:06.520535','6','http://localhost:3000/spike-admin/gitguild-api/pulls/6','2026-06-27 05:06:07.032151','task/quest-12-assignment-9-monk_pan','MERGED','main','[Quest #12] 禁止接取自己发布的任务 / Block self-assignment','2026-06-27 05:06:07.042178',7),(9,'2026-06-27 05:06:08.181768','8','http://localhost:3000/spike-admin/gitguild-api/pulls/8','2026-06-27 05:08:05.224519','task/quest-13-assignment-10-bard_xie','MERGED','main','[Quest #13] 提交闭环集成测试 / Integration test for submission flow','2026-06-27 05:08:05.225750',7),(10,'2026-06-27 05:06:09.463178','2','http://localhost:3000/spike-admin/quest-cli/pulls/2','2026-06-27 05:08:05.758889','task/quest-14-assignment-11-knight_cao','MERGED','main','[Quest #14] CLI: quest list 命令 / Add `quest list` command','2026-06-27 05:08:05.760400',8),(11,'2026-06-27 05:06:10.824640','2','http://localhost:3000/spike-admin/guild-docs/pulls/2','2026-06-27 05:06:11.337618','task/quest-16-assignment-13-thief_meng','MERGED','main','[Quest #16] 冒险者快速上手 / Adventurer quick start','2026-06-27 05:06:11.345534',9),(12,'2026-06-27 05:06:12.532410','2','http://localhost:3000/spike-admin/notify-service/pulls/2','2026-06-27 05:08:06.319888','task/quest-18-assignment-15-scout_jiang','MERGED','main','[Quest #18] 打回通知 / Notify on changes requested','2026-06-27 05:08:06.321365',10),(13,'2026-06-27 05:06:14.214651','2','http://localhost:3000/spike-admin/leaderboard-svc/pulls/2','2026-06-27 05:08:06.851085','task/quest-20-assignment-17-sorcerer_yu','MERGED','main','[Quest #20] 月度排行榜 / Monthly leaderboard','2026-06-27 05:08:06.852424',11);
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
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `quest_assignments` WRITE;
/*!40000 ALTER TABLE `quest_assignments` DISABLE KEYS */;
INSERT INTO `quest_assignments` VALUES (1,'2026-06-07 05:55:09.773379','2026-06-07 06:03:45.358419','2026-06-07 05:55:09.773379','COMPLETED','2026-06-07 06:03:45.361263',6,3,'task/quest-3-assignment-1-advent'),(2,'2026-06-07 06:50:30.995823','2026-06-07 06:50:58.329010','2026-06-07 06:50:30.995823','COMPLETED','2026-06-07 06:50:58.329833',6,4,'task/quest-4-assignment-2-advent'),(3,'2026-06-18 07:23:11.213659',NULL,'2026-06-18 07:23:11.213659','ACTIVE','2026-06-18 07:23:13.492957',5,5,'task/quest-5-assignment-3-guild'),(4,'2026-06-27 05:05:58.177684','2026-06-27 05:05:59.371171','2026-06-27 05:05:58.177684','COMPLETED','2026-06-27 05:05:59.374015',22,7,'task/quest-7-assignment-4-novice_chen'),(5,'2026-06-27 05:06:00.148203','2026-06-27 05:06:01.250068','2026-06-27 05:06:00.148203','COMPLETED','2026-06-27 05:06:01.251998',23,8,'task/quest-8-assignment-5-squire_liu'),(6,'2026-06-27 05:06:01.490540','2026-06-27 05:06:02.458808','2026-06-27 05:06:01.490540','COMPLETED','2026-06-27 05:06:02.461619',24,9,'task/quest-9-assignment-6-rogue_tang'),(7,'2026-06-27 05:06:02.690230','2026-06-27 05:06:03.641540','2026-06-27 05:06:02.690230','COMPLETED','2026-06-27 05:06:03.642733',25,10,'task/quest-10-assignment-7-mage_apprentice_xu'),(8,'2026-06-27 05:06:03.851923','2026-06-27 05:06:04.784452','2026-06-27 05:06:03.851923','COMPLETED','2026-06-27 05:06:04.785750',26,11,'task/quest-11-assignment-8-archer_deng'),(9,'2026-06-27 05:06:05.432029','2026-06-27 05:06:06.567901','2026-06-27 05:06:05.432029','COMPLETED','2026-06-27 05:06:06.569372',27,12,'task/quest-12-assignment-9-monk_pan'),(10,'2026-06-27 05:06:07.183757','2026-06-27 05:06:08.227393','2026-06-27 05:06:07.183757','COMPLETED','2026-06-27 05:06:08.228639',28,13,'task/quest-13-assignment-10-bard_xie'),(11,'2026-06-27 05:06:08.462567','2026-06-27 05:06:09.502223','2026-06-27 05:06:08.462567','COMPLETED','2026-06-27 05:06:09.504114',29,14,'task/quest-14-assignment-11-knight_cao'),(12,'2026-06-27 05:06:09.721895',NULL,'2026-06-27 05:06:09.721895','ACTIVE','2026-06-27 05:06:09.721895',30,15,NULL),(13,'2026-06-27 05:06:09.837583','2026-06-27 05:06:10.855536','2026-06-27 05:06:09.837583','COMPLETED','2026-06-27 05:06:10.856243',31,16,'task/quest-16-assignment-13-thief_meng'),(14,'2026-06-27 05:06:11.454863',NULL,'2026-06-27 05:06:11.454863','ACTIVE','2026-06-27 05:06:11.454863',32,17,NULL),(15,'2026-06-27 05:06:11.573626','2026-06-27 05:06:12.570320','2026-06-27 05:06:11.573626','COMPLETED','2026-06-27 05:06:12.571366',33,18,'task/quest-18-assignment-15-scout_jiang'),(16,'2026-06-27 05:06:12.766795',NULL,'2026-06-27 05:06:12.766795','ABANDONED','2026-06-27 05:06:12.766795',34,19,NULL),(17,'2026-06-27 05:06:13.218894','2026-06-27 05:06:14.248593','2026-06-27 05:06:13.218894','COMPLETED','2026-06-27 05:06:14.249960',35,20,'task/quest-20-assignment-17-sorcerer_yu'),(18,'2026-06-27 05:06:14.458258',NULL,'2026-06-27 05:06:14.458258','ACTIVE','2026-06-27 05:06:14.458258',36,21,NULL);
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
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `quest_categories` WRITE;
/*!40000 ALTER TABLE `quest_categories` DISABLE KEYS */;
INSERT INTO `quest_categories` VALUES (1,'2026-06-04 06:11:55.000000','本地 Gitea 最短 MVP 演示分类',_binary '','MVP','2026-06-04 06:11:55.000000'),(2,'2026-06-07 03:36:40.894316','',_binary '','java','2026-06-07 03:36:40.894316'),(3,'2026-06-27 05:04:06.235278','Web 前端页面、组件与交互',_binary '','前端开发','2026-06-27 05:04:06.235278'),(4,'2026-06-27 05:04:06.286662','服务端 API、业务逻辑与数据层',_binary '','后端开发','2026-06-27 05:04:06.286662'),(5,'2026-06-27 05:04:06.307807','README、API 文档与使用手册',_binary '','文档工程','2026-06-27 05:04:06.307807'),(6,'2026-06-27 05:04:06.328119','单元测试、集成测试与质量保障',_binary '','测试质量','2026-06-27 05:04:06.328119'),(7,'2026-06-27 05:04:06.348504','CI/CD、容器化与部署运维',_binary '','DevOps','2026-06-27 05:04:06.348504'),(8,'2026-06-27 05:04:06.367432','数据库建模、查询优化与缓存',_binary '','数据与存储','2026-06-27 05:04:06.367432');
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
INSERT INTO `quest_tag_relations` VALUES (5,3),(6,3),(7,4),(9,4),(15,4),(16,4),(36,4),(49,4),(7,5),(8,5),(10,5),(11,5),(14,5),(18,5),(20,5),(22,5),(24,5),(25,5),(30,5),(32,5),(33,5),(34,5),(44,5),(45,5),(46,5),(47,5),(50,5),(9,6),(12,6),(19,6),(35,6),(21,7),(27,7),(13,8),(15,8),(16,8),(17,8),(26,8),(31,8),(37,8),(43,8),(10,9),(13,9),(17,9),(20,9),(23,9),(28,9),(29,9),(38,9),(39,9),(40,9),(41,9),(42,9),(48,9),(11,10),(19,10);
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
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `quest_tags` WRITE;
/*!40000 ALTER TABLE `quest_tags` DISABLE KEYS */;
INSERT INTO `quest_tags` VALUES (3,'#43613a','2026-06-07 03:36:33.511813',_binary '','新手友好','2026-06-07 03:36:33.511813'),(4,'#7057ff','2026-06-27 05:04:06.418577',_binary '','good-first-issue','2026-06-27 05:04:06.418577'),(5,'#0e8a16','2026-06-27 05:04:06.437971',_binary '','feature','2026-06-27 05:04:06.437971'),(6,'#d73a4a','2026-06-27 05:04:06.456438',_binary '','bugfix','2026-06-27 05:04:06.456438'),(7,'#fbca04','2026-06-27 05:04:06.475798',_binary '','refactor','2026-06-27 05:04:06.475798'),(8,'#0075ca','2026-06-27 05:04:06.495705',_binary '','docs','2026-06-27 05:04:06.495705'),(9,'#a2eeef','2026-06-27 05:04:06.516884',_binary '','enhancement','2026-06-27 05:04:06.516884'),(10,'#b60205','2026-06-27 05:04:06.536335',_binary '','urgent','2026-06-27 05:04:06.536335');
/*!40000 ALTER TABLE `quest_tags` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `quest_tech_stacks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quest_tech_stacks` (
  `tech_stack_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `enabled` bit(1) NOT NULL,
  `name` varchar(64) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`tech_stack_id`),
  UNIQUE KEY `UKfsv1ucplqde5w3s8cuc0hk9o` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `quest_tech_stacks` WRITE;
/*!40000 ALTER TABLE `quest_tech_stacks` DISABLE KEYS */;
INSERT INTO `quest_tech_stacks` VALUES (1,'2026-06-20 11:04:05.821643',_binary '','Markdown','2026-06-20 11:04:05.821643'),(2,'2026-06-27 05:05:45.486306',_binary '','Java','2026-06-27 05:05:45.486306'),(3,'2026-06-27 05:05:45.504898',_binary '','Spring Boot','2026-06-27 05:05:45.504898'),(4,'2026-06-27 05:05:45.523699',_binary '','Spring Security','2026-06-27 05:05:45.523699'),(5,'2026-06-27 05:05:45.543983',_binary '','JPA','2026-06-27 05:05:45.543983'),(6,'2026-06-27 05:05:45.563283',_binary '','Vue','2026-06-27 05:05:45.563283'),(7,'2026-06-27 05:05:45.582317',_binary '','TypeScript','2026-06-27 05:05:45.582317'),(8,'2026-06-27 05:05:45.602728',_binary '','JavaScript','2026-06-27 05:05:45.602728'),(9,'2026-06-27 05:05:45.623245',_binary '','Vite','2026-06-27 05:05:45.623245'),(10,'2026-06-27 05:05:45.641528',_binary '','MySQL','2026-06-27 05:05:45.641528'),(11,'2026-06-27 05:05:45.662046',_binary '','Redis','2026-06-27 05:05:45.662046'),(12,'2026-06-27 05:05:45.678772',_binary '','Docker','2026-06-27 05:05:45.678772'),(13,'2026-06-27 05:05:45.697109',_binary '','Git','2026-06-27 05:05:45.697109'),(14,'2026-06-27 05:05:45.732127',_binary '','Python','2026-06-27 05:05:45.732127'),(15,'2026-06-27 05:05:45.749329',_binary '','JUnit','2026-06-27 05:05:45.749329'),(16,'2026-06-27 05:05:45.767776',_binary '','Maven','2026-06-27 05:05:45.767776'),(17,'2026-06-27 05:05:45.786019',_binary '','REST API','2026-06-27 05:05:45.786019'),(18,'2026-06-27 05:05:45.805525',_binary '','Gitea','2026-06-27 05:05:45.805525');
/*!40000 ALTER TABLE `quest_tech_stacks` ENABLE KEYS */;
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
) ENGINE=InnoDB AUTO_INCREMENT=51 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `quests` WRITE;
/*!40000 ALTER TABLE `quests` DISABLE KEYS */;
INSERT INTO `quests` VALUES (3,'2','2026-06-07 05:52:25.936334','2','A',1,'2026-06-07 05:54:09.613787',50,'COMPLETED','[\"Markdown\"]','任务二','2026-06-07 06:03:45.361210',1,4,5,1),(4,'3','2026-06-07 06:48:49.676646','3','A',1,'2026-06-07 06:49:52.471103',50,'COMPLETED','[\"Markdown\"]','Hello World','2026-06-07 06:50:58.329802',1,5,5,1),(5,'3','2026-06-14 01:26:58.838202','3','A',1,'2026-06-14 01:58:49.824706',50,'IN_PROGRESS','[\"Markdown\"]','Hello World','2026-06-18 07:23:11.218631',1,6,5,1),(6,'新增 hello.md（或 hello world 文件），内容包含 \"Hello, Git-Guild!\"，并通过 PR 合并。','2026-06-18 07:14:23.820057','此委托用于演示','A',1,NULL,50,'PENDING_ADMIN_REVIEW','[\"Markdown\"]','demo','2026-06-18 07:14:23.998118',1,7,5,3),(7,'下拉框可选 A/B/C/D 与“全部”；切换后列表实时刷新；与分类筛选可叠加；新增组件单测。','2026-06-27 05:05:58.045871','悬赏任务板目前只能按分类筛选。需要新增一个难度（A/B/C/D）下拉筛选器，并与现有筛选条件组合生效。\n\nThe quest board only filters by category. Add a difficulty (A/B/C/D) dropdown filter that composes with the existing filters.','C',6,'2026-06-27 05:05:58.128739',80,'COMPLETED','[\"Vue\", \"TypeScript\", \"Vite\"]','任务板：增加难度筛选 / Quest board difficulty filter','2026-06-27 05:05:59.373732',3,8,13,6),(8,'进度条按任务真实状态高亮当前阶段；移动端自适应；含快照测试。','2026-06-27 05:06:00.042466','在冒险者工作台为每个进行中的任务加上状态进度条（接取→分支→提交→审核→合并）。\n\nAdd a status progress bar to each active quest on the adventurer workbench.','B',10,'2026-06-27 05:06:00.103296',140,'COMPLETED','[\"Vue\", \"TypeScript\"]','工作台进度可视化 / Workbench progress bar','2026-06-27 05:06:01.251660',3,9,13,6),(9,'用户名/密码框回车均可提交；不破坏已有按钮点击；补充交互测试。','2026-06-27 05:06:01.415886','登录表单在输入框按下回车不会触发提交，必须点击按钮。需修复键盘可达性。\n\nPressing Enter in the login form does not submit; fix keyboard accessibility.','D',3,'2026-06-27 05:06:01.459651',40,'COMPLETED','[\"Vue\", \"JavaScript\"]','修复登录页回车不提交 / Fix Enter key on login','2026-06-27 05:06:02.461551',3,10,13,6),(10,'sortBy 接受 rewardXp 且方向可控；分页正确；新增 service 与 MVC 测试。','2026-06-27 05:06:02.620432','GET /api/v1/quests 目前仅支持按创建时间排序。需要支持 sortBy=rewardXp 的升降序。\n\nAdd sortBy=rewardXp (asc/desc) to the quest search endpoint.','B',12,'2026-06-27 05:06:02.660741',160,'COMPLETED','[\"Java\", \"Spring Boot\", \"JPA\"]','任务搜索增加按 XP 排序 / Sort quests by reward XP','2026-06-27 05:06:03.642691',4,11,14,7),(11,'命中缓存不走库；审核/接取/合并后缓存失效；含缓存失效集成测试与压测说明。','2026-06-27 05:06:03.787643','热门任务详情被频繁读取。引入 Redis 缓存任务详情，并在任务状态变更时正确失效。\n\nCache quest detail in Redis and invalidate it on quest state transitions.','A',20,'2026-06-27 05:06:03.825081',260,'COMPLETED','[\"Java\", \"Spring Boot\", \"Redis\"]','任务详情接口缓存 / Cache quest detail with Redis','2026-06-27 05:06:04.785716',4,12,14,7),(12,'自接取返回 409 + 错误码；其他接取路径不受影响；补回归测试。','2026-06-27 05:06:05.347814','发布者接取自己的任务时返回了 500。应返回 409 并给出业务错误码。\n\nReturn a clean 409 business error when a publisher accepts their own quest.','C',6,'2026-06-27 05:06:05.394963',90,'COMPLETED','[\"Java\", \"Spring Boot\"]','禁止接取自己发布的任务 / Block self-assignment','2026-06-27 05:06:06.569336',4,13,14,7),(13,'覆盖正常通过与打回两条路径；使用 H2；CI 中稳定运行。','2026-06-27 05:06:07.102574','发布→接取→提交→审核→合并的全链路缺少端到端集成测试，回归风险高。\n\nAdd an end-to-end integration test for the full quest lifecycle.','B',9,'2026-06-27 05:06:07.146710',130,'COMPLETED','[\"Java\", \"JUnit\", \"Maven\"]','提交闭环集成测试 / Integration test for submission flow','2026-06-27 05:06:08.228604',6,14,14,7),(14,'支持按难度/分类过滤；无 token 时友好提示；含命令单测。','2026-06-27 05:06:08.392246','为 quest-cli 增加 `quest list` 子命令，调用平台 API 拉取并表格化展示可接取任务。\n\nAdd a `quest list` subcommand that fetches and tabulates open quests.','C',7,'2026-06-27 05:06:08.431613',100,'COMPLETED','[\"Python\", \"REST API\"]','CLI: quest list 命令 / Add `quest list` command','2026-06-27 05:06:09.503991',4,15,15,8),(15,'README 含安装、登录、常见命令示例；中英双语。','2026-06-27 05:06:09.659609','quest-cli 缺少安装与 token 配置说明，新人无法上手。\n\nThe CLI lacks install and token-config docs.','D',3,'2026-06-27 05:06:09.695219',50,'IN_PROGRESS','[\"Python\", \"Markdown\"]','CLI 安装文档 / CLI install & auth docs','2026-06-27 05:06:09.730845',7,16,15,8),(16,'覆盖注册→接取→提交全流程；含截图占位；通过文档站构建。','2026-06-27 05:06:09.778934','为新冒险者撰写一页式快速上手指南：注册、浏览任务板、接取、提交。\n\nWrite a one-page quick start for new adventurers.','D',3,'2026-06-27 05:06:09.811026',45,'COMPLETED','[\"Markdown\"]','冒险者快速上手 / Adventurer quick start','2026-06-27 05:06:10.856219',5,17,16,9),(17,'覆盖认证/任务/提交三大模块；每个接口含请求响应示例。','2026-06-27 05:06:11.394736','把分散在代码里的接口整理成一份对外 API 参考，按模块分章。\n\nConsolidate the scattered endpoints into a public API reference.','C',6,'2026-06-27 05:06:11.429838',85,'IN_PROGRESS','[\"Markdown\", \"REST API\"]','REST API 参考文档 / REST API reference','2026-06-27 05:06:11.464391',5,18,16,9),(18,'打回即产生 UNREAD 通知；通知含任务标题与审核摘要；含单测。','2026-06-27 05:06:11.516930','审核结论为“需修改”时，应给提交者发送站内通知并标记未读。\n\nSend an in-app notification to the submitter when a review requests changes.','B',11,'2026-06-27 05:06:11.551067',150,'COMPLETED','[\"Java\", \"Spring Boot\", \"Redis\"]','打回通知 / Notify on changes requested','2026-06-27 05:06:12.571310',4,19,17,10),(19,'并发标记结果一致；补并发测试复现并验证修复。','2026-06-27 05:06:12.713826','并发标记已读时偶发状态回滚，导致红点不消失。\n\nConcurrent mark-as-read occasionally rolls back, leaving the unread badge.','C',6,'2026-06-27 05:06:12.746065',95,'PUBLISHED','[\"Java\", \"JUnit\"]','修复通知已读不生效 / Fix mark-as-read flakiness','2026-06-27 05:06:12.775273',6,20,17,10),(20,'可切换月份；空月份正确显示；聚合查询有索引；含数据层测试。','2026-06-27 05:06:13.147352','现有排行榜只有总榜。需要增加按自然月聚合的 XP 排行，并支持月份切换。\n\nAdd a monthly XP leaderboard aggregated by calendar month.','A',18,'2026-06-27 05:06:13.189315',240,'COMPLETED','[\"Java\", \"MySQL\", \"JPA\"]','月度排行榜 / Monthly leaderboard','2026-06-27 05:06:14.249911',8,21,18,11),(21,'查询次数从 N+1 降为常数；结果一致；附优化前后说明。','2026-06-27 05:06:14.403659','拉取成长档案时逐条查询 XP 流水，产生 N+1。需用一次聚合查询替代。\n\nFetching growth profiles triggers N+1 on XP transactions; replace with one aggregate query.','B',8,'2026-06-27 05:06:14.434795',120,'IN_PROGRESS','[\"Java\", \"MySQL\"]','成长档案 N+1 优化 / Fix N+1 in growth profile','2026-06-27 05:06:14.468055',8,22,18,11),(22,'镜像体积明显下降；构建可缓存；附构建与运行说明。','2026-06-27 05:06:14.516447','为后端补一个多阶段 Dockerfile，缩小镜像体积并加快构建。\n\nAdd a multi-stage Dockerfile for the backend to shrink the image.','C',7,'2026-06-27 05:06:14.548911',100,'PUBLISHED','[\"Docker\", \"Maven\"]','后端多阶段 Docker 构建 / Multi-stage Docker build','2026-06-27 05:06:14.556550',7,23,14,7),(23,'标签溢出折叠为 +N；颜色稳定可读；含组件测试。','2026-06-27 05:06:14.604867','任务卡片当前不展示技术栈。需要把技术栈渲染成彩色小标签，便于快速识别。\n\nRender the quest tech stack as colored chips on the quest card.','C',6,'2026-06-27 05:06:14.643950',90,'PUBLISHED','[\"Vue\", \"TypeScript\"]','任务卡片技术栈标签 / Tech-stack chips on quest card','2026-06-27 05:06:14.650907',3,24,13,6),(24,'刷新后旧令牌失效；重放被拒；含安全测试。','2026-06-27 05:06:14.699557','目前刷新令牌长期有效存在安全隐患。需实现一次性刷新令牌轮换与重放检测。\n\nImplement one-time refresh-token rotation with replay detection.','A',16,NULL,220,'PENDING_ADMIN_REVIEW','[\"Java\", \"Spring Security\", \"REST API\"]','刷新令牌轮换 / Refresh token rotation','2026-06-27 05:06:14.713155',4,25,14,7),(25,'一次请求标记当前用户全部未读；幂等；含测试。','2026-06-27 05:06:14.761357','用户希望一键清空未读。需要提供批量已读接口。\n\nProvide a bulk mark-as-read endpoint.','B',9,NULL,130,'PENDING_ADMIN_REVIEW','[\"Java\", \"Spring Boot\"]','通知批量已读 / Bulk mark notifications read','2026-06-27 05:06:14.774521',4,26,17,10),(26,'基于通用模板本地化；中英双语；链接到 README。','2026-06-27 05:06:14.821756','仓库缺少 CODE_OF_CONDUCT，社区协作无明确规范。\n\nThe repo lacks a CODE_OF_CONDUCT.','D',2,NULL,40,'DRAFT','[\"Markdown\"]','贡献者行为准则 / Code of conduct','2026-06-27 05:06:14.821756',5,27,16,9),(27,'覆盖 list/accept/submit 命令；mock 平台 API；CI 接入。','2026-06-27 05:06:14.879961','CLI 命令缺测试，重构易回归。\n\nThe CLI commands lack tests, making refactors risky.','C',6,NULL,95,'DRAFT','[\"Python\", \"JUnit\"]','CLI 集成测试 / CLI integration tests','2026-06-27 05:06:14.879961',6,28,15,8),(28,'首屏与翻页时展示骨架屏；数据到达后平滑替换；含组件测试。','2026-06-27 05:35:49.279099','任务卡片骨架屏加载 / Skeleton loading for quest cards\n\n本任务用于丰富悬赏板演示数据。\n首屏与翻页时展示骨架屏；数据到达后平滑替换；含组件测试。','C',5,'2026-06-27 05:35:49.324861',80,'PUBLISHED','[\"Vue\", \"TypeScript\", \"JavaScript\"]','任务卡片骨架屏加载 / Skeleton loading for quest cards','2026-06-27 05:35:49.337370',3,29,13,6),(29,'大数据量下用游标分页替代 offset；兼容旧参数。','2026-06-27 05:35:49.398439','任务列表接口加游标分页 / Cursor pagination\n\n本任务用于丰富悬赏板演示数据。\n大数据量下用游标分页替代 offset；兼容旧参数。','A',16,'2026-06-27 05:35:49.444553',220,'PUBLISHED','[\"Java\", \"Spring Boot\", \"Spring Security\"]','任务列表接口加游标分页 / Cursor pagination','2026-06-27 05:35:49.456769',4,30,14,7),(30,'按 questId 接取并打印任务分支信息；无 token 友好提示。','2026-06-27 05:35:49.518902','CLI 增加 quest accept 命令 / `quest accept`\n\n本任务用于丰富悬赏板演示数据。\n按 questId 接取并打印任务分支信息；无 token 友好提示。','C',7,'2026-06-27 05:35:49.564702',100,'PUBLISHED','[\"Python\", \"Docker\", \"Git\"]','CLI 增加 quest accept 命令 / `quest accept`','2026-06-27 05:35:49.575343',7,31,15,8),(31,'覆盖建仓→建 Issue→发布→审核全流程；中英双语。','2026-06-27 05:35:49.633262','撰写维护者发布指南 / Maintainer publishing guide\n\n本任务用于丰富悬赏板演示数据。\n覆盖建仓→建 Issue→发布→审核全流程；中英双语。','C',6,'2026-06-27 05:35:49.678314',85,'PUBLISHED','[\"Markdown\", \"REST API\"]','撰写维护者发布指南 / Maintainer publishing guide','2026-06-27 05:35:49.691666',5,32,16,9),(32,'有人接取即给发布者发 UNREAD 通知；含任务标题。','2026-06-27 05:35:49.751322','任务被接取时通知发布者 / Notify on accept\n\n本任务用于丰富悬赏板演示数据。\n有人接取即给发布者发 UNREAD 通知；含任务标题。','C',6,'2026-06-27 05:35:49.793365',90,'PUBLISHED','[\"Java\", \"Spring Boot\", \"Redis\"]','任务被接取时通知发布者 / Notify on accept','2026-06-27 05:35:49.806980',4,33,17,10),(33,'排行榜分页；额外返回当前用户排名，即使不在本页。','2026-06-27 05:35:49.868949','排行榜分页与我的排名 / Pagination & my rank\n\n本任务用于丰富悬赏板演示数据。\n排行榜分页；额外返回当前用户排名，即使不在本页。','B',9,'2026-06-27 05:35:49.924163',140,'PUBLISHED','[\"Java\", \"MySQL\", \"JPA\"]','排行榜分页与我的排名 / Pagination & my rank','2026-06-27 05:35:49.937105',8,34,18,11),(34,'标题/描述模糊匹配；与现有筛选叠加；防抖输入。','2026-06-27 05:35:50.005138','悬赏板支持关键字搜索 / Keyword search on board\n\n本任务用于丰富悬赏板演示数据。\n标题/描述模糊匹配；与现有筛选叠加；防抖输入。','B',8,'2026-06-27 05:35:50.056123',130,'PUBLISHED','[\"Vue\", \"TypeScript\", \"JavaScript\"]','悬赏板支持关键字搜索 / Keyword search on board','2026-06-27 05:35:50.073520',3,35,13,6),(35,'并发接取同一任务只产生一条有效接取；补并发测试。','2026-06-27 05:35:50.135433','接取并发幂等保护 / Idempotent accept under race\n\n本任务用于丰富悬赏板演示数据。\n并发接取同一任务只产生一条有效接取；补并发测试。','B',11,'2026-06-27 05:35:50.181419',160,'PUBLISHED','[\"Java\", \"Spring Boot\", \"Spring Security\"]','接取并发幂等保护 / Idempotent accept under race','2026-06-27 05:35:50.195286',4,36,14,7),(36,'所有列表命令加 --json；便于脚本消费。','2026-06-27 05:35:50.246204','CLI 输出支持 JSON 格式 / JSON output\n\n本任务用于丰富悬赏板演示数据。\n所有列表命令加 --json；便于脚本消费。','D',3,'2026-06-27 05:35:50.292462',50,'PUBLISHED','[\"Python\", \"Docker\", \"Git\"]','CLI 输出支持 JSON 格式 / JSON output','2026-06-27 05:35:50.303490',7,37,15,8),(37,'汇总登录、接取、提交常见问题与排查；通过文档站构建。','2026-06-27 05:35:50.365930','整理常见问题 FAQ / FAQ page\n\n本任务用于丰富悬赏板演示数据。\n汇总登录、接取、提交常见问题与排查；通过文档站构建。','D',2,'2026-06-27 05:35:50.406498',40,'PUBLISHED','[\"Markdown\", \"REST API\"]','整理常见问题 FAQ / FAQ page','2026-06-27 05:35:50.418058',5,38,16,9),(38,'未读优先、按时间倒序、分页返回；含测试。','2026-06-27 05:35:50.477976','通知支持分页拉取 / Paginated notifications\n\n本任务用于丰富悬赏板演示数据。\n未读优先、按时间倒序、分页返回；含测试。','B',9,'2026-06-27 05:35:50.518144',130,'PUBLISHED','[\"Java\", \"Spring Boot\", \"Redis\"]','通知支持分页拉取 / Paginated notifications','2026-06-27 05:35:50.530147',4,39,17,10),(39,'等级所需 XP 改为可配置表；现有数据平滑迁移。','2026-06-27 05:35:50.604060','成长等级曲线可配置 / Configurable level curve\n\n本任务用于丰富悬赏板演示数据。\n等级所需 XP 改为可配置表；现有数据平滑迁移。','A',17,'2026-06-27 05:35:50.647155',230,'PUBLISHED','[\"Java\", \"MySQL\", \"JPA\"]','成长等级曲线可配置 / Configurable level curve','2026-06-27 05:35:50.660772',8,40,18,11),(40,'移动端/平板布局自适应；图片与代码块不溢出。','2026-06-27 05:35:50.729185','任务详情页响应式适配 / Responsive quest detail\n\n本任务用于丰富悬赏板演示数据。\n移动端/平板布局自适应；图片与代码块不溢出。','C',6,'2026-06-27 05:35:50.774278',90,'PUBLISHED','[\"Vue\", \"TypeScript\", \"JavaScript\"]','任务详情页响应式适配 / Responsive quest detail','2026-06-27 05:35:50.786430',3,41,13,6),(41,'对 XP/工时上限、技术栈非空做服务层校验并返回明确错误码。','2026-06-27 05:35:50.841889','发布任务参数校验增强 / Stronger create validation\n\n本任务用于丰富悬赏板演示数据。\n对 XP/工时上限、技术栈非空做服务层校验并返回明确错误码。','C',6,'2026-06-27 05:35:50.889665',90,'PUBLISHED','[\"Java\", \"Spring Boot\", \"Spring Security\"]','发布任务参数校验增强 / Stronger create validation','2026-06-27 05:35:50.903529',4,42,14,7),(42,'支持 ~/.questrc 存 token 与服务地址；环境变量覆盖。','2026-06-27 05:35:50.965663','CLI 配置文件支持 / Config file support\n\n本任务用于丰富悬赏板演示数据。\n支持 ~/.questrc 存 token 与服务地址；环境变量覆盖。','B',8,'2026-06-27 05:35:51.007345',130,'PUBLISHED','[\"Python\", \"Docker\", \"Git\"]','CLI 配置文件支持 / Config file support','2026-06-27 05:35:51.019745',7,43,15,8),(43,'前端/后端/Gitea/MySQL 关系图 + 文字说明。','2026-06-27 05:35:51.074201','补充架构概览图 / Architecture overview\n\n本任务用于丰富悬赏板演示数据。\n前端/后端/Gitea/MySQL 关系图 + 文字说明。','C',6,'2026-06-27 05:35:51.111066',95,'PUBLISHED','[\"Markdown\", \"REST API\"]','补充架构概览图 / Architecture overview','2026-06-27 05:35:51.121911',5,44,16,9),(44,'用户可开关邮件通知；关闭时仅站内通知。','2026-06-27 05:35:51.174148','邮件通知开关 / Email notification opt-in\n\n本任务用于丰富悬赏板演示数据。\n用户可开关邮件通知；关闭时仅站内通知。','B',11,'2026-06-27 05:35:51.214458',150,'PUBLISHED','[\"Java\", \"Spring Boot\", \"Redis\"]','邮件通知开关 / Email notification opt-in','2026-06-27 05:35:51.227094',4,45,17,10),(45,'按日聚合贡献次数，提供热力图所需接口。','2026-06-27 05:35:51.292709','贡献热力统计 / Contribution heatmap data\n\n本任务用于丰富悬赏板演示数据。\n按日聚合贡献次数，提供热力图所需接口。','B',10,'2026-06-27 05:35:51.340434',150,'PUBLISHED','[\"Java\", \"MySQL\", \"JPA\"]','贡献热力统计 / Contribution heatmap data','2026-06-27 05:35:51.354906',8,46,18,11),(46,'全局暗色主题；偏好持久化到本地存储。','2026-06-27 05:35:51.417209','暗色主题切换 / Dark mode toggle\n\n本任务用于丰富悬赏板演示数据。\n全局暗色主题；偏好持久化到本地存储。','B',10,'2026-06-27 05:35:51.473441',150,'PUBLISHED','[\"Vue\", \"TypeScript\", \"JavaScript\"]','暗色主题切换 / Dark mode toggle','2026-06-27 05:35:51.487451',3,47,13,6),(47,'tagIds 传多个时取交集；分页正确；新增 MVC 测试。','2026-06-27 05:35:51.552601','任务搜索支持多标签过滤 / Multi-tag filter\n\n本任务用于丰富悬赏板演示数据。\ntagIds 传多个时取交集；分页正确；新增 MVC 测试。','B',9,'2026-06-27 05:35:51.603897',140,'PUBLISHED','[\"Java\", \"Spring Boot\", \"Spring Security\"]','任务搜索支持多标签过滤 / Multi-tag filter','2026-06-27 05:35:51.616893',4,48,14,7),(48,'排行榜结果缓存并定时刷新；缓存击穿保护。','2026-06-27 05:35:51.679247','排行榜缓存与定时刷新 / Cache & refresh\n\n本任务用于丰富悬赏板演示数据。\n排行榜结果缓存并定时刷新；缓存击穿保护。','A',16,'2026-06-27 05:35:51.724679',220,'PUBLISHED','[\"Java\", \"MySQL\", \"JPA\"]','排行榜缓存与定时刷新 / Cache & refresh','2026-06-27 05:35:51.739796',8,49,18,11),(49,'点击接取弹确认框，避免误触；含交互测试。','2026-06-27 05:35:51.829505','接取按钮加二次确认 / Confirm before accept\n\n本任务用于丰富悬赏板演示数据。\n点击接取弹确认框，避免误触；含交互测试。','D',3,'2026-06-27 05:35:51.867091',45,'PUBLISHED','[\"Vue\", \"TypeScript\", \"JavaScript\"]','接取按钮加二次确认 / Confirm before accept','2026-06-27 05:35:51.880564',3,50,13,6),(50,'发布/审核/接取/合并写审计；可按用户与时间查询。','2026-06-27 05:35:51.940845','操作审计日志 / Audit log for quest actions\n\n本任务用于丰富悬赏板演示数据。\n发布/审核/接取/合并写审计；可按用户与时间查询。','A',18,'2026-06-27 05:35:51.988369',240,'PUBLISHED','[\"Java\", \"Spring Boot\", \"Spring Security\"]','操作审计日志 / Audit log for quest actions','2026-06-27 05:35:51.999863',4,51,14,7);
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
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `repositories` WRITE;
/*!40000 ALTER TABLE `repositories` DISABLE KEYS */;
INSERT INTO `repositories` VALUES (1,'2026-06-06 12:50:12.388965','main',NULL,'GITEA','2026-06-06 12:50:12.557580','hello-world-mvp','http://localhost:3000/spike-admin/hello-world-mvp',NULL,'SYNCED','2026-06-06 12:50:12.560024',5,NULL),(2,'2026-06-18 07:07:09.455862','main','7','GITEA',NULL,'myfirst-repo-demo','http://localhost:3000/spike-admin/myfirst-repo-demo',NULL,'PENDING','2026-06-18 07:07:09.455862',5,'这个仓库用于进行测试'),(3,'2026-06-18 07:08:08.893587','main','8','GITEA',NULL,'gitguild-demo','http://localhost:3000/spike-admin/gitguild-demo',NULL,'PENDING','2026-06-18 07:08:08.893587',5,'这个仓库用于进行测试'),(4,'2026-06-18 07:08:13.557675','main','9','GITEA',NULL,'gitguild','http://localhost:3000/spike-admin/gitguild',NULL,'PENDING','2026-06-18 07:08:13.557675',5,'这个仓库用于进行测试'),(5,'2026-06-18 07:11:11.546783','main','10','GITEA',NULL,'gitguild-demo2','http://localhost:3000/spike-admin/gitguild-demo2',NULL,'PENDING','2026-06-18 07:11:11.546783',5,'用于演示'),(6,'2026-06-27 05:05:56.462140','main','11','GITEA',NULL,'gitguild-web','http://localhost:3000/spike-admin/gitguild-web',NULL,'PENDING','2026-06-27 05:05:56.462140',13,'Git Guild 冒险者公会 Web 前端（Vue 3 + Vite）'),(7,'2026-06-27 05:05:56.758026','main','12','GITEA',NULL,'gitguild-api','http://localhost:3000/spike-admin/gitguild-api',NULL,'PENDING','2026-06-27 05:05:56.758026',14,'Git Guild 后端服务（Spring Boot 3 + JPA）'),(8,'2026-06-27 05:05:57.057574','main','13','GITEA',NULL,'quest-cli','http://localhost:3000/spike-admin/quest-cli',NULL,'PENDING','2026-06-27 05:05:57.057574',15,'公会任务命令行工具，开发者本地管理悬赏'),(9,'2026-06-27 05:05:57.350269','main','14','GITEA',NULL,'guild-docs','http://localhost:3000/spike-admin/guild-docs',NULL,'PENDING','2026-06-27 05:05:57.350269',16,'Git Guild 文档站点与使用手册'),(10,'2026-06-27 05:05:57.637268','main','15','GITEA',NULL,'notify-service','http://localhost:3000/spike-admin/notify-service',NULL,'PENDING','2026-06-27 05:05:57.637268',17,'站内通知与邮件下发微服务'),(11,'2026-06-27 05:05:57.950973','main','16','GITEA',NULL,'leaderboard-svc','http://localhost:3000/spike-admin/leaderboard-svc',NULL,'PENDING','2026-06-27 05:05:57.950973',18,'成长档案与排行榜统计服务');
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
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `review_items` WRITE;
/*!40000 ALTER TABLE `review_items` DISABLE KEYS */;
INSERT INTO `review_items` VALUES (1,'PR 已关联到当前任务仓库','已关联 PR #5。','2026-06-07 06:03:45.391300',_binary '',1),(2,'PR 合并状态','当前 PR 状态为 OPEN。“接受提交”只完成任务并发放 XP，不会自动合并；如需合并请用「合并 PR」按钮（或在 Gitea 手动合并）。','2026-06-07 06:03:45.395463',_binary '',1),(3,'成果说明已提交','准备提交 QST-0003 的成果。请说明本次修改、测试结果，以及完成标准的逐项自检情况。','2026-06-07 06:03:45.397405',_binary '',1),(4,'2','请结合 PR 变更和提交说明人工确认。','2026-06-07 06:03:45.399388',_binary '',1),(5,'PR 已关联到当前任务仓库','已关联 PR #7。','2026-06-07 06:50:58.345282',_binary '',2),(6,'PR 合并状态','当前 PR 状态为 OPEN。“接受提交”只完成任务并发放 XP，不会自动合并；如需合并请用「合并 PR」按钮（或在 Gitea 手动合并）。','2026-06-07 06:50:58.347401',_binary '',2),(7,'成果说明已提交','准备提交 QST-0004 的成果。请说明本次修改、测试结果，以及完成标准的逐项自检情况。','2026-06-07 06:50:58.349084',_binary '',2),(8,'3','请结合 PR 变更和提交说明人工确认。','2026-06-07 06:50:58.350816',_binary '',2),(9,'功能符合验收标准','已对照完成标准逐项核对。','2026-06-27 05:05:59.420514',_binary '',3),(10,'代码质量','结构清晰，命名规范。','2026-06-27 05:05:59.424747',_binary '',3),(11,'测试覆盖','关键路径有测试覆盖。','2026-06-27 05:05:59.427142',_binary '',3),(12,'功能符合验收标准','已对照完成标准逐项核对。','2026-06-27 05:06:01.290291',_binary '',4),(13,'代码质量','结构清晰，命名规范。','2026-06-27 05:06:01.291479',_binary '',4),(14,'测试覆盖','关键路径有测试覆盖。','2026-06-27 05:06:01.292666',_binary '',4),(15,'功能符合验收标准','已对照完成标准逐项核对。','2026-06-27 05:06:02.486551',_binary '',5),(16,'代码质量','结构清晰，命名规范。','2026-06-27 05:06:02.487779',_binary '',5),(17,'测试覆盖','关键路径有测试覆盖。','2026-06-27 05:06:02.489044',_binary '',5),(18,'功能符合验收标准','已对照完成标准逐项核对。','2026-06-27 05:06:03.676618',_binary '',6),(19,'代码质量','结构清晰，命名规范。','2026-06-27 05:06:03.677800',_binary '',6),(20,'测试覆盖','关键路径有测试覆盖。','2026-06-27 05:06:03.678847',_binary '',6),(21,'功能符合验收标准','已对照完成标准逐项核对。','2026-06-27 05:06:04.827153',_binary '',7),(22,'代码质量','结构清晰，命名规范。','2026-06-27 05:06:04.828710',_binary '',7),(23,'测试覆盖','关键路径有测试覆盖。','2026-06-27 05:06:04.829901',_binary '',7),(24,'功能符合验收标准','已对照完成标准逐项核对。','2026-06-27 05:06:06.599518',_binary '',8),(25,'代码质量','结构清晰，命名规范。','2026-06-27 05:06:06.602166',_binary '',8),(26,'测试覆盖','关键路径有测试覆盖。','2026-06-27 05:06:06.603687',_binary '',8),(27,'功能符合验收标准','已对照完成标准逐项核对。','2026-06-27 05:06:08.269632',_binary '',9),(28,'代码质量','结构清晰，命名规范。','2026-06-27 05:06:08.271385',_binary '',9),(29,'测试覆盖','关键路径有测试覆盖。','2026-06-27 05:06:08.272639',_binary '',9),(30,'功能符合验收标准','已对照完成标准逐项核对。','2026-06-27 05:06:09.541891',_binary '',10),(31,'代码质量','结构清晰，命名规范。','2026-06-27 05:06:09.543363',_binary '',10),(32,'测试覆盖','关键路径有测试覆盖。','2026-06-27 05:06:09.544591',_binary '',10),(33,'功能符合验收标准','已对照完成标准逐项核对。','2026-06-27 05:06:10.877946',_binary '',11),(34,'代码质量','结构清晰，命名规范。','2026-06-27 05:06:10.879270',_binary '',11),(35,'测试覆盖','关键路径有测试覆盖。','2026-06-27 05:06:10.880561',_binary '',11),(36,'功能符合验收标准','已对照完成标准逐项核对。','2026-06-27 05:06:12.603029',_binary '',12),(37,'代码质量','结构清晰，命名规范。','2026-06-27 05:06:12.604581',_binary '',12),(38,'测试覆盖','关键路径有测试覆盖。','2026-06-27 05:06:12.605707',_binary '',12),(39,'功能符合验收标准','已对照完成标准逐项核对。','2026-06-27 05:06:14.286922',_binary '',13),(40,'代码质量','结构清晰，命名规范。','2026-06-27 05:06:14.288024',_binary '',13),(41,'测试覆盖','关键路径有测试覆盖。','2026-06-27 05:06:14.288955',_binary '',13);
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
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `review_records` WRITE;
/*!40000 ALTER TABLE `review_records` DISABLE KEYS */;
INSERT INTO `review_records` VALUES (1,'2026-06-07 06:03:45.385021','APPROVED','2026-06-07 06:03:45.385021','PR 尚未满足全部通过条件，请根据未通过项给出修改意见。',5,1),(2,'2026-06-07 06:50:58.342667','APPROVED','2026-06-07 06:50:58.342667','PR 尚未满足全部通过条件，请根据未通过项给出修改意见。',5,2),(3,'2026-06-27 05:05:59.415945','APPROVED','2026-06-27 05:05:59.415945','实现完整，验收通过，准予合并。',13,3),(4,'2026-06-27 05:06:01.288748','APPROVED','2026-06-27 05:06:01.288748','实现完整，验收通过，准予合并。',13,4),(5,'2026-06-27 05:06:02.484925','APPROVED','2026-06-27 05:06:02.484925','实现完整，验收通过，准予合并。',13,5),(6,'2026-06-27 05:06:03.674667','APPROVED','2026-06-27 05:06:03.674667','实现完整，验收通过，准予合并。',14,6),(7,'2026-06-27 05:06:04.825196','APPROVED','2026-06-27 05:06:04.825196','实现完整，验收通过，准予合并。',14,7),(8,'2026-06-27 05:06:06.596981','APPROVED','2026-06-27 05:06:06.596981','实现完整，验收通过，准予合并。',14,8),(9,'2026-06-27 05:06:08.267651','APPROVED','2026-06-27 05:06:08.267651','实现完整，验收通过，准予合并。',14,9),(10,'2026-06-27 05:06:09.539903','APPROVED','2026-06-27 05:06:09.539903','实现完整，验收通过，准予合并。',15,10),(11,'2026-06-27 05:06:10.876079','APPROVED','2026-06-27 05:06:10.876079','实现完整，验收通过，准予合并。',16,11),(12,'2026-06-27 05:06:12.601418','APPROVED','2026-06-27 05:06:12.601418','实现完整，验收通过，准予合并。',17,12),(13,'2026-06-27 05:06:14.285554','APPROVED','2026-06-27 05:06:14.285554','实现完整，验收通过，准予合并。',18,13);
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
  `evidence` longtext,
  PRIMARY KEY (`submission_id`),
  KEY `FKkdf11d3u4rsa761k87i7ecwj9` (`pull_request_id`),
  KEY `FK7k48utjjd7t8wwvftdm95oacj` (`quest_id`),
  KEY `FKjnorec1kmqhxwisxx065bp7it` (`submitter_id`),
  CONSTRAINT `FK7k48utjjd7t8wwvftdm95oacj` FOREIGN KEY (`quest_id`) REFERENCES `quests` (`quest_id`),
  CONSTRAINT `FKjnorec1kmqhxwisxx065bp7it` FOREIGN KEY (`submitter_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKkdf11d3u4rsa761k87i7ecwj9` FOREIGN KEY (`pull_request_id`) REFERENCES `pull_requests` (`pull_request_id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `submissions` WRITE;
/*!40000 ALTER TABLE `submissions` DISABLE KEYS */;
INSERT INTO `submissions` VALUES (1,'2026-06-07 06:03:25.853528','准备提交 QST-0003 的成果。请说明本次修改、测试结果，以及完成标准的逐项自检情况。','APPROVED','2026-06-07 06:03:25.853528','2026-06-07 06:03:45.361104',1,3,6,NULL),(2,'2026-06-07 06:50:41.035918','准备提交 QST-0004 的成果。请说明本次修改、测试结果，以及完成标准的逐项自检情况。','APPROVED','2026-06-07 06:50:41.035918','2026-06-07 06:50:58.329748',2,4,6,NULL),(3,'2026-06-27 05:05:59.285201','已按完成标准实现并自测：下拉框可选 A/B/C/D 与“全部”；切换后列表实时刷新；与分类筛选可叠加；新增组件单测。','APPROVED','2026-06-27 05:05:59.285201','2026-06-27 05:05:59.373530',3,7,22,NULL),(4,'2026-06-27 05:06:01.194643','已按完成标准实现并自测：进度条按任务真实状态高亮当前阶段；移动端自适应；含快照测试。','APPROVED','2026-06-27 05:06:01.194643','2026-06-27 05:06:01.251507',4,8,23,NULL),(5,'2026-06-27 05:06:02.425928','已按完成标准实现并自测：用户名/密码框回车均可提交；不破坏已有按钮点击；补充交互测试。','APPROVED','2026-06-27 05:06:02.425928','2026-06-27 05:06:02.461317',5,9,24,NULL),(6,'2026-06-27 05:06:03.607617','已按完成标准实现并自测：sortBy 接受 rewardXp 且方向可控；分页正确；新增 service 与 MVC 测试。','APPROVED','2026-06-27 05:06:03.607617','2026-06-27 05:06:03.642610',6,10,25,NULL),(7,'2026-06-27 05:06:04.754299','已按完成标准实现并自测：命中缓存不走库；审核/接取/合并后缓存失效；含缓存失效集成测试与压测说明。','APPROVED','2026-06-27 05:06:04.754299','2026-06-27 05:06:04.785662',7,11,26,NULL),(8,'2026-06-27 05:06:06.522872','已按完成标准实现并自测：自接取返回 409 + 错误码；其他接取路径不受影响；补回归测试。','APPROVED','2026-06-27 05:06:06.522872','2026-06-27 05:06:06.569270',8,12,27,NULL),(9,'2026-06-27 05:06:08.183852','已按完成标准实现并自测：覆盖正常通过与打回两条路径；使用 H2；CI 中稳定运行。','APPROVED','2026-06-27 05:06:08.183852','2026-06-27 05:06:08.228552',9,13,28,NULL),(10,'2026-06-27 05:06:09.465346','已按完成标准实现并自测：支持按难度/分类过滤；无 token 时友好提示；含命令单测。','APPROVED','2026-06-27 05:06:09.465346','2026-06-27 05:06:09.503916',10,14,29,NULL),(11,'2026-06-27 05:06:10.826587','已按完成标准实现并自测：覆盖注册→接取→提交全流程；含截图占位；通过文档站构建。','APPROVED','2026-06-27 05:06:10.826587','2026-06-27 05:06:10.856194',11,16,31,NULL),(12,'2026-06-27 05:06:12.533936','已按完成标准实现并自测：打回即产生 UNREAD 通知；通知含任务标题与审核摘要；含单测。','APPROVED','2026-06-27 05:06:12.533936','2026-06-27 05:06:12.571259',12,18,33,NULL),(13,'2026-06-27 05:06:14.216001','已按完成标准实现并自测：可切换月份；空月份正确显示；聚合查询有索引；含数据层测试。','APPROVED','2026-06-27 05:06:14.216001','2026-06-27 05:06:14.249839',13,20,35,NULL);
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
) ENGINE=InnoDB AUTO_INCREMENT=44 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (3,'2026-06-03 13:09:49.272054','admin@gg.local','$2a$10$Qd5H2WYJeBvIPgSo9tVf9eoIglSPUzsAnJH8Be/tDVUX/JNyD4XyC','ADMIN','ACTIVE',0,'2026-06-06 12:50:12.099071','admin',NULL,NULL,NULL),(5,'2026-06-04 06:11:55.000000','guild@gg.local','$2a$10$Qd5H2WYJeBvIPgSo9tVf9eoIglSPUzsAnJH8Be/tDVUX/JNyD4XyC','MAINTAINER','ACTIVE',0,'2026-06-06 12:50:12.099071','guild',NULL,NULL,NULL),(6,'2026-06-04 06:11:55.000000','advent@gg.local','$2a$10$Qd5H2WYJeBvIPgSo9tVf9eoIglSPUzsAnJH8Be/tDVUX/JNyD4XyC','BEGINNER','ACTIVE',0,'2026-06-06 12:50:12.099071','advent',NULL,NULL,NULL),(12,'2026-06-27 05:05:45.912656','ye.guildmaster@gg.local','$2a$10$2y/0U7ptzhC4UQzGdClnnuLn39ExEEOEeaWTGcBUBfNXg9j8ZbgFy','MAINTAINER','ACTIVE',0,'2026-06-27 05:05:45.912656','guildmaster_ye',NULL,NULL,'å…¬ä¼šçš„ç« ç¨‹ï¼Œç”±æˆ‘æ‰§ç¬”ã€‚'),(13,'2026-06-27 05:05:46.066376','lin.archmage@gg.local','$2a$10$Id7Oc5iCfFSlfpGb64sxU.BvZRQnO.juwPT3iBkDt3kTxg.z9IQyO','MAINTAINER','ACTIVE',0,'2026-06-27 05:05:46.066376','archmage_lin',NULL,NULL,'ä»£ç å³å’’æ–‡ï¼Œé€»è¾‘å³é­”åŠ›ã€‚'),(14,'2026-06-27 05:05:46.218196','zhao.warden@gg.local','$2a$10$1WUNvFpWXq15rigiHgPfXOHn7jlrphY4HT3WK9n3zOXkawTv1mvmu','MAINTAINER','ACTIVE',0,'2026-06-27 05:05:46.218196','warden_zhao',NULL,NULL,'å®ˆå¥½ä¸»åˆ†æ”¯è¿™é“åŸŽé—¨ã€‚'),(15,'2026-06-27 05:05:46.371404','huang.smith@gg.local','$2a$10$HkXxrZzKk.xEdTtJcH8/PupDatPMxqY./GA.RnFJZaMWn9HrlQPWe','MAINTAINER','ACTIVE',0,'2026-06-27 05:05:46.371404','smith_huang',NULL,NULL,'å¥½å·¥å…·æ˜¯åå¤é”»æ‰“å‡ºæ¥çš„ã€‚'),(16,'2026-06-27 05:05:46.532013','qian.scribe@gg.local','$2a$10$25A.8/0nqzQNhEF8OK8wEeA44xscbuSazGVVKuVfWuWTfYOXpHrom','MAINTAINER','ACTIVE',0,'2026-06-27 05:05:46.532013','scribe_qian',NULL,NULL,'æ²¡æœ‰æ–‡æ¡£çš„åŠŸèƒ½ç­‰äºŽä¸å­˜åœ¨ã€‚'),(17,'2026-06-27 05:05:46.688543','sun.alchemist@gg.local','$2a$10$sJQsJNY41FgPxw88N/oj..rvlM930ig2/lpIeO5AOUytmDzH7P7CC','MAINTAINER','ACTIVE',0,'2026-06-27 05:05:46.688543','alchemist_sun',NULL,NULL,'æŠŠæ··ä¹±çš„æ•°æ®ç‚¼æˆç§©åºã€‚'),(18,'2026-06-27 05:05:46.843660','zhou.captain@gg.local','$2a$10$Yi8aSfGwIx6xJm1kld4gWe5UX6XH97bH.5QFTPFblf8.P9E.ViAjO','MAINTAINER','ACTIVE',0,'2026-06-27 05:05:46.843660','captain_zhou',NULL,NULL,'éƒ¨ç½²ä¸Šçº¿ï¼Œç¨³å­—å½“å¤´ã€‚'),(19,'2026-06-27 05:05:46.994670','wu.ranger@gg.local','$2a$10$H4..pxkl4tK9KQJAHZJ67ux37aAocBSHkG6vwgwGZPz4MWXgM.kne','MAINTAINER','ACTIVE',0,'2026-06-27 05:05:46.994670','ranger_wu',NULL,NULL,'åœ¨æœ€æ·±çš„è°ƒç”¨æ ˆé‡Œè¿½è¸ª bugã€‚'),(20,'2026-06-27 05:05:47.147265','feng.elder@gg.local','$2a$10$DGnCVEYWxOAEPd1pv6i2keNtfPkzJZae4YcHMOvMk/mN7DGhsbala','MAINTAINER','ACTIVE',0,'2026-06-27 05:05:47.147265','elder_feng',NULL,NULL,'è¯„å®¡æ˜¯å…¬ä¼šçš„è‰¯å¿ƒã€‚'),(21,'2026-06-27 05:05:47.299832','he.merchant@gg.local','$2a$10$S9BYNp5xTySQ.ks9MQAqLeUroKyyTpSNxU.sGAPYqGOgetUSOR6le','MAINTAINER','ACTIVE',0,'2026-06-27 05:05:47.299832','merchant_he',NULL,NULL,'æ‚¬èµè¦å¼€å¾—è®©äººæ„¿æ„æŽ¥ã€‚'),(22,'2026-06-27 05:05:47.452716','chen.novice@gg.local','$2a$10$nVB9M3MMf1Sf9XS1S7ijR.2fQvJILekxUlDSDb1buBvrcePc69tIq','BEGINNER','ACTIVE',0,'2026-06-27 05:05:47.452716','novice_chen',NULL,NULL,'ä»Žç¬¬ä¸€ä¸ª commit å¼€å§‹æˆé•¿ã€‚'),(23,'2026-06-27 05:05:47.606349','liu.squire@gg.local','$2a$10$.5kZYaJtnBq4AIjqYzOk8.NzAfZ15mLB6SwwC2ugBySIRNU.6MxHu','BEGINNER','ACTIVE',0,'2026-06-27 05:05:47.606349','squire_liu',NULL,NULL,'ä»Šå¤©æ¯”æ˜¨å¤©å¤šæ‡‚ä¸€ä¸ªæŠ¥é”™ã€‚'),(24,'2026-06-27 05:05:47.763602','tang.rogue@gg.local','$2a$10$fY31qtYVl7mSA6JuYerKy.leqLKE2X2P9w45puEAQUD1vw4OoNu3O','BEGINNER','ACTIVE',0,'2026-06-27 05:05:47.763602','rogue_tang',NULL,NULL,'ä¸“æŒ‘ç¡¬éª¨å¤´çš„ä»»åŠ¡å•ƒã€‚'),(25,'2026-06-27 05:05:47.915515','xu.apprentice@gg.local','$2a$10$0cGBqQUDCyCiAUfzZh5hrOpWWuTvZLJBPMAqBlL9RTyZgScHtmp/O','BEGINNER','ACTIVE',0,'2026-06-27 05:05:47.915515','mage_apprentice_xu',NULL,NULL,'å‰ç«¯çš„ä¸–ç•Œç”±æˆ‘ç‚¹äº®ã€‚'),(26,'2026-06-27 05:05:48.067478','deng.archer@gg.local','$2a$10$9IumfJDJYtKFhgVQIBB.PeS.I5CNSgAZoWRWdes2h5mn/TcIl2Np2','BEGINNER','ACTIVE',0,'2026-06-27 05:05:48.067478','archer_deng',NULL,NULL,'ä¸€å‘å…¥é­‚ï¼Œä¸€æµ‹å³è¿‡ã€‚'),(27,'2026-06-27 05:05:48.221298','pan.monk@gg.local','$2a$10$yQbLYCKUUJCunodW6KlsveWHvCmOniUJmoLZi4jiU8.nuyemmJuta','BEGINNER','ACTIVE',0,'2026-06-27 05:05:48.221298','monk_pan',NULL,NULL,'é‡æž„æ˜¯ä¸€ç§ä¿®è¡Œã€‚'),(28,'2026-06-27 05:05:48.372545','xie.bard@gg.local','$2a$10$uM8PrcpPc5UDovkUeNfhauWXHvEHiqikSqGhc8xN6n8tRk0wAH.Lq','BEGINNER','ACTIVE',0,'2026-06-27 05:05:48.372545','bard_xie',NULL,NULL,'ç”¨ README æŠŠæ•…äº‹è®²æ¸…æ¥šã€‚'),(29,'2026-06-27 05:05:48.522297','cao.knight@gg.local','$2a$10$20AUgMDExUFJ7YT/4grw9.eATnEFU2l92H/dRfU60GQLijo0vZTBK','BEGINNER','ACTIVE',0,'2026-06-27 05:05:48.522297','knight_cao',NULL,NULL,'æŽ¥ä¸‹çš„ä»»åŠ¡å¿…æœ‰äº¤ä»£ã€‚'),(30,'2026-06-27 05:05:48.675276','lu.druid@gg.local','$2a$10$mQAp.UpTI0RR3X.NzfJlfuNuKLz9Cz1xiWAMbNkr.j8fNo.79Bu1G','BEGINNER','ACTIVE',0,'2026-06-27 05:05:48.675276','druid_lu',NULL,NULL,'è®©æ•°æ®åº“é•¿æˆå®ƒè¯¥æœ‰çš„æ ·å­ã€‚'),(31,'2026-06-27 05:05:48.827379','meng.thief@gg.local','$2a$10$hJTYdlFmOLNT54Bonop5cO80c9zi7dJraY9k7KtddxQVvkkbpprxq','BEGINNER','ACTIVE',0,'2026-06-27 05:05:48.827379','thief_meng',NULL,NULL,'æ‚„æ‚„ä¿®å¥½å®ƒï¼ŒæƒŠè‰³æ‰€æœ‰äººã€‚'),(32,'2026-06-27 05:05:48.979578','yao.paladin@gg.local','$2a$10$7W/jzuGBGzNcsYzitXoRIe2FFpwKprAjl/eetNcnCikt0MQASD/Z2','BEGINNER','ACTIVE',0,'2026-06-27 05:05:48.979578','paladin_yao',NULL,NULL,'å®ˆæŠ¤ä»£ç è´¨é‡çš„æœ€åŽä¸€é“å…‰ã€‚'),(33,'2026-06-27 05:05:49.131152','jiang.scout@gg.local','$2a$10$/0iX52N6xXAjCHlWLX3IQ.HJXComlRLxROu2NTSMymWT2JfIQD3AO','BEGINNER','ACTIVE',0,'2026-06-27 05:05:49.131152','scout_jiang',NULL,NULL,'å…ˆä¾¦å¯Ÿæ¸…æ¥š issue å†åŠ¨æ‰‹ã€‚'),(34,'2026-06-27 05:05:49.286485','fang.hunter@gg.local','$2a$10$sZ06se558q1UheryUDeRBu3ylNWifhXdRmNUerENLXVC9Y0VkAVl6','BEGINNER','ACTIVE',0,'2026-06-27 05:05:49.286485','hunter_fang',NULL,NULL,'çŒŽ bug æ˜¯æˆ‘çš„æœ¬èŒã€‚'),(35,'2026-06-27 05:05:49.439067','yu.sorcerer@gg.local','$2a$10$mVO/ZtCQq1c.rQtHJQy9G.zzLFZTkjT3wEtwmXUuKNTV6/h.ge2my','BEGINNER','ACTIVE',0,'2026-06-27 05:05:49.439067','sorcerer_yu',NULL,NULL,'å¼‚æ­¥ä¸Žå¹¶å‘ï¼Œçš†åœ¨æŽŒæŽ§ã€‚'),(36,'2026-06-27 05:05:49.589605','ma.warrior@gg.local','$2a$10$6mjVRD9biZPP9LXMyqA6luv.UT2ZKQwZzV3j.hIyN3i/UZ9oRTWTu','BEGINNER','ACTIVE',0,'2026-06-27 05:05:49.589605','warrior_ma',NULL,NULL,'ç¡¬åˆšæœ€å¤æ‚çš„ä¸šåŠ¡é€»è¾‘ã€‚'),(37,'2026-06-27 05:05:49.742509','shen.cleric@gg.local','$2a$10$IwtS0lH7eRnfI/tsBWeUUeIVUc4CtV88ESMEVTZKTt0fMRTEKH3Pe','BEGINNER','ACTIVE',0,'2026-06-27 05:05:49.742509','cleric_shen',NULL,NULL,'ä¸ºç”Ÿäº§çŽ¯å¢ƒç¥ˆç¥·ï¼Œä¹Ÿä¸ºå®ƒå†™ç›‘æŽ§ã€‚'),(38,'2026-06-27 05:05:49.893278','gao.ninja@gg.local','$2a$10$DSxirPxw8VonTwSISPv18.eFZ6JuL2sZdU3NXvjn0NDL6KAKOycmy','BEGINNER','ACTIVE',0,'2026-06-27 05:05:49.893278','ninja_gao',NULL,NULL,'æ‚„æ— å£°æ¯åœ°äº¤ä»˜é«˜è´¨é‡ä»£ç ã€‚'),(39,'2026-06-27 05:05:50.041871','dai.apprentice@gg.local','$2a$10$b2Jida8wBi3l9Ek5.VkHRuv..4o0jjk.z18BpFcnlCH36F3Xphgay','BEGINNER','ACTIVE',0,'2026-06-27 05:05:50.041871','apprentice_dai',NULL,NULL,'å…¬ä¼šæ–°äººï¼Œå‰æ¥æŠ¥åˆ°ã€‚'),(40,'2026-06-27 05:05:50.193886','lei.trader@gg.local','$2a$10$gBZYaQIsQ/cn8Ksyj67lBuSJ.IfpYBhdooz8whOcGTmKyvBUYBWZC','BEGINNER','ACTIVE',0,'2026-06-27 05:05:50.193886','trader_lei',NULL,NULL,'ç”¨æœ€å°‘çš„æ”¹åŠ¨æ¢æœ€å¤§çš„æ”¶ç›Šã€‚'),(41,'2026-06-27 05:05:50.343549','qiu.wizard@gg.local','$2a$10$HtfVbkM9Zp3nb3XEWPboWu4EPMob7fJ9g.CF9NR0mGa3E1XXTzSpu','BEGINNER','ACTIVE',0,'2026-06-27 05:05:50.343549','wizard_qiu',NULL,NULL,'TypeScript æ˜¯æˆ‘çš„æ³•æ–ã€‚'),(42,'2026-06-27 05:05:50.496683','bai.guard@gg.local','$2a$10$61uIXZjTwMHcWj94JyGrJecWcdQb0irLXvRCfcR.fmTolj9BoiH3C','BEGINNER','ACTIVE',0,'2026-06-27 05:05:50.496683','guard_bai',NULL,NULL,'å®ˆä½æµ‹è¯•è¦†ç›–çŽ‡ã€‚'),(43,'2026-06-27 05:05:50.650025','song.explorer@gg.local','$2a$10$SEPGa5i8FCuFLOCHquLKau09friHgUGveHEH0iNi4Beo/nBba.7xe','BEGINNER','ACTIVE',0,'2026-06-27 05:05:50.650025','explorer_song',NULL,NULL,'æ¯ä¸ªä»“åº“éƒ½æ˜¯ä¸€ç‰‡æ–°å¤§é™†ã€‚');
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
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `xp_transactions` WRITE;
/*!40000 ALTER TABLE `xp_transactions` DISABLE KEYS */;
INSERT INTO `xp_transactions` VALUES (1,50,'2026-06-07 06:03:45.376480','QUEST_COMPLETED',3,6),(2,50,'2026-06-07 06:50:58.338554','QUEST_COMPLETED',4,6),(3,80,'2026-06-27 05:05:59.397576','QUEST_COMPLETED',7,22),(4,140,'2026-06-27 05:06:01.263704','QUEST_COMPLETED',8,23),(5,40,'2026-06-27 05:06:02.473705','QUEST_COMPLETED',9,24),(6,160,'2026-06-27 05:06:03.651998','QUEST_COMPLETED',10,25),(7,260,'2026-06-27 05:06:04.795127','QUEST_COMPLETED',11,26),(8,90,'2026-06-27 05:06:06.584808','QUEST_COMPLETED',12,27),(9,130,'2026-06-27 05:06:08.240179','QUEST_COMPLETED',13,28),(10,100,'2026-06-27 05:06:09.514145','QUEST_COMPLETED',14,29),(11,45,'2026-06-27 05:06:10.866559','QUEST_COMPLETED',16,31),(12,150,'2026-06-27 05:06:12.580160','QUEST_COMPLETED',18,33),(13,240,'2026-06-27 05:06:14.257860','QUEST_COMPLETED',20,35);
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

