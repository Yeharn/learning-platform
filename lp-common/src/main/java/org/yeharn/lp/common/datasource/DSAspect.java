/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: @DS 多数据源切面
 */

package org.yeharn.lp.common.datasource;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @DS 注解切面
 */
@Aspect
@Component
@Order(1)
public class DSAspect {

    @Around("@annotation(org.yeharn.lp.common.datasource.DS) || @within(org.yeharn.lp.common.datasource.DS)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        DS ds = signature.getMethod().getAnnotation(DS.class);
        if (ds == null) {
            ds = point.getTarget().getClass().getAnnotation(DS.class);
        }
        if (ds == null) {
            return point.proceed();
        }
        String old = DataSourceContextHolder.get();
        try {
            DataSourceContextHolder.set(ds.value());
            return point.proceed();
        } finally {
            if (old == null) {
                DataSourceContextHolder.clear();
            } else {
                DataSourceContextHolder.set(old);
            }
        }
    }
}
