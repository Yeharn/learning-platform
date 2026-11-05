/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 流程服务启动类
 */

package org.yeharn.lp.flow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 流程服务：Dromara Warm-Flow 1.8.9（Apache-2.0），独立部署
 * <p>
 * 仅扫描流程自身包与公共配置，避免加载 MyBatis-Flex 相关组件（本服务使用 MyBatis-Plus）；
 * Mapper 同时扫描引擎的 orm 包，完成与引擎数据层的对接。
 */
@EnableDiscoveryClient
@EnableTransactionManagement
@MapperScan(basePackages = {
        "org.yeharn.lp.flow.mapper",
        "org.dromara.warm.flow.orm.mapper"
})
@SpringBootApplication(scanBasePackages = {
        "org.yeharn.lp.flow",
        "org.yeharn.lp.common.config",
        "org.yeharn.lp.common.core",
        "org.yeharn.lp.common.security",
        "org.yeharn.lp.common.log"
})
public class FlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlowApplication.class, args);
    }
}
