package com.ruoyi.web.core.config;

import java.util.Collection;
import java.util.Map;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Swagger 接口配置
 *
 * @author ruoyi
 */
@Configuration
public class SwaggerConfig {

    @Value("${springdoc.info.title:API文档}")
    private String title;

    @Value("${springdoc.info.description:}")
    private String description;

    @Value("${springdoc.info.version:1.0.0}")
    private String version;

    @Value("${ruoyi.name:}")
    private String appName;

    /**
     * 自定义 OpenAPI 信息（仅设置 info 和 security，不覆盖注解扫描的 tags）
     */
    @Bean
    public OpenAPI customOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title(title)
                .description(description)
                .contact(new Contact().name(appName))
                .version(version));
    }

    /**
     * 通过 Customizer 添加 security scheme，避免覆盖注解生成的 tags
     */
    @Bean
    public GlobalOpenApiCustomizer securityCustomizer() {
        return openApi -> {
            Components components = openApi.getComponents();
            if (components == null) {
                components = new Components();
                openApi.setComponents(components);
            }
            components.addSecuritySchemes("apikey", new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .name("Authorization")
                .in(SecurityScheme.In.HEADER)
                .scheme("Bearer"));
            openApi.addSecurityItem(new SecurityRequirement().addList("apikey"));
        };
    }

    /**
     * 补全文档说明，兼容只填写 summary/title 的注解写法。
     */
    @Bean
    public GlobalOpenApiCustomizer descriptionCustomizer() {
        return openApi -> {
            if (openApi.getPaths() != null) {
                openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(this::fillOperationDescription));
            }
            if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
                openApi.getComponents().getSchemas().values().forEach(this::fillSchemaDescription);
            }
        };
    }

    private void fillOperationDescription(Operation operation) {
        if (!StringUtils.hasText(operation.getDescription()) && StringUtils.hasText(operation.getSummary())) {
            operation.setDescription(operation.getSummary());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void fillSchemaDescription(Schema schema) {
        if (schema == null) {
            return;
        }
        if (!StringUtils.hasText(schema.getDescription()) && StringUtils.hasText(schema.getTitle())) {
            schema.setDescription(schema.getTitle());
        }
        fillSchemaDescription(schema.getItems());
        fillSchemaDescription(schema.getNot());
        fillSchemaDescriptions(schema.getAllOf());
        fillSchemaDescriptions(schema.getAnyOf());
        fillSchemaDescriptions(schema.getOneOf());

        Map<String, Schema> properties = schema.getProperties();
        if (properties != null) {
            properties.values().forEach(this::fillSchemaDescription);
        }
        Object additionalProperties = schema.getAdditionalProperties();
        if (additionalProperties instanceof Schema additionalPropertiesSchema) {
            fillSchemaDescription(additionalPropertiesSchema);
        }
    }

    @SuppressWarnings("rawtypes")
    private void fillSchemaDescriptions(Collection<Schema> schemas) {
        if (schemas != null) {
            schemas.forEach(this::fillSchemaDescription);
        }
    }
}
