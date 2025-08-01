package sk.yeung.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sk.yeung.config.PartitionDataSourceManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Interceptor
public class DataSourceInterceptor {
    private static final String PARTITION_HEADER = "X-Partition";
    
    @Autowired
    private PartitionDataSourceManager dataSourceManager;

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
    public void preHandle(HttpServletRequest request, HttpServletResponse response) {
        try {
            String partition = request.getHeader(PARTITION_HEADER);
            
            if (partition == null || partition.trim().isEmpty()) {
                throw new InvalidRequestException("X-Partition header is required");
            }

            try {
                dataSourceManager.getDataSource(partition);
                dataSourceManager.setCurrentPartition(partition);
								System.out.println("Set partition to: " + partition);
            } catch (IllegalArgumentException e) {
                throw new ResourceNotFoundException("Invalid partition: " + partition);
            }
            
        } catch (Exception e) {
            throw e; // Let HAPI FHIR handle the exception mapping
        }
    }

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
    public void postHandle(HttpServletRequest request, HttpServletResponse response) {
        dataSourceManager.clearCurrentPartition();
    }
}
