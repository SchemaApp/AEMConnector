/*************************************************************************
 *
 * ADOBE CONFIDENTIAL
 * __________________
 *
 *  Copyright 2017 Adobe Systems Incorporated
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated and its
 * suppliers and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 **************************************************************************/
package apps.gigya.touchui.datasource;

import com.adobe.cq.sightly.WCMUsePojo;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.Template;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Set;
import com.day.cq.wcm.api.Page;

public class Configuration extends WCMUsePojo {
    private Resource currentResource;

    private final String CONF_ROOT = "/etc/cloudservices/schemaapp";

    @Override
    public void activate() throws Exception {
        currentResource = getResource();
        if (!currentResource.getPath().startsWith(CONF_ROOT)) {
            String suffixPath = getRequest().getRequestPathInfo().getSuffix();
            if (suffixPath != null && suffixPath.startsWith(CONF_ROOT)) {
                currentResource = getResourceResolver().getResource(suffixPath);
            }
        }
    }

    public String getTitle() {
        return getProperties().get("jcr:content/jcr:title", getProperties().get("jcr:title", currentResource.getName()));
    }

    public String getName() {
        return currentResource.getName();
    }

    public boolean hasChildren() {
        for (Resource child : currentResource.getChildren())
            if (child.adaptTo(Page.class) != null)
                return true;
        return false;
    }

    public String getThumbnail() {
        if (isConfiguration(currentResource)) {
            Page page = currentResource.adaptTo(Page.class);
            if (page != null) {
                Template template = page.getTemplate();
                if (template != null) {
                    String templateThubnail = template.getThumbnailPath();
                    if (StringUtils.isNotBlank(templateThubnail)) {
                        return templateThubnail;
                    }
                }
            }
            return "/libs/cq/ui/widgets/themes/default/icons/240x180/page.png";
        }
        return null;
    }

    public Calendar getLastModifiedDate() {
        Page page = currentResource.adaptTo(Page.class);
        if (page != null) {
            return page.getLastModified();
        }
        if (getProperties() != null) {
            return getProperties().get(JcrConstants.JCR_LASTMODIFIED, Calendar.class);
        }
        return null;
    }

    public String getLastModifiedBy() {
        Page page = currentResource.adaptTo(Page.class);
        if (page != null) {
            return page.getLastModifiedBy();
        }
        if (getProperties() != null) {
            return getProperties().get(JcrConstants.JCR_LAST_MODIFIED_BY, String.class);
        }
        return null;
    }

    public Set<String> getQuickactionsRels() {
        Set<String> quickactions = new LinkedHashSet<String>();

        if (isConfiguration(currentResource)) {
            if (hasPermission(Session.ACTION_SET_PROPERTY)) {
                quickactions.add("cq-confadmin-actions-properties-activator");
            }

            if (hasPermission(Session.ACTION_REMOVE)) {
                quickactions.add("cq-confadmin-actions-delete-activator");
            }
        }

        return quickactions;
    }

    private boolean hasPermission(String action) {
        try {
            return getResourceResolver().adaptTo(Session.class).hasPermission(currentResource.getPath(), action);
        } catch (RepositoryException e) {
            return false;
        }
    }

    private boolean isConfiguration(Resource resource) {
        return resource != null && resource.adaptTo(Page.class) != null;
    }
}
