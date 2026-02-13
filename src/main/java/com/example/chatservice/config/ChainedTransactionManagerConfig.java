package com.example.chatservice.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Main + Message 트랜잭션 매니저를 하나로 묶어서 사용.
 */
@Configuration
public class ChainedTransactionManagerConfig {

    @Bean(name = "chainedTransactionManager")
    public PlatformTransactionManager chainedTransactionManager(
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            @Qualifier("messageTransactionManager") PlatformTransactionManager messageTransactionManager
    ) {
        return new ChainedTransactionManager(transactionManager, messageTransactionManager);
    }

    @Bean(name = "chainedTransactionTemplate")
    public TransactionTemplate chainedTransactionTemplate(
            @Qualifier("chainedTransactionManager") PlatformTransactionManager chainedTransactionManager
    ) {
        return new TransactionTemplate(chainedTransactionManager);
    }
}
