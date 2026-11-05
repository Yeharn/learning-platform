/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 个人中心接口
 */

package org.yeharn.lp.system.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yeharn.lp.common.core.R;
import org.yeharn.lp.common.log.BusinessType;
import org.yeharn.lp.common.log.OperLog;
import org.yeharn.lp.system.dto.UserInfoVo;
import org.yeharn.lp.system.entity.SysUser;
import org.yeharn.lp.system.service.AuthService;
import org.yeharn.lp.system.service.SysUserService;

import java.util.Map;

/**
 * 个人中心
 */
@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final AuthService authService;
    private final SysUserService userService;

    @GetMapping
    public R<UserInfoVo> profile() {
        return R.ok(authService.getUserInfo());
    }

    @OperLog(title = "个人中心-修改资料", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> update(@RequestBody SysUser user) {
        userService.updateProfile(user);
        return R.ok();
    }

    @OperLog(title = "个人中心-修改密码", businessType = BusinessType.UPDATE, saveParam = false)
    @PutMapping("/password")
    public R<Void> password(@RequestBody Map<String, String> body) {
        userService.changePassword(body.get("oldPassword"), body.get("newPassword"));
        return R.ok();
    }
}
