/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 认证接口：验证码、登录、用户信息、动态路由
 */

package org.yeharn.lp.system.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yeharn.lp.common.core.R;
import org.yeharn.lp.system.dto.CaptchaVo;
import org.yeharn.lp.system.dto.LoginBody;
import org.yeharn.lp.system.dto.LoginVo;
import org.yeharn.lp.system.dto.RouterVo;
import org.yeharn.lp.system.dto.UserInfoVo;
import org.yeharn.lp.system.service.AuthService;

import java.util.List;

/**
 * 认证接口
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/captcha")
    public R<CaptchaVo> captcha() {
        return R.ok(authService.captcha());
    }

    @PostMapping("/login")
    public R<LoginVo> login(@Valid @RequestBody LoginBody body, HttpServletRequest request) {
        return R.ok("登录成功", authService.login(body, request));
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok("退出成功", null);
    }

    @GetMapping("/info")
    public R<UserInfoVo> info() {
        return R.ok(authService.getUserInfo());
    }

    @GetMapping("/routers")
    public R<List<RouterVo>> routers() {
        return R.ok(authService.getRouters());
    }
}
