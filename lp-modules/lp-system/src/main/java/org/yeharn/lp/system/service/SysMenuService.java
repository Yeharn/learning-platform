/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 菜单业务服务
 */

package org.yeharn.lp.system.service;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.yeharn.lp.common.core.ServiceException;
import org.yeharn.lp.common.security.SecurityUtils;
import org.yeharn.lp.system.dto.MetaVo;
import org.yeharn.lp.system.dto.RouterVo;
import org.yeharn.lp.system.entity.SysMenu;
import org.yeharn.lp.system.mapper.SysMenuMapper;
import org.yeharn.lp.system.mapper.SysRoleMenuMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 菜单/权限管理
 */
@Service
@RequiredArgsConstructor
public class SysMenuService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    public List<SysMenu> list(String menuName, String status) {
        QueryWrapper qw = QueryWrapper.create()
                .like("menu_name", menuName, StrUtil.isNotBlank(menuName))
                .eq("status", status, StrUtil.isNotBlank(status))
                .orderBy("parent_id", true)
                .orderBy("order_num", true);
        return buildTree(menuMapper.selectListByQuery(qw), 0L);
    }

    public List<SysMenu> listAll() {
        return menuMapper.selectListByQuery(QueryWrapper.create().orderBy("parent_id", true).orderBy("order_num", true));
    }

    public List<SysMenu> listForUser(Long userId) {
        List<SysMenu> menus = SecurityUtils.isAdmin() ? menuMapper.selectAllMenus() : menuMapper.selectMenusByUserId(userId);
        return buildTree(menus, 0L);
    }

    public Set<String> listPermsByUser(Long userId) {
        Set<String> perms = new HashSet<>();
        List<String> list = menuMapper.selectPermsByUserId(userId);
        for (String perm : list) {
            if (StrUtil.isNotBlank(perm)) {
                for (String p : perm.trim().split(",")) {
                    if (StrUtil.isNotBlank(p)) {
                        perms.add(p.trim());
                    }
                }
            }
        }
        return perms;
    }

    public SysMenu getById(Long menuId) {
        return menuMapper.selectOneById(menuId);
    }

    public void add(SysMenu menu) {
        checkMenuNameUnique(menu);
        menu.setCreateBy(SecurityUtils.getUsername());
        menu.setCreateTime(LocalDateTime.now());
        menuMapper.insertSelective(menu);
    }

    public void update(SysMenu menu) {
        if (menu.getMenuId().equals(menu.getParentId())) {
            throw new ServiceException("上级菜单不能是自己");
        }
        checkMenuNameUnique(menu);
        menu.setUpdateBy(SecurityUtils.getUsername());
        menu.setUpdateTime(LocalDateTime.now());
        menuMapper.update(menu);
    }

    public void delete(Long menuId) {
        if (menuMapper.selectCountByQuery(QueryWrapper.create().eq("parent_id", menuId)) > 0) {
            throw new ServiceException("存在子菜单，不允许删除");
        }
        roleMenuMapper.deleteByMenuId(menuId);
        menuMapper.deleteById(menuId);
    }

    public List<Long> listMenuIdsByRole(Long roleId) {
        return menuMapper.selectMenuIdsByRoleId(roleId);
    }

    /**
     * 构建前端路由
     */
    public List<RouterVo> buildRouters(Long userId) {
        List<SysMenu> menus = SecurityUtils.isAdmin() ? menuMapper.selectAllMenus() : menuMapper.selectMenusByUserId(userId);
        return buildRouters(buildTree(menus, 0L), null);
    }

    private List<RouterVo> buildRouters(List<SysMenu> menus, String parentPath) {
        List<RouterVo> routers = new ArrayList<>();
        for (SysMenu menu : menus) {
            RouterVo router = new RouterVo();
            String path = menu.getPath() == null ? "" : menu.getPath();
            // 外链菜单：path 直接存完整地址，不做路由注册，由前端以新窗口打开
            if (isExternalLink(path)) {
                router.setName("External" + menu.getMenuId());
                router.setPath(path);
                router.setHidden("1".equals(menu.getVisible()));
                MetaVo meta = new MetaVo(menu.getMenuName(), menu.getIcon(), true);
                meta.setLink(path);
                router.setMeta(meta);
                routers.add(router);
                continue;
            }
            boolean top = menu.getParentId() == null || menu.getParentId() == 0;
            String fullPath = parentPath == null ? path : parentPath + "/" + path;
            router.setName(buildRouteName(fullPath));
            router.setPath(top ? "/" + path : path);
            router.setHidden("1".equals(menu.getVisible()));
            router.setQuery(StrUtil.isBlank(menu.getQueryParam()) ? null : menu.getQueryParam());
            router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), menu.getIsCache() != null && menu.getIsCache() == 0));

            if (top && "M".equals(menu.getMenuType())) {
                router.setComponent("Layout");
                router.setAlwaysShow(true);
                router.setRedirect("noRedirect");
                if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
                    router.setChildren(buildRouters(menu.getChildren(), fullPath));
                }
            } else if (top && "C".equals(menu.getMenuType())) {
                router.setComponent("Layout");
                RouterVo child = new RouterVo();
                child.setPath(path);
                child.setComponent(menu.getComponent());
                child.setName(buildRouteName(fullPath));
                child.setMeta(router.getMeta());
                router.setMeta(null);
                router.setChildren(List.of(child));
            } else {
                router.setComponent(menu.getComponent());
            }
            routers.add(router);
        }
        return routers;
    }

    private boolean isExternalLink(String path) {
        return path.startsWith("http://") || path.startsWith("https://") || path.startsWith("//");
    }

    private String buildRouteName(String path) {
        String[] parts = path.replace("/", " ").trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(StrUtil.upperFirst(part));
        }
        return sb.length() == 0 ? "Route" : sb.toString();
    }

    public List<SysMenu> buildTree(List<SysMenu> menus, Long parentId) {
        List<SysMenu> tree = new ArrayList<>();
        for (SysMenu menu : menus) {
            if (parentId.equals(menu.getParentId())) {
                menu.setChildren(buildTree(menus, menu.getMenuId()));
                tree.add(menu);
            }
        }
        return tree;
    }

    private void checkMenuNameUnique(SysMenu menu) {
        QueryWrapper qw = QueryWrapper.create()
                .eq("menu_name", menu.getMenuName())
                .eq("parent_id", menu.getParentId());
        if (menu.getMenuId() != null) {
            qw.ne("menu_id", menu.getMenuId());
        }
        if (menuMapper.selectCountByQuery(qw) > 0) {
            throw new ServiceException("同级菜单名称已存在");
        }
    }
}
