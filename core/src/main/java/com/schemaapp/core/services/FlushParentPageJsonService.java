package com.schemaapp.core.services;

public interface FlushParentPageJsonService {

	/**
	 * Override invalidatePageJson method of FlushParentPageJsonService
	 *
	 * @param String the pageUrl
	 */
	void invalidatePageJson(String pageUrl);

}
