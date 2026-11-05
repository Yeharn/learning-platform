/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 角色菜单数据访问接口
 */

package org.yeharn.lp.system.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMenuMapper {

    @Insert("<script>insert into sys_role_menu(role_id, menu_id) values " +
            "<foreach collection='menuIds' item='mid' separator=','>(#{roleId}, #{mid})</foreach></script>")
    int batchInsert(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);

    @Delete("delete from sys_role_menu where role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    @Delete("delete from sys_role_menu where menu_id = #{menuId}")
    int deleteByMenuId(@Param("menuId") Long menuId);

    @Select("select menu_id from sys_role_menu where role_id = #{roleId}")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);
}
