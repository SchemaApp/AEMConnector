package com.schemaapp.core.services.impl;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.json.JSONException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.replication.ReplicationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.schemaapp.core.models.SchemaAppConfig;
import com.schemaapp.core.services.BulkDataLoaderAPIService;
import com.schemaapp.core.services.CDNHandlerService;
import com.schemaapp.core.util.Constants;

@Component(service = BulkDataLoaderAPIService.class, immediate = true)
public class BulkDataLoaderAPIServiceImpl implements BulkDataLoaderAPIService {

    private boolean existing = false;

    @Reference
    private CDNHandlerService cdnDataHandlerService;

    private static final ObjectMapper mapperObject = new ObjectMapper()
            .configure(FAIL_ON_MISSING_CREATOR_PROPERTIES, true);

    public static Logger logger = LoggerFactory.getLogger(BulkDataLoaderAPIServiceImpl.class);

    /**
     * Fetches and processes paginated data from a remote API.
     *
     * @param config           The SchemaApp configuration.
     * @param resourceResolver The resource resolver.
     */
    public void fetchAndProcessPaginatedData(SchemaAppConfig config, ResourceResolver resourceResolver) {
        try {
            String baseUrl = "https://api.schemaapp.com";
            String nextPage = baseUrl + "/export/" + config.getAccountId();
            List<String> newPages = new ArrayList<>();
            existing = false;
            while (nextPage != null) {
                String response = executeApiRequest(config.getApiKey(), nextPage);
                if (response == null) break;
                JsonNode rootNode = parseJsonResponse(response);

                processJsonData(rootNode, newPages, resourceResolver, config);

                nextPage = getNextPage(rootNode, baseUrl);
            }

            List<String> pagePaths = retrievePagePathsFromNode(resourceResolver, "/conf/schemaapp/pagesData");
            List<String> missingPages = findMissingPages(pagePaths, newPages);
            cdnDataHandlerService.savePagePathsToNode(resourceResolver, "/conf/schemaapp", newPages);

            missingPages.forEach(path -> cdnDataHandlerService.removeResource(path, resourceResolver));

        } catch (IOException | RepositoryException | JSONException | ReplicationException e) {
            logger.error("Error fetching and processing paginated data", e);
        }
    }

    /**
     * Executes an API request and returns the response as a string.
     *
     * @param apiKey The API key.
     * @param url    The URL to execute the request.
     * @return The API response as a string.
     */
    public String executeApiRequest(String apiKey, String url) {
        try (CloseableHttpClient client = getClient()) {
            HttpGet httpGet = new HttpGet(URI.create(url));
            httpGet.addHeader("x-api-key", apiKey);

            try (CloseableHttpResponse response = client.execute(httpGet)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    logger.error("API request failed with status code: {} URL: {} and reason: {}", statusCode, url, response.getStatusLine().getReasonPhrase());
                    return null;
                }

                HttpEntity entity = response.getEntity();
                return EntityUtils.toString(entity, StandardCharsets.UTF_8);
            }

        } catch (HttpResponseException e) {
            logger.error("HTTP response error: Status code: {}, Reason: {}", e.getStatusCode(), e);
        } catch (ClientProtocolException e) {
            logger.error("HTTP protocol error while making API request to URL: {}", url, e);
        } catch (IOException e) {
            logger.error("I/O error while making API request to URL: {}", url, e);
        } catch (Exception e) {
            logger.error("Unexpected error during API request to URL: {}", url, e);
        }
        return null; // Return null or appropriate default response
    }


    public CloseableHttpClient getClient() {
        return HttpClients.createDefault();
    }

    /**
     * Parses a JSON response and returns the root node.
     *
     * @param response The JSON response.
     * @return The root node of the JSON.
     * @throws IOException If an I/O error occurs during JSON parsing.
     */
    public JsonNode parseJsonResponse(String response) throws IOException {
        return new ObjectMapper().readTree(response);
    }

    /**
     * Processes JSON data, extracting information about pages.
     *
     * @param rootNode         The root node of the JSON data.
     * @param newPages         List to store newly fetched page paths.
     * @param resourceResolver The resource resolver.
     * @param config           The SchemaApp configuration.
     * @throws IOException               If an I/O error occurs during processing.
     * @throws RepositoryException       If a repository error occurs during processing.
     * @throws JSONException             If a JSON error occurs during processing.
     * @throws ReplicationException      If a replication error occurs during processing.
     */
    public void processJsonData(JsonNode rootNode, List<String> newPages, ResourceResolver resourceResolver, SchemaAppConfig config)
            throws IOException, RepositoryException, JSONException, ReplicationException {
        JsonNode memberObject = rootNode.get("member");

        if (memberObject != null && memberObject.isArray()) {
            for (JsonNode objectNode : memberObject) {
                processObjectNode(objectNode, newPages, resourceResolver, config);
            }
        }
    }

    /**
     * Processes an object node, extracting information about a specific page.
     *
     * @param objectNode        The JSON object node representing a page.
     * @param newPages          List to store newly fetched page paths.
     * @param resourceResolver  The resource resolver.
     * @param config            The SchemaApp configuration.
     * @throws IOException               If an I/O error occurs during processing.
     * @throws RepositoryException       If a repository error occurs during processing.
     * @throws JSONException             If a JSON error occurs during processing.
     * @throws ReplicationException      If a replication error occurs during processing.
     */
    private void processObjectNode(JsonNode objectNode, List<String> newPages, ResourceResolver resourceResolver, SchemaAppConfig config) {
        Iterator<String> fieldNames = objectNode.fieldNames();

        while (fieldNames.hasNext()) {
            String pageUri = fieldNames.next();
            try {
                JsonNode pageData = objectNode.get(pageUri);

                logger.debug("PAGE_URI: {}", pageUri);
                logger.debug("schemamodel:etag: {}", pageData.get("schemamodel:etag"));
                logger.debug("schemamodel:source: {}", pageData.get("schemamodel:source"));
                logger.debug("aws:lastUpdated: {}", pageData.get("aws:lastUpdated"));
                logger.debug("is existing :: {}", existing);
                
                String path = getContentPagePath(pageUri);
                newPages.add(path);

                if (!existing) {
                    String nodePath = path + "/jcr:content/schemaapp";
                    Resource schemaappResource = resourceResolver.getResource(nodePath);

                    String eTagAEM = schemaappResource != null ? schemaappResource.getValueMap().get(Constants.E_TAG, StringUtils.EMPTY) : StringUtils.EMPTY;
                    String eTag = pageData.get("schemamodel:etag") != null ? pageData.get("schemamodel:etag").asText() : null;

                    if (Strings.isNullOrEmpty(eTag) || !eTagAEM.equals(eTag)) {
                        processDifferentETagsPages(pageData, path, resourceResolver, config);
                    } else {
                        existing = true;
                    }
                }
            } catch (IOException | RepositoryException | JSONException | ReplicationException e) {
                logger.error("Error while processing page data, page url:  {}", pageUri, e);
            }
        }
    }

    /**
     * Processes pages with different ETags, updating the repository.
     *
     * @param pageData          The JSON node containing page data.
     * @param path              The path of the page.
     * @param resourceResolver  The resource resolver.
     * @param config            The SchemaApp configuration.
     * @throws IOException               If an I/O error occurs during processing.
     * @throws RepositoryException       If a repository error occurs during processing.
     * @throws JSONException             If a JSON error occurs during processing.
     * @throws ReplicationException      If a replication error occurs during processing.
     */
    private void processDifferentETagsPages(JsonNode pageData, String path, ResourceResolver resourceResolver, SchemaAppConfig config)
            throws IOException, RepositoryException, JSONException, ReplicationException {
        Map<String, String> additionalConfigMap = new HashMap<>();
        additionalConfigMap.put(Constants.E_TAG, pageData.get("schemamodel:etag").asText());
        additionalConfigMap.put(Constants.SOURCE_HEADER, pageData.get("schemamodel:source").asText());

        Resource pageResource = resourceResolver.getResource(path);
        Object graphNode = pageData.get("@graph");

        try {
            mapperObject.readValue(graphNode.toString(), Object.class);
            cdnDataHandlerService.savenReplicate(graphNode, resourceResolver, additionalConfigMap, pageResource, config);
        } catch (IOException | RepositoryException | JSONException | ReplicationException e) {
            logger.error("Error processing page data for path: {}", path, e);
        }
    }

    /**
     * Retrieves the URL of the next page from the JSON response.
     *
     * @param rootNode The root node of the JSON response.
     * @param baseUrl  The base URL.
     * @return The URL of the next page, or null if there is no next page.
     */
    public String getNextPage(JsonNode rootNode, String baseUrl) {
        JsonNode view = rootNode.get("view");
        String nextPage = view.has("next") ? view.get("next").asText() : null;
        String last = view.has("last") ? view.get("last").asText() : null;

        return (last != null && last.equalsIgnoreCase(nextPage)) ? null : baseUrl + nextPage;
    }



    /**
     * Gets the content page path from the given page URI.
     *
     * @param pageUri The URI of the page.
     * @return The content page path without the file name and extension.
     */
    private String getContentPagePath(String pageUri) {
        try {
            URI contentPageURL = new URI(pageUri);
            // Get the path without the file name and extension
            String pathWithoutExtension = removeExtension(contentPageURL.getPath());

            return pathWithoutExtension;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return pageUri;
    }

    /**
     * Removes the file extension from the given path.
     *
     * @param path The path containing the file extension.
     * @return The path without the file extension.
     */
    private static String removeExtension(String path) {
        int lastDotIndex = path.lastIndexOf('.');
        int lastSeparatorIndex = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));

        // Check if there is a dot after the last path separator
        if (lastDotIndex > lastSeparatorIndex) {
            return path.substring(0, lastDotIndex);
        }

        return path;
    }

    /**
     * Retrieves page paths from the specified parent node in the resource resolver.
     *
     * @param resolver       The resource resolver.
     * @param parentNodePath The path of the parent node.
     * @return The list of retrieved page paths.
     */
    public List<String> retrievePagePathsFromNode(ResourceResolver resolver, String parentNodePath) {
        List<String> pagePaths = new ArrayList<>();

        Resource parentNode = resolver.getResource(parentNodePath);

        if (parentNode != null) {
            ValueMap properties = parentNode.adaptTo(ValueMap.class);

            // Assuming 'pagePaths' is a property of type String[]
            String[] storedPagePaths = properties.get("pagePaths", new String[0]);

            pagePaths.addAll(Arrays.asList(storedPagePaths));
        }

        return pagePaths;
    }

    /**
     * Finds and returns the missing pages by comparing existing and new page paths.
     *
     * @param existingPagePaths The list of existing page paths.
     * @param newPagePaths      The list of new page paths.
     * @return The list of missing pages.
     */
    public List<String> findMissingPages(List<String> existingPagePaths, List<String> newPagePaths) {
        List<String> missingPages = new ArrayList<>(existingPagePaths);
        missingPages.removeAll(newPagePaths);

        return missingPages;
    }

}
