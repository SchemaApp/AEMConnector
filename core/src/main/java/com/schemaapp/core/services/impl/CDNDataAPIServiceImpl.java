package com.schemaapp.core.services.impl;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaapp.core.services.CDNDataAPIService;
import com.schemaapp.core.services.WebhookHandlerService;
import com.schemaapp.core.util.ConfigurationUtil;
import com.schemaapp.core.util.Constants;

@Component(service = CDNDataAPIService.class, immediate = true)
public class CDNDataAPIServiceImpl implements CDNDataAPIService {

	private static final String CQ_CLOUDSERVICECONFIGS = "cq:cloudserviceconfigs";

	private final Logger LOG = LoggerFactory.getLogger(CDNDataAPIServiceImpl.class);

	@Reference
	private ConfigurationAdmin configurationAdmin;

	@Reference
	private WebhookHandlerService webhookHandlerService;

	@Reference
	transient ResourceResolverFactory resolverFactory;
	
	private static ObjectMapper mapperObject = new ObjectMapper().configure(FAIL_ON_MISSING_CREATOR_PROPERTIES, true);

	@Override
	public void readCDNData() {

		try {
			ResourceResolver resolver = getResourceResolver();
			List<Page> rootpages = getSiteRootPages(resolver);
			for (Page page : rootpages) {
				getSchemaAppCDNData(resolver, page);
			}
		} catch (Exception e) {
			LOG.error("Error occurs while read CDN data, Error :: {}", e.getMessage());
		}
	}

	/**
	 * @param resolver
	 * @param page
	 */
	private void getSchemaAppCDNData(ResourceResolver resolver, Page page) {
		try {
			ValueMap configDetailMap = getConfigNodeValueMap(resolver, page);
			String accountId = getAccountId(configDetailMap);
			String siteURL = configDetailMap != null ?  (String) configDetailMap.get("siteURL") : StringUtils.EMPTY;
			String deploymentMethod = configDetailMap != null ?  (String) configDetailMap.get("deploymentMethod") : StringUtils.EMPTY;
			Iterator<Page> childPages = page.listChildren(new PageFilter(), true);
			while (childPages.hasNext()) {
				processPage(resolver, configDetailMap, accountId, siteURL, deploymentMethod, childPages);
			}
		} catch (Exception e) {
			LOG.error("Error in fetching details from SchemaApp CDN Data API", e);
		}
	}

	private void processPage(ResourceResolver resolver, ValueMap configDetailMap, String accountId, String siteURL,
			String deploymentMethod, Iterator<Page> childPages) {
		try {
			final Page child = childPages.next();
			String endpoint = ConfigurationUtil.getConfiguration(Constants.SCHEMAAPP_DATA_API_ENDPOINT_KEY,
					Constants.API_ENDPOINT_CONFIG_PID,
					configurationAdmin, "");
			LOG.info("CDNDataAPIServiceImpl :: endpoint: {}", endpoint);
			String pagePath = siteURL + child.getPath();
			LOG.info("CDNDataAPIServiceImpl :: pagepath: {}", pagePath);
			String encodedURL = Base64.getUrlEncoder().encodeToString(pagePath.getBytes());
			if (encodedURL != null && encodedURL.contains("=")) {
				encodedURL = encodedURL.replace("=", "");
			}
			URL url = getURL(endpoint, accountId, encodedURL, deploymentMethod);
			HttpURLConnection connection = getHttpURLConnection(url);
			connection.setRequestMethod(HttpConstants.METHOD_GET);
			connection.setConnectTimeout(5 * 1000);
			connection.setReadTimeout(7 * 1000);

			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			StringBuilder content = new StringBuilder();
			while ((inputLine = bufferedReader.readLine()) != null) { content.append(inputLine); }

			if (StringUtils.isNotBlank(content.toString())) {
				Object jsonObject = mapperObject.readValue(content.toString(), Object.class);
				LOG.info(String.format("CDNDataAPIServiceImpl :: Response not blank :: Page :: {}, Response :: {}", pagePath, content.toString()));
				Resource pageResource = child.adaptTo(Resource.class);
				webhookHandlerService.savenReplicate(jsonObject, resolver, resolver.adaptTo(Session.class), pageResource, configDetailMap);
			}

			bufferedReader.close();
			connection.disconnect();
		} catch (Exception e) {
			LOG.error("Error while reading and processing CDN URL", e);
		}
	}

	private String getAccountId(ValueMap configDetailMap) {
		String accountId = configDetailMap != null ?  (String) configDetailMap.get("accountID") : StringUtils.EMPTY;
		if (accountId != null && accountId.lastIndexOf('/') > 0) {
			int index = accountId.lastIndexOf('/');
			accountId = accountId.substring(index, accountId.length());
		}
		return accountId;
	}

	private ValueMap getConfigNodeValueMap(ResourceResolver resolver, Page page) {
		ValueMap valueMap = page.getProperties();
		if (valueMap.containsKey(CQ_CLOUDSERVICECONFIGS)) {
			String[] cloudserviceconfigs = (String[]) valueMap.get(CQ_CLOUDSERVICECONFIGS);
			for (String cloudserviceconfig : cloudserviceconfigs) {
				if (cloudserviceconfig.startsWith("/etc/cloudservices/schemaapp/")) {
					Resource configResource = resolver.getResource(cloudserviceconfig + "/jcr:content");
					if (configResource != null) {
						return configResource.getValueMap();
					}
				}
			}
		}

		return null;
	}

	/**
	 * @param resolver
	 * @return
	 */
	private List<Page> getSiteRootPages(ResourceResolver resolver) {
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

	public URL getURL(String endpoint, String accountId, String encodedURL, String deploymentMethod) throws MalformedURLException {
		if (deploymentMethod != null && deploymentMethod.equals("javaScript")) {
			return new URL(endpoint + accountId + "/__highlighter_js/" +encodedURL);
		} else {
			return new URL(endpoint + accountId + "/" +encodedURL);
		}
		
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

	@Override
	public HttpURLConnection getHttpURLConnection(URL url) throws IOException {
		return (HttpURLConnection) url.openConnection();
	}

}
