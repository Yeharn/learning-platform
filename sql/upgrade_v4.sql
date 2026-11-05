-- ----------------------------------------------------------------------------
-- v4 增量升级：流程引擎由 FlowLong 迁移至 Warm-Flow 1.8.9
-- 在项目根目录执行：mysql -uroot -p learning_platform < sql/upgrade_v4.sql
-- ----------------------------------------------------------------------------

-- 1. warm-flow 引擎表（flow_definition / flow_node / flow_skip / flow_instance /
--    flow_task / flow_his_task / flow_user / flow_form）已在 sql/warm-flow/warm-flow-mysql.sql，
--    请先执行：mysql -uroot -p learning_platform < sql/warm-flow/warm-flow-mysql.sql
--    （mysql 客户端非交互模式不支持 SOURCE，故拆分为两个脚本）

-- 2. 表单模板（可视化表单设计器产物，流程定义通过 form_path 引用 form_code）
CREATE TABLE IF NOT EXISTS `lp_flow_form` (
  `id` bigint NOT NULL COMMENT '主键',
  `form_code` varchar(64) NOT NULL COMMENT '表单编码',
  `form_name` varchar(100) NOT NULL COMMENT '表单名称',
  `fields_json` longtext COMMENT '字段定义JSON',
  `ext_json` longtext COMMENT '附加参数JSON（发起时可透传的扩展字段定义）',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '1' COMMENT '版本号',
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `del_flag` char(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_form_code` (`form_code`)
) ENGINE = InnoDB COMMENT ='流程表单模板';

-- 3. 已阅记录（warm-flow 无已阅语义，由业务侧留痕）
CREATE TABLE IF NOT EXISTS `lp_flow_read` (
  `id` bigint NOT NULL,
  `instance_id` bigint NOT NULL COMMENT '流程实例id',
  `task_id` bigint DEFAULT NULL COMMENT '任务id',
  `user_id` varchar(64) DEFAULT NULL COMMENT '用户id',
  `user_name` varchar(64) DEFAULT NULL COMMENT '用户名称',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_read` (`instance_id`, `user_id`)
) ENGINE = InnoDB COMMENT ='流程已阅记录';

-- 4. 流程沟通评论（沿用 v3，存在则跳过）
CREATE TABLE IF NOT EXISTS `lp_flow_comment` (
  `id` bigint NOT NULL,
  `instance_id` bigint NOT NULL,
  `task_id` bigint DEFAULT NULL,
  `user_id` varchar(64) DEFAULT NULL,
  `user_name` varchar(64) DEFAULT NULL,
  `content` varchar(1000) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_instance` (`instance_id`)
) ENGINE = InnoDB COMMENT ='流程沟通评论';

-- 5. 菜单补充：流程定义页新增「表单设计」权限，任务页新增「流程监控」入口
DELETE FROM `sys_menu` WHERE `menu_id` IN (3314, 3315);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `perms`, `icon`, `visible`, `status`, `create_by`, `create_time`, `remark`)
VALUES (3314, '表单设计', 3301, 5, NULL, NULL, 'F', 'tool:flow:form', '#', 0, 0, 'admin', NOW(), '流程表单模板设计'),
       (3315, '流程监控', 3302, 5, NULL, NULL, 'F', 'tool:flow:monitor', '#', 0, 0, 'admin', NOW(), '流程实例强制跳转与终止');

-- 6. 角色菜单授权：超级管理员角色自动拥有新权限（若使用菜单表驱动鉴权）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 3314 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 3314);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 3315 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 1 AND `menu_id` = 3315);
