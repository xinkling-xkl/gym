/*
 Navicat Premium Dump SQL

 Source Server         : taffy
 Source Server Type    : MySQL
 Source Server Version : 90001 (9.0.1)
 Source Host           : localhost:3306
 Source Schema         : gym

 Target Server Type    : MySQL
 Target Server Version : 90001 (9.0.1)
 File Encoding         : 65001

 Date: 23/07/2026
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for check_in
-- ----------------------------
DROP TABLE IF EXISTS `check_in`;
CREATE TABLE `check_in` (
  `check_in_id` int NOT NULL AUTO_INCREMENT COMMENT '签到ID',
  `member_account` int NOT NULL COMMENT '会员账号',
  `member_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会员姓名',
  `class_order_id` int NULL DEFAULT NULL COMMENT '关联预约ID（可为空）',
  `check_in_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '签到时间',
  `check_in_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT 'GYM' COMMENT '签到类型: CLASS-上课, GYM-自主训练',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`check_in_id`) USING BTREE,
  INDEX `idx_member_account`(`member_account`) USING BTREE,
  INDEX `idx_check_in_time`(`check_in_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '签到记录表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
