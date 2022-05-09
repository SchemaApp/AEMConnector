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

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import com.day.cq.search.QueryBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaapp.core.exception.AEMURLNotFoundException;
import com.schemaapp.core.exception.InvalidWebHookJsonDataException;
import com.schemaapp.core.models.AssetFolderDefinition;
import com.schemaapp.core.models.WebhookEntity;
import com.schemaapp.core.models.WebhookEntityResult;
import com.schemaapp.core.services.FlushService;
import com.schemaapp.core.services.WebhookHandlerService;
import com.schemaapp.core.util.Constants;
import com.schemaapp.core.util.JsonSanitizer;
import com.schemaapp.core.util.QueryHelper;

@Component(service = WebhookHandlerService.class, immediate = true)
public class WebhookHandlerServiceImpl implements WebhookHandlerService {

	private static final String UNABLE_TO_CREATE_ASSET_FOLDER = "Unable to create Asset Folder [ {} -> {} ]";

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
	 * Save Entity Data to AEM's User Generated Content
	 * 
	 * @param entity
	 * @return
	 * @throws LoginException
	 */
	private WebhookEntityResult saveEntityDatatoUserGenerated(WebhookEntity entity) throws LoginException {

		ResourceResolver resolver = getResourceResolver();
		Session session = resolver.adaptTo(Session.class);
		Resource resource = resolver.getResource(Constants.CONTENT_USERGENERATED_SCHEMAAPP);
		if (resource == null || ResourceUtil.isNonExistingResource(resource)) {
			AssetFolderDefinition folderDefinition = getAssestFolderDefinition();
			createAssetFolder(folderDefinition, resolver);
		}
		resource = resolver.getResource(Constants.CONTENT_USERGENERATED_SCHEMAAPP);

		if (resource != null) {
			Node node = resource.adaptTo(Node.class);
			try {
				Node dataNode = createDataNode(node);
				String nodeName = JcrUtil.createValidChildName(dataNode, entity.getId());
				Node pageNode = dataNode.addNode(nodeName, JcrConstants.NT_UNSTRUCTURED);
				saveGraphDatatoNode(entity, pageNode);
				pageNode.setProperty(Constants.ID, entity.getId());
				resolver.commit();
				replicator.replicate(session, ReplicationActionType.ACTIVATE, pageNode.getPath());
			} catch (RepositoryException | PersistenceException e) {
				String errorMessage = "WebhookHandlerServiceImpl :: Occured error during creation Schema App Entity Node into the AEM Instance ";
				LOG.error(errorMessage, e);
				return WebhookEntityResult.prepareError(errorMessage);
			} catch (JsonProcessingException | JSONException e) {
				String errorMessage = "WebhookHandlerServiceImpl :: Occured error during parsing JSONL-D graph data ";
				LOG.error(errorMessage, e);
				return WebhookEntityResult.prepareError(errorMessage);
			} catch (ReplicationException e) {
				String errorMessage = "WebhookHandlerServiceImpl :: Occured error during Replicating Schema App data ";
				LOG.error(errorMessage, e);
				return WebhookEntityResult.prepareError(errorMessage);
			} 
		}
		return WebhookEntityResult.prepareSucessResponse(entity);
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
	private void saveGraphDatatoNode(WebhookEntity entity, Node pageNode) throws JsonProcessingException, JSONException, RepositoryException {
		
		String graphData = mapper.writeValueAsString(entity.getGraph());
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
				Node pageNode = urlResource.adaptTo(Node.class);
				Node dataNode = createDataNode(pageNode);
				saveGraphDatatoNode(entity, dataNode);
				resolver.commit();
				replicator.replicate(session, ReplicationActionType.ACTIVATE, urlResource.getPath() + "/" +Constants.DATA);
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
			throw new InvalidWebHookJsonDataException();
		}catch (ReplicationException e) {
			String errorMessage = "WebhookHandlerServiceImpl :: Occured error during Replicating Schema App data  ";
			LOG.error(errorMessage, e);
			return WebhookEntityResult.prepareError(errorMessage);
		} 
		return WebhookEntityResult.prepareSucessResponse(entity);
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
		if (urlResource == null || ResourceUtil.isNonExistingResource(urlResource)) {
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

	/**
	 * @return
	 */
	private AssetFolderDefinition getAssestFolderDefinition() {
		
		AssetFolderDefinition folderDefinition = new AssetFolderDefinition();
		folderDefinition.setParentPath(Constants.CONTENT_USERGENERATED);
		folderDefinition.setName("schemaApp");
		folderDefinition.setTitle("Schema App");
		folderDefinition.setPath(Constants.CONTENT_USERGENERATED_SCHEMAAPP);
		folderDefinition.setNodeType(JcrResourceConstants.NT_SLING_FOLDER);
		return folderDefinition;
	}

	/**
	 * Creates an Asset Folder.
	 *
	 * @param assetFolderDefinition the asset folder definition to create.
	 * @param resourceResolver the resource resolver object used to create the asset folder.
	 * @throws PersistenceException
	 * @throws RepositoryException
	 */
	private void createAssetFolder(final AssetFolderDefinition assetFolderDefinition, final ResourceResolver resourceResolver) {

		Resource folder = resourceResolver.getResource(assetFolderDefinition.getPath());
		if (folder == null) {
			final Map<String, Object> folderProperties = new HashMap<>();
			folderProperties.put(JcrConstants.JCR_PRIMARYTYPE, assetFolderDefinition.getNodeType());
			try {
				folder = resourceResolver.create(resourceResolver.getResource(assetFolderDefinition.getParentPath()),
						assetFolderDefinition.getName(),
						folderProperties);
			} catch (PersistenceException e) {
				LOG.error(UNABLE_TO_CREATE_ASSET_FOLDER, new String[]{assetFolderDefinition.getPath(), assetFolderDefinition.getTitle()}, e);
			}
		} 

		if (folder != null) {
			final Resource jcrContent = folder.getChild(JcrConstants.JCR_CONTENT);

			if (jcrContent == null) {
				final Map<String, Object> jcrContentProperties = new HashMap<>();
				jcrContentProperties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
				try {
					resourceResolver.create(folder, JcrConstants.JCR_CONTENT, jcrContentProperties);
				} catch (PersistenceException e) {
					LOG.error(UNABLE_TO_CREATE_ASSET_FOLDER, new String[]{assetFolderDefinition.getPath(), assetFolderDefinition.getTitle()}, e);
				}
			}

			try {
				setTitles(folder, assetFolderDefinition);
				resourceResolver.commit();
			} catch (PersistenceException e) {
				LOG.error(UNABLE_TO_CREATE_ASSET_FOLDER, new String[]{assetFolderDefinition.getPath(), assetFolderDefinition.getTitle()}, e);
			}
		}
		LOG.debug("Created Asset Folder [ {} -> {} ]", assetFolderDefinition.getPath(), assetFolderDefinition.getTitle());
	}

	/**
	 * @param folder
	 * @param assetFolderDefinition
	 * @throws RepositoryException
	 */
	private void setTitles(final Resource folder, final AssetFolderDefinition assetFolderDefinition) {

		Resource jcrContent = folder.getChild(JcrConstants.JCR_CONTENT);
		if (jcrContent == null) {
			LOG.error("Asset Folder [ {} ] does not have a jcr:content child", assetFolderDefinition.getPath());
			return;
		}

		final ModifiableValueMap properties = jcrContent.adaptTo(ModifiableValueMap.class);

		if (properties != null && !StringUtils.equals(assetFolderDefinition.getTitle(), properties.get(com.day.cq.commons.jcr.JcrConstants.JCR_TITLE, String.class))) {
			properties.put(com.day.cq.commons.jcr.JcrConstants.JCR_TITLE, assetFolderDefinition.getTitle());
		}
	}

}
