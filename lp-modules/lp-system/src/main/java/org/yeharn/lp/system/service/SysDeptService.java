/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 部门业务服务
 */

package org.yeharn.lp.system.service;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.yeharn.lp.common.core.ServiceException;
import org.yeharn.lp.common.security.SecurityUtils;
import org.yeharn.lp.system.entity.SysDept;
import org.yeharn.lp.system.entity.SysUser;
import org.yeharn.lp.system.mapper.SysDeptMapper;
import org.yeharn.lp.system.mapper.SysUserMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 部门管理
 */
@Service
@RequiredArgsConstructor
public class SysDeptService {

    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;

    public List<SysDept> list(String deptName, String status) {
        QueryWrapper qw = QueryWrapper.create()
                .like("dept_name", deptName, StrUtil.isNotBlank(deptName))
                .eq("status", status, StrUtil.isNotBlank(status))
                .orderBy("parent_id", true)
                .orderBy("order_num", true);
        List<SysDept> depts = deptMapper.selectListByQuery(qw);
        return buildTree(depts, 0L);
    }

    public List<SysDept> listAll() {
        return deptMapper.selectListByQuery(QueryWrapper.create().orderBy("order_num", true));
    }

    public SysDept getById(Long deptId) {
        return deptMapper.selectOneById(deptId);
    }

    public void add(SysDept dept) {
        checkNameUnique(dept);
        SysDept parent = dept.getParentId() == null || dept.getParentId() == 0 ? null : deptMapper.selectOneById(dept.getParentId());
        if (parent != null && "1".equals(parent.getStatus())) {
            throw new ServiceException("上级部门已停用，不允许新增");
        }
        dept.setAncestors(parent == null ? "0" : parent.getAncestors() + "," + parent.getDeptId());
        dept.setCreateBy(SecurityUtils.getUsername());
        dept.setCreateTime(LocalDateTime.now());
        deptMapper.insertSelective(dept);
    }

    public void update(SysDept dept) {
        checkNameUnique(dept);
        if (dept.getDeptId().equals(dept.getParentId())) {
            throw new ServiceException("上级部门不能是自己");
        }
        SysDept old = deptMapper.selectOneById(dept.getDeptId());
        if (old == null) {
            throw new ServiceException("部门不存在");
        }
        SysDept parent = dept.getParentId() == null || dept.getParentId() == 0 ? null : deptMapper.selectOneById(dept.getParentId());
        dept.setAncestors(parent == null ? "0" : parent.getAncestors() + "," + parent.getDeptId());
        dept.setUpdateBy(SecurityUtils.getUsername());
        dept.setUpdateTime(LocalDateTime.now());
        deptMapper.update(dept);
    }

    public void delete(Long deptId) {
        if (deptMapper.selectCountByQuery(QueryWrapper.create().eq("parent_id", deptId)) > 0) {
            throw new ServiceException("存在下级部门，不允许删除");
        }
        if (userMapper.selectCountByQuery(QueryWrapper.create().eq("dept_id", deptId)) > 0) {
            throw new ServiceException("部门下存在用户，不允许删除");
        }
        deptMapper.deleteById(deptId);
    }

    /**
     * 获取部门及其所有下级部门ID
     */
    public List<Long> listChildrenIds(Long deptId) {
        List<SysDept> all = deptMapper.selectListByQuery(QueryWrapper.create());
        List<Long> ids = new ArrayList<>();
        ids.add(deptId);
        collect(all, deptId, ids);
        return ids;
    }

    private void collect(List<SysDept> all, Long parentId, List<Long> ids) {
        for (SysDept dept : all) {
            if (parentId.equals(dept.getParentId())) {
                ids.add(dept.getDeptId());
                collect(all, dept.getDeptId(), ids);
            }
        }
    }

    public String getDeptName(Long deptId) {
        if (deptId == null) {
            return "";
        }
        SysDept dept = deptMapper.selectOneById(deptId);
        return dept == null ? "" : dept.getDeptName();
    }

    private void checkNameUnique(SysDept dept) {
        QueryWrapper qw = QueryWrapper.create()
                .eq("dept_name", dept.getDeptName())
                .eq("parent_id", dept.getParentId() == null ? 0L : dept.getParentId());
        if (dept.getDeptId() != null) {
            qw.ne("dept_id", dept.getDeptId());
        }
        if (deptMapper.selectCountByQuery(qw) > 0) {
            throw new ServiceException("同级部门名称已存在");
        }
    }

    public List<SysDept> buildTree(List<SysDept> depts, Long parentId) {
        List<SysDept> tree = new ArrayList<>();
        for (SysDept dept : depts) {
            if (parentId.equals(dept.getParentId())) {
                dept.setChildren(buildTree(depts, dept.getDeptId()));
                tree.add(dept);
            }
        }
        return tree;
    }

    public List<SysUser> listUsersByDept(Long deptId) {
        return userMapper.selectListByQuery(QueryWrapper.create().eq("dept_id", deptId).eq("status", "0"));
    }
}
