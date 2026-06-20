package com.schemaapp.core.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionResponse;
import org.apache.sling.distribution.Distributor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.schemaapp.core.services.impl.FlushServiceImpl;

public class FlushServiceImplTest {

    @InjectMocks
    private FlushServiceImpl flushService;

    @Mock
    private ResourceResolverFactory resolverFactory;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private Distributor distributor;

    @Mock
    private DistributionResponse distributionResponse;
    
    @BeforeEach
    public void setUp() throws LoginException {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);
        
        // Mock ResourceResolver retrieval
        when(resolverFactory.getServiceResourceResolver(anyMap())).thenReturn(resourceResolver);
    }

    @Test
    public void testInvalidatePageJsonSuccess() throws LoginException {
        // Arrange
        when(distributor.distribute(eq("publish"), eq(resourceResolver), any(DistributionRequest.class)))
                .thenReturn(distributionResponse);
        when(distributionResponse.isSuccessful()).thenReturn(true);

        // Act
        flushService.invalidatePageJson("/content/testPage");

        // Assert
        verify(distributor, times(1)).distribute(eq("publish"), eq(resourceResolver), any(DistributionRequest.class));
    }

    @Test
    public void testInvalidatePageJsonFailure() throws LoginException {
        // Arrange
        when(distributor.distribute(eq("publish"), eq(resourceResolver), any(DistributionRequest.class)))
                .thenReturn(distributionResponse);
        when(distributionResponse.isSuccessful()).thenReturn(false);

        // Act
        flushService.invalidatePageJson("/content/testPage");

        // Assert
        verify(distributor, times(1)).distribute(eq("publish"), eq(resourceResolver), any(DistributionRequest.class));
    }

    @Test
    public void testInvalidatePageJsonException() throws LoginException {
        // Arrange
        when(distributor.distribute(eq("publish"), eq(resourceResolver), any(DistributionRequest.class)))
                .thenThrow(new RuntimeException("Distribution failed"));

        // Act
        flushService.invalidatePageJson("/content/testPage");

        // Assert
        verify(distributor, times(1)).distribute(eq("publish"), eq(resourceResolver), any(DistributionRequest.class));
    }

    @Test
    public void testInvalidatePageJsonLoginException() throws LoginException {
        // Arrange
        when(resolverFactory.getServiceResourceResolver(anyMap())).thenThrow(new LoginException("Login failed"));

        // Act and Assert
        assertDoesNotThrow(() -> flushService.invalidatePageJson("/content/testPage"));
        verify(resolverFactory, times(1)).getServiceResourceResolver(anyMap());
    }
}

