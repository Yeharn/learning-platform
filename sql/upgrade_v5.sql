-- ============================================================
-- upgrade_v5.sql：新增"服务注册监控"菜单（基于 Eureka 注册中心）
-- @Author: Yeharn
-- @Date:2026-11-05
-- 适用：已按 lp.sql 初始化的库；执行后刷新页面或重新登录生效
-- ============================================================

USE learning_platform;

INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query_param`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`icon`,`remark`,`create_by`,`create_time`) VALUES
(206,'服务注册监控',2,7,'service','monitor/service/index',NULL,0,1,'C','0','0','monitor:service:list','DataAnalysis','Eureka 注册中心服务健康监控','admin',NOW()),
(1160,'服务注册监控查询',206,1,'','',NULL,0,1,'F','0','0','monitor:service:query',NULL,NULL,'admin',NOW());

-- 超级管理员自动拥有（sys_role_menu 全量关联）；普通用户/运维角色追加授权
INSERT INTO `sys_role_menu` (`role_id`,`menu_id`) VALUES (2,206),(2,1160),(3,206),(3,1160);