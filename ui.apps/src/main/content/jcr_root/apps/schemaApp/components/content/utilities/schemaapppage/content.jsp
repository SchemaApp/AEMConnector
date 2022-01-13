<%@page contentType="text/html"
            pageEncoding="utf-8"%><%
%><%@include file="/libs/foundation/global.jsp"%><div>

<div>
    <h3>Schema App </h3>
    <ul>
        <li><div class="li-bullet"><strong>API Key: </strong><br><%= properties.get("apiKey", "") %></div></li>
        <li><div class="li-bullet"><strong>Account Id : </strong><br><%= properties.get("accountID", "") %></div></li>
        <li><div class="li-bullet"><strong>AEM Publisher Site URL : </strong><br><%= xssAPI.encodeForHTML(properties.get("siteURL", "")).replaceAll("\\&\\#xa;","<br>") %></div></li>
    </ul>
</div>

