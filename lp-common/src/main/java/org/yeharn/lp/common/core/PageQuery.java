/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 分页查询参数基类
 */

package org.yeharn.lp.common.core;

import lombok.Data;

import java.io.Serializable;

/**
 * 分页查询基础参数
 */
@Data
public class PageQuery implements Serializable {

    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String orderByColumn;
    private String isAsc = "desc";

    public int getPageNumSafe() {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    public int getPageSizeSafe() {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 500);
    }
}
