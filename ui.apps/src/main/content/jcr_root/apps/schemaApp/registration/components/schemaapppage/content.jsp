<%@page contentType="text/html"
            pageEncoding="utf-8"%><%
%><%@include file="/libs/foundation/global.jsp"%><div>
<%@include file="/libs/cq/cloudserviceconfigs/components/configpage/init.jsp"%>
<%@include file="/libs/cq/cloudserviceconfigs/components/configpage/hideeditok.jsp"%>
<cq:includeClientLib categories="cq.personalization" />
<div>
    <h3>Schema App General Settings</h3>
    <ul>
        <li><div class="li-bullet"><strong>API Key: </strong><br><%=  xssAPI.encodeForHTML(properties.get("apiKey", "")) %></div></li>
        <li><div class="li-bullet"><strong>Account Id : </strong><br><%=  xssAPI.encodeForHTML(properties.get("accountID", "")) %></div></li>
        <li><div class="li-bullet"><strong>AEM Publisher Site URL : </strong><br><%= xssAPI.encodeForHTML(properties.get("siteURL", "")).replaceAll("\\&\\#xa;","<br>") %></div></li>
    </ul>
</div>

