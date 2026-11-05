/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 操作日志切面
 */

package org.yeharn.lp.common.log;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.yeharn.lp.common.security.LoginUser;
import org.yeharn.lp.common.security.SecurityUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 操作日志切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    private final ApplicationEventPublisher eventPublisher;

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint point, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();
        OperLogEvent event = new OperLogEvent();
        event.setTitle(operLog.title());
        event.setBusinessType(operLog.businessType().getCode());
        event.setOperTime(LocalDateTime.now());
        event.setStatus(0);

        MethodSignature signature = (MethodSignature) point.getSignature();
        event.setMethod(point.getTarget().getClass().getSimpleName() + "." + signature.getName() + "()");

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes == null ? null : attributes.getRequest();
        if (request != null) {
            event.setRequestMethod(request.getMethod());
            event.setOperUrl(StrUtil.sub(request.getRequestURI(), 0, 255));
            event.setOperIp(JakartaServletUtil.getClientIP(request));
        }
        LoginUser loginUser = SecurityUtils.getLoginUserOrNull();
        if (loginUser != null) {
            event.setOperName(loginUser.getUsername());
            event.setDeptName(loginUser.getDeptName());
        }
        if (operLog.saveParam()) {
            try {
                String params = Arrays.stream(point.getArgs())
                        .filter(arg -> !(arg instanceof MultipartFile)
                                && !(arg instanceof jakarta.servlet.ServletRequest)
                                && !(arg instanceof jakarta.servlet.ServletResponse))
                        .map(JSONUtil::toJsonStr)
                        .collect(Collectors.joining(","));
                event.setOperParam(StrUtil.sub(params, 0, 1900));
            } catch (Exception e) {
                event.setOperParam("参数序列化失败");
            }
        }

        try {
            Object result = point.proceed();
            if (operLog.saveResult()) {
                try {
                    event.setJsonResult(StrUtil.sub(JSONUtil.toJsonStr(result), 0, 1900));
                } catch (Exception ignored) {
                    // 忽略结果序列化异常
                }
            }
            return result;
        } catch (Throwable e) {
            event.setStatus(1);
            event.setErrorMsg(StrUtil.sub(e.getMessage(), 0, 1900));
            throw e;
        } finally {
            event.setCostTime(System.currentTimeMillis() - start);
            try {
                eventPublisher.publishEvent(event);
            } catch (Exception e) {
                log.warn("操作日志发布失败: {}", e.getMessage());
            }
        }
    }
}
