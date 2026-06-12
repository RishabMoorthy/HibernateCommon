package com.stubserver.backend.database.config;

import jakarta.persistence.EntityManagerFactory;
import org.common.db.repository.*;
import org.hibernate.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class RepositoryConfig {

    private final SessionFactory sessionFactory;

    public RepositoryConfig(EntityManagerFactory emf) {
        this.sessionFactory = emf.unwrap(SessionFactory.class);
    }

    @Bean
    public VsDetailsRepository vsDetailsRepository() {
        return new VsDetailsRepository(sessionFactory);
    }

    @Bean
    public VsCatalogRepository vsCatalogRepository() {
        return new VsCatalogRepository(sessionFactory);
    }

    @Bean
    public MasterCatalogRepository masterCatalogRepository() {
        return new MasterCatalogRepository(sessionFactory);
    }

    @Bean
    public ExecutionModeRepository executionModeRepository() {
        return new ExecutionModeRepository(sessionFactory);
    }

    @Bean
    public LiveUrlRepository liveUrlRepository() {
        return new LiveUrlRepository(sessionFactory);
    }

    @Bean
    public UserRepository userRepository() {
        return new UserRepository(sessionFactory);
    }

    @Bean
    public RefreshTokenRepository refreshTokenRepository() {
        return new RefreshTokenRepository(sessionFactory);
    }

    @Bean
    public AuditLogRepository auditLogRepository() {
        return new AuditLogRepository(sessionFactory);
    }

    @Bean
    public AssignedServiceRepository assignedServiceRepository() {
        return new AssignedServiceRepository(sessionFactory);
    }

    @Bean
    public PortRangeRepository portRangeRepository() {
        return new PortRangeRepository(sessionFactory);
    }

    @Bean
    public MetricsRepository metricsRepository(DataSource dataSource) {
        return new MetricsRepository(dataSource);
    }
}
