/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 内置示例定时任务
 */

package org.yeharn.lp.system.job;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.yeharn.lp.system.entity.SysMessage;
import org.yeharn.lp.system.mapper.SysMessageMapper;
import org.yeharn.lp.system.service.LogTableService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 内置任务处理器示例
 */
@Component
@RequiredArgsConstructor
public class SampleJobs {

    private final LogTableService logTableService;
    private final SysMessageMapper messageMapper;

    /** 每月预创建下月日志分表，避免月初首次写入建表延迟 */
    @Component("logTablePrepareJob")
    public static class LogTablePrepareJob implements LpJob {

        private final LogTableService logTableService;

        public LogTablePrepareJob(LogTableService logTableService) {
            this.logTableService = logTableService;
        }

        @Override
        public String execute(String param) {
            String next = logTableService.nextMonth();
            String oper = logTableService.ensureTable(LogTableService.OPER, next);
            String login = logTableService.ensureTable(LogTableService.LOGIN, next);
            return "已准备 " + oper + ", " + login;
        }
    }

    /** 清理 N 天前的已读站内消息，参数为天数，默认 30 */
    @Component("messageCleanupJob")
    public static class MessageCleanupJob implements LpJob {

        private final SysMessageMapper messageMapper;

        public MessageCleanupJob(SysMessageMapper messageMapper) {
            this.messageMapper = messageMapper;
        }

        @Override
        public String execute(String param) {
            int days = 30;
            if (StrUtil.isNotBlank(param)) {
                try {
                    days = Integer.parseInt(param.trim());
                } catch (NumberFormatException ignored) {
                }
            }
            List<SysMessage> expired = messageMapper.selectListByQuery(QueryWrapper.create()
                    .eq("read_flag", "1")
                    .lt("create_time", LocalDateTime.now().minusDays(days))
                    .limit(1000));
            expired.forEach(m -> messageMapper.deleteById(m.getMessageId()));
            return "清理已读消息 " + expired.size() + " 条";
        }
    }
}
