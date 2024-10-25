package com.schemaapp.core.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.day.cq.wcm.api.Page;
import com.schemaapp.core.services.impl.FlushServiceImpl;
import com.schemaapp.core.util.ReplicationConstants;

@ExtendWith(MockitoExtension.class)
public class FlushServiceImplTest {

    @InjectMocks
    private FlushServiceImpl flushService;

    @Mock
    private ResourceResolverFactory resolverFactory;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private Resource publishReplicationAgentResource;

    @Mock
    private Resource contentResource;

    @Mock
    private Node contentResourceNode;

    @Mock
    private Page publishReplicationAgentPage;

    @Mock
    private Page childPage;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse httpResponse;

    @Mock
    private Iterator<Page> pageIterator;
    
    @Mock
    private Property property;
    
    @Mock
    private Value value;

    @BeforeEach
    public void setUp() throws LoginException {
        // Mock ResourceResolver retrieval
        when(resolverFactory.getServiceResourceResolver(anyMap())).thenReturn(resourceResolver);
    }

    @Test
    public void testInvalidatePageJsonSuccess() throws LoginException {
        // Arrange
        when(resourceResolver.getResource(anyString())).thenReturn(publishReplicationAgentResource);
        when(publishReplicationAgentResource.adaptTo(Page.class)).thenReturn(publishReplicationAgentPage);
        when(publishReplicationAgentPage.listChildren()).thenReturn(pageIterator);
        when(pageIterator.hasNext()).thenReturn(false);

        // Act
        flushService.invalidatePageJson("/content/testPage");

        // Assert
        verify(resourceResolver, times(1)).getResource(ReplicationConstants.REPLICATION_AGENT_PATH_PUBLISH);
        verify(publishReplicationAgentResource, times(1)).adaptTo(Page.class);
    }

    @Test
    public void testInvalidatePageJsonResourceNull() throws LoginException {
        // Arrange
        when(resourceResolver.getResource(anyString())).thenReturn(null);

        // Act
        flushService.invalidatePageJson("/content/testPage");

        // Assert
        verify(resourceResolver, times(1)).getResource(ReplicationConstants.REPLICATION_AGENT_PATH_PUBLISH);
        verify(publishReplicationAgentResource, never()).adaptTo(Page.class);
    }

    @Test
    public void testGetResourceNodeSuccess() throws RepositoryException {
        // Arrange
        when(resourceResolver.getResource(anyString())).thenReturn(publishReplicationAgentResource);
        when(publishReplicationAgentResource.adaptTo(Page.class)).thenReturn(publishReplicationAgentPage);
        when(publishReplicationAgentPage.listChildren()).thenReturn(pageIterator);
        when(pageIterator.hasNext()).thenReturn(true, false);
        when(pageIterator.next()).thenReturn(childPage);
        when(childPage.getContentResource()).thenReturn(contentResource);
        when(contentResource.adaptTo(Node.class)).thenReturn(contentResourceNode);
        when(contentResourceNode.hasProperty("transportUri")).thenReturn(true);
        when(contentResourceNode.getProperty("transportUri")).thenReturn(property);
        when(property.getValue()).thenReturn(value);
        when(value.getString()).thenReturn("http://dispatcher/cache");
        // Act
        flushService.invalidatePageJson("/content/testPage");

        // Assert
        verify(contentResourceNode, times(1)).getProperty("transportUri");
    }

    @Test
    public void testInvalidatePageJsonLoginException() throws LoginException {
        // Arrange
        when(resolverFactory.getServiceResourceResolver(anyMap())).thenThrow(new LoginException("Login failed"));

        // Act and Assert
        assertDoesNotThrow(() -> flushService.invalidatePageJson("/content/testPage"));
        verify(resolverFactory, times(1)).getServiceResourceResolver(anyMap());
    }

}
