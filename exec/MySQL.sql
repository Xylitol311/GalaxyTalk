Enter password: 
-- MySQL dump 10.13  Distrib 8.0.41, for Linux (x86_64)
--
-- Host: localhost    Database: galaxytalk
-- ------------------------------------------------------
-- Server version	8.0.41

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
-- Table structure for table `feedback`
--

DROP TABLE IF EXISTS `feedback`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `feedback` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `title` varchar(255) NOT NULL,
  `writer_id` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `feedback`
--

LOCK TABLES `feedback` WRITE;
/*!40000 ALTER TABLE `feedback` DISABLE KEYS */;
INSERT INTO `feedback` VALUES (1,'들어가나?','2025-02-18 09:03:32.199010','아아아','3919215125'),(2,'분명히 매칭을 거절했는데 해당 상대와 다시 매칭이 됩니다.','2025-02-18 11:40:31.696252','매칭 거절 상대와 자꾸 만나게 돼요','3916782565'),(3,'이렇게만 계속 해주세요','2025-02-20 03:53:40.430981','아주 잘하고 있어요','3916782565'),(4,'ㅠㅠ','2025-02-20 11:40:47.098987','모바일에서 화면이 잘려요','3916782565'),(5,'ㅎㅇㅎㅇ','2025-02-20 12:40:20.526119','기능이 끝내줘요','3916782565');
/*!40000 ALTER TABLE `feedback` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `letter`
--

DROP TABLE IF EXISTS `letter`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `letter` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `chat_room_id` varchar(255) NOT NULL,
  `content` varchar(255) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `is_hide` int NOT NULL DEFAULT '0',
  `receiver_id` varchar(255) NOT NULL,
  `sender_id` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `letter`
--

LOCK TABLES `letter` WRITE;
/*!40000 ALTER TABLE `letter` DISABLE KEYS */;
INSERT INTO `letter` VALUES (25,'67b7c192bd532977f78e6c38','못했던 얘기를 털어놓을 수 있어 좋았어요. 감사합니다.','2025-02-20 23:59:51.015850',0,'3916786809','3916782565'),(26,'67b7c192bd532977f78e6c38','고민을 잘 들어주셔서 너무 좋았습니다 ㅠㅠ\n다음에도 또 뵐 수 있으면 좋겠어요!','2025-02-21 00:00:26.305398',0,'3916782565','3916786809'),(27,'67b7cc52bd532977f78e6c39','덕분에 힐링했어요 ㅎㅎ','2025-02-21 00:45:40.737938',0,'3916782565','3916820423'),(28,'67b7cc52bd532977f78e6c39','너무 좋은 대화였습니다ㅎㅎ','2025-02-21 00:45:53.026676',0,'3916820423','3916782565'),(29,'67b7cd12bd532977f78e6c3a','또 만났네요 ㅎㅎ','2025-02-21 00:47:46.829884',0,'3916782565','3916820423'),(30,'67b7cd12bd532977f78e6c3a','좋았습니다','2025-02-21 00:47:49.774462',0,'3916820423','3916782565'),(31,'67b7cdd2bd532977f78e6c3b','안녕하세요\n안녕히계세요','2025-02-21 00:53:18.133834',0,'3930465866','3920611349');
/*!40000 ALTER TABLE `letter` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `planets`
--

DROP TABLE IF EXISTS `planets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `planets` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `avatar` varchar(255) NOT NULL,
  `description` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `planets`
--

LOCK TABLES `planets` WRITE;
/*!40000 ALTER TABLE `planets` DISABLE KEYS */;
INSERT INTO `planets` VALUES (1,'','1.PNG','   '),(2,'','2.PNG','   '),(3,'','3.PNG','   '),(4,'','4.PNG','   ');
/*!40000 ALTER TABLE `planets` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `serial_number` varchar(255) NOT NULL,
  `mbti` varchar(255) DEFAULT NULL,
  `planet_id` bigint DEFAULT NULL,
  `energy` int NOT NULL DEFAULT '0',
  `number_of_blocks` int NOT NULL DEFAULT '0',
  `role` varchar(20) NOT NULL,
  `created_at` datetime NOT NULL,
  `withdrawn_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `serial_number` (`serial_number`),
  KEY `planet_id` (`planet_id`),
  CONSTRAINT `users_ibfk_1` FOREIGN KEY (`planet_id`) REFERENCES `planets` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (3,'3916782565','ESFP',1,30,0,'ROLE_USER','2025-02-11 08:42:06','2025-02-13 05:33:48'),(4,'3916786809','ISFJ',1,50,0,'ROLE_USER','2025-02-11 08:45:09',NULL),(5,'3916820423','ESTJ',2,99,0,'ROLE_USER','2025-02-11 09:10:00','2025-02-21 02:44:25'),(6,'3916548832','ISTP',1,53,0,'ROLE_USER','2025-02-11 15:44:48',NULL),(7,'ubfcCEg8qrioXkI87mvkLU5XMt0EC87d90RtxZmPF7Q',NULL,NULL,75,0,'ROLE_GUEST','2025-02-11 19:23:33',NULL),(8,'testuser1','INTP',1,24,0,'ROLE_USER','2025-02-11 07:45:31',NULL),(9,'testuser2','ISTJ',1,17,0,'ROLE_USER','2025-02-11 07:45:31',NULL),(10,'testuser3','ENFP',1,37,0,'ROLE_USER','2025-02-11 07:45:31',NULL),(11,'testuser4','ENTP',1,58,0,'ROLE_USER','2025-02-11 07:45:31',NULL),(18,'3919215125','INTJ',3,86,0,'ROLE_USER','2025-02-13 05:21:42',NULL),(19,'TQLFszqK6szLPq_uuxyMLyQqzlobDJYBSwKPu53qRvs','ESTJ',1,79,0,'ROLE_USER','2025-02-13 05:30:53','2025-02-13 05:31:49'),(20,'3920611349','ESTP',1,65,0,'ROLE_USER','2025-02-14 01:54:50',NULL),(21,'3920729144',NULL,1,48,0,'ROLE_USER','2025-02-14 03:24:10',NULL),(22,'3921322582','ENFJ',1,60,0,'ROLE_USER','2025-02-14 10:56:05',NULL),(23,'3922938786','ENTP',4,80,0,'ROLE_USER','2025-02-15 15:17:20',NULL),(24,'3923638389','ENTJ',3,78,0,'ROLE_USER','2025-02-16 06:41:33',NULL),(25,'3916513038','ENTP',4,67,0,'ROLE_USER','2025-02-18 03:00:39',NULL),(26,'3927960235','INTJ',1,56,0,'ROLE_USER','2025-02-19 05:16:20',NULL),(27,'3928204283','ESTP',1,45,0,'ROLE_USER','2025-02-19 08:03:35',NULL),(28,'3928451013','ESTP',1,34,0,'ROLE_USER','2025-02-19 11:07:15',NULL),(29,'3930030289','ISFJ',1,0,0,'ROLE_USER','2025-02-20 12:39:07',NULL),(30,'3930465866','ESFP',1,0,0,'ROLE_USER','2025-02-21 00:34:00',NULL),(31,'3930473388','INFP',4,0,0,'ROLE_USER','2025-02-21 00:41:03',NULL),(32,'3930483083','ISFP',1,0,0,'ROLE_USER','2025-02-21 00:49:57',NULL),(33,'3930483358','ENTP',1,0,0,'ROLE_USER','2025-02-21 00:50:11',NULL);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-02-21  2:56:22
