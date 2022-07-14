package com.schemaapp.core.schedulers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.schemaapp.core.services.CDNDataAPIService;

import junitx.util.PrivateAccessor;

@ExtendWith({MockitoExtension.class})
class SchemaAppDataAPISchedulerTest {

	SchemaAppDataAPIScheduler schemaAppDataAPIScheduler = new SchemaAppDataAPIScheduler();

	@Rule
	public SlingContext context = new SlingContext();

	@Mock
	ConfigurationAdmin configurationAdmin;

	@Mock
	Configuration configuration;

	@Mock
	private CDNDataAPIService cdnDataAPIService;

	@Mock
	private Scheduler mockScheduler;

	Map<String, Object> parameters = new HashMap<>();

	@BeforeEach
	public void setup() throws NoSuchMethodException {
		MockitoAnnotations.initMocks(this);
		context.registerService(SchemaAppDataAPIScheduler.class, schemaAppDataAPIScheduler);
		context.registerService(CDNDataAPIService.class, cdnDataAPIService);
		context.registerService(Scheduler.class, mockScheduler);

		ScheduleOptions mockScheduleOptions = mock(ScheduleOptions.class);
		lenient().when(mockScheduler.EXPR(anyString())).thenReturn(mockScheduleOptions);
	}
	
	@Test
    void testSchedulerInit() {
        parameters.put("enabled", true);
        parameters.put("scheduler.expression", "0 */30 * ? * *");
        parameters.put("scheduler.concurrent", false);
        context.registerInjectActivateService(schemaAppDataAPIScheduler, parameters);

        assertTrue(schemaAppDataAPIScheduler.enabled);
        assertEquals("0 */30 * ? * *", schemaAppDataAPIScheduler.schedulerExpression);
        assertFalse(schemaAppDataAPIScheduler.schedulerConcurrent);
    }

    @Test
    void testSchedulerJob() throws NoSuchFieldException, LoginException {
        PrivateAccessor.setField(schemaAppDataAPIScheduler, "cdnDataAPIService", cdnDataAPIService);

        boolean result = schemaAppDataAPIScheduler.schedulerJob();
        assertFalse(result);
    }


}