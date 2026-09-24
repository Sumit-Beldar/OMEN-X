package com.omenx.database;

import com.omenx.database.model.ScanRecord;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ScanDatabase {

    private final List<ScanRecord> records = new CopyOnWriteArrayList<>();

    public void addRecord(ScanRecord record) {
        if (record != null) {
            records.add(record);
        }
    }

    public List<ScanRecord> getAllRecords() {
        return Collections.unmodifiableList(records);
    }

    public int getRecordCount() {
        return records.size();
    }

    public void clear() {
        records.clear();
    }

    public boolean isEmpty() {
        return records.isEmpty();
    }
}