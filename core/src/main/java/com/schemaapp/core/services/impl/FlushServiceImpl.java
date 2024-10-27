package com.schemaapp.core.services.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

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
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import com.day.cq.wcm.api.Page;
import com.schemaapp.core.services.FlushService;
import com.schemaapp.core.util.ReplicationConstants;

@Component(service = FlushService.class, immediate = true)
public class FlushServiceImpl implements FlushService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FlushServiceImpl.class);

    @Reference
    private transient ResourceResolverFactory resolverFactory;
    
    @Reference
    private Replicator replicator;

    /**
     * Invalidates the JSON cache for a specified page URL by triggering replication.
     *
     * @param pageUrl the URL of the page to invalidate in the Dispatcher cache
     */
    @Override
    public void invalidatePageJson(String pageUrl) {
        LOGGER.debug("invalidatePageJson start for page URL: {}", pageUrl);
        try (ResourceResolver resourceResolver = getResourceResolver()) {
            Session session = resourceResolver.adaptTo(Session.class);
            if (session != null) {
                flushDispatcherForNode(session, pageUrl);
                Resource publishReplicationAgentResource = resourceResolver.getResource(ReplicationConstants.REPLICATION_AGENT_PATH_PUBLISH);
                if (publishReplicationAgentResource != null) {
                    getResourceNode(publishReplicationAgentResource, pageUrl);
                } else {
                    LOGGER.warn("Publish replication agent resource is null.");
                }
            } else {
                LOGGER.error("Session could not be retrieved from resource resolver.");
            }
        } catch (LoginException e) {
            LOGGER.error("LoginException occurred while accessing the resource resolver:", e);
        } catch (Exception e) {
            LOGGER.error("Unexpected exception during page JSON invalidation for URL {}: {}", pageUrl, e.getMessage(), e);
        }
    }
    
    /**
     * Initiates Dispatcher cache invalidation for a specific node by triggering replication.
     *
     * @param session the JCR session used to access the repository
     * @param nodePath the path of the node to invalidate in the cache
     */
    private void flushDispatcherForNode(Session session, String nodePath) {
        LOGGER.info("Starting Dispatcher cache invalidation for node: {}", nodePath);
        try {
            replicator.replicate(session, ReplicationActionType.ACTIVATE, nodePath);
            LOGGER.info("Cache invalidation successful for node: {}", nodePath);
        } catch (ReplicationException e) {
            LOGGER.error("ReplicationException during cache invalidation for node {}: {}", nodePath, e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error("Unexpected exception during cache invalidation for node {}: {}", nodePath, e.getMessage(), e);
        }
    }

    /**
     * Retrieves content resource nodes from the publish replication agent and invalidates
     * the Dispatcher cache for the specified page URL.
     *
     * @param publishReplicationAgentResource the publish replication agent resource
     * @param pageUrl the URL of the page to invalidate in the Dispatcher cache
     */
    private void getResourceNode(Resource publishReplicationAgentResource, String pageUrl) {
        Page publishReplicationAgentPage = publishReplicationAgentResource.adaptTo(Page.class);
        if (publishReplicationAgentPage != null) {
            Iterator<Page> publishAgentPageIterator = publishReplicationAgentPage.listChildren();
            while (publishAgentPageIterator.hasNext()) {
                Page childPage = publishAgentPageIterator.next();
                Resource contentResource = childPage.getContentResource();
                Node contentResourceNode = contentResource != null ? contentResource.adaptTo(Node.class) : null;
                if (contentResourceNode != null) {
                    try {
                        if (contentResourceNode.hasProperty("transportUri")) {
                            String dispatcherCacheUrl = contentResourceNode.getProperty("transportUri").getString();
                            LOGGER.debug("Dispatcher Cache URL: {}", dispatcherCacheUrl);
                            invalidateDispatcherCache(pageUrl, dispatcherCacheUrl);
                        }
                    } catch (RepositoryException e) {
                        LOGGER.error("RepositoryException in retrieving transportUri property:", e);
                    }
                }
            }
        } else {
            LOGGER.warn("Publish replication agent page could not be adapted.");
        }
    }

    /**
     * Sends an HTTP request to invalidate the Dispatcher cache for the specified page.
     *
     * @param pagePath the path of the page to invalidate
     * @param dispatcherCacheUrl the Dispatcher cache URL for invalidation
     */
    public void invalidateDispatcherCache(String pagePath, String dispatcherCacheUrl) {
        if (pagePath != null && dispatcherCacheUrl != null) {
            LOGGER.debug("Starting Dispatcher cache invalidation for page: {}", pagePath);
            HttpGet request = new HttpGet(dispatcherCacheUrl);
            try {
                HttpClient client = HttpClientBuilder.create()
                        .setDefaultRequestConfig(RequestConfig.custom().setConnectTimeout(10000).setSocketTimeout(10000).build())
                        .build();
                request.addHeader(ReplicationConstants.CQ_ACTION_HEADER, ReplicationConstants.ACTIVATE);
                request.addHeader(ReplicationConstants.CQ_HANDLE_HEADER, pagePath);
                request.addHeader(ReplicationConstants.CQ_PATH, pagePath);
                
                HttpResponse response = client.execute(request);
                int statusCode = response.getStatusLine().getStatusCode();
                LOGGER.debug("Dispatcher invalidation response code: {}", statusCode);
                if (statusCode != HttpStatus.SC_OK) {
                    LOGGER.warn("Dispatcher invalidation returned status code: {} for page: {}", statusCode, pagePath);
                } else {
                    LOGGER.info("Dispatcher cache successfully invalidated for page: {}", pagePath);
                }
            } catch (IOException e) {
                LOGGER.error("IOException during Dispatcher cache invalidation for page {}: {}", pagePath, e.getMessage(), e);
            } finally {
                request.releaseConnection();
            }
        } else {
            LOGGER.warn("Page path or Dispatcher cache URL is null. Cannot invalidate cache.");
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
