package com.schemaapp.core.schedulers;

import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.schemaapp.core.services.CDNDataAPIService;

@Designate(ocd = SchemaAppDataAPIScheduler.Config.class)
@Component
public class SchemaAppDataAPIScheduler {

	private final Logger LOG = LoggerFactory.getLogger(SchemaAppDataAPIScheduler.class);

	private static final String AUTOSUGGESTJOBNAME = "SchemaAppDataAPIScheduler";

	@Reference
	private Scheduler scheduler;

	@Reference
	private CDNDataAPIService cdnDataAPIService;
	
	@Reference
    private SlingSettingsService slingSettingsService;

	boolean enabled;
	String schedulerExpression;
	boolean schedulerConcurrent;
	
	@ObjectClassDefinition(
			name = "SchemaApp Data API Scheduler",
			description = "Scheduler to create list of valid silicon devices for Devtool Selector Search."
			)
	public @interface Config {
		@AttributeDefinition(name = "Enabled", description = "Enable/disable the scheduler")
		boolean enabled() default true;

		@AttributeDefinition(name = "Cron-job expression", description = "Cron-job expression. Default is every 30 min.")
		String expression() default "0 */30 * ? * *";

		@AttributeDefinition(name = "Concurrent task", description = "Whether or not to schedule this task concurrently")
		boolean concurrent() default false;
	}

	@Activate
	public void activate(final Config config){
		LOG.error(" :: SchemaAppDataAPIScheduler activate ::");
		enabled = config.enabled();
		schedulerExpression = config.expression();
		schedulerConcurrent = config.concurrent();

		ScheduleOptions scheduleOptions = scheduler.EXPR(schedulerExpression);
		scheduleOptions.name(AUTOSUGGESTJOBNAME);
		scheduleOptions.canRunConcurrently(schedulerConcurrent);

		final Runnable autoSuggestSchedulerJob = () -> {
			LOG.error(" :: SchemaAppDataAPIScheduler Runnable ::");
			if(enabled && isPublish()) {
				schedulerJob();
			}
		};
		autoSuggestSchedulerJob.run();
		try {
			scheduler.schedule(autoSuggestSchedulerJob, scheduleOptions);
		} catch (Exception e) {
			LOG.error("Exception executing auto suggest scheduler: {}", e.getMessage());
			autoSuggestSchedulerJob.run();
		}
	}

	public boolean schedulerJob() {
		LOG.error(" :: SchemaAppDataAPIScheduler start ::");
		cdnDataAPIService.readCDNData();
		return enabled;

	}
	
	private boolean isPublish() {
        return this.slingSettingsService.getRunModes().contains("publish");
    }
}
