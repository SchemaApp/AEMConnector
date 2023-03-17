package com.schemaapp.core.services.impl;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.replication.ReplicationException;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaapp.core.services.CDNDataAPIService;
import com.schemaapp.core.services.CDNHandlerService;
import com.schemaapp.core.util.ConfigurationUtil;
import com.schemaapp.core.util.Constants;

@Component(service = CDNDataAPIService.class, immediate = true)
public class CDNDataAPIServiceImpl implements CDNDataAPIService {

    private static final String BODY = "body";

    private static final String JAVA_SCRIPT = "javaScript";

    private static final String CQ_CLOUDSERVICECONFIGS = "cq:cloudserviceconfigs";

    private final Logger LOG = LoggerFactory.getLogger(CDNDataAPIServiceImpl.class);

    @Reference
    private ConfigurationAdmin configurationAdmin;

    @Reference
    private CDNHandlerService webhookHandlerService;

    @Reference
    transient ResourceResolverFactory resolverFactory;

    private static ObjectMapper mapperObject = new ObjectMapper()
            .configure(FAIL_ON_MISSING_CREATOR_PROPERTIES, true);
    

    @Override
    public void readCDNData() {

        ResourceResolver resolver = getResourceResolver();
        try {
            if (resolver != null) {
                List<Page> rootpages = getSiteRootPages(resolver);
                for (Page page : rootpages) {
                    updateSchemaAppCDNData(resolver, page);
                }
            } else {
                LOG.debug("Error occurs while getting ResourceResolver");
            }
        } catch (Exception e) {
            LOG.error("Error occurs while read CDN data, Error :: {}"
                    , e.getMessage());
        } finally {
            if (resolver != null && resolver.isLive()) {
                resolver.close();
            }
        }
    }

    /**
     * @param resolver
     * @param page
     */
    private void updateSchemaAppCDNData(ResourceResolver resolver, Page page) {
        try {
            ValueMap configDetailMap = getConfigNodeValueMap(resolver, page);
            if (configDetailMap != null) {
                String accountId = getAccountId(configDetailMap);
                String siteURL = configDetailMap.get("siteURL") != null ? 
                        (String) configDetailMap.get("siteURL") : StringUtils.EMPTY;
                String deploymentMethod = configDetailMap.get("deploymentMethod") != null 
                        ? (String) configDetailMap.get("deploymentMethod")
                                : StringUtils.EMPTY;
                Iterator<Page> childPages = page.listChildren(new PageFilter(), true);
                String endpoint = ConfigurationUtil.getConfiguration(Constants.SCHEMAAPP_DATA_API_ENDPOINT_KEY,
                        Constants.API_ENDPOINT_CONFIG_PID, configurationAdmin, "");
                while (childPages.hasNext()) {
                    processPage(resolver, configDetailMap, accountId, siteURL, deploymentMethod, childPages, endpoint);
                }
            }
        } catch (Exception e) {
            LOG.error("Error in fetching details from SchemaApp CDN Data API", e);
        }
    }

    private void processPage(ResourceResolver resolver, 
            ValueMap configDetailMap, 
            String accountId, 
            String siteURL,
            String deploymentMethod, 
            Iterator<Page> childPages, 
            String endpoint) {

        Object graphJsonData = null;
        Map<String, String> additionalConfigMap = null;
        try {
            final Page child = childPages.next();
            Resource pageResource = child.adaptTo(Resource.class);
            Resource schemaAppRes = resolver.getResource(Constants.DATA);
            String pagePath = siteURL + child.getPath();
            String encodedURL = Base64.getUrlEncoder().encodeToString(pagePath.getBytes());
            if (encodedURL != null && encodedURL.contains("=")) {
                encodedURL = encodedURL.replace("=", "");
            }
            LOG.debug("CDNDataAPIServiceImpl :: endpoint ::{}, "
                    + "pagepath ::{}, encodedURL ::{}", 
                    endpoint,
                    pagePath, encodedURL);
            URL url = getURL(endpoint, accountId, encodedURL);
            Map<String, Object> responseMap = httpGet(url);
            String response = responseMap.containsKey(BODY) 
                    ? responseMap.get(BODY).toString() : StringUtils.EMPTY;
            
            String eTag = responseMap.containsKey(Constants.E_TAG) 
                    ? (String) responseMap.get(Constants.E_TAG) : StringUtils.EMPTY;
            
            String sourceHeader = responseMap.containsKey(Constants.SOURCE_HEADER) 
                    ? (String) responseMap.get(Constants.SOURCE_HEADER) : StringUtils.EMPTY;
            
            String eTagNodeValue = StringUtils.EMPTY;
            
            eTagNodeValue = getETagNodeValue(schemaAppRes, eTagNodeValue);
            LOG.debug("CDN API page path :: {}, eTag request header:: {}, page node eTag :: {}", pagePath, eTag, eTagNodeValue);
            if (eTagNodeValue.equals(eTag) && !deploymentMethod.equals(JAVA_SCRIPT)) {
                return;
            }
            
            additionalConfigMap = new HashMap<>();
            additionalConfigMap.put(Constants.E_TAG, eTag);
            additionalConfigMap.put(Constants.SOURCE_HEADER, sourceHeader);
            
            graphJsonData = convertStringtoJson(pagePath,
                    response);

            if (StringUtils.isNotBlank(deploymentMethod) 
                    && deploymentMethod.equals(JAVA_SCRIPT)) {

                url = getHighlighterURL(endpoint, accountId, encodedURL);
                responseMap = httpGet(url);
                response = responseMap.containsKey(BODY) 
                        ? (String) responseMap.get(BODY) : StringUtils.EMPTY;
                String eTagJavascript = responseMap.containsKey(Constants.E_TAG) 
                        ? (String) responseMap.get(Constants.E_TAG) : StringUtils.EMPTY;
                String eTagNodeValueJavascript = StringUtils.EMPTY;
                
                eTagNodeValueJavascript = getETagNodeValueJavascript(
                        schemaAppRes, eTagNodeValueJavascript);
                if (eTagNodeValue.equals(eTag) && eTagNodeValueJavascript.equals(eTagJavascript) ) {
                    return;
                }
                
                if (!eTagNodeValueJavascript.equals(eTagJavascript)) {
                    graphJsonData = processJavaScriptCDNData(response, graphJsonData);
                    additionalConfigMap.put(Constants.E_TAG_JAVASCRIPT, eTagJavascript);
                }
            }
            if (graphJsonData == null) {
                webhookHandlerService.deleteEntity(child, resolver);
                return;
            }
            save(resolver, configDetailMap,
                    graphJsonData, pageResource, additionalConfigMap);

        } catch (Exception e) {
            LOG.error("Error while reading and processing CDN URL", e);
        }
    }

    private String getETagNodeValue(Resource schemaAppRes,
            String eTagNodeValue) {
        if (schemaAppRes != null) {
            ValueMap vMap = schemaAppRes.getValueMap();
            return vMap.get(Constants.E_TAG) != null
                    ? (String) vMap.get(Constants.E_TAG)
                            : StringUtils.EMPTY;
        }
        return eTagNodeValue;
    }

    private String getETagNodeValueJavascript(Resource schemaAppRes,
            String eTagNodeValueJavascript) {
        if (schemaAppRes != null) {
            ValueMap vMap = schemaAppRes.getValueMap();
            return vMap.get(Constants.E_TAG_JAVASCRIPT) != null
                    ? (String) vMap.get(Constants.E_TAG_JAVASCRIPT)
                            : StringUtils.EMPTY;
        }
        return eTagNodeValueJavascript;
    }

    private Object convertStringtoJson(String pagePath,
            String response) throws JSONException {
        
        Object graphJsonData = null;
        if (StringUtils.isNotBlank(response)) {
            if (response.startsWith("[")) {
                graphJsonData = new JSONArray(response);
            } else {
                graphJsonData = new JSONObject(response);
            }
            LOG.debug("CDN API page path :: {}, response data:: {}", pagePath, response);
        }
        return graphJsonData;
    }

    private void save(ResourceResolver resolver,
            ValueMap configDetailMap, 
            Object graphJsonData, final Resource pageResource, Map<String, String> additionalConfigMap)
            throws JSONException, IOException, RepositoryException,
            ReplicationException {

        if (graphJsonData != null) {
            mapperObject.readValue(graphJsonData.toString(),
                    Object.class);
            webhookHandlerService.savenReplicate(graphJsonData, 
                    resolver,
                    additionalConfigMap, 
                    pageResource,
                    configDetailMap);
        }
    }

    /**
     * Process the JavaScript CDN Data.
     * 
     * @param response
     * @param graphJsonData
     * @return
     */
    private Object processJavaScriptCDNData(
            String response, 
            Object graphJsonData) {
        try {
            if (StringUtils.isNotBlank(response) && response.startsWith("[")) {
                return processJavascriptArrayData(graphJsonData, response);
            } else if (StringUtils.isNotBlank(response)) {
                return processJavascriptSingleJsonObjectData(graphJsonData,
                        response);
            }
        } catch (Exception e) {
            LOG.error("Error while processing Javascript CDN data", e);
        }
        return graphJsonData;
    }

    /**
     * Process the Javascript Single data JsonObject.
     * 
     * @param graphJsonData
     * @param response
     * @return
     * @throws JSONException
     */
    private Object processJavascriptSingleJsonObjectData(Object graphJsonData,
            String response) throws JSONException {
        JSONArray graphJsonDataArray = new JSONArray();
        JSONObject graphJsonDataObject = new JSONObject(response);
        if (graphJsonData instanceof JSONArray) {
            JSONArray array = (JSONArray) graphJsonData;
            for (int i = 0; i < array.length(); i++) {
                graphJsonDataArray.put(array.getJSONObject(i));
            }
            graphJsonDataArray.put(graphJsonDataObject);
        } else {
            graphJsonDataArray.put(graphJsonDataObject);
            if (graphJsonData != null)
                graphJsonDataArray.put(graphJsonData);
        }
        LOG.info("CDN data response:: javascript :: {}", response);
        return graphJsonDataArray;
    }

    private Object processJavascriptArrayData(Object graphJsonData,
            String response) throws JSONException {
        JSONArray graphJsonDataArray = new JSONArray(response);
        if (graphJsonData instanceof JSONArray) {
            JSONArray array = (JSONArray) graphJsonData;
            for (int i = 0; i < array.length(); i++) {
                graphJsonDataArray.put(array.getJSONObject(i));
            }
        } else {
            if (graphJsonData != null)
                graphJsonDataArray.put(graphJsonData);
        }
        LOG.info("CDN data response:: javascript array :: {}", 
                response);
        return graphJsonDataArray;
    }

    private Map<String, Object> httpGet(URL url) throws IOException {

        Map<String, Object> responseMap = new HashMap<>();
        HttpURLConnection connection = getHttpURLConnection(url);
        if (connection != null) {
            connection.setRequestMethod(HttpConstants.METHOD_GET);
            connection.setConnectTimeout(5 * 1000);
            connection.setReadTimeout(7 * 1000);
            int status = connection.getResponseCode();

            if (status == HttpURLConnection.HTTP_OK) {
                String eTag = connection.getHeaderField("ETag");
                responseMap.put(Constants.E_TAG, eTag);
                
                String sourceHeader = connection.getHeaderField("x-amz-meta-source");
                responseMap.put(Constants.SOURCE_HEADER, sourceHeader);
                
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = bufferedReader.readLine()) != null) {
                    content.append(inputLine);
                }
                bufferedReader.close();
                responseMap.put(BODY, content);
            }

            connection.disconnect();
        }
        return responseMap;
    }

    private String getAccountId(ValueMap configDetailMap) {
        String accountId = configDetailMap != null ? 
                (String) configDetailMap.get("accountID") : 
                    StringUtils.EMPTY;
        if (accountId != null && accountId.lastIndexOf('/') > 0) {
            int index = accountId.lastIndexOf('/');
            accountId = accountId.substring(index, accountId.length());
        }
        return accountId;
    }

    private ValueMap getConfigNodeValueMap(ResourceResolver resolver, Page page) {
        ValueMap valueMap = page != null ? page.getProperties() : null;
        if (valueMap != null && valueMap.containsKey(CQ_CLOUDSERVICECONFIGS)) {
            String[] cloudserviceconfigs = (String[]) valueMap.get(CQ_CLOUDSERVICECONFIGS);
            for (String cloudserviceconfig : cloudserviceconfigs) {
                if (cloudserviceconfig.startsWith("/etc/cloudservices/schemaapp/")) {
                    Resource configResource = resolver.getResource(cloudserviceconfig + "/jcr:content");
                    if (configResource != null) {
                        return configResource.getValueMap();
                    }
                }
            }
        }

        return null;
    }

    /**
     * @param resolver
     * @return
     */
    private List<Page> getSiteRootPages(ResourceResolver resolver) {
        List<Page> rootpages = new ArrayList<>();
        Resource contentResource = resolver.getResource("/content");
        if (contentResource == null)
            return rootpages;
        Iterator<Resource> childResourceIterator = contentResource
                .listChildren();
        while (childResourceIterator.hasNext()) {
            final Resource child = childResourceIterator.next();
            if (child != null && child.getResourceType().equals("cq:Page")) {
                Resource jcrcontentResource = child.getChild("jcr:content");
                if (jcrcontentResource != null && jcrcontentResource
                        .getValueMap().get(CQ_CLOUDSERVICECONFIGS) != null) {
                    String[] cloudserviceconfigs = (String[]) jcrcontentResource
                            .getValueMap().get(CQ_CLOUDSERVICECONFIGS);
                    boolean contains = Arrays.stream(cloudserviceconfigs)
                            .anyMatch(s -> s.startsWith(
                                    "/etc/cloudservices/schemaapp"));

                    if (contains) {
                        Page page = child.adaptTo(Page.class);
                        rootpages.add(page);
                    }
                }
            }
        }
        return rootpages;
    }

    public URL getURL(String endpoint, String accountId, String encodedURL) throws MalformedURLException {
        return new URL(endpoint + accountId + "/" + encodedURL);
    }

    public URL getHighlighterURL(String endpoint, String accountId, String encodedURL) throws MalformedURLException {
        return new URL(endpoint + accountId + "/__highlighter_js/" + encodedURL);
    }

    /**
     * This method Get Resource Resolver instance
     * 
     * @return
     */
    public ResourceResolver getResourceResolver() {
        Map<String, Object> param = new HashMap<>();
        param.put(ResourceResolverFactory.SUBSERVICE, "schema-app-service");
        try {
            return resolverFactory.getServiceResourceResolver(param);
        } catch (LoginException e) {
            LOG.error("getResourceResolver :: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public HttpURLConnection getHttpURLConnection(URL url) throws IOException {
        return url != null ? (HttpURLConnection) url.openConnection() : null;
    }

}
