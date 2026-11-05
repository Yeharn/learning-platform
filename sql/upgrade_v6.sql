-- ============================================================
-- upgrade_v6.sql：定时任务独立为 lp-job 服务
--   1. sys_job 增加上次运行时间 / 上次运行状态
--   2. 新增任务运行日志表 sys_job_log（每次执行 = 一个批次）
--   3. 内置任务切换为 lp-job 处理器（运行日志清理 / 服务健康巡检）
--   4. 新增「任务运行日志」隐藏菜单与按钮权限
-- @Author: Yeharn
-- @Date:2026-11-05
-- 适用：已按 lp.sql 初始化的库；脚本可重复执行
-- ============================================================

USE learning_platform;

-- ---------- 1. sys_job 运行状态字段（幂等） ----------
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS 
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_job' AND COLUMN_NAME = 'last_run_time');
SET @s := IF(@c = 0,
  'ALTER TABLE `sys_job` ADD COLUMN `last_run_time` datetime NULL COMMENT ''上次运行时间'' AFTER `status`',
  'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS 
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_job' AND COLUMN_NAME = 'last_status');
SET @s := IF(@c = 0,
  'ALTER TABLE `sys_job` ADD COLUMN `last_status` char(1) NULL COMMENT ''上次运行状态(0成功 1失败)'' AFTER `last_run_time`',
  'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------- 2. 任务运行日志表 ----------
CREATE TABLE IF NOT EXISTS `sys_job_log` (
  `log_id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '批次号',
  `job_id`          bigint       NOT NULL COMMENT '任务ID',
  `job_name`        varchar(64)  DEFAULT '' COMMENT '任务名称',
  `job_bean`        varchar(100) DEFAULT '' COMMENT '处理器Bean名称',
  `job_param`       varchar(255) DEFAULT '' COMMENT '运行参数',
  `cron_expression` varchar(64)  DEFAULT '' COMMENT 'cron表达式',
  `trigger_type`    char(1)      DEFAULT '0' COMMENT '触发方式(0自动 1手动)',
  `trigger_by`      varchar(64)  DEFAULT 'system' COMMENT '触发人',
  `status`          char(1)      DEFAULT '0' COMMENT '运行状态(0成功 1失败)',
  `start_time`      datetime     DEFAULT NULL COMMENT '开始时间',
  `end_time`        datetime     DEFAULT NULL COMMENT '结束时间',
  `cost_time`       bigint       DEFAULT 0 COMMENT '耗时(毫秒)',
  `result`          varchar(2000) DEFAULT NULL COMMENT '执行结果',
  `error_msg`       varchar(2000) DEFAULT NULL COMMENT '异常信息',
  `create_time`     datetime     DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_job_id` (`job_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='定时任务运行日志表';

-- ---------- 3. 内置任务切换为 lp-job 处理器 ----------
UPDATE `sys_job` SET `job_name` = '运行日志清理', `job_bean` = 'jobLogCleanupJob', `job_param` = '30',
       `cron_expression` = '0 0 3 * * ?', `status` = '0',
       `remark` = '每天 03:00 清理 30 天前的任务运行日志'
WHERE `job_id` = 1;

UPDATE `sys_job` SET `job_name` = '服务健康巡检', `job_bean` = 'serviceHealthCheckJob', `job_param` = '',
       `cron_expression` = '0 */10 * * * ?', `status` = '0',
       `remark` = '每 10 分钟巡检注册中心全部实例健康状态'
WHERE `job_id` = 2;

-- ---------- 4. 菜单：任务运行日志（隐藏路由，供「查看批次」跳转） ----------
INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query_param`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`remark`,`create_by`,`create_time`)
SELECT 303,'任务运行日志',3,4,'job-log','tool/job/log',NULL,0,1,'C','1','0','tool:job:list','Document','定时任务运行批次（由任务列表跳转，默认隐藏）','admin',NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 303);

INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query_param`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`remark`,`create_by`,`create_time`)
SELECT 3030,'运行日志查询',303,1,'','',NULL,0,1,'F','0','0','tool:job:list',NULL,NULL,'admin',NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 3030);

-- 超管授权（普通角色如需查看可由管理员在角色菜单中勾选）
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`)
SELECT 1, m.menu_id FROM `sys_menu` m
WHERE m.menu_id IN (303, 3030)
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);
