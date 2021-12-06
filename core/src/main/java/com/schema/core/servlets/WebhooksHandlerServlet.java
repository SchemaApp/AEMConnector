package com.schema.core.servlets;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schema.core.models.WebhookEntity;
import com.schema.core.models.WebhookEntityResult;
import com.schema.core.services.WebhookHandlerService;

@Component(service = Servlet.class, configurationPolicy = ConfigurationPolicy.OPTIONAL)
@SlingServletPaths("/bin/schemaApp/WebhooksHandler")
@ServiceDescription("Supergroup Permissions Servlet")
public class WebhooksHandlerServlet extends SlingAllMethodsServlet {

	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(WebhooksHandlerServlet.class);
	private static ObjectMapper MAPPER = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
	private static final JsonFactory FACTORY = new JsonFactory().disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

	@Reference
	transient WebhookHandlerService webhookHandlerService;

	@Override
	public void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
			throws ServletException, IOException {

		final WebhookEntity entity = MAPPER.readValue(request.getReader(), WebhookEntity.class);
		WebhookEntityResult rerult = null;
		LOG.info("Schema App : WebhooksHandlerServlet : ID - {} , Type - {}", entity.getId(), entity.getType());
		try {
			rerult = webhookHandlerService.createEntity(entity);
		} catch (LoginException e) {
			LOG.error("Schema App : WebhooksHandlerServlet : Error occurred white creating entity", e);
			response.setStatus(500);	
		}

		writeJsonResponse(response, rerult);
	}

	private void writeJsonResponse(final SlingHttpServletResponse response, final Object object) throws IOException {
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");

		try {
			final JsonGenerator generator = FACTORY.createGenerator(response.getWriter());
			MAPPER.writeValue(generator, object);
		} catch (IOException e) {
			throw new IOException("error writing JSON response", e);
		}
	}

}
