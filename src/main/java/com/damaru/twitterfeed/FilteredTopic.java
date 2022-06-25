package com.damaru.twitterfeed;

import lombok.Data;

import java.util.Set;

@Data
public class FilteredTopic {
    public FilteredTopic(String topic, Set<String> filterStrings) {
        this.topic = topic;
        this.filterStrings = filterStrings;
    }

    private String topic;
    private Set<String> filterStrings;
    private int count;

    public void incrementCount() {
        ++count;
    }

    public String toString() {
        return topic + ": " + count;
    }
}
