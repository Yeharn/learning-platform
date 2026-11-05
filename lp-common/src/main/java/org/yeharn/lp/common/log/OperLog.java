/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 操作日志注解
 */

package org.yeharn.lp.common.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperLog {

    /** 模块标题 */
    String title() default "";

    /** 业务类型 */
    BusinessType businessType() default BusinessType.OTHER;

    /** 是否保存请求参数 */
    boolean saveParam() default true;

    /** 是否保存返回结果 */
    boolean saveResult() default false;
}
