{
    "uniqueId": "application-control-UoWxjzdYct",
    "category": "Application Control",
    "description": "The number of sessions grouped by category.",
    "displayOrder": 200,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "hits",
    "pieGroupColumn": "application_control_category",
    "pieSumColumn": "count(*)",
    "readOnly": true,
    "table": "sessions",
    "conditions": [
        {
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "column": "application_control_category",
            "operator": "is",
            "value": "not null"
        }
    ],
    "title": "Top Categories (by sessions)",
    "type": "PIE_GRAPH"
}
