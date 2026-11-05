/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 代码生成表字段数据访问接口
 */

package org.yeharn.lp.system.mapper;

import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.yeharn.lp.system.entity.GenTableColumn;

@Mapper
public interface GenTableColumnMapper extends BaseMapper<GenTableColumn> {

    @Delete("delete from gen_table_column where table_id = #{tableId}")
    int deleteByTableId(@Param("tableId") Long tableId);
}
