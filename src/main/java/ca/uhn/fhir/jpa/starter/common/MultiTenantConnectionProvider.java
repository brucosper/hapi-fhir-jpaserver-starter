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
        // Return the first available datasource for schema validation
        Set<String> tenantIds = tenantDataSourceRegistry.getTenantIds();
        if (tenantIds.isEmpty()) {
            throw new IllegalStateException("No tenant datasources available");
        }
        return tenantDataSourceRegistry.getDataSource(tenantIds.iterator().next());
    }

    @Override
    protected DataSource selectDataSource(Object tenantIdentifier) {
        if (tenantIdentifier == null) {
            throw new IllegalArgumentException("Tenant identifier cannot be null");
        }

        String currentTenant = TenantContext.getTenantId();
        if (currentTenant == null || currentTenant.trim().isEmpty()) {
            throw new IllegalStateException("No tenant specified for current request");
        }

        if (!tenantDataSourceRegistry.hasTenant(currentTenant)) {
            throw new IllegalArgumentException("Invalid tenant identifier: " + currentTenant);
        }

        return tenantDataSourceRegistry.getDataSource(currentTenant);
    }
}