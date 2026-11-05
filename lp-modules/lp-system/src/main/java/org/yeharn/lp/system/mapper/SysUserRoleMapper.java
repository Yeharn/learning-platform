/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 用户角色数据访问接口
 */

package org.yeharn.lp.system.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserRoleMapper {

    @Insert("<script>insert into sys_user_role(user_id, role_id) values " +
            "<foreach collection='roleIds' item='rid' separator=','>(#{userId}, #{rid})</foreach></script>")
    int batchInsert(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);

    @Delete("delete from sys_user_role where user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @Delete("delete from sys_user_role where role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    @Select("select role_id from sys_user_role where user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    @Select("select count(1) from sys_user_role where role_id = #{roleId}")
    int countUserByRoleId(@Param("roleId") Long roleId);

    @Select("select u.* from sys_user u inner join sys_user_role ur on ur.user_id = u.user_id " +
            "where ur.role_id = #{roleId} and u.del_flag = '0'")
    List<org.yeharn.lp.system.entity.SysUser> selectUsersByRoleId(@Param("roleId") Long roleId);
}
