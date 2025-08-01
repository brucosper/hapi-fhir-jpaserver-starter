package sk.yeung.interceptors;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sk.yeung.config.PartitionDataSourceManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DataSourceInterceptorTest {

    @Mock
    private PartitionDataSourceManager dataSourceManager;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private DataSourceInterceptor interceptor;

    @Test
    void whenPartitionHeaderMissing_shouldThrowInvalidRequestException() {
        when(request.getHeader("X-Partition")).thenReturn(null);

        assertThrows(InvalidRequestException.class, () -> 
            interceptor.preHandle(request, response)
        );
    }

    @Test
    void whenPartitionHeaderEmpty_shouldThrowInvalidRequestException() {
        when(request.getHeader("X-Partition")).thenReturn("  ");

        assertThrows(InvalidRequestException.class, () -> 
            interceptor.preHandle(request, response)
        );
    }

    @Test
    void whenPartitionInvalid_shouldThrowResourceNotFoundException() {
        when(request.getHeader("X-Partition")).thenReturn("invalid-partition");
        doThrow(new IllegalArgumentException("Invalid partition"))
            .when(dataSourceManager).getDataSource("invalid-partition");

        assertThrows(ResourceNotFoundException.class, () -> 
            interceptor.preHandle(request, response)
        );
    }

    @Test
    void whenPartitionValid_shouldSetCurrentPartition() {
        String partition = "valid-partition";
        when(request.getHeader("X-Partition")).thenReturn(partition);

        interceptor.preHandle(request, response);

        verify(dataSourceManager).getDataSource(partition);
        verify(dataSourceManager).setCurrentPartition(partition);
    }

    @Test
    void postHandle_shouldClearCurrentPartition() {
        interceptor.postHandle(request, response);
        verify(dataSourceManager).clearCurrentPartition();
    }
}