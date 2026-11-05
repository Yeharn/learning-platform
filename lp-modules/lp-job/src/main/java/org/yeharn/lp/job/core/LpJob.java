/**
 * @Author: Yeharn
 * @Date:2026-11-05
 * @Version: 1.0.0
 * @Description: 定时任务服务 - 任务处理器接口
 */

package org.yeharn.lp.job.core;

/**
 * 定时任务处理器：实现类注册为 Spring Bean 后即可在「定时任务」中通过 Bean 名称调度
 */
public interface LpJob {

    /**
     * 执行任务
     *
     * @param param 任务参数
     * @return 执行结果描述
     */
    String execute(String param) throws Exception;
}
