package com.schema.core.services.impl;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.schema.core.models.AssetFolderDefinition;
import com.schema.core.models.WebhookEntity;
import com.schema.core.models.WebhookEntityResult;
import com.schema.core.services.WebhookHandlerService;

@Component(service = WebhookHandlerService.class, immediate = true)
public class WebhookHandlerServiceImpl implements WebhookHandlerService {

	private static final String ID = "id";

	private static final String CONTENT_USERGENERATED = "/content/usergenerated/content";

	private static final String CONTENT_USERGENERATED_SCHEMAAPP = "/content/usergenerated/content/schemaApp";

	public static final String TYPE = "type";
	public static final String PATH = "path";
	public static final String P_LIMIT = "p.limit";
	public static final String INFINITE = "-1";
	public static final String PROPERTY = "property";
	public static final String PROPERTY_VALUE = "property.value";


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
	public WebhookEntityResult createEntity(WebhookEntity entiry) throws LoginException {
		ResourceResolver resolver = getResourceResolver();
		Resource resource = resolver.getResource(CONTENT_USERGENERATED_SCHEMAAPP);
		if (resource == null || ResourceUtil.isNonExistingResource(resource)) {
			AssetFolderDefinition folderDefinition = getAssestFolderDefinition();
			createAssetFolder(folderDefinition, resolver);
		}

		resource = resolver.getResource(CONTENT_USERGENERATED_SCHEMAAPP);

		if (resource != null) {
			Node node = resource.adaptTo(Node.class);
			try {
				Node dataNode = createDataNode(node);
				String nodeName = JcrUtil.createValidChildName(dataNode, entiry.getId());
				Node pageNode = dataNode.addNode(nodeName, JcrConstants.NT_UNSTRUCTURED);
				pageNode.setProperty("entity", entiry.getGraph().toString());
				pageNode.setProperty(ID, entiry.getId());
				resolver.commit();
			} catch (RepositoryException | PersistenceException e) {
				String errorMessage = "WebhookHandlerServiceImpl :: Occured error during creation Schema App Entity Node into the AEM Instance ";
				LOG.error(errorMessage, e);
				return WebhookEntityResult.prepareError(errorMessage);
			}
		}
		return WebhookEntityResult.fromEntity(entiry);
	}

	private Node createDataNode(Node node) throws RepositoryException {
		Node dataNode = null;
		if (!node.hasNode("data")) {
			dataNode = node.addNode("data", JcrConstants.NT_UNSTRUCTURED);
		} else {
			dataNode = node.getNode("data");
		}
		return dataNode;
	}

	@Override
	public WebhookEntityResult updateEntity(WebhookEntity entiry) throws LoginException {
		ResourceResolver resolver = getResourceResolver();
		session = resolver.adaptTo(Session.class);
		Resource resource = getResultsUsingId(entiry.getId());
		if (resource != null) {
			Node node = resource.adaptTo(Node.class);
			try {
				if (node != null) {
					node.setProperty("entity", entiry.getGraph().toString());
					node.setProperty(ID, entiry.getId());
					resolver.commit();
				} else {
					createEntity(entiry);
				}
			} catch (RepositoryException | PersistenceException e) {
				String errorMessage = "WebhookHandlerServiceImpl :: Occured error during updating Schema App Entity Node into the AEM Instance ";
				LOG.error(errorMessage, e);
				return WebhookEntityResult.prepareError(errorMessage);
			}
		}
		return WebhookEntityResult.fromEntity(entiry);
	}


	@Override
	public WebhookEntityResult deleteEntity(WebhookEntity entiry) throws LoginException, PersistenceException {
		ResourceResolver resolver = getResourceResolver();
		session = resolver.adaptTo(Session.class);
		Resource resource = getResultsUsingId(entiry.getId());
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
		return WebhookEntityResult.fromEntity(entiry);
	}

	private AssetFolderDefinition getAssestFolderDefinition() {
		AssetFolderDefinition folderDefinition = new AssetFolderDefinition();
		folderDefinition.setParentPath(CONTENT_USERGENERATED);
		folderDefinition.setName("schemaApp");
		folderDefinition.setTitle("Schema App");
		folderDefinition.setPath(CONTENT_USERGENERATED_SCHEMAAPP);
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

	private Resource getResultsUsingId(String id) {

		final Map<String, String> map = new HashMap<>();
		map.put(TYPE, JcrConstants.NT_UNSTRUCTURED);
		map.put(PATH, CONTENT_USERGENERATED_SCHEMAAPP);
		map.put(PROPERTY, ID);
		map.put(PROPERTY_VALUE, id);
		map.put(P_LIMIT, INFINITE);
		final Query query = builder.createQuery(PredicateGroup.create(map), session);
		final SearchResult result = query.getResult();
		for(Hit hit : result.getHits()) {
			try {
				final Resource entityResource = hit.getResource();
				if (entityResource!= null && !ResourceUtil.isNonExistingResource(entityResource)) {
					return entityResource;
				}
			}
			catch (final RepositoryException error) {
				LOG.error("WebhookHandlerServiceImpl > getResultsUsingId  ", error);
			}
		}
		return null;
	}

}
