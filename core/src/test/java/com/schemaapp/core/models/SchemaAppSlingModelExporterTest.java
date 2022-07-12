package com.schemaapp.core.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

@ExtendWith({ MockitoExtension.class, AemContextExtension.class})
class SchemaAppSlingModelExporterTest {

	@Rule
	public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

	@InjectMocks
	private SchemaAppSlingModelExporter schemaAppSlingModelExporter;

	@BeforeEach
	public void setup() throws NoSuchMethodException {
		MockitoAnnotations.initMocks(this);
		context.addModelsForClasses(SchemaAppSlingModelExporter.class);
		context.load().json("/AEM/core/models/schemaappdata.json", "/content");
	}

	@Test
	void test_entitydata() {
		context.currentResource("/content/schemaapp");
		schemaAppSlingModelExporter = context.request().adaptTo(SchemaAppSlingModelExporter.class);
		String actual = schemaAppSlingModelExporter.getEntity();

		assertEquals("test", actual);
	}
}
