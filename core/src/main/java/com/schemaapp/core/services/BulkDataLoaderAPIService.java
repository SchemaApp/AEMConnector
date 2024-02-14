package com.schemaapp.core.services;

import org.apache.sling.api.resource.ResourceResolver;

import com.schemaapp.core.models.SchemaAppConfig;

public interface BulkDataLoaderAPIService {

    public void fetchAndProcessPaginatedData(SchemaAppConfig config, ResourceResolver resolver);
}
