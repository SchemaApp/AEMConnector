package com.schemaapp.core.services.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaapp.core.models.SchemaAppConfig;
import com.schemaapp.core.services.BulkDataLoaderAPIService;

@Component(service = BulkDataLoaderAPIService.class, immediate = true)
public class BulkDataLoaderAPIServiceImpl implements BulkDataLoaderAPIService {
   
    /**
     * Fetches and processes paginated data from a remote API.
     */
    public void fetchAndProcessPaginatedData(SchemaAppConfig config, ResourceResolver resourceResolver) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String baseUrl = "https://api.hunchmanifest.com/export/" + config.getAccountId();
            String nextPage = baseUrl;

            while (nextPage != null) {
                URI uri = URI.create(nextPage);
                HttpGet httpGet = new HttpGet(uri);

                // Make the API call and retrieve the JSON response
                String response = executeHttpRequest(httpGet);

                // Parse the JSON response
                JsonNode rootNode = objectMapper.readTree(response);

             // Access the "member" object
                JsonNode memberObject = rootNode.get("member");

                if (memberObject != null && memberObject.isObject()) {
                    // Iterate through the keys (dynamic PAGE_URI values)
                    memberObject.fields().forEachRemaining(entry -> {
                        String pageUri = entry.getKey();
                        JsonNode pageData = entry.getValue();

                        // Access and print values within each inner object
                        System.out.println("PAGE_URI: " + pageUri);
                        System.out.println("schemamodel:etag: " + pageData.get("schemamodel:etag"));
                        System.out.println("schemamodel:source: " + pageData.get("schemamodel:source"));
                        System.out.println("aws:lastUpdated: " + pageData.get("aws:lastUpdated"));
                     // Access and handle @graph element (which can be an array or an object)
                        JsonNode graphNode = pageData.get("@graph");
                        if (graphNode != null) {
                            if (graphNode.isArray()) {
                                System.out.println("@graph (Array): " + graphNode);
                                // Process each element of the array if needed
                            } else {
                                System.out.println("@graph (Object): " + graphNode);
                                // Process the single object if needed
                            }
                        }
                        
                        
                        String path = getContentPagePath(pageUri);
                        // Construct the path for the node
                        String nodePath = path + "/jcr:content/schemaapp"; // Path to the schemaapp node

                        // Check if the node already exists
                        Resource schemaappNode = resourceResolver.getResource(nodePath);

                       
                    });
                }
                
                // Check if there is a "next" page in the "view" element
                JsonNode view = rootNode.get("view");
                nextPage = view.has("next") ? view.get("next").asText() : null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getContentPagePath(String pageUri) {
        try {
            URI contentPageURL = new URI(pageUri);
            return contentPageURL.getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        
        return pageUri;
    }

    /**
     * Executes an HTTP request and returns the response as a string.
     *
     * @param request The HTTP request to execute.
     * @return The response as a string.
     * @throws IOException If an I/O error occurs during the HTTP request.
     */
    private String executeHttpRequest(HttpGet request) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            CloseableHttpResponse response = client.execute(request);
            int statusCode = response.getStatusLine().getStatusCode(); // Corrected to getStatus()
            if (statusCode != 200) {
                throw new HttpResponseException(statusCode, response.getStatusLine().getReasonPhrase());
            }
            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity, StandardCharsets.UTF_8);
        }
    }

}
