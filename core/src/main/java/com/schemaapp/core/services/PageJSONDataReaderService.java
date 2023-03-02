package com.schemaapp.core.services;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * The <code> PageJSONDataReaderService </code> class represents to fetch page JSON-LD data.
 * @author nikhil
 *
 */
public interface PageJSONDataReaderService {

	/**
	 * Init method used to get page specific JSON data String and Source.
	 * 
	 * @param pageUrl
	 */
    public void init(String pageUrl);
	
	/**
	 * @return
	 */
	public ResourceResolver getResourceResolver() throws LoginException;
	
	/**
	 * Graph Data.
	 * 
	 * @return
	 */
	public String getGraphData();
	
	/**
	 * Source Header.
	 * 
	 * @return
	 */
	public String getSource();
}
