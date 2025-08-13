package ca.uhn.fhir.jpa.starter.common;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "hapi.fhir")
public class TenantProperties {
    
    private Map<String, TenantDatabaseConfig> tenants = new HashMap<>();

    public Map<String, TenantDatabaseConfig> getTenants() {
        return tenants;
    }

    public void setTenants(Map<String, TenantDatabaseConfig> tenants) {
        this.tenants = tenants;
    }

    public static class TenantDatabaseConfig {
        private String url;
        private String username;
        private String password;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}