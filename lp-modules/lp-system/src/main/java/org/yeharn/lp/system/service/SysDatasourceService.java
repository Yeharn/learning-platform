/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 数据源管理业务服务
 */

package org.yeharn.lp.system.service;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yeharn.lp.common.core.Constants;
import org.yeharn.lp.common.core.ServiceException;
import org.yeharn.lp.common.datasource.DataSourceInfo;
import org.yeharn.lp.common.datasource.DynamicDataSourceManager;
import org.yeharn.lp.common.security.SecurityUtils;
import org.yeharn.lp.common.util.CryptoUtils;
import org.yeharn.lp.system.entity.SysDatasource;
import org.yeharn.lp.system.mapper.SysDatasourceMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 多数据源管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDatasourceService {

    private final SysDatasourceMapper datasourceMapper;
    private final DynamicDataSourceManager dataSourceManager;

    public List<SysDatasource> list() {
        List<SysDatasource> list = datasourceMapper.selectListByQuery(QueryWrapper.create().orderBy("id", true));
        list.forEach(ds -> {
            ds.setPasswordMask(CryptoUtils.mask(CryptoUtils.decrypt(ds.getPassword())));
            ds.setConnected(dataSourceManager.exists(ds.getDsKey()));
        });
        return list;
    }

    public SysDatasource getById(Long id) {
        SysDatasource ds = datasourceMapper.selectOneById(id);
        if (ds != null) {
            ds.setPasswordMask(CryptoUtils.mask(CryptoUtils.decrypt(ds.getPassword())));
        }
        return ds;
    }

    public void add(SysDatasource ds) {
        if (datasourceMapper.selectCountByQuery(QueryWrapper.create().eq("ds_key", ds.getDsKey())) > 0) {
            throw new ServiceException("数据源标识已存在：" + ds.getDsKey());
        }
        if (StrUtil.isBlank(ds.getDriverClass())) {
            ds.setDriverClass(driverOf(ds.getDbType()));
        }
        ds.setPassword(CryptoUtils.encrypt(ds.getPassword()));
        ds.setIsDefault(Constants.NO);
        ds.setCreateBy(SecurityUtils.getUsername());
        ds.setCreateTime(LocalDateTime.now());
        datasourceMapper.insertSelective(ds);
        if (Constants.STATUS_NORMAL.equals(ds.getStatus())) {
            dataSourceManager.register(toInfo(ds));
        }
    }

    public void update(SysDatasource ds) {
        SysDatasource old = datasourceMapper.selectOneById(ds.getId());
        if (old == null) {
            throw new ServiceException("数据源不存在");
        }
        if (Constants.DS_MASTER.equals(old.getDsKey())) {
            throw new ServiceException("主数据源请在配置文件中维护");
        }
        if (StrUtil.isBlank(ds.getPassword())) {
            ds.setPassword(old.getPassword());
        } else {
            ds.setPassword(CryptoUtils.encrypt(ds.getPassword()));
        }
        if (StrUtil.isBlank(ds.getDriverClass())) {
            ds.setDriverClass(driverOf(ds.getDbType()));
        }
        ds.setIsDefault(Constants.NO);
        ds.setUpdateBy(SecurityUtils.getUsername());
        ds.setUpdateTime(LocalDateTime.now());
        datasourceMapper.update(ds);
        dataSourceManager.remove(old.getDsKey());
        if (Constants.STATUS_NORMAL.equals(ds.getStatus())) {
            dataSourceManager.register(toInfo(ds));
        }
    }

    public void delete(Long id) {
        SysDatasource ds = datasourceMapper.selectOneById(id);
        if (ds == null) {
            return;
        }
        if (Constants.DS_MASTER.equals(ds.getDsKey()) || Constants.YES.equals(ds.getIsDefault())) {
            throw new ServiceException("主数据源不允许删除");
        }
        dataSourceManager.remove(ds.getDsKey());
        datasourceMapper.deleteById(id);
    }

    public String test(SysDatasource ds) {
        try {
            DataSourceInfo info = new DataSourceInfo();
            info.setDsKey("test-" + System.currentTimeMillis());
            info.setUrl(ds.getUrl());
            info.setUsername(ds.getUsername());
            info.setDriverClass(StrUtil.isBlank(ds.getDriverClass()) ? driverOf(ds.getDbType()) : ds.getDriverClass());
            String password = ds.getPassword();
            if (ds.getId() != null && StrUtil.isBlank(password)) {
                SysDatasource old = datasourceMapper.selectOneById(ds.getId());
                password = old == null ? "" : old.getPassword();
            }
            info.setPassword(CryptoUtils.decrypt(password));
            return "连接成功：" + dataSourceManager.test(info);
        } catch (Exception e) {
            throw new ServiceException("连接失败：" + e.getMessage());
        }
    }

    public Map<String, Object> poolStats(String dsKey) {
        return dataSourceManager.stats(dsKey);
    }

    /**
     * 启动时加载所有启用的数据源
     */
    public void loadAll() {
        List<SysDatasource> list = datasourceMapper.selectListByQuery(
                QueryWrapper.create().eq("status", Constants.STATUS_NORMAL));
        for (SysDatasource ds : list) {
            if (Constants.DS_MASTER.equals(ds.getDsKey()) || dataSourceManager.exists(ds.getDsKey())) {
                continue;
            }
            try {
                dataSourceManager.register(toInfo(ds));
            } catch (Exception e) {
                log.warn("数据源 [{}] 加载失败: {}", ds.getDsKey(), e.getMessage());
            }
        }
    }

    private DataSourceInfo toInfo(SysDatasource ds) {
        DataSourceInfo info = new DataSourceInfo();
        info.setDsKey(ds.getDsKey());
        info.setName(ds.getName());
        info.setDbType(ds.getDbType());
        info.setUrl(ds.getUrl());
        info.setUsername(ds.getUsername());
        info.setPassword(CryptoUtils.decrypt(ds.getPassword()));
        info.setDriverClass(StrUtil.isBlank(ds.getDriverClass()) ? driverOf(ds.getDbType()) : ds.getDriverClass());
        return info;
    }

    public static String driverOf(String dbType) {
        if (dbType == null) {
            return "com.mysql.cj.jdbc.Driver";
        }
        return switch (dbType.toLowerCase()) {
            case "postgresql" -> "org.postgresql.Driver";
            case "oracle" -> "oracle.jdbc.OracleDriver";
            case "sqlserver" -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            case "dm" -> "dm.jdbc.driver.DmDriver";
            default -> "com.mysql.cj.jdbc.Driver";
        };
    }
}
