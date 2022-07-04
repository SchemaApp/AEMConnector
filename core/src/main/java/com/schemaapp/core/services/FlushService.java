package com.schemaapp.core.services;

public interface FlushService {

	/**
	 * Override invalidatePageJson method of FlushParentPageJsonService
	 *
	 * @param String the pageUrl
	 */
	void invalidatePageJson(String pageUrl);

}
