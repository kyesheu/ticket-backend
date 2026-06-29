package com.ruoyi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

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
        ConfigurableApplicationContext context = SpringApplication.run(RuoYiApplication.class, args);
        Environment env = context.getEnvironment();

        String port = env.getProperty("local.server.port",
                env.getProperty("server.port", "8080"));
        String contextPath = env.getProperty("server.servlet.context-path", "");

        // 确保 contextPath 既不为 null，也不只是一个孤零零的斜杠
        if (contextPath == null || "/".equals(contextPath)) {
            contextPath = "";
        } else if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }

        String baseUrl = "http://localhost:" + port + contextPath;

        System.out.println("\n========================================");
        System.out.println("  Ticket Backend 启动成功");
        System.out.println("  应用地址:  " + baseUrl + "/");
        System.out.println("  Swagger:   " + baseUrl + "/swagger-ui.html");
        System.out.println("  Druid:     " + baseUrl + "/druid");
        System.out.println("========================================\n");
    }
}
