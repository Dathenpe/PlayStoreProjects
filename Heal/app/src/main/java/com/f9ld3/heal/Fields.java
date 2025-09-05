package com.f9ld3.heal;

public class Fields {
    Project project;
    String summary;
    String description;
    IssueType issuetype;
    // You can add other fields here if needed, like assignee, priority, custom fields etc.
    // For custom fields, the key would be like "customfield_10001"

    public Fields(Project project, String summary, String description, IssueType issuetype) {
        this.project = project;
        this.summary = summary;
        this.description = description;
        this.issuetype = issuetype;
    }

    // Getters (optional, for completeness)
    public Project getProject() {
        return project;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public IssueType getIssuetype() {
        return issuetype;
    }
}