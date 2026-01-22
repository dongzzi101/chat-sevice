package com.example.chatservice.config.dataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class DataSourceBuilder {
    
    public DataSource buildHikariDataSource(
            String url,
            String username,
            String password,
            String driverClassName,
            int maxPoolSize,
            int minIdle
    ) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        return new HikariDataSource(config);
    }
}
