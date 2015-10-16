Ext.define('Webui.config.hostMonitor', {
    extend: 'Ung.StatusWin',
    helpSource: 'host_viewer',
    sortField:'bypassed',
    sortOrder: 'ASC',
    displayName: 'Host Viewer',
    hasReports: true,
    reportCategory: 'Host Viewer',
    defaultBandwidthColumns: false,
    enableBandwidthColumns: false,
    initComponent: function() {
        this.breadcrumbs = [{
            title: i18n._('Host Viewer')
        }];
        this.buildGridCurrentHosts();
        this.buildGridPenaltyBox();
        this.buildGridQuotaBox();
        this.buildTabPanel([this.gridCurrentHosts, this.gridPenaltyBox, this.gridQuotaBox]);
        this.callParent(arguments);
    },
    closeWindow: function() {
        this.gridCurrentHosts.stopAutoRefresh(true);
        this.hide();
    },
    getHosts: function(handler) {
        if (!this.isVisible()) {
            handler({javaClass:"java.util.LinkedList", list:[]});
            return;
        }
        rpc.hostTable.getHosts(Ext.bind(function(result, exception) {
            if(testMode && result != null && result.list!=null ) {
                var testSize = 450 + Math.floor((Math.random()*100));
                for(var i=0;i<testSize;i++) {
                    var ii=i+Math.floor((Math.random()*10));
                    var d=new Date();
                    result.list.push({
                        "address": "184.27.239."+(ii%10),
                        "macAddress": "11:22:33:44:55:6"+(ii%10),
                        "macVendor": "MAC vendor"+i,
                        "hostname": i%3?("p.twitter.com"+i):null,
                        "licensed": true,
                        "licenseNeeded": true,
                        "lastAccessTime": 0,//d.getTime()+(i*86400000),
                        "lastSessionTime": 0,//d.getTime()+(i*86400000),
                        "username": "testuser"+i,
                        "usernameAdConnector": "uad"+ii,
                        "captivePortalAuthenticated":(ii%2)==1,
                        "usernameCapture": "ucap"+(ii%50),
                        "penaltyBoxed":(ii%2)==1,
                        "penaltyBoxEntryTime": d.getTime()-(ii*86400000),
                        "penaltyBoxExitTime": d.getTime()+(ii*86400000),
                        "quotaSize": ii * 10000,
                        "quotaRemaining": ii * 5000,
                        "quotaIssueTime": 0,
                        "quotaExpirationTime": 0,
                        "httpUserAgent": (ii%3)?("MOZFirefox"+i):null,
                        "httpUserAgentOs": (ii%4)?("Win"+ii):null
                    });
                }
            }
            handler(result, exception);
        }, this));
    },
    megaByteRenderer: function(bytes) {
        var units = ["bytes","Kbytes","Mbytes","Gbytes"];
        var units_itr = 0;

        while ((bytes >= 1000 || bytes <= -1000) && units_itr < 3) {
            bytes = bytes/1000;
            units_itr++;
        }

        bytes = Math.round(bytes*100)/100;

        return "" + bytes + " " + units[units_itr];
    },
    // Current Hosts Grid
    buildGridCurrentHosts: function(columns, groupField) {
        var dateConvertFn = function(value) {
            if( value == 0 || value == "") {
                return "";
            } else {
                var d=new Date();
                d.setTime(value);
                return d;
            }
        };
        this.gridCurrentHosts = Ext.create('Ung.MonitorGrid',{
            name: "hostMonitorGrid",
            helpSource: 'host_viewer_current_hosts',
            settingsCmp: this,
            height: 500,
            title: i18n._("Current Hosts"),
            tooltip: i18n._("This shows all current hosts."),
            dataFn: Ext.bind(this.getHosts, this),
            sortField: this.sortField,
            sortOrder: this.sortOrder,
            groupField: this.groupField,
            fields: [{
                name: "id"
            },{
                name: "address",
                type: 'string',
                sortType: 'asIp'
            },{
                name: "macAddress",
                type: 'string'
            },{
                name: "macVendor",
                type: 'string'
            },{
                name: "hostname",
                type: 'string'
            }, {
                name: "lastAccessTime"
            }, {
                name: "lastAccessTimeDate",
                mapping: "lastAccessTime",
                convert: dateConvertFn
            }, {
                name: "lastSessionTime"
            }, {
                name: "lastSessionTimeDate",
                mapping: "lastSessionTime",
                convert: dateConvertFn
            }, {
                name: "licensed",
                type: 'boolean'
            }, {
                name: "licenseNeeded",
                type: 'boolean'
            },{
                name: "username",
                type: 'string'
            },{
                name: "usernameAdConnector",
                type: 'string'
            },{
                name: "captivePortalAuthenticated"
            },{
                name: "usernameCapture",
                type: 'string'
            },{
                name: "usernameTunnel",
                type: 'string'
            },{
                name: "usernameOpenvpn",
                type: 'string'
            },{
                name: "penaltyBoxed"
            },{
                name: "penaltyBoxEntryTime"
            },{
                name: "penaltyBoxEntryTimeDate",
                mapping: "penaltyBoxEntryTime",
                convert: dateConvertFn
            },{
                name: "penaltyBoxExitTime"
            },{
                name: "penaltyBoxExitTimeDate",
                mapping: "penaltyBoxExitTime",
                convert: dateConvertFn
            },{
                name: "quotaSize"
            },{
                name: "quotaRemaining"
            },{
                name: "quotaIssueTime"
            },{
                name: "quotaIssueTimeDate",
                mapping: "quotaIssueTime",
                convert: dateConvertFn
            },{
                name: "quotaExpirationTime"
            },{
                name: "quotaExpirationTimeDate",
                mapping: "quotaExpirationTime",
                convert: dateConvertFn
            },{
                name: "httpUserAgent",
                type: 'string'
            },{
                name: "httpUserAgentOs",
                type: 'string'
            }],
            columns: [{
                header: i18n._("IP"),
                dataIndex: "address",
                width: 100,
                filter: {
                    type: 'string'
                }
            }, {
                header: i18n._("MAC Address"),
                dataIndex: "macAddress",
                width: 100,
                filter: {
                    type: 'string'
                }
            }, {
                header: i18n._("MAC Vendor"),
                dataIndex: "macVendor",
                width: 150,
                filter: {
                    type: 'string'
                }
            }, {
                hidden: true,
                header: i18n._("Last Access Time"),
                dataIndex: "lastAccessTimeDate",
                width: 150,
                renderer: function(value, metaData, record) {
                    var val=record.get("lastAccessTime");
                    return val == 0 || val == "" ? "" : i18n.timestampFormat(val);
                },
                filter: {
                    type: 'date'
                }
            }, {
                hidden: true,
                header: i18n._("Last Session Time"),
                dataIndex: "lastSessionTimeDate",
                width: 150,
                renderer: function(value, metaData, record) {
                    var val=record.get("lastSessionTime");
                    return val == 0 || val == "" ? "" : i18n.timestampFormat(val);
                },
                filter: {
                    type: 'date'
                }
            }, {
                hidden: true,
                header: i18n._("Licensed"),
                dataIndex: "licensed",
                width: 80,
                filter: {
                    type: 'boolean'
                }
            }, {
                hidden: true,
                header: i18n._("License Needed"),
                dataIndex: "licenseNeeded",
                width: 80,
                filter: {
                    type: 'boolean'
                }
            }, {
                header: i18n._("Hostname"),
                dataIndex: "hostname",
                width: 100,
                filter: {
                    type: 'string'
                }
            },{
                header: i18n._("Username"),
                dataIndex: "username",
                width: 100,
                filter: {
                    type: 'string'
                }
            },{
                header: i18n._("Penalty Boxed"),
                dataIndex: "penaltyBoxed",
                width: 100,
                filter: {
                    type: 'boolean'
                }
            },{
                hidden: true,
                header: i18n._("Penalty Box Entry Time"),
                dataIndex: "penaltyBoxEntryTimeDate",
                width: 100,
                renderer: function(value, metaData, record) {
                    var val=record.get("penaltyBoxEntryTime");
                    return val == 0 || val == "" ? "" : i18n.timestampFormat(val);
                },
                filter: {
                    type: 'date'
                }
            },{
                hidden: true,
                header: i18n._("Penalty Box Exit Time"),
                dataIndex: "penaltyBoxExitTimeDate",
                width: 100,
                renderer: function(value, metaData, record) {
                    var val=record.get("penaltyBoxExitTime");
                    return val == 0 || val == "" ? "" : i18n.timestampFormat(val);
                },
                filter: {
                    type: 'date'
                }
            },{
                header: i18n._("Quota Size"),
                dataIndex: "quotaSize",
                width: 100,
                renderer: function(value) {
                    return value == 0 || value == "" ? "" : value;
                },
                filter: {
                    type: 'numeric'
                }
            },{
                hidden: true,
                header: i18n._("Quota Remaining"),
                dataIndex: "quotaRemaining",
                width: 100,
                filter: {
                    type: 'numeric'
                }
            },{
                hidden: true,
                header: i18n._("Quota Issue Time"),
                dataIndex: "quotaIssueTimeDate",
                width: 100,
                renderer: function(value, metaData, record) {
                    var val=record.get("quotaIssueTime");
                    return val == 0 || val == "" ? "" : i18n.timestampFormat(val);
                },
                filter: {
                    type: 'date'
                }
            },{
                hidden: true,
                header: i18n._("Quota Expiration Time"),
                dataIndex: "quotaExpirationTimeDate",
                width: 100,
                renderer: function(value, metaData, record) {
                    var val=record.get("quotaExpirationTime");
                    return val == 0 || val == "" ? "" : i18n.timestampFormat(val);
                },
                filter: {
                    type: 'date'
                }
            },{
                header: "HTTP" + " - " + i18n._("User Agent"),
                dataIndex: "httpUserAgent",
                width: 200,
                filter: {
                    type: 'string'
                }
            },{
                hidden: true,
                header: "HTTP" + " - " + i18n._("User Agent OS"),
                dataIndex: "httpUserAgentOs",
                width: 200,
                filter: {
                    type: 'string'
                }
            },{
                hidden: true,
                header: "Captive Portal" + " - " + i18n._("Authenticated"),
                dataIndex: "captivePortalAuthenticated",
                width: 100,
                filter: {
                    type: 'boolean'
                }
            },{
                hidden: true,
                header: "Captive Portal" + " - " + i18n._("Username"),
                dataIndex: "usernameCapture",
                width: 100,
                filter: {
                    type: 'string'
                }
            },{
                hidden: true,
                header: "Directory Connector" + " - " + i18n._("Username"),
                dataIndex: "usernameAdConnector",
                width: 100,
                filter: {
                    type: 'string'
                }
            },{
                hidden: true,
                header: "L2TP" + " - " + i18n._("Username"),
                dataIndex: "usernameTunnel",
                width: 100,
                filter: {
                    type: 'string'
                }
            },{
                hidden: true,
                header: "OpenVPN" + " - " + i18n._("Username"),
                dataIndex: "usernameOpenvpn",
                width: 100,
                filter: {
                    type: 'string'
                }
            }]
        });
    },
    buildGridPenaltyBox: function() {
        this.gridPenaltyBox = Ext.create('Ung.grid.Panel',{
            helpSource: 'host_viewer_penalty_box_hosts',
            name: "PenaltyBoxHosts",
            settingsCmp: this,
            hasAdd: false,
            hasEdit: false,
            hasDelete: false,
            hasRefresh: true,
            title: i18n._("Penalty Box Hosts"),
            qtip: i18n._("This shows all hosts currently in the Penalty Box."),
            dataFn: Ext.bind(rpc.hostTable.getPenaltyBoxedHosts, this),
            recordJavaClass: "com.untangle.uvm.HostTableEntry",
            fields: [{
                name: "address",
                sortType: 'asIp'
            },{
                name: "penaltyBoxEntryTime"
            },{
                name: "penaltyBoxExitTime"
            },{
                name: "id"
            }],
            columns: [{
                header: i18n._("IP Address"),
                dataIndex: 'address',
                width: 150
            },{
                header: i18n._("Entry Time"),
                dataIndex: 'penaltyBoxEntryTime',
                width: 180,
                renderer: function(value) { return i18n.timestampFormat(value); }
            },{
                header: i18n._("Planned Exit Time"),
                dataIndex: 'penaltyBoxExitTime',
                width: 180,
                renderer: function(value) { return i18n.timestampFormat(value); }
            }, {
                header: i18n._("Release host"),
                xtype: 'actioncolumn',
                width: 120,
                items: [{
                    tooltip: i18n._("Release host"),
                    iconCls: 'icon-play-row',
                    handler: Ext.bind(function(view, rowIndex, colIndex, item, e, record) {
                        Ext.MessageBox.wait(i18n._("Releasing host..."), i18n._("Please wait"));
                        rpc.hostTable.releaseHostFromPenaltyBox(Ext.bind(function(result,exception) {
                            Ext.MessageBox.hide();
                            if(Ung.Util.handleException(exception)) return;
                            this.gridPenaltyBox.reload();
                        }, this), record.data.address );
                    }, this)
                }]
            }]
        });

    },
    buildGridQuotaBox: function() {
        this.gridQuotaBox = Ext.create('Ung.grid.Panel',{
            name: "CurrentQuotas",
            helpSource: 'host_viewer_current_quotas',
            settingsCmp: this,
            hasAdd: false,
            hasEdit: false,
            hasDelete: false,
            hasRefresh: true,
            title: i18n._("Current Quotas"),
            qtip: i18n._("This shows all hosts currently with quotas."),
            dataFn: Ext.bind(rpc.hostTable.getQuotaHosts, this),
            recordJavaClass: "com.untangle.uvm.HostTableEntry",
            fields: [{
                name: "address",
                sortType: 'asIp'
            },{
                name: "quotaSize"
            },{
                name: "quotaRemaining"
            },{
                name: "quotaIssueTime"
            },{
                name: "quotaExpirationTime"
            },{
                name: "id"
            }],
            columns: [{
                header: i18n._("IP Address"),
                dataIndex: 'address',
                width: 150
            },{
                header: i18n._("Quota Size"),
                dataIndex: 'quotaSize',
                width: 100,
                renderer: Ext.bind(this.megaByteRenderer, this)
            },{
                header: i18n._("Quota Remaining"),
                dataIndex: 'quotaRemaining',
                width: 100,
                renderer: Ext.bind(this.megaByteRenderer, this)
            },{
                header: i18n._("Allocated"),
                dataIndex: 'quotaIssueTime',
                width: 180,
                renderer: function(value) { return i18n.timestampFormat(value); }
            },{
                header: i18n._("Expires"),
                dataIndex: 'quotaExpirationTime',
                width: 180,
                renderer: function(value) { return i18n.timestampFormat(value); }
            }, {
                header: i18n._("Refill Quota"),
                xtype: 'actioncolumn',
                width: 110,
                items: [{
                    tooltip: i18n._("Refill Quota"),
                    iconCls: 'icon-refresh-row',
                    handler: Ext.bind(function(view, rowIndex, colIndex, item, e, record) {
                        Ext.MessageBox.wait(i18n._("Refilling..."), i18n._("Please wait"));
                        rpc.hostTable.refillQuota(Ext.bind(function(result,exception) {
                            Ext.MessageBox.hide();
                            if(Ung.Util.handleException(exception)) return;
                            this.gridQuotaBox.reload();
                        }, this), record.data.address );
                    }, this)
                }]
            }, {
                header: i18n._("Drop Quota"),
                xtype: 'actioncolumn',
                width: 110,
                items: [{
                    tooltip: i18n._("Drop Quota"),
                    iconCls: 'icon-drop-row',
                    handler: Ext.bind(function(view, rowIndex, colIndex, item, e, record) {
                        Ext.MessageBox.wait(i18n._("Removing Quota..."), i18n._("Please wait"));
                        rpc.hostTable.removeQuota(Ext.bind(function(result,exception) {
                            Ext.MessageBox.hide();
                            if(Ung.Util.handleException(exception)) return;
                            this.gridQuotaBox.reload();
                        }, this), record.data.address );
                    }, this)
                }]
            }]
        });
    }
});
//# sourceURL=hostMonitor.js