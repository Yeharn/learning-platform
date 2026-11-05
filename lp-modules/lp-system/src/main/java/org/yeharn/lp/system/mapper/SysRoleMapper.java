/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 角色数据访问接口
 */

package org.yeharn.lp.system.mapper;

import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.yeharn.lp.system.entity.SysRole;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Select("select r.* from sys_role r inner join sys_user_role ur on ur.role_id = r.role_id " +
            "where ur.user_id = #{userId} and r.del_flag = '0' and r.status = '0'")
    List<SysRole> selectRolesByUserId(@Param("userId") Long userId);
}
