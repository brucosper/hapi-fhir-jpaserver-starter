package sk.yeung.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sk.yeung.config.PartitionDataSourceConfig;
import sk.yeung.config.PartitionDataSourceManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hapi.fhir.partitions")
@Component
@Interceptor
public class DataSourceInterceptor {
    private static final String PARTITION_HEADER = "X-Partition";
    private final PartitionDataSourceManager dataSourceManager;
    private Map<String, String> datasources;

    @Autowired
    @Value("${partitions.datasources.partition1.url}")
    private String url;

    public DataSourceInterceptor() {
        // Create sample configuration for testing
        Map<String, PartitionDataSourceConfig> configs = new HashMap<>();
        PartitionDataSourceConfig config1 = new PartitionDataSourceConfig();
        config1.setUrl("jdbc:postgresql://localhost:5432/fhir_partition1");
        config1.setUsername("fhir_user");
        config1.setPassword("fhir_password");
        config1.setDriverClassName("org.postgresql.Driver");
        configs.put("partition1", config1);

        PartitionDataSourceConfig config2 = new PartitionDataSourceConfig();
        config2.setUrl("jdbc:postgresql://localhost:5433/fhir_partition2");
        config2.setUsername("fhir_user");
        config2.setPassword("fhir_password");
        config2.setDriverClassName("org.postgresql.Driver");
        configs.put("partition2", config2);

        this.dataSourceManager = new PartitionDataSourceManager(configs);
    }

    @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLER_SELECTED)
    public void preHandle(HttpServletRequest request, HttpServletResponse response) {
        try {
            String partition = request.getHeader(PARTITION_HEADER);
            System.err.println("datasources: " + datasources);
            System.err.println("url: " + url);
            
            if (partition == null || partition.trim().isEmpty()) {
                throw new InvalidRequestException("X-Partition header is required");
            }

            try {
                dataSourceManager.getDataSource(partition);
                dataSourceManager.setCurrentPartition(partition);
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
