-- plan_item 表新增字段：同步课程的开课时间和教练姓名
-- 执行方式：在 Navicat 中对 gym 数据库执行以下 SQL
ALTER TABLE plan_item
    ADD COLUMN scheduled_time DATETIME NULL COMMENT '计划执行时间（同步自课程开课时间）' AFTER completed,
    ADD COLUMN coach_name     VARCHAR(50) NULL COMMENT '教练姓名（同步自课程）' AFTER scheduled_time;
