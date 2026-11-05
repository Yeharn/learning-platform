/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 文件记录数据访问接口
 */

package org.yeharn.lp.file.mapper;

import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.yeharn.lp.file.entity.SysFile;

@Mapper
public interface SysFileMapper extends BaseMapper<SysFile> {
}
