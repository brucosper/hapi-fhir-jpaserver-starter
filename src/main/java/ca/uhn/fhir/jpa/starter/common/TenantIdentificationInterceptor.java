package ca.uhn.fhir.jpa.starter.common;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Interceptor
public class TenantIdentificationInterceptor {

    private static final String X_PARTITION_NAME = "X-Partition-Name";

    @Autowired
    private TenantProperties tenantProperties;

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_PROCESSED)
    public void preProcess(RequestDetails requestDetails) {
        String tenantId = requestDetails.getHeader(X_PARTITION_NAME);
        
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new InvalidRequestException("Missing required header: " + X_PARTITION_NAME);
        }

        if (!tenantProperties.getTenants().containsKey(tenantId)) {
            throw new InvalidRequestException("Invalid tenant ID: " + tenantId);
        }

        TenantContext.setTenantId(tenantId);
    }

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
    public void postProcess(RequestDetails requestDetails) {
        TenantContext.clear();
    }
}