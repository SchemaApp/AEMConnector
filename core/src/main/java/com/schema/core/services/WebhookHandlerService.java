package com.schema.core.services;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;

import com.schema.core.models.WebhookEntity;
import com.schema.core.models.WebhookEntityResult;

/**
 * 
 * The <code>WebhookEntityResult</code> interface represents a Schema App Webhooks envets.
 * 
 * @author nikhil
 *
 */
public interface WebhookHandlerService {

	/**
	 * Schema App Create Entity Webhook. When markup for a page is first created.
	 * 
	 * @param entiry
	 * @return
	 * @throws LoginException
	 */
	public WebhookEntityResult createEntity(WebhookEntity entity) throws LoginException;
	
	/**
	 * Schema App Update Entity Webhook. When markup for a page is updated.
	 * 
	 * @param entiry
	 * @return
	 * @throws LoginException
	 */
	public WebhookEntityResult updateEntity(WebhookEntity entity) throws LoginException;
	
	/**
	 * Schema App Delete Entity Webhook. When markup for a page is deleted.
	 * 
	 * @param entiry
	 * @return
	 * @throws LoginException
	 * @throws PersistenceException
	 */
	public WebhookEntityResult deleteEntity(WebhookEntity entity) throws LoginException, PersistenceException;
	
}
