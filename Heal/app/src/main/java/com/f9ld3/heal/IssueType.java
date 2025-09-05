package com.f9ld3.heal;

public class IssueType {
    String name; // Using name, e.g., "Bug". Jira might also accept "id" if you know the numeric ID.

    public IssueType(String name) {
        this.name = name;
    }

    // Getter
    public String getName() {
        return name;
    }
}