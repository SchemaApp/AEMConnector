package com.schemaapp.core.util;

import org.apache.commons.lang3.StringUtils;

import com.schemaapp.core.exception.AEMURLNotFoundException;
import com.schemaapp.core.models.CDNEntity;

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
	 * @throws AEMURLNotFoundException
	 */
	public static boolean validateEntity(CDNEntity entity) throws AEMURLNotFoundException {
		if (entity == null) throw new AEMURLNotFoundException("Missing Required Attributes ");
		if (StringUtils.isBlank(entity.getType())) throw new AEMURLNotFoundException("Missing Required Attribute -> Type ");
		if (StringUtils.isBlank(entity.getId())) throw new AEMURLNotFoundException("Missing Required Attribute -> Id ");
		if (StringUtils.isBlank(entity.getBase64encode())) throw new AEMURLNotFoundException("Missing Required Attribute -> Base64encode");
		if (StringUtils.isBlank(entity.getUrl())) throw new AEMURLNotFoundException("Missing Required Attribute -> URL");
		if (entity.getContext() == null) throw new AEMURLNotFoundException("Missing Required Attribute -> Context : ");
		if (StringUtils.isBlank(entity.getContext().getVocab())) throw new AEMURLNotFoundException("Missing Required Attribute -> Context- Vocab : ");
		if (StringUtils.isBlank(entity.getContext().getGeneratedAtTime())) throw new AEMURLNotFoundException("Missing Required Attribute -> Context-Base64encode : ");
		if (entity.getGraph() == null) throw new AEMURLNotFoundException("Missing Required Attribute -> Graph : ");
		return true;
	}
}
