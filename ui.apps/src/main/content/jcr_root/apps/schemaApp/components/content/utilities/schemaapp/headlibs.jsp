<%@page session="false" import="org.apache.sling.api.resource.Resource,
                org.apache.sling.api.resource.ValueMap,
                org.apache.sling.api.resource.ResourceUtil,
                com.day.cq.wcm.webservicesupport.Configuration,
                com.day.cq.wcm.webservicesupport.ConfigurationManager,
                org.apache.commons.lang.StringUtils" %>
<%@page import="com.schemaapp.core.services.PageJSONDataReaderService"%>
<%@include file="/libs/foundation/global.jsp" %>
<%
String[] services = pageProperties.getInherited("cq:cloudserviceconfigs", new String[]{});
ConfigurationManager cfgMgr = resourceResolver.adaptTo(ConfigurationManager.class);
if(cfgMgr != null) {
	String apiKey = null;
	String accountID = null;
	String siteURL = null;
	Configuration cfg = cfgMgr.getConfiguration("schemaapp", services);
	if (cfg != null) {
		apiKey = cfg.get("apiKey", "");
		accountID = cfg.get("accountID", "");
		siteURL = cfg.get("siteURL", "");
	}
	if (StringUtils.isNotBlank(apiKey) && StringUtils.isNotBlank(accountID)) {
		%>
		<script>window.schema_highlighter={output: false, key:"<%=apiKey%>", accountId: "<%=accountID%>"}</script>
		<script async src="https://cdn.schemaapp.com/javascript/highlight.js"></script>
		<%
	}
}


String pageURL = request.getRequestURL().toString();

final PageJSONDataReaderService schemaAppJSONReader=sling.getService(PageJSONDataReaderService.class);

String graphData = schemaAppJSONReader.getPageData(pageURL);
if (graphData != null) {
%>
<script><%=graphData%></script>

<%}%>