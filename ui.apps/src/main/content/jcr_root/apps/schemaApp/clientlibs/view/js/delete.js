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
    $(document).on('click', '.cq-confadmin-actions-delete-activator', deleteConfigMessage);

    function createEl(name) {
        return $(document.createElement(name));
    }

    function deleteConfigMessage() {
        var message = createEl('div');
        var intro = createEl('p').appendTo(message);
        var selections = $('.foundation-selections-item');
        if (selections.length === 1) {
            intro.text(Granite.I18n.get('You are going to delete the following item:'));
        } else {
            intro.text(Granite.I18n.get('You are going to delete the following {0} items:', selections.length));
        }
        var list = [];
        var maxCount = Math.min(selections.length, 12);
        for (var i = 0, ln = maxCount; i < ln; i++) {
            var title = $(selections[i]).find('.foundation-collection-item-title').text();
            list.push(createEl('b').text(title).prop('outerHTML'));
        }
        if (selections.length > maxCount) {
            list.push('&#8230;'); // &#8230; is ellipsis
        }

        createEl('p').html(list.join('<br>')).appendTo(message);

        var ui = $(window).adaptTo('foundation-ui');

        var paths = [];
        var items = $('.foundation-selections-item');
        if (items.length) {
            items.each(function () {
                var item = $(this);
                var itemPath = item.data('foundation-collection-item-id');
                paths.push(itemPath);
            });

            ui.prompt(Granite.I18n.get('Delete'), message.html(), 'notice', [{
                text: Granite.I18n.get('Cancel')
            }, {
                text: Granite.I18n.get('Delete'),
                warning: true,
                handler: function () {
                    deleteConfig(paths, false);
                }
            }]);
        }
    }

    function deleteConfig(paths,force) {
        var ui = $(window).adaptTo("foundation-ui");
        ui.wait();
        $.ajax({
            url: Granite.HTTP.externalize('/bin/wcmcommand'),
            type: 'POST',
            data: {
                _charset_: 'UTF-8',
                cmd: 'deletePage',
                path: paths,
                force: force,
                checkChildren: true
            },
            success: function () {
                location.reload();
            },statusCode: {
                412: function () {
                    ui.clearWait();
                    if (paths.length === 1) {
                        $.ajax({
                            url: Granite.HTTP.externalize('/bin/wcm/references.json'),
                            type: 'GET',
                            data: {
                                _charset_: 'UTF-8',
                                path: paths[0],
                                predicate: 'wcmcontent'
                            },
                            success: function (responseText) {
                                var times = responseText["pages"].length;
                                showForceDialogue(paths, times)
                            }
                        });
                    }
                    else {
                        showForceDialogue(paths);
                    }
                }
            },
            error: function(response) {
                ui.clearWait();
                if (response.status === 412) {
                    // Ignore. Already Handled
                    return;
                }
                new Coral.Dialog().set({
                    id: "delErrorDialogConf",
                    variant: "error",
                    header: {
                        innerHTML: Granite.I18n.get("Error")
                    },
                    content: {
                        innerHTML: "<p>" + Granite.I18n.get("Failed to delete.") + "</p>"
                    },
                    footer: {
                        innerHTML: '<button is="coral-button" variant="primary" ' +
                        'coral-close size="M">' + Granite.I18n.get("Ok") + "</button>"
                    }
                }).show();

            }

        });
    }
    function showForceDialogue(paths,times) {
        var content;
        if (paths.length === 1) {
            if (times === 1) {
                content = Granite.I18n.get("This item is referenced once.");
            } else if (times > 1) {
                content = Granite.I18n.get("This item is referenced {0} times.",
                    times);
            }
        } else {
            content = Granite.I18n.get("One or more item(s) are referenced.");
        }
        var forceDelDialog = new Coral.Dialog().set(({
            id: "forceDelDialogConf",
            variant: "error",
            header: {
                innerHTML: Granite.I18n.get("Force Delete")
            },
            content: {
                innerHTML: content
            },
            footer: {
                innerHTML: '<button is="coral-button" coral-close size="M">' +
                Granite.I18n.get("Cancel") + "</button>"
            }
        }));
        var footer = forceDelDialog.querySelector("coral-dialog-footer");

        var forceDeleteButton = new Coral.Button().set({
            label: {
                innerHTML: Granite.I18n.get("Delete")
            },
            variant: "warning"
        });
        footer.appendChild(forceDeleteButton).off("click").on("click", function () {
            deleteConfig(paths, true);
            forceDelDialog.hide();
        });

        forceDelDialog.show();
    }
})(document, Granite.$);
