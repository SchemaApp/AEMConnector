package com.schemaapp.core.services.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageFilter;
import com.schemaapp.core.models.SchemaAppConfig;
import com.schemaapp.core.services.BulkDataLoaderAPIService;
import com.schemaapp.core.services.CDNDataProcessor;
import com.schemaapp.core.util.ConfigurationUtil;
import com.schemaapp.core.util.Constants;

@Component(service = CDNDataProcessor.class, immediate = true)
public class CDNDataProcessorImpl implements CDNDataProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(CDNDataProcessorImpl.class);
    private static final String CQ_CLOUDSERVICECONFIGS = "cq:cloudserviceconfigs";

    @Reference
    private ConfigurationAdmin configurationAdmin;

    @Reference
    transient ResourceResolverFactory resolverFactory;
    
    @Reference
    private BulkDataLoaderAPIService bulkDataLoaderAPIService;

    /**
     * Processes CDN data and updates schema information for pages.
     */
    public void processCDNDataAndUpdateSchema() {
        ResourceResolver resolver = getResourceResolver();
        try {
            if (resolver != null) {
                List<Page> rootPages = fetchSiteRootPages(resolver);
                for (Page page : rootPages) {
                    updateSchemaAppCDNData(resolver, page);
                }
            } else {
                LOG.debug("Error occurs while getting ResourceResolver");
            }
        } catch (Exception e) {
            LOG.error("Error occurs while processing CDN data and updating schema: {}", e.getMessage(), e);
        } finally {
            if (resolver != null && resolver.isLive()) {
                LOG.info("ResourceResolver is live after processing CDN data");
                try {
                    resolver.commit();
                } catch (PersistenceException e) {
                    LOG.error("Error while committing the resolver: {}", e.getMessage(), e);
                } finally {
                    resolver.close();
                }
            }
        }
    }

    /**
     * Retrieves the list of root pages under /content with specific cloud service configurations.
     *
     * @param resolver The resource resolver.
     * @return A list of root pages.
     */
    private List<Page> fetchSiteRootPages(ResourceResolver resolver) {
        List<Page> rootPages = new ArrayList<>();
        Resource contentResource = resolver.getResource("/content");
        if (contentResource == null)
            return rootPages;
        Iterator<Resource> childResourceIterator = contentResource.listChildren();
        while (childResourceIterator.hasNext()) {
            final Resource child = childResourceIterator.next();
            if (child != null && "cq:Page".equals(child.getResourceType())) {
                Resource jcrContentResource = child.getChild("jcr:content");
                if (jcrContentResource != null && jcrContentResource.getValueMap().get(CQ_CLOUDSERVICECONFIGS) != null) {
                    String[] cloudServiceConfigs = (String[]) jcrContentResource.getValueMap().get(CQ_CLOUDSERVICECONFIGS);
                    boolean contains = Arrays.stream(cloudServiceConfigs)
                            .anyMatch(s -> s.startsWith("/etc/cloudservices/schemaapp"));

                    if (contains) {
                        Page page = child.adaptTo(Page.class);
                        rootPages.add(page);
                    }
                }
            }
        }
        return rootPages;
    }

    /**
     * Updates schema information for a page based on CDN data.
     *
     * @param resolver The resource resolver.
     * @param page     The page to update.
     */
    private void updateSchemaAppCDNData(ResourceResolver resolver, Page page) {
        try {
            ValueMap configDetailMap = fetchConfigNodeValueMap(resolver, page);
            if (configDetailMap != null) {
                String accountId = extractAccountId(configDetailMap);
                String siteURL = configDetailMap.get("siteURL", String.class);
                String deploymentMethod = configDetailMap.get("deploymentMethod", String.class);
                String apiKey = configDetailMap.get("apiKey", String.class);
                
                Iterator<Page> childPages = page.listChildren(new PageFilter(), true);
                String endpoint = ConfigurationUtil.getConfiguration(Constants.SCHEMAAPP_DATA_API_ENDPOINT_KEY,
                        Constants.API_ENDPOINT_CONFIG_PID, configurationAdmin, "");

                SchemaAppConfig config = new SchemaAppConfig(accountId, siteURL, deploymentMethod, endpoint, apiKey);
                while (childPages.hasNext()) {
                    createOrUpdateSchemaNode(childPages, resolver, siteURL);
                }

                commitChanges(resolver);
                bulkDataLoaderAPIService.fetchAndProcessPaginatedData(config, resolver);
                
            }
        } catch (Exception e) {
            LOG.error("Error in fetching details from SchemaApp CDN Data API", e);
        }
    }

    /**
     * Commits the changes in the resource resolver.
     *
     * @param resolver The resource resolver.
     */
    private void commitChanges(ResourceResolver resolver) {
        try {
            resolver.commit();
        } catch (PersistenceException e) {
            LOG.error("Error while committing the resolver: {}", e.getMessage(), e);
        }
    }

    /**
     * Extracts the account ID from the configuration details.
     *
     * @param configDetailMap The configuration details.
     * @return The extracted account ID.
     */
    private String extractAccountId(ValueMap configDetailMap) {
        String accountId = configDetailMap != null ? configDetailMap.get("accountID", String.class) : StringUtils.EMPTY;
        if (accountId != null && accountId.lastIndexOf('/') > 0) {
            int index = accountId.lastIndexOf('/');
            accountId = accountId.substring(index + 1);
        }
        return accountId;
    }

    /**
     * Retrieves configuration details for a page.
     *
     * @param resolver The resource resolver.
     * @param page     The page to fetch configuration for.
     * @return The configuration details as a ValueMap.
     */
    private ValueMap fetchConfigNodeValueMap(ResourceResolver resolver, Page page) {
        ValueMap valueMap = page != null ? page.getProperties() : null;
        if (valueMap != null && valueMap.containsKey(CQ_CLOUDSERVICECONFIGS)) {
            String[] cloudServiceConfigs = valueMap.get(CQ_CLOUDSERVICECONFIGS, String[].class);
            for (String cloudServiceConfig : cloudServiceConfigs) {
                if (cloudServiceConfig.startsWith("/etc/cloudservices/schemaapp/")) {
                    Resource configResource = resolver.getResource(cloudServiceConfig + "/jcr:content");
                    if (configResource != null) {
                        return configResource.getValueMap();
                    }
                }
            }
        }

        return null;
    }

    /**
     * Creates or updates a schema node for a page.
     *
     * @param childPages      Iterator for child pages.
     * @param resourceResolver The resource resolver.
     * @param siteURL         The site URL.
     */
    private void createOrUpdateSchemaNode(Iterator<Page> childPages, ResourceResolver resourceResolver, String siteURL) {
        try {
            // Get the next child page
            final Page child = childPages.next();

            // Construct the path for the node
            String nodePath = child.getPath() + "/jcr:content/schemaapp"; // Path to the schemaapp node

            // Check if the node already exists
            Resource existingNode = resourceResolver.getResource(nodePath);

            if (existingNode == null) {
                // Construct the path for the node
                String parentNodePath = child.getPath() + "/jcr:content";

                // Get or create the parent node
                Resource parentNode = resourceResolver.getResource(parentNodePath);

                // Define the node name and properties
                String nodeName = "schemaapp";
                ValueMap properties = new ValueMapDecorator(new HashMap<String, Object>());
                properties.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, "schemaApp/components/content/entitydata");
                properties.put(Constants.SITEURL, siteURL + child.getPath());

                // Create the node
                resourceResolver.create(parentNode, nodeName, properties);
            } else {

                ModifiableValueMap map = existingNode.adaptTo(ModifiableValueMap.class);

                if (map != null) {
                    map.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, "schemaApp/components/content/entitydata");
                    map.put(Constants.SITEURL, siteURL + child.getPath());
                }
            }
        } catch (PersistenceException e) {
            // Handle any persistence-related exceptions here
            LOG.error("Error creating schema node: {}", e.getMessage(), e);
        }
    }

    /**
     * Obtains a resource resolver for the schema app service.
     *
     * @return The resource resolver.
     */
    public ResourceResolver getResourceResolver() {
        ResourceResolver resourceResolver = null;
        try {
            Map<String, Object> serviceUserParams = new HashMap<>();
            serviceUserParams.put(ResourceResolverFactory.SUBSERVICE, "schema-app-service");
            resourceResolver = resolverFactory.getServiceResourceResolver(serviceUserParams);

        } catch (LoginException e) {
            LOG.error("Error obtaining resource resolver: {}", e.getMessage());
        }
        return resourceResolver;
    }
}
