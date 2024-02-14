package com.schemaapp.core.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import com.day.cq.search.QueryBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.schemaapp.core.models.SchemaAppConfig;
import com.schemaapp.core.services.impl.CDNHandlerServiceImpl;
import com.schemaapp.core.util.Constants;

@ExtendWith({ MockitoExtension.class})
public class CDNHandlerServiceTest {

    @Spy
    @InjectMocks
    private CDNHandlerServiceImpl cdnHandlerService;

    @Mock
    private ResourceResolver resolver;

    @Mock
    private Resource urlResourceNode;

    @Mock
    private Node pageNode;

    @Mock
    private Session session;

    @Mock
    private SchemaAppConfig config;

    @Mock
    private Map<String, String> additionalConfigMap;

    @Mock
    private FlushService flushService;


    @BeforeEach
    public void setUp() throws RepositoryException {
        SlingContext context = new SlingContext(ResourceResolverType.JCR_MOCK);
        context.registerService(QueryBuilder.class, mock(QueryBuilder.class));
        context.registerService(FlushService.class, mock(FlushService.class));
        context.registerService(Replicator.class, mock(Replicator.class));
    }

    @Test
    public void testSavenReplicate() throws RepositoryException, JsonProcessingException, JSONException, PersistenceException, ReplicationException {
        String dataPath = "/some/path";
        String flushedPath = dataPath + "/" + Constants.DATA;
        Node dataNode = mock(Node.class);
        when(urlResourceNode.adaptTo(Node.class)).thenReturn(pageNode);
        when(resolver.adaptTo(Session.class)).thenReturn(session);
        when(urlResourceNode.getPath()).thenReturn(dataPath);
        when(urlResourceNode.adaptTo(Node.class)).thenReturn(pageNode);
        when(pageNode.addNode(eq(Constants.DATA), eq(JcrConstants.NT_UNSTRUCTURED))).thenReturn(dataNode);
        Object jsonGraphData = "[{\"@type\":[\"Article\"],\"@id\":\"https://publish-p62138-e507792.adobeaemcloud.com/content/schemaapp/en/article/sample-article-1.html#Article\",\"@context\":{\"@vocab\":\"http://schema.org/\",\"kg\":\"http://g.co/kg\"},\"url\":\"https://publish-p62138-e507792.adobeaemcloud.com/content/schemaapp/en/article/sample-article-1.html\",\"about\":[{\"@id\":\"https://publish-p62138-e507792.adobeaemcloud.com/content/schemaapp/en/article\"}],\"archivedAt\":\"New Fixed Property\",\"accessMode\":\"Highlighter JavaScript\",\"identifier\":[{\"@type\":\"PropertyValue\",\"@id\":\"https://publish-p62138-e507792.adobeaemcloud.com/content/schemaapp/en/article/sample-article-1.html#Article_identifier_PropertyValue\",\"name\":\"InternalUrl\",\"value\":\"https://publish-p62138-e507792.adobeaemcloud.com/content/schemaapp/en/article/sample-article-1\"}],\"headline\":\"Subtitle\",\"name\":\"Sample Article 1\",\"description\":\"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Duis ac pulvinar erat. In nunc orci, tincidunt a enim eu, malesuada pharetra felis. Suspendisse ut lectus id lorem vulputate scelerisque aliquam vel arcu. Aliquam elementum purus ut ornare aliquam. Phasellus rhoncus auctor velit. Etiam sollicitudin enim in tincidunt dignissim. In lacinia vulputate varius. Curabitur et accumsan justo. Pellentesque tristique malesuada risus vitae faucibus. Integer commodo dui nisl, sit amet viverra velit dapibus a.\"},{\"@context\":\"http://schema.org\",\"@type\":\"Thing\",\"name\":\"article2\",\"description\":\"my test data tttt\",\"additionalType\":\"my test data eeee\",\"identifier\":\"my test data uuuu\",\"disambiguatingDescription\":\"my test data yyyy\",\"alternateName\":\"my test data rrrr\",\"@id\":\"https://publish-p62138-e507792.adobeaemcloud.com/content/schemaapp/en/article\"}]";
        cdnHandlerService.savenReplicate(jsonGraphData, resolver, additionalConfigMap, urlResourceNode, config);

        // Verify that the methods were called with the expected arguments
        verify(cdnHandlerService).createDataNode(pageNode);
        verify(resolver).commit();
        verify(session).save();
        verify(flushService).invalidatePageJson(flushedPath);
    }

    @Test
    public void testSaveGraphDatatoNode() throws RepositoryException, JSONException {
        Node pageNode = mock(Node.class);
        Object jsonGraphData = new JSONObject("{\"key\":\"value\"}");

        cdnHandlerService.saveGraphDatatoNode(jsonGraphData, pageNode);

        verify(pageNode).setProperty(eq(Constants.ENTITY), anyString());
    }

    @Test
    public void testCreateDataNode() throws RepositoryException {
        Node node = mock(Node.class);
        Node dataNode = mock(Node.class);

        when(node.hasNode(Constants.DATA)).thenReturn(false);
        when(node.addNode(eq(Constants.DATA), eq(JcrConstants.NT_UNSTRUCTURED))).thenReturn(dataNode);

        Node result = cdnHandlerService.createDataNode(node);

        assertNotNull(result);
        assertEquals(dataNode, result);

        verify(dataNode).setProperty(eq(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY), eq("schemaApp/components/content/entitydata"));
    }

    @Test
    public void testSavePagePathsToNode() throws PersistenceException {
        ResourceResolver resolver = mock(ResourceResolver.class);
        Resource parentNode = mock(Resource.class);
        ModifiableValueMap properties = mock(ModifiableValueMap.class);

        when(resolver.getResource(anyString())).thenReturn(parentNode);
        when(parentNode.adaptTo(ModifiableValueMap.class)).thenReturn(properties);

        List<String> pagePaths = Arrays.asList("/path1", "/path2");
        cdnHandlerService.savePagePathsToNode(resolver, "/parentPath", pagePaths);

        verify(properties).put(eq("pagePaths"), argThat(array -> Arrays.equals(pagePaths.toArray(), (String[]) array)));
    }
}
