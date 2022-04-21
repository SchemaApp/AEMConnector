/*
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
 */
(function (document, $) {
    'use strict';

    function createCloudServicePage() {
        var createConfigData = {};
        createConfigData.title = 'Schema App';
        createConfigData.parentPath = '/etc/cloudservices';
        createConfigData.cmd = 'createPage';
        createConfigData.template = '/libs/cq/cloudserviceconfigs/templates/servicepage';
        createConfigData.label = 'schemaapp';

        $.ajax({
            type: 'POST',
            url: Granite.HTTP.externalize('/bin/wcmcommand'),
            contentType: 'application/x-www-form-urlencoded',
            data: createConfigData,
            cache: false
        }).done(function (data, textStatus, jqXHR) {
            addCloudServicePageData();
        }).fail(function (jqXHR, textStatus, errorThrown) {
            ui.notify(null, Granite.I18n.get('You are not allowed to create cloud service configurations'), 'error');
        });
    }
	
    function addCloudServicePageData() {
        var updateConfigData = {};
        updateConfigData.componentReference = 'schemaApp/registration/components/schemaapp';
        updateConfigData.thumbnailPath = '/apps/schemaApp/registration/components/schemaapp/thumbnail.png';

        $.ajax({
            type: 'POST',
            url: Granite.HTTP.externalize('/etc/cloudservices/schemaapp/jcr:content'),
            contentType: 'application/x-www-form-urlencoded',
            data: updateConfigData,
            cache: false
        }).done(function (data, textStatus, jqXHR) {
			activateCloudServicePage();
        }).fail(function (jqXHR, textStatus, errorThrown) {
            ui.notify(null, Granite.I18n.get('You are not allowed to create cloud service configurations'), 'error');
        });
    }
	
    function activateCloudServicePage() {
        var activateConfigData = {};
        activateConfigData.path = '/etc/cloudservices/schemaapp';
        activateConfigData.cmd = 'activate';

        $.ajax({
            type: 'POST',
            url: Granite.HTTP.externalize('/bin/replicate.json'),
            contentType: 'application/x-www-form-urlencoded',
            data: activateConfigData,
            cache: false
        }).done(function (data, textStatus, jqXHR) {
			location.href = Granite.HTTP.externalize('/apps/schemaApp/utilities/schemaAppcloudconfig.html/etc/cloudservices/schemaapp');
        }).fail(function (jqXHR, textStatus, errorThrown) {
            ui.notify(null, Granite.I18n.get('You are not allowed to create cloud service configurations'), 'error');
        });
    }
	
	$.ajax({
		type: 'GET',
		url: Granite.HTTP.externalize('/etc/cloudservices/schemaapp.1.json'),
		cache: false
	}).done(function (data, textStatus, jqXHR) {
		//do nothing on success
	}).fail(function (jqXHR, textStatus, errorThrown) {
		createCloudServicePage(addCloudServicePageData);
	});
	
})(document, Granite.$);
