package com.schemaapp.core.models;

import com.day.cq.wcm.api.Page;
import com.schemaapp.core.services.PageJSONDataReaderService;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import javax.annotation.PostConstruct;

@Model(
        adaptables = SlingHttpServletRequest.class,
        adapters = SchemaAppHeadlibs.class,
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class SchemaAppHeadlibs {

    @OSGiService
    private PageJSONDataReaderService pageJSONDataReaderService;

    @SlingObject
    private SlingHttpServletRequest request;

    @ScriptVariable
    private Page currentPage;

    private String source;
    private String graphData;
    private String accountID;
    private String deploymentMethod;

    @PostConstruct
    private void initSchemaAppHeadlibs() {
        pageJSONDataReaderService.init(request.getRequestURL().toString());
        source = pageJSONDataReaderService.getSource();
        graphData = pageJSONDataReaderService.getGraphData();
    }

    public String getSource() {
        return source;
    }

    public String getGraphData() {
        return graphData;
    }

    public String getAccountID() {
        return accountID;
    }

    public String getDeploymentMethod() {
        return deploymentMethod;
    }
}
