package com.ruoyi.common.filter;

import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 排除JSON敏感属性
 *
 * @author ruoyi
 */
public class PropertyPreExcludeFilter
{
    private final Set<String> excludes = new HashSet<>();

    public PropertyPreExcludeFilter()
    {
    }

    public PropertyPreExcludeFilter addExcludes(String... filters)
    {
        this.excludes.addAll(Arrays.asList(filters));
        return this;
    }

    /**
     * 生成 Jackson FilterProvider，配合 JsonUtils 使用
     */
    public FilterProvider getFilterProvider()
    {
        return new SimpleFilterProvider()
                .addFilter("propertyExcludeFilter",
                        SimpleBeanPropertyFilter.serializeAllExcept(excludes.toArray(new String[0])));
    }
}
