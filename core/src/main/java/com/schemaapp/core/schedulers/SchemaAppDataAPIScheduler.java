package com.schemaapp.core.schedulers;

import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
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

		@AttributeDefinition(name = "Cron-job expression", description = "Cron-job expression. Default is every 6 hours.")
		String expression() default "0 0 0 * * *";

		@AttributeDefinition(name = "Concurrent task", description = "Whether or not to schedule this task concurrently")
		boolean concurrent() default false;
	}

	@Activate
	public void activate(final Config config){
		enabled = config.enabled();
		schedulerExpression = config.expression();
		schedulerConcurrent = config.concurrent();

		ScheduleOptions scheduleOptions = scheduler.EXPR(schedulerExpression);
		scheduleOptions.name(AUTOSUGGESTJOBNAME);
		scheduleOptions.canRunConcurrently(schedulerConcurrent);

		final Runnable autoSuggestSchedulerJob = () -> {
			if(enabled) {
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
		
		cdnDataAPIService.readCDNData();
		return enabled;

	}
}
