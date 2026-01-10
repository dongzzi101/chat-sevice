package com.example.chatservice.config.dataSource;

import com.example.chatservice.sharding.MessageShardKeySelector;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class MessageShardDataSourceConfig {

    private final Environment env;

    public MessageShardDataSourceConfig(Environment env) {
        this.env = env;
    }

    private DataSource createDataSource(String url, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(env.getProperty("app.datasource.message.driver-class-name", "com.mysql.cj.jdbc.Driver"));
        config.setMaximumPoolSize(getInt("app.datasource.message.max-pool-size", 20));
        config.setMinimumIdle(getInt("app.datasource.message.min-idle", 5));
        return new HikariDataSource(config);
    }

    @Bean(name = "messageDataSource")
    public DataSource messageDataSource() {
        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put(0, createDataSource(
                env.getProperty("app.datasource.message.shard0.url", "jdbc:mysql://localhost/messagedb1"),
                env.getProperty("app.datasource.message.username", "root"),
                env.getProperty("app.datasource.message.password", "mySql1313")
        ));
        dataSourceMap.put(1, createDataSource(
                env.getProperty("app.datasource.message.shard1.url", "jdbc:mysql://localhost/messagedb2"),
                env.getProperty("app.datasource.message.username", "root"),
                env.getProperty("app.datasource.message.password", "mySql1313")
        ));
        MessageShardKeySelector messageShardKeySelector = new MessageShardKeySelector();
        return new LazyConnectionDataSourceProxy(new ShardingDataSource(dataSourceMap, messageShardKeySelector));
    }

    private int getInt(String key, int defaultValue) {
        return Integer.parseInt(env.getProperty(key, String.valueOf(defaultValue)));
    }
}

