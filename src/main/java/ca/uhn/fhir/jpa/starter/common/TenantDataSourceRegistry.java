package ca.uhn.fhir.jpa.starter.common;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class TenantDataSourceRegistry {

    @Autowired
    private TenantProperties tenantProperties;

    private final Map<String, DataSource> tenantDataSources = new HashMap<>();

    @PostConstruct
    public void initialize() {
        tenantProperties.getTenants().forEach((tenantId, config) -> {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(config.getUrl());
            hikariConfig.setUsername(config.getUsername());
            hikariConfig.setPassword(config.getPassword());
            hikariConfig.setPoolName("hikari-" + tenantId);
            
            // Basic connection pool settings
            hikariConfig.setMaximumPoolSize(10);
            hikariConfig.setMinimumIdle(2);
            
            System.err.println(config.getUrl());
            DataSource dataSource = new HikariDataSource(hikariConfig);
            System.err.println(config.getUrl());
            tenantDataSources.put(tenantId, dataSource);
        });

        if (tenantDataSources.isEmpty()) {
            throw new IllegalStateException("No tenant databases configured. Please check application.yaml");
        }
    }

    public DataSource getDataSource(String tenantId) {
        DataSource dataSource = tenantDataSources.get(tenantId);
        if (dataSource == null) {
            throw new IllegalArgumentException("No datasource configured for tenant: " + tenantId);
        }
        return dataSource;
    }

    public Set<String> getTenantIds() {
        return tenantDataSources.keySet();
    }

    public boolean hasTenant(String tenantId) {
        return tenantDataSources.containsKey(tenantId);
    }
}