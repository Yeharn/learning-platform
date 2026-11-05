/**
 * @Author: Yeharn
 * @Date:2026-11-05
 * @Version: 1.0.0
 * @Description: 定时任务服务 - cron 表达式解析与下次执行时间计算
 */

package org.yeharn.lp.job.core;

import cn.hutool.core.util.StrUtil;
import org.quartz.CronExpression;
import org.yeharn.lp.common.core.ServiceException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * cron 工具：基于 Quartz CronExpression，支持 5/6/7 段表达式（含年字段）
 * <p>
 * 段位定义（7 段）：秒 分 时 日 月 周 年；日月与周互斥（一个为 ?，另一个可为 * 或具体值）。
 */
public final class CronSupport {

    /** 调度时区 */
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private CronSupport() {
    }

    /**
     * 归一化为 Quartz 7 段表达式：5 段补秒、6 段补年，并处理日/周互斥
     */
    public static String normalize(String cron) {
        if (StrUtil.isBlank(cron)) {
            throw new ServiceException("cron 表达式不能为空");
        }
        String[] parts = cron.trim().replaceAll("\\s+", " ").split(" ");
        String[] full;
        if (parts.length == 5) {
            full = new String[]{"0", parts[0], parts[1], parts[2], parts[3], parts[4], "*"};
        } else if (parts.length == 6) {
            full = new String[]{parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], "*"};
        } else if (parts.length == 7) {
            full = parts.clone();
        } else {
            throw new ServiceException("cron 表达式段数不正确（支持 5/6/7 段）：" + cron);
        }
        // 日 / 周 互斥：周为具体值时按周调度，否则按日调度
        String dayOfMonth = full[3];
        String dayOfWeek = full[5];
        boolean weekSpecific = !"?".equals(dayOfWeek) && !"*".equals(dayOfWeek);
        boolean daySpecific = !"?".equals(dayOfMonth) && !"*".equals(dayOfMonth);
        if (weekSpecific && daySpecific) {
            throw new ServiceException("cron 的「日」与「周」不能同时指定具体值：" + cron);
        }
        if (weekSpecific) {
            full[3] = "?";
        } else {
            full[5] = "?";
        }
        return String.join(" ", full);
    }

    /**
     * 解析 cron（自动归一化）
     */
    public static CronExpression parse(String cron) {
        try {
            CronExpression expression = new CronExpression(normalize(cron));
            expression.setTimeZone(TimeZone.getTimeZone(ZONE));
            return expression;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("cron 表达式无效：" + cron + "（" + e.getMessage() + "）");
        }
    }

    /**
     * 校验 cron 是否合法
     */
    public static void validate(String cron) {
        parse(cron);
    }

    /**
     * 计算下一次执行时间，无未来触发时间返回 null
     */
    public static LocalDateTime nextRunTime(String cron) {
        return nextRunTime(cron, LocalDateTime.now());
    }

    public static LocalDateTime nextRunTime(String cron, LocalDateTime after) {
        CronExpression expression = parse(cron);
        Date next = expression.getNextValidTimeAfter(Date.from(after.atZone(ZONE).toInstant()));
        return next == null ? null : LocalDateTime.ofInstant(next.toInstant(), ZONE);
    }

    /**
     * 预览未来 N 次执行时间
     */
    public static List<LocalDateTime> preview(String cron, int count) {
        CronExpression expression = parse(cron);
        List<LocalDateTime> list = new ArrayList<>(count);
        Date next = expression.getNextValidTimeAfter(new Date());
        while (next != null && list.size() < count) {
            list.add(LocalDateTime.ofInstant(next.toInstant(), ZONE));
            next = expression.getNextValidTimeAfter(next);
        }
        return list;
    }
}
