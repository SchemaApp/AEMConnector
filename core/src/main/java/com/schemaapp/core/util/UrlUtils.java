package com.schemaapp.core.util;

import org.apache.commons.lang3.StringUtils;

public final class UrlUtils {

    private UrlUtils() {}

    /**
     * Concatenates the site URL (the domain) and a path to a page, removing a trailing / if one
     * exists after the site URL.
     * @param siteUrl The site URL
     * @param path The path to the page
     * @return String of the concatenated siteUrl and path.
     */
    public static String concatSiteUrlPath(String siteUrl, String path) {
        if (StringUtils.endsWith(siteUrl, "/") && StringUtils.startsWith(path, "/")) {
            return StringUtils.removeEnd(siteUrl, "/") + path;
        }
        return siteUrl + path;
    }

}
