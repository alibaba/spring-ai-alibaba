package com.alibaba.cloud.ai.studio.admin.config;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zhuoguang
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "nacos")
public class NacosProperties {
    
    public static final String CONFIG_PREFIX = "nacos";
    
    private static final Pattern PATTERN = Pattern.compile("-(\\w)");
    
    String namespace = "public";
    
    String serverAddr;
    
    String username;
    
    String password;
    
    String accessKey;
    
    String secretKey;
    
    
    @Autowired
    @JsonIgnore
    private Environment environment;
    
    public Properties getNacosProperties() {
        Properties properties = new Properties();
        properties.put(PropertyKeyConst.NAMESPACE, Objects.toString(this.namespace, ""));
        properties.put(PropertyKeyConst.SERVER_ADDR, Objects.toString(this.serverAddr, ""));
        properties.put(PropertyKeyConst.USERNAME, Objects.toString(this.username, ""));
        properties.put(PropertyKeyConst.PASSWORD, Objects.toString(this.password, ""));
        properties.put(PropertyKeyConst.ACCESS_KEY, Objects.toString(this.accessKey, ""));
        properties.put(PropertyKeyConst.SECRET_KEY, Objects.toString(this.secretKey, ""));
        
        enrichNacosConfigProperties(properties);
        
        return properties;
    }
    
    protected void enrichNacosConfigProperties(Properties nacosConfigProperties) {
        if (environment == null) {
            return;
        }
        ConfigurableEnvironment env = (ConfigurableEnvironment) environment;
        Map<String, Object> properties = getSubProperties(env.getPropertySources(), env, CONFIG_PREFIX);
        properties.forEach((k, v) -> nacosConfigProperties.putIfAbsent(resolveKey(k), String.valueOf(v)));
    }
    
    protected String resolveKey(String key) {
        Matcher matcher = PATTERN.matcher(key);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
    
    private Map<String, Object> getSubProperties(PropertySources propertySources, PropertyResolver propertyResolver,
            String prefix) {
        
        Map<String, Object> subProperties = new LinkedHashMap<String, Object>();
        
        for (PropertySource<?> source : propertySources) {
            for (String name : getPropertyNames(source)) {
                if (!subProperties.containsKey(name) && name.startsWith(prefix)) {
                    String subName = name.substring(prefix.length() + 1);
                    if (!subProperties.containsKey(subName)) {
                        Object value = source.getProperty(name);
                        if (value instanceof String) {
                            value = propertyResolver.resolvePlaceholders((String) value);
                        }
                        subProperties.put(subName, value);
                    }
                }
            }
        }
        return Collections.unmodifiableMap(subProperties);
    }
    
    private String[] getPropertyNames(PropertySource propertySource) {
        
        String[] propertyNames = propertySource instanceof EnumerablePropertySource
                ? ((EnumerablePropertySource<?>) propertySource).getPropertyNames() : null;
        
        if (propertyNames == null) {
            return new String[0];
        }
        return propertyNames;
    }
}
