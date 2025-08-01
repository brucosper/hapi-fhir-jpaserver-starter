package sk.yeung.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartitionDataSourceManagerTest {

    private PartitionDataSourceManager manager;
    
    @Mock
    private PartitionDataSourceConfig config1;
    
    @Mock
    private PartitionDataSourceConfig config2;
    
    @Mock
    private HikariDataSource mockDataSource1;
    
    @Mock
    private HikariDataSource mockDataSource2;

    @BeforeEach
    void setUp() {
        Map<String, PartitionDataSourceConfig> configs = new HashMap<>();
        configs.put("partition1", config1);
        
        lenient().when(config1.createDataSource()).thenReturn(mockDataSource1);
        
        manager = new PartitionDataSourceManager(configs);
    }

    @Test
    void getDataSource_ShouldCreateNewDataSourceOnFirstAccess() {
        DataSource ds1 = manager.getDataSource("partition1");
        
        assertNotNull(ds1);
        verify(config1, times(1)).createDataSource();
    }

    @Test
    void getDataSource_ShouldReuseExistingDataSource() {
        DataSource ds1 = manager.getDataSource("partition1");
        DataSource ds2 = manager.getDataSource("partition1");
        
        assertSame(ds1, ds2);
        verify(config1, times(1)).createDataSource();
    }

    @Test
    void getDataSource_ShouldThrowException_WhenPartitionNotFound() {
        assertThrows(IllegalArgumentException.class, () ->
            manager.getDataSource("non-existent-partition")
        );
    }

    @Test
    void currentPartition_ShouldBeThreadLocal() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(() -> {
            try {
                manager.setCurrentPartition("partition1");
                assertEquals("partition1", manager.getCurrentPartition());
            } finally {
                latch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                manager.setCurrentPartition("partition2");
                assertEquals("partition2", manager.getCurrentPartition());
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();
    }

    @Test
    void shutdown_ShouldCloseAllDataSources() {
        manager.getDataSource("partition1");
        
        manager.shutdown();
        
        verify(mockDataSource1).close();
    }
}