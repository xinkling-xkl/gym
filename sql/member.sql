/*
 Navicat Premium Dump SQL

 Source Server         : next
 Source Server Type    : MySQL
 Source Server Version : 80046 (8.0.46)
 Source Host           : localhost:3307
 Source Schema         : gym

 Target Server Type    : MySQL
 Target Server Version : 80046 (8.0.46)
 File Encoding         : 65001

 Date: 10/08/2026 22:11:17
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for member
-- ----------------------------
DROP TABLE IF EXISTS `member`;
CREATE TABLE `member`  (
  `member_account` int NOT NULL COMMENT '会员账号',
  `member_password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会员密码',
  `member_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会员姓名',
  `member_gender` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '会员性别',
  `member_age` int NULL DEFAULT NULL COMMENT '会员年龄',
  `member_height` int NULL DEFAULT NULL COMMENT '会员身高',
  `member_weight` int NULL DEFAULT NULL COMMENT '会员体重',
  `member_phone` bigint NULL DEFAULT NULL COMMENT '会员电话',
  `card_time` date NULL DEFAULT NULL COMMENT '办卡时间/卡片状态',
  `card_class` int NULL DEFAULT NULL COMMENT '会员卡种/课程类别',
  `card_next_class` int NULL DEFAULT NULL COMMENT '剩余课程/下节课程',
  `card_expire_date` date NULL DEFAULT NULL COMMENT '会员卡到期日期',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像URL',
  PRIMARY KEY (`member_account`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '会员表' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
