Ext.define('Ung.cmp.Grid', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.ungrid',

    controller: 'ungrid',

    actions: {
        add: { text: 'Add'.t(), iconCls: 'fa fa-plus-circle fa-lg', handler: 'addRecord' },
        addInline: { text: 'Add'.t(), iconCls: 'fa fa-plus-circle fa-lg', handler: 'addRecordInline' },
        import: { text: 'Import'.t(), iconCls: 'fa fa-arrow-down', handler: 'importData' },
        export: { text: 'Export'.t(), iconCls: 'fa fa-arrow-up', handler: 'exportData' },
        replace: { text: 'Import'.t(), iconCls: 'fa fa-arrow-down', handler: 'replaceData' },
        // moveUp: { iconCls: 'fa fa-chevron-up', tooltip: 'Move Up'.t(), direction: -1, handler: 'moveUp' },
        // moveDown: { iconCls: 'fa fa-chevron-down', tooltip: 'Move Down'.t(), direction: 1, handler: 'moveUp' }
    },

    /**
     * @cfg {Array} tbar
     * Contains the grid action buttons placed in the top toolbar
     * Possible values:
     * '@add' - opens up a popup form with an emptyRecord
     * '@addInline' - add a new emptyRecord directly to the grid (meaning that grid columns have an editor defined for inline cell editing)
     * '@import' - imports data from file
     * '@export' - exports data to file
     * '@replace' - imports data from file without prepend or append options
     */
    tbar: null,

    /**
     * @cfg {Array} recordActions
     * The action columns for the grid.
     * Possible values:
     * '@edit' - opens a popup form for editing record
     * '@delete' - marks record for deletions (is removed from grid upon save)
     * '@reorder' - enables records reordering by drag and drop
     */
    recordActions: null,

    /**
     * @cfg {String} listProperty
     * the string wich represents the object expression for the list
     * e.g. 'settings.portForwardRules.list'
     */
    listProperty: null,

    /**
     * @cfg {Array} conditions
     * Required for data containing conditions
     * Represents a list of conditions as defined in Ung.cmp.GridConditions, or custom conditions defined inline
     * e.g. [Condition.dstAddr, Condition.dstPort, Condition.protocol([['TCP','TCP'],['UDP','UDP']])]
     */
    conditions: null,

    /**
     * @cfg {Array} columns
     * The default columns configuration
     * Represents a list of columns as defined in Ung.cmp.GridColumns, or custom columns defined inline
     * e.g. [Column.ruleId, Column.enabled, Column.description, Column.conditions]
     */
    columns: null,

    /**
     * @cfg {Array} editorFields
     * The definition of fields which are used in the Popup record editor form
     * Represents a list of fields as defined in Ung.cmp.GridEditorFields, or custom field defined inline
     * e.g. [Field.description, Fields.conditions, Field.newDestination, Field.newPort]
     */
    editorFields: null,

    /**
     * @cfg {string} editorXtype
     * Override the default record editor xtype of 'ung.cmp.recordeditor'.
     * Almost certainly a defintion you'll extend from ung.cmp.recordeditor.
     */
    editorXtype: 'ung.cmp.recordeditor',

    /**
     * @cfg {Object} emptyRow
     * Required for adding new records
     * Represents an object used to create a new record for a specific grid
     * example:
     * {
     *     ruleId: -1,
     *     enabled: true,
     *     javaClass: 'com.untangle.uvm.network.PortForwardRule',
     *     conditions: {
     *         javaClass: 'java.util.LinkedList',
     *         list: []
     *     }
     * }
     */
    emptyRow: null,

    /**
     * @cfg {String} actionText
     * Used in grids with conditions.
     * Tells the actions which are taken if condition are met
     * e.g. 'Forward to the following location:'.t()
     */
    actionText: 'Perform the following action(s):'.t(),

    /**
     * @cfg {String} parentView
     * The itemId of the component used to get an extra controller with actioncolumn methods specific for that view purpose
     * e.g. '#users' which alloes accessing the UsersController and call actioncolumn methods from it
     */
    parentView: null,

    stateful: false,

    layout: 'fit',
    trackMouseOver: false,
    enableColumnHide: false,
    // columnLines: true,
    scrollable: true,
    selModel: {
        type: 'cellmodel'
    },

    plugins: [{
        ptype: 'cellediting',
        clicksToEdit: 1
    }, {
        ptype: 'responsive'
    }],

    initComponentColumn: function(column){
        if( this.stateful &&
            !column.stateId &&
            column.dataIndex){
            column.stateId = column.dataIndex;
        }

        if( column.xtype == 'checkcolumn' && column.checkAll){
            var columnDataIndex = column.dataIndex;

            if( this.tbar ){
                this.tbar.splice( this.tbarSeparatorIndex, 0, Ext.applyIf(column.checkAll, {
                    xtype: 'checkbox',
                    hidden: !rpc.isExpertMode,
                    hideLabel: true,
                    margin: '0 5px 0 5px',
                    boxLabel: Ext.String.format("{0} All".t(), column.header),
                    // scope: {columnDataIndex: columnDataIndex},
                    handler: function(checkbox, checked) {
                        var records=checkbox.up("grid").getStore().getRange();
                        for(var i=0; i<records.length; i++) {
                            records[i].set(this.colDataIndex, checked);
                        }
                    },
                }));
                this.tbarSeparatorIndex++;
            }
        }

        if( column.rtype ){
            column.renderer = 'columnRenderer';
        }
    },

    listeners: {
        beforeedit: 'beforeEdit'
    },

    initComponent: function () {
        /*
         * Treat viewConfig as an object that inline configuration can override on an
         * individual field level instead of the entire viewConfig object itself.
         */
        var viewConfig = {
            enableTextSelection: true,
            // emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-lg"></i> No Data!</p>',
            stripeRows: false,
            getRowClass: function(record) {
                if (record.get('markedForDelete')) {
                    return 'mark-delete';
                }
                if (record.get('markedForNew')) {
                    return 'mark-new';
                }
                if (record.get('readOnly')) {
                    return 'mark-readonly';
                }
            }
        };
        if( this.viewConfig ){
            Ext.apply( viewConfig, this.viewConfig );
        }

        var columns = Ext.clone(this.columns), i;

        if( this.stateful &&
            ( this.itemId ||
              this.reference ) ) {
            this.stateId = "ungrid-" + this.itemId ? this.itemId : this.reference;
        }

        if (this.tbar == null) {
            this.tbar=[];
        }
        this.tbarSeparatorIndex = this.tbar.indexOf('->');
        if( this.tbarSeparatorIndex == -1 ){
            this.tbarSeparatorIndex = this.tbar.length;
        }

        if(columns){
            /*
             * Reports and others can set their columns manually.
             */
            columns.forEach( Ext.bind(function( column ){
                if( column.columns ){
                    /*
                     * Grouping
                     */
                    column.columns.forEach( Ext.bind( function( subColumn ){
                        this.initComponentColumn( subColumn );
                    }, this ) );
                }

                this.initComponentColumn( column );
            }, this ) );
        }

        if (this.recordActions) {
            for (i = 0; i < this.recordActions.length; i += 1) {
                var action = this.recordActions[i];
                if (action === 'changePassword') {
                    columns.push({
                        xtype: 'actioncolumn',
                        width: 120,
                        header: 'Change Password'.t(),
                        align: 'center',
                        resizable: false,
                        tdCls: 'action-cell',
                        iconCls: 'fa fa-lock',
                        menuDisabled: true,
                        hideable: false,
                        handler: 'changePassword'
                    });
                }
                if (action === 'edit') {
                    columns.push({
                        xtype: 'actioncolumn',
                        width: 60,
                        header: 'Edit'.t(),
                        align: 'center',
                        resizable: false,
                        tdCls: 'action-cell',
                        iconCls: 'fa fa-pencil',
                        handler: 'editRecord',
                        menuDisabled: true,
                        hideable: false,
                        isDisabled: function (table, rowIndex, colIndex, item, record) {
                            return record.get('readOnly') || false;
                        }
                    });
                }
                if (action === 'copy') {
                    columns.push({
                        xtype: 'actioncolumn',
                        width: 60,
                        header: 'Copy'.t(),
                        align: 'center',
                        resizable: false,
                        tdCls: 'action-cell',
                        iconCls: 'fa fa-files-o',
                        handler: 'copyRecord',
                        menuDisabled: true,
                        hideable: false,
                    });
                }
                if (action === 'delete') {
                    columns.push({
                        xtype: 'actioncolumn',
                        width: 60,
                        header: 'Delete'.t(),
                        align: 'center',
                        resizable: false,
                        tdCls: 'action-cell',
                        iconCls: 'fa fa-trash-o fa-red',
                        handler: 'deleteRecord',
                        menuDisabled: true,
                        hideable: false,
                        isDisabled: function (table, rowIndex, colIndex, item, record) {
                            return record.get('readOnly') || false;
                        }
                    });
                }

                if (action === 'reorder') {
                    this.sortableColumns = false;
                    Ext.apply( viewConfig, {
                        plugins: {
                            ptype: 'gridviewdragdrop',
                            dragText: 'Drag and drop to reorganize'.t(),
                            // allow drag only from drag column icons
                            dragZone: {
                                onBeforeDrag: function (data, e) {
                                    return Ext.get(e.target).hasCls('fa-arrows');
                                }
                            }
                        }
                    });
                    columns.unshift({
                        xtype: 'gridcolumn',
                        header: '<i class="fa fa-sort"></i>',
                        align: 'center',
                        width: 30,
                        resizable: false,
                        tdCls: 'action-cell',
                        menuDisabled: true,
                        hideable: false,
                        // iconCls: 'fa fa-arrows'
                        renderer: function() {
                            return '<i class="fa fa-arrows" style="cursor: move;"></i>';
                        },
                    });
                }
            }
        }

        Ext.apply(this, {
            columns: columns,
            viewConfig: viewConfig
        });
        this.callParent(arguments);
    }

});