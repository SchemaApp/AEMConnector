package com.schemaapp.core.servlets;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaapp.core.exception.MissingWebhookEntityAttributesException;
import com.schemaapp.core.models.WebhookEntity;
import com.schemaapp.core.models.WebhookEntityResult;
import com.schemaapp.core.services.WebhookHandlerService;
import com.schemaapp.core.util.Validator;

/**
 * The <code>WebhooksHandlerServlet</code> class to handle webhooks API calls.
 * 
 * @author nikhil
 *
 */
@ServiceDescription("Supergroup Permissions Servlet")
@Component(service=Servlet.class,
property={
        "sling.servlet.methods=" + HttpConstants.METHOD_POST,
        "sling.servlet.paths="+ "/bin/schemaApp/WebhooksHandler",
        "sling.servlet.extensions=" + "json",
})
public class WebhooksHandlerServlet extends SlingAllMethodsServlet {

	private static final String INVALID_JSONL_D_MESSAGE = "Invalid JSONL-D Data, Unable to parse.";

	private static final long serialVersionUID = 1L;

	private static final Logger LOG = LoggerFactory.getLogger(WebhooksHandlerServlet.class);
	private static ObjectMapper mapperObject = new ObjectMapper().configure(FAIL_ON_MISSING_CREATOR_PROPERTIES, true);

	private static final JsonFactory FACTORY = new JsonFactory().disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

	@Reference
	transient WebhookHandlerService webhookHandlerService;

	@Override
	public void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
			throws ServletException, IOException {

		WebhookEntityResult rerult = null;
		try {
			final WebhookEntity entity = mapperObject.readValue(request.getReader(), WebhookEntity.class);
			LOG.info("Schema App : WebhooksHandlerServlet : ID - {} , Type - {}", entity.getId(), entity.getType());
			if (Validator.validateEntity(entity)) {
				if (entity.getType().equals("EntityCreated")) rerult = webhookHandlerService.createEntity(entity);
				if (entity.getType().equals("EntityUpdated")) rerult = webhookHandlerService.updateEntity(entity);
				if (entity.getType().equals("EntityDeleted")) rerult = webhookHandlerService.deleteEntity(entity);
			} 
		} catch (LoginException e) {
			LOG.error("Schema App : WebhooksHandlerServlet : Error occurred while creating entity", e);
			response.setStatus(500);	
		} catch (IOException e) {
			LOG.error("Schema App : WebhooksHandlerServlet : Error occurred while parsing JSONL-D Data", e);
			response.setStatus(400);
			rerult = WebhookEntityResult.prepareError(INVALID_JSONL_D_MESSAGE);
		} catch (MissingWebhookEntityAttributesException e) {
			response.setStatus(400);
			rerult = WebhookEntityResult.prepareError(e.getMessage());
		}

		writeJsonResponse(response, rerult);
	}
	
	private void writeJsonResponse(final SlingHttpServletResponse response, final Object object) throws IOException {
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");

		try {
			final JsonGenerator generator = FACTORY.createGenerator(response.getWriter());
			mapperObject.writeValue(generator, object);
		} catch (IOException e) {
			throw new IOException("error writing JSON response", e);
		}
	}

}
