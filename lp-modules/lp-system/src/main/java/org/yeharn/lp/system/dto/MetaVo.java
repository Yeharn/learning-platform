/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 元信息视图对象
 */

package org.yeharn.lp.system.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetaVo {

    private String title;
    private String icon;
    private boolean noCache;
    private String link;

    public MetaVo() {
    }

    public MetaVo(String title, String icon, boolean noCache) {
        this.title = title;
        this.icon = icon;
        this.noCache = noCache;
    }
}
