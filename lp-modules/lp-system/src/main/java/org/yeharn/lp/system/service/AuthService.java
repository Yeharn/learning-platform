/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 认证业务服务
 */

package org.yeharn.lp.system.service;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.yeharn.lp.common.core.Constants;
import org.yeharn.lp.common.core.ServiceException;
import org.yeharn.lp.common.redis.RedisUtils;
import org.yeharn.lp.common.security.LoginUser;
import org.yeharn.lp.common.security.SecurityUtils;
import org.yeharn.lp.system.dto.CaptchaVo;
import org.yeharn.lp.system.dto.LoginBody;
import org.yeharn.lp.system.dto.LoginVo;
import org.yeharn.lp.system.dto.RouterVo;
import org.yeharn.lp.system.dto.UserInfoVo;
import org.yeharn.lp.system.entity.SysLoginLog;
import org.yeharn.lp.system.entity.SysRole;
import org.yeharn.lp.system.entity.SysUser;
import org.yeharn.lp.system.mapper.SysLoginLogMapper;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserService userService;
    private final SysRoleService roleService;
    private final SysMenuService menuService;
    private final SysConfigService configService;
    private final SysLoginLogMapper loginLogMapper;
    private final LogTableService logTableService;
    private final RedisUtils redisUtils;
    private final PasswordEncoder passwordEncoder;
    private final OnlineUserService onlineUserService;

    public CaptchaVo captcha() {
        boolean enabled = configService.getBoolean("sys.account.captchaEnabled", true);
        if (!enabled) {
            return new CaptchaVo(UUID.randomUUID().toString(), "", false);
        }
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(130, 46, 4, 20);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        redisUtils.set(Constants.CAPTCHA_CODE_KEY + uuid, captcha.getCode(), 120);
        return new CaptchaVo(uuid, captcha.getImageBase64Data(), true);
    }

    public LoginVo login(LoginBody body, HttpServletRequest request) {
        String ip = JakartaServletUtil.getClientIP(request);
        String ua = request.getHeader("User-Agent");
        try {
            validateCaptcha(body);
            SysUser user = userService.getByUsername(body.getUsername());
            if (user == null) {
                throw new ServiceException("用户名或密码错误");
            }
            if (!passwordEncoder.matches(body.getPassword(), user.getPassword())) {
                throw new ServiceException("用户名或密码错误");
            }
            if ("1".equals(user.getStatus())) {
                throw new ServiceException("账号已停用，请联系管理员");
            }

            long timeout = Boolean.TRUE.equals(body.getRememberMe()) ? 7 * 24 * 3600L : 86400L;
            SaLoginModel model = new SaLoginModel().setTimeout(timeout).setDevice("pc");
            StpUtil.login(user.getUserId(), model);

            LoginUser loginUser = buildLoginUser(user);
            loginUser.setLoginIp(ip);
            loginUser.setLoginTime(System.currentTimeMillis());
            loginUser.setExpireTime(System.currentTimeMillis() + timeout * 1000);
            fillClient(loginUser, ua);
            StpUtil.getSession().set(Constants.TOKEN_SESSION_USER, loginUser);

            userService.updateLoginInfo(user.getUserId(), ip, LocalDateTime.now());
            onlineUserService.save(StpUtil.getTokenValue(), loginUser, timeout);
            recordLog(body.getUsername(), ip, ua, "0", "登录成功");
            return new LoginVo(StpUtil.getTokenValue(), StpUtil.getTokenName(), timeout);
        } catch (ServiceException e) {
            recordLog(body.getUsername(), ip, ua, "1", e.getMessage());
            throw e;
        } catch (Exception e) {
            recordLog(body.getUsername(), ip, ua, "1", "登录异常：" + e.getMessage());
            throw new ServiceException("登录失败：" + e.getMessage());
        }
    }

    public void logout() {
        try {
            onlineUserService.remove(StpUtil.getTokenValue());
            StpUtil.logout();
        } catch (Exception ignored) {
            // 未登录时忽略
        }
    }

    public UserInfoVo getUserInfo() {
        LoginUser loginUser = SecurityUtils.getLoginUserOrNull();
        if (loginUser == null) {
            throw new ServiceException(401, "登录已失效，请重新登录");
        }
        SysUser user = userService.getById(loginUser.getUserId());
        if (user == null) {
            throw new ServiceException(401, "用户不存在");
        }
        UserInfoVo vo = new UserInfoVo();
        vo.setUser(user);
        vo.setRoles(loginUser.getRoles());
        vo.setPermissions(loginUser.getPermissions());
        vo.setAdmin(loginUser.isAdmin());
        return vo;
    }

    public List<RouterVo> getRouters() {
        Long userId = StpUtil.getLoginIdAsLong();
        return menuService.buildRouters(userId);
    }

    public LoginUser buildLoginUser(SysUser user) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getUserId());
        loginUser.setUsername(user.getUsername());
        loginUser.setNickname(user.getNickname());
        loginUser.setDeptId(user.getDeptId());
        loginUser.setDeptName(user.getDeptName());
        loginUser.setAvatar(user.getAvatar());

        List<SysRole> roles = roleService.listByUserId(user.getUserId());
        Set<String> roleKeys = new HashSet<>();
        roles.forEach(r -> roleKeys.add(r.getRoleKey()));
        loginUser.setRoles(roleKeys);

        boolean admin = roleKeys.contains(Constants.ADMIN_ROLE_KEY);
        if (admin) {
            loginUser.getPermissions().add(Constants.ALL_PERMISSION);
            loginUser.setDataScope("1");
        } else {
            loginUser.getPermissions().addAll(userService.getPermissions(user.getUserId()));
            loginUser.setDataScope(roles.stream()
                    .map(SysRole::getDataScope)
                    .filter(StrUtil::isNotBlank)
                    .min(String::compareTo)
                    .orElse("1"));
        }
        return loginUser;
    }

    private void validateCaptcha(LoginBody body) {
        if (!configService.getBoolean("sys.account.captchaEnabled", true)) {
            return;
        }
        if (StrUtil.isBlank(body.getUuid()) || StrUtil.isBlank(body.getCode())) {
            throw new ServiceException("请输入验证码");
        }
        String key = Constants.CAPTCHA_CODE_KEY + body.getUuid();
        String code = redisUtils.getString(key);
        redisUtils.delete(key);
        if (code == null) {
            throw new ServiceException("验证码已过期，请刷新");
        }
        if (!code.equalsIgnoreCase(body.getCode().trim())) {
            throw new ServiceException("验证码错误");
        }
    }

    private void fillClient(LoginUser loginUser, String ua) {
        if (StrUtil.isBlank(ua)) {
            return;
        }
        UserAgent agent = UserAgentUtil.parse(ua);
        loginUser.setBrowser(agent.getBrowser() == null ? "Unknown" : agent.getBrowser().getName());
        loginUser.setOs(agent.getOs() == null ? "Unknown" : agent.getOs().getName());
    }

    private void recordLog(String username, String ip, String ua, String status, String msg) {
        try {
            SysLoginLog log = new SysLoginLog();
            log.setUsername(username);
            log.setIpaddr(ip);
            log.setLoginLocation(StrUtil.startWithAny(ip, "127.", "192.168.", "10.", "172.") ? "内网IP" : "外网IP");
            if (StrUtil.isNotBlank(ua)) {
                UserAgent agent = UserAgentUtil.parse(ua);
                log.setBrowser(agent.getBrowser() == null ? "" : agent.getBrowser().getName());
                log.setOs(agent.getOs() == null ? "" : agent.getOs().getName());
            }
            log.setStatus(status);
            log.setMsg(StrUtil.sub(msg, 0, 250));
            log.setLoginTime(LocalDateTime.now());
            logTableService.insertLoginLog(log);
        } catch (Exception e) {
            log.warn("记录登录日志失败: {}", e.getMessage());
        }
    }
}
