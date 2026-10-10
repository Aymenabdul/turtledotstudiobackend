package com.turtledot.studio.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
@Profile("mysql")
public class CloudDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(CloudDataSourceConfig.class);

    @Bean
    @Primary
    public DataSource dataSource(Environment env) {
        String rawUrl = getFirstNonBlank(env,
                "DB_URL",
                "DATABASE_URL",
                "SPRING_DATASOURCE_URL",
                "MYSQL_URL",
                "MYSQLURL"
        );

        String username = getFirstNonBlank(env,
                "DB_USERNAME",
                "DATABASE_USERNAME",
                "SPRING_DATASOURCE_USERNAME",
                "MYSQLUSER",
                "MYSQL_USER"
        );

        String password = getFirstNonBlank(env,
                "DB_PASSWORD",
                "DATABASE_PASSWORD",
                "SPRING_DATASOURCE_PASSWORD",
                "MYSQLPASSWORD",
                "MYSQL_PASSWORD"
        );

        String host = getFirstNonBlank(env, "DB_HOST", "MYSQLHOST", "MYSQL_HOST");
        String port = getFirstNonBlank(env, "DB_PORT", "MYSQLPORT", "MYSQL_PORT");
        String database = getFirstNonBlank(env, "DB_NAME", "MYSQLDATABASE", "MYSQL_DATABASE");

        String jdbcUrl;

        if (rawUrl != null && !rawUrl.isBlank()) {
            rawUrl = rawUrl.trim();
            // Handle standard URI formats: mysql://user:pass@host:port/db
            if (rawUrl.startsWith("mysql://") || rawUrl.startsWith("mysqls://")) {
                try {
                    URI uri = URI.create(rawUrl);
                    String uriHost = uri.getHost();
                    int uriPort = uri.getPort() > 0 ? uri.getPort() : 3306;
                    String uriPath = uri.getPath();
                    if (uriPath != null && uriPath.startsWith("/")) {
                        uriPath = uriPath.substring(1);
                    }
                    if (uri.getUserInfo() != null) {
                        String[] userInfo = uri.getUserInfo().split(":", 2);
                        if (username == null || username.isBlank()) {
                            username = userInfo[0];
                        }
                        if (userInfo.length > 1 && (password == null || password.isBlank())) {
                            password = userInfo[1];
                        }
                    }

                    jdbcUrl = String.format(
                            "jdbc:mysql://%s:%d/%s?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                            uriHost, uriPort, uriPath != null ? uriPath : ""
                    );
                    log.info("[DataSource] Successfully parsed mysql:// URI -> Host: {}:{}, Database: {}, Username: {}",
                            uriHost, uriPort, uriPath, username);
                } catch (Exception e) {
                    log.warn("[DataSource] Failed to parse URI, using raw URL with jdbc: prefix: {}", e.getMessage());
                    jdbcUrl = "jdbc:" + rawUrl;
                }
            } else if (rawUrl.startsWith("jdbc:mysql://")) {
                // If it's already jdbc:mysql://, check if user:pass@ is present in the host section
                String withoutPrefix = rawUrl.substring("jdbc:mysql://".length());
                if (withoutPrefix.contains("@")) {
                    int atIndex = withoutPrefix.indexOf("@");
                    String userPass = withoutPrefix.substring(0, atIndex);
                    String rest = withoutPrefix.substring(atIndex + 1);
                    String[] parts = userPass.split(":", 2);
                    if (username == null || username.isBlank()) {
                        username = parts[0];
                    }
                    if (parts.length > 1 && (password == null || password.isBlank())) {
                        password = parts[1];
                    }
                    jdbcUrl = "jdbc:mysql://" + rest;
                    log.info("[DataSource] Extracted credentials from jdbc:mysql:// -> Host: {}, Username: {}", rest, username);
                } else {
                    jdbcUrl = rawUrl;
                }

                // Ensure SSL and public key retrieval parameters exist for cloud stability
                if (!jdbcUrl.contains("allowPublicKeyRetrieval")) {
                    jdbcUrl += (jdbcUrl.contains("?") ? "&" : "?") + "allowPublicKeyRetrieval=true";
                }
                if (!jdbcUrl.contains("serverTimezone")) {
                    jdbcUrl += "&serverTimezone=UTC";
                }
                log.info("[DataSource] Using configured JDBC URL: {}", maskUrl(jdbcUrl));
            } else {
                jdbcUrl = rawUrl.startsWith("jdbc:") ? rawUrl : "jdbc:mysql://" + rawUrl;
            }
        } else if (host != null && !host.isBlank()) {
            int portNum = (port != null && !port.isBlank()) ? Integer.parseInt(port) : 3306;
            String dbName = (database != null && !database.isBlank()) ? database : "studio_crm";
            jdbcUrl = String.format(
                    "jdbc:mysql://%s:%d/%s?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                    host, portNum, dbName
            );
            log.info("[DataSource] Constructed from HOST/PORT -> Host: {}:{}, Database: {}, Username: {}",
                    host, portNum, dbName, username);
        } else {
            // Default local fallback
            jdbcUrl = "jdbc:mysql://localhost:3306/studio_crm?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            if (username == null) username = "root";
            if (password == null) password = "Parkour@123";
            log.warn("[DataSource] WARNING: No cloud database credentials found in environment variables! Falling back to localhost:3306");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        if (username != null && !username.isBlank()) {
            config.setUsername(username);
        }
        if (password != null && !password.isBlank()) {
            config.setPassword(password);
        }

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        return new HikariDataSource(config);
    }

    private String getFirstNonBlank(Environment env, String... keys) {
        for (String key : keys) {
            String val = env.getProperty(key);
            if (val != null && !val.trim().isEmpty()) {
                return val.trim();
            }
        }
        return null;
    }

    private String maskUrl(String url) {
        return url.replaceAll("password=[^&;]*", "password=******");
    }
}
