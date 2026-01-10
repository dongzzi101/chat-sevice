package com.example.chatservice.config.dataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

@Configuration
public class MainDataSourceConfig {

    private final Environment env;

    public MainDataSourceConfig(Environment env) {
        this.env = env;
    }

    @Primary
    @Bean(name = "dataSource")
    public DataSource dataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(env.getProperty("app.datasource.main.url", "jdbc:mysql://localhost/chat_service"));
        hikariConfig.setUsername(env.getProperty("app.datasource.main.username", "root"));
        hikariConfig.setPassword(env.getProperty("app.datasource.main.password", "mySql1313"));
        hikariConfig.setDriverClassName(env.getProperty("app.datasource.main.driver-class-name", "com.mysql.cj.jdbc.Driver"));
        hikariConfig.setMaximumPoolSize(getInt("app.datasource.main.max-pool-size", 20));
        hikariConfig.setMinimumIdle(getInt("app.datasource.main.min-idle", 5));
        return new HikariDataSource(hikariConfig);
    }

    private int getInt(String key, int defaultValue) {
        return Integer.parseInt(env.getProperty(key, String.valueOf(defaultValue)));
    }
}

