/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 参数业务服务
 */

package org.yeharn.lp.system.service;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.yeharn.lp.common.core.Constants;
import org.yeharn.lp.common.core.PageResult;
import org.yeharn.lp.common.core.ServiceException;
import org.yeharn.lp.common.redis.RedisUtils;
import org.yeharn.lp.common.security.SecurityUtils;
import org.yeharn.lp.system.entity.SysConfig;
import org.yeharn.lp.system.mapper.SysConfigMapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 参数配置
 */
@Service
@RequiredArgsConstructor
public class SysConfigService {

    private final SysConfigMapper configMapper;
    private final RedisUtils redisUtils;

    public PageResult<SysConfig> page(String configName, String configKey, int pageNum, int pageSize) {
        QueryWrapper qw = QueryWrapper.create()
                .like("config_name", configName, StrUtil.isNotBlank(configName))
                .like("config_key", configKey, StrUtil.isNotBlank(configKey))
                .orderBy("config_id", false);
        Page<SysConfig> page = configMapper.paginate(Page.of(pageNum, pageSize), qw);
        return new PageResult<>(page.getTotalRow(), page.getRecords(), pageNum, pageSize);
    }

    public List<SysConfig> listAll() {
        return configMapper.selectListByQuery(QueryWrapper.create().orderBy("config_id", true));
    }

    public String getValue(String key) {
        Object cached = redisUtils.get(Constants.CONFIG_CACHE_KEY + key);
        if (cached != null) {
            return String.valueOf(cached);
        }
        SysConfig config = configMapper.selectOneByQuery(QueryWrapper.create().eq("config_key", key));
        if (config == null) {
            return null;
        }
        redisUtils.set(Constants.CONFIG_CACHE_KEY + key, config.getConfigValue(), 3600);
        return config.getConfigValue();
    }

    public String getValue(String key, String defaultValue) {
        String value = getValue(key);
        return StrUtil.isBlank(value) ? defaultValue : value;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getValue(key);
        return StrUtil.isBlank(value) ? defaultValue : Boolean.parseBoolean(value);
    }

    public SysConfig getById(Long id) {
        return configMapper.selectOneById(id);
    }

    public void add(SysConfig config) {
        checkKeyUnique(config);
        config.setCreateBy(SecurityUtils.getUsername());
        config.setCreateTime(LocalDateTime.now());
        configMapper.insertSelective(config);
        clearCache(config.getConfigKey());
    }

    public void update(SysConfig config) {
        checkKeyUnique(config);
        config.setUpdateBy(SecurityUtils.getUsername());
        config.setUpdateTime(LocalDateTime.now());
        configMapper.update(config);
        clearCache(config.getConfigKey());
    }

    public void delete(List<Long> ids) {
        for (Long id : ids) {
            SysConfig config = configMapper.selectOneById(id);
            if (config != null && Constants.YES.equals(config.getConfigType())) {
                throw new ServiceException("内置参数【" + config.getConfigName() + "】不允许删除");
            }
            configMapper.deleteById(id);
            if (config != null) {
                clearCache(config.getConfigKey());
            }
        }
    }

    public void refreshCache() {
        configMapper.selectAll().forEach(c ->
                redisUtils.set(Constants.CONFIG_CACHE_KEY + c.getConfigKey(), c.getConfigValue(), 3600));
    }

    private void checkKeyUnique(SysConfig config) {
        QueryWrapper qw = QueryWrapper.create().eq("config_key", config.getConfigKey());
        if (config.getConfigId() != null) {
            qw.ne("config_id", config.getConfigId());
        }
        if (configMapper.selectCountByQuery(qw) > 0) {
            throw new ServiceException("参数键名已存在：" + config.getConfigKey());
        }
    }

    private void clearCache(String key) {
        redisUtils.delete(Constants.CONFIG_CACHE_KEY + key);
    }
}
