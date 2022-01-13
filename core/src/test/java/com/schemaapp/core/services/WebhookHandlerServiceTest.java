package com.schemaapp.core.services;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.jackrabbit.JcrConstants;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaapp.core.models.WebhookEntity;
import com.schemaapp.core.services.impl.WebhookHandlerServiceImpl;

@ExtendWith({ MockitoExtension.class})
class WebhookHandlerServiceTest {

	private static final String GRAPH_DATA = "[{\"@context\":\"http://schema.org\",\"@type\":\"Product\",\"@id\":\"https://dell.ca/products/monitors/dellultra30\",\"aggregateRating\":{\"@type\":\"AggregateRating\",\"bestRating\":\"100\",\"ratingCount\":\"24\",\"ratingValue\":\"87\"},\"image\":\"dell-30in-lcd.jpg\",\"name\":\"Dell UltraSharp 30\\\" LCD Monitor\",\"offers\":{\"@type\":\"AggregateOffer\",\"highPrice\":\"$1495\",\"lowPrice\":\"$1250\"}}]";

	private static final String JSONL_D = "{\"@context\":{\"@vocab\":\"http://hunchmanifest.com/ontology/Application#\",\"generatedAtTime\":\"http://www.w3.org/ns/prov#generatedAtTime\"},\"@type\":\"EntityCreated\",\"@id\":\"http://localhost:4502/page33\",\"base64encode\":\"aHR0cHM6Ly9kZWxsLmNhL3Byb2R1Y3RzL21vbml0b3JzL2RlbGx1bHRyYTMw\",\"url\":\"https://data.schemaapp.com/ACCOUNTID/aHR0cHM6Ly9kZWxsLmNhL3Byb2R1Y3RzL21vbml0b3JzL2RlbGx1bHRyYTMw\",\"generatedAtTime\":\"2018-02-15T10:20:00Z\",\"@graph\":[{\"@context\":\"http://schema.org\",\"@type\":\"Product\",\"@id\":\"https://dell.ca/products/monitors/dellultra30\",\"aggregateRating\":{\"@type\":\"AggregateRating\",\"bestRating\":\"100\",\"ratingCount\":\"24\",\"ratingValue\":\"87\"},\"image\":\"dell-30in-lcd.jpg\",\"name\":\"Dell UltraSharp 30\\\" LCD Monitor\",\"offers\":{\"@type\":\"AggregateOffer\",\"highPrice\":\"$1495\",\"lowPrice\":\"$1250\"}}]}";

	@Mock
	private ResourceResolver resolver;

	@Mock
	private ResourceResolverFactory resolverFactory;

	@Mock
	private Resource resource;

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

	@InjectMocks
	private final WebhookHandlerServiceImpl webhookHandlerService = new WebhookHandlerServiceImpl();

	private static ObjectMapper MAPPER = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
	
	@Test
	void createEntityTest() throws Exception {

		mockResolver();
		when(resource.getResourceType()).thenReturn("sling:nonexisting");
		when(resolver.getResource(anyString())).thenReturn(resource);
		when(entity.getGraph()).thenReturn(new String(GRAPH_DATA));
		when(entity.getId()).thenReturn(new String("https://dell.ca/products/monitors/dellultra30"));
		when(resource.adaptTo(Node.class)).thenReturn(node);
		when(node.addNode(anyString(), eq(JcrConstants.NT_UNSTRUCTURED))).thenReturn(node);
		
		webhookHandlerService.createEntity(entity);
		verify(resolver, times(1)).commit();
	}
	
	@Test
	void updateEntityTest() throws Exception {

		mockResolver();
		final WebhookEntity mockEntity = MAPPER.readValue(JSONL_D, WebhookEntity.class);
		when(entity.getGraph()).thenReturn(mockEntity.getGraph());
		when(entity.getId()).thenReturn(new String("https://dell.ca/products/monitors/dellultra30"));
		when(resource.adaptTo(Node.class)).thenReturn(node);
		when(resolver.adaptTo(Session.class)).thenReturn(session); //maybe not needed
		when(queryBuilder.createQuery(Mockito.any(PredicateGroup.class), Mockito.any(Session.class))).thenReturn(query);
		when(query.getResult()).thenReturn(searchResult);
		final List<Hit> hits = new ArrayList<>();
		hits.add(hit);
		when(searchResult.getHits()).thenReturn(hits);
		when(hits.get(0).getResource()).thenReturn(resource);
		
		webhookHandlerService.updateEntity(entity);
		verify(resolver, times(1)).commit();
	}
	
	@Test
	void deleteEntityTest() throws Exception {

		mockResolver();
		when(entity.getId()).thenReturn(new String("https://dell.ca/products/monitors/dellultra30"));
		when(resolver.adaptTo(Session.class)).thenReturn(session); //maybe not needed
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
	

