/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 操作日志业务类型枚举
 */

package org.yeharn.lp.common.log;

/**
 * 业务操作类型
 */
public enum BusinessType {

    OTHER(0, "其它"),
    INSERT(1, "新增"),
    UPDATE(2, "修改"),
    DELETE(3, "删除"),
    GRANT(4, "授权"),
    EXPORT(5, "导出"),
    IMPORT(6, "导入"),
    FORCE_LOGOUT(7, "强退"),
    GENCODE(8, "生成代码"),
    CLEAN(9, "清空数据");

    private final int code;
    private final String desc;

    BusinessType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
