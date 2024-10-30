package com.schemaapp.core.services.impl;

import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import com.day.cq.search.QueryBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.schemaapp.core.models.SchemaAppConfig;
import com.schemaapp.core.services.CDNHandlerService;
import com.schemaapp.core.services.FlushService;
import com.schemaapp.core.util.Constants;
import com.schemaapp.core.util.JsonSanitizer;

@Component(service = CDNHandlerService.class, immediate = true)
public class CDNHandlerServiceImpl implements CDNHandlerService {

	private static final String SCHEMA_APP_COMPONENTS_RESOURCE_TYPE = "schemaApp/components/content/entitydata";

	@Reference
	ResourceResolverFactory resolverFactory;

	@Reference
	private QueryBuilder builder;

	@Reference
	FlushService flushService;

	@Reference
	Replicator replicator;
	
    public static Logger logger = LoggerFactory.getLogger(CDNHandlerServiceImpl.class);


	/**
	 * Save Graph Data to AEM Node
	 * 
	 * @param jsonGraphData
	 * @param pageNode
	 * @throws JSONException
	 * @throws RepositoryException
	 */
	public void saveGraphDatatoNode(Object jsonGraphData, Node pageNode) throws JSONException, RepositoryException {

		String graphData = getGraphDataString(jsonGraphData);
		String wellFormedJson = null;
		if (!StringUtils.isBlank(graphData) && graphData.startsWith("[")) {
			JSONArray obj = new JSONArray(graphData);
			wellFormedJson = JsonSanitizer.sanitize(obj.toString());
		} else {
			JSONObject graphJsonObject = new JSONObject(graphData);
			wellFormedJson = JsonSanitizer.sanitize(graphJsonObject.toString());
		}	
		
		pageNode.setProperty(Constants.ENTITY, wellFormedJson);
	}
	
	/**
	 * Get Graph data string.
	 * 
	 * @param jsonGraphData
	 * @return graphData
	 */
	private String getGraphDataString(Object jsonGraphData) {
	    return jsonGraphData.toString();
    }

	/**
	 * This method used to create data node.
	 * 
	 * @param node
	 * @return
	 * @throws RepositoryException
	 */
	public Node createDataNode(Node node) throws RepositoryException {
		Node dataNode = null;
		if (!node.hasNode(Constants.DATA)) {
			dataNode = node.addNode(Constants.DATA, JcrConstants.NT_UNSTRUCTURED);
			dataNode.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, SCHEMA_APP_COMPONENTS_RESOURCE_TYPE);
		} else {
			dataNode = getDataNode(node);
		}
		return dataNode;
	}

    private Node getDataNode(Node node)
            throws RepositoryException {
        return node.getNode(Constants.DATA);
    }


	/**
	 * Saves the given JSON graph data to the specified URL resource node and replicates it.
	 *
	 * @param jsonGraphData        The JSON graph data to save in the node.
	 * @param resolver             The ResourceResolver used to adapt the URL resource to a JCR Node and to perform persistence actions.
	 * @param additionalConfigMap   A map containing additional configuration settings required for the replication.
	 * @param urlResource          The URL resource representing the page node where data is to be saved.
	 * @param config               The SchemaAppConfig containing the configuration details needed for processing.
	 * @throws RepositoryException      If there is an error accessing the repository.
	 * @throws JsonProcessingException  If there is an error in processing the JSON graph data.
	 * @throws JSONException            If there is an error in handling JSON operations.
	 * @throws PersistenceException     If there is an error in persisting changes in the repository.
	 * @throws ReplicationException     If there is an error in replicating the content to publish.
	 */
	@Override
	public void savenReplicate(Object jsonGraphData, ResourceResolver resolver, Map<String, String> additionalConfigMap, Resource urlResource, SchemaAppConfig config)
	        throws RepositoryException, JsonProcessingException, JSONException, PersistenceException, ReplicationException {
	    
	    logger.debug("Starting savenReplicate method with provided JSON graph data.");

	    if (urlResource == null) {
	        logger.warn("URL resource is null, exiting savenReplicate method.");
	        return;
	    }

	    Node pageNode = urlResource.adaptTo(Node.class);
	    Session session = resolver.adaptTo(Session.class);
	    logger.debug("Adapted urlResource to pageNode and obtained session.");

	    if (pageNode != null) {
	        logger.debug("Creating data node under the page node.");
	        
	        Node dataNode = createDataNode(pageNode);
	        addConfigDetails(config, dataNode, urlResource, additionalConfigMap);

	        logger.debug("Saving JSON graph data to the data node.");
	        saveGraphDatatoNode(jsonGraphData, dataNode);
	        
	        logger.debug("Committing changes to the resolver.");
	        resolver.commit();
	        
	        if (session != null) {
	            session.save();
	            logger.debug("Session saved successfully.");
	        }
	        
	        String dataPath = urlResource.getPath() + "/" + Constants.DATA;
	        logger.debug("Invalidating page JSON for path: {}", dataPath);
	        flushService.invalidatePageJson(dataPath);
	    } else {
	        logger.warn("Page node could not be adapted from the provided urlResource.");
	    }
	}

	

	/**
	 * Adds configuration details from the provided {@link SchemaAppConfig} and additional configuration map 
	 * to the specified page node.
	 *
	 * @param config              The SchemaAppConfig object containing primary configuration details like 
	 *                            account ID, site URL, and deployment method.
	 * @param pageNode            The JCR Node representing the page where configuration properties will be added.
	 * @param urlResource         The Resource used to derive the URL path for setting the site URL property.
	 * @param additionalConfigMap A map containing additional configuration properties such as ETag and Source Header.
	 * @throws RepositoryException If there is an error accessing the repository or setting properties on the node.
	 */
	private void addConfigDetails(SchemaAppConfig config, Node pageNode, Resource urlResource, Map<String, String> additionalConfigMap) 
	        throws RepositoryException {
	    
	    if (config != null) {
	        // Set various properties on the page node based on the config and additional configuration map
	        setAccountIdProperty(config, pageNode);
	        setSiteURLProperty(config, pageNode, urlResource);
	        setDeploymentMethodProperty(config, pageNode);
	        setEtagProperty(additionalConfigMap, pageNode);
	        setSourceHeaderProperty(additionalConfigMap, pageNode);
	    }
	}
    
    /**
     * Checks if a child node exists under a specified parent node in Adobe Experience Manager (AEM)
     * and creates it if it doesn't exist.
     *
     * @param resolver      The ResourceResolver used to access the AEM repository.
     * @param parentNodePath The path of the parent node in which the child node will be checked/created.
     * @param newNodeName    The name of the new child node to be checked/created.
     */
    private void checkAndCreateNode(ResourceResolver resolver, String parentNodePath, String newNodeName) {
        // Construct the path for the child node
        String childNodePath = parentNodePath + "/" + newNodeName;

        try {
            // Adapt the resolver to a JCR Session
            Session session = resolver.adaptTo(Session.class);

            // Check if the child node already exists
            Resource childNodeResource = resolver.getResource(childNodePath);
            if (childNodeResource == null) {

                // Ensure the parent node exists
                Resource parentNode = resolver.getResource(parentNodePath);

                if (parentNode != null) {
                    // Adapt the parent node to a JCR Node
                    Node parentNodeJCR = parentNode.adaptTo(Node.class);

                    // Create the child node
                    parentNodeJCR.addNode(newNodeName, "sling:Folder");

                    // Save the session to persist the changes
                    session.save();
                }
            }
        } catch (RepositoryException e) {
            // Handle RepositoryException
            e.printStackTrace();
        }
    }


    /**
     * Saves a list of page paths to a specified node in the JCR repository. If the target node does not exist,
     * it is created under the provided parent path.
     *
     * @param resolver       The ResourceResolver used for accessing and modifying repository nodes.
     * @param parentNodePath The path of the parent node where the "pagesData" node should be created or accessed.
     * @param pagePaths      A list of page paths to be saved as a property on the "pagesData" node.
     * @throws PersistenceException If there is an error in committing changes to the repository.
     */
    public void savePagePathsToNode(ResourceResolver resolver, String parentNodePath, List<String> pagePaths) 
            throws PersistenceException {
        
        // Check if the "pagesData" node exists under the parent path; create it if not
        checkAndCreateNode(resolver, parentNodePath, "pagesData");
        Resource parentNode = resolver.getResource(parentNodePath + "/pagesData");

        if (parentNode != null) {
            ModifiableValueMap properties = parentNode.adaptTo(ModifiableValueMap.class);

            // Assuming 'pagePaths' is a property of type String[]
            properties.put("pagePaths", pagePaths.toArray(new String[0]));

            // Commit changes to the repository
            resolver.commit();
        }
    }

    /**
     * Sets the SOURCE_HEADER property on the given page node if it exists in the additionalConfigMap.
     *
     * @param additionalConfigMap The map containing additional configuration parameters.
     * @param pageNode            The JCR Node representing the page where the property should be set.
     * @throws RepositoryException If there is an error accessing the repository or setting the property.
     */
    private void setSourceHeaderProperty(Map<String, String> additionalConfigMap, Node pageNode)
            throws RepositoryException {
        String sourceHeader = additionalConfigMap.containsKey(Constants.SOURCE_HEADER) ? additionalConfigMap.get(Constants.SOURCE_HEADER) : "";
        if (StringUtils.isNotEmpty(sourceHeader)) {
            pageNode.setProperty(Constants.SOURCE_HEADER, sourceHeader);
        }
    }

    /**
     * Sets the E_TAG and E_TAG_JAVASCRIPT properties on the given page node if they exist in the etagMap.
     *
     * @param etagMap  The map containing ETag values and related configuration.
     * @param pageNode The JCR Node representing the page where the properties should be set.
     * @throws RepositoryException If there is an error accessing the repository or setting the properties.
     */
    private void setEtagProperty(Map<String, String> etagMap, Node pageNode)
            throws RepositoryException {
        String eTag = etagMap.containsKey(Constants.E_TAG) ? etagMap.get(Constants.E_TAG) : "";
        if (StringUtils.isNotEmpty(eTag)) {
            pageNode.setProperty(Constants.E_TAG, eTag);
        }
        
        String eTagJavascript = etagMap.containsKey(Constants.E_TAG_JAVASCRIPT) ? etagMap.get(Constants.E_TAG_JAVASCRIPT) : "";
        if (StringUtils.isNotEmpty(eTagJavascript)) {
            pageNode.setProperty(Constants.E_TAG_JAVASCRIPT, eTagJavascript);
        }
    }

    /**
     * Sets the DEPLOYMENTMETHOD property on the given page node based on the deployment method from the SchemaAppConfig.
     *
     * @param config   The SchemaAppConfig containing configuration details.
     * @param pageNode The JCR Node representing the page where the property should be set.
     * @throws RepositoryException If there is an error accessing the repository or setting the property.
     */
    private void setDeploymentMethodProperty(SchemaAppConfig config, Node pageNode)
            throws RepositoryException {
        if (StringUtils.isNotEmpty(config.getDeploymentMethod())) {
            pageNode.setProperty(Constants.DEPLOYMENTMETHOD, config.getDeploymentMethod());
        }
    }

    /**
     * Sets the SITEURL property on the given page node, combining the site URL from the SchemaAppConfig
     * and the path of the given urlResource.
     *
     * @param config      The SchemaAppConfig containing the site URL.
     * @param pageNode    The JCR Node representing the page where the property should be set.
     * @param urlResource The Resource whose path will be appended to the site URL.
     * @throws RepositoryException If there is an error accessing the repository or setting the property.
     */
    private void setSiteURLProperty(SchemaAppConfig config, Node pageNode, Resource urlResource)
            throws RepositoryException {
        if (StringUtils.isNotEmpty(config.getSiteURL())) {
            pageNode.setProperty(Constants.SITEURL, config.getSiteURL() + urlResource.getPath());
        }
    }

    /**
     * Sets the ACCOUNT_ID property on the given page node if the account ID exists in the SchemaAppConfig.
     *
     * @param config   The SchemaAppConfig containing the account ID.
     * @param pageNode The JCR Node representing the page where the property should be set.
     * @throws RepositoryException If there is an error accessing the repository or setting the property.
     */
    private void setAccountIdProperty(SchemaAppConfig config, Node pageNode)
            throws RepositoryException {
        if (StringUtils.isNotEmpty(config.getAccountId())) {
            pageNode.setProperty(Constants.ACCOUNT_ID, config.getAccountId());
        }
    }

    /**
     * Removes a specific resource node at the provided resource path if it exists.
     *
     * @param resourcePath The path of the resource to remove, with the target node assumed to be located at "/jcr:content/schemaapp" under this path.
     * @param resolver     The ResourceResolver used to access and modify the repository nodes.
     */
    public void removeResource(String resourcePath, ResourceResolver resolver) {
        try {
            // Adapt the resource resolver to obtain a session
            Session session = resolver.adaptTo(Session.class);

            if (session == null) {
                logger.error("Unable to retrieve session from the resource resolver. Cannot proceed with resource removal.");
                return;
            }

            // Attempt to retrieve the node at the specified resource path
            Node node = session.getNode(resourcePath + "/jcr:content/schemaapp");

            if (node != null) {
                logger.debug("Node found at path {}. Removing node.", resourcePath + "/jcr:content/schemaapp");

                // Remove the node
                node.remove();

                // Save the session to persist changes
                session.save();
                logger.info("Successfully removed resource at path {}", resourcePath + "/jcr:content/schemaapp");
            } else {
                logger.warn("Node at path {} does not exist. No action taken.", resourcePath + "/jcr:content/schemaapp");
            }
        } catch (RepositoryException e) {
            logger.error("Failed to remove resource at path {} due to repository exception: {}", resourcePath + "/jcr:content/schemaapp", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("An unexpected error occurred while trying to remove resource at path {}: {}", resourcePath + "/jcr:content/schemaapp", e.getMessage(), e);
        }
    }

}
