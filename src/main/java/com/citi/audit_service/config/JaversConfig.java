package com.citi.audit_service.config;

import org.javers.core.Javers;
import org.javers.core.MappingStyle;
import org.javers.core.diff.ListCompareAlgorithm;
import org.javers.repository.sql.DialectName;
import org.javers.repository.sql.JaversSqlRepository;
import org.javers.repository.sql.SqlRepositoryBuilder;
import org.javers.spring.jpa.TransactionalJpaJaversBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Manual JaVers configuration to override auto-configuration.
 * This is needed because JaVers auto-configuration fails to detect HSQLDB dialect.
 * We use POSTGRES dialect which uses IDENTITY columns (not sequences) and is compatible with HSQLDB.
 */
@Configuration
public class JaversConfig {

    /**
     * Manually configure Javers bean with POSTGRES dialect
     * POSTGRES dialect uses IDENTITY columns which matches our HSQLDB schema
     * Schema management is disabled - tables will be created via schema.sql with CLOB instead of TEXT
     */
    @Bean
    @Primary
    public Javers javers(PlatformTransactionManager transactionManager, DataSource dataSource) {

        // Create SQL repository with POSTGRES dialect
        // POSTGRES dialect uses IDENTITY/SERIAL columns, not sequences
        // This is compatible with our HSQLDB IDENTITY column setup
        JaversSqlRepository sqlRepository = SqlRepositoryBuilder
                .sqlRepository()
                .withConnectionProvider(() -> {
                    try {
                        return dataSource.getConnection();
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to get database connection", e);
                    }
                })
                .withDialect(DialectName.POSTGRES)  // POSTGRES uses IDENTITY columns like our HSQLDB schema
                .withSchemaManagementEnabled(false)  // Disable - we create tables manually
                .build();

        // Build Javers with transactional support
        return TransactionalJpaJaversBuilder
                .javers()
                .withTxManager(transactionManager)
                .registerJaversRepository(sqlRepository)
                .withListCompareAlgorithm(ListCompareAlgorithm.LEVENSHTEIN_DISTANCE)
                .withMappingStyle(MappingStyle.FIELD)
                .withPrettyPrint(true)
                .build();
    }
}
