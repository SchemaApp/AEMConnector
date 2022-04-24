package com.schemaapp.core.services.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.Page;
import com.schemaapp.core.services.FlushParentPageJsonService;
import com.schemaapp.core.util.Constants;
import com.schemaapp.core.util.ReplicationConstants;

@Component(service = FlushParentPageJsonService.class)
public class FlushParentPageJsonImpl implements FlushParentPageJsonService {
	public static final Logger LOGGER = LoggerFactory.getLogger(FlushParentPageJsonImpl.class);
	private int locationLevel = 5;

	@Reference
	transient ResourceResolverFactory resolverFactory;

	/**
	 * Override invalidatePageJson method of FlushParentPageJsonService
	 *
	 * @param String the pageUrl
	 * @throws org.apache.sling.api.resource.LoginException 
	 */
	@Override
	public void invalidatePageJson(String pageUrl) {
		try {
			LOGGER.debug("invalidatePageJson start {}", pageUrl);
			ResourceResolver resourceResolver = getResourceResolver();
			Resource publishReplicationAgentResource = resourceResolver.getResource(ReplicationConstants.REPLICATION_AGENT_PATH_PUBLISH);
			if (publishReplicationAgentResource != null) {
				getResourceNode(publishReplicationAgentResource, pageUrl);
			} else {
				LOGGER.debug("publishReplicationAgentResource is null");
			}
		} catch (LoginException e) {
			LOGGER.error("login exception:", e);
		}
	}
	/**
	 * Method to get content resource node of the publish replication agent.
	 *
	 * @param publishReplicationAgentResource , Resource Resolver object to get the resource properties
	 * @param pageUrl                                                 , Resource page which is being
	 *                                        activated/deactivated
	 * @return void
	 */
	private void getResourceNode(Resource publishReplicationAgentResource, String pageUrl) {
		Page publishReplicationAgentPage = publishReplicationAgentResource.adaptTo(Page.class);
		try {
			if (publishReplicationAgentPage != null) {
				Iterator<Page> publishAgentPageIterator = publishReplicationAgentPage.listChildren();
				while (publishAgentPageIterator.hasNext()) {
					Page publishReplicationAgentsChildPage = publishAgentPageIterator.next();
					Resource contentResource = publishReplicationAgentsChildPage.getContentResource();
					Node contentResourceNode = contentResource.adaptTo(Node.class);
					if (contentResourceNode != null && contentResourceNode.hasProperty("transportUri")) {
						String dispatcherCacheUrl = contentResourceNode.getProperty("transportUri").getValue().getString();
						LOGGER.debug("getResourceNode :: dispatcherCacheUrl is {}", dispatcherCacheUrl);
						getTransportUri(pageUrl, dispatcherCacheUrl);
					}
				}
			}
		} catch (RepositoryException e) {
			LOGGER.error("getResourceNode :: RepositoryException is", e);
		}
	}
	/**
	 * Method to get Transport Uri from the publish replication agent jcr:content
	 * property.
	 *
	 * @param pageUrl                        , Resource page which is being activated/deactivated
	 * @param dispatcherCacheUrl  ,  The dispatcher cache url
	 * @return void
	 */
	public void getTransportUri(String pageUrl, String dispatcherCacheUrl) {
		try {
			Resource pageUrlResource = getResourceResolver().getResource(pageUrl);
			LOGGER.debug("getTransportUri :: Resource path:{}", pageUrl);
			if (pageUrlResource != null && pageUrlResource.adaptTo(Page.class) != null) {
				invalidateDispatcherCacheURL(pageUrl, dispatcherCacheUrl);
			} else {
				LOGGER.error("getTransportUri :: Resource is either NULL or not a content page for resource path: {}",
						pageUrl);
			}
		} catch (LoginException e) {
			LOGGER.error("getTransportUri :: LoginException is", e);
		}
	}
	/**
	 * Method to invalidation Dispatcher cache of page.
	 *
	 * @param pageUrl                       , Resource page which is being activated/deactivated
	 * @param dispatcherCacheUrl , Dispatcher cache url page retrieved from publish replication agent
	 * @return Boolean
	 */
	private void invalidateDispatcherCacheURL(String pageUrl, String dispatcherCacheUrl) {
		String[] contentPathSplit = pageUrl.split("\\/");
		if (contentPathSplit.length >= locationLevel) {
			String pagePath = Arrays.stream(contentPathSplit).limit(locationLevel)
					.collect(Collectors.joining(Constants.SLASH));
			LOGGER.debug("invalidateDispatcherCacheURL :: Parent Page Path {}", pagePath);
			HttpGet request = new HttpGet(dispatcherCacheUrl);
			try {
				HttpClientBuilder builder = HttpClientBuilder.create();
				RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(10000)
						.setSocketTimeout(10000).build();
				builder.setDefaultRequestConfig(requestConfig);
				HttpClient client = builder.build();
				request.addHeader(ReplicationConstants.CQ_ACTION_HEADER, ReplicationConstants.ACTIVATE);
				request.addHeader(ReplicationConstants.CQ_HANDLE_HEADER, pagePath);
				request.addHeader(ReplicationConstants.CQ_PATH, pagePath);
				HttpResponse response = client.execute(request);
				int statusCode = response.getStatusLine().getStatusCode();
				LOGGER.debug("invalidateDispatcherCacheURL :: status code is {}", statusCode);
				if (statusCode != HttpStatus.SC_OK) {
					LOGGER.debug("Dispatcher Cache Invalidator returned status-code:{} with summary: {}",
							statusCode, response.getStatusLine());
				}
				LOGGER.error("invalidateDispatcherCacheURL :: Invalidating {}", pagePath);
			} catch (ClientProtocolException e) {
				LOGGER.error("Dispatcher Cache Invalidator ClientProtocolException: ", e);
			} catch (IOException e) {
				LOGGER.error("Dispatcher Cache Invalidator IOException:", e);
			} finally {
				request.releaseConnection();
			}
		}
	}

	/**
	 * @return
	 * @throws LoginException
	 * @throws org.apache.sling.api.resource.LoginException 
	 */
	private ResourceResolver getResourceResolver() throws LoginException {
		Map<String, Object> param = new HashMap<>();
		param.put(ResourceResolverFactory.SUBSERVICE, "schema-app-service");
		return resolverFactory.getServiceResourceResolver(param);
	}
}