package com.schemaapp.core.models;

import org.apache.sling.api.resource.ValueMap;

public class SchemaAppConfig {

    private String accountId;

    private String siteURL;

    private String deploymentMethod;

    private String endpoint;
    
    private String apiKey;

    private boolean enableProxy;

    private String proxyHost;

    private String proxyPort;

    private String proxyUsername;

    private String proxyPassword;

    public SchemaAppConfig(String accountId, String endpoint, ValueMap configDetailMap) {
        this.accountId = accountId;
        this.siteURL = configDetailMap.get("siteURL", String.class);
        this.deploymentMethod = configDetailMap.get("deploymentMethod", String.class);
        this.endpoint = endpoint;
        this.apiKey = configDetailMap.get("apiKey", String.class);
        this.enableProxy = configDetailMap.get("enableProxy", false);
        this.proxyHost = configDetailMap.get("proxyHost", String.class);
        this.proxyPort = configDetailMap.get("proxyPort", String.class);
        this.proxyUsername = configDetailMap.get("proxyUsername", String.class);
        this.proxyPassword = configDetailMap.get("proxyPassword", String.class);
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

    public boolean isEnableProxy() {
        return enableProxy;
    }

    public void setEnableProxy(boolean enableProxy) {
        this.enableProxy = enableProxy;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public String getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(String proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getProxyUsername() {
        return proxyUsername;
    }

    public void setProxyUsername(String proxyUsername) {
        this.proxyUsername = proxyUsername;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }
}
