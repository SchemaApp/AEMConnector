# Using Event Handlers to Clear CDN Caches
The Schema App will publish the jcr:content/schemaapp node on pages as they are returned by the API so they can be embedded within the content to be rendered. If your organization has content cached via a CDN that sits in front of Dispatcher, it may be necessary to implement a cache clearing solution within AEM.

This can be best handled by using an Event Handler. A couple of examples can be found below:

## When Using AEM 6.5.X  
These instances use Replication to publish content between author and publish environments, so the handler that is implemented will need to listen to Replication events.

```java
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

@Component(
        service = EventHandler.class,
        property = {
                EventConstants.EVENT_TOPIC + "=" + ReplicationAction.EVENT_TOPIC
        }
)
public class ReplicationEventHandler implements EventHandler {

    private static final String SCHEMA_NODE = "jcr:content/schemaapp";

    @Override
    public void handleEvent(Event event) {
        ReplicationAction action = ReplicationAction.fromEvent(event);

        if (action.getType() == ReplicationActionType.ACTIVATE) {
            String path = action.getPath();
            if (path.endsWith(SCHEMA_NODE)) {
                /*
                 * TODO: Implement call to Sling Job or OSGi Service
                 *  that will trigger CDN cache clear for the containing page
                 */
            }
        }
    }
}
```

## When Using AEMaaCS  
As AEM as a Cloud Service does not utilize Replication, but instead relies on Sling Content Distribution to publish content, a slightly different Event Handler will need to be implemented.

```java
import org.apache.commons.lang3.Strings;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import static org.apache.sling.distribution.event.DistributionEventProperties.DISTRIBUTION_PATHS;
import static org.apache.sling.distribution.event.DistributionEventProperties.DISTRIBUTION_TYPE;
import static org.apache.sling.distribution.event.DistributionEventTopics.AGENT_PACKAGE_DISTRIBUTED;

@Component(
        service = EventHandler.class,
        property = {
                EventConstants.EVENT_TOPIC + "=" + AGENT_PACKAGE_DISTRIBUTED
        }
)
public class DistributionEventHandler implements EventHandler {

    private static final String ADD_TYPE = "ADD";
    private static final String SCHEMA_NODE = "jcr:content/schemaapp";

    @Override
    public void handleEvent(Event event) {
        String[] paths = (String[]) event.getProperty(DISTRIBUTION_PATHS);
        String type = (String) event.getProperty(DISTRIBUTION_TYPE);

        if (Strings.CS.equals(type, ADD_TYPE) && paths != null) {
            for (String path : paths) {
                if (path.endsWith(SCHEMA_NODE)) {
                    /*
                     * TODO: Implement call to Sling Job or OSGi Service
                     *  that will trigger CDN cache clear for the containing page
                     */
                }
            }
        }
    }
}
```
