/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 菜单数据访问接口
 */

package org.yeharn.lp.system.mapper;

import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.yeharn.lp.system.entity.SysMenu;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    @Select("select distinct m.perms from sys_menu m " +
            "inner join sys_role_menu rm on rm.menu_id = m.menu_id " +
            "inner join sys_user_role ur on ur.role_id = rm.role_id " +
            "where ur.user_id = #{userId} and m.status = '0' and m.perms is not null and m.perms <> ''")
    List<String> selectPermsByUserId(@Param("userId") Long userId);

    @Select("select distinct m.* from sys_menu m " +
            "inner join sys_role_menu rm on rm.menu_id = m.menu_id " +
            "inner join sys_user_role ur on ur.role_id = rm.role_id " +
            "where ur.user_id = #{userId} and m.menu_type in ('M','C') and m.status = '0' " +
            "order by m.parent_id, m.order_num")
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);

    @Select("select * from sys_menu where menu_type in ('M','C') and status = '0' order by parent_id, order_num")
    List<SysMenu> selectAllMenus();

    @Select("select menu_id from sys_role_menu where role_id = #{roleId}")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    @Select("select distinct m.perms from sys_menu m " +
            "inner join sys_role_menu rm on rm.menu_id = m.menu_id " +
            "where rm.role_id = #{roleId} and m.status = '0' and m.perms is not null and m.perms <> ''")
    List<String> selectPermsByRoleId(@Param("roleId") Long roleId);
}
