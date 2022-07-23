package com.schemaapp.core.services;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public interface CDNDataAPIService {

	void readCDNData();
	
	URL getURL(String endpoint, String accountId, String encodedURL, String deploymentMethod) throws MalformedURLException;
	
	HttpURLConnection getHttpURLConnection(URL url) throws IOException;
}
