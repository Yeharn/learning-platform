/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 菜单
 */

package org.yeharn.lp.system.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.yeharn.lp.common.core.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Table("sys_menu")
public class SysMenu extends BaseEntity {

    @Id(keyType = KeyType.Auto)
    private Long menuId;

    private String menuName;
    private Long parentId;
    private Integer orderNum;
    private String path;
    private String component;
    private String queryParam;
    private Integer isFrame;
    private Integer isCache;
    private String menuType;
    private String visible;
    private String status;
    private String perms;
    private String icon;
    private String remark;

    @Column(ignore = true)
    private List<SysMenu> children = new ArrayList<>();

    @Column(ignore = true)
    private String parentName;

    public boolean isMenu() {
        return "M".equals(menuType) || "C".equals(menuType);
    }
}
