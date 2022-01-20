package com.schemaapp.core.services.impl;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

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
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.json.JSONArray;
import org.json.JSONException;
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
import com.schemaapp.core.services.WebhookHandlerService;
import com.schemaapp.core.util.Constants;
import com.schemaapp.core.util.JsonSanitizer;
import com.schemaapp.core.util.QueryHelper;

@Component(service = WebhookHandlerService.class, immediate = true)
public class WebhookHandlerServiceImpl implements WebhookHandlerService {

	private static ObjectMapper MAPPER = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

	private static final Logger LOG = LoggerFactory.getLogger(WebhookHandlerServiceImpl.class);

	@Reference
	transient ResourceResolverFactory resolverFactory;

	@Reference
	private QueryBuilder builder;

	private Session session;

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
				setGraphDatatoNode(entity, pageNode);
				pageNode.setProperty(Constants.ID, entity.getId());
				resolver.commit();
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
		JSONArray obj = new JSONArray(graphData);
		String wellFormedJson = JsonSanitizer.sanitize(obj.toString());
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
		session = resolver.adaptTo(Session.class);
		Resource resource = QueryHelper.getResultsUsingId(entity.getId(), builder, session);
		if (resource != null) {
			Node node = resource.adaptTo(Node.class);
			try {
				if (node != null) {
					setGraphDatatoNode(entity, node);
					node.setProperty(Constants.ID, entity.getId());
					resolver.commit();
				} else {
					createEntity(entity);
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
		}
		return WebhookEntityResult.prepareSucessResponse(entity);
	}


	@Override
	public WebhookEntityResult deleteEntity(WebhookEntity entiry) throws LoginException, PersistenceException {
		ResourceResolver resolver = getResourceResolver();
		session = resolver.adaptTo(Session.class);
		Resource resource = QueryHelper.getResultsUsingId(entiry.getId(), builder, session);
		try {
			if (resource != null) {
				resolver.delete(resource);
				resolver.commit();
			}
		} catch (PersistenceException e) {
			String errorMessage = "WebhookHandlerServiceImpl :: Occured error during deleting Schema App Entity Node into the AEM Instance ";
			LOG.error(errorMessage, e);
			return WebhookEntityResult.prepareError(errorMessage);
		}
		return WebhookEntityResult.prepareSucessResponse(entiry);
	}

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
		try {
			if (folder == null) {
				final Map<String, Object> folderProperties = new HashMap<>();
				folderProperties.put(JcrConstants.JCR_PRIMARYTYPE, assetFolderDefinition.getNodeType());
				folder = resourceResolver.create(resourceResolver.getResource(assetFolderDefinition.getParentPath()),
						assetFolderDefinition.getName(),
						folderProperties);
			} 

			final Resource jcrContent = folder.getChild(JcrConstants.JCR_CONTENT);

			if (jcrContent == null) {
				final Map<String, Object> jcrContentProperties = new HashMap<>();
				jcrContentProperties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
				resourceResolver.create(folder, JcrConstants.JCR_CONTENT, jcrContentProperties);
			}

			setTitles(folder, assetFolderDefinition);
			resourceResolver.commit();
			LOG.debug("Created Asset Folder [ {} -> {} ]", assetFolderDefinition.getPath(), assetFolderDefinition.getTitle());
		} catch (Exception e) {
			LOG.error("Unable to create Asset Folder [ {} -> {} ]", new String[]{assetFolderDefinition.getPath(), assetFolderDefinition.getTitle()}, e);
		}
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

		if (!StringUtils.equals(assetFolderDefinition.getTitle(), properties.get(com.day.cq.commons.jcr.JcrConstants.JCR_TITLE, String.class))) {
			properties.put(com.day.cq.commons.jcr.JcrConstants.JCR_TITLE, assetFolderDefinition.getTitle());
		}
	}

}
