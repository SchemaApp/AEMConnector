<%@page session="false" import="org.apache.sling.api.resource.Resource,
                org.apache.sling.api.resource.ValueMap,
                org.apache.sling.api.resource.ResourceUtil,
                com.day.cq.wcm.webservicesupport.Configuration,
                org.apache.sling.api.resource.ResourceResolver,
                com.day.cq.wcm.webservicesupport.ConfigurationManager,
                com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap,
                org.apache.commons.lang.StringUtils" %>
<%@page import="com.schemaapp.core.services.PageJSONDataReaderService"%>
<%@include file="/libs/foundation/global.jsp" %>
<%
HierarchyNodeInheritanceValueMap pageProperties1 = new HierarchyNodeInheritanceValueMap(resource);
String[] services = inheritedProperties.getInherited("cq:cloudserviceconfigs", new String[]{});
final PageJSONDataReaderService schemaAppJSONReader=sling.getService(PageJSONDataReaderService.class);
ResourceResolver resolver = schemaAppJSONReader.getResourceResolver();
ConfigurationManager cfgMgr = resolver.adaptTo(ConfigurationManager.class);
if(cfgMgr != null) {
	String apiKey = null;
	String accountID = null;
	String siteURL = null;
	Configuration cfg = cfgMgr.getConfiguration("schemaapp", services);
	if (cfg != null) {
		accountID = cfg.get("accountID", "");
		siteURL = cfg.get("siteURL", "");
	}
	if (StringUtils.isNotBlank(apiKey) && StringUtils.isNotBlank(accountID)) {
		%>
		<script>window.schema_highlighter={output: false, accountId: "<%=accountID%>"}</script>
		<script async src="https://cdn.schemaapp.com/javascript/highlight.js"></script>
		<%
	}
}


String pageURL = request.getRequestURL().toString();


String graphData = schemaAppJSONReader.getPageData(pageURL);
if (graphData != null) {
%>
<script type="application/ld+json"><%=graphData%></script>

<%}%>