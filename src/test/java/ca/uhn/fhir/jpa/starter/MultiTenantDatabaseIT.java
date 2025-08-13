package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.client.interceptor.AdditionalRequestHeadersInterceptor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {Application.class}, properties = {
    "spring.datasource.url=jdbc:h2:mem:dbr4-mt-test",
    "hapi.fhir.fhir_version=r4",
    "hapi.fhir.subscription.websocket_enabled=false",
    "hapi.fhir.cr_enabled=false",
    "hapi.fhir.partitioning.partitioning_include_in_search_hashes=false",
    "hapi.fhir.partitioning.request_tenant_partitioning_mode=true",
    "hapi.fhir.tenants.tenant1.url=jdbc:h2:mem:tenant1",
    "hapi.fhir.tenants.tenant1.username=sa",
    "hapi.fhir.tenants.tenant1.password=",
    "hapi.fhir.tenants.tenant2.url=jdbc:h2:mem:tenant2",
    "hapi.fhir.tenants.tenant2.username=sa",
    "hapi.fhir.tenants.tenant2.password="
})
class MultiTenantDatabaseIT {

    private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(MultiTenantDatabaseIT.class);
    private IGenericClient ourClient;
    private FhirContext ourCtx;
    private LoggingInterceptor loggingInterceptor;

    @LocalServerPort
    private int port;

    @BeforeEach
    void beforeEach() {
        ourCtx = FhirContext.forR4();
        ourCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        ourCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
        
        String ourServerBase = "http://localhost:" + port + "/fhir/";
        ourClient = ourCtx.newRestfulGenericClient(ourServerBase);
        
        loggingInterceptor = new LoggingInterceptor(true);
        ourClient.registerInterceptor(loggingInterceptor);
    }

    @Test
    void testCreateAndReadInTenant1() {
        // Create patient in tenant1
        setTenantHeader("tenant1");
        
        Patient pt = new Patient();
        pt.addName().setFamily("Family Tenant1");
        ourClient.create().resource(pt).execute();

        // Search in tenant1
        Bundle searchResult = ourClient.search()
            .forResource(Patient.class)
            .returnBundle(Bundle.class)
            .execute();
        
        assertEquals(1, searchResult.getEntry().size());
        Patient pt2 = (Patient) searchResult.getEntry().get(0).getResource();
        assertEquals("Family Tenant1", pt2.getName().get(0).getFamily());

        // Search in tenant2 should return empty
        setTenantHeader("tenant2");
        Bundle searchResult2 = ourClient.search()
            .forResource(Patient.class)
            .returnBundle(Bundle.class)
            .execute();
        
        assertEquals(0, searchResult2.getEntry().size());
    }

    @Test
    void testMissingTenantHeader() {
        // Clear all interceptors except logging
        List.copyOf(ourClient.getInterceptorService().getAllRegisteredInterceptors())
            .forEach(i -> {
                if (!(i instanceof LoggingInterceptor)) {
                    ourClient.getInterceptorService().unregisterInterceptor(i);
                }
            });

        Patient pt = new Patient();
        pt.addName().setFamily("Test");

        // Should throw exception for missing header
        assertThrows(Exception.class, () -> {
            ourClient.create().resource(pt).execute();
        });
    }

    private void setTenantHeader(String tenantId) {
        // Remove any existing tenant header interceptors
        List.copyOf(ourClient.getInterceptorService().getAllRegisteredInterceptors())
            .forEach(i -> {
                if (i instanceof AdditionalRequestHeadersInterceptor) {
                    ourClient.getInterceptorService().unregisterInterceptor(i);
                }
            });
        
        // Add new header interceptor
        AdditionalRequestHeadersInterceptor headerInterceptor = new AdditionalRequestHeadersInterceptor();
        headerInterceptor.addHeaderValue("X-Partition-Name", tenantId);
        ourClient.registerInterceptor(headerInterceptor);
    }
}