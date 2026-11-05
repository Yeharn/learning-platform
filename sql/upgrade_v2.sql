/*
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: v2 增量升级脚本：文件/消息/定时任务/流程菜单与表结构
 */

-- ============================================================
-- Learning Platform 升级脚本 v2
-- 文件服务 / 消息中心 / 定时任务 / 慢SQL治理菜单 / 按月分表基础
-- 可重复执行（幂等）
-- ============================================================

-- ----------------------------
-- 文件表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_file` (
  `file_id`       bigint       NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `category`      varchar(20)  NOT NULL DEFAULT 'file' COMMENT '分类(image图片/video视频/file文件)',
  `storage_type`  varchar(20)  NOT NULL DEFAULT 'local' COMMENT '存储类型(local本地/minio/rustfs)',
  `original_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `file_name`     varchar(255) NOT NULL COMMENT '存储文件名',
  `file_path`     varchar(500) NOT NULL COMMENT '存储路径/对象Key',
  `url`           varchar(500) DEFAULT NULL COMMENT '访问地址',
  `file_size`     bigint       DEFAULT 0 COMMENT '大小(字节)',
  `file_type`     varchar(100) DEFAULT NULL COMMENT 'MIME类型',
  `create_by`     varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`   datetime     DEFAULT NULL COMMENT '创建时间',
  `update_by`     varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`   datetime     DEFAULT NULL COMMENT '更新时间',
  `remark`        varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag`      char(1)      DEFAULT '0' COMMENT '删除标志(0存在 2删除)',
  PRIMARY KEY (`file_id`),
  KEY `idx_file_category` (`category`),
  KEY `idx_file_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件表';

-- ----------------------------
-- 站内消息表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_message` (
  `message_id`    bigint       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `title`         varchar(200) NOT NULL COMMENT '标题',
  `content`       text         COMMENT '内容',
  `msg_type`      varchar(20)  DEFAULT 'notice' COMMENT '类型(notice系统通知/announce公告/task任务提醒)',
  `sender_id`     bigint       DEFAULT NULL COMMENT '发送人ID',
  `sender_name`   varchar(64)  DEFAULT NULL COMMENT '发送人',
  `receiver_id`   bigint       NOT NULL COMMENT '接收人ID',
  `receiver_name` varchar(64)  DEFAULT NULL COMMENT '接收人',
  `read_flag`     char(1)      DEFAULT '0' COMMENT '读取标志(0未读 1已读)',
  `read_time`     datetime     DEFAULT NULL COMMENT '阅读时间',
  `create_by`     varchar(64)  DEFAULT '' COMMENT '创建者',
  `create_time`   datetime     DEFAULT NULL COMMENT '创建时间',
  `update_by`     varchar(64)  DEFAULT '' COMMENT '更新者',
  `update_time`   datetime     DEFAULT NULL COMMENT '更新时间',
  `remark`        varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag`      char(1)      DEFAULT '0' COMMENT '删除标志(0存在 2删除)',
  PRIMARY KEY (`message_id`),
  KEY `idx_message_receiver` (`receiver_id`, `read_flag`),
  KEY `idx_message_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息表';

-- ----------------------------
-- 内置定时任务表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_job` (
  `job_id`          bigint      NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `job_name`        varchar(64) NOT NULL COMMENT '任务名称',
  `job_bean`        varchar(100) NOT NULL COMMENT '处理器Bean名称(实现LpJob接口)',
  `job_param`       varchar(255) DEFAULT '' COMMENT '任务参数',
  `cron_expression` varchar(64) NOT NULL COMMENT 'cron表达式',
  `status`          char(1)     DEFAULT '0' COMMENT '状态(0正常 1暂停)',
  `remark`          varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by`       varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time`     datetime    DEFAULT NULL COMMENT '创建时间',
  `update_by`       varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time`     datetime    DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内置定时任务表';

-- ----------------------------
-- 模式配置参数
-- ----------------------------
INSERT INTO `sys_config` (`config_name`,`config_key`,`config_value`,`config_type`,`remark`,`create_by`,`create_time`)
SELECT '文件服务-存储模式','sys.file.mode','local','Y','文件存储模式(local本地/minio/rustfs)','admin',NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key`='sys.file.mode');

INSERT INTO `sys_config` (`config_name`,`config_key`,`config_value`,`config_type`,`remark`,`create_by`,`create_time`)
SELECT '消息中心-推送模式','sys.message.mode','inner','Y','消息推送模式(inner站内信/email邮件/websocket实时推送)','admin',NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_config` WHERE `config_key`='sys.message.mode');

-- ----------------------------
-- 内置任务种子
-- ----------------------------
INSERT INTO `sys_job` (`job_id`,`job_name`,`job_bean`,`job_param`,`cron_expression`,`status`,`remark`,`create_by`,`create_time`)
SELECT 1,'日志月表预创建','logTablePrepareJob','','0 0 1 25 * ?','0','每月25日预创建下月日志分表','admin',NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_job` WHERE `job_id`=1);

INSERT INTO `sys_job` (`job_id`,`job_name`,`job_bean`,`job_param`,`cron_expression`,`status`,`remark`,`create_by`,`create_time`)
SELECT 2,'过期消息清理','messageCleanupJob','30','0 30 2 * * ?','0','每天02:30清理30天前的已读消息','admin',NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_job` WHERE `job_id`=2);

-- ----------------------------
-- 新增菜单：文件管理 / 消息中心 / 定时任务 / 流程管理 / 慢SQL治理
-- ----------------------------
INSERT IGNORE INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query_param`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`remark`,`create_by`,`create_time`) VALUES
(108,'文件管理',1,9,'file','system/file/index',NULL,0,1,'C','0','0','system:file:list','FolderOpened',NULL,'admin',NOW()),
(109,'消息中心',1,10,'message','system/message/index',NULL,0,1,'C','0','0','system:message:list','ChatDotRound',NULL,'admin',NOW()),
(301,'定时任务',3,2,'job','tool/job/index',NULL,0,1,'C','0','0','tool:job:list','Timer',NULL,'admin',NOW()),
(302,'流程管理',3,3,'flow','tool/flow/index',NULL,0,1,'C','0','0','tool:flow:list','Guide',NULL,'admin',NOW()),
(1080,'文件上传',108,1,'','',NULL,0,1,'F','0','0','system:file:upload',NULL,NULL,'admin',NOW()),
(1081,'文件删除',108,2,'','',NULL,0,1,'F','0','0','system:file:remove',NULL,NULL,'admin',NOW()),
(1082,'存储模式切换',108,3,'','',NULL,0,1,'F','0','0','system:file:mode',NULL,NULL,'admin',NOW()),
(1090,'消息发送',109,1,'','',NULL,0,1,'F','0','0','system:message:send',NULL,NULL,'admin',NOW()),
(1091,'消息删除',109,2,'','',NULL,0,1,'F','0','0','system:message:remove',NULL,NULL,'admin',NOW()),
(3010,'任务查询',301,1,'','',NULL,0,1,'F','0','0','tool:job:query',NULL,NULL,'admin',NOW()),
(3011,'任务新增',301,2,'','',NULL,0,1,'F','0','0','tool:job:add',NULL,NULL,'admin',NOW()),
(3012,'任务修改',301,3,'','',NULL,0,1,'F','0','0','tool:job:edit',NULL,NULL,'admin',NOW()),
(3013,'任务删除',301,4,'','',NULL,0,1,'F','0','0','tool:job:remove',NULL,NULL,'admin',NOW()),
(3014,'执行一次',301,5,'','',NULL,0,1,'F','0','0','tool:job:run',NULL,NULL,'admin',NOW()),
(3015,'任务启停',301,6,'','',NULL,0,1,'F','0','0','tool:job:changeStatus',NULL,NULL,'admin',NOW()),
(3020,'流程部署',302,1,'','',NULL,0,1,'F','0','0','tool:flow:deploy',NULL,NULL,'admin',NOW()),
(3021,'流程发起',302,2,'','',NULL,0,1,'F','0','0','tool:flow:start',NULL,NULL,'admin',NOW()),
(3022,'流程办理',302,3,'','',NULL,0,1,'F','0','0','tool:flow:execute',NULL,NULL,'admin',NOW()),
(3023,'流程删除',302,4,'','',NULL,0,1,'F','0','0','tool:flow:remove',NULL,NULL,'admin',NOW()),
(1151,'慢SQL治理',205,1,'','',NULL,0,1,'F','0','0','monitor:mysql:kill',NULL,NULL,'admin',NOW());

-- 超级管理员拥有全部菜单
INSERT IGNORE INTO `sys_role_menu` (`role_id`,`menu_id`) SELECT 1, `menu_id` FROM `sys_menu`;
-- 普通用户：消息中心（收通知）
INSERT IGNORE INTO `sys_role_menu` (`role_id`,`menu_id`) VALUES (2,109);
-- 运维人员：MySQL 慢SQL治理
INSERT IGNORE INTO `sys_role_menu` (`role_id`,`menu_id`) VALUES (3,1151);
