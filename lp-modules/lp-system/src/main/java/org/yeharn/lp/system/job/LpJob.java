/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 定时任务
 */

package org.yeharn.lp.system.job;

/**
 * 内置定时任务处理器接口，实现类注册为 Spring Bean 后即可在「定时任务」中通过 Bean 名称调度
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
