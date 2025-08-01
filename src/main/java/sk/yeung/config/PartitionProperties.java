package sk.yeung.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "hapi.fhir.partitions")
public class PartitionProperties {
    private Map<String, PartitionDataSourceConfig> datasources = new HashMap<>();
    private boolean enabled = false;
    private boolean requirePartitionHeader = true;

    public Map<String, PartitionDataSourceConfig> getDatasources() {
        return datasources;
    }

    public void setDatasources(Map<String, PartitionDataSourceConfig> datasources) {
        this.datasources = datasources;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRequirePartitionHeader() {
        return requirePartitionHeader;
    }

    public void setRequirePartitionHeader(boolean requirePartitionHeader) {
        this.requirePartitionHeader = requirePartitionHeader;
    }
}