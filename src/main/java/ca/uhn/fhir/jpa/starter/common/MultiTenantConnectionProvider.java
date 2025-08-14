package ca.uhn.fhir.jpa.starter.common;

import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Set;

@Component
public class MultiTenantConnectionProvider extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl {

    private static final long serialVersionUID = 1L;

    @Autowired
    private TenantDataSourceRegistry tenantDataSourceRegistry;

    @Override
    protected DataSource selectAnyDataSource() {
        // Return canada datasource for schema validation and system operations
        Set<String> tenantIds = tenantDataSourceRegistry.getTenantIds();
        if (tenantIds.isEmpty()) {
            throw new IllegalStateException("No tenant datasources available. Please check application.yaml configuration.");
        }
        
        // Prefer canada for system operations, fallback to first available
        if (tenantDataSourceRegistry.hasTenant("canada")) {
            return tenantDataSourceRegistry.getDataSource("canada");
        }
        return tenantDataSourceRegistry.getDataSource(tenantIds.iterator().next());
    }

    @Override
    protected DataSource selectDataSource(Object tenantIdentifier) {
        if (tenantIdentifier == null) {
            // During startup or system operations, use canada as default
            tenantIdentifier = "canada";
        }

        String tenantId = tenantIdentifier.toString();
        
        // Validate tenant exists
        if (!tenantDataSourceRegistry.hasTenant(tenantId)) {
            throw new IllegalArgumentException("Invalid tenant identifier: " + tenantId + ". Available tenants: " + tenantDataSourceRegistry.getTenantIds());
        }

        return tenantDataSourceRegistry.getDataSource(tenantId);
    }
}