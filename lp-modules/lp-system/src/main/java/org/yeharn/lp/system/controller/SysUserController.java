/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 系统服务 - 用户接口控制器
 */

package org.yeharn.lp.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.yeharn.lp.common.core.PageResult;
import org.yeharn.lp.common.core.R;
import org.yeharn.lp.common.log.BusinessType;
import org.yeharn.lp.common.log.OperLog;
import org.yeharn.lp.system.dto.UserQuery;
import org.yeharn.lp.system.entity.SysUser;
import org.yeharn.lp.system.service.SysUserService;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 用户管理
 */
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    @SaCheckPermission("system:user:list")
    @GetMapping("/list")
    public R<PageResult<SysUser>> list(UserQuery query) {
        return R.ok(userService.page(query));
    }

    @SaCheckPermission("system:user:query")
    @GetMapping("/{userId}")
    public R<SysUser> getInfo(@PathVariable Long userId) {
        return R.ok(userService.getById(userId));
    }

    @SaCheckPermission("system:user:add")
    @OperLog(title = "用户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@RequestBody SysUser user) {
        userService.add(user);
        return R.ok();
    }

    @SaCheckPermission("system:user:edit")
    @OperLog(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody SysUser user) {
        userService.update(user);
        return R.ok();
    }

    @SaCheckPermission("system:user:remove")
    @OperLog(title = "用户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userIds}")
    public R<Void> remove(@PathVariable List<Long> userIds) {
        userService.delete(userIds);
        return R.ok();
    }

    @SaCheckPermission("system:user:resetPwd")
    @OperLog(title = "用户管理-重置密码", businessType = BusinessType.UPDATE, saveParam = false)
    @PutMapping("/resetPwd")
    public R<Void> resetPwd(@RequestBody SysUser user) {
        userService.resetPassword(user.getUserId(), user.getPassword());
        return R.ok();
    }

    @SaCheckPermission("system:user:edit")
    @OperLog(title = "用户管理-状态变更", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody SysUser user) {
        userService.changeStatus(user.getUserId(), user.getStatus());
        return R.ok();
    }

    @SaCheckPermission("system:user:export")
    @OperLog(title = "用户管理-导出", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public void export(UserQuery query, HttpServletResponse response) throws Exception {
        query.setPageNum(1);
        query.setPageSize(500);
        List<SysUser> users = userService.page(query).getRows();
        StringBuilder sb = new StringBuilder();
        sb.append("用户ID,登录账号,昵称,部门,手机号,状态,创建时间\n");
        for (SysUser u : users) {
            sb.append(u.getUserId()).append(',')
                    .append(csv(u.getUsername())).append(',')
                    .append(csv(u.getNickname())).append(',')
                    .append(csv(u.getDeptName())).append(',')
                    .append(csv(u.getPhone())).append(',')
                    .append("0".equals(u.getStatus()) ? "正常" : "停用").append(',')
                    .append(u.getCreateTime() == null ? "" : u.getCreateTime()).append('\n');
        }
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=users.csv");
        OutputStream out = response.getOutputStream();
        out.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    @GetMapping("/checkUsername")
    public R<Boolean> checkUsername(@RequestParam String username) {
        return R.ok(userService.getByUsername(username) == null);
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
