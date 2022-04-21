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

    var CLOUDCONFIGS_PATH = 'settings/cloudconfigs';

    $(document).on('foundation-contentloaded', function () {
        $('.create-config').on('click', function (event) {
            var contentPath = $('.foundation-collection').data('foundationCollectionId');
            var createPageUrl = '/apps/schemaApp/utilities/schemaAppcloudconfig/wizard.html' + contentPath;
            createPageUrl = Granite.HTTP.externalize(createPageUrl); //context path support
            location.href = createPageUrl + '/' + new Date().getTime();
        });
        return;
    });
})(document, Granite.$);
