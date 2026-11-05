/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: Jackson 序列化配置
 */

package org.yeharn.lp.common.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * JSON 序列化配置：Long 转字符串防止前端精度丢失，时间统一格式
 */
@Configuration
public class JacksonConfig {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        DateTimeFormatter dateTime = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        DateTimeFormatter date = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter time = DateTimeFormatter.ofPattern("HH:mm:ss");
        return builder -> {
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTime));
            module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTime));
            module.addSerializer(LocalDate.class, new LocalDateSerializer(date));
            module.addSerializer(LocalTime.class, new LocalTimeSerializer(time));
            builder.modules(module);
            builder.simpleDateFormat(DATE_TIME_PATTERN);
        };
    }
}
