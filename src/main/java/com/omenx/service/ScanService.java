package com.omenx.service;

import com.omenx.database.ScanDatabase;
import com.omenx.model.ScanRecord;

import java.util.List;

// =========================================================
// SCAN SERVICE
// Handles communication between scanners and scan storage.
// =========================================================

public class ScanService {

    private final ScanDatabase database;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public ScanService() {

        database = new ScanDatabase();
    }

    // =========================================================
    // SAVE SCAN
    // =========================================================

    public void saveScan(
            String type,
            String target,
            int found,
            String time
    ) {

        ScanRecord record =
                new ScanRecord(
                        type,
                        target,
                        found,
                        time
                );

        database.addRecord(record);
    }

    // =========================================================
    // GET HISTORY
    // =========================================================

    public List<ScanRecord> getHistory() {

        return database.getAllRecords();
    }

    // =========================================================
    // GET SCAN COUNT
    // =========================================================

    public int getScanCount() {

        return database.getRecordCount();
    }

    // =========================================================
    // GET TOTAL FINDINGS
    // =========================================================

    public int getTotalFindings() {

        return database.getAllRecords()
                .stream()
                .mapToInt(ScanRecord::getFound)
                .sum();
    }

    // =========================================================
    // CLEAR HISTORY
    // =========================================================

    public void clearHistory() {

        database.clear();
    }
}