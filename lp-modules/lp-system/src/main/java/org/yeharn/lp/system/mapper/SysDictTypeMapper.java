/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 字典类型数据访问接口
 */

package org.yeharn.lp.system.mapper;

import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.yeharn.lp.system.entity.SysDictType;

@Mapper
public interface SysDictTypeMapper extends BaseMapper<SysDictType> {
}
