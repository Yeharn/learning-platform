/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: warm-flow 全局监听器
 */

package org.yeharn.lp.flow.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.warm.flow.core.listener.GlobalListener;
import org.dromara.warm.flow.core.listener.ListenerVariable;
import org.springframework.stereotype.Component;
import org.yeharn.lp.flow.service.FlowAutomationService;

/**
 * 引擎全局监听：任务创建后驱动节点级自动化（自动审批 / 远程调用 / 脚本执行）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlowGlobalListener implements GlobalListener {

    private final FlowAutomationService automationService;

    @Override
    public void create(ListenerVariable listenerVariable) {
        try {
            automationService.onTaskCreated(listenerVariable.getTask(), listenerVariable.getNode(),
                    listenerVariable.getInstance());
        } catch (Exception e) {
            log.warn("流程创建监听器执行失败：{}", e.getMessage());
        }
    }
}
