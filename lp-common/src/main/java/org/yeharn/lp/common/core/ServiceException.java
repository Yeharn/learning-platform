/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 业务异常
 */

package org.yeharn.lp.common.core;

/**
 * 业务异常
 */
public class ServiceException extends RuntimeException {

    private final int code;

    public ServiceException(String message) {
        super(message);
        this.code = 500;
    }

    public ServiceException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
