# Multi-Tenant Database Configuration

This HAPI FHIR server supports multi-tenancy with separate physical databases per tenant. Each tenant's data is completely isolated in its own database.

## Configuration

### 1. Enable Multi-Tenancy

In your `application.yaml`, enable request-based tenant partitioning:

```yaml
hapi:
  fhir:
    partitioning:
      partitioning_include_in_search_hashes: false
      request_tenant_partitioning_mode: true
```

### 2. Configure Tenant Databases

Define tenant-specific database configurations:

```yaml
hapi:
  fhir:
    tenants:
      tenant1:
        url: jdbc:postgresql://db1:5432/fhir_tenant1
        username: fhir_user1
        password: secret1
      tenant2:
        url: jdbc:postgresql://db2:5432/fhir_tenant2
        username: fhir_user2
        password: secret2
```

Each tenant requires:
- A unique identifier (e.g., tenant1, tenant2)
- Database connection details (URL, username, password)

## Usage

### Making Requests

Include the `X-Partition-Name` header in your requests to specify the tenant:

```http
GET http://localhost:8080/fhir/Patient
X-Partition-Name: tenant1
```

### Error Handling

- Missing `X-Partition-Name` header: 400 Bad Request with message "Missing required header: X-Partition-Name"
- Invalid tenant ID: 400 Bad Request with message "Invalid tenant ID: {tenantId}"
- Database connection issues: 500 Internal Server Error

## Implementation Details

The multi-tenant setup uses:
- Hibernate's multi-tenancy support with DATABASE strategy
- Per-tenant connection pools using HikariCP
- ThreadLocal context for tenant tracking
- Request interceptor for tenant header validation

## Configuration Classes

- `TenantProperties` - Loads tenant configuration from application.yaml
- `TenantDataSourceRegistry` - Manages per-tenant DataSources
- `MultiTenantConnectionProvider` - Provides database connections based on tenant context
- `TenantIdentificationInterceptor` - Validates and extracts tenant from request headers
- `TenantContext` - ThreadLocal storage for current tenant ID

## Database Setup

1. Create separate databases for each tenant
2. Apply the HAPI FHIR schema to each database
3. Configure connection details in application.yaml
4. Ensure proper database user permissions

## Security Considerations

- Each tenant's data is physically isolated
- Database credentials are tenant-specific
- No cross-tenant data access possible
- Consider moving database configurations to environment variables in production

## Example Client Code

Java example using HAPI FHIR client:

```java
IGenericClient client = ctx.newRestfulGenericClient("http://localhost:8080/fhir");

// Add tenant header
AdditionalRequestHeadersInterceptor headerInterceptor = new AdditionalRequestHeadersInterceptor();
headerInterceptor.addHeaderValue("X-Partition-Name", "tenant1");
client.registerInterceptor(headerInterceptor);

// Make requests - they'll automatically use the specified tenant
Patient patient = client.read().resource(Patient.class).withId("123").execute();
```

## Troubleshooting

### "No tenant databases configured" Error

This error occurs when:
1. The `TenantProperties` class is not being scanned by Spring
2. The configuration properties are not properly bound
3. The application.yaml structure is incorrect

**Solution**: Ensure the `TenantProperties` class has the correct annotations:
- `@Component` or `@Configuration`
- `@ConfigurationProperties(prefix = "hapi.fhir.tenants")`
- `@EnableConfigurationProperties` in a configuration class

### Configuration Not Loading

If tenant configuration is not being loaded:
1. Verify the application.yaml structure matches the expected format
2. Check that Spring Boot configuration processing is enabled
3. Ensure the configuration class is in the component scan path