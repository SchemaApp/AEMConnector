CQ.SchemaAppRegistrationService = CQ.SchemaAppRegistrationService || {};
CQ.SchemaAppRegistrationService.showButtonIndicator = function(dialog, isShown) {
    if (!isShown) {
        CQ.Ext.Msg.wait(CQ.I18n.getMessage("Connection successful")).hide();
    } else {
        CQ.Ext.Msg.wait(CQ.I18n.getMessage("Connecting to server..."));
    }
}
CQ.SchemaAppRegistrationService.doConnect = function(dialog) {
    var that = this;
    var apiKey = dialog.find("name", "./apiKey")[0];
    var accountID = dialog.find("name", "./accountID")[0];
    var endpoint = dialog.find("name", "./siteURL")[0];



    this.showButtonIndicator(dialog, true);

    function fieldEmpty(field, msg) {
        if (!field || field.getValue() == "") {
            that.showButtonIndicator(dialog, false);
            CQ.Ext.Msg.alert(CQ.I18n.getMessage("Error"), msg);
            return true;
        }
        return false;
    }

    if (fieldEmpty(apiKey, CQ.I18n.getMessage("Please enter the apiKey.")) ||
        fieldEmpty(accountID, CQ.I18n.getMessage("Please enter the accountID.")) ||
        fieldEmpty(endpoint, CQ.I18n.getMessage("Please select the siteURL."))) {
        return;
    }

    that.showButtonIndicator(dialog, false);

    var akey = encodeURIComponent(apiKey.getValue());
    var aID = accountID.getValue();
    var siteurl = endpoint.getValue();
    var checkUrl = CQ.shared.HTTP.externalize("https://app.schemaapp.com/register/plugin", true);
    siteurl = siteurl.replace(/\/$/, "");


    var data = {
        source: "AdobeExperienceManager",
        url: siteurl,
        webhook: siteurl+"/bin/schemaApp/WebhooksHandler.html"
    };


    $CQ.ajax({
        url: checkUrl,
        type: "POST",
        headers: {
            "Accept": "application/json; charset=utf-8",
            "Content-Type": "application/json; charset=utf-8",
            "x-api-key": akey,
            "x-account-id": aID
        },
        data: JSON.stringify(data),
        dataType: "json",
        success: function(data) {
            console.info(data);
             CQ.cloudservices.getEditOk().enable();
            CQ.Ext.Msg.show({
                "title": CQ.I18n.getMessage("Success"),
                "msg": CQ.I18n.getMessage("Connection tested successfully."),
                "buttons": CQ.Ext.Msg.OK,
                "icon": CQ.Ext.Msg.INFO
            });
        },
        error: function( jqXhr, textStatus, errorThrown ){
        	CQ.Ext.Msg.alert(CQ.I18n.getMessage("Error"), CQ.I18n.getMessage("Could not connect. Please verify credentials."));
    	}
    });
};