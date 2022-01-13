package com.schemaapp.core.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

	@Test
	void testDoPost() throws ServletException, IOException, NoSuchFieldException, RepositoryException, LoginException {

		when(request.getReader()).thenReturn(reader);
		when(entity.getType()).thenReturn(new String("EntityCreated"));
		when(mockObjectMapper.readValue(reader, WebhookEntity.class)).thenReturn(entity);

		FieldSetter.setField(servlet, servlet.getClass().getDeclaredField("MAPPER"), mockObjectMapper);
		FieldSetter.setField(servlet, servlet.getClass().getDeclaredField("webhookHandlerService"), webhookHandlerService);

		servlet.doPost(request, response);
		assertNotNull(response);
	}

	@Test
	void testDoPostException() throws ServletException, IOException, NoSuchFieldException, LoginException {

		WebhookEntity entity = new WebhookEntity();
		when(request.getReader()).thenReturn(reader);
		when(mockObjectMapper.readValue(reader, WebhookEntity.class)).thenReturn(entity);

		FieldSetter.setField(servlet, servlet.getClass().getDeclaredField("MAPPER"), mockObjectMapper);
		try {
			servlet.doPost(request, response);
		}catch (NullPointerException e)	{
			assertEquals(500, response.getStatus());
		}
	}
}
