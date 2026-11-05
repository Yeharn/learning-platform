/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 角色部门数据访问接口
 */

package org.yeharn.lp.system.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleDeptMapper {

    @Insert("<script>insert into sys_role_dept(role_id, dept_id) values " +
            "<foreach collection='deptIds' item='did' separator=','>(#{roleId}, #{did})</foreach></script>")
    int batchInsert(@Param("roleId") Long roleId, @Param("deptIds") List<Long> deptIds);

    @Delete("delete from sys_role_dept where role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    @Select("select dept_id from sys_role_dept where role_id = #{roleId}")
    List<Long> selectDeptIdsByRoleId(@Param("roleId") Long roleId);
}
