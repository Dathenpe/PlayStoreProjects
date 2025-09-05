package com.f9ld3.heal;

public class JiraIssueRequest {
    Fields fields;

    public JiraIssueRequest(Fields fields) {
        this.fields = fields;
    }

    // Getter
    public Fields getFields() {
        return fields;
    }
}
