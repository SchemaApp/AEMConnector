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


	@Override
	public void savenReplicate(Object jsonGraphData, ResourceResolver resolver, Map<String, String> additionalConfigMap, Resource urlResource, SchemaAppConfig config)
			throws RepositoryException, JsonProcessingException, JSONException, PersistenceException,
			ReplicationException {
	    if (urlResource == null) return;
		Node pageNode = urlResource.adaptTo(Node.class);
		Session session = resolver.adaptTo(Session.class);
		if (pageNode != null) {
			Node dataNode = createDataNode(pageNode);
			addConfigDetails(config, dataNode, urlResource, additionalConfigMap);
			saveGraphDatatoNode(jsonGraphData, dataNode);
			resolver.commit();
			if (session != null) session.save();
			flushService.invalidatePageJson(urlResource.getPath() + "/" +Constants.DATA);
		}
	}
	

    public void addConfigDetails(SchemaAppConfig config, Node pageNode,
            Resource urlResource, Map<String, String> additionalConfigMap) throws RepositoryException {
        if (config != null) {

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


    public void savePagePathsToNode(ResourceResolver resolver, String parentNodePath, List<String> pagePaths) throws PersistenceException {
        checkAndCreateNode(resolver, parentNodePath, "pagesData");
        Resource parentNode = resolver.getResource(parentNodePath + "/pagesData");

        if (parentNode != null) {
            ModifiableValueMap properties = parentNode.adaptTo(ModifiableValueMap.class);

            // Assuming 'pagePaths' is a property of type String[]
            properties.put("pagePaths", pagePaths.toArray(new String[0]));

            resolver.commit();
        }
    }
    

    private void setSourceHeaderProperty(
            Map<String, String> additionalConfigMap, Node pageNode)
                    throws RepositoryException {
        String sourceHeadear = additionalConfigMap.containsKey(Constants.SOURCE_HEADER) ? additionalConfigMap.get(Constants.SOURCE_HEADER) : "";
        if (StringUtils.isNotEmpty(sourceHeadear)) pageNode.setProperty(Constants.SOURCE_HEADER, sourceHeadear);
    }

    private void setEtagProperty(Map<String, String> etagMap, Node pageNode)
            throws RepositoryException {
        String eTag = etagMap.containsKey(Constants.E_TAG) ? etagMap.get(Constants.E_TAG) : "";
        if (StringUtils.isNotEmpty(eTag)) pageNode.setProperty(Constants.E_TAG, eTag);
        String eTagJavascript = etagMap.containsKey(Constants.E_TAG_JAVASCRIPT) ? etagMap.get(Constants.E_TAG_JAVASCRIPT) : "";
        if (StringUtils.isNotEmpty(eTagJavascript)) pageNode.setProperty(Constants.E_TAG_JAVASCRIPT, eTagJavascript);
    }


    private void setDeploymentMethodProperty(SchemaAppConfig config,
            Node pageNode) throws RepositoryException {
        if (StringUtils.isNotEmpty(config.getDeploymentMethod())) pageNode.setProperty(Constants.DEPLOYMENTMETHOD, config.getDeploymentMethod());
    }


    private void setSiteURLProperty(SchemaAppConfig config, Node pageNode,
            Resource urlResource) throws RepositoryException {
        if (StringUtils.isNotEmpty(config.getSiteURL())) pageNode.setProperty(Constants.SITEURL, config.getSiteURL() + urlResource.getPath());
    }


    private void setAccountIdProperty(SchemaAppConfig config, Node pageNode)
            throws RepositoryException {
        if (StringUtils.isNotEmpty(config.getAccountId())) pageNode.setProperty(Constants.ACCOUNT_ID, config.getAccountId());
    }
    

    public void removeResource(String resourcePath, ResourceResolver resolver) {
        // Acquire a resource resolver using the service user
        try {

            // Get the session from the resource resolver
            Session session = resolver.adaptTo(Session.class);

            // Get the node for the specified resource path
            Node node = session.getNode(resourcePath+"/jcr:content/schemaapp");

            if (node != null) {
                // Remove the node
                node.remove();
            }

            // Save the session to persist the changes
            session.save();
        } catch (Exception e) {
            // Handle exceptions appropriately
            e.printStackTrace();
        }
    }
    

}
