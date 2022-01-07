package com.schemaapp.core.services.impl;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.search.QueryBuilder;
import com.schemaapp.core.services.PageJSONDataReaderService;
import com.schemaapp.core.util.QueryHelper;

/**
 * The <code>WebhookHandlerService</code> class used to prepare page script data.
 * 
 * @author nikhil
 *
 */
@Component(service = PageJSONDataReaderService.class, immediate = true)
public class PageJSONDataReaderServiceImpl implements PageJSONDataReaderService {

	private static final Logger LOG = LoggerFactory.getLogger(PageJSONDataReaderServiceImpl.class);

	@Reference
	transient ResourceResolverFactory resolverFactory;

	@Reference
	private QueryBuilder builder;

	public String getPageData(String pageUrl) {

		try {
			ResourceResolver resolver = getResourceResolver();
			Session session = resolver.adaptTo(Session.class);
			Resource resource = QueryHelper.getResultsUsingId(pageUrl, builder, session);
			if (resource != null) {
				Node node = resource.adaptTo(Node.class);

				if (node != null && node.hasProperty("entity")) {
					Property entityProperty = node.getProperty("entity");
					if(entityProperty != null) {
						return entityProperty.getString();
					}
				} 
			}
		} catch (RepositoryException | LoginException e) {
			String errorMessage = "PageJSONDataReaderServiceImpl :: Occured error during reading Schema App Entity Node into the AEM Instance ";
			LOG.error(errorMessage, e);
		}
		return null;
	}
	
	/**
	 * This method Get Resource Resolver instance 
	 * 
	 * @return
	 * @throws LoginException
	 */
	private ResourceResolver getResourceResolver() throws LoginException {
		Map<String, Object> param = new HashMap<>();
		param.put(ResourceResolverFactory.SUBSERVICE, "schema-app-service");
		return resolverFactory.getServiceResourceResolver(param);
	}


}
