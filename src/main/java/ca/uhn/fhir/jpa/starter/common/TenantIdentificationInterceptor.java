package ca.uhn.fhir.jpa.starter.common;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Interceptor
public class TenantIdentificationInterceptor {

    private static final String X_PARTITION_NAME = "X-Partition-Name";
    private static final String X_TENANT_NAME = "X-Tenant-Name";

    @Autowired
    private TenantProperties tenantProperties;

    @Hook(Pointcut.STORAGE_PARTITION_IDENTIFY_ANY)
    public RequestPartitionId preProcess(ServletRequestDetails requestDetails) {
        String tenantId = requestDetails.getHeader(X_TENANT_NAME);
        
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new InvalidRequestException("Missing required header: " + X_PARTITION_NAME);
        }

        if (!tenantProperties.getTenants().containsKey(tenantId)) {
            throw new InvalidRequestException("Invalid tenant ID: " + tenantId);
        }
        TenantContext.setTenantId(tenantId);
        String partitionName = requestDetails.getHeader(X_PARTITION_NAME);

        return RequestPartitionId.fromPartitionName(partitionName);
    }
}