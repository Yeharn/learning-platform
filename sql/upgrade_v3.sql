-- ============================================================
-- Learning Platform 升级脚本 v3
-- 内容：流程沟通评论表、流程中心菜单（流程定义 / 流程实例 / 我的任务 / 流程历史）
-- 执行：mysql -uroot -p learning_platform < upgrade_v3.sql
-- ============================================================

-- 流程沟通评论
CREATE TABLE IF NOT EXISTS `lp_flow_comment` (
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `instance_id` bigint       NOT NULL COMMENT '流程实例ID',
    `task_id`     bigint       DEFAULT NULL COMMENT '关联任务ID',
    `user_id`     varchar(50)  NOT NULL COMMENT '评论人ID',
    `user_name`   varchar(100) NOT NULL COMMENT '评论人姓名',
    `content`     varchar(1000) NOT NULL COMMENT '沟通内容',
    `create_time` datetime     NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_instance` (`instance_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '流程沟通评论';

-- 流程中心目录与子菜单（顶级菜单，独立路由）
DELETE FROM `sys_menu` WHERE `menu_id` BETWEEN 330 AND 3319;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`,
                        `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
VALUES (330, '流程中心', 0, 4, 'flow', NULL, NULL, 0, 1, 'M', '0', '0', NULL, 'Guide', 'admin', NOW()),
       (3301, '流程定义', 330, 1, 'definition', 'tool/flow/definition', NULL, 0, 1, 'C', '0', '0', 'tool:flow:list', 'Files', 'admin', NOW()),
       (3302, '流程实例', 330, 2, 'instance', 'tool/flow/instance', NULL, 0, 1, 'C', '0', '0', 'tool:flow:list', 'DataLine', 'admin', NOW()),
       (3303, '我的任务', 330, 3, 'task', 'tool/flow/task', NULL, 0, 1, 'C', '0', '0', 'tool:flow:list', 'Finished', 'admin', NOW()),
       (3304, '流程历史', 330, 4, 'history', 'tool/flow/history', NULL, 0, 1, 'C', '0', '0', 'tool:flow:list', 'Clock', 'admin', NOW());

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`,
                        `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
VALUES (3310, '流程部署', 3301, 1, NULL, NULL, NULL, 0, 1, 'F', '0', '0', 'tool:flow:deploy', NULL, 'admin', NOW()),
       (3311, '流程删除', 3301, 2, NULL, NULL, NULL, 0, 1, 'F', '0', '0', 'tool:flow:remove', NULL, 'admin', NOW()),
       (3312, '流程发起', 3303, 1, NULL, NULL, NULL, 0, 1, 'F', '0', '0', 'tool:flow:start', NULL, 'admin', NOW()),
       (3313, '流程办理', 3303, 2, NULL, NULL, NULL, 0, 1, 'F', '0', '0', 'tool:flow:execute', NULL, 'admin', NOW());

-- 旧版单页流程管理菜单隐藏，保留数据兼容已有角色授权
UPDATE `sys_menu` SET `visible` = '1' WHERE `menu_id` = 302;
