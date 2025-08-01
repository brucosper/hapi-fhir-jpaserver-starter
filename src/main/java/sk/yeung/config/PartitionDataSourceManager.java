package sk.yeung.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PartitionDataSourceManager {
    private final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();
    private final Map<String, PartitionDataSourceConfig> partitionConfigs;
    private static final ThreadLocal<String> currentPartition = new ThreadLocal<>();

    public PartitionDataSourceManager(Map<String, PartitionDataSourceConfig> partitionConfigs) {
        this.partitionConfigs = partitionConfigs;
    }

    public DataSource getDataSource(String partition) {
        if (!StringUtils.hasText(partition)) {
            throw new IllegalArgumentException("Partition header is required");
        }
        
        if (!partitionConfigs.containsKey(partition)) {
            throw new IllegalArgumentException("Invalid partition: " + partition);
        }

        return dataSources.computeIfAbsent(partition, this::createDataSource);
    }

    private DataSource createDataSource(String partition) {
        PartitionDataSourceConfig config = partitionConfigs.get(partition);
        return config.createDataSource();
    }

    public void setCurrentPartition(String partition) {
        currentPartition.set(partition);
    }

    public void clearCurrentPartition() {
        currentPartition.remove();
    }

    public String getCurrentPartition() {
        return currentPartition.get();
    }

    public void shutdown() {
        dataSources.values().forEach(dataSource -> {
            if (dataSource instanceof HikariDataSource) {
                ((HikariDataSource) dataSource).close();
            }
        });
        dataSources.clear();
    }
}