package com.schemaapp.core.services.impl;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
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
import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.schemaapp.core.services.CDNHandlerService;
import com.schemaapp.core.services.FlushService;
import com.schemaapp.core.util.Constants;
import com.schemaapp.core.util.JsonSanitizer;

@Component(service = CDNHandlerService.class, immediate = true)
public class CDNHandlerServiceImpl implements CDNHandlerService {

	private static final String SCHEMA_APP_COMPONENTS_RESOURCE_TYPE = "schemaApp/components/content/entitydata";

	private static final Logger LOG = LoggerFactory.getLogger(CDNHandlerServiceImpl.class);

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
	 * @param entity
	 * @param pageNode
	 * @throws JsonProcessingException
	 * @throws JSONException
	 * @throws RepositoryException
	 */
	private void saveGraphDatatoNode(Object jsonGraphData, Node pageNode) throws JSONException, RepositoryException {

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
	private Node createDataNode(Node node) throws RepositoryException {
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
	public void savenReplicate(Object jsonGraphData, ResourceResolver resolver, Map<String, String> additionalConfigMap, Resource urlResource, ValueMap configDetailMap)
			throws RepositoryException, JsonProcessingException, JSONException, PersistenceException,
			ReplicationException {
		Node pageNode = urlResource.adaptTo(Node.class);
		Session session = resolver.adaptTo(Session.class);
		if (pageNode != null) {
			Node dataNode = createDataNode(pageNode);
			addConfigDetails(configDetailMap, dataNode, urlResource, additionalConfigMap);
			saveGraphDatatoNode(jsonGraphData, dataNode);
			resolver.commit();
			if (session != null) session.save();
			flushService.invalidatePageJson(urlResource.getPath() + "/" +Constants.DATA);
		}
	}
	

    private void addConfigDetails(ValueMap configDetailMap, Node pageNode,
            Resource urlResource, Map<String, String> additionalConfigMap) throws RepositoryException {
        if (configDetailMap != null) {

            setAccountIdProperty(configDetailMap, pageNode);

            setSiteURLProperty(configDetailMap, pageNode, urlResource);

            setDeploymentMethodProperty(configDetailMap, pageNode);

            setEtagProperty(additionalConfigMap, pageNode);
            
            setSourceHeaderProperty(additionalConfigMap, pageNode);
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


    private String setDeploymentMethodProperty(ValueMap configDetailMap,
            Node pageNode) throws RepositoryException {
        String deploymentMethod = configDetailMap.containsKey("deploymentMethod") ?  (String) configDetailMap.get("deploymentMethod") : StringUtils.EMPTY;
        if (StringUtils.isNotEmpty(deploymentMethod)) pageNode.setProperty(Constants.DEPLOYMENTMETHOD, deploymentMethod);
        return deploymentMethod;
    }


    private void setSiteURLProperty(ValueMap configDetailMap, Node pageNode,
            Resource urlResource) throws RepositoryException {
        String siteURL = configDetailMap.containsKey("siteURL") ?  (String) configDetailMap.get("siteURL") : StringUtils.EMPTY;
        if (StringUtils.isNotEmpty(siteURL)) pageNode.setProperty(Constants.SITEURL, siteURL + urlResource.getPath());
    }


    private void setAccountIdProperty(ValueMap configDetailMap, Node pageNode)
            throws RepositoryException {
        String accountId = configDetailMap.containsKey("accountID") ? (String) configDetailMap.get("accountID") : StringUtils.EMPTY;
        if (StringUtils.isNotEmpty(accountId)) pageNode.setProperty(Constants.ACCOUNT_ID, accountId);
    }
    

	/**
	 * This method used to delete entity 
	 */
	@Override
	public void deleteEntity(Page page, ResourceResolver resolver) throws LoginException, PersistenceException {

	    Node pageNode = page.adaptTo(Node.class);
	    Session session = resolver.adaptTo(Session.class);
	    try {
	        if (pageNode != null && pageNode.hasNode(Constants.DATA)) {
	            Node dataNode = getDataNode(pageNode);
	            if (dataNode != null) dataNode.remove();
	            if (session != null) session.save();
	        }
	    } catch (RepositoryException e) {
	        String errorMessage = "CDNHandlerServiceImpl :: Occured error during deleting Schema App Entity Node into the AEM Instance ";
	        LOG.error(errorMessage, e);
	    }
	}

}
