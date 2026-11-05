/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 启动时加载并注册动态数据源
 */

package org.yeharn.lp.system.config;

import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.yeharn.lp.common.core.Constants;
import org.yeharn.lp.common.util.CryptoUtils;
import org.yeharn.lp.system.entity.SysDatasource;
import org.yeharn.lp.system.mapper.SysDatasourceMapper;
import org.yeharn.lp.system.service.SysDatasourceService;

import java.time.LocalDateTime;

/**
 * 启动时注册主数据源记录并加载其它数据源
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DatasourceInitializer {

    private final SysDatasourceMapper datasourceMapper;
    private final SysDatasourceService datasourceService;

    @Value("${spring.datasource.url}")
    private String url;
    @Value("${spring.datasource.username}")
    private String username;
    @Value("${spring.datasource.password}")
    private String password;
    @Value("${spring.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}")
    private String driverClass;
    @Value("${lp.security.aes-key:LearningPlatform2026AesKey!}")
    private String aesKey;

    @Bean
    public ApplicationRunner datasourceRunner() {
        return args -> {
            CryptoUtils.setAesKey(aesKey);
            SysDatasource master = datasourceMapper.selectOneByQuery(
                    QueryWrapper.create().eq("ds_key", Constants.DS_MASTER));
            if (master == null) {
                master = new SysDatasource();
                master.setName("主数据源");
                master.setDsKey(Constants.DS_MASTER);
                master.setDbType("mysql");
                master.setDriverClass(driverClass);
                master.setUrl(url);
                master.setUsername(username);
                master.setPassword(CryptoUtils.encrypt(password));
                master.setStatus(Constants.STATUS_NORMAL);
                master.setIsDefault(Constants.YES);
                master.setRemark("系统主数据源，请勿删除");
                master.setCreateBy("system");
                master.setCreateTime(LocalDateTime.now());
                datasourceMapper.insertSelective(master);
                log.info("已注册主数据源记录");
            }
            datasourceService.loadAll();
            log.info("动态数据源加载完成");
        };
    }
}
