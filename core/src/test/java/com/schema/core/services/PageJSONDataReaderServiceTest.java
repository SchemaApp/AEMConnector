package com.schema.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
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

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.schema.core.services.impl.PageJSONDataReaderServiceImpl;

@ExtendWith({ MockitoExtension.class})
class PageJSONDataReaderServiceTest {

	private static final String GRAPH_DATA = "[{\"@context\":\"http://schema.org\",\"@type\":\"Product\",\"@id\":\"https://dell.ca/products/monitors/dellultra30\",\"aggregateRating\":{\"@type\":\"AggregateRating\",\"bestRating\":\"100\",\"ratingCount\":\"24\",\"ratingValue\":\"87\"},\"image\":\"dell-30in-lcd.jpg\",\"name\":\"Dell UltraSharp 30\\\" LCD Monitor\",\"offers\":{\"@type\":\"AggregateOffer\",\"highPrice\":\"$1495\",\"lowPrice\":\"$1250\"}}]";

	@Mock
	private ResourceResolver resolver;

	@Mock
	private ResourceResolverFactory resolverFactory;

	@Mock
	private Resource resource;

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
	private Property entityProperty;

	@InjectMocks
	private final PageJSONDataReaderServiceImpl pageJSONDataReaderService = new PageJSONDataReaderServiceImpl();

	@Test
	void pageDataTest() throws Exception {

		mockResolver();
		when(resource.adaptTo(Node.class)).thenReturn(node);
		when(resolver.adaptTo(Session.class)).thenReturn(session); 
		when(queryBuilder.createQuery(Mockito.any(PredicateGroup.class), Mockito.any(Session.class))).thenReturn(query);
		when(query.getResult()).thenReturn(searchResult);
		final List<Hit> hits = new ArrayList<>();
		hits.add(hit);
		when(searchResult.getHits()).thenReturn(hits);
		when(hits.get(0).getResource()).thenReturn(resource);
		when(node.hasProperty("entity")).thenReturn(true);
		when(node.getProperty("entity")).thenReturn(entityProperty);
		when(entityProperty.getString()).thenReturn(GRAPH_DATA);

		assertEquals(pageJSONDataReaderService.getPageData("www.demosite.com/test.html"), GRAPH_DATA);
	}

	private void mockResolver() throws NoSuchFieldException, LoginException {
		Map<String, Object> param = new HashMap<>();
		param.put(ResourceResolverFactory.SUBSERVICE, "schema-app-service");
		FieldSetter.setField(pageJSONDataReaderService, pageJSONDataReaderService.getClass().getDeclaredField("resolverFactory"), resolverFactory);
		when(resolverFactory.getServiceResourceResolver(param)).thenReturn(resolver);
	}
}
