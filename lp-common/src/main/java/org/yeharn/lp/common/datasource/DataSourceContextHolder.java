/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 数据源上下文持有者
 */

package org.yeharn.lp.common.datasource;

import org.yeharn.lp.common.core.Constants;

/**
 * 数据源上下文（线程隔离）
 */
public final class DataSourceContextHolder {

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    private DataSourceContextHolder() {
    }

    public static void set(String key) {
        CONTEXT.set(key);
    }

    public static String get() {
        return CONTEXT.get();
    }

    public static String getOrDefault() {
        String key = CONTEXT.get();
        return key == null ? Constants.DS_MASTER : key;
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static <T> T execute(String key, java.util.function.Supplier<T> supplier) {
        String old = CONTEXT.get();
        try {
            set(key);
            return supplier.get();
        } finally {
            if (old == null) {
                clear();
            } else {
                set(old);
            }
        }
    }

    public static void execute(String key, Runnable runnable) {
        execute(key, () -> {
            runnable.run();
            return null;
        });
    }
}
