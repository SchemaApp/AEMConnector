package com.schemaapp.core.services;

public interface FlushService {

	/**
	 * Override invalidatePageJson method of FlushParentPageJsonService
	 *
	 * @param pageUrl
	 */
	void invalidatePageJson(String pageUrl); 
}
