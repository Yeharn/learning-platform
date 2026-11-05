/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: 文件存储模式切换服务
 */

package org.yeharn.lp.file.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文件存储模式（存放于 sys_config，键 sys.file.mode）
 */
@Service
@RequiredArgsConstructor
public class FileModeService {

    public static final String MODE_KEY = "sys.file.mode";

    private final JdbcTemplate jdbcTemplate;

    public String current() {
        List<String> values = jdbcTemplate.query("SELECT config_value FROM sys_config WHERE config_key = ?",
                (rs, rowNum) -> rs.getString(1), MODE_KEY);
        return values.isEmpty() ? "local" : values.get(0);
    }

    public void save(String mode) {
        int updated = jdbcTemplate.update(
                "UPDATE sys_config SET config_value = ?, update_time = NOW() WHERE config_key = ?", mode, MODE_KEY);
        if (updated == 0) {
            jdbcTemplate.update("INSERT INTO sys_config(config_name, config_key, config_value, config_type, create_time) "
                    + "VALUES (?, ?, ?, 'Y', NOW())", "文件服务-存储模式", MODE_KEY, mode);
        }
    }
}
