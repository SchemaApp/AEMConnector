package com.schemaapp.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UrlUtilsTest {

    @Test
    public void testConcatSiteTrailingSlash() {
        String result = UrlUtils.concatSiteUrlPath("https://testurl.com/", "/content/en/us/test");
        assertEquals("https://testurl.com/content/en/us/test", result);
    }

    @Test
    public void testConcatSiteNoTrailingSlash() {
        String result = UrlUtils.concatSiteUrlPath("https://testurl.com", "/content/en/us/test");
        assertEquals("https://testurl.com/content/en/us/test", result);
    }
}
