package com.schemaapp.core.servlet;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaapp.core.models.WebhookEntity;
import com.schemaapp.core.services.WebhookHandlerService;
import com.schemaapp.core.servlets.WebhooksHandlerServlet;

@ExtendWith({ MockitoExtension.class})
class WebhooksHandlerServletTest {

	private static final String JSON_LD = "{\"@context\":{\"@vocab\":\"http://hunchmanifest.com/ontology/Application#\",\"generatedAtTime\":\"http://www.w3.org/ns/prov#generatedAtTime\"},\"@type\":\"EntityCreated\",\"@id\":\"http://localhost:4502/page33\",\"base64encode\":\"aHR0cHM6Ly9kZWxsLmNhL3Byb2R1Y3RzL21vbml0b3JzL2RlbGx1bHRyYTMw\",\"url\":\"https://data.schemaapp.com/ACCOUNTID/aHR0cHM6Ly9kZWxsLmNhL3Byb2R1Y3RzL21vbml0b3JzL2RlbGx1bHRyYTMw\",\"generatedAtTime\":\"2022-02-02T11:57:49.185Z\",\"@graph\":[{\"@context\":\"http://schema.org\",\"@type\":\"Product\",\"@id\":\"https://dell.ca/products/monitors/dellultra30\",\"aggregateRating\":{\"@type\":\"AggregateRating\",\"bestRating\":\"100\",\"ratingCount\":\"24\",\"ratingValue\":\"87\"},\"image\":\"dell-30in-lcd.jpg\",\"name\":\"Dell UltraSharp 30\\\" LCD Monitor\",\"offers\":{\"@type\":\"AggregateOffer\",\"highPrice\":\"$1495\",\"lowPrice\":\"$1250\"}}]}";

	private static final String JSON_LD_ENTITYUPDATED = "{\"@context\":{\"@vocab\":\"http://hunchmanifest.com/ontology/Application#\",\"generatedAtTime\":\"http://www.w3.org/ns/prov#generatedAtTime\"},\"@type\":\"EntityUpdated\",\"@id\":\"http://localhost:4502/page33\",\"base64encode\":\"aHR0cHM6Ly9kZWxsLmNhL3Byb2R1Y3RzL21vbml0b3JzL2RlbGx1bHRyYTMw\",\"url\":\"https://data.schemaapp.com/ACCOUNTID/aHR0cHM6Ly9kZWxsLmNhL3Byb2R1Y3RzL21vbml0b3JzL2RlbGx1bHRyYTMw\",\"generatedAtTime\":\"2022-02-02T11:57:49.185Z\",\"@graph\":[{\"@context\":\"http://schema.org\",\"@type\":\"Product\",\"@id\":\"https://dell.ca/products/monitors/dellultra30\",\"aggregateRating\":{\"@type\":\"AggregateRating\",\"bestRating\":\"100\",\"ratingCount\":\"24\",\"ratingValue\":\"87\"},\"image\":\"dell-30in-lcd.jpg\",\"name\":\"Dell UltraSharp 30\\\" LCD Monitor\",\"offers\":{\"@type\":\"AggregateOffer\",\"highPrice\":\"$1495\",\"lowPrice\":\"$1250\"}}]}";

	private static final String JSON_LD_ENTITYDELETED = "{\"@context\":{\"@vocab\":\"http://hunchmanifest.com/ontology/Application#\",\"generatedAtTime\":\"http://www.w3.org/ns/prov#generatedAtTime\"},\"@type\":\"EntityDeleted\",\"@id\":\"http://localhost:4502/page33\",\"base64encode\":\"aHR0cHM6Ly9kZWxsLmNhL3Byb2R1Y3RzL21vbml0b3JzL2RlbGx1bHRyYTMw\",\"url\":\"https://data.schemaapp.com/ACCOUNTID/aHR0cHM6Ly9kZWxsLmNhL3Byb2R1Y3RzL21vbml0b3JzL2RlbGx1bHRyYTMw\",\"generatedAtTime\":\"2022-02-02T11:57:49.185Z\",\"@graph\":[{\"@context\":\"http://schema.org\",\"@type\":\"Product\",\"@id\":\"https://dell.ca/products/monitors/dellultra30\",\"aggregateRating\":{\"@type\":\"AggregateRating\",\"bestRating\":\"100\",\"ratingCount\":\"24\",\"ratingValue\":\"87\"},\"image\":\"dell-30in-lcd.jpg\",\"name\":\"Dell UltraSharp 30\\\" LCD Monitor\",\"offers\":{\"@type\":\"AggregateOffer\",\"highPrice\":\"$1495\",\"lowPrice\":\"$1250\"}}]}";

	@InjectMocks
	private final WebhooksHandlerServlet servlet = new WebhooksHandlerServlet();

	@Mock
	private SlingHttpServletRequest request;

	@Mock
	private SlingHttpServletResponse response;

	@Mock
	private WebhookHandlerService webhookHandlerService;

	@Mock
	private ObjectMapper mockObjectMapper;

	@Mock
	private BufferedReader reader;
	
	@Mock
	private WebhookEntity entity;
	
	private static ObjectMapper MAPPER = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
	
	@Test
	void testDoPost_entityCreated() throws ServletException, IOException, NoSuchFieldException, RepositoryException, LoginException {

		final WebhookEntity entity = MAPPER.readValue(JSON_LD, WebhookEntity.class);
		when(request.getReader()).thenReturn(reader);
		when(mockObjectMapper.readValue(reader, WebhookEntity.class)).thenReturn(entity);

		FieldSetter.setField(servlet, servlet.getClass().getDeclaredField("mapperObject"), mockObjectMapper);
		FieldSetter.setField(servlet, servlet.getClass().getDeclaredField("webhookHandlerService"), webhookHandlerService);

		servlet.doPost(request, response);
		assertNotNull(response);
	}
	
	@Test
	void testDoPost_entityUpdated() throws ServletException, IOException, NoSuchFieldException, RepositoryException, LoginException {

		final WebhookEntity entity = MAPPER.readValue(JSON_LD_ENTITYUPDATED, WebhookEntity.class);
		when(request.getReader()).thenReturn(reader);
		when(mockObjectMapper.readValue(reader, WebhookEntity.class)).thenReturn(entity);

		FieldSetter.setField(servlet, servlet.getClass().getDeclaredField("mapperObject"), mockObjectMapper);
		FieldSetter.setField(servlet, servlet.getClass().getDeclaredField("webhookHandlerService"), webhookHandlerService);

		servlet.doPost(request, response);
		assertNotNull(response);
	}
	
	@Test
	void testDoPost_entityDeleted() throws ServletException, IOException, NoSuchFieldException, RepositoryException, LoginException {

		final WebhookEntity entity = MAPPER.readValue(JSON_LD_ENTITYDELETED, WebhookEntity.class);
		when(request.getReader()).thenReturn(reader);
		when(mockObjectMapper.readValue(reader, WebhookEntity.class)).thenReturn(entity);

		FieldSetter.setField(servlet, servlet.getClass().getDeclaredField("mapperObject"), mockObjectMapper);
		FieldSetter.setField(servlet, servlet.getClass().getDeclaredField("webhookHandlerService"), webhookHandlerService);

		servlet.doPost(request, response);
		assertNotNull(response);
	}
	
	@Test
	void testDoPostException() throws ServletException, IOException, NoSuchFieldException, LoginException {

		when(request.getReader()).thenReturn(reader);

		FieldSetter.setField(servlet, servlet.getClass().getDeclaredField("mapperObject"), mockObjectMapper);
		servlet.doPost(request, response);
		assertNotNull(response);
	}
}
