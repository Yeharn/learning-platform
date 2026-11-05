/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 全局异常处理器
 */

package org.yeharn.lp.common.core;

import cn.dev33.satoken.exception.DisableServiceException;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.stream.Collectors;

/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public R<Void> handleServiceException(ServiceException e) {
        log.warn("业务异常: {}", e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(NotLoginException.class)
    public R<Void> handleNotLogin(NotLoginException e) {
        String msg = switch (e.getType()) {
            case NotLoginException.NOT_TOKEN -> "未提供登录凭证，请先登录";
            case NotLoginException.INVALID_TOKEN -> "登录凭证无效，请重新登录";
            case NotLoginException.TOKEN_TIMEOUT -> "登录已过期，请重新登录";
            case NotLoginException.BE_REPLACED -> "账号已在其它设备登录";
            case NotLoginException.KICK_OUT -> "账号已被强制下线";
            default -> "未登录，请先登录";
        };
        return R.fail(401, msg);
    }

    @ExceptionHandler(NotPermissionException.class)
    public R<Void> handleNotPermission(NotPermissionException e) {
        return R.fail(403, "没有操作权限：" + e.getCode());
    }

    @ExceptionHandler(NotRoleException.class)
    public R<Void> handleNotRole(NotRoleException e) {
        return R.fail(403, "没有角色权限：" + e.getRole());
    }

    @ExceptionHandler(DisableServiceException.class)
    public R<Void> handleDisable(DisableServiceException e) {
        return R.fail(403, "账号已被封禁");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public R<Void> handleValid(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return R.fail(400, msg);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return R.fail(400, "缺少必要参数：" + e.getParameterName());
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        HttpServletRequest request = null;
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            request = attrs.getRequest();
        }
        log.error("系统异常 [{} {}]", request == null ? "-" : request.getMethod(),
                request == null ? "-" : request.getRequestURI(), e);
        String msg = e.getMessage();
        if (msg != null && msg.contains("Duplicate entry")) {
            return R.fail(500, "数据已存在，请勿重复添加");
        }
        return R.fail(500, "系统异常，请联系管理员");
    }
}
