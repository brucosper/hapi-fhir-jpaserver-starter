# HAPI FHIR Partition Feature

This document describes how to use the partition feature to route requests to different databases based on the `X-Partition` header.

## Overview

The partition feature allows you to:
- Route requests to different databases based on the `X-Partition` header
- Configure multiple database connections for different partitions
- Handle concurrent requests to different partitions safely
- Manage connection pools efficiently for each partition

## Configuration

### 1. Enable Partitioning

Add the following to your `application.yaml`:

```yaml
hapi:
  fhir:
    partitions:
      enabled: true
      require-partition-header: true  # Set to false if you want to make the header optional
      datasources:
        partition1:
          url: jdbc:postgresql://localhost:5432/fhir_partition1
          username: fhir_user
          password: fhir_password
          driver-class-name: org.postgresql.Driver
          maximum-pool-size: 10
          minimum-idle: 5
          idle-timeout: 300000
        
        partition2:
          url: jdbc:postgresql://localhost:5432/fhir_partition2
          username: fhir_user
          password: fhir_password
          driver-class-name: org.postgresql.Driver
          maximum-pool-size: 10
          minimum-idle: 5
          idle-timeout: 300000
```

### 2. Database Configuration Properties

Each partition datasource supports the following properties:

- `url`: JDBC URL for the database
- `username`: Database username
- `password`: Database password
- `driver-class-name`: JDBC driver class name
- `maximum-pool-size`: Maximum number of connections in the pool
- `minimum-idle`: Minimum number of idle connections
- `idle-timeout`: Maximum time (in milliseconds) that a connection can remain idle

## Usage

### Making Requests

Include the `X-Partition` header in your HTTP requests to specify which partition to use:

```bash
# Example using curl
curl -H "X-Partition: partition1" http://localhost:8080/fhir/Patient

# Example using Postman
Headers:
X-Partition: partition1
```

### Error Handling

The system will return appropriate HTTP status codes:

- `400 Bad Request`: When the X-Partition header is missing (if required)
- `404 Not Found`: When an invalid or unknown partition is specified

### Thread Safety

The partition feature is designed to be thread-safe:
- Each partition has its own connection pool
- Partition context is maintained per-request using ThreadLocal storage
- Concurrent requests to different partitions are handled safely

## Implementation Details

The feature consists of several components:

1. `DataSourceInterceptor`: Handles the X-Partition header and manages request routing
2. `PartitionDataSourceManager`: Manages DataSource instances for each partition
3. `PartitionDataSourceConfig`: Holds configuration for each partition's database connection
4. `PartitionProperties`: Manages partition-specific properties and configuration

## Best Practices

1. Configure appropriate pool sizes based on your load requirements
2. Monitor connection pool metrics for each partition
3. Use meaningful partition names that reflect their purpose
4. Consider implementing a fallback strategy for when partitions are unavailable
5. Regularly monitor and maintain each partition's database

## Limitations

- Transactions cannot span multiple partitions
- Cross-partition queries are not supported
- All partitions must have the same schema structure

## Troubleshooting

Common issues and solutions:

1. Connection Pool Exhaustion
   - Increase `maximum-pool-size`
   - Decrease `idle-timeout`
   - Monitor connection usage patterns

2. Missing Partition Header
   - Check if the header is properly set in your client
   - Verify header name is exactly "X-Partition"
   - Set `require-partition-header: false` if header should be optional

3. Invalid Partition Names
   - Verify partition name matches configuration
   - Check for case sensitivity
   - Ensure partition is properly configured in application.yaml