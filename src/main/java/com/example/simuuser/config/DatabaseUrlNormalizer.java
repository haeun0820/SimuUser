package com.example.simuuser.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

public class DatabaseUrlNormalizer
		implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

	private static final String PROPERTY_SOURCE_NAME = "databaseUrlNormalizer";

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		ConfigurableEnvironment environment = event.getEnvironment();
		String configuredUrl = firstNonBlank(
				environment.getProperty("spring.datasource.url"),
				environment.getProperty("DATABASE_URL"));

		if (!StringUtils.hasText(configuredUrl)) {
			return;
		}

		Map<String, Object> normalized = normalizeDatabaseProperties(environment, configuredUrl);
		if (!normalized.isEmpty()) {
			environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, normalized));
		}
	}

	private Map<String, Object> normalizeDatabaseProperties(
			ConfigurableEnvironment environment,
			String configuredUrl
	) {
		Map<String, Object> properties = new LinkedHashMap<>();

		if (configuredUrl.startsWith("jdbc:")) {
			properties.put("spring.datasource.url", configuredUrl);

			if (!StringUtils.hasText(environment.getProperty("spring.datasource.driver-class-name"))) {
				addVendorDefaults(properties, configuredUrl);
			}

			return properties;
		}

		try {
			URI uri = new URI(configuredUrl);
			String scheme = uri.getScheme();
			if (!StringUtils.hasText(scheme)) {
				return properties;
			}

			String jdbcUrl = buildJdbcUrl(uri, scheme);
			if (!StringUtils.hasText(jdbcUrl)) {
				return properties;
			}

			properties.put("spring.datasource.url", jdbcUrl);

			String userInfo = uri.getUserInfo();
			if (StringUtils.hasText(userInfo)) {
				String[] credentials = userInfo.split(":", 2);
				if (credentials.length > 0
						&& StringUtils.hasText(credentials[0])
						&& !StringUtils.hasText(environment.getProperty("spring.datasource.username"))) {
					properties.put("spring.datasource.username", credentials[0]);
				}
				if (credentials.length > 1
						&& !StringUtils.hasText(environment.getProperty("spring.datasource.password"))) {
					properties.put("spring.datasource.password", credentials[1]);
				}
			}

			addVendorDefaults(properties, scheme);
		} catch (URISyntaxException ignored) {
			// Leave the original datasource properties untouched if DATABASE_URL is malformed.
		}

		return properties;
	}

	private String buildJdbcUrl(URI uri, String scheme) {
		String normalizedScheme = scheme.toLowerCase();
		if (!("postgres".equals(normalizedScheme)
				|| "postgresql".equals(normalizedScheme)
				|| "mysql".equals(normalizedScheme))) {
			return null;
		}

		String host = uri.getHost();
		if (!StringUtils.hasText(host)) {
			return null;
		}

		int port = uri.getPort();
		String path = StringUtils.hasText(uri.getPath()) ? uri.getPath() : "";
		String query = StringUtils.hasText(uri.getQuery()) ? "?" + uri.getQuery() : "";
		String jdbcScheme = "mysql".equals(normalizedScheme) ? "mysql" : "postgresql";

		return "jdbc:" + jdbcScheme + "://" + host
				+ (port > 0 ? ":" + port : "")
				+ path
				+ query;
	}

	private void addVendorDefaults(Map<String, Object> properties, String urlOrScheme) {
		String lowerValue = urlOrScheme.toLowerCase();
		if (lowerValue.contains("postgres")) {
			properties.putIfAbsent("spring.datasource.driver-class-name", "org.postgresql.Driver");
			properties.putIfAbsent("spring.jpa.database-platform", "org.hibernate.dialect.PostgreSQLDialect");
			return;
		}

		if (lowerValue.contains("mysql")) {
			properties.putIfAbsent("spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver");
		}
	}

	private String firstNonBlank(String... values) {
		for (String value : values) {
			if (StringUtils.hasText(value)) {
				return value;
			}
		}
		return null;
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}
}
