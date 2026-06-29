package com.ruoyi.framework.config;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 应用启动信息日志
 *
 * @author ruoyi
 */
@Component
public class StartupInfoLogger
{
    private static final Logger log = LoggerFactory.getLogger(StartupInfoLogger.class);

    private final Environment environment;

    private final WebServerApplicationContext webServerApplicationContext;

    public StartupInfoLogger(Environment environment, WebServerApplicationContext webServerApplicationContext)
    {
        this.environment = environment;
        this.webServerApplicationContext = webServerApplicationContext;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logStartupInfo()
    {
        String protocol = getProtocol();
        int port = webServerApplicationContext.getWebServer().getPort();
        String contextPath = getContextPath();
        String baseUrl = protocol + "://localhost:" + port + contextPath;

        boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        if (prod)
        {
            log.info("\n----------------------------------------------------------\n" +
                    "  Ticket Backend 启动成功\n" +
                    "  当前环境:   {}\n" +
                    "  访问地址:   {}\n" +
                    "----------------------------------------------------------",
                    Arrays.toString(environment.getActiveProfiles()),
                    baseUrl + "/");
            return;
        }

        log.info("\n----------------------------------------------------------\n" +
                "  Ticket Backend 启动成功\n" +
                "  当前环境:   {}\n" +
                "  本地地址:   {}\n" +
                "  Swagger:    {}\n" +
                "  Druid:      {}\n" +
                "----------------------------------------------------------",
                Arrays.toString(environment.getActiveProfiles()),
                baseUrl + "/",
                baseUrl + "/swagger-ui.html",
                baseUrl + "/druid");
    }

    private String getProtocol()
    {
        boolean sslEnabled = Boolean.parseBoolean(environment.getProperty("server.ssl.enabled", "false"));
        boolean hasKeyStore = environment.getProperty("server.ssl.key-store") != null;
        return (sslEnabled || hasKeyStore) ? "https" : "http";
    }

    private String getContextPath()
    {
        String contextPath = environment.getProperty("server.servlet.context-path", "");

        if (contextPath == null || contextPath.isBlank() || "/".equals(contextPath))
        {
            return "";
        }

        if (!contextPath.startsWith("/"))
        {
            contextPath = "/" + contextPath;
        }

        if (contextPath.endsWith("/"))
        {
            contextPath = contextPath.substring(0, contextPath.length() - 1);
        }

        return contextPath;
    }
}
