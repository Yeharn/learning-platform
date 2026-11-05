/**
 * @Author: Yeharn
 * @Date:2026-11-05
 * @Version: 1.0.0
 * @Description: 定时任务服务 - 定时任务数据访问接口
 */

package org.yeharn.lp.job.mapper;

import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.yeharn.lp.job.entity.SysJob;

@Mapper
public interface SysJobMapper extends BaseMapper<SysJob> {
}
