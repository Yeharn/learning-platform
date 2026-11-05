/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 字典业务服务
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
import org.yeharn.lp.system.entity.SysDictData;
import org.yeharn.lp.system.entity.SysDictType;
import org.yeharn.lp.system.mapper.SysDictDataMapper;
import org.yeharn.lp.system.mapper.SysDictTypeMapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 字典管理
 */
@Service
@RequiredArgsConstructor
public class SysDictService {

    private final SysDictTypeMapper typeMapper;
    private final SysDictDataMapper dataMapper;
    private final RedisUtils redisUtils;

    public PageResult<SysDictType> pageType(String dictName, String dictType, String status, int pageNum, int pageSize) {
        QueryWrapper qw = QueryWrapper.create()
                .like("dict_name", dictName, StrUtil.isNotBlank(dictName))
                .like("dict_type", dictType, StrUtil.isNotBlank(dictType))
                .eq("status", status, StrUtil.isNotBlank(status))
                .orderBy("dict_id", false);
        Page<SysDictType> page = typeMapper.paginate(Page.of(pageNum, pageSize), qw);
        return new PageResult<>(page.getTotalRow(), page.getRecords(), pageNum, pageSize);
    }

    public List<SysDictType> listTypes() {
        return typeMapper.selectListByQuery(QueryWrapper.create().orderBy("dict_id", true));
    }

    public void addType(SysDictType type) {
        if (typeMapper.selectCountByQuery(QueryWrapper.create().eq("dict_type", type.getDictType())) > 0) {
            throw new ServiceException("字典类型已存在：" + type.getDictType());
        }
        type.setCreateBy(SecurityUtils.getUsername());
        type.setCreateTime(LocalDateTime.now());
        typeMapper.insertSelective(type);
    }

    public void updateType(SysDictType type) {
        SysDictType old = typeMapper.selectOneById(type.getDictId());
        if (old == null) {
            throw new ServiceException("字典类型不存在");
        }
        if (!old.getDictType().equals(type.getDictType())) {
            SysDictData patch = new SysDictData();
            patch.setDictType(type.getDictType());
            dataMapper.updateByQuery(patch, QueryWrapper.create().eq("dict_type", old.getDictType()));
            clearCache(old.getDictType());
        }
        type.setUpdateBy(SecurityUtils.getUsername());
        type.setUpdateTime(LocalDateTime.now());
        typeMapper.update(type);
        clearCache(type.getDictType());
    }

    public void deleteType(List<Long> ids) {
        for (Long id : ids) {
            SysDictType type = typeMapper.selectOneById(id);
            if (type == null) {
                continue;
            }
            dataMapper.deleteByQuery(QueryWrapper.create().eq("dict_type", type.getDictType()));
            typeMapper.deleteById(id);
            clearCache(type.getDictType());
        }
    }

    public PageResult<SysDictData> pageData(String dictType, String dictLabel, String status, int pageNum, int pageSize) {
        QueryWrapper qw = QueryWrapper.create()
                .eq("dict_type", dictType, StrUtil.isNotBlank(dictType))
                .like("dict_label", dictLabel, StrUtil.isNotBlank(dictLabel))
                .eq("status", status, StrUtil.isNotBlank(status))
                .orderBy("dict_sort", true);
        Page<SysDictData> page = dataMapper.paginate(Page.of(pageNum, pageSize), qw);
        return new PageResult<>(page.getTotalRow(), page.getRecords(), pageNum, pageSize);
    }

    @SuppressWarnings("unchecked")
    public List<SysDictData> listDataByType(String dictType) {
        String key = Constants.DICT_CACHE_KEY + dictType;
        Object cached = redisUtils.get(key);
        if (cached instanceof List<?> list) {
            return (List<SysDictData>) list;
        }
        List<SysDictData> list = dataMapper.selectListByQuery(QueryWrapper.create()
                .eq("dict_type", dictType)
                .eq("status", "0")
                .orderBy("dict_sort", true));
        redisUtils.set(key, list, 3600);
        return list;
    }

    public void addData(SysDictData data) {
        data.setCreateBy(SecurityUtils.getUsername());
        data.setCreateTime(LocalDateTime.now());
        dataMapper.insertSelective(data);
        clearCache(data.getDictType());
    }

    public void updateData(SysDictData data) {
        data.setUpdateBy(SecurityUtils.getUsername());
        data.setUpdateTime(LocalDateTime.now());
        dataMapper.update(data);
        clearCache(data.getDictType());
    }

    public void deleteData(List<Long> ids) {
        for (Long id : ids) {
            SysDictData data = dataMapper.selectOneById(id);
            if (data != null) {
                dataMapper.deleteById(id);
                clearCache(data.getDictType());
            }
        }
    }

    private void clearCache(String dictType) {
        redisUtils.delete(Constants.DICT_CACHE_KEY + dictType);
    }
}
