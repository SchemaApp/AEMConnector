package com.schemaapp.core.services.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

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
import com.schemaapp.core.util.Constants;

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
	
	private String graphData;
	
	private String source;

    
	/**
	 * Initialize and read page data.
	 * 
	 * @param pageUrl
	 */
	@Override
	public void init(String pageUrl) {

	    try {
	        ResourceResolver resolver = getResourceResolver();
	        String id = getPath(pageUrl);
	        Resource urlResource = resolver.resolve(id);
	        Resource schemaAppData = urlResource.getChild(Constants.DATA);
	        if (schemaAppData != null) {
	            Node node = schemaAppData.adaptTo(Node.class);
	            if (node != null && node.hasProperty("entity")) {
	                Property entityProperty = node.getProperty("entity");
	                if(entityProperty != null) {
	                    setGraphData(entityProperty.getString());
	                }
	            } 
	            if (node != null && node.hasProperty(Constants.SOURCE_HEADER)) {
	                Property sourceProperty = node.getProperty(Constants.SOURCE_HEADER);
	                if(sourceProperty != null) {
	                    setSource(sourceProperty.getString());
	                }
	            }
	        }
	    } catch (RepositoryException | LoginException e) {
			String errorMessage = "PageJSONDataReaderServiceImpl :: Occured error during reading Schema App Entity Node into the AEM Instance ";
			LOG.error(errorMessage, e);
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

	/**
	 * Prepare and Get Path from the Payload
	 * @param pageUrl
	 * @return
	 */
	private String getPath(String pageUrl) {
		
		URL aURL;
		try {
			aURL = new URL(pageUrl);
			String path = aURL.getPath();
			if (path.indexOf(".") > -1) {
				path = path.substring(0, path.lastIndexOf("."));
			}
			return path;
		} catch (MalformedURLException e) {
			LOG.error(e.getMessage());
		}
		return pageUrl;
	}
	

    public String getGraphData() {
        return graphData;
    }

    public void setGraphData(String graphData) {
        this.graphData = graphData;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
