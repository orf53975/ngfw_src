{
    "uniqueId": "web-filter-R0Bc7vikgu",
    "category": "Web Filter",
    "description": "The amount of flagged, and blocked web requests over time.",
    "displayOrder": 103,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderDesc": false,
    "units": "hits",
    "readOnly": true,
    "table": "http_events",
    "timeDataColumns": [
        "sum(web_filter_blocked::int) as blocked"
    ],
    "colors": [
        "#8c0000"
    ],
    "timeDataInterval": "AUTO",
    "timeStyle": "BAR_3D",
    "title": "Web Usage (blocked)",
    "type": "TIME_GRAPH"
}
