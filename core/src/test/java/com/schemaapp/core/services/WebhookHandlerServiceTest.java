package com.schemaapp.core.services;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.jupiter.MockitoExtension;

import com.day.cq.replication.Replicator;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.schemaapp.core.models.WebhookEntity;
import com.schemaapp.core.services.impl.WebhookHandlerServiceImpl;

@ExtendWith({ MockitoExtension.class})
class WebhookHandlerServiceTest {

	@Mock
	private ResourceResolver resolver;

	@Mock
	private ResourceResolverFactory resolverFactory;

	@Mock
	private Resource resource, entityResource;

	@Mock
	private WebhookEntity entity;
	
	@Mock
	private QueryBuilder queryBuilder;
	
	@Mock
	private Query query;
	
	@Mock
	private SearchResult searchResult;
	
	@Mock
	private Session session;
	
	@Mock
	private Hit hit;
	
	@Mock
	private Node node;
	
	@Mock
	private Replicator replicator;
	
	@InjectMocks
	private final WebhookHandlerServiceImpl webhookHandlerService = new WebhookHandlerServiceImpl();

	@Test
	void deleteEntityTest() throws Exception {

		mockResolver();
		when(entity.getId()).thenReturn(new String("https://dell.ca/products/monitors/dellultra30"));
		when(resolver.adaptTo(Session.class)).thenReturn(session); 
		when(resolver.resolve(anyString())).thenReturn(resource);
		when(resource.getResourceType()).thenReturn(Resource.RESOURCE_TYPE_NON_EXISTING);
		when(queryBuilder.createQuery(Mockito.any(PredicateGroup.class), Mockito.any(Session.class))).thenReturn(query);
		when(query.getResult()).thenReturn(searchResult);
		final List<Hit> hits = new ArrayList<>();
		hits.add(hit);
		when(searchResult.getHits()).thenReturn(hits);
		when(hits.get(0).getResource()).thenReturn(resource);

		webhookHandlerService.deleteEntity(entity);
		verify(resolver, times(1)).delete(resource);
		verify(resolver, times(1)).commit();
	}

	private void mockResolver() throws NoSuchFieldException, LoginException {
		Map<String, Object> param = new HashMap<>();
		param.put(ResourceResolverFactory.SUBSERVICE, "schema-app-service");
		FieldSetter.setField(webhookHandlerService, webhookHandlerService.getClass().getDeclaredField("resolverFactory"), resolverFactory);
		when(resolverFactory.getServiceResourceResolver(param)).thenReturn(resolver);
	}
}
	

