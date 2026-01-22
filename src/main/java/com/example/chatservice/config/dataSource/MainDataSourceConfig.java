package com.example.chatservice.config.dataSource;

import com.example.chatservice.property.DataSourceProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@RequiredArgsConstructor
@Configuration
@EnableConfigurationProperties(DataSourceProperty.class)
public class MainDataSourceConfig {
    
    private final DataSourceProperty dataSourceProperty;
    private final DataSourceBuilder dataSourceBuilder;
    
    @Primary
    @Bean(name = "dataSource")
    public DataSource dataSource() {
        DataSourceProperty.MainDataSource config = dataSourceProperty.getMain();
        return dataSourceBuilder.buildHikariDataSource(
            config.getUrl(),
            config.getUsername(),
            config.getPassword(),
            config.getDriverClassName(),
            config.getMaxPoolSize(),
            config.getMinIdle()
        );
    }
}

