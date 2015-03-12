Ext.namespace('Ung');
Ext.namespace('Ung.SetupWizard');

// the main json rpc object
var rpc = {};

Ext.define('Ung.SetupWizard.Language', {
    constructor: function( config ) {
        this.languageStore = [];
        var c = 0;
        var languageList = Ung.SetupWizard.CurrentValues.languageList.list;
        for ( c = 0 ; c < languageList.length ; c++ ) {
            var language = languageList[c];
            this.languageStore.push([ language.code, language.name ]);
        }

        this.panel = Ext.create('Ext.container.Container', {
            items: [{
                xtype: 'component',
                html: '<h2 class="wizard-title">'+i18n._( "Language Selection" )+'</h2>'
            }, {
                xtype: 'combo',
                fieldLabel: i18n._('Please select your language'),
                name: "language",
                editable: false,
                validationEvent: 'blur',
                msgTarget: 'side'
                labelWidth: 200,
                store: this.languageStore,
                value: Ung.SetupWizard.CurrentValues.language,
                queryMode: 'local',
                cls: 'small-top-margin'
            }]
        });

        this.card = {
            title: i18n._( "Language" ),
            panel: this.panel,

            onValidate: Ext.bind(this.validateSettings,this)
        };
    },

    validateSettings: function() {
        var rv = Ung.Util.validateItems(this.panel.items.items);
        return rv;
    },

    saveSettings: function( handler ) {
        var language = this.panel.down('combo[name="language"]').getValue();
        rpc.setup.setLanguage( Ext.bind(this.complete,this, [ handler ], true ), language );
    },

    complete: function( result, exception, foo, handler ) {
        if(Ung.Util.handleException(exception, "Unable to save the language")) return;
        // Send the user to the setup wizard.
        parent.location = "index.do";
    },

    enableHandler: function() {
        this.card.onNext = Ext.bind(this.saveSettings, this );
    }
});

Ung.Language = {
    isInitialized: false,
    init: function() {
        if ( this.isInitialized == true ) return;
        this.isInitialized = true;

        JSONRpcClient.toplevel_ex_handler = Ung.Util.rpcExHandler;
        rpc = {};
        rpc.setup = new JSONRpcClient("/setup/JSON-RPC").SetupContext;

        i18n = new Ung.I18N( { "map": {} });

        var language = Ext.create('Ung.SetupWizard.Language',{});

        Ext.get("container").setStyle("width", "800px");
        this.wizard = Ext.create('Ung.Wizard',{
            height: 500,
            width: 800,
            cardDefaults: {
                cls: 'untangle-form-panel'
            },
            cards: [ language.card ],
            disableNext: false,
            renderTo: "container"
        });

        Ext.QuickTips.init();
        this.wizard.goToPage( 0 );

        this.wizard.nextButton.setText( "Next &raquo;" );

        // The on next handler is always called when calling goToPage,
        // this disables it until after starting
        language.enableHandler();
    }
};
