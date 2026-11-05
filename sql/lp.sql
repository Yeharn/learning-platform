/*
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 数据库初始化脚本：建库建表与菜单/角色/用户等基础数据
 */

-- ============================================================
--  Learning Platform 微服务权限管理系统 数据库脚本
--  MySQL 8.0+ / 9.x
--  默认账号: root / MySQL@ZnSoft
--  admin / admin123      test / admin123
-- ============================================================
DROP DATABASE IF EXISTS `learning_platform`;
CREATE DATABASE `learning_platform` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `learning_platform`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 部门表
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
  `dept_id`     bigint       NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `parent_id`   bigint       NOT NULL DEFAULT 0 COMMENT '父部门ID',
  `ancestors`   varchar(255) NOT NULL DEFAULT '' COMMENT '祖级列表',
  `dept_name`   varchar(64)  NOT NULL COMMENT '部门名称',
  `order_num`   int          NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `leader`      varchar(32)  DEFAULT NULL COMMENT '负责人',
  `phone`       varchar(20)  DEFAULT NULL COMMENT '联系电话',
  `email`       varchar(64)  DEFAULT NULL COMMENT '邮箱',
  `status`      char(1)      NOT NULL DEFAULT '0' COMMENT '状态(0正常 1停用)',
  `del_flag`    char(1)      NOT NULL DEFAULT '0' COMMENT '删除标志(0存在 2删除)',
  `create_by`   varchar(64)  DEFAULT NULL,
  `create_time` datetime     DEFAULT NULL,
  `update_by`   varchar(64)  DEFAULT NULL,
  `update_time` datetime     DEFAULT NULL,
  PRIMARY KEY (`dept_id`),
  KEY `idx_dept_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- ----------------------------
-- 用户表
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `user_id`     bigint       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `dept_id`     bigint       DEFAULT NULL COMMENT '部门ID',
  `username`    varchar(64)  NOT NULL COMMENT '登录账号',
  `nickname`    varchar(64)  NOT NULL COMMENT '用户昵称',
  `password`    varchar(128) NOT NULL COMMENT '密码(BCrypt)',
  `user_type`   varchar(8)   NOT NULL DEFAULT '00' COMMENT '用户类型(00系统用户)',
  `email`       varchar(64)  DEFAULT NULL COMMENT '邮箱',
  `phone`       varchar(20)  DEFAULT NULL COMMENT '手机号',
  `sex`         char(1)      NOT NULL DEFAULT '2' COMMENT '性别(0男 1女 2未知)',
  `avatar`      varchar(512) DEFAULT NULL COMMENT '头像',
  `status`      char(1)      NOT NULL DEFAULT '0' COMMENT '状态(0正常 1停用)',
  `login_ip`    varchar(64)  DEFAULT NULL COMMENT '最后登录IP',
  `login_date`  datetime     DEFAULT NULL COMMENT '最后登录时间',
  `remark`      varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag`    char(1)      NOT NULL DEFAULT '0' COMMENT '删除标志(0存在 2删除)',
  `create_by`   varchar(64)  DEFAULT NULL,
  `create_time` datetime     DEFAULT NULL,
  `update_by`   varchar(64)  DEFAULT NULL,
  `update_time` datetime     DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_user_username` (`username`),
  KEY `idx_user_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------
-- 角色表
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `role_id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name`          varchar(64)  NOT NULL COMMENT '角色名称',
  `role_key`           varchar(100) NOT NULL COMMENT '角色权限字符串',
  `role_sort`          int          NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `data_scope`         char(1)      NOT NULL DEFAULT '1' COMMENT '数据范围(1全部 2自定义 3本部门 4本部门及以下 5仅本人)',
  `menu_check_strictly` tinyint(1)  NOT NULL DEFAULT 1 COMMENT '菜单树选择项是否父子联动',
  `dept_check_strictly` tinyint(1)  NOT NULL DEFAULT 1 COMMENT '部门树选择项是否父子联动',
  `status`             char(1)      NOT NULL DEFAULT '0' COMMENT '状态(0正常 1停用)',
  `del_flag`           char(1)      NOT NULL DEFAULT '0' COMMENT '删除标志(0存在 2删除)',
  `remark`             varchar(500) DEFAULT NULL,
  `create_by`          varchar(64)  DEFAULT NULL,
  `create_time`        datetime     DEFAULT NULL,
  `update_by`          varchar(64)  DEFAULT NULL,
  `update_time`        datetime     DEFAULT NULL,
  PRIMARY KEY (`role_id`),
  UNIQUE KEY `uk_role_key` (`role_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ----------------------------
-- 菜单/权限表
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `menu_id`     bigint       NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name`   varchar(64)  NOT NULL COMMENT '菜单名称',
  `parent_id`   bigint       NOT NULL DEFAULT 0 COMMENT '父菜单ID',
  `order_num`   int          NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `path`        varchar(200) DEFAULT '' COMMENT '路由地址',
  `component`   varchar(255) DEFAULT NULL COMMENT '组件路径',
  `query_param` varchar(255) DEFAULT NULL COMMENT '路由参数',
  `is_frame`    tinyint(1)   NOT NULL DEFAULT 0 COMMENT '是否外链(0否 1是)',
  `is_cache`    tinyint(1)   NOT NULL DEFAULT 1 COMMENT '是否缓存(0否 1是)',
  `menu_type`   char(1)      NOT NULL DEFAULT 'M' COMMENT '菜单类型(M目录 C菜单 F按钮)',
  `visible`     char(1)      NOT NULL DEFAULT '0' COMMENT '显示状态(0显示 1隐藏)',
  `status`      char(1)      NOT NULL DEFAULT '0' COMMENT '菜单状态(0正常 1停用)',
  `perms`       varchar(150) DEFAULT NULL COMMENT '权限标识',
  `icon`        varchar(100) DEFAULT NULL COMMENT '菜单图标',
  `remark`      varchar(500) DEFAULT NULL,
  `create_by`   varchar(64)  DEFAULT NULL,
  `create_time` datetime     DEFAULT NULL,
  `update_by`   varchar(64)  DEFAULT NULL,
  `update_time` datetime     DEFAULT NULL,
  PRIMARY KEY (`menu_id`),
  KEY `idx_menu_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';

-- ----------------------------
-- 关联表
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint NOT NULL,
  `menu_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept` (
  `role_id` bigint NOT NULL,
  `dept_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`, `dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色部门关联表(数据权限)';

-- ----------------------------
-- 字典类型
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_type`;
CREATE TABLE `sys_dict_type` (
  `dict_id`     bigint       NOT NULL AUTO_INCREMENT COMMENT '字典ID',
  `dict_name`   varchar(100) NOT NULL COMMENT '字典名称',
  `dict_type`   varchar(100) NOT NULL COMMENT '字典类型',
  `status`      char(1)      NOT NULL DEFAULT '0' COMMENT '状态(0正常 1停用)',
  `remark`      varchar(500) DEFAULT NULL,
  `create_by`   varchar(64)  DEFAULT NULL,
  `create_time` datetime     DEFAULT NULL,
  `update_by`   varchar(64)  DEFAULT NULL,
  `update_time` datetime     DEFAULT NULL,
  PRIMARY KEY (`dict_id`),
  UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data` (
  `dict_code`   bigint       NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  `dict_sort`   int          NOT NULL DEFAULT 0 COMMENT '排序',
  `dict_label`  varchar(100) NOT NULL COMMENT '字典标签',
  `dict_value`  varchar(100) NOT NULL COMMENT '字典键值',
  `dict_type`   varchar(100) NOT NULL COMMENT '字典类型',
  `css_class`   varchar(100) DEFAULT NULL COMMENT '样式属性',
  `list_class`  varchar(100) DEFAULT NULL COMMENT '表格回显样式',
  `is_default`  char(1)      NOT NULL DEFAULT 'N' COMMENT '是否默认(Y/N)',
  `status`      char(1)      NOT NULL DEFAULT '0' COMMENT '状态(0正常 1停用)',
  `remark`      varchar(500) DEFAULT NULL,
  `create_by`   varchar(64)  DEFAULT NULL,
  `create_time` datetime     DEFAULT NULL,
  `update_by`   varchar(64)  DEFAULT NULL,
  `update_time` datetime     DEFAULT NULL,
  PRIMARY KEY (`dict_code`),
  KEY `idx_dict_data_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';

-- ----------------------------
-- 参数配置表
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `config_id`    bigint       NOT NULL AUTO_INCREMENT COMMENT '参数ID',
  `config_name`  varchar(100) NOT NULL COMMENT '参数名称',
  `config_key`   varchar(100) NOT NULL COMMENT '参数键名',
  `config_value` varchar(500) NOT NULL COMMENT '参数键值',
  `config_type`  char(1)      NOT NULL DEFAULT 'N' COMMENT '系统内置(Y/N)',
  `remark`       varchar(500) DEFAULT NULL,
  `create_by`    varchar(64)  DEFAULT NULL,
  `create_time`  datetime     DEFAULT NULL,
  `update_by`    varchar(64)  DEFAULT NULL,
  `update_time`  datetime     DEFAULT NULL,
  PRIMARY KEY (`config_id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参数配置表';

-- ----------------------------
-- 通知公告表
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice` (
  `notice_id`      bigint       NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `notice_title`   varchar(100) NOT NULL COMMENT '公告标题',
  `notice_type`    char(1)      NOT NULL DEFAULT '1' COMMENT '公告类型(1通知 2公告)',
  `notice_content` longtext     DEFAULT NULL COMMENT '公告内容',
  `status`         char(1)      NOT NULL DEFAULT '0' COMMENT '状态(0正常 1关闭)',
  `remark`         varchar(500) DEFAULT NULL,
  `create_by`      varchar(64)  DEFAULT NULL,
  `create_time`    datetime     DEFAULT NULL,
  `update_by`      varchar(64)  DEFAULT NULL,
  `update_time`    datetime     DEFAULT NULL,
  PRIMARY KEY (`notice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知公告表';

-- ----------------------------
-- 登录日志
-- ----------------------------
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log` (
  `info_id`        bigint       NOT NULL AUTO_INCREMENT COMMENT '访问ID',
  `username`       varchar(64)  DEFAULT NULL COMMENT '用户账号',
  `ipaddr`         varchar(64)  DEFAULT NULL COMMENT '登录IP',
  `login_location` varchar(255) DEFAULT NULL COMMENT '登录地点',
  `browser`        varchar(64)  DEFAULT NULL COMMENT '浏览器',
  `os`             varchar(64)  DEFAULT NULL COMMENT '操作系统',
  `status`         char(1)      NOT NULL DEFAULT '0' COMMENT '登录状态(0成功 1失败)',
  `msg`            varchar(255) DEFAULT NULL COMMENT '提示消息',
  `login_time`     datetime     DEFAULT NULL COMMENT '登录时间',
  PRIMARY KEY (`info_id`),
  KEY `idx_login_log_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';

-- ----------------------------
-- 操作日志
-- ----------------------------
DROP TABLE IF EXISTS `sys_oper_log`;
CREATE TABLE `sys_oper_log` (
  `oper_id`        bigint        NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `title`          varchar(100)  DEFAULT NULL COMMENT '模块标题',
  `business_type`  int           NOT NULL DEFAULT 0 COMMENT '业务类型(0其它 1新增 2修改 3删除 4授权 5导出 6导入 7强退 8生成代码 9清空)',
  `method`         varchar(255)  DEFAULT NULL COMMENT '方法名称',
  `request_method` varchar(16)   DEFAULT NULL COMMENT '请求方式',
  `operator_type`  int           NOT NULL DEFAULT 0 COMMENT '操作类别(0其它 1后台用户 2手机端用户)',
  `oper_name`      varchar(64)   DEFAULT NULL COMMENT '操作人员',
  `dept_name`      varchar(64)   DEFAULT NULL COMMENT '部门名称',
  `oper_url`       varchar(255)  DEFAULT NULL COMMENT '请求URL',
  `oper_ip`        varchar(64)   DEFAULT NULL COMMENT '主机地址',
  `oper_location`  varchar(255)  DEFAULT NULL COMMENT '操作地点',
  `oper_param`     varchar(2000) DEFAULT NULL COMMENT '请求参数',
  `json_result`    varchar(2000) DEFAULT NULL COMMENT '返回参数',
  `status`         int           NOT NULL DEFAULT 0 COMMENT '操作状态(0正常 1异常)',
  `error_msg`      varchar(2000) DEFAULT NULL COMMENT '错误消息',
  `oper_time`      datetime      DEFAULT NULL COMMENT '操作时间',
  `cost_time`      bigint        DEFAULT 0 COMMENT '消耗时间(毫秒)',
  PRIMARY KEY (`oper_id`),
  KEY `idx_oper_log_time` (`oper_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ----------------------------
-- 多数据源
-- ----------------------------
DROP TABLE IF EXISTS `sys_datasource`;
CREATE TABLE `sys_datasource` (
  `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`          varchar(64)  NOT NULL COMMENT '数据源名称',
  `ds_key`        varchar(64)  NOT NULL COMMENT '数据源标识',
  `db_type`       varchar(32)  NOT NULL DEFAULT 'mysql' COMMENT '数据库类型',
  `driver_class`  varchar(128) DEFAULT NULL COMMENT '驱动类',
  `url`           varchar(500) NOT NULL COMMENT '连接地址',
  `username`      varchar(100) NOT NULL COMMENT '用户名',
  `password`      varchar(300) DEFAULT NULL COMMENT '密码(AES加密)',
  `status`        char(1)      NOT NULL DEFAULT '0' COMMENT '状态(0正常 1停用)',
  `is_default`    char(1)      NOT NULL DEFAULT 'N' COMMENT '是否主数据源(Y/N)',
  `remark`        varchar(500) DEFAULT NULL,
  `create_by`     varchar(64)  DEFAULT NULL,
  `create_time`   datetime     DEFAULT NULL,
  `update_by`     varchar(64)  DEFAULT NULL,
  `update_time`   datetime     DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ds_key` (`ds_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多数据源配置表';

-- ----------------------------
-- 代码生成业务表
-- ----------------------------
DROP TABLE IF EXISTS `gen_table`;
CREATE TABLE `gen_table` (
  `table_id`        bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_name`      varchar(200) DEFAULT '' COMMENT '表名称',
  `table_comment`   varchar(500) DEFAULT '' COMMENT '表描述',
  `class_name`      varchar(100) DEFAULT '' COMMENT '实体类名称',
  `tpl_category`    varchar(200) DEFAULT 'crud' COMMENT '模板类型(crud/tree)',
  `package_name`    varchar(100) DEFAULT NULL COMMENT '生成包路径',
  `module_name`     varchar(30)  DEFAULT NULL COMMENT '生成模块名',
  `business_name`   varchar(30)  DEFAULT NULL COMMENT '生成业务名',
  `function_name`   varchar(50)  DEFAULT NULL COMMENT '生成功能名',
  `function_author` varchar(50)  DEFAULT NULL COMMENT '生成作者',
  `gen_type`        char(1)      DEFAULT '0' COMMENT '生成代码方式(0zip压缩包 1自定义路径)',
  `gen_path`        varchar(200) DEFAULT '/' COMMENT '生成路径',
  `options`         varchar(1000) DEFAULT NULL COMMENT '其它生成选项',
  `ds_key`          varchar(64)  DEFAULT 'master' COMMENT '数据源标识',
  `remark`          varchar(500) DEFAULT NULL,
  `create_by`       varchar(64)  DEFAULT NULL,
  `create_time`     datetime     DEFAULT NULL,
  `update_by`       varchar(64)  DEFAULT NULL,
  `update_time`     datetime     DEFAULT NULL,
  PRIMARY KEY (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成业务表';

DROP TABLE IF EXISTS `gen_table_column`;
CREATE TABLE `gen_table_column` (
  `column_id`      bigint       NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_id`       bigint       DEFAULT NULL COMMENT '归属表编号',
  `column_name`    varchar(200) DEFAULT NULL COMMENT '列名称',
  `column_comment` varchar(500) DEFAULT NULL COMMENT '列描述',
  `column_type`    varchar(100) DEFAULT NULL COMMENT '列类型',
  `java_type`      varchar(500) DEFAULT NULL COMMENT 'JAVA类型',
  `java_field`     varchar(200) DEFAULT NULL COMMENT 'JAVA字段名',
  `is_pk`          char(1)      DEFAULT NULL COMMENT '是否主键(1是)',
  `is_increment`   char(1)      DEFAULT NULL COMMENT '是否自增(1是)',
  `is_required`    char(1)      DEFAULT NULL COMMENT '是否必填(1是)',
  `is_insert`      char(1)      DEFAULT NULL COMMENT '是否为插入字段(1是)',
  `is_edit`        char(1)      DEFAULT NULL COMMENT '是否编辑字段(1是)',
  `is_list`        char(1)      DEFAULT NULL COMMENT '是否列表字段(1是)',
  `is_query`       char(1)      DEFAULT NULL COMMENT '是否查询字段(1是)',
  `query_type`     varchar(200) DEFAULT 'EQ' COMMENT '查询方式(EQ/NE/GT/LT/LIKE/BETWEEN)',
  `html_type`      varchar(200) DEFAULT 'input' COMMENT '显示类型(input/textarea/select/radio/checkbox/datetime/image)',
  `dict_type`      varchar(200) DEFAULT '' COMMENT '字典类型',
  `sort`           int          DEFAULT NULL COMMENT '排序',
  `create_by`      varchar(64)  DEFAULT NULL,
  `create_time`    datetime     DEFAULT NULL,
  `update_by`      varchar(64)  DEFAULT NULL,
  `update_time`    datetime     DEFAULT NULL,
  PRIMARY KEY (`column_id`),
  KEY `idx_gen_column_table` (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成业务表字段';

-- ----------------------------
-- 文件表
-- ----------------------------
DROP TABLE IF EXISTS `sys_file`;
CREATE TABLE `sys_file` (
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
DROP TABLE IF EXISTS `sys_message`;
CREATE TABLE `sys_message` (
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
DROP TABLE IF EXISTS `sys_job`;
CREATE TABLE `sys_job` (
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

-- ============================================================
-- 初始化数据
-- ============================================================
INSERT INTO `sys_dept` (`dept_id`,`parent_id`,`ancestors`,`dept_name`,`order_num`,`leader`,`phone`,`email`,`status`,`del_flag`,`create_by`,`create_time`) VALUES
(100, 0,   '0',       '星溯智能科技', 0, '叶寒', '13800000000', 'admin@yeharn.org', '0', '0', 'admin', NOW()),
(101, 100, '0,100',   '研发部',       1, '张三', '13800000001', 'dev@yeharn.org',   '0', '0', 'admin', NOW()),
(102, 100, '0,100',   '市场部',       2, '李四', '13800000002', 'mkt@yeharn.org',   '0', '0', 'admin', NOW()),
(103, 100, '0,100',   '财务部',       3, '王五', '13800000003', 'fin@yeharn.org',   '0', '0', 'admin', NOW()),
(104, 101, '0,100,101','后端组',      1, '赵六', '13800000004', 'be@yeharn.org',    '0', '0', 'admin', NOW()),
(105, 101, '0,100,101','前端组',      2, '孙七', '13800000005', 'fe@yeharn.org',    '0', '0', 'admin', NOW());

-- 密码均为 admin123
INSERT INTO `sys_user` (`user_id`,`dept_id`,`username`,`nickname`,`password`,`user_type`,`email`,`phone`,`sex`,`avatar`,`status`,`remark`,`del_flag`,`create_by`,`create_time`) VALUES
(1, 101, 'admin', '超级管理员', '$2b$10$PLHWGWKSqrlMmMrXQW79o.n5CHAQFJgWlaobMDalovp2ZqkvDpmQ2', '00', 'admin@yeharn.org', '13800000000', '0', NULL, '0', '系统内置超级管理员', '0', 'admin', NOW()),
(2, 104, 'test',  '测试用户',   '$2b$10$PLHWGWKSqrlMmMrXQW79o.n5CHAQFJgWlaobMDalovp2ZqkvDpmQ2', '00', 'test@yeharn.org',  '13800000006', '1', NULL, '0', '普通演示账号',       '0', 'admin', NOW());

INSERT INTO `sys_role` (`role_id`,`role_name`,`role_key`,`role_sort`,`data_scope`,`menu_check_strictly`,`dept_check_strictly`,`status`,`del_flag`,`remark`,`create_by`,`create_time`) VALUES
(1, '超级管理员', 'admin',  1, '1', 1, 1, '0', '0', '拥有全部权限', 'admin', NOW()),
(2, '普通用户',   'common', 2, '4', 1, 1, '0', '0', '仅可查看用户与日志', 'admin', NOW()),
(3, '运维人员',   'ops',    3, '3', 1, 1, '0', '0', '监控与日志权限', 'admin', NOW());

INSERT INTO `sys_user_role` (`user_id`,`role_id`) VALUES (1,1),(2,2);

INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query_param`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`remark`,`create_by`,`create_time`) VALUES
(1,'系统管理',0,1,'system',NULL,NULL,0,1,'M','0','0',NULL,'Setting',NULL,'admin',NOW()),
(2,'系统监控',0,2,'monitor',NULL,NULL,0,1,'M','0','0',NULL,'Monitor',NULL,'admin',NOW()),
(3,'系统工具',0,3,'tool',NULL,NULL,0,1,'M','0','0',NULL,'Tools',NULL,'admin',NOW()),
(4,'接口文档',0,4,'http://127.0.0.1:8080/doc.html',NULL,NULL,0,1,'C','0','0',NULL,'Document',NULL,'admin',NOW()),

(100,'用户管理',1,1,'user','system/user/index',NULL,0,1,'C','0','0','system:user:list','User',NULL,'admin',NOW()),
(101,'角色管理',1,2,'role','system/role/index',NULL,0,1,'C','0','0','system:role:list','UserFilled',NULL,'admin',NOW()),
(102,'菜单管理',1,3,'menu','system/menu/index',NULL,0,1,'C','0','0','system:menu:list','Menu',NULL,'admin',NOW()),
(103,'部门管理',1,4,'dept','system/dept/index',NULL,0,1,'C','0','0','system:dept:list','OfficeBuilding',NULL,'admin',NOW()),
(104,'字典管理',1,5,'dict','system/dict/index',NULL,0,1,'C','0','0','system:dict:list','Collection',NULL,'admin',NOW()),
(105,'参数设置',1,6,'config','system/config/index',NULL,0,1,'C','0','0','system:config:list','Edit',NULL,'admin',NOW()),
(106,'通知公告',1,7,'notice','system/notice/index',NULL,0,1,'C','0','0','system:notice:list','Bell',NULL,'admin',NOW()),
(107,'数据源管理',1,8,'datasource','system/datasource/index',NULL,0,1,'C','0','0','system:datasource:list','Coin',NULL,'admin',NOW()),

(200,'在线用户',2,1,'online','monitor/online/index',NULL,0,1,'C','0','0','monitor:online:list','Connection',NULL,'admin',NOW()),
(201,'登录日志',2,2,'loginlog','monitor/loginlog/index',NULL,0,1,'C','0','0','monitor:loginlog:list','Key',NULL,'admin',NOW()),
(202,'操作日志',2,3,'operlog','monitor/operlog/index',NULL,0,1,'C','0','0','monitor:operlog:list','Document',NULL,'admin',NOW()),
(203,'服务监控',2,4,'server','monitor/server/index',NULL,0,1,'C','0','0','monitor:server:list','Cpu',NULL,'admin',NOW()),
(204,'Redis监控',2,5,'redis','monitor/redis/index',NULL,0,1,'C','0','0','monitor:redis:list','DataLine',NULL,'admin',NOW()),
(205,'MySQL监控',2,6,'mysql','monitor/mysql/index',NULL,0,1,'C','0','0','monitor:mysql:list','Coin',NULL,'admin',NOW()),
(206,'服务注册监控',2,7,'service','monitor/service/index',NULL,0,1,'C','0','0','monitor:service:list','DataAnalysis','Eureka 注册中心服务健康监控','admin',NOW()),

(300,'代码生成',3,1,'gen','tool/gen/index',NULL,0,1,'C','0','0','tool:gen:list','MagicStick',NULL,'admin',NOW()),

(1000,'用户查询',100,1,'','',NULL,0,1,'F','0','0','system:user:query',NULL,NULL,'admin',NOW()),
(1001,'用户新增',100,2,'','',NULL,0,1,'F','0','0','system:user:add',NULL,NULL,'admin',NOW()),
(1002,'用户修改',100,3,'','',NULL,0,1,'F','0','0','system:user:edit',NULL,NULL,'admin',NOW()),
(1003,'用户删除',100,4,'','',NULL,0,1,'F','0','0','system:user:remove',NULL,NULL,'admin',NOW()),
(1004,'重置密码',100,5,'','',NULL,0,1,'F','0','0','system:user:resetPwd',NULL,NULL,'admin',NOW()),
(1005,'用户导出',100,6,'','',NULL,0,1,'F','0','0','system:user:export',NULL,NULL,'admin',NOW()),

(1010,'角色查询',101,1,'','',NULL,0,1,'F','0','0','system:role:query',NULL,NULL,'admin',NOW()),
(1011,'角色新增',101,2,'','',NULL,0,1,'F','0','0','system:role:add',NULL,NULL,'admin',NOW()),
(1012,'角色修改',101,3,'','',NULL,0,1,'F','0','0','system:role:edit',NULL,NULL,'admin',NOW()),
(1013,'角色删除',101,4,'','',NULL,0,1,'F','0','0','system:role:remove',NULL,NULL,'admin',NOW()),
(1014,'分配权限',101,5,'','',NULL,0,1,'F','0','0','system:role:auth',NULL,NULL,'admin',NOW()),

(1020,'菜单查询',102,1,'','',NULL,0,1,'F','0','0','system:menu:query',NULL,NULL,'admin',NOW()),
(1021,'菜单新增',102,2,'','',NULL,0,1,'F','0','0','system:menu:add',NULL,NULL,'admin',NOW()),
(1022,'菜单修改',102,3,'','',NULL,0,1,'F','0','0','system:menu:edit',NULL,NULL,'admin',NOW()),
(1023,'菜单删除',102,4,'','',NULL,0,1,'F','0','0','system:menu:remove',NULL,NULL,'admin',NOW()),

(1030,'部门查询',103,1,'','',NULL,0,1,'F','0','0','system:dept:query',NULL,NULL,'admin',NOW()),
(1031,'部门新增',103,2,'','',NULL,0,1,'F','0','0','system:dept:add',NULL,NULL,'admin',NOW()),
(1032,'部门修改',103,3,'','',NULL,0,1,'F','0','0','system:dept:edit',NULL,NULL,'admin',NOW()),
(1033,'部门删除',103,4,'','',NULL,0,1,'F','0','0','system:dept:remove',NULL,NULL,'admin',NOW()),

(1040,'字典查询',104,1,'','',NULL,0,1,'F','0','0','system:dict:query',NULL,NULL,'admin',NOW()),
(1041,'字典新增',104,2,'','',NULL,0,1,'F','0','0','system:dict:add',NULL,NULL,'admin',NOW()),
(1042,'字典修改',104,3,'','',NULL,0,1,'F','0','0','system:dict:edit',NULL,NULL,'admin',NOW()),
(1043,'字典删除',104,4,'','',NULL,0,1,'F','0','0','system:dict:remove',NULL,NULL,'admin',NOW()),

(1050,'参数查询',105,1,'','',NULL,0,1,'F','0','0','system:config:query',NULL,NULL,'admin',NOW()),
(1051,'参数新增',105,2,'','',NULL,0,1,'F','0','0','system:config:add',NULL,NULL,'admin',NOW()),
(1052,'参数修改',105,3,'','',NULL,0,1,'F','0','0','system:config:edit',NULL,NULL,'admin',NOW()),
(1053,'参数删除',105,4,'','',NULL,0,1,'F','0','0','system:config:remove',NULL,NULL,'admin',NOW()),

(1060,'公告查询',106,1,'','',NULL,0,1,'F','0','0','system:notice:query',NULL,NULL,'admin',NOW()),
(1061,'公告新增',106,2,'','',NULL,0,1,'F','0','0','system:notice:add',NULL,NULL,'admin',NOW()),
(1062,'公告修改',106,3,'','',NULL,0,1,'F','0','0','system:notice:edit',NULL,NULL,'admin',NOW()),
(1063,'公告删除',106,4,'','',NULL,0,1,'F','0','0','system:notice:remove',NULL,NULL,'admin',NOW()),

(1070,'数据源查询',107,1,'','',NULL,0,1,'F','0','0','system:datasource:query',NULL,NULL,'admin',NOW()),
(1071,'数据源新增',107,2,'','',NULL,0,1,'F','0','0','system:datasource:add',NULL,NULL,'admin',NOW()),
(1072,'数据源修改',107,3,'','',NULL,0,1,'F','0','0','system:datasource:edit',NULL,NULL,'admin',NOW()),
(1073,'数据源删除',107,4,'','',NULL,0,1,'F','0','0','system:datasource:remove',NULL,NULL,'admin',NOW()),
(1074,'连接测试',107,5,'','',NULL,0,1,'F','0','0','system:datasource:test',NULL,NULL,'admin',NOW()),

(1100,'在线查询',200,1,'','',NULL,0,1,'F','0','0','monitor:online:query',NULL,NULL,'admin',NOW()),
(1101,'强制下线',200,2,'','',NULL,0,1,'F','0','0','monitor:online:kick',NULL,NULL,'admin',NOW()),

(1110,'登录日志查询',201,1,'','',NULL,0,1,'F','0','0','monitor:loginlog:query',NULL,NULL,'admin',NOW()),
(1111,'登录日志删除',201,2,'','',NULL,0,1,'F','0','0','monitor:loginlog:remove',NULL,NULL,'admin',NOW()),

(1120,'操作日志查询',202,1,'','',NULL,0,1,'F','0','0','monitor:operlog:query',NULL,NULL,'admin',NOW()),
(1121,'操作日志删除',202,2,'','',NULL,0,1,'F','0','0','monitor:operlog:remove',NULL,NULL,'admin',NOW()),

(1130,'服务监控查询',203,1,'','',NULL,0,1,'F','0','0','monitor:server:query',NULL,NULL,'admin',NOW()),
(1140,'Redis监控查询',204,1,'','',NULL,0,1,'F','0','0','monitor:redis:query',NULL,NULL,'admin',NOW()),
(1150,'MySQL监控查询',205,1,'','',NULL,0,1,'F','0','0','monitor:mysql:query',NULL,NULL,'admin',NOW()),

(1200,'代码生成查询',300,1,'','',NULL,0,1,'F','0','0','tool:gen:query',NULL,NULL,'admin',NOW()),
(1201,'导入表',300,2,'','',NULL,0,1,'F','0','0','tool:gen:import',NULL,NULL,'admin',NOW()),
(1202,'修改生成配置',300,3,'','',NULL,0,1,'F','0','0','tool:gen:edit',NULL,NULL,'admin',NOW()),
(1203,'删除生成配置',300,4,'','',NULL,0,1,'F','0','0','tool:gen:remove',NULL,NULL,'admin',NOW()),
(1204,'预览代码',300,5,'','',NULL,0,1,'F','0','0','tool:gen:preview',NULL,NULL,'admin',NOW()),
(1205,'下载代码',300,6,'','',NULL,0,1,'F','0','0','tool:gen:download',NULL,NULL,'admin',NOW()),

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
(1151,'慢SQL治理',205,1,'','',NULL,0,1,'F','0','0','monitor:mysql:kill',NULL,NULL,'admin',NOW()),
(1160,'服务注册监控查询',206,1,'','',NULL,0,1,'F','0','0','monitor:service:query',NULL,NULL,'admin',NOW());

-- 超级管理员拥有全部菜单；普通用户：用户管理(只读) + 日志/服务监控
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`) SELECT 1, menu_id FROM `sys_menu`;
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`) VALUES
(2,1),(2,100),(2,1000),(2,2),(2,201),(2,1110),(2,202),(2,1120),(2,203),(2,1130),(2,206),(2,1160),
(3,1),(3,107),(3,1070),(3,1074),(3,2),(2,200),(2,109),(3,1151);

INSERT INTO `sys_dict_type` (`dict_name`,`dict_type`,`status`,`remark`,`create_by`,`create_time`) VALUES
('系统开关','sys_normal_disable','0','系统开关列表','admin',NOW()),
('用户性别','sys_user_sex','0','用户性别列表','admin',NOW()),
('菜单状态','sys_show_hide','0','菜单状态列表','admin',NOW()),
('通知类型','sys_notice_type','0','通知类型列表','admin',NOW()),
('操作类型','sys_oper_type','0','操作类型列表','admin',NOW()),
('系统状态','sys_common_status','0','登录状态列表','admin',NOW());

INSERT INTO `sys_dict_data` (`dict_sort`,`dict_label`,`dict_value`,`dict_type`,`list_class`,`is_default`,`status`,`create_by`,`create_time`) VALUES
(1,'正常','0','sys_normal_disable','success','Y','0','admin',NOW()),
(2,'停用','1','sys_normal_disable','danger','N','0','admin',NOW()),
(1,'男','0','sys_user_sex','','Y','0','admin',NOW()),
(2,'女','1','sys_user_sex','','N','0','admin',NOW()),
(3,'未知','2','sys_user_sex','','N','0','admin',NOW()),
(1,'显示','0','sys_show_hide','primary','Y','0','admin',NOW()),
(2,'隐藏','1','sys_show_hide','danger','N','0','admin',NOW()),
(1,'通知','1','sys_notice_type','warning','Y','0','admin',NOW()),
(2,'公告','2','sys_notice_type','success','N','0','admin',NOW()),
(1,'其它','0','sys_oper_type','info','N','0','admin',NOW()),
(2,'新增','1','sys_oper_type','info','N','0','admin',NOW()),
(3,'修改','2','sys_oper_type','info','N','0','admin',NOW()),
(4,'删除','3','sys_oper_type','danger','N','0','admin',NOW()),
(5,'授权','4','sys_oper_type','primary','N','0','admin',NOW()),
(6,'导出','5','sys_oper_type','warning','N','0','admin',NOW()),
(7,'导入','6','sys_oper_type','warning','N','0','admin',NOW()),
(8,'强退','7','sys_oper_type','danger','N','0','admin',NOW()),
(9,'生成代码','8','sys_oper_type','warning','N','0','admin',NOW()),
(10,'清空数据','9','sys_oper_type','danger','N','0','admin',NOW()),
(1,'成功','0','sys_common_status','success','Y','0','admin',NOW()),
(2,'失败','1','sys_common_status','danger','N','0','admin',NOW());

INSERT INTO `sys_config` (`config_name`,`config_key`,`config_value`,`config_type`,`remark`,`create_by`,`create_time`) VALUES
('主框架页-默认皮肤','sys.index.skinName','skin-blue','Y','蓝色 skin-blue、绿色 skin-green','admin',NOW()),
('用户管理-账号初始密码','sys.user.initPassword','admin123','Y','新增用户默认密码','admin',NOW()),
('账号自助-验证码开关','sys.account.captchaEnabled','true','Y','是否开启登录验证码(true/false)','admin',NOW()),
('账号自助-是否开启注册','sys.account.registerUser','false','Y','是否开启注册功能','admin',NOW()),
('系统名称','sys.app.name','Learning Platform','Y','系统展示名称','admin',NOW()),
('文件服务-存储模式','sys.file.mode','local','Y','文件存储模式(local本地/minio/rustfs)','admin',NOW()),
('消息中心-推送模式','sys.message.mode','inner','Y','消息推送模式(inner站内信/email邮件/websocket实时推送)','admin',NOW());

INSERT INTO `sys_job` (`job_id`,`job_name`,`job_bean`,`job_param`,`cron_expression`,`status`,`remark`,`create_by`,`create_time`) VALUES
(1,'日志月表预创建','logTablePrepareJob','','0 0 1 25 * ?','0','每月25日预创建下月日志分表','admin',NOW()),
(2,'过期消息清理','messageCleanupJob','30','0 30 2 * * ?','0','每天02:30清理30天前的已读消息','admin',NOW());

INSERT INTO `sys_notice` (`notice_title`,`notice_type`,`notice_content`,`status`,`create_by`,`create_time`) VALUES
('欢迎使用 Learning Platform','2','<p>Learning Platform 是基于 Spring Boot 3 + Spring Cloud Alibaba + Sa-Token + MyBatis-Flex 的微服务权限管理系统。</p><p>默认账号：admin / admin123</p>','0','admin',NOW()),
('系统维护通知','1','<p>系统将于每周日 02:00 进行例行维护，请提前保存数据。</p>','0','admin',NOW());

INSERT INTO `sys_login_log` (`username`,`ipaddr`,`login_location`,`browser`,`os`,`status`,`msg`,`login_time`) VALUES
('admin','127.0.0.1','内网IP','Chrome','Windows','0','登录成功',NOW());

SET FOREIGN_KEY_CHECKS = 1;
