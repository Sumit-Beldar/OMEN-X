package com.omenx.database.model;

public class ScanRecord {

    private final String type;
    private final String target;
    private final int found;
    private final String time;

    public ScanRecord(
            String type,
            String target,
            int found,
            String time
    ) {
        this.type = type;
        this.target = target;
        this.found = found;
        this.time = time;
    }

    public String getType() {
        return type;
    }

    public String getTarget() {
        return target;
    }

    public int getFound() {
        return found;
    }

    public String getTime() {
        return time;
    }
}