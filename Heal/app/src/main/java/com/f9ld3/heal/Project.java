package com.f9ld3.heal;

public class Project {
    String key;

    public Project(String key) {
        this.key = key;
    }

    // Getter (optional, as Gson can access fields directly, but good practice)
    public String getKey() {
        return key;
    }
}