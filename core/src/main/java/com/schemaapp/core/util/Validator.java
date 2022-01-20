package com.schemaapp.core.util;

import org.apache.commons.lang3.StringUtils;

import com.schemaapp.core.exception.MissingWebhookEntityAttributesException;
import com.schemaapp.core.models.WebhookEntity;

/**
 * The <code>Validator</code> class to use validation related functions.
 * 
 * @author nikhil
 *
 */
public final class Validator {

	private Validator() {}
	
	/**
	 * This method used to validate Webhook Entity.
	 * 
	 * @param entity
	 * @return
	 * @throws MissingWebhookEntityAttributesException
	 */
	public static boolean validateEntity(WebhookEntity entity) throws MissingWebhookEntityAttributesException {
		if (StringUtils.isBlank(entity.getType())) throw new MissingWebhookEntityAttributesException("Missing Required Attribute -> Type ");
		if (StringUtils.isBlank(entity.getId())) throw new MissingWebhookEntityAttributesException("Missing Required Attribute -> Id ");
		if (StringUtils.isBlank(entity.getBase64encode())) throw new MissingWebhookEntityAttributesException("Missing Required Attribute -> Base64encode");
		if (StringUtils.isBlank(entity.getUrl())) throw new MissingWebhookEntityAttributesException("Missing Required Attribute -> URL");
		if (entity.getContext() == null) throw new MissingWebhookEntityAttributesException("Missing Required Attribute -> Context : ");
		if (StringUtils.isBlank(entity.getContext().getVocab())) throw new MissingWebhookEntityAttributesException("Missing Required Attribute -> Context- Vocab : ");
		if (StringUtils.isBlank(entity.getContext().getGeneratedAtTime())) throw new MissingWebhookEntityAttributesException("Missing Required Attribute -> Context-Base64encode : ");
		if (entity.getGraph() == null) throw new MissingWebhookEntityAttributesException("Missing Required Attribute -> Graph : ");
		return true;
	}
}
