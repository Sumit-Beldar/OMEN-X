package com.omenx.backend.service;

import com.omenx.database.ScanDatabase;
import com.omenx.database.model.ScanRecord;

import java.util.List;

public class ScanService {

    private final ScanDatabase database;

    public ScanService() {
        this.database = new ScanDatabase();
    }

    public void saveScan(
            String type,
            String target,
            int found,
            String time
    ) {
        ScanRecord record = new ScanRecord(
                type,
                target,
                found,
                time
        );
        database.addRecord(record);
    }

    public List<ScanRecord> getHistory() {
        return database.getAllRecords();
    }

    public int getScanCount() {
        return database.getRecordCount();
    }

    public int getTotalFindings() {
        return database.getAllRecords()
                .stream()
                .mapToInt(ScanRecord::getFound)
                .sum();
    }

    public void clearHistory() {
        database.clear();
    }
}