package com.schemaapp.core.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class})
class WebhookEntityResultTest {

	private static final String ID = "https://dell.ca/products/monitors/dellultra30";

	private static final String TYPE = "created";

	private static final String TEST_ERROR_MESSAGE = "Test Error Message";

	@Mock
	private WebhookEntity entity;

	@Test
	void prepareErrorTest() throws Exception {
		WebhookEntityResult result = WebhookEntityResult.prepareError(TEST_ERROR_MESSAGE);
		assertEquals(TEST_ERROR_MESSAGE, result.getError());
	}

	@Test
	void prepareResultTest() throws Exception {
		when(entity.getId()).thenReturn(new String(ID));
		when(entity.getType()).thenReturn(new String(TYPE));
		WebhookEntityResult result = WebhookEntityResult.fromEntity(entity);
		assertEquals(TYPE, result.getType());
		assertEquals(ID, result.getId());
		assertEquals("WebhookEntityResult{id=https://dell.ca/products/monitors/dellultra30, type=created, error=null}", result.toString());
	}

}
