package com.schema.core.services;

/**
 * The <code> PageJSONDataReaderService </code> class represents to fetch page JSON-LD data.
 * @author nikhil
 *
 */
public interface PageJSONDataReaderService {

	/**
	 * Get Page Data method used to get page specific JSON data String.
	 * 
	 * @param pageURL
	 * @return
	 */
	public String getPageData(String pageURL);
}
