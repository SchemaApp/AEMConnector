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
String[] services = pageProperties1.getInherited("cq:cloudserviceconfigs", new String[]{});
final PageJSONDataReaderService schemaAppJSONReader=sling.getService(PageJSONDataReaderService.class);
ResourceResolver resolver = schemaAppJSONReader.getResourceResolver();
ConfigurationManager cfgMgr = resolver.adaptTo(ConfigurationManager.class);
if(cfgMgr != null) {
	String apiKey = null;
	String accountID = null;
	String siteURL = null;
	String deploymentMethod = null;
	Configuration cfg = cfgMgr.getConfiguration("schemaapp", services);
	if (cfg != null) {
		accountID = cfg.get("accountID", "");
		siteURL = cfg.get("siteURL", "");
		deploymentMethod = cfg.get("deploymentMethod", "");
	}
	if (StringUtils.isNotBlank(accountID) && StringUtils.isNotBlank(deploymentMethod) && deploymentMethod.equals("javaScript")) {
		%>
		<script>window.schema_highlighter={output: false, accountId: "<%=accountID%>"}</script>
		<script async src="https://cdn.schemaapp.com/javascript/highlight.js"></script>
		<%
	}
}
%>
<meta data-page-path="<%=request.getScheme()+"://"+request.getServerName()+currentPage.getPath()%>">
<%

String pageURL = request.getRequestURL().toString();

schemaAppJSONReader.init(pageURL);
String source = schemaAppJSONReader.getSource();
String graphData = schemaAppJSONReader.getGraphData();
if (graphData != null) {
%>
<script type="application/ld+json" data-source="<%=source%>" ><%=graphData%></script>
<%}%>