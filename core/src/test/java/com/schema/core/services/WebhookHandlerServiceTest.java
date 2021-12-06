package com.schema.core.services;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;

import org.apache.jackrabbit.JcrConstants;
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

import com.schema.core.models.WebhookEntity;
import com.schema.core.services.impl.WebhookHandlerServiceImpl;

@ExtendWith({ MockitoExtension.class})
class WebhookHandlerServiceTest {

	@Mock
	private ResourceResolver resolver;

	@Mock
	private ResourceResolverFactory resolverFactory;

	@Mock
	private Resource resource;

	@Mock
	private WebhookEntity entity;
	
	@Mock
	private Node node;
	

	@InjectMocks
	private final WebhookHandlerServiceImpl webhookHandlerService = new WebhookHandlerServiceImpl();

	@Test
	void createEntityTest() throws Exception {

		mockResolver();
		when(resource.getResourceType()).thenReturn("sling:nonexisting");
		when(resolver.getResource(anyString())).thenReturn(resource);
		when(entity.getGraph()).thenReturn(new String("test"));
		when(resource.adaptTo(Node.class)).thenReturn(node);
		when(node.addNode("data", JcrConstants.NT_UNSTRUCTURED)).thenReturn(node);
		
		webhookHandlerService.createEntity(entity);
		verify(resolver, times(2)).commit();
	}

	private void mockResolver() throws NoSuchFieldException, LoginException {
		Map<String, Object> param = new HashMap<>();
		param.put(ResourceResolverFactory.SUBSERVICE, "schema-app-service");
		FieldSetter.setField(webhookHandlerService, webhookHandlerService.getClass().getDeclaredField("resolverFactory"), resolverFactory);
		when(resolverFactory.getServiceResourceResolver(param)).thenReturn(resolver);
	}
}
	

