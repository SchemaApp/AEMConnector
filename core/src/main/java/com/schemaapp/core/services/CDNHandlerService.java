package com.schemaapp.core.services;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.json.JSONException;

import com.day.cq.replication.ReplicationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.schemaapp.core.models.CDNEntity;
import com.schemaapp.core.models.CDNEntityResult;

/**
 * 
 * The <code>WebhookEntityResult</code> interface represents a Schema App Webhooks envets.
 * 
 * @author nikhil
 *
 */
public interface CDNHandlerService {
	
	/**
	 * Schema App Delete Entity Webhook. When markup for a page is deleted.
	 * 
	 * @param entiry
	 * @return
	 * @throws LoginException
	 * @throws PersistenceException
	 */
	public CDNEntityResult deleteEntity(CDNEntity entity) throws LoginException, PersistenceException;
	
	/**
	 * @param entity
	 * @param resolver
	 * @param session
	 * @param urlResource
	 * @throws RepositoryException
	 * @throws JsonProcessingException
	 * @throws JSONException
	 * @throws PersistenceException
	 * @throws ReplicationException
	 */
	public void savenReplicate(Object jsonGraphData, ResourceResolver resolver, Session session, Resource urlResource, ValueMap configDetailMap) throws RepositoryException, JsonProcessingException, JSONException, PersistenceException, ReplicationException;
	
}
