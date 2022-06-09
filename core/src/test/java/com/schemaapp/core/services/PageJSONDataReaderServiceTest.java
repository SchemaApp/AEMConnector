package com.schemaapp.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.HashMap;
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
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.jupiter.MockitoExtension;

import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.schemaapp.core.services.impl.PageJSONDataReaderServiceImpl;
import com.schemaapp.core.util.Constants;

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
		when(resolver.resolve(anyString())).thenReturn(resource);
		when(resource.getChild(Constants.DATA)).thenReturn(resource);
		when(resource.adaptTo(Node.class)).thenReturn(node);
		when(node.hasProperty("entity")).thenReturn(true);
		when(node.getProperty("entity")).thenReturn(entityProperty);
		when(entityProperty.getString()).thenReturn(GRAPH_DATA);

		assertEquals(GRAPH_DATA, pageJSONDataReaderService.getPageData("www.demosite.com/test.html"));
	}

	private void mockResolver() throws NoSuchFieldException, LoginException {
		Map<String, Object> param = new HashMap<>();
		param.put(ResourceResolverFactory.SUBSERVICE, "schema-app-service");
		FieldSetter.setField(pageJSONDataReaderService, pageJSONDataReaderService.getClass().getDeclaredField("resolverFactory"), resolverFactory);
		when(resolverFactory.getServiceResourceResolver(param)).thenReturn(resolver);
	}
}
