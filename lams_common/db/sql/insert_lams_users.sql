SET FOREIGN_KEY_CHECKS=0;
SET NAMES utf8mb4 ;
--
-- Dumping data for table `lams_theme`
--

LOCK TABLES `lams_theme` WRITE;
INSERT INTO `lams_theme` VALUES (1,'defaultHTML','Default HTML style','css'),(2,'purple','Purple Theme','css');
UNLOCK TABLES;

--
-- Dumping data for table `lams_workspace_folder`
--

LOCK TABLES `lams_workspace_folder` WRITE;
INSERT INTO `lams_workspace_folder` VALUES (1,NULL,'ROOT',1,1,NOW(),NOW(),1),(2,1,'Developers Playpen',1,2,NOW(),NOW(),1),(3,1,'MATH111',1,4,NOW(),NOW(),1),(4,NULL,'Mary Morgan',4,NULL,NOW(),NOW(),1),(5,NULL,'One Test',5,NULL,NOW(),NOW(),1),(6,NULL,'Two Test',6,NULL,NOW(),NOW(),1),(7,NULL,'Three Test',7,NULL,NOW(),NOW(),1),(8,NULL,'Four Test',8,NULL,NOW(),NOW(),1),(22,2,'Lesson Sequence Folder',1,2,NOW(),NOW(),2),(23,3,'Lesson Sequence Folder',1,4,NOW(),NOW(),2),(40,1,'Moodle Test',1,7,NOW(),NOW(),2),(41,40,'Lesson Sequence Folder',1,7,NOW(),NOW(),2),(45,NULL,'System Administrator',1,NULL,NOW(),NOW(),1),(46,1,'Public Folder',1,NULL,NOW(),NOW(),3);
UNLOCK TABLES;


--
-- Dumping data for table `lams_organisation`
--

LOCK TABLES `lams_organisation` WRITE;
INSERT INTO `lams_organisation` VALUES (1,'Root',NULL,'Root Organisation',NULL,1,NOW(),1,1,0,0,0,0,0,0,1,1,0,NULL,NULL),(2,'Playpen','PP101','Developers Playpen',1,2,NOW(),1,1,0,0,0,0,1,1,1,1,0,NULL,NULL),(3,'Everybody',NULL,'All People In Course',2,3,NOW(),1,1,0,0,0,0,0,0,1,1,0,NULL,NULL),(4,'Mathematics 1','MATH111','Mathematics 1',1,2,NOW(),1,1,0,0,0,0,1,1,1,1,0,NULL,NULL),(5,'Tutorial Group A','TUTA','Tutorial Group A',4,3,NOW(),1,1,0,0,0,0,0,0,1,1,0,NULL,NULL),(6,'Tutorial Group B','TUTB','Tutorial Group B',4,3,NOW(),1,1,0,0,0,0,0,0,1,1,0,NULL,NULL),(7,'Moodle','Moodle','Moodle Test',1,2,NOW(),1,2,0,0,0,0,0,0,1,1,0,NULL,NULL);
UNLOCK TABLES;

--
-- Dumping data for table `lams_user`
--

LOCK TABLES `lams_user` WRITE;
INSERT INTO `lams_user` VALUES (1,'sysadmin','a159b7ae81ba3552af61e9731b20870515944538',NULL,0,NULL,'The','System','Administrator',NULL,NULL,NULL,'Sydney','NSW',NULL,'AU',NULL,NULL,NULL,NULL,'sysadmin@email.com',1,0,NOW(),1,45,1,1,NULL,0,'Australia/Sydney',1,NULL,NULL,NULL,NULL),(2,'test','a94a8fe5ccb19ba61c4c0873d391e987982fbbd3',NULL,0,NULL,'Dr','Testing','LDAP',NULL,NULL,NULL,'Sydney','NSW',NULL,'AU',NULL,NULL,NULL,NULL,'test@email.com',1,0,NOW(),3,NULL,1,3,NULL,0,'Australia/Sydney',1,NULL,NULL,NULL,NULL),(4,'mmm','91223fd10ce86fc852b449583aa2196c304bf6e0',NULL,0,NULL,'Ms','Mary','Morgan','99','First Ave',NULL,'Sydney','NSW',NULL,'AU','0295099999','0298939999','0499999999','0299999999','mmmmmmm@email.com',1,0,NOW(),1,4,1,1,NULL,0,'Australia/Sydney',1,NULL,NULL,NULL,NULL),(5,'test1','b444ac06613fc8d63795be9ad0beaf55011936ac',NULL,0,NULL,'Dr','One','Test','1','Test Ave',NULL,'Sydney','NSW',NULL,'AU','0211111111','0211111112','0411111111','0211111113','test1@email.com',1,0,NOW(),1,5,1,1,NULL,0,'Australia/Sydney',1,NULL,NULL,NULL,NULL),(6,'test2','109f4b3c50d7b0df729d299bc6f8e9ef9066971f',NULL,0,NULL,'Dr','Two','Test','2','Test Ave',NULL,'Sydney','NSW',NULL,'AU','0211111111','0211111112','0411111111','0211111113','test2@email.com',1,0,NOW(),1,6,1,1,NULL,0,'Australia/Sydney',1,NULL,NULL,NULL,NULL),(7,'test3','3ebfa301dc59196f18593c45e519287a23297589',NULL,0,NULL,'Dr','Three','Test','3','Test Ave',NULL,'Sydney','NSW',NULL,'AU','0211111111','0211111112','0411111111','0211111113','test3@email.com',1,0,NOW(),1,7,1,1,NULL,0,'Australia/Sydney',1,NULL,NULL,NULL,NULL),(8,'test4','1ff2b3704aede04eecb51e50ca698efd50a1379b',NULL,0,NULL,'Dr','Four','Test','4','Test Ave',NULL,'Sydney','NSW',NULL,'AU','0211111111','0211111112','0411111111','0211111113','test4@email.com',1,0,NOW(),1,8,1,1,NULL,0,'Australia/Sydney',1,NULL,NULL,NULL,NULL);
UNLOCK TABLES;

--
-- Dumping data for table `lams_user_organisation`
--

LOCK TABLES `lams_user_organisation` WRITE;
INSERT INTO `lams_user_organisation` VALUES (1,1,1),(2,2,2),(4,2,4),(5,2,5),(6,2,6),(7,2,7),(8,2,8),(9,3,2),(11,3,4),(12,3,5),(13,3,6),(14,3,7),(15,3,8),(16,4,2),(18,4,4),(19,4,5),(20,4,6),(21,4,7),(22,4,8),(23,5,2),(25,5,4),(26,5,5),(27,5,6),(28,6,7),(29,6,8);
UNLOCK TABLES;


--
-- Dumping data for table `lams_user_organisation_role`
--

LOCK TABLES `lams_user_organisation_role` WRITE;
INSERT INTO `lams_user_organisation_role` VALUES (1,1,1),(2,2,3),(4,4,3),(5,5,3),(6,6,3),(7,7,3),(8,8,3),(9,2,4),(11,4,4),(12,5,4),(13,6,4),(14,7,4),(15,8,4),(16,2,5),(18,4,5),(19,5,5),(20,6,5),(21,7,5),(22,8,5),(23,9,4),(25,11,4),(26,12,4),(27,13,4),(28,14,4),(29,16,4),(30,9,5),(32,11,5),(33,12,5),(34,13,5),(35,14,5),(36,15,5),(37,22,2),(38,16,3),(40,18,3),(41,19,3),(42,20,3),(43,21,3),(44,22,3),(45,16,4),(47,18,4),(48,19,4),(49,20,4),(50,21,4),(51,22,4),(52,16,5),(54,18,5),(55,19,5),(56,20,5),(57,21,5),(58,22,5),(60,23,4),(62,25,4),(63,26,4),(64,27,4),(65,28,4),(66,29,4),(67,23,5),(69,25,5),(70,26,5),(71,27,5),(72,28,5),(73,29,5),(74,18,2);
UNLOCK TABLES;

SET FOREIGN_KEY_CHECKS=1;
