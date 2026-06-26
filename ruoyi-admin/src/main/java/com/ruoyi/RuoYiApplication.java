package com.ruoyi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 启动程序
 * 
 * @author ruoyi
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class RuoYiApplication
{
    public static void main(String[] args)
    {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(RuoYiApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("  Ticket Backend 启动成功");
        System.out.println("  应用地址:  http://localhost:8080");
        System.out.println("  Swagger:   http://localhost:8080/swagger-ui.html");
        System.out.println("  Druid:     http://localhost:8080/druid");
        System.out.println("========================================\n");
    }
}
