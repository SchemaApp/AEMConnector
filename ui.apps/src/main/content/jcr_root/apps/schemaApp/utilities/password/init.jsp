<%--
  ADOBE CONFIDENTIAL
  ___________________

  Copyright 2013 Adobe
  All Rights Reserved.

  NOTICE: All information contained herein is, and remains
  the property of Adobe and its suppliers, if any. The intellectual
  and technical concepts contained herein are proprietary to Adobe
  and its suppliers and are protected by all applicable intellectual
  property laws, including trade secret and copyright laws.
  Dissemination of this information or reproduction of this material
  is strictly forbidden unless prior written permission is obtained
  from Adobe.
--%><%
%><%@ include file="/libs/granite/ui/global.jsp" %><%
%><%@ page session="false"
          import="java.util.HashMap,
                  org.apache.sling.api.wrappers.ValueMapDecorator,
                  com.adobe.granite.ui.components.Config,
                  com.adobe.granite.ui.components.Field" %><%

    Config cfg = cmp.getConfig();
    String value = cmp.getValue().val(cmp.getExpressionHelper().getString(cfg.get("value", "")));

    ValueMap vm = new ValueMapDecorator(new HashMap<String, Object>());
    vm.put("value", value.trim().length() > 0 ? "hasValue" : "");

    request.setAttribute(Field.class.getName(), vm);
%>