package com.schemaapp.core.util;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;

/**
 * The <code> QueryHelper </code> class is utilities for AEM Query Builder.
 * 
 * @author nikhil
 *
 */
public class QueryHelper {

	private static final Logger LOG = LoggerFactory.getLogger(QueryHelper.class);

	/**
	 * This method is used to find Entity Resource from repository using ID.
	 *   
	 * @param id
	 * @param builder
	 * @param session
	 * @return
	 */
	public static Resource getResultsUsingId(String id, QueryBuilder builder, Session session) {

		final Map<String, String> map = new HashMap<>();
		map.put(Constants.TYPE, JcrConstants.NT_UNSTRUCTURED);
		map.put(Constants.PATH, Constants.CONTENT_USERGENERATED_SCHEMAAPP);
		map.put(Constants.PROPERTY, Constants.ID);
		map.put(Constants.PROPERTY_VALUE, id);
		map.put(Constants.P_LIMIT, Constants.INFINITE);
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
				LOG.error("QueryHelper > getResultsUsingId  ", error);
			}
		}
		return null;
	}
	
	private QueryHelper() {}
}
