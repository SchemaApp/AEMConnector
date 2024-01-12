package com.schemaapp.core.models;

public class SchemaAppConfig {

    private String accountId;

    private String siteURL;

    private String deploymentMethod;

    private String endpoint;
    
    private String apiKey;

    public SchemaAppConfig(String accountId, String siteURL, String deploymentMethod, String endpoint, String apiKey) {
        this.accountId = accountId;
        this.siteURL = siteURL;
        this.deploymentMethod = deploymentMethod;
        this.endpoint = endpoint;
        this.apiKey = apiKey;
    }

    public SchemaAppConfig() {
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getSiteURL() {
        return siteURL;
    }

    public void setSiteURL(String siteURL) {
        this.siteURL = siteURL;
    }

    public String getDeploymentMethod() {
        return deploymentMethod;
    }

    public void setDeploymentMethod(String deploymentMethod) {
        this.deploymentMethod = deploymentMethod;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
    
    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
