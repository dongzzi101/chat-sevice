package com.example.chatservice.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateSettings;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.example.chatservice.message.repository",
        entityManagerFactoryRef = "messageEntityManagerFactory",
        transactionManagerRef = "messageTransactionManager"
)
public class MessageJpaConfig {

    private final JpaProperties jpaProperties;
    private final HibernateProperties hibernateProperties;

    public MessageJpaConfig(JpaProperties jpaProperties, HibernateProperties hibernateProperties) {
        this.jpaProperties = jpaProperties;
        this.hibernateProperties = hibernateProperties;
    }

    @Bean(name = "messageEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean messageEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("messageDataSource") DataSource messageDataSource
    ) {
        Map<String, Object> properties = hibernateProperties.determineHibernateProperties(
                jpaProperties.getProperties(), new HibernateSettings());

        return builder
                .dataSource(messageDataSource)
                // Include related entities referenced by Message (ChatRoom, User, etc.)
                .packages(
                        "com.example.chatservice.message",
                        "com.example.chatservice.chat",
                        "com.example.chatservice.user"
                )
                .properties(properties)
                .persistenceUnit("message")
                .build();
    }

    @Bean(name = "messageTransactionManager")
    public PlatformTransactionManager messageTransactionManager(
            @Qualifier("messageEntityManagerFactory") EntityManagerFactory messageEntityManagerFactory
    ) {
        return new JpaTransactionManager(messageEntityManagerFactory);
    }
}

