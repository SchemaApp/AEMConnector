package com.schemaapp.core.schedulers;

import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.ScheduledJobInfo;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

@Designate(ocd = SchemaAppDataAPIScheduler.Config.class)
@Component(
		service = SchemaAppDataAPIScheduler.class,
		immediate = true,
		property = {
				"scheduler.runOn=LEADER"
		}
)
public class SchemaAppDataAPIScheduler {

	private final Logger LOG = LoggerFactory.getLogger(SchemaAppDataAPIScheduler.class);

	public static final String AUTOSUGGESTJOBNAME = "SchemaAppDataAPIScheduler";

	@Reference
	private JobManager jobManager;
	
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
		LOG.debug(" :: SchemaAppDataAPIScheduler activate ::");
		enabled = config.enabled();
		schedulerExpression = config.expression();
		schedulerConcurrent = config.concurrent();

		//remove any previously existing scheduled jobs
		removeScheduler();

		ScheduledJobInfo info = jobManager.createJob(AUTOSUGGESTJOBNAME)
				.schedule()
				.cron(schedulerExpression)
				.add();

		if (info == null) {
			LOG.error(" :: SchemaAppDataAPIScheduler activate :: Failed to create scheduled job");
		}
		jobManager.getScheduledJobs(AUTOSUGGESTJOBNAME, Integer.MAX_VALUE, null)
				.forEach(i ->
						LOG.info(" :: SchemaAppDataAPIScheduler activate :: Scheduled job next at: {}", i.getNextScheduledExecution()));
	}

	private void removeScheduler() {
		Collection<ScheduledJobInfo> jobs = jobManager.getScheduledJobs(AUTOSUGGESTJOBNAME, Integer.MAX_VALUE, null);
		jobs.forEach(ScheduledJobInfo::unschedule);
	}

}
