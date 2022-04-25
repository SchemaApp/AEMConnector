package com.schemaapp.core.services.impl;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.search.QueryBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaapp.core.models.AssetFolderDefinition;
import com.schemaapp.core.models.WebhookEntity;
import com.schemaapp.core.models.WebhookEntityResult;
import com.schemaapp.core.services.FlushParentPageJsonService;
import com.schemaapp.core.services.WebhookHandlerService;
import com.schemaapp.core.util.Constants;
import com.schemaapp.core.util.JsonSanitizer;
import com.schemaapp.core.util.QueryHelper;

@Component(service = WebhookHandlerService.class, immediate = true)
public class WebhookHandlerServiceImpl implements WebhookHandlerService {

	private static final String UNABLE_TO_CREATE_ASSET_FOLDER = "Unable to create Asset Folder [ {} -> {} ]";

	private static ObjectMapper MAPPER = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

	private static final Logger LOG = LoggerFactory.getLogger(WebhookHandlerServiceImpl.class);

	@Reference
	transient ResourceResolverFactory resolverFactory;

	@Reference
	private QueryBuilder builder;

	@Reference
	FlushParentPageJsonService flushService;

	/**
	 * @return
	 * @throws LoginException
	 */
	private ResourceResolver getResourceResolver() throws LoginException {
		Map<String, Object> param = new HashMap<>();
		param.put(ResourceResolverFactory.SUBSERVICE, "schema-app-service");
		return resolverFactory.getServiceResourceResolver(param);
	}

	@Override
	public WebhookEntityResult createEntity(WebhookEntity entity) throws LoginException {
		ResourceResolver resolver = getResourceResolver();
		Resource resource = resolver.getResource(Constants.CONTENT_SCHEMAAPP);
		if (resource == null || ResourceUtil.isNonExistingResource(resource)) {
			AssetFolderDefinition folderDefinition = getAssestFolderDefinition();
			createAssetFolder(folderDefinition, resolver);
		}

		resource = resolver.getResource(Constants.CONTENT_SCHEMAAPP);

		if (resource != null) {
			Node node = resource.adaptTo(Node.class);
			try {
				Node dataNode = createDataNode(node);
				String nodeName = JcrUtil.createValidChildName(dataNode, entity.getId());
				Node pageNode = dataNode.addNode(nodeName, JcrConstants.NT_UNSTRUCTURED);
				setGraphDatatoNode(entity, pageNode);
				pageNode.setProperty(Constants.ID, entity.getId());
				resolver.commit();

				String path = getPath(entity);
				flushService.invalidatePageJson(path);
			} catch (RepositoryException | PersistenceException e) {
				String errorMessage = "WebhookHandlerServiceImpl :: Occured error during creation Schema App Entity Node into the AEM Instance ";
				LOG.error(errorMessage, e);
				return WebhookEntityResult.prepareError(errorMessage);
			} catch (JsonProcessingException | JSONException e) {
				String errorMessage = "WebhookHandlerServiceImpl :: Occured error during parsing JSONL-D graph data ";
				LOG.error(errorMessage, e);
				return WebhookEntityResult.prepareError(errorMessage);
			} 
		}
		return WebhookEntityResult.prepareSucessResponse(entity);
	}

	private void setGraphDatatoNode(WebhookEntity entity, Node pageNode) throws JsonProcessingException, JSONException, RepositoryException {
		String graphData = MAPPER.writeValueAsString(entity.getGraph());
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
	public WebhookEntityResult updateEntity(WebhookEntity entity) throws LoginException {
		ResourceResolver resolver = getResourceResolver();
		Session session = resolver.adaptTo(Session.class);
		Resource resource = QueryHelper.getResultsUsingId(entity.getId(), builder, session);
		try {
			if (resource != null) {
				Node node = resource.adaptTo(Node.class);
				if (node != null) {
					setGraphDatatoNode(entity, node);
					node.setProperty(Constants.ID, entity.getId());
					resolver.commit();
					String path = getPath(entity);
					flushService.invalidatePageJson(path);
				} 
			} else {
				return createEntity(entity);
			}
		} catch (RepositoryException | PersistenceException e) {
			String errorMessage = "WebhookHandlerServiceImpl :: Occured error during updating Schema App Entity Node into the AEM Instance ";
			LOG.error(errorMessage, e);
			return WebhookEntityResult.prepareError(errorMessage);
		} catch (JsonProcessingException | JSONException e) {
			String errorMessage = "WebhookHandlerServiceImpl :: Occured error during parsing JSONL-D graph data ";
			LOG.error(errorMessage, e);
			return WebhookEntityResult.prepareError(errorMessage);
		} 
		return WebhookEntityResult.prepareSucessResponse(entity);
	}

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
		}
		return entity.getId();
	}


	@Override
	public WebhookEntityResult deleteEntity(WebhookEntity entity) throws LoginException, PersistenceException {
		ResourceResolver resolver = getResourceResolver();
		Session session = resolver.adaptTo(Session.class);
		Resource resource = QueryHelper.getResultsUsingId(entity.getId(), builder, session);
		try {
			if (resource != null) {
				resolver.delete(resource);
				resolver.commit();

				String path = getPath(entity);
				flushService.invalidatePageJson(path);
			}

		} catch (PersistenceException e) {
			String errorMessage = "WebhookHandlerServiceImpl :: Occured error during deleting Schema App Entity Node into the AEM Instance ";
			LOG.error(errorMessage, e);
			return WebhookEntityResult.prepareError(errorMessage);
		}
		return WebhookEntityResult.prepareSucessResponse(entity);
	}

	private AssetFolderDefinition getAssestFolderDefinition() {
		AssetFolderDefinition folderDefinition = new AssetFolderDefinition();
		folderDefinition.setParentPath(Constants.CONTENT);
		folderDefinition.setName("schemaApp-data");
		folderDefinition.setTitle("Schema App");
		folderDefinition.setPath(Constants.CONTENT_SCHEMAAPP);
		folderDefinition.setNodeType("cq:Page");
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
			} catch (PersistenceException | RepositoryException e) {
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
	private void setTitles(final Resource folder, final AssetFolderDefinition assetFolderDefinition) throws RepositoryException {

		Resource jcrContent = folder.getChild(JcrConstants.JCR_CONTENT);
		if (jcrContent == null) {
			LOG.error("Asset Folder [ {} ] does not have a jcr:content child", assetFolderDefinition.getPath());
			return;
		}

		final ModifiableValueMap properties = jcrContent.adaptTo(ModifiableValueMap.class);

		if (properties != null) {
			if (!StringUtils.equals(assetFolderDefinition.getTitle(), properties.get(com.day.cq.commons.jcr.JcrConstants.JCR_TITLE, String.class))) {
				properties.put(com.day.cq.commons.jcr.JcrConstants.JCR_TITLE, assetFolderDefinition.getTitle());
			}
		}
	}

}
