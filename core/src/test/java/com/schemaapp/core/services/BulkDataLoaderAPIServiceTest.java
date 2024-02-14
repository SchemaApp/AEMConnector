package com.schemaapp.core.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemaapp.core.models.SchemaAppConfig;
import com.schemaapp.core.services.impl.BulkDataLoaderAPIServiceImpl;

@ExtendWith({ MockitoExtension.class})
public class BulkDataLoaderAPIServiceTest {

    @Mock
    private CDNHandlerService cdnDataHandlerService;

    @Spy
    @InjectMocks
    private BulkDataLoaderAPIServiceImpl bulkDataLoaderAPIService;


    @Test
    void fetchAndProcessPaginatedDataSuccess() throws Exception {
        // Mock dependencies
        SchemaAppConfig config = new SchemaAppConfig("accountId", "siteURL", "dMethod", "endpoint", "apikey");
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        String response = "{}";

        doReturn(response).when(bulkDataLoaderAPIService).executeApiRequest(anyString(), anyString());
        doReturn(null).when(bulkDataLoaderAPIService).getNextPage(any(), anyString());
        doReturn(new ArrayList<>()).when(bulkDataLoaderAPIService).retrievePagePathsFromNode(resourceResolver, "/conf/schemaapp/pagesData");
        doReturn(new ArrayList<>()).when(bulkDataLoaderAPIService).findMissingPages(anyList(), anyList());

        // Test the method
        bulkDataLoaderAPIService.fetchAndProcessPaginatedData(config, resourceResolver);

        // Verify that methods were called
        verify(bulkDataLoaderAPIService, times(1)).executeApiRequest(anyString(), anyString());
        verify(bulkDataLoaderAPIService, times(1)).parseJsonResponse(anyString());
        verify(bulkDataLoaderAPIService, times(1)).processJsonData(any(), anyList(), eq(resourceResolver), eq(config));
        verify(bulkDataLoaderAPIService, times(1)).retrievePagePathsFromNode(eq(resourceResolver), anyString());
        verify(cdnDataHandlerService, times(0)).removeResource(anyString(), eq(resourceResolver));
    }

    @Test
    void fetchAndProcessPaginatedDataWithDummyDataSuccess() throws Exception {
        // Mock dependencies
        SchemaAppConfig config = new SchemaAppConfig("accountId", "siteURL", "dMethod", "endpoint", "apikey");
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        String response = "{\"@context\":{\"@vocab\":\"http://www.w3.org/ns/hydra/core#\",\"schemamodel\":\"http://schemaapp.com/ontology/schemamodel#\"},\"@id\":\"https://api.schemaapp.com/export/NikhilAMETestCompany\",\"@type\":\"Collection\",\"@totalItems\":91,\"member\":[{\"https://publish-p62138-e507792.adobeaemcloud.com/content/schemaapp/en/article/sample-article-1\":{\"schemamodel:etag\":\"NULL\",\"schemamodel:source\":\"NULL\",\"aws:lastUpdated\":\"2023-03-17 23:28:41.483709\",\"@graph\":[{\"@type\":[\"Article\"],\"@id\":\"https://publish-p62138-e507792.adobeaemcloud.com/content/schemaapp/en/article/sample-article-1.html#Article\",\"@context\":{\"@vocab\":\"http://schema.org/\",\"kg\":\"http://g.co/kg\"},\"url\":\"https://publish-p62138-e507792.adobeaemcloud.com/content/schemaapp/en/article/sample-article-1.html\",\"about\":[{\"@id\":\"https://publish-p62138-e507792.adobeaemcloud.com/content/schemaapp/en/article\"}],\"archivedAt\":\"New Fixed Property\",\"accessMode\":\"Highlighter JavaScript\",\"identifier\":[{\"@type\":\"PropertyValue\",\"@id\":\"https://publish-p62138-e507792.adobeaemcloud.com/content/schemaapp/en/article/sample-article-1.html#Article_identifier_PropertyValue\",\"name\":\"InternalUrl\",\"value\":\"https://publish-p62138-e507792.adobeaemcloud.com/content/schemaapp/en/article/sample-article-1\"}],\"headline\":\"Subtitle\",\"name\":\"Sample Article 1\",\"description\":\"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Duis ac pulvinar erat. In nunc orci, tincidunt a enim eu, malesuada pharetra felis. Suspendisse ut lectus id lorem vulputate scelerisque aliquam vel arcu. Aliquam elementum purus ut ornare aliquam. Phasellus rhoncus auctor velit. Etiam sollicitudin enim in tincidunt dignissim. In lacinia vulputate varius. Curabitur et accumsan justo. Pellentesque tristique malesuada risus vitae faucibus. Integer commodo dui nisl, sit amet viverra velit dapibus a.\"},{\"@context\":\"http://schema.org\",\"@type\":\"Thing\",\"name\":\"article2\",\"description\":\"my test data tttt\",\"additionalType\":\"my test data eeee\",\"identifier\":\"my test data uuuu\",\"disambiguatingDescription\":\"my test data yyyy\",\"alternateName\":\"my test data rrrr\",\"@id\":\"https://publish-p62138-e507792.adobeaemcloud.com/content/schemaapp/en/article\"}]}}],\"view\":{\"@id\":\"https://api.schemaapp.com/export/NikhilAMETestCompany\",\"@type\":\"PartialCollectionView\",\"first\":\"/export/NikhilAMETestCompany?page=1&pageSize=15&includeSubAccounts=False\",\"last\":\"/export/NikhilAMETestCompany?page=7&pageSize=15&includeSubAccounts=False\",\"previous\":\"/export/NikhilAMETestCompany?page=6&pageSize=15&includeSubAccounts=False\",\"next\":\"/export/NikhilAMETestCompany?page=7&pageSize=15&includeSubAccounts=False\",\"items\":1,\"page\":7,\"pageSize\":15,\"includeSubAccounts\":false}}";

        doReturn(response).when(bulkDataLoaderAPIService).executeApiRequest(anyString(), anyString());

        // Test the method
        bulkDataLoaderAPIService.fetchAndProcessPaginatedData(config, resourceResolver);

        // Verify that methods were called
        verify(bulkDataLoaderAPIService, times(1)).executeApiRequest(anyString(), anyString());
        verify(bulkDataLoaderAPIService, times(1)).parseJsonResponse(anyString());
        verify(bulkDataLoaderAPIService, times(1)).processJsonData(any(), anyList(), eq(resourceResolver), eq(config));
        verify(bulkDataLoaderAPIService, times(1)).retrievePagePathsFromNode(eq(resourceResolver), anyString());
        verify(cdnDataHandlerService, times(0)).removeResource(anyString(), eq(resourceResolver));
    }

    @Test
    void fetchAndProcessPaginatedDataException() throws Exception {
        // Mock dependencies
        SchemaAppConfig config = new SchemaAppConfig("accountId", "siteURL", "dMethod", "endpoint", "apikey");
        ResourceResolver resourceResolver = mock(ResourceResolver.class);

        doThrow(IOException.class).when(bulkDataLoaderAPIService).executeApiRequest(config.getApiKey(), "https://api.schemaapp.com/export/accountId");

        // Test the method
        bulkDataLoaderAPIService.fetchAndProcessPaginatedData(config, resourceResolver);

        // Verify that the error is logged
        verify(bulkDataLoaderAPIService, times(1)).executeApiRequest(anyString(), anyString());
        verify(bulkDataLoaderAPIService, times(0)).parseJsonResponse(anyString());
        verify(bulkDataLoaderAPIService, times(0)).processJsonData(any(), anyList(), eq(resourceResolver), eq(config));
        verify(bulkDataLoaderAPIService, times(0)).retrievePagePathsFromNode(eq(resourceResolver), anyString());
        verify(cdnDataHandlerService, times(0)).removeResource(anyString(), eq(resourceResolver));
    }

    @Test
    void executeApiRequestException() throws IOException {
        // Mock dependencies
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);

        doReturn(httpClient).when(bulkDataLoaderAPIService).getClient();
        when(httpClient.execute(any(HttpGet.class))).thenThrow(IOException.class);

        // Test the method
        assertThrows(IOException.class, () -> bulkDataLoaderAPIService.executeApiRequest("apiKey", "url"));

        // Verify that the correct HTTP methods were called
        verify(httpClient, times(1)).execute(any(HttpGet.class));
        verify(httpClient, times(1)).close();
    }


}
