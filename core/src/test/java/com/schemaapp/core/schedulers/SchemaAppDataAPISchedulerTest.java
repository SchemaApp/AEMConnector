package com.schemaapp.core.schedulers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.settings.SlingSettingsService;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.schemaapp.core.services.CDNDataAPIService;

import junitx.util.PrivateAccessor;

@ExtendWith({MockitoExtension.class})
class SchemaAppDataAPISchedulerTest {

	@InjectMocks
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
	
	@Mock
	private SlingSettingsService slingSettingsService;
	
	Map<String, Object> parameters = new HashMap<>();

	@BeforeEach
	public void setup() throws NoSuchMethodException {
		MockitoAnnotations.initMocks(this);
		context.registerService(SchemaAppDataAPIScheduler.class, schemaAppDataAPIScheduler);
		context.registerService(CDNDataAPIService.class, cdnDataAPIService);
		context.registerService(Scheduler.class, mockScheduler);
		context.registerService(SlingSettingsService.class, slingSettingsService);
		
		ScheduleOptions mockScheduleOptions = mock(ScheduleOptions.class);
		lenient().when(mockScheduler.EXPR(anyString())).thenReturn(mockScheduleOptions);
	}
	
	@Test
    void testSchedulerInit() throws NoSuchFieldException {
        parameters.put("enabled", true);
        parameters.put("scheduler.expression", "0 */30 * ? * *");
        parameters.put("scheduler.concurrent", false);
        context.registerInjectActivateService(schemaAppDataAPIScheduler, parameters);
        PrivateAccessor.setField(schemaAppDataAPIScheduler, "slingSettingsService", slingSettingsService);

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