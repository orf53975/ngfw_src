Ext.define('Ung.view.extra.DevicesController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.devices',

    control: {
        '#': {
            afterrender: 'getDevices',
            deactivate: 'onDeactivate'
        }
    },

    onDeactivate: function (view) {
        view.destroy();
    },

    getDevices: function () {
        var me = this;
        me.getView().setLoading(true);
        Rpc.asyncData('rpc.deviceTable.getDevices')
            .then(function(result) {
                me.getView().setLoading(false);
                Ext.getStore('devices').loadData(result.list);
            });
    },

    saveDevices: function () {
        var me = this, store = me.getView().down('ungrid').getStore(), list = [];

        me.getView().query('ungrid').forEach(function (grid) {
            var store = grid.getStore();
            if (store.getModifiedRecords().length > 0 ||
                store.getNewRecords().length > 0 ||
                store.getRemovedRecords().length > 0 ||
                store.isReordered) {
                store.each(function (record) {
                    if (record.get('markedForDelete')) {
                        record.drop();
                    }
                });
                store.isReordered = undefined;
                list = Ext.Array.pluck(store.getRange(), 'data');
            }
        });


        me.getView().setLoading(true);
        Rpc.asyncData('rpc.deviceTable.setDevices', {
            javaClass: 'java.util.LinkedList',
            list: list
        }).then(function(result, ex) {
             me.getDevices();
        }, function (ex) {
            Util.exceptionToast(ex);
        }).always(function () {
            me.getView().setLoading(false);
        });
   }

});