package com.schemaapp.core.services.impl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Session;

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

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageFilter;
import com.day.cq.wcm.api.PageManager;
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
        LOG.debug("Schema App fetchSiteRootPages start");

        List<Page> rootPages = new ArrayList<>();

        try {
            // Get the QueryBuilder instance
            QueryBuilder queryBuilder = resolver.adaptTo(QueryBuilder.class);
            if (queryBuilder == null) {
                LOG.error("Could not adapt QueryBuilder");
                return rootPages;
            }

            // Define search parameters (without LIKE)
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put("path", "/content"); // Search under /content
            queryMap.put("type", "cq:PageContent"); // Search cq:PageContent nodes
            queryMap.put("property", CQ_CLOUDSERVICECONFIGS); // Search by cloudserviceconfigs property
            queryMap.put("property.operation", "exists"); // Check that the property exists
            queryMap.put("p.limit", "-1"); // No limit on results

            // Create a PredicateGroup using the queryMap
            PredicateGroup predicateGroup = PredicateGroup.create(queryMap);

            // Create the query
            Query query = queryBuilder.createQuery(predicateGroup, resolver.adaptTo(Session.class));
            SearchResult result = query.getResult();

            // Get the PageManager from the resolver
            PageManager pageManager = resolver.adaptTo(PageManager.class);

            // Process the results and manually filter for /etc/cloudservices/schemaapp
            for (Hit hit : result.getHits()) {
                try {
                    Resource jcrContentResource = hit.getResource();
                    if (jcrContentResource != null) {
                        // Get the parent resource (the cq:Page node) of the jcr:content node
                        Resource pageResource = jcrContentResource.getParent(); 
                        if (pageResource != null) {
                            // Adapt the parent resource to a Page object
                            Page page = pageManager.getContainingPage(pageResource);
                            if (page != null) {
                                // Check manually if any value in the multi-valued property starts with /etc/cloudservices/schemaapp
                                String[] cloudServiceConfigs = jcrContentResource.getValueMap().get(CQ_CLOUDSERVICECONFIGS, new String[0]);
                                for (String config : cloudServiceConfigs) {
                                    if (config.startsWith("/etc/cloudservices/schemaapp")) {
                                        rootPages.add(page);
                                        LOG.debug("Schema App fetchSiteRootPages found match for page: {}", page.getPath());
                                        break; // No need to check other values once a match is found
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    LOG.error("Error processing hit: ", e);
                }
            }

        } catch (Exception e) {
            LOG.error("Error executing query for schema app cloud service: ", e);
        }

        LOG.debug("Schema App fetchSiteRootPages End, rootPages {}", rootPages);
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
            LOG.debug("Schema App Update Page node page path {}", page.getPath());
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

            LOG.debug("Schema App Create/update node, path {}", child.getPath());
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
                
                LOG.debug("Schema App Create node, creating new node nodeName {}", nodeName);
            } else {

                ModifiableValueMap map = existingNode.adaptTo(ModifiableValueMap.class);

                if (map != null) {
                    map.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, "schemaApp/components/content/entitydata");
                    map.put(Constants.SITEURL, siteURL + child.getPath());
                    
                    LOG.debug("Schema App update node, updating existing node, node path {}", nodePath);
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
