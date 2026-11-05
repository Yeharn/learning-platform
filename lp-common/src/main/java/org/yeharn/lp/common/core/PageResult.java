/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 分页结果封装
 */

package org.yeharn.lp.common.core;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页结果
 */
@Data
public class PageResult<T> implements Serializable {

    private long total;
    private List<T> rows;
    private long pageNum;
    private long pageSize;

    public PageResult() {
    }

    public PageResult(long total, List<T> rows) {
        this.total = total;
        this.rows = rows == null ? Collections.emptyList() : rows;
    }

    public PageResult(long total, List<T> rows, long pageNum, long pageSize) {
        this(total, rows);
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>(0, Collections.emptyList());
    }
}
