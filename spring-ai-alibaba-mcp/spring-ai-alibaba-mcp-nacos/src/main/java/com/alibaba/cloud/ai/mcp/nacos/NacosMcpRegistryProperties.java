package com.alibaba.cloud.ai.mcp.nacos;

import com.alibaba.nacos.api.utils.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.alibaba.nacos.api.PropertyKeyConst.ACCESS_KEY;
import static com.alibaba.nacos.api.PropertyKeyConst.ENDPOINT;
import static com.alibaba.nacos.api.PropertyKeyConst.ENDPOINT_PORT;
import static com.alibaba.nacos.api.PropertyKeyConst.NAMESPACE;
import static com.alibaba.nacos.api.PropertyKeyConst.PASSWORD;
import static com.alibaba.nacos.api.PropertyKeyConst.SECRET_KEY;
import static com.alibaba.nacos.api.PropertyKeyConst.SERVER_ADDR;
import static com.alibaba.nacos.api.PropertyKeyConst.USERNAME;
import static java.util.Collections.unmodifiableMap;

/**
 * @author Sunrisea
 */
@ConfigurationProperties(prefix = "spring.ai.alibaba.mcp.nacos")
public class NacosMcpRegistryProperties {

	public static final String DEFAULT_NAMESPACE = "public";

	public static final String DEFAULT_ADDRESS = "127.0.0.1:8848";

	private static final Pattern PATTERN = Pattern.compile("-(\\w)");

	private static final Logger log = LoggerFactory.getLogger(NacosMcpRegistryProperties.class);

	@Autowired
	@JsonIgnore
	private Environment environment;

	String serverAddr;

	String namespace;

	String group = "DEFAULT_GROUP";

	String username;

	String password;

	String accessKey;

	String secretKey;

	String endpoint;

	String ip;

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getAccessKey() {
		return accessKey;
	}

	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	String getServerAddr() {
		return serverAddr;
	}

	void setServerAddr(String serverAddr) {
		this.serverAddr = serverAddr;
	}

	String getNamespace() {
		return namespace;
	}

	void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	@PostConstruct
	public void init() throws Exception {
		if (StringUtils.isEmpty(this.ip)) {
			this.ip = getFirstNonLoopbackIPv4Address().getHostAddress();
		}
		if (DEFAULT_NAMESPACE.equals(this.namespace)) {
			this.namespace = "";
		}
		if (StringUtils.isBlank(this.namespace)) {
			this.namespace = "";
		}
	}

	public Properties getNacosProperties() {
		Properties properties = new Properties();
		properties.put(SERVER_ADDR, Objects.toString(this.serverAddr, ""));
		properties.put(USERNAME, Objects.toString(this.username, ""));
		properties.put(PASSWORD, Objects.toString(this.password, ""));
		properties.put(NAMESPACE, this.resolveNamespace());
		properties.put(ACCESS_KEY, Objects.toString(this.accessKey, ""));
		properties.put(SECRET_KEY, Objects.toString(this.secretKey, ""));
		String endpoint = Objects.toString(this.endpoint, "");
		if (endpoint.contains(":")) {
			int index = endpoint.indexOf(":");
			properties.put(ENDPOINT, endpoint.substring(0, index));
			properties.put(ENDPOINT_PORT, endpoint.substring(index + 1));
		}
		else {
			properties.put(ENDPOINT, endpoint);
		}

		enrichNacosConfigProperties(properties);

		if (StringUtils.isEmpty(this.serverAddr) && StringUtils.isEmpty(this.endpoint)) {
			properties.put(SERVER_ADDR, DEFAULT_ADDRESS);
		}

		return properties;
	}

	private String resolveNamespace() {
		if (DEFAULT_NAMESPACE.equals(this.namespace)) {
			return "";
		}
		else {
			return Objects.toString(this.namespace, "");
		}
	}

	protected void enrichNacosConfigProperties(Properties nacosConfigProperties) {
		if (environment == null) {
			return;
		}
		String prefix = "spring.ai.alibaba.mcp.nacos";

		ConfigurableEnvironment env = (ConfigurableEnvironment) environment;
		Map<String, Object> properties = getSubProperties(env.getPropertySources(), env, prefix);
		properties.forEach((k, v) -> nacosConfigProperties.putIfAbsent(resolveKey(k), String.valueOf(v)));
	}

	protected String resolveKey(String key) {
		Matcher matcher = PATTERN.matcher(key);
		StringBuffer sb = new StringBuffer();
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
					String subName = name.substring(prefix.length());
					if (!subProperties.containsKey(subName)) { // take first one
						Object value = source.getProperty(name);
						if (value instanceof String) {
							value = propertyResolver.resolvePlaceholders((String) value);
						}
						subProperties.put(subName, value);
					}
				}
			}
		}
		return unmodifiableMap(subProperties);
	}

	private String[] getPropertyNames(PropertySource propertySource) {

		String[] propertyNames = propertySource instanceof EnumerablePropertySource
				? ((EnumerablePropertySource<?>) propertySource).getPropertyNames() : null;

		if (propertyNames == null) {
			return new String[0];
		}
		return propertyNames;
	}

	public static InetAddress getFirstNonLoopbackIPv4Address() {
		try {
			Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
			while (networkInterfaces.hasMoreElements()) {
				NetworkInterface networkInterface = networkInterfaces.nextElement();
				if (!networkInterface.isLoopback() && networkInterface.isUp()) {
					Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
					while (inetAddresses.hasMoreElements()) {
						InetAddress inetAddress = inetAddresses.nextElement();
						if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()
								&& !inetAddress.isLinkLocalAddress() && !inetAddress.isAnyLocalAddress()
								&& !inetAddress.isSiteLocalAddress()) {
							return inetAddress;
						}
					}
				}
			}
		}
		catch (IOException e) {
			log.error("Error getting first non-loopback IPv4 address", e);
		}
		return null;
	}

}
