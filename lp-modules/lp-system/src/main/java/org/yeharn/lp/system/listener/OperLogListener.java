/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 操作日志异步落库监听器
 */

package org.yeharn.lp.system.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.yeharn.lp.common.log.OperLogEvent;
import org.yeharn.lp.system.entity.SysOperLog;
import org.yeharn.lp.system.service.LogTableService;

/**
 * 操作日志落库（按月分表）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperLogListener {

    private final LogTableService logTableService;

    @Async
    @EventListener
    public void onOperLog(OperLogEvent event) {
        try {
            SysOperLog entity = new SysOperLog();
            entity.setTitle(event.getTitle());
            entity.setBusinessType(event.getBusinessType());
            entity.setMethod(event.getMethod());
            entity.setRequestMethod(event.getRequestMethod());
            entity.setOperatorType(1);
            entity.setOperName(event.getOperName());
            entity.setDeptName(event.getDeptName());
            entity.setOperUrl(event.getOperUrl());
            entity.setOperIp(event.getOperIp());
            entity.setOperLocation(event.getOperLocation());
            entity.setOperParam(event.getOperParam());
            entity.setJsonResult(event.getJsonResult());
            entity.setStatus(event.getStatus());
            entity.setErrorMsg(event.getErrorMsg());
            entity.setOperTime(event.getOperTime());
            entity.setCostTime(event.getCostTime());
            logTableService.insertOperLog(entity);
        } catch (Exception e) {
            log.warn("保存操作日志失败: {}", e.getMessage());
        }
    }
}
