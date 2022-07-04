package com.schemaapp.core.services.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageFilter;
import com.schemaapp.core.services.CDNDataAPIService;
import com.schemaapp.core.util.ConfigurationUtil;
import com.schemaapp.core.util.Constants;

@Component(service = CDNDataAPIService.class, immediate = true)
public class CDNDataAPIServiceImpl implements CDNDataAPIService {

	private static final String CQ_CLOUDSERVICECONFIGS = "cq:cloudserviceconfigs";

	private final Logger LOG = LoggerFactory.getLogger(CDNDataAPIServiceImpl.class);

	@Reference
	ConfigurationAdmin configurationAdmin;

	@Reference
	transient ResourceResolverFactory resolverFactory;

	@Override
	public void readCDNData() {

		try {
			ResourceResolver resolver = getResourceResolver();
			List<Page> rootpages = getSiteRootPages(resolver);
			for (Page page : rootpages) {
				getSchemaAppCDNData(resolver, page);
			}
		} catch (Exception e) {
			LOG.error("Error occurs while read CDN data", e.getMessage());
		}
	}

	private void getSchemaAppCDNData(ResourceResolver resolver, Page page) {
		String accountId;
		try {
			accountId = geAccountId(resolver, page);
			Iterator<Page> childPages = page.listChildren(new PageFilter(), true);
			while (childPages.hasNext()) {
				final Page child = childPages.next();
				String endpoint = ConfigurationUtil.getConfiguration(Constants.SCHEMAAPP_DATA_API_ENDPOINT_KEY,
						Constants.API_ENDPOINT_CONFIG_PID,
						configurationAdmin, "");
				String pagePath = child.getPath();
				String encodedURL = Base64.getUrlEncoder().encodeToString(pagePath.getBytes());
				URL url = getURL(endpoint, accountId, encodedURL);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod(HttpConstants.METHOD_GET);
				connection.setConnectTimeout(5 * 1000);
				connection.setReadTimeout(7 * 1000);

				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String inputLine;
				StringBuilder content = new StringBuilder();
				while ((inputLine = bufferedReader.readLine()) != null) { content.append(inputLine); }

				LOG.error("pagePath - {} Data  {}",pagePath, content.toString());

				bufferedReader.close();
				connection.disconnect();
			}
		} catch (Exception e) {
			LOG.error("Error in fetching details from API", e.getMessage());
		}
	}

	private String geAccountId(ResourceResolver resolver, Page page) {
		String accountId = null;
		ValueMap valueMap = page.getProperties();
		if (valueMap.containsKey(CQ_CLOUDSERVICECONFIGS)) {
			String[] cloudserviceconfigs = (String[]) valueMap.get(CQ_CLOUDSERVICECONFIGS);
			for (String cloudserviceconfig : cloudserviceconfigs) {
				if (cloudserviceconfig.startsWith("/etc/cloudservices/schemaapp/")) {
					Resource configResource = resolver.getResource(cloudserviceconfig + "/jcr:content");
					if (configResource != null) {
						accountId = (String) configResource.getValueMap().get("accountID");

						if (accountId != null) {
							int index = accountId.lastIndexOf('/');
							accountId = accountId.substring(index, accountId.length());
						}
					}
				}
			}
		}

		return accountId;
	}

	private List<Page> getSiteRootPages(ResourceResolver resolver) throws LoginException {
		List<Page> rootpages = new ArrayList<>();
		Resource contentResource = resolver.getResource("/content");
		if (contentResource == null) return rootpages;
		Iterator<Resource> childResourceIterator = contentResource.listChildren();
		while (childResourceIterator.hasNext()) {
			final Resource child = childResourceIterator.next();
			if (child != null && child.getResourceType().equals("cq:Page")) {
				Resource jcrcontentResource = child.getChild("jcr:content");
				if (jcrcontentResource != null && jcrcontentResource.getValueMap().get(CQ_CLOUDSERVICECONFIGS) != null) {
					String[] cloudserviceconfigs = (String[]) jcrcontentResource.getValueMap().get(CQ_CLOUDSERVICECONFIGS);
					boolean contains = Arrays.stream(cloudserviceconfigs).anyMatch(s -> s.startsWith("/etc/cloudservices/schemaapp"));

					if (contains) {
						Page page = child.adaptTo(Page.class);
						rootpages.add(page);
					}
				}
			}
		}
		return rootpages;
	}

	public URL getURL(String endpoint, String accountId, String encodedURL) throws MalformedURLException {
		return new URL(endpoint + accountId + "/" +encodedURL);
	}

	/**
	 * This method Get Resource Resolver instance 
	 * 
	 * @return
	 * @throws LoginException 
	 */
	public ResourceResolver getResourceResolver() throws LoginException {
		Map<String, Object> param = new HashMap<>();
		param.put(ResourceResolverFactory.SUBSERVICE, "schema-app-service");
		return resolverFactory.getServiceResourceResolver(param);
	}

}
