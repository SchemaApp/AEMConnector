package com.schemaapp.core.services;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.json.JSONException;

import com.day.cq.replication.ReplicationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.schemaapp.core.exception.AEMURLNotFoundException;
import com.schemaapp.core.exception.InvalidWebHookJsonDataException;
import com.schemaapp.core.models.WebhookEntity;
import com.schemaapp.core.models.WebhookEntityResult;

/**
 * 
 * The <code>WebhookEntityResult</code> interface represents a Schema App Webhooks envets.
 * 
 * @author nikhil
 *
 */
public interface WebhookHandlerService {
	
	/**
	 * Schema App Update Entity Webhook. When markup for a page is updated.
	 * 
	 * @param entiry
	 * @return
	 * @throws LoginException, AEMURLNotFoundException
	 */
	public WebhookEntityResult updateEntity(WebhookEntity entity) throws LoginException, AEMURLNotFoundException, InvalidWebHookJsonDataException;
	
	/**
	 * Schema App Delete Entity Webhook. When markup for a page is deleted.
	 * 
	 * @param entiry
	 * @return
	 * @throws LoginException
	 * @throws PersistenceException
	 */
	public WebhookEntityResult deleteEntity(WebhookEntity entity) throws LoginException, PersistenceException;
	
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
	public void savenReplicate(Object jsonGraphData, ResourceResolver resolver, Session session, Resource urlResource) throws RepositoryException, JsonProcessingException, JSONException, PersistenceException, ReplicationException;
	
}
