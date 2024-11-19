package com.schemaapp.core.util;

import org.apache.commons.lang3.StringUtils;

public final class UrlUtils {

    private UrlUtils() {}

    public static String concatSiteUrlPath(String siteUrl, String path) {
        if (StringUtils.endsWith(siteUrl, "/") && StringUtils.startsWith(path, "/")) {
            return StringUtils.removeEnd(siteUrl, "/") + path;
        }
        return siteUrl + path;
    }

}
