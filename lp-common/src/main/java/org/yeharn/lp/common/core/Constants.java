/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 全局常量定义
 */

package org.yeharn.lp.common.core;

/**
 * 通用常量
 */
public interface Constants {

    String TOKEN_SESSION_USER = "loginUser";
    String CAPTCHA_CODE_KEY = "captcha:code:";
    String CONFIG_CACHE_KEY = "sys:config:";
    String DICT_CACHE_KEY = "sys:dict:";
    String PERM_CACHE_KEY = "sys:perm:";
    String ROLE_CACHE_KEY = "sys:role:";

    String ADMIN_ROLE_KEY = "admin";
    String ALL_PERMISSION = "*:*:*";

    String YES = "Y";
    String NO = "N";
    String STATUS_NORMAL = "0";
    String STATUS_DISABLE = "1";
    String DEL_FLAG_NORMAL = "0";
    String DEL_FLAG_DELETED = "2";

    String DS_MASTER = "master";
}
