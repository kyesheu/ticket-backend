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
        ConfigurableApplicationContext context = SpringApplication.run(RuoYiApplication.class, args);
        Environment env = context.getEnvironment();

        // 1. 动态判断协议 (有配置 SSL 证书则使用 https)
        String protocol = env.getProperty("server.ssl.key-store") != null ? "https" : "http";

        // 2. 从已启动的 Web 容器中获取绝对真实的端口
        int port = ((org.springframework.boot.web.context.WebServerApplicationContext) context).getWebServer().getPort();

        // 3. 获取真实的局域网 IP 地址
        String ip;
        try {
            ip = java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (java.net.UnknownHostException e) {
            ip = "127.0.0.1"; // 极端未联网情况下的兜底
        }

        // 4. 规范化处理 contextPath 避免双斜杠
        String contextPath = env.getProperty("server.servlet.context-path", "");
        if (contextPath == null || "/".equals(contextPath)) {
            contextPath = "";
        } else if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }

        // 5. 组装唯一的真实 IP 地址
        String baseUrl = protocol + "://" + ip + ":" + port + contextPath;

        System.out.println("\n========================================");
        System.out.println("  Ticket Backend 启动成功");
        System.out.println("  应用地址:  " + baseUrl + "/");
        System.out.println("  Swagger:   " + baseUrl + "/swagger-ui.html");
        System.out.println("  Druid:     " + baseUrl + "/druid");
        System.out.println("========================================\n");
    }
}
