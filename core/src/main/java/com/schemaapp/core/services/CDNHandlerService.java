package com.schemaapp.core.services;

import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.json.JSONException;

import com.day.cq.replication.ReplicationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.schemaapp.core.models.SchemaAppConfig;

/**
 * 
 * The <code>WebhookEntityResult</code> interface represents a Schema App Webhooks envets.
 * 
 * @author nikhil
 *
 */
public interface CDNHandlerService {
	
    public void removeResource(String resourcePath, ResourceResolver resolver);
	
	/**
	 * @param resolver
	 * @param additionalConfigMap
	 * @param urlResource
	 * @throws RepositoryException
	 * @throws JsonProcessingException
	 * @throws JSONException
	 * @throws PersistenceException
	 * @throws ReplicationException
	 */
	public void savenReplicate(Object jsonGraphData, 
	        ResourceResolver resolver, 
	        Map<String, String> additionalConfigMap, 
	        Resource urlResource, 
	        SchemaAppConfig configDetailMap) throws RepositoryException, 
	                JsonProcessingException, 
	                JSONException, PersistenceException, 
	                ReplicationException;
	
	public void savePagePathsToNode(ResourceResolver resolver, String parentNodePath, List<String> pagePaths) throws PersistenceException;
	
	
}
