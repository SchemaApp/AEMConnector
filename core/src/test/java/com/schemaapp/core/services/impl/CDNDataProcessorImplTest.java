package com.schemaapp.core.services.impl;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.resourceresolver.MockValueMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CDNDataProcessorImplTest {

    @InjectMocks
    private CDNDataProcessorImpl cdnDataProcessor;
    
    @Mock
    private Resource resource;

    @Test
    void testExtractAccountId() {
        assertEquals("", cdnDataProcessor.extractAccountId(null));

        ValueMap valueMap = new MockValueMap(resource);
        assertEquals("", cdnDataProcessor.extractAccountId(valueMap));

        valueMap.put("accountID", "http://schemaapp.com/db/AEMTest");
        assertEquals("AEMTest", cdnDataProcessor.extractAccountId(valueMap));

        valueMap.put("accountID", "https://schemaapp.com/db/AEMTest");
        assertEquals("AEMTest", cdnDataProcessor.extractAccountId(valueMap));

        valueMap.put("accountID", "http://schemaapp.com/db/AcmeDataTest/AEMTest");
        assertEquals("AcmeDataTest/AEMTest", cdnDataProcessor.extractAccountId(valueMap));
    }
}
