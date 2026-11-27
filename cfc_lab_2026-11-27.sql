-- MySQL dump 10.13  Distrib 8.4.11, for Linux (x86_64)
--
-- Host: localhost    Database: cfc_lab
-- ------------------------------------------------------
-- Server version	8.4.11

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

--
-- Current Database: `cfc_lab`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `cfc_lab` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `cfc_lab`;

--
-- Table structure for table `auth_sessions`
--

DROP TABLE IF EXISTS `auth_sessions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `auth_sessions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `token_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint NOT NULL,
  `expires_at` timestamp(6) NOT NULL,
  `revoked_at` timestamp(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_sessions_token_hash` (`token_hash`),
  KEY `idx_auth_sessions_user_id` (`user_id`),
  CONSTRAINT `fk_auth_sessions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `auth_sessions`
--

LOCK TABLES `auth_sessions` WRITE;
/*!40000 ALTER TABLE `auth_sessions` DISABLE KEYS */;
INSERT INTO `auth_sessions` VALUES (1,'5DplvJzwzSG3XuhvZogOCs--Nk4FpRoMIT5vWWkNnZg',1,'2026-12-12 09:01:21.848839',NULL),(2,'NDpY_cHRg_AReYIRXKpjnCHjZUTh-M1_ThcDlbjeULI',1,'2026-12-12 09:04:10.569059','2026-11-12 09:04:24.175876'),(3,'OM0JdxLU4VYn34K9rqsgP6DIsQhl7aCkbIcC6igiWx4',1,'2026-12-12 09:09:27.525470',NULL),(4,'j-ZOzpPGTOlndCHyPae_OwK2Joq2-Gtdq_tgsm9d1js',1,'2026-12-12 09:09:55.958757','2026-11-12 09:15:37.507654'),(5,'3zXoeeCE_cANZ0Y2JqsIYofbQM65Zrpyn-XhRPbqeqs',1,'2026-12-12 09:15:41.957225',NULL),(6,'cAi73Ahdd05K87Z4LpMvWMsUlRLKU8TBAagE_n-E0D4',1,'2026-12-12 09:18:16.037854',NULL),(7,'p86mKh9IdURmVJ27OYWBiQGjgLkswjP6uOgaeXS5xIc',1,'2026-12-12 09:44:24.059809',NULL),(8,'7ct2SxPBvgNnkSu7C0Eb7cqK0lVU3K_zquu7QFZ6Rms',1,'2026-12-13 00:44:00.801145',NULL),(9,'1ZriP8FMxGc5T5wRJmSNombJZOnKvQRYzMfa56QqFtQ',1,'2026-12-13 00:46:11.870738',NULL),(10,'VixpM0cLvbIyFgoAFUCridGv0GxKQ7uJ-Lv37m9TXec',1,'2026-12-13 01:30:03.986770',NULL),(11,'R12icFCqw8thIebfZ3nV5ugjnFFsqNKuUpBDOpukUUM',1,'2026-12-13 03:16:54.444364',NULL),(12,'9jou6vA8moWQcdG1OjyvcrYTIQo9OuFI4PrLdnHc6sU',1,'2026-12-13 03:23:14.129919',NULL),(13,'ucHeW6nkXKfB6i-B6P6N5QhoF_2m9KRDMzDwGGktTMY',1,'2026-12-13 03:36:03.091062',NULL),(14,'dL0h18kgv98gMhPtZqmo4EQm7n-axJxGwJh0gCiR3Vs',1,'2026-12-13 03:39:15.903794',NULL),(15,'SQFIFYS-c8x4AlAVfaVUO82f9IvSZ2bivtRrcntDlxI',1,'2026-12-13 06:13:57.800751',NULL),(16,'7DUOFJyWorqvecfT_OsvC0KuSDtnFBQlF042DxnvfjE',1,'2026-12-13 06:18:12.188273','2026-11-13 06:24:56.077059'),(17,'A9DIzp5_8UyWaMStwNJFibJf128DxtTJRoU1SE0xSsE',1,'2026-12-13 06:25:08.783241','2026-11-13 06:25:30.739842'),(18,'vw3_9UL9a0hJEUkaZYWNTgSEPW1ugUmllm-VK-TiX4I',3,'2026-12-13 06:25:37.984363','2026-11-13 06:27:21.851177'),(19,'KkGVEO4IAeuERFlBbhqhnvIYfkMANnmcFt5ofKwYDOk',1,'2026-12-13 06:27:24.342457',NULL);
/*!40000 ALTER TABLE `auth_sessions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `customer_notification_controls`
--

DROP TABLE IF EXISTS `customer_notification_controls`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customer_notification_controls` (
  `id` int NOT NULL,
  `paused` tinyint(1) NOT NULL DEFAULT '0',
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `customer_notification_controls`
--

LOCK TABLES `customer_notification_controls` WRITE;
/*!40000 ALTER TABLE `customer_notification_controls` DISABLE KEYS */;
INSERT INTO `customer_notification_controls` VALUES (1,0,'2026-11-23 06:51:27.181738');
/*!40000 ALTER TABLE `customer_notification_controls` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `customer_notification_operation_audit`
--

DROP TABLE IF EXISTS `customer_notification_operation_audit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customer_notification_operation_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `request_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `action` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `request_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `actor_user_id` bigint NOT NULL,
  `reason` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_customer_operation_request` (`request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `customer_notification_operation_audit`
--

LOCK TABLES `customer_notification_operation_audit` WRITE;
/*!40000 ALTER TABLE `customer_notification_operation_audit` DISABLE KEYS */;
/*!40000 ALTER TABLE `customer_notification_operation_audit` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `customer_notification_outbox`
--

DROP TABLE IF EXISTS `customer_notification_outbox`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customer_notification_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `receipt_id` bigint NOT NULL,
  `event_key` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `event_type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `provider_mode` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `session_key` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `app_id` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `oa_id` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `template_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `template_version` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `request_number` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `template_data` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reason_code` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `attempt_count` int NOT NULL DEFAULT '0',
  `next_attempt_at` datetime(6) NOT NULL,
  `lease_owner` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `lease_until` datetime(6) DEFAULT NULL,
  `retry_request_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `retry_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `retry_actor_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_customer_notification_event` (`event_key`),
  UNIQUE KEY `uk_customer_notification_receipt_event` (`receipt_id`,`event_type`),
  KEY `ix_customer_notification_due` (`status`,`next_attempt_at`),
  CONSTRAINT `fk_customer_outbox_receipt` FOREIGN KEY (`receipt_id`) REFERENCES `receipts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `customer_notification_outbox`
--

LOCK TABLES `customer_notification_outbox` WRITE;
/*!40000 ALTER TABLE `customer_notification_outbox` DISABLE KEYS */;
/*!40000 ALTER TABLE `customer_notification_outbox` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `customer_notification_test_budgets`
--

DROP TABLE IF EXISTS `customer_notification_test_budgets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customer_notification_test_budgets` (
  `session_key` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `used_calls` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`session_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `customer_notification_test_budgets`
--

LOCK TABLES `customer_notification_test_budgets` WRITE;
/*!40000 ALTER TABLE `customer_notification_test_budgets` DISABLE KEYS */;
/*!40000 ALTER TABLE `customer_notification_test_budgets` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `customers`
--

DROP TABLE IF EXISTS `customers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(160) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL,
  `updated_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_customers_phone` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=70 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `customers`
--

LOCK TABLES `customers` WRITE;
/*!40000 ALTER TABLE `customers` DISABLE KEYS */;
INSERT INTO `customers` VALUES (67,'Hộ Kinh Doanh Hưng Thị','0939740009',NULL,'40 Phan Văn trị, Phường Ninh Kiều, TP Cần Thơ','2026-11-19 03:41:30.731942','2026-11-19 03:41:30.731942'),(68,'Công ty TNHH MTV HỮU CƠ ĐẬM ĐẶC','0901234567',NULL,'Phụng hiệp','2026-11-23 08:57:12.724728','2026-11-23 08:57:12.724728'),(69,'Anh Long','0939351371',NULL,'Cần Thơ','2026-11-27 02:18:50.131842','2026-11-27 02:18:50.131842');
/*!40000 ALTER TABLE `customers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dossier_audit`
--

DROP TABLE IF EXISTS `dossier_audit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dossier_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actor_user_id` bigint NOT NULL,
  `action` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `object_id` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `detail` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_dossier_audit_created` (`created_at`),
  KEY `fk_dossier_audit_user` (`actor_user_id`),
  CONSTRAINT `fk_dossier_audit_user` FOREIGN KEY (`actor_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dossier_audit`
--

LOCK TABLES `dossier_audit` WRITE;
/*!40000 ALTER TABLE `dossier_audit` DISABLE KEYS */;
/*!40000 ALTER TABLE `dossier_audit` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dossier_export_entries`
--

DROP TABLE IF EXISTS `dossier_export_entries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dossier_export_entries` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `job_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `entry_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `location_id` bigint DEFAULT NULL,
  `relative_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `storage_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `size` bigint NOT NULL,
  `checksum` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `payload` mediumtext COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  KEY `idx_export_entry_job` (`job_id`,`id`),
  CONSTRAINT `fk_export_entry_job` FOREIGN KEY (`job_id`) REFERENCES `dossier_export_jobs` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dossier_export_entries`
--

LOCK TABLES `dossier_export_entries` WRITE;
/*!40000 ALTER TABLE `dossier_export_entries` DISABLE KEYS */;
/*!40000 ALTER TABLE `dossier_export_entries` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dossier_export_jobs`
--

DROP TABLE IF EXISTS `dossier_export_jobs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dossier_export_jobs` (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `request_id` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `actor_user_id` bigint NOT NULL,
  `scope_json` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(24) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'QUEUED',
  `cutoff_at` timestamp(6) NOT NULL,
  `snapshot_at` timestamp(6) NULL DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `finished_at` timestamp(6) NULL DEFAULT NULL,
  `expires_at` timestamp(6) NULL DEFAULT NULL,
  `lease_token` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `lease_until` timestamp(6) NULL DEFAULT NULL,
  `download_until` timestamp(6) NULL DEFAULT NULL,
  `attempt` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `total_files` bigint NOT NULL DEFAULT '0',
  `total_bytes` bigint NOT NULL DEFAULT '0',
  `processed_files` bigint NOT NULL DEFAULT '0',
  `processed_bytes` bigint NOT NULL DEFAULT '0',
  `error_code` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `allow_missing` tinyint(1) NOT NULL DEFAULT '0',
  `attempts` int NOT NULL DEFAULT '0',
  `warning_count` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dossier_export_request` (`actor_user_id`,`request_id`),
  KEY `idx_dossier_export_claim` (`status`,`created_at`),
  KEY `idx_dossier_export_expiry` (`status`,`expires_at`),
  CONSTRAINT `fk_dossier_export_actor` FOREIGN KEY (`actor_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dossier_export_jobs`
--

LOCK TABLES `dossier_export_jobs` WRITE;
/*!40000 ALTER TABLE `dossier_export_jobs` DISABLE KEYS */;
/*!40000 ALTER TABLE `dossier_export_jobs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dossier_export_parts`
--

DROP TABLE IF EXISTS `dossier_export_parts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dossier_export_parts` (
  `job_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `part_number` int NOT NULL,
  `relative_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `size` bigint NOT NULL,
  `checksum` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`job_id`,`part_number`),
  CONSTRAINT `fk_export_part_job` FOREIGN KEY (`job_id`) REFERENCES `dossier_export_jobs` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dossier_export_parts`
--

LOCK TABLES `dossier_export_parts` WRITE;
/*!40000 ALTER TABLE `dossier_export_parts` DISABLE KEYS */;
/*!40000 ALTER TABLE `dossier_export_parts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dossier_export_worker`
--

DROP TABLE IF EXISTS `dossier_export_worker`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dossier_export_worker` (
  `id` int NOT NULL,
  `lease_token` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `lease_until` timestamp(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dossier_export_worker`
--

LOCK TABLES `dossier_export_worker` WRITE;
/*!40000 ALTER TABLE `dossier_export_worker` DISABLE KEYS */;
INSERT INTO `dossier_export_worker` VALUES (1,NULL,NULL);
/*!40000 ALTER TABLE `dossier_export_worker` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `field_sampling_forms`
--

DROP TABLE IF EXISTS `field_sampling_forms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `field_sampling_forms` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `test_request_id` bigint NOT NULL,
  `form_number` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `form_date` date NOT NULL,
  `representative` varchar(160) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `representative_phone` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sample_collector` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `collector_phone` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sampling_start` timestamp(6) NULL DEFAULT NULL,
  `sampling_end` timestamp(6) NULL DEFAULT NULL,
  `sampling_location` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `growing_area` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `packing_facility` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `vehicle_number` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `trailer_number` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `container_number` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sample_condition` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `storage_condition` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sample_seal_number` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `seal_present` tinyint(1) NOT NULL DEFAULT '0',
  `sampling_method` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sample_quantity` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `field_notes` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp(6) NOT NULL,
  `updated_at` timestamp(6) NOT NULL,
  `storage_temperature` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_field_sampling_request` (`test_request_id`),
  UNIQUE KEY `uk_field_sampling_number` (`form_number`),
  CONSTRAINT `fk_field_sampling_request` FOREIGN KEY (`test_request_id`) REFERENCES `test_requests` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `field_sampling_forms`
--

LOCK TABLES `field_sampling_forms` WRITE;
/*!40000 ALTER TABLE `field_sampling_forms` DISABLE KEYS */;
INSERT INTO `field_sampling_forms` VALUES (2,3,'CFC26.00003','2026-11-23','Anh Lộc','0901234567','Nguyễn Bá Tân','0703637776','2026-11-23 03:00:00.000000','2026-11-23 04:15:00.000000','Công ty TNHH hữu cơ đậm đặc','HG001','PH001','Rf12345','Rt12345','Rc12345','Nguyên vẹn','Thường','A12345',1,'Tcvn','2',NULL,'2026-11-23 08:39:25.306746','2026-11-23 08:39:25.306967',NULL);
/*!40000 ALTER TABLE `field_sampling_forms` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `field_sampling_items`
--

DROP TABLE IF EXISTS `field_sampling_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `field_sampling_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `field_sampling_id` bigint NOT NULL,
  `row_order` int NOT NULL,
  `sample_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sampling_method` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `quantity` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `parameters` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `parameters_other` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_field_sampling_items_form` (`field_sampling_id`,`row_order`),
  CONSTRAINT `fk_field_sampling_items_form` FOREIGN KEY (`field_sampling_id`) REFERENCES `field_sampling_forms` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `field_sampling_items`
--

LOCK TABLES `field_sampling_items` WRITE;
/*!40000 ALTER TABLE `field_sampling_items` DISABLE KEYS */;
INSERT INTO `field_sampling_items` VALUES (2,2,1,'Mẫu 1','Tcvn','2','VANG_O,CD',NULL);
/*!40000 ALTER TABLE `field_sampling_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `script` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flyway_schema_history`
--

LOCK TABLES `flyway_schema_history` WRITE;
/*!40000 ALTER TABLE `flyway_schema_history` DISABLE KEYS */;
INSERT INTO `flyway_schema_history` VALUES (1,'1','existing-schema','BASELINE','existing-schema',NULL,'sa','2026-11-16 03:37:42',0,1),(2,'2','align phase1 phase3 schema','SQL','V2__align_phase1_phase3_schema.sql',631653531,'sa','2026-11-16 03:37:43',229,1),(3,'3','align intake workspace schema','SQL','V3__align_intake_workspace_schema.sql',-315541760,'sa','2026-11-16 03:37:43',235,1),(4,'4','align receipt form3 schema','SQL','V4__align_receipt_form3_schema.sql',419528868,'sa','2026-11-16 03:37:43',75,1),(5,'5','receiving query indexes','SQL','V5__receiving_query_indexes.sql',173862950,'sa','2026-11-16 07:39:42',42,1),(6,'6','result release drafts','SQL','V6__result_release_drafts.sql',646346000,'sa','2026-11-16 09:39:38',32,1),(7,'7','sample coding gate','SQL','V7__sample_coding_gate.sql',-794621821,'sa','2026-11-17 01:41:44',388,1),(8,'8','result release workflow','SQL','V8__result_release_workflow.sql',-2059706700,'sa','2026-11-17 01:41:45',244,1),(9,'9','result report content','SQL','V9__result_report_content.sql',1705353722,'sa','2026-11-17 02:25:21',141,1),(10,'10','result report file','SQL','V10__result_report_file.sql',-955333649,'sa','2026-11-17 06:19:37',31,1),(11,'11','test request sampling flow','SQL','V11__test_request_sampling_flow.sql',-1750617570,'sa','2026-11-19 01:02:53',234,1),(12,'12','test request counters','SQL','V12__test_request_counters.sql',1430670548,'sa','2026-11-19 06:39:33',25,1),(13,'13','sampling checklists','SQL','V13__sampling_checklists.sql',1705444539,'sa','2026-11-19 06:51:11',99,1),(14,'14','sync sampling form numbers','SQL','V14__sync_sampling_form_numbers.sql',-464632075,'sa','2026-11-19 09:04:58',5,1),(15,'15','receipt form3 alignment','SQL','V15__receipt_form3_alignment.sql',-58172295,'sa','2026-11-19 09:32:46',88,1),(16,'16','receipt request number unique','SQL','V16__receipt_request_number_unique.sql',-588910013,'sa','2026-11-20 01:48:36',97,1),(17,'17','test methods','SQL','V17__test_methods.sql',1793681465,'sa','2026-11-20 02:01:03',30,1),(18,'18','canonical request numbers','SQL','V18__canonical_request_numbers.sql',-272114705,'sa','2026-11-20 02:26:40',8,1),(19,'19','staff notification actor','SQL','V19__staff_notification_actor.sql',-697799910,'sa','2026-11-20 07:15:37',115,1),(20,'20','customer notification test outbox','SQL','V20__customer_notification_test_outbox.sql',-157498287,'sa','2026-11-23 06:51:27',99,1),(21,'21','zalo delivery tracking and operations','SQL','V21__zalo_delivery_tracking_and_operations.sql',-2140853750,'sa','2026-11-23 06:51:27',177,1),(22,'22','sample weight','SQL','V22__sample_weight.sql',-406223581,'sa','2026-11-25 00:37:19',25,1),(23,'23','staff notification routing','SQL','V23__staff_notification_routing.sql',1031168287,'sa','2026-11-25 00:58:53',13,1),(24,'24','dossier storage locations','SQL','V24__dossier_storage_locations.sql',761877977,'sa','2026-11-25 03:29:05',251,1),(25,'25','dossier export queue','SQL','V25__dossier_export_queue.sql',-274837536,'sa','2026-11-25 03:29:06',82,1),(26,'26','request numbers five digits','SQL','V26__request_numbers_five_digits.sql',1687371931,'sa','2026-11-27 03:20:25',16,1),(27,'27','receipt edit logs','SQL','V27__receipt_edit_logs.sql',1856746847,'sa','2026-11-27 03:20:25',24,1),(28,'28','transfer assignees','SQL','V28__transfer_assignees.sql',-1283729496,'sa','2026-11-27 03:53:44',45,1);
/*!40000 ALTER TABLE `flyway_schema_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notification_delivery_attempts`
--

DROP TABLE IF EXISTS `notification_delivery_attempts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_delivery_attempts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `outbox_id` bigint NOT NULL,
  `attempt_number` int NOT NULL,
  `tracking_id` varchar(48) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `session_key` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `provider_mode` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `provider_code` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `provider_message_id` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `request_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `retry_request_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `actor_user_id` bigint DEFAULT NULL,
  `reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `delivered_at` datetime(6) DEFAULT NULL,
  `delivery_source` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status_check_count` int NOT NULL DEFAULT '0',
  `next_status_check_at` datetime(6) DEFAULT NULL,
  `last_status_check_at` datetime(6) DEFAULT NULL,
  `last_status_code` varchar(60) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_customer_delivery_tracking` (`tracking_id`),
  UNIQUE KEY `uk_customer_delivery_attempt` (`outbox_id`,`attempt_number`),
  UNIQUE KEY `uk_customer_delivery_retry` (`outbox_id`,`retry_request_id`),
  KEY `ix_customer_status_checks` (`status`,`next_status_check_at`),
  CONSTRAINT `fk_customer_attempt_outbox` FOREIGN KEY (`outbox_id`) REFERENCES `customer_notification_outbox` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notification_delivery_attempts`
--

LOCK TABLES `notification_delivery_attempts` WRITE;
/*!40000 ALTER TABLE `notification_delivery_attempts` DISABLE KEYS */;
/*!40000 ALTER TABLE `notification_delivery_attempts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `public_lookup_audit`
--

DROP TABLE IF EXISTS `public_lookup_audit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `public_lookup_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `release_id` bigint DEFAULT NULL,
  `action` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `outcome` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `client_hash` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_public_lookup_audit_created` (`created_at`),
  KEY `idx_public_lookup_audit_release` (`release_id`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `public_lookup_audit`
--

LOCK TABLES `public_lookup_audit` WRITE;
/*!40000 ALTER TABLE `public_lookup_audit` DISABLE KEYS */;
INSERT INTO `public_lookup_audit` VALUES (27,34,'LOOKUP','NOT_FOUND','b4a56056d596b4f92d37a48329f21a089876cf570ecbff2f1823759467406e2e','2026-11-23 01:35:47.388788'),(28,34,'LOOKUP','OK','b4a56056d596b4f92d37a48329f21a089876cf570ecbff2f1823759467406e2e','2026-11-23 01:36:23.748235'),(29,34,'DOWNLOAD','OK','b4a56056d596b4f92d37a48329f21a089876cf570ecbff2f1823759467406e2e','2026-11-23 01:36:44.680104'),(30,34,'LOOKUP','OK','b4a56056d596b4f92d37a48329f21a089876cf570ecbff2f1823759467406e2e','2026-11-23 02:08:05.691702'),(31,34,'DOWNLOAD','OK','b4a56056d596b4f92d37a48329f21a089876cf570ecbff2f1823759467406e2e','2026-11-23 02:08:08.697250'),(32,34,'LOOKUP','OK','b4a56056d596b4f92d37a48329f21a089876cf570ecbff2f1823759467406e2e','2026-11-23 09:22:10.419267'),(33,34,'DOWNLOAD','OK','b4a56056d596b4f92d37a48329f21a089876cf570ecbff2f1823759467406e2e','2026-11-23 09:22:18.259736'),(34,34,'LOOKUP','OK','b4a56056d596b4f92d37a48329f21a089876cf570ecbff2f1823759467406e2e','2026-11-27 01:47:46.450751'),(35,34,'DOWNLOAD','OK','b4a56056d596b4f92d37a48329f21a089876cf570ecbff2f1823759467406e2e','2026-11-27 01:47:49.909990');
/*!40000 ALTER TABLE `public_lookup_audit` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `receipt_attachments`
--

DROP TABLE IF EXISTS `receipt_attachments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `receipt_attachments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `receipt_id` bigint NOT NULL,
  `category` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OTHER',
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `original_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content_type` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `storage_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `size` bigint NOT NULL,
  `checksum` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `captured_at` timestamp(6) NULL DEFAULT NULL,
  `uploaded_at` timestamp(6) NOT NULL,
  `uploaded_by_user_id` bigint DEFAULT NULL,
  `storage_location_id` bigint NOT NULL DEFAULT '1',
  `relative_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_receipt_attachments_receipt` (`receipt_id`),
  KEY `idx_receipt_attachments_category` (`receipt_id`,`category`),
  KEY `fk_receipt_attachments_uploaded_by` (`uploaded_by_user_id`),
  KEY `fk_receipt_attachment_storage` (`storage_location_id`),
  CONSTRAINT `fk_receipt_attachment_storage` FOREIGN KEY (`storage_location_id`) REFERENCES `storage_locations` (`id`),
  CONSTRAINT `fk_receipt_attachments_receipt` FOREIGN KEY (`receipt_id`) REFERENCES `receipts` (`id`),
  CONSTRAINT `fk_receipt_attachments_uploaded_by` FOREIGN KEY (`uploaded_by_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `receipt_attachments`
--

LOCK TABLES `receipt_attachments` WRITE;
/*!40000 ALTER TABLE `receipt_attachments` DISABLE KEYS */;
INSERT INTO `receipt_attachments` VALUES (31,71,'TRANSFER_EVIDENCE','Bằng chứng bàn giao mẫu','17950599195248505414613613420217.jpg','image/jpeg','d6d9dcc0-42c3-4bfc-b249-16a38db9e4ef-17950599195248505414613613420217.jpg',3121041,'fe54b8f2d899c9207f0ca362a0dbb5105cc9e04448999f060c0588e71f9392fd','2026-11-19 03:45:22.420000','2026-11-19 03:45:25.355025',3,1,NULL),(32,74,'SAMPLE_PHOTO','ok','Screenshot from 2026-11-17 08-45-08.png','image/png','87290745-814d-41c5-9e2f-43e53f74bc16-Screenshot from 2026-11-17 08-45-08.png',67629,'1647875271ad1275ae384047d22fb818cb8a458a7a7a4cf917405a9f796e4aa2','2026-11-17 01:45:09.090000','2026-11-20 01:38:54.109565',1,1,NULL),(33,76,'SAMPLE_PHOTO','Ảnh mẫu khi tiếp nhận','17954240213968194781887209573915.jpg','image/jpeg','fc4e0362-367e-48a2-abed-7f34fee1bba4-17954240213968194781887209573915.jpg',4121754,'9be82654e10c657ef6ff9bccfcce9bc61f3edbe543880e737b4a8d451943f5bf','2026-11-23 08:54:08.931000','2026-11-23 08:57:13.401751',172,1,NULL),(34,76,'LOT_PHOTO','Ảnh lô hàng khi tiếp nhận','17954240518735098544928175123930.jpg','image/jpeg','18332339-968b-4211-9567-961e586ea7ce-17954240518735098544928175123930.jpg',4952890,'e6478f44ba42282936d85b017b6cab6a537d6abb3c19f6bb41d666cb28eb2dc9','2026-11-23 08:55:01.991000','2026-11-23 08:57:13.445298',172,1,NULL),(35,76,'FORM_3','Phiếu tiếp nhận số 3','17954238847632644418078624438028.jpg','image/jpeg','47aac015-4d9a-41d3-b19b-a1e952675cb0-17954238847632644418078624438028.jpg',4035027,'84f7be3dfee60cff72632902f9ddb7ab0f6ead93205b2292123deefdbb0ed76b','2026-11-23 08:53:34.479000','2026-11-23 08:57:13.461676',172,1,NULL),(36,77,'SAMPLE_PHOTO',NULL,'image.jpg','image/jpeg','13dd3520-1f75-40de-ae95-52dd5dc97fbb.jpg',2935285,'ad32fa469895112eab238d4483fdf39c6ccaf7339317a10967c5149eed97abe3','2026-11-27 02:00:54.000000','2026-11-27 02:01:01.032055',170,1,'13dd3520-1f75-40de-ae95-52dd5dc97fbb.jpg'),(37,78,'SAMPLE_PHOTO','Ảnh mẫu khi tiếp nhận','image.jpg','image/jpeg','c3450a33-3e74-41cf-b26f-d1a58155b3fe.jpg',1890596,'aa60eead4be18c4d574ccd3ab42efcbaa76ce2001f3ef036beed1bd0ab3c78fb','2026-11-27 02:18:35.000000','2026-11-27 02:18:50.294777',170,1,'c3450a33-3e74-41cf-b26f-d1a58155b3fe.jpg'),(38,78,'SAMPLE_PHOTO','Ảnh mẫu khi tiếp nhận','image.jpg','image/jpeg','47b5c792-61cc-462c-8f83-cee497072600.jpg',1913363,'2e0186dba08b9e54e59ae1e88b6d0b1f1ce85c83e6c55135cec55025c5dd0bad','2026-11-27 02:18:45.000000','2026-11-27 02:18:50.344548',170,1,'47b5c792-61cc-462c-8f83-cee497072600.jpg');
/*!40000 ALTER TABLE `receipt_attachments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `receipt_edit_logs`
--

DROP TABLE IF EXISTS `receipt_edit_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `receipt_edit_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `receipt_id` bigint NOT NULL,
  `edited_by_user_id` bigint NOT NULL,
  `edited_at` timestamp(6) NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `after_transfer` tinyint(1) NOT NULL DEFAULT '0',
  `summary` text COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_receipt_edit_logs_receipt` (`receipt_id`,`id`),
  KEY `fk_receipt_edit_logs_user` (`edited_by_user_id`),
  CONSTRAINT `fk_receipt_edit_logs_receipt` FOREIGN KEY (`receipt_id`) REFERENCES `receipts` (`id`),
  CONSTRAINT `fk_receipt_edit_logs_user` FOREIGN KEY (`edited_by_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `receipt_edit_logs`
--

LOCK TABLES `receipt_edit_logs` WRITE;
/*!40000 ALTER TABLE `receipt_edit_logs` DISABLE KEYS */;
INSERT INTO `receipt_edit_logs` VALUES (1,78,1,'2026-11-27 03:23:48.601622','Sửa tên mẫu',1,'Mẫu ĐT001-SR27112026-002 · tên: Mẫu 1 → Mẫu 2'),(2,78,1,'2026-11-27 06:20:28.119583','sửa',1,'Mẫu ĐT001-SR27112026-002 · chỉ tiêu: Khác: Ruột + vỏ → Cadimi (Cd), Khác: Ruột + vỏ'),(3,78,1,'2026-11-27 06:23:34.442594','Sửa chỉ tiêu',1,'Mẫu ĐT001-SR27112026-001 · chỉ tiêu: Cadimi (Cd), Khác: Ruột + vỏ → Khác: Cadimi (Cd) Ruột + vỏ\nMẫu ĐT001-SR27112026-002 · chỉ tiêu: Cadimi (Cd), Khác: Ruột + vỏ → Khác: Cadimi (Cd) Ruột + vỏ');
/*!40000 ALTER TABLE `receipt_edit_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `receipt_notification_consents`
--

DROP TABLE IF EXISTS `receipt_notification_consents`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `receipt_notification_consents` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `receipt_id` bigint NOT NULL,
  `recipient_phone` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `consent_accepted` tinyint(1) NOT NULL,
  `consent_version` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `actor_user_id` bigint NOT NULL,
  `recorded_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_receipt_notification_consent` (`receipt_id`),
  KEY `fk_customer_consent_actor` (`actor_user_id`),
  CONSTRAINT `fk_customer_consent_actor` FOREIGN KEY (`actor_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_customer_consent_receipt` FOREIGN KEY (`receipt_id`) REFERENCES `receipts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `receipt_notification_consents`
--

LOCK TABLES `receipt_notification_consents` WRITE;
/*!40000 ALTER TABLE `receipt_notification_consents` DISABLE KEYS */;
/*!40000 ALTER TABLE `receipt_notification_consents` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `receipts`
--

DROP TABLE IF EXISTS `receipts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `receipts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `receipt_code` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `customer_id` bigint NOT NULL,
  `received_by_user_id` bigint NOT NULL,
  `delivered_by` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `received_at` timestamp(6) NOT NULL,
  `sampled_by` varchar(160) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sampled_at` timestamp(6) NULL DEFAULT NULL,
  `growing_area` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `packing_facility` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `vehicle_number` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `trailer_number` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `container_number` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sample_name` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `quantity` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `packaging` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `seal_number` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `container_seal_number` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `seal_intact` tinyint(1) NOT NULL,
  `sample_condition` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `condition_detail` text COLLATE utf8mb4_unicode_ci,
  `transport_info` text COLLATE utf8mb4_unicode_ci,
  `received_temperature` double DEFAULT NULL,
  `storage_condition` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `storage_after_receipt` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `requested_parameters` text COLLATE utf8mb4_unicode_ci,
  `expected_date` date DEFAULT NULL,
  `special_request` text COLLATE utf8mb4_unicode_ci,
  `acceptance_decision` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `acceptance_reason` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp(6) NOT NULL,
  `updated_at` timestamp(6) NOT NULL,
  `request_number` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `request_number_unique` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `test_request_id` bigint DEFAULT NULL,
  `field_sampling_id` bigint DEFAULT NULL,
  `seal_status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `archive_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_receipts_code` (`receipt_code`),
  UNIQUE KEY `uk_receipts_request_number_unique` (`request_number_unique`),
  KEY `idx_receipts_customer` (`customer_id`),
  KEY `idx_receipts_created` (`created_at`),
  KEY `idx_receipts_received_at` (`received_at`),
  KEY `idx_receipts_status_received` (`status`,`received_at`),
  KEY `idx_receipts_received_by_received` (`received_by_user_id`,`received_at`),
  KEY `idx_receipts_request_number` (`request_number`),
  KEY `idx_receipts_test_request` (`test_request_id`),
  KEY `idx_receipts_field_sampling` (`field_sampling_id`),
  CONSTRAINT `fk_receipts_customer` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`),
  CONSTRAINT `fk_receipts_field_sampling` FOREIGN KEY (`field_sampling_id`) REFERENCES `field_sampling_forms` (`id`),
  CONSTRAINT `fk_receipts_test_request` FOREIGN KEY (`test_request_id`) REFERENCES `test_requests` (`id`),
  CONSTRAINT `fk_receipts_user` FOREIGN KEY (`received_by_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=79 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `receipts`
--

LOCK TABLES `receipts` WRITE;
/*!40000 ALTER TABLE `receipts` DISABLE KEYS */;
INSERT INTO `receipts` VALUES (71,'TN-20261119-94F6AF',67,3,'Nguyễn Bá Tân','2026-11-19 03:41:30.732575','Trân Thị Lệ Thuý','2026-11-16 03:30:00.000000','CT001','00002','29E-37437','89R.01622','CRLU1135207','A72604937','01/1,8KG','Túi PE','A72604937','11111',1,'Nguyên vẹn','','65N.08620',NULL,'Thường',NULL,'Cadimi, Vàng O, Cadimi (Cd), Auramine O (Vàng ô)','2026-11-18','','ACCEPTED','','TRANSFERRED','2026-11-19 03:41:30.732637','2026-11-19 03:52:40.971390','CFC26.00001','CFC26.00001',1,NULL,'INTACT',NULL),(74,'TN-20261120-9A22F4',67,1,'Nguyễn Bá Tân','2026-11-20 01:37:15.615281',NULL,NULL,'00002','Cần Thơ','01231512431','1230120312','12312','A72604937','','Túi PE','12312','12312',1,'Nguyên vẹn','','12312',NULL,'Thường',NULL,'Cadimi (Cd), Auramine O (Vàng ô)','2026-11-18','','ACCEPTED','','RECEIVED','2026-11-20 01:37:15.615414','2026-11-20 01:37:15.615414','CFC26.00001',NULL,NULL,NULL,'INTACT',NULL),(75,'TN-20261120-1F174A',67,3,'Nguyễn Bá Tân','2026-11-20 01:53:36.293323','Nguyễn Trung Thuận','2026-11-20 01:52:00.000000','00002','312321','012312312','12312','12312','A72604937','1/20kg','Túi PE','12321','123',1,'Nguyên vẹn','','1',NULL,'Thường','tủ mát 20','Cadimi (Cd), Auramine O (Vỏ)','2026-11-18','','ACCEPTED','','TRANSFERRED','2026-11-20 01:53:36.293485','2026-11-20 02:09:15.679308','CFC26.00002','CFC26.00002',NULL,NULL,'INTACT',NULL),(76,'TN-20261123-30378D',68,172,'Nguyễn Bá Tân','2026-11-23 08:57:12.742975','Nguyễn Thuý','2026-11-23 04:15:00.000000','HG001','PH001','Rf12345','Rt12345','Rc12345','Mẫu 1','2','Túi PE','A12345','A12345',1,'Nguyên vẹn','Nguyên vẹn','',NULL,'Thường',NULL,'Cadimi (Cd), Auramine O (Vỏ), Auramine O (Ruột)','2026-11-25','','ACCEPTED','','TRANSFERRED','2026-11-23 08:57:12.743056','2026-11-23 09:09:30.512101','CFC26.00003','CFC26.00003',3,2,'INTACT',NULL),(77,'TN-20261127-6EE0C9',68,170,'Anh Long','2026-11-27 01:58:44.039629','Thuý','2026-11-27 00:30:00.000000','HG001','PH001','Rf12345','Rt12345','Rc12345','Đồng Tháp 01','1 trái','Túi PE','A12345',NULL,1,'Nguyên vẹn','Nguyên vẹn','',NULL,'Thường',NULL,'Cadimi (Cd)','2026-11-29','','ACCEPTED','','RECEIVED','2026-11-27 01:58:44.039736','2026-11-27 01:58:44.039736','CFC26.00004','CFC26.00004',NULL,NULL,'INTACT',NULL),(78,'TN-20261127-3566FC',69,170,'Anh Long','2026-11-27 02:18:50.141388','Trần Thị Lệ Thuý','2026-11-27 00:30:00.000000','Đồng Tháp',NULL,NULL,NULL,NULL,'Mẫu 1','1','Túi PE','Đồng Tháp T01',NULL,1,'Nguyên vẹn','Nguyên vẹn',NULL,NULL,'Thường',NULL,'Khác: Cadimi (Cd) Ruột + vỏ','2026-11-25',NULL,'ACCEPTED','','TRANSFERRED','2026-11-27 02:18:50.141599','2026-11-27 06:23:34.442557','CFC26.00005','CFC26.00005',NULL,NULL,'INTACT',NULL);
/*!40000 ALTER TABLE `receipts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `result_number_counters`
--

DROP TABLE IF EXISTS `result_number_counters`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `result_number_counters` (
  `prefix` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `last_sequence` int NOT NULL,
  PRIMARY KEY (`prefix`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `result_number_counters`
--

LOCK TABLES `result_number_counters` WRITE;
/*!40000 ALTER TABLE `result_number_counters` DISABLE KEYS */;
INSERT INTO `result_number_counters` VALUES ('A72604937112026',1),('VO112026',1);
/*!40000 ALTER TABLE `result_number_counters` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `result_release_events`
--

DROP TABLE IF EXISTS `result_release_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `result_release_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `release_id` bigint NOT NULL,
  `event_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `from_status` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `to_status` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `actor_user_id` bigint NOT NULL,
  `created_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_result_release_events_release` (`release_id`,`id`),
  KEY `fk_result_release_events_actor` (`actor_user_id`),
  CONSTRAINT `fk_result_release_events_actor` FOREIGN KEY (`actor_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_result_release_events_release` FOREIGN KEY (`release_id`) REFERENCES `result_releases` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=120 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `result_release_events`
--

LOCK TABLES `result_release_events` WRITE;
/*!40000 ALTER TABLE `result_release_events` DISABLE KEYS */;
INSERT INTO `result_release_events` VALUES (113,34,'CREATED',NULL,'DRAFT',NULL,4,'2026-11-20 07:49:34.695386'),(114,34,'REPORT_UPDATED','DRAFT','DRAFT','2 chỉ tiêu',4,'2026-11-20 07:49:34.713439'),(115,34,'SUBMITTED','DRAFT','SUBMITTED',NULL,4,'2026-11-20 07:50:12.126961'),(116,34,'PUBLISHED','SUBMITTED','PUBLISHED','A72604937112026-001',1,'2026-11-20 07:50:31.670269'),(117,34,'REPORT_REGENERATED','PUBLISHED','PUBLISHED','Tạo lại PDF từ mẫu Word',1,'2026-11-20 08:19:54.802361'),(118,34,'REPORT_REGENERATED','PUBLISHED','PUBLISHED','Tạo lại PDF từ mẫu Word',1,'2026-11-23 01:35:15.429831'),(119,34,'REPORT_REGENERATED','PUBLISHED','PUBLISHED','Tạo lại PDF từ mẫu Word',1,'2026-11-23 02:07:41.360125');
/*!40000 ALTER TABLE `result_release_events` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `result_release_files`
--

DROP TABLE IF EXISTS `result_release_files`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `result_release_files` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `release_id` bigint NOT NULL,
  `original_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content_type` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `extension` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `storage_name` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `size` bigint NOT NULL,
  `checksum` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `public_download` tinyint(1) NOT NULL DEFAULT '0',
  `uploaded_by_user_id` bigint NOT NULL,
  `uploaded_at` timestamp(6) NOT NULL,
  `removed_at` timestamp(6) NULL DEFAULT NULL,
  `removed_by_user_id` bigint DEFAULT NULL,
  `kind` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'UPLOAD',
  `storage_location_id` bigint NOT NULL DEFAULT '2',
  `relative_path` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_result_release_file_storage` (`storage_name`),
  KEY `idx_result_release_files_release` (`release_id`,`removed_at`),
  KEY `fk_result_release_files_uploader` (`uploaded_by_user_id`),
  KEY `fk_result_release_files_remover` (`removed_by_user_id`),
  KEY `fk_result_file_storage` (`storage_location_id`),
  CONSTRAINT `fk_result_file_storage` FOREIGN KEY (`storage_location_id`) REFERENCES `storage_locations` (`id`),
  CONSTRAINT `fk_result_release_files_release` FOREIGN KEY (`release_id`) REFERENCES `result_releases` (`id`),
  CONSTRAINT `fk_result_release_files_remover` FOREIGN KEY (`removed_by_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_result_release_files_uploader` FOREIGN KEY (`uploaded_by_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=47 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `result_release_files`
--

LOCK TABLES `result_release_files` WRITE;
/*!40000 ALTER TABLE `result_release_files` DISABLE KEYS */;
INSERT INTO `result_release_files` VALUES (43,34,'KQKN-A72604937112026-001.pdf','application/pdf','pdf','fb094c26d8da46f4ad187209f886a2ac.pdf',291933,'c6f377afbeed7741a2966f1cc1957a2ae417de933adc76d33d021f2b048f882c',0,1,'2026-11-20 07:50:31.666619','2026-11-20 08:19:54.775278',1,'GENERATED_REPORT',2,NULL),(44,34,'KQKN-A72604937112026-001.pdf','application/pdf','pdf','6a9503fb8e2d4b5e80ca16f54dec561e.pdf',210316,'cb303c335025176f0c414a5d73713e238209542cdef8f3886621330f26e2d350',0,1,'2026-11-20 08:19:54.779510','2026-11-23 01:35:15.391671',1,'GENERATED_REPORT',2,NULL),(45,34,'KQKN-CFC26.0002.pdf','application/pdf','pdf','c0e01b905a65420fa28594303c0192ed.pdf',210322,'045b54a82580548888a683bc2760e7cf2a8a0cb7615f44e3fe1cc6b526a736f4',0,1,'2026-11-23 01:35:15.397011','2026-11-23 02:07:41.327476',1,'GENERATED_REPORT',2,NULL),(46,34,'KQKN-CFC26.0002.pdf','application/pdf','pdf','278cfdafdb0643d1b80428b78c10ba26.pdf',203160,'be9b4e669be9f37cd6194222422d352bb3c31f4c0acbfdf23e1ac9ab4b736d45',1,1,'2026-11-23 02:07:41.331463',NULL,NULL,'GENERATED_REPORT',2,NULL);
/*!40000 ALTER TABLE `result_release_files` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `result_release_items`
--

DROP TABLE IF EXISTS `result_release_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `result_release_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `release_id` bigint NOT NULL,
  `row_order` int NOT NULL,
  `parameter_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `unit` varchar(60) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `method` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `result_value` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `note` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_result_release_items_release` (`release_id`,`row_order`),
  CONSTRAINT `fk_result_release_items_release` FOREIGN KEY (`release_id`) REFERENCES `result_releases` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `result_release_items`
--

LOCK TABLES `result_release_items` WRITE;
/*!40000 ALTER TABLE `result_release_items` DISABLE KEYS */;
INSERT INTO `result_release_items` VALUES (25,34,1,'Cadimi (Cd)','µg/kg','CFC-TN-17-2026','KPH','LOQ = 1,5 µg/kg'),(26,34,2,'Auramine O (Vỏ)','µg/kg','CFC-TN-16-2026','< LOQ','LOQ = 30 µg/kg');
/*!40000 ALTER TABLE `result_release_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `result_releases`
--

DROP TABLE IF EXISTS `result_releases`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `result_releases` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `testing_record_id` bigint NOT NULL,
  `version` int NOT NULL,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_by_user_id` bigint NOT NULL,
  `created_at` timestamp(6) NOT NULL,
  `updated_at` timestamp(6) NOT NULL,
  `lookup_code` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `result_number` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `submitted_at` timestamp(6) NULL DEFAULT NULL,
  `submitted_by_user_id` bigint DEFAULT NULL,
  `decided_at` timestamp(6) NULL DEFAULT NULL,
  `decided_by_user_id` bigint DEFAULT NULL,
  `decision_note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `published_at` timestamp(6) NULL DEFAULT NULL,
  `revoked_at` timestamp(6) NULL DEFAULT NULL,
  `revoked_by_user_id` bigint DEFAULT NULL,
  `revoke_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `published_marker` tinyint GENERATED ALWAYS AS (if((`status` = _utf8mb4'PUBLISHED'),1,NULL)) STORED,
  `quality_head_name` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `director_name` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `document_number` varchar(60) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_result_release_record_version` (`testing_record_id`,`version`),
  UNIQUE KEY `uk_result_release_lookup_code` (`lookup_code`),
  UNIQUE KEY `uk_result_release_number` (`result_number`),
  UNIQUE KEY `uk_result_release_one_published` (`testing_record_id`,`published_marker`),
  KEY `idx_result_release_status_created` (`status`,`created_at`),
  KEY `fk_result_release_creator` (`created_by_user_id`),
  KEY `fk_result_release_submitter` (`submitted_by_user_id`),
  KEY `fk_result_release_decider` (`decided_by_user_id`),
  KEY `fk_result_release_revoker` (`revoked_by_user_id`),
  CONSTRAINT `fk_result_release_creator` FOREIGN KEY (`created_by_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_result_release_decider` FOREIGN KEY (`decided_by_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_result_release_revoker` FOREIGN KEY (`revoked_by_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_result_release_submitter` FOREIGN KEY (`submitted_by_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_result_release_testing_record` FOREIGN KEY (`testing_record_id`) REFERENCES `testing_records` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `result_releases`
--

LOCK TABLES `result_releases` WRITE;
/*!40000 ALTER TABLE `result_releases` DISABLE KEYS */;
INSERT INTO `result_releases` (`id`, `testing_record_id`, `version`, `status`, `created_by_user_id`, `created_at`, `updated_at`, `lookup_code`, `result_number`, `submitted_at`, `submitted_by_user_id`, `decided_at`, `decided_by_user_id`, `decision_note`, `published_at`, `revoked_at`, `revoked_by_user_id`, `revoke_reason`, `quality_head_name`, `director_name`, `document_number`) VALUES (34,36,1,'PUBLISHED',4,'2026-11-20 07:49:34.692513','2026-11-20 07:50:31.384545','JBSEGPJ3LMRJBSXC','CFC26.00002','2026-11-20 07:50:12.126948',4,'2026-11-20 07:50:31.384544',1,NULL,'2026-11-20 07:50:31.384544',NULL,NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `result_releases` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sample_code_counters`
--

DROP TABLE IF EXISTS `sample_code_counters`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sample_code_counters` (
  `counter_date` date NOT NULL,
  `last_sequence` int NOT NULL,
  PRIMARY KEY (`counter_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sample_code_counters`
--

LOCK TABLES `sample_code_counters` WRITE;
/*!40000 ALTER TABLE `sample_code_counters` DISABLE KEYS */;
INSERT INTO `sample_code_counters` VALUES ('2026-11-12',1),('2026-11-16',3),('2026-11-17',6),('2026-11-18',1),('2026-11-19',6),('2026-11-20',2),('2026-11-23',1),('2026-11-27',4);
/*!40000 ALTER TABLE `sample_code_counters` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sample_code_events`
--

DROP TABLE IF EXISTS `sample_code_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sample_code_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sample_id` bigint NOT NULL,
  `event_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `old_code` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `new_code` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `actor_user_id` bigint NOT NULL,
  `created_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_sample_code_events_sample` (`sample_id`,`id`),
  KEY `fk_sample_code_events_actor` (`actor_user_id`),
  CONSTRAINT `fk_sample_code_events_actor` FOREIGN KEY (`actor_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_sample_code_events_sample` FOREIGN KEY (`sample_id`) REFERENCES `samples` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sample_code_events`
--

LOCK TABLES `sample_code_events` WRITE;
/*!40000 ALTER TABLE `sample_code_events` DISABLE KEYS */;
/*!40000 ALTER TABLE `sample_code_events` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sample_transfer_items`
--

DROP TABLE IF EXISTS `sample_transfer_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sample_transfer_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `transfer_id` bigint NOT NULL,
  `sample_id` bigint NOT NULL,
  `sample_name` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sample_code` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `weight` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `quantity` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sample_condition` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `requested_parameters` text COLLATE utf8mb4_unicode_ci,
  `test_method` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_sample_transfer_items_transfer` (`transfer_id`),
  KEY `idx_sample_transfer_items_sample` (`sample_id`),
  CONSTRAINT `fk_sample_transfer_items_sample` FOREIGN KEY (`sample_id`) REFERENCES `samples` (`id`),
  CONSTRAINT `fk_sample_transfer_items_transfer` FOREIGN KEY (`transfer_id`) REFERENCES `sample_transfers` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=79 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sample_transfer_items`
--

LOCK TABLES `sample_transfer_items` WRITE;
/*!40000 ALTER TABLE `sample_transfer_items` DISABLE KEYS */;
INSERT INTO `sample_transfer_items` VALUES (72,70,93,'A72604937','CT001-SR-16112026-010','01/1,8KG','01/1,8KG','Nguyên vẹn','Cadimi, Vàng O, Cadimi (Cd), Auramine O (Vàng ô)','CFC-TN016, CFC-TN017'),(73,71,99,'A72604937','CT0000000156','1/20kg','1/20kg','Nguyên vẹn','Cadimi (Cd), Auramine O (Vỏ)','CFC-TN-17-2026, CFC-TN-16-2026'),(74,72,100,'Mẫu 1','CT001-SR0001-003','2','2','Nguyên vẹn','Cadimi (Cd), Auramine O (Vỏ), Auramine O (Ruột)','CFC-TN-16-2026, CFC-TN-17-2026'),(75,73,103,'Mẫu 1','ĐT001-SR27112026-001','2,4kg','1','Nguyên vẹn','Khác: Cadimi (Cd) Ruột + vỏ','CFC-TN-17-2026'),(76,73,104,'Mẫu 2','ĐT001-SR27112026-002','1,6kg','1','Nguyên vẹn','Khác: Cadimi (Cd) Ruột + vỏ','CFC-TN-17-2026'),(77,74,101,'Đồng Tháp 01','CT001-SR-27112026-001','2,4kg','1 trái','Nguyên vẹn','Cadimi (Cd)','CFC-TN-17-2026'),(78,74,102,'Đồng Tháp 02','CT001-SR-27112026-002','1,6kg','1 trái','Nguyên vẹn','Cadimi (Cd)','CFC-TN-17-2026');
/*!40000 ALTER TABLE `sample_transfer_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sample_transfers`
--

DROP TABLE IF EXISTS `sample_transfers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sample_transfers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `transfer_code` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `receipt_id` bigint NOT NULL,
  `delivered_by_user_id` bigint NOT NULL,
  `received_by_user_id` bigint DEFAULT NULL,
  `receiving_department` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'KCS/PTN',
  `transfer_date` date NOT NULL,
  `results_due_date` date NOT NULL,
  `return_to_customer` tinyint(1) NOT NULL DEFAULT '0',
  `customer_return_date` date DEFAULT NULL,
  `changes_note` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `receiving_note` text COLLATE utf8mb4_unicode_ci,
  `lab_opinion` text COLLATE utf8mb4_unicode_ci,
  `lab_retain` tinyint(1) NOT NULL DEFAULT '0',
  `lab_retention_until` date DEFAULT NULL,
  `submitted_at` timestamp(6) NULL DEFAULT NULL,
  `received_at` timestamp(6) NULL DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL,
  `updated_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sample_transfers_code` (`transfer_code`),
  KEY `idx_sample_transfers_receipt` (`receipt_id`),
  KEY `idx_sample_transfers_status` (`status`),
  KEY `fk_sample_transfers_delivered_by` (`delivered_by_user_id`),
  KEY `fk_sample_transfers_received_by` (`received_by_user_id`),
  KEY `idx_sample_transfers_status_submitted` (`status`,`submitted_at`),
  KEY `idx_sample_transfers_received_at` (`received_at`),
  CONSTRAINT `fk_sample_transfers_delivered_by` FOREIGN KEY (`delivered_by_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_sample_transfers_receipt` FOREIGN KEY (`receipt_id`) REFERENCES `receipts` (`id`),
  CONSTRAINT `fk_sample_transfers_received_by` FOREIGN KEY (`received_by_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=75 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sample_transfers`
--

LOCK TABLES `sample_transfers` WRITE;
/*!40000 ALTER TABLE `sample_transfers` DISABLE KEYS */;
INSERT INTO `sample_transfers` VALUES (70,'CM-20261116-49693B',71,3,4,'KCS','2026-11-16','2026-11-18',1,'2026-11-18',NULL,'ACCEPTED',NULL,NULL,1,'2026-11-26','2026-11-19 03:45:31.529267','2026-11-19 03:52:40.970074','2026-11-19 03:44:42.124853','2026-11-19 03:52:40.970074'),(71,'CM-20261120-A23639',75,3,4,'KCS','2026-11-20','2026-11-22',0,NULL,NULL,'ACCEPTED',NULL,NULL,0,NULL,'2026-11-20 02:09:06.327672','2026-11-20 02:09:15.677328','2026-11-20 02:08:55.241352','2026-11-20 02:09:15.677328'),(72,'CM-20261123-9751DB',76,170,171,'KCS','2026-11-23','2026-11-30',0,NULL,NULL,'ACCEPTED',NULL,NULL,0,NULL,'2026-11-23 09:07:45.637598','2026-11-23 09:09:30.510125','2026-11-23 09:07:40.916748','2026-11-23 09:09:30.510125'),(73,'CM-20261127-48BEB5',78,170,1,'KCS','2026-11-27','2026-11-29',0,NULL,NULL,'ACCEPTED',NULL,NULL,0,NULL,'2026-11-27 02:20:47.172343','2026-11-27 02:31:14.972116','2026-11-27 02:20:42.619324','2026-11-27 02:31:14.972116'),(74,'CM-20261127-21F060',77,3,4,'KCS','2026-11-27','2026-12-04',0,NULL,NULL,'REJECTED','Demo',NULL,0,NULL,'2026-11-27 03:44:41.002506','2026-11-27 03:54:41.947201','2026-11-27 03:44:35.800364','2026-11-27 03:54:41.947201');
/*!40000 ALTER TABLE `sample_transfers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `samples`
--

DROP TABLE IF EXISTS `samples`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `samples` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sample_code` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `product_type` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `received_date` date NOT NULL,
  `daily_sequence` int NOT NULL,
  `name` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `quantity` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `packaging` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sample_condition` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `requested_parameters` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `receipt_id` bigint NOT NULL,
  `coding_status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING_CODING',
  `coding_legacy` tinyint(1) NOT NULL DEFAULT '0',
  `coded_by_user_id` bigint DEFAULT NULL,
  `coded_at` timestamp(6) NULL DEFAULT NULL,
  `verified_by_user_id` bigint DEFAULT NULL,
  `verified_at` timestamp(6) NULL DEFAULT NULL,
  `weight` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_samples_code` (`sample_code`),
  KEY `idx_samples_receipt` (`receipt_id`),
  KEY `idx_samples_date` (`received_date`),
  KEY `idx_samples_status_receipt` (`status`,`receipt_id`),
  KEY `idx_samples_coding_status` (`coding_status`,`receipt_id`),
  KEY `fk_samples_coded_by` (`coded_by_user_id`),
  KEY `fk_samples_verified_by` (`verified_by_user_id`),
  CONSTRAINT `fk_samples_coded_by` FOREIGN KEY (`coded_by_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_samples_receipt` FOREIGN KEY (`receipt_id`) REFERENCES `receipts` (`id`),
  CONSTRAINT `fk_samples_verified_by` FOREIGN KEY (`verified_by_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=105 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `samples`
--

LOCK TABLES `samples` WRITE;
/*!40000 ALTER TABLE `samples` DISABLE KEYS */;
INSERT INTO `samples` VALUES (93,'CT001-SR-16112026-010','CT001','A72604937','2026-11-19',2,'A72604937','01/1,8KG','Túi PE','Nguyên vẹn','Cadimi, Vàng O, Cadimi (Cd), Auramine O (Vàng ô)','TRANSFERRED',71,'VERIFIED',0,3,'2026-11-19 03:41:30.734145',NULL,NULL,NULL),(98,'CT 001','CT001','A72604937','2026-11-20',1,'A72604937','','Túi PE','Nguyên vẹn','Cadimi (Cd), Auramine O (Vàng ô)','RECEIVED',74,'VERIFIED',0,1,'2026-11-20 01:37:15.631307',NULL,NULL,NULL),(99,'CT0000000156','CT001','A72604937','2026-11-20',2,'A72604937','1/20kg','Túi PE','Nguyên vẹn','Cadimi (Cd), Auramine O (Vỏ)','TEST_REVIEWED',75,'VERIFIED',0,3,'2026-11-20 01:53:36.307549',NULL,NULL,NULL),(100,'CT001-SR0001-003','CT001','Mẫu 1','2026-11-23',1,'Mẫu 1','2','Túi PE','Nguyên vẹn','Cadimi (Cd), Auramine O (Vỏ), Auramine O (Ruột)','TRANSFERRED',76,'VERIFIED',0,172,'2026-11-23 08:57:12.756899',NULL,NULL,NULL),(101,'CT001-SR-27112026-001','.','Đồng Tháp 01','2026-11-27',1,'Đồng Tháp 01','1 trái','Túi PE','Nguyên vẹn','Cadimi (Cd)','RECEIVED',77,'VERIFIED',0,170,'2026-11-27 01:58:44.054930',NULL,NULL,'2,4kg'),(102,'CT001-SR-27112026-002','.','Đồng Tháp 02','2026-11-27',2,'Đồng Tháp 02','1 trái','Túi PE','Nguyên vẹn','Cadimi (Cd)','RECEIVED',77,'VERIFIED',0,170,'2026-11-27 01:58:44.056085',NULL,NULL,'1,6kg'),(103,'ĐT001-SR27112026-001','ĐỒNG THÁP','Mẫu 1','2026-11-27',3,'Mẫu 1','1','Túi PE','Nguyên vẹn','Khác: Cadimi (Cd) Ruột + vỏ','TRANSFERRED',78,'VERIFIED',0,170,'2026-11-27 02:18:50.145418',NULL,NULL,'2,4kg'),(104,'ĐT001-SR27112026-002','ĐỒNG THÁP','Mẫu 1','2026-11-27',4,'Mẫu 2','1','Túi PE','Nguyên vẹn','Khác: Cadimi (Cd) Ruột + vỏ','TRANSFERRED',78,'VERIFIED',0,170,'2026-11-27 02:18:50.146241',NULL,NULL,'1,6kg');
/*!40000 ALTER TABLE `samples` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sampling_plans`
--

DROP TABLE IF EXISTS `sampling_plans`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sampling_plans` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `test_request_id` bigint NOT NULL,
  `form_number` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `form_date` date NOT NULL,
  `representative` varchar(160) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `representative_phone` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sample_collector` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `collector_phone` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sampling_start` timestamp(6) NULL DEFAULT NULL,
  `sampling_end` timestamp(6) NULL DEFAULT NULL,
  `sampling_location` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `growing_area` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `packing_facility` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `vehicle_number` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `trailer_number` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `container_number` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `scheme_notes` text COLLATE utf8mb4_unicode_ci,
  `transport_conditions` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp(6) NOT NULL,
  `updated_at` timestamp(6) NOT NULL,
  `sample_forms` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `lot_size_band` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `transport_modes` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sampling_plans_request` (`test_request_id`),
  UNIQUE KEY `uk_sampling_plans_number` (`form_number`),
  CONSTRAINT `fk_sampling_plans_request` FOREIGN KEY (`test_request_id`) REFERENCES `test_requests` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sampling_plans`
--

LOCK TABLES `sampling_plans` WRITE;
/*!40000 ALTER TABLE `sampling_plans` DISABLE KEYS */;
INSERT INTO `sampling_plans` VALUES (2,3,'CFC26.00003','2026-11-23','Anh Lộc','0901234567','Nguyễn Bá Tân','0703637776',NULL,NULL,'Công ty TNHH hữu cơ đậm đặc','HG001','PH001','Rf12345','Rt12345','Rc12345',NULL,NULL,'2026-11-23 08:37:34.113870','2026-11-23 08:37:34.113898','WHOLE','5_20','WHOLE_AMBIENT');
/*!40000 ALTER TABLE `sampling_plans` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `staff_notifications`
--

DROP TABLE IF EXISTS `staff_notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `staff_notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `recipient_user_id` bigint NOT NULL,
  `type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(180) COLLATE utf8mb4_unicode_ci NOT NULL,
  `message` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `entity_type` varchar(60) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `entity_id` bigint DEFAULT NULL,
  `dedupe_key` varchar(180) COLLATE utf8mb4_unicode_ci NOT NULL,
  `read_at` timestamp(6) NULL DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL,
  `actor_user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_staff_notifications_dedupe` (`dedupe_key`),
  KEY `idx_staff_notifications_recipient_created` (`recipient_user_id`,`created_at`),
  KEY `idx_staff_notifications_recipient_read` (`recipient_user_id`,`read_at`),
  KEY `fk_staff_notifications_actor` (`actor_user_id`),
  CONSTRAINT `fk_staff_notifications_actor` FOREIGN KEY (`actor_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_staff_notifications_recipient` FOREIGN KEY (`recipient_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=282 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `staff_notifications`
--

LOCK TABLES `staff_notifications` WRITE;
/*!40000 ALTER TABLE `staff_notifications` DISABLE KEYS */;
INSERT INTO `staff_notifications` VALUES (244,1,'INTAKE_RECORDED','Phiếu tiếp nhận mới','Nguyễn Trung Thuận đã ghi nhận 1 mẫu từ Hộ Kinh Doanh Hưng Thị','/admin/intake/71','RECEIPT',71,'INTAKE_RECORDED:RECEIPT:71:1','2026-11-19 04:07:59.455349','2026-11-19 03:41:30.737206',NULL),(245,4,'INTAKE_RECORDED','Phiếu tiếp nhận mới','Nguyễn Trung Thuận đã ghi nhận 1 mẫu từ Hộ Kinh Doanh Hưng Thị','/admin/intake/71','RECEIPT',71,'INTAKE_RECORDED:RECEIPT:71:4','2026-11-23 06:20:37.511422','2026-11-19 03:41:30.738458',NULL),(246,4,'TRANSFER_SUBMITTED','Phiếu chuyển mẫu mới','Nguyễn Trung Thuận đã chuyển 1 mẫu của Số/No. CFC26.0001 đến KCS','/admin/transfers/70','TRANSFER',70,'TRANSFER_SUBMITTED:TRANSFER:70:4','2026-11-23 06:20:37.511422','2026-11-19 03:45:31.531428',NULL),(247,3,'TRANSFER_ACCEPTED','KCS đã nhận mẫu','Tiếp nhận đã xác nhận nhận phiếu CM-20261116-49693B','/admin/transfers/70','TRANSFER',70,'TRANSFER_ACCEPTED:TRANSFER:70:3','2026-11-20 02:53:48.245019','2026-11-19 03:52:40.972137',NULL),(252,4,'INTAKE_RECORDED','Phiếu tiếp nhận mới','Quản trị viên đã ghi nhận 1 mẫu từ Hộ Kinh Doanh Hưng Thị','/admin/intake/74','RECEIPT',74,'INTAKE_RECORDED:RECEIPT:74:4','2026-11-23 06:20:37.511422','2026-11-20 01:37:15.659540',NULL),(253,1,'INTAKE_RECORDED','Phiếu tiếp nhận mới','Nguyễn Trung Thuận đã ghi nhận 1 mẫu từ Hộ Kinh Doanh Hưng Thị','/admin/intake/75','RECEIPT',75,'INTAKE_RECORDED:RECEIPT:75:1','2026-11-20 04:03:44.760102','2026-11-20 01:53:36.340203',NULL),(254,4,'INTAKE_RECORDED','Phiếu tiếp nhận mới','Nguyễn Trung Thuận đã ghi nhận 1 mẫu từ Hộ Kinh Doanh Hưng Thị','/admin/intake/75','RECEIPT',75,'INTAKE_RECORDED:RECEIPT:75:4','2026-11-20 07:07:01.945303','2026-11-20 01:53:36.343263',NULL),(255,4,'TRANSFER_SUBMITTED','Phiếu chuyển mẫu mới','Nguyễn Trung Thuận đã chuyển 1 mẫu của Số/No. CFC26.0002 đến KCS','/admin/transfers/71','TRANSFER',71,'TRANSFER_SUBMITTED:TRANSFER:71:4','2026-11-20 02:09:10.193945','2026-11-20 02:09:06.336595',NULL),(256,3,'TRANSFER_ACCEPTED','KCS đã nhận mẫu','Tiếp nhận đã xác nhận nhận phiếu CM-20261120-A23639','/admin/transfers/71','TRANSFER',71,'TRANSFER_ACCEPTED:TRANSFER:71:3','2026-11-20 02:53:48.245019','2026-11-20 02:09:15.680459',NULL),(257,1,'INTAKE_RECORDED','Phiếu tiếp nhận mới','Nguyễn Bá Tân đã ghi nhận 1 mẫu từ Công ty TNHH MTV HỮU CƠ ĐẬM ĐẶC','/admin/intake/76','RECEIPT',76,'INTAKE_RECORDED:RECEIPT:76:1','2026-11-23 08:57:19.041699','2026-11-23 08:57:12.772323',172),(258,4,'INTAKE_RECORDED','Phiếu tiếp nhận mới','Nguyễn Bá Tân đã ghi nhận 1 mẫu từ Công ty TNHH MTV HỮU CƠ ĐẬM ĐẶC','/admin/intake/76','RECEIPT',76,'INTAKE_RECORDED:RECEIPT:76:4','2026-11-25 00:41:08.760690','2026-11-23 08:57:12.775459',172),(259,171,'INTAKE_RECORDED','Phiếu tiếp nhận mới','Nguyễn Bá Tân đã ghi nhận 1 mẫu từ Công ty TNHH MTV HỮU CƠ ĐẬM ĐẶC','/admin/intake/76','RECEIPT',76,'INTAKE_RECORDED:RECEIPT:76:171',NULL,'2026-11-23 08:57:12.777128',172),(260,4,'TRANSFER_SUBMITTED','Phiếu chuyển mẫu mới','Trần Thị Lệ Thúy đã chuyển 1 mẫu của Số/No. CFC26.0003 đến KCS','/admin/transfers/72','TRANSFER',72,'TRANSFER_SUBMITTED:TRANSFER:72:4','2026-11-25 00:35:48.884219','2026-11-23 09:07:45.639763',170),(261,171,'TRANSFER_SUBMITTED','Phiếu chuyển mẫu mới','Trần Thị Lệ Thúy đã chuyển 1 mẫu của Số/No. CFC26.0003 đến KCS','/admin/transfers/72','TRANSFER',72,'TRANSFER_SUBMITTED:TRANSFER:72:171','2026-11-27 02:11:54.704548','2026-11-23 09:07:45.640913',170),(262,170,'TRANSFER_ACCEPTED','KCS đã nhận mẫu','Đặng Phương Thảo đã xác nhận nhận mẫu của Số/No. CFC26.0003','/admin/transfers/72','TRANSFER',72,'TRANSFER_ACCEPTED:TRANSFER:72:170','2026-11-25 03:07:59.969770','2026-11-23 09:09:30.513177',171),(263,1,'TRANSFER_SUBMITTED','Phiếu chuyển mẫu mới','Nguyễn Trung Thuận đã chuyển 1 mẫu của Số/No. CFC26.0001 đến KCS','/admin/transfers/70','TRANSFER',70,'TRANSFER_SUBMITTED:TRANSFER:70:1','2026-11-25 07:24:42.669656','2026-11-19 03:45:31.531428',NULL),(264,1,'TRANSFER_ACCEPTED','KCS đã nhận mẫu','Tiếp nhận đã xác nhận nhận phiếu CM-20261116-49693B','/admin/transfers/70','TRANSFER',70,'TRANSFER_ACCEPTED:TRANSFER:70:1','2026-11-25 07:24:42.669656','2026-11-19 03:52:40.972137',NULL),(265,1,'INTAKE_RECORDED','Phiếu tiếp nhận mới','Quản trị viên đã ghi nhận 1 mẫu từ Hộ Kinh Doanh Hưng Thị','/admin/intake/74','RECEIPT',74,'INTAKE_RECORDED:RECEIPT:74:1','2026-11-25 07:24:42.669656','2026-11-20 01:37:15.659540',NULL),(266,1,'TRANSFER_SUBMITTED','Phiếu chuyển mẫu mới','Nguyễn Trung Thuận đã chuyển 1 mẫu của Số/No. CFC26.0002 đến KCS','/admin/transfers/71','TRANSFER',71,'TRANSFER_SUBMITTED:TRANSFER:71:1','2026-11-25 07:24:42.669656','2026-11-20 02:09:06.336595',NULL),(267,1,'TRANSFER_ACCEPTED','KCS đã nhận mẫu','Tiếp nhận đã xác nhận nhận phiếu CM-20261120-A23639','/admin/transfers/71','TRANSFER',71,'TRANSFER_ACCEPTED:TRANSFER:71:1','2026-11-25 07:24:42.669656','2026-11-20 02:09:15.680459',NULL),(268,1,'TRANSFER_SUBMITTED','Phiếu chuyển mẫu mới','Trần Thị Lệ Thúy đã chuyển 1 mẫu của Số/No. CFC26.0003 đến KCS','/admin/transfers/72','TRANSFER',72,'TRANSFER_SUBMITTED:TRANSFER:72:1','2026-11-25 07:24:42.669656','2026-11-23 09:07:45.639763',170),(269,1,'TRANSFER_ACCEPTED','KCS đã nhận mẫu','Đặng Phương Thảo đã xác nhận nhận mẫu của Số/No. CFC26.0003','/admin/transfers/72','TRANSFER',72,'TRANSFER_ACCEPTED:TRANSFER:72:1','2026-11-25 07:24:42.669656','2026-11-23 09:09:30.513177',171),(270,1,'INTAKE_RECORDED','Phiếu tiếp nhận mới','Trần Thị Lệ Thúy đã ghi nhận 2 mẫu từ Công ty TNHH MTV HỮU CƠ ĐẬM ĐẶC','/admin/intake/77','RECEIPT',77,'INTAKE_RECORDED:RECEIPT:77:1','2026-11-27 06:31:04.223941','2026-11-27 01:58:44.101404',170),(271,1,'INTAKE_RECORDED','Phiếu tiếp nhận mới','Trần Thị Lệ Thúy đã ghi nhận 2 mẫu từ Anh Long','/admin/intake/78','RECEIPT',78,'INTAKE_RECORDED:RECEIPT:78:1','2026-11-27 06:31:04.223941','2026-11-27 02:18:50.156932',170),(272,4,'TRANSFER_SUBMITTED','Phiếu chuyển mẫu mới','Trần Thị Lệ Thúy đã chuyển 2 mẫu của Số/No. CFC26.0005 đến KCS','/admin/receiving/transfers/73','TRANSFER',73,'TRANSFER_SUBMITTED:TRANSFER:73:4','2026-11-27 03:21:17.551949','2026-11-27 02:20:47.175380',170),(273,171,'TRANSFER_SUBMITTED','Phiếu chuyển mẫu mới','Trần Thị Lệ Thúy đã chuyển 2 mẫu của Số/No. CFC26.0005 đến KCS','/admin/receiving/transfers/73','TRANSFER',73,'TRANSFER_SUBMITTED:TRANSFER:73:171',NULL,'2026-11-27 02:20:47.177304',170),(274,1,'TRANSFER_SUBMITTED','Phiếu chuyển mẫu mới','Trần Thị Lệ Thúy đã chuyển 2 mẫu của Số/No. CFC26.0005 đến KCS','/admin/receiving/transfers/73','TRANSFER',73,'TRANSFER_SUBMITTED:TRANSFER:73:1','2026-11-27 06:31:04.223941','2026-11-27 02:20:47.179921',170),(275,170,'TRANSFER_ACCEPTED','KCS đã nhận mẫu','Quản trị viên đã xác nhận nhận mẫu của Số/No. CFC26.0005','/admin/transfers/73','TRANSFER',73,'TRANSFER_ACCEPTED:TRANSFER:73:170',NULL,'2026-11-27 02:31:14.976352',1),(276,1,'TRANSFER_ACCEPTED','KCS đã nhận mẫu','Quản trị viên đã xác nhận nhận mẫu của Số/No. CFC26.0005','/admin/transfers/73','TRANSFER',73,'TRANSFER_ACCEPTED:TRANSFER:73:1','2026-11-27 06:31:04.223941','2026-11-27 02:31:14.977720',1),(277,4,'TRANSFER_SUBMITTED','Phiếu chuyển mẫu mới','Nguyễn Trung Thuận đã chuyển 2 mẫu của Số/No. CFC26.00004 đến KCS','/admin/receiving/transfers/74','TRANSFER',74,'TRANSFER_SUBMITTED:TRANSFER:74:4',NULL,'2026-11-27 03:44:41.015606',3),(278,171,'TRANSFER_SUBMITTED','Phiếu chuyển mẫu mới','Nguyễn Trung Thuận đã chuyển 2 mẫu của Số/No. CFC26.00004 đến KCS','/admin/receiving/transfers/74','TRANSFER',74,'TRANSFER_SUBMITTED:TRANSFER:74:171',NULL,'2026-11-27 03:44:41.018996',3),(279,1,'TRANSFER_SUBMITTED','Phiếu chuyển mẫu mới','Nguyễn Trung Thuận đã chuyển 2 mẫu của Số/No. CFC26.00004 đến KCS','/admin/receiving/transfers/74','TRANSFER',74,'TRANSFER_SUBMITTED:TRANSFER:74:1','2026-11-27 06:31:04.223941','2026-11-27 03:44:41.021560',3),(280,3,'TRANSFER_REJECTED','KCS từ chối nhận mẫu','Tiếp nhận đã từ chối mẫu của Số/No. CFC26.00004: Demo','/admin/transfers/74','TRANSFER',74,'TRANSFER_REJECTED:TRANSFER:74:3','2026-11-27 03:55:19.203528','2026-11-27 03:54:41.967527',4),(281,1,'TRANSFER_REJECTED','KCS từ chối nhận mẫu','Tiếp nhận đã từ chối mẫu của Số/No. CFC26.00004: Demo','/admin/transfers/74','TRANSFER',74,'TRANSFER_REJECTED:TRANSFER:74:1','2026-11-27 06:31:04.223941','2026-11-27 03:54:41.982869',4);
/*!40000 ALTER TABLE `staff_notifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `storage_locations`
--

DROP TABLE IF EXISTS `storage_locations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `storage_locations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `label` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `legacy_key` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `root_path` varchar(700) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `marker` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `volume_identity` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `validated_at` timestamp(6) NULL DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_storage_legacy` (`legacy_key`),
  UNIQUE KEY `uk_storage_root` (`root_path`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `storage_locations`
--

LOCK TABLES `storage_locations` WRITE;
/*!40000 ALTER TABLE `storage_locations` DISABLE KEYS */;
INSERT INTO `storage_locations` VALUES (1,'Tiếp nhận hiện tại','RECEIPTS',NULL,NULL,NULL,NULL,'2026-11-25 03:29:05.675705'),(2,'Kết quả hiện tại','RESULTS',NULL,NULL,NULL,NULL,'2026-11-25 03:29:05.675705');
/*!40000 ALTER TABLE `storage_locations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `storage_write_settings`
--

DROP TABLE IF EXISTS `storage_write_settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `storage_write_settings` (
  `id` int NOT NULL,
  `location_id` bigint DEFAULT NULL,
  `version` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `fk_storage_write_location` (`location_id`),
  CONSTRAINT `fk_storage_write_location` FOREIGN KEY (`location_id`) REFERENCES `storage_locations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `storage_write_settings`
--

LOCK TABLES `storage_write_settings` WRITE;
/*!40000 ALTER TABLE `storage_write_settings` DISABLE KEYS */;
INSERT INTO `storage_write_settings` VALUES (1,NULL,0);
/*!40000 ALTER TABLE `storage_write_settings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `test_methods`
--

DROP TABLE IF EXISTS `test_methods`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `test_methods` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp(6) NOT NULL,
  `updated_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_test_methods_code` (`code`),
  KEY `idx_test_methods_active_code` (`active`,`code`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `test_methods`
--

LOCK TABLES `test_methods` WRITE;
/*!40000 ALTER TABLE `test_methods` DISABLE KEYS */;
INSERT INTO `test_methods` VALUES (1,'CFC-TN-16-2026',NULL,1,'2026-11-20 02:01:03.649218','2026-11-20 02:01:03.649218'),(2,'CFC-TN-17-2026',NULL,1,'2026-11-20 02:01:03.649218','2026-11-20 02:01:03.649218');
/*!40000 ALTER TABLE `test_methods` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `test_request_counters`
--

DROP TABLE IF EXISTS `test_request_counters`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `test_request_counters` (
  `year_code` char(2) COLLATE utf8mb4_unicode_ci NOT NULL,
  `last_sequence` int NOT NULL,
  PRIMARY KEY (`year_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `test_request_counters`
--

LOCK TABLES `test_request_counters` WRITE;
/*!40000 ALTER TABLE `test_request_counters` DISABLE KEYS */;
INSERT INTO `test_request_counters` VALUES ('26',3);
/*!40000 ALTER TABLE `test_request_counters` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `test_request_items`
--

DROP TABLE IF EXISTS `test_request_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `test_request_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `test_request_id` bigint NOT NULL,
  `row_order` int NOT NULL,
  `sample_name_or_id` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `parameters_text` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `sample_matrix` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `test_method` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_test_request_items_request` (`test_request_id`,`row_order`),
  CONSTRAINT `fk_test_request_items_request` FOREIGN KEY (`test_request_id`) REFERENCES `test_requests` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `test_request_items`
--

LOCK TABLES `test_request_items` WRITE;
/*!40000 ALTER TABLE `test_request_items` DISABLE KEYS */;
INSERT INTO `test_request_items` VALUES (1,1,1,'A72604937','Cadimi, Vàng O',NULL,'CFC-TN016, CFC-TN017'),(3,3,1,'Mẫu 1','Cadimi (Cd)','Ruột',NULL),(4,3,2,'Mẫu 1','Vàng O (Auramin O)','Vỏ',NULL),(5,3,3,'Mẫu 1','Vàng O (Auramin O)','Ruột',NULL);
/*!40000 ALTER TABLE `test_request_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `test_requests`
--

DROP TABLE IF EXISTS `test_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `test_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `request_number` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `request_date` date NOT NULL,
  `customer_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tax_code` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contact_person` varchar(160) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `customer_address` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `customer_phone` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `customer_email` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sample_name` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `lot_code` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `variety` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `origin` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `harvest_date` date DEFAULT NULL,
  `packing_date` date DEFAULT NULL,
  `sampling_mode` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `purpose` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `expected_date` date DEFAULT NULL,
  `result_delivery_method` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `service_conditions` text COLLATE utf8mb4_unicode_ci,
  `delay_notice` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT',
  `created_by_user_id` bigint NOT NULL,
  `created_at` timestamp(6) NOT NULL,
  `updated_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_test_requests_number` (`request_number`),
  KEY `idx_test_requests_date` (`request_date`),
  KEY `fk_test_requests_created_by` (`created_by_user_id`),
  CONSTRAINT `fk_test_requests_created_by` FOREIGN KEY (`created_by_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `test_requests`
--

LOCK TABLES `test_requests` WRITE;
/*!40000 ALTER TABLE `test_requests` DISABLE KEYS */;
INSERT INTO `test_requests` VALUES (1,'CFC26.00001','2026-11-19','Hộ Kinh Doanh Hưng Thị','093185009265','Anh Hưng','40 Phan Văn trị, Phường Ninh Kiều, TP Cần Thơ','0939740009',NULL,'Sầu Riêng Trái','2C26THT','Thái','00002','2026-11-15','2026-11-15','COMPANY','Yêu cầu của khách hàng','2026-11-18','email',NULL,NULL,'COMPLETED',3,'2026-11-19 03:22:47.799176','2026-11-19 03:22:47.799469'),(3,'CFC26.00003','2026-11-23','Công ty TNHH MTV HỮU CƠ ĐẬM ĐẶC','1800155439','Anh Lộc','Phụng hiệp','0901234567',NULL,'Sầu riêng trái','003','Thái','HG001','2026-11-22','2026-11-23','COMPANY','Đánh giá lô hàng','2026-11-25','Mail, điện thoại',NULL,'Điện thoại','COMPLETED',172,'2026-11-23 08:35:47.895060','2026-11-23 08:35:47.895348');
/*!40000 ALTER TABLE `test_requests` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `testing_equipment`
--

DROP TABLE IF EXISTS `testing_equipment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `testing_equipment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `testing_record_id` bigint NOT NULL,
  `name` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `serial_number` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `manufacturer` varchar(160) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `identification_code` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `calibration_date` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_testing_equipment_record` (`testing_record_id`),
  CONSTRAINT `fk_testing_equipment_record` FOREIGN KEY (`testing_record_id`) REFERENCES `testing_records` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `testing_equipment`
--

LOCK TABLES `testing_equipment` WRITE;
/*!40000 ALTER TABLE `testing_equipment` DISABLE KEYS */;
/*!40000 ALTER TABLE `testing_equipment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `testing_measurements`
--

DROP TABLE IF EXISTS `testing_measurements`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `testing_measurements` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `testing_record_id` bigint NOT NULL,
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `row_order` int NOT NULL,
  `replicate` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sample_amount` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `final_volume` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `dilution_factor` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `measured_concentration` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `result_per_replicate` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `result` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `deviation` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_testing_measurements_record_type` (`testing_record_id`,`type`),
  CONSTRAINT `fk_testing_measurements_record` FOREIGN KEY (`testing_record_id`) REFERENCES `testing_records` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=46 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `testing_measurements`
--

LOCK TABLES `testing_measurements` WRITE;
/*!40000 ALTER TABLE `testing_measurements` DISABLE KEYS */;
/*!40000 ALTER TABLE `testing_measurements` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `testing_reagents`
--

DROP TABLE IF EXISTS `testing_reagents`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `testing_reagents` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `testing_record_id` bigint NOT NULL,
  `name` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `code` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `origin` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `preparation_date` date DEFAULT NULL,
  `expiry_date` date DEFAULT NULL,
  `storage_condition` varchar(160) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `concentration` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_testing_reagents_record` (`testing_record_id`),
  CONSTRAINT `fk_testing_reagents_record` FOREIGN KEY (`testing_record_id`) REFERENCES `testing_records` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `testing_reagents`
--

LOCK TABLES `testing_reagents` WRITE;
/*!40000 ALTER TABLE `testing_reagents` DISABLE KEYS */;
/*!40000 ALTER TABLE `testing_reagents` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `testing_records`
--

DROP TABLE IF EXISTS `testing_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `testing_records` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `record_code` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `transfer_item_id` bigint NOT NULL,
  `analyst_user_id` bigint NOT NULL,
  `checked_by_user_id` bigint DEFAULT NULL,
  `analysis_start` timestamp(6) NULL DEFAULT NULL,
  `analysis_end` timestamp(6) NULL DEFAULT NULL,
  `calibration_curve` text COLLATE utf8mb4_unicode_ci,
  `calculation_formula` text COLLATE utf8mb4_unicode_ci,
  `recovery_percent` decimal(12,4) DEFAULT NULL,
  `qc_chart_note` text COLLATE utf8mb4_unicode_ci,
  `analyst_note` text COLLATE utf8mb4_unicode_ci,
  `review_note` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `submitted_at` timestamp(6) NULL DEFAULT NULL,
  `reviewed_at` timestamp(6) NULL DEFAULT NULL,
  `created_at` timestamp(6) NOT NULL,
  `updated_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_testing_records_code` (`record_code`),
  UNIQUE KEY `uk_testing_records_transfer_item` (`transfer_item_id`),
  KEY `idx_testing_records_status` (`status`),
  KEY `idx_testing_records_analyst` (`analyst_user_id`),
  KEY `fk_testing_records_checker` (`checked_by_user_id`),
  CONSTRAINT `fk_testing_records_analyst` FOREIGN KEY (`analyst_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_testing_records_checker` FOREIGN KEY (`checked_by_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_testing_records_transfer_item` FOREIGN KEY (`transfer_item_id`) REFERENCES `sample_transfer_items` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `testing_records`
--

LOCK TABLES `testing_records` WRITE;
/*!40000 ALTER TABLE `testing_records` DISABLE KEYS */;
INSERT INTO `testing_records` VALUES (36,'BBTN-20261120-CF2AD9',73,4,4,'2026-11-19 17:00:00.000000',NULL,NULL,NULL,NULL,NULL,NULL,'Hồ sơ được lập từ kết quả nhập trên hệ thống','REVIEWED','2026-11-20 07:49:34.659480','2026-11-20 07:49:34.659480','2026-11-20 07:49:34.659476','2026-11-20 07:49:34.659480');
/*!40000 ALTER TABLE `testing_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `transfer_assignees`
--

DROP TABLE IF EXISTS `transfer_assignees`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transfer_assignees` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `transfer_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `assigned_by_user_id` bigint NOT NULL,
  `assigned_at` timestamp(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_transfer_assignee` (`transfer_id`,`user_id`),
  KEY `idx_transfer_assignees_user` (`user_id`,`transfer_id`),
  KEY `fk_transfer_assignees_by` (`assigned_by_user_id`),
  CONSTRAINT `fk_transfer_assignees_by` FOREIGN KEY (`assigned_by_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_transfer_assignees_transfer` FOREIGN KEY (`transfer_id`) REFERENCES `sample_transfers` (`id`),
  CONSTRAINT `fk_transfer_assignees_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `transfer_assignees`
--

LOCK TABLES `transfer_assignees` WRITE;
/*!40000 ALTER TABLE `transfer_assignees` DISABLE KEYS */;
/*!40000 ALTER TABLE `transfer_assignees` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `display_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp(6) NOT NULL,
  `updated_at` timestamp(6) NOT NULL,
  `avatar_updated_at` timestamp(6) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=173 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'nguyentrungthuan417@gmail.com','Quản trị viên','$2a$10$DYcDGAgsc1LuQ8OUSNknwOdPlUQtRA0r32beaVNuOmmPXfSVRBgsi','ADMIN',1,'2026-11-12 09:00:46.000091','2026-11-12 09:00:46.000091',NULL),(3,'nguyentrungthuan4177@gmail.com','Nguyễn Trung Thuận','$2a$10$GohQJSS8TrnMqt/OeIzlB.83iMQExJNym32NaiTOB5nqpOczcSa/.','RECEPTIONIST',1,'2026-11-13 06:24:48.197579','2026-11-20 03:19:08.571455','2026-11-20 03:19:08.571455'),(4,'nguyentrungthuan@gmail.com','Tiếp nhận','$2a$10$zvzeewJ1m36RfGPXaNUjqulGobJS3r32WbRDS7DSFwv/jgk6AWgEa','TECHNICIAN',1,'2026-11-16 07:40:41.737870','2026-11-16 07:40:41.737870',NULL),(170,'tranthilethuy@gmail.com','Trần Thị Lệ Thúy','$2a$10$L0Rx2M0MrBl9F6YUbhHZpuPx25RojVY8PHuKTrh8VMUyfBestXXDu','RECEPTIONIST',1,'2026-11-23 01:38:08.025656','2026-11-23 01:38:08.025656',NULL),(171,'dangphuongthao@gmail.com','Đặng Phương Thảo','$2a$10$igGcb6JtBA3XrPPpuukHiefZnVNK0gkKZuBE2chUVa2Al7tNz8tLC','TECHNICIAN',1,'2026-11-23 01:39:48.045079','2026-11-23 01:39:48.045079',NULL),(172,'nguyenbatan@gmail.com','Nguyễn Bá Tân','$2a$10$YFm.yhX4KN7fnjGWDx1FSu2E3vyz633IZX2aR1D9chaQv/jHserCW','RECEPTIONIST',1,'2026-11-23 01:43:33.612546','2026-11-23 01:43:33.612546',NULL);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `zalo_delivery_inbox`
--

DROP TABLE IF EXISTS `zalo_delivery_inbox`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `zalo_delivery_inbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `app_id` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `oa_id` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `message_id` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `tracking_id` varchar(48) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `recipient_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `delivered_at` datetime(6) NOT NULL,
  `received_at` datetime(6) NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `reason_code` varchar(60) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `next_match_at` datetime(6) NOT NULL,
  `match_count` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_zalo_inbox_event` (`event_key`),
  KEY `ix_zalo_inbox_due` (`status`,`next_match_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `zalo_delivery_inbox`
--

LOCK TABLES `zalo_delivery_inbox` WRITE;
/*!40000 ALTER TABLE `zalo_delivery_inbox` DISABLE KEYS */;
/*!40000 ALTER TABLE `zalo_delivery_inbox` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `zalo_managed_tokens`
--

DROP TABLE IF EXISTS `zalo_managed_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `zalo_managed_tokens` (
  `scope_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `app_id` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `oa_id` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `provider_mode` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `encrypted_pair` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `access_expires_at` datetime(6) NOT NULL,
  `refresh_expires_at` datetime(6) NOT NULL,
  `state` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `lease_owner` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
  `lease_until` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `reason_code` varchar(60) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`scope_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `zalo_managed_tokens`
--

LOCK TABLES `zalo_managed_tokens` WRITE;
/*!40000 ALTER TABLE `zalo_managed_tokens` DISABLE KEYS */;
/*!40000 ALTER TABLE `zalo_managed_tokens` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'cfc_lab'
--

--
-- Dumping routines for database 'cfc_lab'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-27  6:53:33
