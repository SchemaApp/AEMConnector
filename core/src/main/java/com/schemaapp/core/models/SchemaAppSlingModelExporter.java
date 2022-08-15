package com.schemaapp.core.models;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.ExporterOption;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

/**
 * Excellent documentation: http://sling.apache.org/documentation/bundles/models.html
 *
 * For the Sling Models to be picked up; Ensure the packages containing Sling Models are listed in the Bundle pom.xml
 *
 * <Sling-Model-Packages>
 *    com.adobe.acs.samples.models
 * </Sling-Model-Packages>
 *
 * ************************************************************************
 *
 *  The "adaptables" for a Sling Model is key element.
 *
 *  All the injected fields are looked up via a set of Injectors
 *  > http://sling.apache.org/documentation/bundles/models.html#available-injectors
 *  > Ensure you are using the latest Sling Model API and Impl bundles for access
 *    to the latest and greatest Injectors
 */

/** THIS SAMPLE TARGETS SLING MODELS v1.3+ **/

@Model(
        // This must adapt from a SlingHttpServletRequest, since this is invoked directly via a request, and not via a resource.
        // If can specify Resource.class as a second adaptable as needed
        adaptables = { SlingHttpServletRequest.class },
        // The resourceType is required if you want Sling to "naturally" expose this model as the exporter for a Resource.
        resourceType = "schemaApp/components/content/entitydata",
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
// name = the registered name of the exporter
// extensions = the extensions this exporter is registered to
// selector = defaults to "model", can override as needed; This is helpful if a single resource needs 2 different JSON renditions
@Exporter(name = "jackson", extensions = "json", options = {
        /**
         * Jackson options:
         * - Mapper Features: http://static.javadoc.io/com.fasterxml.jackson.core/jackson-databind/2.8.5/com/fasterxml/jackson/databind/MapperFeature.html
         * - Serialization Features: http://static.javadoc.io/com.fasterxml.jackson.core/jackson-databind/2.8.5/com/fasterxml/jackson/databind/SerializationFeature.html
         */
        @ExporterOption(name = "MapperFeature.SORT_PROPERTIES_ALPHABETICALLY", value = "true"),
        @ExporterOption(name = "SerializationFeature.WRITE_DATES_AS_TIMESTAMPS", value="false")
})
/**
 * For Jackson Annotations: https://github.com/FasterXML/jackson-annotations/wiki/Jackson-Annotations
 */
public class SchemaAppSlingModelExporter {

    // Inject a fields whose property name DOES match the model field name
    @ValueMapValue
    @Optional
    private String entity;

	public String getEntity() {
		return entity;
	}
}
