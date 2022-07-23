package com.schemaapp.core.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.day.cq.replication.ReplicationException;
import com.day.cq.wcm.api.Page;
import com.schemaapp.core.services.impl.CDNDataAPIServiceImpl;

import junitx.util.PrivateAccessor;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith({MockitoExtension.class})
class CDNDataAPIServiceTest {

	@Mock
	private ResourceResolver resolver;

	@Mock
	private ResourceResolverFactory resolverFactory;

	@Mock
	private Resource resource;

	@Mock
	private WebhookHandlerService webhookHandlerService;

	@Mock
	private Iterator<Resource> childResourceIterator;

	@Mock
	private ValueMap valueMap;

	@Mock
	private Page page;

	@Mock
	private Iterator<Page> childPages;

	@Mock
	ConfigurationAdmin configurationAdmin;

	@Mock
	Configuration configuration;

	@Spy
	private final CDNDataAPIService cdnDataAPIServiceImpl = new CDNDataAPIServiceImpl();

	@BeforeEach
	void init() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	void testReadCDNData() throws NoSuchFieldException, LoginException, IOException, RepositoryException, JSONException, ReplicationException {
		mockResolver();
		PrivateAccessor.setField(cdnDataAPIServiceImpl, "webhookHandlerService", webhookHandlerService);
		when(resolver.getResource(anyString())).thenReturn(resource);
		mockResource();
		mockValueMap();
		when(resource.adaptTo(Page.class)).thenReturn(page);
		mockPage();
		mockConnection();

		cdnDataAPIServiceImpl.readCDNData();
		verify(webhookHandlerService, times(1)).savenReplicate(any(), any(), any(), any(), any());
	}

	private void mockResource() {
		when(resource.listChildren()).thenReturn(childResourceIterator);
		when(childResourceIterator.hasNext()).thenReturn(true, false);
		when(childResourceIterator.next()).thenReturn(resource);
		when(resource.getResourceType()).thenReturn("cq:Page");
		when(resource.getChild("jcr:content")).thenReturn(resource);
	}

	private void mockValueMap() {
		when(resource.getValueMap()).thenReturn(valueMap);
		when(valueMap.get("cq:cloudserviceconfigs")).thenReturn(new String[]{"/etc/cloudservices/schemaapp/config1"});
		when(valueMap.containsKey("cq:cloudserviceconfigs")).thenReturn(true);
		when(valueMap.get("accountID")).thenReturn("myproject/testaccountID");
		when(valueMap.get("siteURL")).thenReturn("http://localhost:4502");
	}

	private void mockPage() {
		when(page.listChildren(any(), anyBoolean())).thenReturn(childPages);
		when(childPages.next()).thenReturn(page);
		when(childPages.hasNext()).thenReturn(true, false);
		when(page.getPath()).thenReturn("/content/we-retail/us/en/men");
		when(page.getProperties()).thenReturn(valueMap);
	}

	private void mockConnection() throws NoSuchFieldException, IOException, MalformedURLException {
		PrivateAccessor.setField(cdnDataAPIServiceImpl, "configurationAdmin", configurationAdmin);
		Dictionary<String, Object> configurationProperties = new Hashtable<>();
		configurationProperties.put("SchemaAppCDNDataAPIEndpoint", "https://data.schemaapp.com");
		when(configurationAdmin.getConfiguration(anyString())).thenReturn(configuration);
		when(configuration.getProperties()).thenReturn(configurationProperties);

		HttpURLConnection connection = mock(HttpURLConnection.class);
		URL url = new URL("https://data.schemaapp.com");
		doReturn(url).when(cdnDataAPIServiceImpl).getURL(anyString(), anyString(), anyString(), anyString());
		doReturn(connection).when(cdnDataAPIServiceImpl).getHttpURLConnection(any());

		byte[] json = Files.readAllBytes(Paths.get("src/test/resources/AEM/core/services/jsonld.json"));
		InputStream inputStream = new ByteArrayInputStream(json);
		when(connection.getInputStream()).thenReturn(inputStream);
	}

	private void mockResolver() throws NoSuchFieldException, LoginException {
		Map<String, Object> param = new HashMap<>();
		param.put(ResourceResolverFactory.SUBSERVICE, "schema-app-service");
		PrivateAccessor.setField(cdnDataAPIServiceImpl, "resolverFactory", resolverFactory);
		when(resolverFactory.getServiceResourceResolver(param)).thenReturn(resolver);
	}
}
