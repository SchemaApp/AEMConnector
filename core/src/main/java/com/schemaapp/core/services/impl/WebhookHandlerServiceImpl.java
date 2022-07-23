package com.schemaapp.core.services.impl;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import com.day.cq.search.QueryBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaapp.core.exception.AEMURLNotFoundException;
import com.schemaapp.core.exception.InvalidWebHookJsonDataException;
import com.schemaapp.core.models.WebhookEntity;
import com.schemaapp.core.models.WebhookEntityResult;
import com.schemaapp.core.services.FlushService;
import com.schemaapp.core.services.WebhookHandlerService;
import com.schemaapp.core.util.Constants;
import com.schemaapp.core.util.JsonSanitizer;
import com.schemaapp.core.util.QueryHelper;

@Component(service = WebhookHandlerService.class, immediate = true)
public class WebhookHandlerServiceImpl implements WebhookHandlerService {

	private static ObjectMapper mapper = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

	private static final Logger LOG = LoggerFactory.getLogger(WebhookHandlerServiceImpl.class);

	@Reference
	ResourceResolverFactory resolverFactory;

	@Reference
	private QueryBuilder builder;

	@Reference
	FlushService flushService;

	@Reference
	Replicator replicator;

	/**
	 * @return
	 * @throws LoginException
	 */
	private ResourceResolver getResourceResolver() throws LoginException {
		Map<String, Object> param = new HashMap<>();
		param.put(ResourceResolverFactory.SUBSERVICE, "schema-app-service");
		return resolverFactory.getServiceResourceResolver(param);
	}


	/**
	 * Save Graph Data to AEM Node
	 * 
	 * @param entity
	 * @param pageNode
	 * @throws JsonProcessingException
	 * @throws JSONException
	 * @throws RepositoryException
	 */
	private void saveGraphDatatoNode(Object jsonGraphData, Node pageNode) throws JsonProcessingException, JSONException, RepositoryException {
		
		String graphData = mapper.writeValueAsString(jsonGraphData);
		String wellFormedJson = null;
		if (!StringUtils.isBlank(graphData) && graphData.startsWith("[")) {
			JSONArray obj = new JSONArray(graphData);
			wellFormedJson = JsonSanitizer.sanitize(obj.toString());
		} else {
			JSONObject graphJsonObject = new JSONObject(graphData);
			wellFormedJson = JsonSanitizer.sanitize(graphJsonObject.toString());

		}	
		pageNode.setProperty(Constants.ENTITY, wellFormedJson);
	}

	/**
	 * This method used to create data node.
	 * 
	 * @param node
	 * @return
	 * @throws RepositoryException
	 */
	private Node createDataNode(Node node) throws RepositoryException {
		Node dataNode = null;
		if (!node.hasNode(Constants.DATA)) {
			dataNode = node.addNode(Constants.DATA, JcrConstants.NT_UNSTRUCTURED);
		} else {
			dataNode = node.getNode(Constants.DATA);
		}
		return dataNode;
	}

	@Override
	public WebhookEntityResult updateEntity(WebhookEntity entity) throws AEMURLNotFoundException, InvalidWebHookJsonDataException {

		try {
			ResourceResolver resolver = getResourceResolver();
			Session session = resolver.adaptTo(Session.class);
			Resource urlResource = getPageResource(entity, resolver, session);
			if (urlResource != null) {
				LOG.info("WebhookHandlerServiceImpl > updateEntity > URL > {}", urlResource.getPath());
				savenReplicate(entity.getGraph(), resolver, session, urlResource, null);
			} else {
				String errorMessage = "WebhookHandlerServiceImpl :: Unable to find Content URL in AEM "+entity.getId();
				throw new AEMURLNotFoundException(errorMessage);
			}
		} catch (RepositoryException | LoginException e) {
			String errorMessage = "WebhookHandlerServiceImpl :: Occured error during updating Schema App Entity Node into the AEM Instance ";
			LOG.error(errorMessage, e);
			return WebhookEntityResult.prepareError(errorMessage);
		} catch (JsonProcessingException | JSONException | PersistenceException e) {
			String errorMessage = "WebhookHandlerServiceImpl :: Occured error during parsing JSONL-D graph data ";
			LOG.error(errorMessage, e);
			return WebhookEntityResult.prepareError(errorMessage);
		} catch (ReplicationException e) {
			String errorMessage = "WebhookHandlerServiceImpl :: Occured error during Replicating Schema App data  ";
			LOG.error(errorMessage, e);
			return WebhookEntityResult.prepareError(errorMessage);
		} catch (Exception e) {
			String errorMessage = "WebhookHandlerServiceImpl :: Occured error during processing Schema App data  ";
			LOG.error(errorMessage, e);
			return WebhookEntityResult.prepareError(errorMessage);
		} 
		return WebhookEntityResult.prepareSucessResponse(entity);
	}


	@Override
	public void savenReplicate(Object jsonGraphData, ResourceResolver resolver, Session session, Resource urlResource, ValueMap configDetailMap)
			throws RepositoryException, JsonProcessingException, JSONException, PersistenceException,
			ReplicationException {
		Node pageNode = urlResource.adaptTo(Node.class);
		if (pageNode != null) {
			Node dataNode = createDataNode(pageNode);
			addConfigDetails(configDetailMap, pageNode);
			saveGraphDatatoNode(jsonGraphData, dataNode);
			resolver.commit();
			replicator.replicate(session, ReplicationActionType.ACTIVATE, urlResource.getPath() + "/" +Constants.DATA);
		}
	}


	private void addConfigDetails(ValueMap configDetailMap, Node pageNode) throws RepositoryException {
		if (configDetailMap != null) {
			String accountId = configDetailMap.containsKey("accountID") ? (String) configDetailMap.get("accountID") : StringUtils.EMPTY;
			if (StringUtils.isNotEmpty(accountId)) pageNode.setProperty(Constants.ACCOUNT_ID, accountId);
			String siteURL = configDetailMap.containsKey("siteURL") ?  (String) configDetailMap.get("siteURL") : StringUtils.EMPTY;
			if (StringUtils.isNotEmpty(siteURL)) pageNode.setProperty(Constants.SITEURL, siteURL + pageNode.getPath());
			String deploymentMethod = configDetailMap.containsKey("deploymentMethod") ?  (String) configDetailMap.get("deploymentMethod") : StringUtils.EMPTY;
			if (StringUtils.isNotEmpty(deploymentMethod)) pageNode.setProperty(Constants.DEPLOYMENTMETHOD, deploymentMethod);
		}
	}

	/**
	 * This method used to get Page Resource Object.
	 * 
	 * @param entity
	 * @param resolver
	 * @param session
	 * @return
	 */
	private Resource getPageResource(WebhookEntity entity, ResourceResolver resolver, Session session) {

		String id = getPath(entity);
		Resource urlResource = resolver.resolve(id);
		if (ResourceUtil.isNonExistingResource(urlResource)) {
			try {
				List<Resource> schemaAppConfigresources = findSchemaAppConfigUsingSiteDomain(entity, session);
				for (Resource configResource : schemaAppConfigresources) {

					String configPath = getParentResourcePath(configResource);
					LOG.debug("WebhookHandlerServiceImpl > updateEntity -> Schema App Configuration Path {} ", configPath);
					urlResource = resolveEntityPathfromConfig(resolver, session, id, configPath);
				}
			} catch (MalformedURLException e1) {
				LOG.error(e1.getMessage());
			}
		}
		return urlResource;
	}

	/**
	 * This method used to Find Schema App Config Using Site Domain.
	 * 
	 * @param entity
	 * @param session
	 * @return
	 * @throws MalformedURLException
	 */
	private List<Resource> findSchemaAppConfigUsingSiteDomain(WebhookEntity entity, Session session)
			throws MalformedURLException {

		String siteDomain = getWebsiteDomain(entity);
		LOG.debug("WebhookHandlerServiceImpl > updateEntity -> Website Domain from Entity Payload {} ", siteDomain);

		return QueryHelper.getSchemaAppConfig(siteDomain, builder, session);
	}

	/**
	 * This method used to Resolve Entity Path from Config data.
	 * 
	 * @param resolver
	 * @param session
	 * @param id
	 * @param configPath
	 * @return
	 */
	private Resource resolveEntityPathfromConfig(ResourceResolver resolver, Session session, String id,
			String configPath) {

		Resource urlResource;
		int locationLevel = 3;
		List<Resource> rootpathResources = QueryHelper.getContentRootPath(configPath, builder, session);
		for (Resource rootpathRes : rootpathResources) {

			String contentRootPath= getParentResourcePath(rootpathRes); 
			LOG.debug("WebhookHandlerServiceImpl > updateEntity -> AEM Author Site Content Root Path {} ", contentRootPath);

			contentRootPath = getContentRootPathUsingLocationlevel(locationLevel, contentRootPath);
			urlResource = resolver.resolve(contentRootPath + "/" + id);

			if (!ResourceUtil.isNonExistingResource(urlResource)) {
				LOG.debug("WebhookHandlerServiceImpl > updateEntity -> Entity Path {} ", urlResource.getPath());
				return urlResource;
			}
		}
		return null;
	}

	/**
	 * @param locationLevel
	 * @param contentRootPath
	 * @return
	 */
	private String getContentRootPathUsingLocationlevel(int locationLevel, String contentRootPath) {
		if (contentRootPath !=null && !contentRootPath.isEmpty()) {
			String[] contentPathSplit = contentRootPath.split("\\/");
			if (contentPathSplit.length >= locationLevel) {
				return Arrays.stream(contentPathSplit).limit(locationLevel)
						.collect(Collectors.joining("/"));

			}
		}
		return contentRootPath;
	}

	/**
	 * @param configResource
	 * @return
	 */
	private String getParentResourcePath(Resource configResource) {
		if (configResource != null) {
			Resource parentResource = configResource.getParent();
			if (parentResource != null) {
				return  parentResource.getPath();
			}
		}
		return null;
	}

	private String getWebsiteDomain(WebhookEntity entity) throws MalformedURLException {
		URL aURL = new URL(entity.getId());
		return String.format("%s://%s", aURL.getProtocol(), aURL.getAuthority());
	}

	/**
	 * Prepare and Get Path from the Payload
	 * @param entity
	 * @return
	 */
	private String getPath(WebhookEntity entity) {
		
		URL aURL;
		try {
			aURL = new URL(entity.getId());
			String path = aURL.getPath();
			if (path.indexOf(".") > -1) {
				path = path.substring(0, path.lastIndexOf("."));
			}
			return path;
		} catch (MalformedURLException e) {
			LOG.error(e.getMessage());
		}
		return entity.getId();
	}


	/**
	 * This method used to delete entity 
	 */
	@Override
	public WebhookEntityResult deleteEntity(WebhookEntity entity) throws LoginException, PersistenceException {
		
		ResourceResolver resolver = getResourceResolver();
		Session session = resolver.adaptTo(Session.class);
		Resource urlResource = getPageResource(entity, resolver, session);
		try {
			if (urlResource != null) {
				resolver.delete(urlResource);
				resolver.commit();
			}
		} catch (PersistenceException e) {
			String errorMessage = "WebhookHandlerServiceImpl :: Occured error during deleting Schema App Entity Node into the AEM Instance ";
			LOG.error(errorMessage, e);
			return WebhookEntityResult.prepareError(errorMessage);
		}
		return WebhookEntityResult.prepareSucessResponse(entity);
	}

}
