package com.schemaapp.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.jcr.Node;
import javax.jcr.Property;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemaapp.core.services.impl.PageJSONDataReaderServiceImpl;
import com.schemaapp.core.util.Constants;

@ExtendWith({ MockitoExtension.class})
class PageJSONDataReaderServiceImplTest {

    @Mock
    private ResourceResolverFactory resolverFactory;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private Resource urlResource;

    @Mock
    private Node schemaAppDataNode;

    @Mock
    private Property entityProperty;

    @Mock
    private Property sourceProperty;

    @InjectMocks
    private PageJSONDataReaderServiceImpl pageJSONDataReaderService;
    
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }
    
    @AfterEach
    void tearDown() throws Exception {
        closeable.close();  // Close mock session
    }

    // Test: init() - Happy Path
    @Test
    void testInit_Success() throws Exception {
        String pageUrl = "https://example.com/page.html";

        // Mock resolver and resource behavior
        when(resolverFactory.getServiceResourceResolver(anyMap())).thenReturn(resourceResolver);
        when(resourceResolver.resolve(anyString())).thenReturn(urlResource);
        when(urlResource.getChild(anyString())).thenReturn(urlResource);
        when(urlResource.adaptTo(Node.class)).thenReturn(schemaAppDataNode);

        // Mock Node properties
        when(schemaAppDataNode.hasProperty("entity")).thenReturn(true);
        when(schemaAppDataNode.getProperty("entity")).thenReturn(entityProperty);
        when(entityProperty.getString()).thenReturn("mockGraphData");

        when(schemaAppDataNode.hasProperty(Constants.SOURCE_HEADER)).thenReturn(true);
        when(schemaAppDataNode.getProperty(Constants.SOURCE_HEADER)).thenReturn(sourceProperty);
        when(sourceProperty.getString()).thenReturn("mockSource");

        // Call the method under test
        pageJSONDataReaderService.init(pageUrl);

        // Verify the graph data and source have been set correctly
        assertEquals("mockGraphData", pageJSONDataReaderService.getGraphData());
        assertEquals("mockSource", pageJSONDataReaderService.getSource());
    }

    // Test: init() - Resource not found or missing child node
    @Test
    void testInit_NoChildNode() throws Exception {
        String pageUrl = "https://example.com/page.html";

        // Mock resolver and resource behavior
        when(resolverFactory.getServiceResourceResolver(anyMap())).thenReturn(resourceResolver);
        when(resourceResolver.resolve(anyString())).thenReturn(urlResource);
        when(urlResource.getChild(anyString())).thenReturn(null);  // No child node found

        // Call the method under test
        pageJSONDataReaderService.init(pageUrl);

        // Verify that graph data and source remain null
        assertNull(pageJSONDataReaderService.getGraphData());
        assertNull(pageJSONDataReaderService.getSource());
    }

    // Test: getResourceResolver() - Success
    @Test
    void testGetResourceResolver_Success() throws Exception {
        // Mock resolver behavior
        when(resolverFactory.getServiceResourceResolver(anyMap())).thenReturn(resourceResolver);

        // Call the method under test
        ResourceResolver result = pageJSONDataReaderService.getResourceResolver();

        // Verify the resolver is returned
        assertNotNull(result);
        verify(resolverFactory, times(1)).getServiceResourceResolver(anyMap());
    }

    // Test: getResourceResolver() - LoginException handling
    @Test
    void testGetResourceResolver_LoginException() throws Exception {
        // Simulate LoginException
        when(resolverFactory.getServiceResourceResolver(anyMap())).thenThrow(new LoginException("Test Exception"));

        // Call the method and expect the exception to be thrown
        assertThrows(LoginException.class, () -> pageJSONDataReaderService.getResourceResolver());
    }

    // Test: getPath() - Valid URL
    @Test
    void testGetPath_ValidUrl() {
        String pageUrl = "https://example.com/page.html";
        String expectedPath = "/page"; // Without the extension

        // Call the method under test
        String result = pageJSONDataReaderService.getPath(pageUrl);

        // Verify the path is correctly returned without extension
        assertEquals(expectedPath, result);
    }

    // Test: getPath() - Malformed URL
    @Test
    void testGetPath_MalformedUrl() {
        String invalidUrl = "htp://invalid-url"; // Invalid protocol

        // Call the method under test
        String result = pageJSONDataReaderService.getPath(invalidUrl);

        // Verify the result is the same as input for malformed URLs
        assertEquals(invalidUrl, result);
    }
}
