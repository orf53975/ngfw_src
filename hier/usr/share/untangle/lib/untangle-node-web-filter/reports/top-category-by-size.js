{
    "uniqueId": "web-filter-KN3CqjeVLRA",
    "category": "Web Filter",
    "description": "The sum of the size of requested web content grouped by category.",
    "displayOrder": 201,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "bytes",
    "pieGroupColumn": "web_filter_category",
    "pieSumColumn": "coalesce(sum(s2c_content_length),0)",
    "readOnly": true,
    "table": "http_events",
    "title": "Top Categories (by size)",
    "type": "PIE_GRAPH"
}



