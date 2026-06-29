package com.ruoyi.common.utils;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ruoyi.common.filter.PropertyPreExcludeFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TimeZone;

/**
 * Jackson JSON 工具类
 *
 * @author ruoyi
 */
public class JsonUtils
{
    private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static
    {
        OBJECT_MAPPER.setTimeZone(TimeZone.getDefault());
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * 对象序列化为 JSON 字符串
     */
    public static String toJSONString(Object object)
    {
        if (object == null)
        {
            return null;
        }
        try
        {
            return OBJECT_MAPPER.writeValueAsString(object);
        }
        catch (JsonProcessingException e)
        {
            log.error("JSON 序列化失败", e);
            return null;
        }
    }

    /**
     * JSON 字符串解析为 JsonNode（等效 fastjson2 JSON.parseObject(str)）
     */
    public static JsonNode parseObject(String json)
    {
        if (StringUtils.isEmpty(json))
        {
            return null;
        }
        try
        {
            return OBJECT_MAPPER.readTree(json);
        }
        catch (JsonProcessingException e)
        {
            log.error("JSON 解析失败", e);
            return null;
        }
    }

    /**
     * 对象序列化为 JSON 字符串，排除指定属性（等效 fastjson2 JSON.toJSONString(obj, filter)）
     */
    public static String toJSONString(Object object, PropertyPreExcludeFilter filter)
    {
        if (object == null)
        {
            return null;
        }
        try
        {
            ObjectMapper copy = OBJECT_MAPPER.copy();
            copy.addMixIn(Object.class, PropertyExcludeMixin.class);
            return copy.writer(filter.getFilterProvider()).writeValueAsString(object);
        }
        catch (JsonProcessingException e)
        {
            log.error("JSON 序列化失败", e);
            return null;
        }
    }

    @JsonFilter("propertyExcludeFilter")
    private static class PropertyExcludeMixin
    {
    }

    /**
     * JSON 字符串解析为指定类型（等效 fastjson2 JSON.parseObject(str, clazz)）
     */
    public static <T> T parseObject(String json, Class<T> clazz)
    {
        if (StringUtils.isEmpty(json))
        {
            return null;
        }
        try
        {
            return OBJECT_MAPPER.readValue(json, clazz);
        }
        catch (JsonProcessingException e)
        {
            log.error("JSON 解析失败: {}", clazz.getName(), e);
            return null;
        }
    }
}
