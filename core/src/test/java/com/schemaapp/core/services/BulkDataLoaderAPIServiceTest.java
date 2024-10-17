package com.schemaapp.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.json.JSONArray;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaapp.core.models.SchemaAppConfig;
import com.schemaapp.core.services.impl.BulkDataLoaderAPIServiceImpl;

@ExtendWith(MockitoExtension.class)
public class BulkDataLoaderAPIServiceTest {

    @InjectMocks
    private BulkDataLoaderAPIServiceImpl bulkDataLoaderAPIService;

    @Mock
    private CDNHandlerService cdnDataHandlerService;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private Resource resource;

    @Mock
    private ValueMap valueMap;

    @Mock
    private SchemaAppConfig config;

    @Mock
    private JsonNode rootNode;
    
    @Mock
    private JSONArray rootNodeArray;

    @Mock
    private JsonNode memberNode;
    
    @Mock
    private JsonNode pageData;

    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private Iterator<String> fieldNames;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testFetchAndProcessPaginatedDataSuccess() throws Exception {
        // Arrange
        when(resourceResolver.getResource(anyString())).thenReturn(resource);
        when(resource.adaptTo(ValueMap.class)).thenReturn(valueMap);
        String[] mockedPagePaths = {"path1", "path2"};
        when(valueMap.get(eq("pagePaths"), any(String[].class))).thenReturn(mockedPagePaths);

        // Act
        bulkDataLoaderAPIService.fetchAndProcessPaginatedData(config, resourceResolver);

        // Assert
        verify(cdnDataHandlerService, times(1)).savePagePathsToNode(eq(resourceResolver), eq("/conf/schemaapp"), anyList());
    }

    @Test
    public void testExecuteApiRequestSuccess() throws IOException {
        // Arrange
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);
        HttpEntity httpEntity = mock(HttpEntity.class);

        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        when(httpEntity.getContent()).thenReturn(new ByteArrayInputStream("Success".getBytes()));

        // Mock getClient to return mocked httpClient
        BulkDataLoaderAPIServiceImpl bulkDataLoaderAPIServiceSpy = spy(bulkDataLoaderAPIService);
        doReturn(httpClient).when(bulkDataLoaderAPIServiceSpy).getClient();

        // Act
        String result = bulkDataLoaderAPIServiceSpy.executeApiRequest("apiKey", "testUrl");

        // Assert
        assertNotNull(result);
        assertEquals("Success", result);
    }

 // Ensure only necessary stubbing is done:
    @Test
    public void testExecuteApiRequestFailure() throws IOException {
        // Arrange: Mock only the necessary part of the test case
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class);
        StatusLine statusLine = mock(StatusLine.class);

        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(404); // Simulate failure

        // Mock getClient to return mocked httpClient
        BulkDataLoaderAPIServiceImpl bulkDataLoaderAPIServiceSpy = spy(bulkDataLoaderAPIService);
        doReturn(httpClient).when(bulkDataLoaderAPIServiceSpy).getClient();

        // Act
        String result = bulkDataLoaderAPIServiceSpy.executeApiRequest("apiKey", "testUrl");

        // Assert
        assertNull(result); // Ensure null is returned on failure
    }


    @Test
    public void testProcessJsonData() throws Exception {
        // Arrange
        when(rootNode.get("member")).thenReturn(memberNode);
        when(memberNode.isArray()).thenReturn(true);

        when(fieldNames.hasNext()).thenReturn(true, false); // Simulate one element in the array
        when(fieldNames.next()).thenReturn("https://experience.adobe.com/content/testpag.html"); 
        when(memberNode.fieldNames()).thenReturn(fieldNames);
        when(memberNode.get("https://experience.adobe.com/content/testpag.html")).thenReturn(pageData);

        when(pageData.get(anyString())).thenReturn(pageData);
        when(pageData.asText()).thenReturn("testData");
        Iterator<JsonNode> mockIterator = mock(Iterator.class);
        when(mockIterator.hasNext()).thenReturn(true, false); // Simulate one element
        when(mockIterator.next()).thenReturn(memberNode); // Provide mock object node
        when(memberNode.elements()).thenReturn(mockIterator); // Return mock iterator

        
        List<String> newPages =  new ArrayList();
        newPages.add("/content/testpage");

        bulkDataLoaderAPIService.processJsonData(rootNode, newPages, resourceResolver, config);

        // Assert
        // Verify that the interactions with mocks occurred as expected
        verify(rootNode).get("member");
        verify(memberNode).isArray();
        verify(memberNode).elements();

        // Add meaningful assertions based on your logic (example)
        assertNotNull(newPages);
        assertEquals(2, newPages.size());
        assertTrue(newPages.contains("/content/testpage"));

    }


    @Test
    public void testGetNextPage() {
        // Mock the behavior of rootNode and viewNode
        JsonNode viewNode = mock(JsonNode.class);
        JsonNode nextNode = mock(JsonNode.class);
        
        when(rootNode.get("view")).thenReturn(viewNode);
        when(viewNode.has("next")).thenReturn(true);
        when(viewNode.get("next")).thenReturn(nextNode);
        when(nextNode.asText()).thenReturn("nextPage");

        // Act
        String nextPage = bulkDataLoaderAPIService.getNextPage(rootNode, "https://api.schemaapp.com");

        // Assert
        assertEquals("https://api.schemaapp.comnextPage", nextPage);
    }


    @Test
    public void testRetrievePagePathsFromNode() {
        // Arrange
        when(resourceResolver.getResource(anyString())).thenReturn(resource);
        when(resource.adaptTo(ValueMap.class)).thenReturn(valueMap);
        when(valueMap.get(anyString(), any(String[].class))).thenReturn(new String[]{"path1", "path2"});

        // Act
        List<String> pagePaths = bulkDataLoaderAPIService.retrievePagePathsFromNode(resourceResolver, "/conf/schemaapp/pagesData");

        // Assert
        assertEquals(2, pagePaths.size());
        assertEquals("path1", pagePaths.get(0));
        assertEquals("path2", pagePaths.get(1));
    }
}
