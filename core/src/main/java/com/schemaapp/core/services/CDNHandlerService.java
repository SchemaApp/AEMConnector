package com.schemaapp.core.services;

import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.json.JSONException;

import com.day.cq.replication.ReplicationException;
import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * 
 * The <code>WebhookEntityResult</code> interface represents a Schema App Webhooks envets.
 * 
 * @author nikhil
 *
 */
public interface CDNHandlerService {
	
	/**
	 *     Schema App Delete Entity Webhook. When markup for a page is deleted.
	 * 
	 * @param page
	 * @param resolver
	 * @throws LoginException
	 * @throws PersistenceException
	 */
    public void deleteEntity(Page page, ResourceResolver resolver) 
            throws LoginException, PersistenceException;
	
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
	        ValueMap configDetailMap,
	        String cacheCleaningRequired) throws RepositoryException, 
	                JsonProcessingException, 
	                JSONException, PersistenceException, 
	                ReplicationException;
	
}
