package com.schemaapp.core.services.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.distribution.*;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.Page;
import com.schemaapp.core.services.FlushService;
import com.schemaapp.core.util.ReplicationConstants;

@Component(service = FlushService.class, immediate = true)
public class FlushServiceImpl implements FlushService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FlushServiceImpl.class);

    @Reference
    private transient ResourceResolverFactory resolverFactory;

    @Reference
    private Distributor distributor;

    /**
     * Invalidates the JSON cache for a specified page URL by triggering distribution.
     *
     * @param pageUrl the URL of the page to invalidate in the Dispatcher cache
     */
    @Override
    public void invalidatePageJson(String pageUrl) {
        LOGGER.debug("invalidatePageJson start for page URL: {}", pageUrl);
        try (ResourceResolver resourceResolver = getResourceResolver()) {
                flushDispatcherForNode(resourceResolver, pageUrl);
        } catch (LoginException e) {
            LOGGER.error("LoginException occurred while accessing the resource resolver:", e);
        } catch (Exception e) {
            LOGGER.error("Unexpected exception during page JSON invalidation for URL {}: {}", pageUrl, e.getMessage(), e);
        }
    }
    
    /**
     * Initiates Dispatcher cache invalidation for a specific node by triggering distribution.
     *
     * @param resourceResolver the ResourceResolver used to access the repository
     * @param nodePath the path of the node to invalidate in the cache
     */
    private void flushDispatcherForNode(ResourceResolver resourceResolver, String nodePath) {
        LOGGER.info("Starting Dispatcher cache invalidation for node: {}", nodePath);
        try {
            DistributionRequest distributionRequest = new SimpleDistributionRequest(
                    DistributionRequestType.INVALIDATE, //This may need to use DistributionRequestType.ADD instead to actually publish the node
                    false,
                    nodePath
            );

            DistributionResponse response = distributor.distribute(
                    ReplicationConstants.DISTRIBUTION_AGENT_NAME_PUBLISH,
                    resourceResolver,
                    distributionRequest);

            if (response.isSuccessful()) {
                LOGGER.info("Cache invalidation successful for node: {}", nodePath);
            } else {
                LOGGER.error("Cache invalidation failure for node: {}", nodePath);
            }
        } catch (Exception e) {
            LOGGER.error("Unexpected exception during cache invalidation for node {}: {}", nodePath, e.getMessage(), e);
        }
    }

    /**
     * Provides a service resource resolver with schema-app-service privileges.
     *
     * @return a ResourceResolver object
     * @throws LoginException if login fails
     */
    private ResourceResolver getResourceResolver() throws LoginException {
        Map<String, Object> param = new HashMap<>();
        param.put(ResourceResolverFactory.SUBSERVICE, "schema-app-service");
        return resolverFactory.getServiceResourceResolver(param);
    }
}
