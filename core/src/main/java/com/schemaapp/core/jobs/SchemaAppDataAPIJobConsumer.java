package com.schemaapp.core.jobs;

import com.schemaapp.core.services.CDNDataProcessor;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static com.schemaapp.core.schedulers.SchemaAppDataAPIScheduler.AUTOSUGGESTJOBNAME;

@Component(
        service = JobConsumer.class,
        property = {
                JobConsumer.PROPERTY_TOPICS + "=" + AUTOSUGGESTJOBNAME,
                "scheduler.concurrent=false",
                "scheduler.runOn=LEADER"
        }
)
public class SchemaAppDataAPIJobConsumer implements JobConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(SchemaAppDataAPIJobConsumer.class);

    @Reference
	private CDNDataProcessor cndDataProcessor;

    @Override
    public JobResult process(Job job) {
        UUID uuid = UUID.randomUUID();
        String uuidAsString = uuid.toString();
        LOG.info(":: SchemaAppDataAPIJobConsumer :: job running - {}", uuidAsString);
        cndDataProcessor.processCDNDataAndUpdateSchema();
        LOG.info(":: SchemaAppDataAPIJobConsumer :: job completed - {}", uuidAsString);
        return JobResult.OK;
    }

}
