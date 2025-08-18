package ca.uhn.fhir.jpa.starter.common;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Interceptor
public class TenantIdentificationInterceptor {

    private static final String X_PARTITION_NAME = "X-Partition-Name";
    private static final String X_TENANT = "X-Tenant-Name";

    @Autowired
    private TenantProperties tenantProperties;

    // @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_PROCESSED)
    @Hook(Pointcut.STORAGE_PARTITION_IDENTIFY_ANY)
    public RequestPartitionId preProcess(ServletRequestDetails requestDetails) {
        String tenantId = requestDetails.getHeader(X_TENANT);
        
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new InvalidRequestException("Missing required header: " + X_PARTITION_NAME);
        }

        if (!tenantProperties.getTenants().containsKey(tenantId)) {
            throw new InvalidRequestException("Invalid tenant ID: " + tenantId);
        }
        TenantContext.setTenantId(tenantId);
        String partitionName = requestDetails.getHeader("X-Partition-Name");

        return RequestPartitionId.fromPartitionName(partitionName);
    }

//     @Hook(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
//     public void postProcess(RequestDetails requestDetails) {
//         TenantContext.clear();
//     }
}