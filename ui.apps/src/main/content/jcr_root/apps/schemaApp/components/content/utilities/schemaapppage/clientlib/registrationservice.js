CQ.SchemaAppRegistrationService = CQ.SchemaAppRegistrationService || {};
CQ.SchemaAppRegistrationService.showButtonIndicator = function(isShown) {
    if (!isShown) {
        CQ.Ext.Msg.wait(CQ.I18n.getMessage("Registration complete")).hide();
    } else {
        CQ.Ext.Msg.wait(CQ.I18n.getMessage("Connecting to server..."));
    }
}

/**
 * doConnect function used to connect Schema App and register the Account.
 * @param  {[type]} dialog [dialog reference object ]
 * 
 */
CQ.SchemaAppRegistrationService.doConnect = function(dialog) {
    var that = this;
    var apiKey = dialog.find("name", "./apiKey")[0];
    var accountID = dialog.find("name", "./accountID")[0];
    var endpoint = dialog.find("name", "./siteURL")[0];



    this.showButtonIndicator(true);

	/**
	 * fieldEmpty function used to field empty or not. if empty, display alert box with error message.
	 * @param  {[type]} field [field object ]
	 * @param  {[type]} msg [Error message to display if field value is empty]
	 * 
	 */
    function fieldEmpty(field, msg) {
        if (!field || field.getValue() == "") {
            that.showButtonIndicator(false);
            CQ.Ext.Msg.alert(CQ.I18n.getMessage("Error"), msg);
            return true;
        }
        return false;
    }

    if (fieldEmpty(apiKey, CQ.I18n.getMessage("Please enter the API Key.")) ||
        fieldEmpty(accountID, CQ.I18n.getMessage("Please enter the Account ID.")) ||
        fieldEmpty(endpoint, CQ.I18n.getMessage("Please select the site URL."))) {
        return;
    }

    that.showButtonIndicator(false);

    var apiKeyValue = encodeURIComponent(apiKey.getValue());
    var accountIdValue = accountID.getValue();
    var siteurl = endpoint.getValue();
    var schemaAppRegistrationEndpoint = CQ.shared.HTTP.externalize("https://app.schemaapp.com/register/plugin", true);
    siteurl = siteurl.replace(/\/$/, "");


    var data = {
        source: "AdobeExperienceManager",
        url: siteurl,
        webhook: siteurl+"/bin/schemaApp/WebhooksHandler.html"
    };


    $CQ.ajax({
        url: schemaAppRegistrationEndpoint,
        type: "POST",
        headers: {
            "Accept": "application/json; charset=utf-8",
            "Content-Type": "application/json; charset=utf-8",
            "x-api-key": apiKeyValue,
            "x-account-id": accountIdValue
        },
        data: JSON.stringify(data),
        dataType: "json",
        success: function(data) {
            CQ.cloudservices.getEditOk().enable();
            CQ.Ext.Msg.show({
                "title": CQ.I18n.getMessage("Success"),
                "msg": CQ.I18n.getMessage("Your site has been registered with Schema App."),
                "buttons": CQ.Ext.Msg.OK,
                "icon": CQ.Ext.Msg.INFO
            });
        },
        error: function( jqXhr, textStatus, errorThrown ){
        	CQ.Ext.Msg.alert(CQ.I18n.getMessage("Error"), CQ.I18n.getMessage("Could not connect. Please verify credentials."));
    	}
    });
};