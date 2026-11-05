/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 在线用户接口
 */

package org.yeharn.lp.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.yeharn.lp.common.core.PageResult;
import org.yeharn.lp.common.core.R;
import org.yeharn.lp.common.log.BusinessType;
import org.yeharn.lp.common.log.OperLog;
import org.yeharn.lp.common.security.LoginUser;
import org.yeharn.lp.system.service.OnlineUserService;

import java.util.List;

/**
 * 在线用户
 */
@RestController
@RequestMapping("/online")
@RequiredArgsConstructor
public class OnlineController {

    private final OnlineUserService onlineUserService;

    @SaCheckPermission("monitor:online:list")
    @GetMapping("/list")
    public R<List<LoginUser>> list(@RequestParam(required = false) String username) {
        return R.ok(onlineUserService.list(username));
    }

    @SaCheckPermission("monitor:online:list")
    @GetMapping("/page")
    public R<PageResult<LoginUser>> page(@RequestParam(required = false) String username,
                                         @RequestParam(defaultValue = "1") long pageNum,
                                         @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(onlineUserService.page(username, pageNum, pageSize));
    }

    /**
     * 在线人数：登录用户均可访问，仅返回数量用于顶部徽标联动
     */
    @SaCheckLogin
    @GetMapping("/count")
    public R<Long> count() {
        return R.ok(onlineUserService.count());
    }

    @SaCheckPermission("monitor:online:kick")
    @OperLog(title = "在线用户-强制下线", businessType = BusinessType.FORCE_LOGOUT)
    @DeleteMapping("/{token}")
    public R<Void> kick(@PathVariable String token) {
        onlineUserService.kick(token);
        try {
            StpUtil.logoutByTokenValue(token);
        } catch (Exception ignored) {
            // 忽略
        }
        return R.ok();
    }
}
