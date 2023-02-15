package com.schemaapp.core.services;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.day.cq.replication.Replicator;
import com.day.cq.wcm.api.Page;
import com.schemaapp.core.models.CDNEntity;
import com.schemaapp.core.services.impl.CDNHandlerServiceImpl;
import com.schemaapp.core.util.Constants;

@ExtendWith({ MockitoExtension.class})
class CDNHandlerServiceTest {

	@Mock
	private ResourceResolver resolver;

	@Mock
	private ResourceResolverFactory resolverFactory;

	@Mock
	private Resource resource, entityResource;

	@Mock
	private CDNEntity entity;
	
	@Mock
	private Node node;
	
	@Mock
    private Page page;
	
	@Mock
	private Replicator replicator;
	
	@Mock
    private Session session;
	
	@InjectMocks
	private final CDNHandlerServiceImpl webhookHandlerService = new CDNHandlerServiceImpl();

	@Test
	void deleteEntityTest() throws Exception {

	    when(page.adaptTo(Node.class)).thenReturn(node);
	    when(node.hasNode(Constants.DATA)).thenReturn(true);
	    when(node.getNode(Constants.DATA)).thenReturn(node);
	    when(resolver.adaptTo(Session.class)).thenReturn(session);

		webhookHandlerService.deleteEntity(page, resolver);
		verify(node, times(1)).remove();
		verify(session, times(1)).save();
	}

}
	

