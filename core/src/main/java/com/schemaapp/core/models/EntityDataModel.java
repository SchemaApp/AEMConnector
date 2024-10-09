package com.schemaapp.core.models;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.cq.export.json.ComponentExporter;


//Define the EntityDataModel class, which adapts the SlingHttpServletRequest and exports JSON data
@Model(
 adaptables = SlingHttpServletRequest.class, 
 adapters = ComponentExporter.class, 
 defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL, 
 resourceType = EntityDataModel.RESOURCE_TYPE
)
@Exporter(name = "jackson", extensions = "json")
public class EntityDataModel implements ComponentExporter {

    // Define the resource type for this model
    protected static final String RESOURCE_TYPE = "schemaApp/components/content/entitydata";

    // Properties for the entity data model, injected from the resource using @ValueMapValue annotation
    @ValueMapValue
    private String entity;

    @ValueMapValue
    private String siteURL;

    @ValueMapValue
    private String accountId;

    @ValueMapValue
    private String deploymentMethod;

    @ValueMapValue
    private String eTag;

    @ValueMapValue
    private String eTagJavascript;

    // This method returns the resource type, which will be included in the exported JSON as `:type`
    @Override
    public String getExportedType() {
        // Returns the resource type defined above
        return RESOURCE_TYPE;
    }

    // Getter methods for the properties

    // Returns the entity
    public String getEntity() {
        return entity;
    }

    // Returns the site URL
    public String getSiteURL() {
        return siteURL;
    }

    // Returns the account ID
    public String getAccountId() {
        return accountId;
    }

    // Returns the deployment method
    public String getDeploymentMethod() {
        return deploymentMethod;
    }

    // Returns the eTag
    public String geteTag() {
        return eTag;
    }

    // Returns the JavaScript-specific eTag
    public String geteTagJavascript() {
        return eTagJavascript;
    }
}