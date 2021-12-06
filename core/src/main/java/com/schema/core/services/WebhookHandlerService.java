package com.schema.core.services;

import org.apache.sling.api.resource.LoginException;

import com.schema.core.models.WebhookEntity;
import com.schema.core.models.WebhookEntityResult;

public interface WebhookHandlerService {

	/**
	 * Schema App Create Entity.
	 * @param entiry
	 * @return
	 * @throws LoginException
	 */
	public WebhookEntityResult createEntity(WebhookEntity entiry) throws LoginException;
}
