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
    var ui = $(window).adaptTo('foundation-ui');
    var contentPath;
	var isInitialized = false;

    $(document).on('foundation-contentloaded', function () {
		if (isInitialized)
			return;
		isInitialized = true;
        contentPath = $('#edit-configuration-properties-form').data('contentPath');
		
		// change the page title in editing mode, need to consider I18N case
		var pageTitle = $('#edit-configuration-properties-form .foundation-layout-wizard2-title').text();
		$('#edit-configuration-properties-form .foundation-layout-wizard2-title').text(pageTitle);
		$('.cq-confadmin-schema-app-connect').on('click', doConnect);
		toggleSubmit();
        $('#edit-configuration-properties-form').on('submit', doSubmit);
        
    });

    // common
    function doSubmit() {
        var parentPath = contentPath.substr(0,contentPath.lastIndexOf('/'));
        $.ajax({
            type: 'GET',
            url: Granite.HTTP.externalize(contentPath + '.1.json'),
            cache: false
        }).done(function (data, textStatus, jqXHR) {
			saveConfig();
        }).fail(function (jqXHR, textStatus, errorThrown) {
			createConfigPage(saveConfig);
        });
        return false;
    }

    function createConfigPage(callback) {
        var createConfigData = {};
        createConfigData.title = $('input[name="jcr:title"]').val();
        var parentPath = contentPath.substr(0,contentPath.lastIndexOf('/')) ;
        createConfigData.parentPath = parentPath;
        createConfigData.cmd = 'createPage';
        createConfigData.template = '/apps/schemaApp/registration/templates/schemaapp';
        createConfigData.label = 'schemaAppConfig';

        $.ajax({
            type: 'POST',
            url: Granite.HTTP.externalize('/bin/wcmcommand'),
            contentType: 'application/x-www-form-urlencoded',
            data: createConfigData,
            cache: false
        }).done(function (data, textStatus, jqXHR) {
			var tempContentPath = $(data).find('#Location').attr('href');
			if (tempContentPath)
				contentPath = tempContentPath;
            callback();
        }).fail(function (jqXHR, textStatus, errorThrown) {
            ui.notify(null, Granite.I18n.get('Failed to save configuration'), 'error');
        });
    }

    function saveConfig() {
        var form = $('#edit-configuration-properties-form')[0];
		var data = $(form).serializeArray();

        $.ajax({
            type: form.method,
            url: Granite.HTTP.externalize(contentPath + '/jcr:content'),
            contentType: form.encType,
            data: $.param(data),
            cache: false
        }).done(function (data, textStatus, jqXHR) {
			location.href = Granite.HTTP.externalize('/apps/schemaApp/utilities/schemaAppcloudconfig.html/etc/cloudservices/schemaapp');
        }).fail(function (jqXHR, textStatus, errorThrown) {
            ui.notify(null, Granite.I18n.get('Failed to save configuration'), 'error');
        });
    }

	

    function toggleSubmit() {
		var incompleteForm = $('#edit-configuration-properties-form input[aria-required="true"]').is(function (index) {
			if (!$(this).val()) {
				return true;
			}
		});

		if (incompleteForm) {
			$('.dmconfig-sa-submit-btn').attr('disabled', 'disabled');
		} else {
			$('.dmconfig-sa-submit-btn').removeAttr('disabled');
		}
    }

    function popupAlert(message) {
        new Coral.Dialog().set({
            id: 'delErrorDialog',
            variant: 'error',
            header: {
                innerHTML: Granite.I18n.get('Error')
            },
            content: {
                innerHTML: '<p>' + message + '</p>'
            },
            footer: {
                innerHTML: '<button is="coral-button" variant="primary" coral-close size="M">' + Granite.I18n.get('Ok') + '</button>'
            }
        }).show();
    }

 function doConnect() {
		var apiKey = $('input[name="./apiKey"]').val();
        var accountID = $('input[name="./accountID"]').val();
        var siteurl = $('input[name="./siteURL"]').val();

        if (!apiKey) {
            popupAlert(Granite.I18n.get('Please provide an Schema App API Key.'));
            return;
        }
        
        if (!accountID) {
            popupAlert(Granite.I18n.get('Please provide the Schema App account\'s Id.'));
            return;
        }
        if (!siteurl) {
            popupAlert(Granite.I18n.get('Please provide the AEM Publisher URL.'));
            return;
        }

        var schemaAppConnectionPath = 'https://app.schemaapp.com/register/plugin';
        ui.wait();
        var schemaAppData = {
            source: "AdobeExperienceManager",
	        url: siteurl,
	        webhook: siteurl+"/bin/schemaApp/WebhooksHandler.html"
        };
        $.ajax({
            type: 'POST',
            url: Granite.HTTP.externalize(schemaAppConnectionPath),
            headers: {
	            "Accept": "application/json; charset=utf-8",
	            "Content-Type": "application/json; charset=utf-8",
	            "x-api-key": apiKey,
	            "x-account-id": accountID
	        },
            data: JSON.stringify(schemaAppData),
            cache: false
        }).done(function (data, textStatus, jqXHR) {
            ui.clearWait();
			var status = data.status;
			
            if (status !== 'Registration Complete' ) {
                cleanupFields();
				ui.notify(null, Granite.I18n.get('Connection has failed'), 'error');
			} else {                 
                 ui.notify(null, Granite.I18n.get('Your site has been registered with Schema App.'), 'Success');
                 toggleSubmit();
            }
        }).fail(function (jqXHR, textStatus, errorThrown) {
            ui.clearWait();
            cleanupFields();
            ui.notify(null, Granite.I18n.get('Connection has failed'), 'error');
        });
    }
    
    function cleanupFields() {
        $('input[name="./apiKey"]').val('');
        $('input[name="./accountID"]').val('');
        $('input[name="./siteURL"]').val('');
    }
})(document, Granite.$);
