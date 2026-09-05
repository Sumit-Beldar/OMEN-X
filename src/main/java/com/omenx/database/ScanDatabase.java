package com.omenx.database;

import com.omenx.model.ScanRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// =========================================================
// SCAN DATABASE
// Stores investigation history during the application session.
// =========================================================

public class ScanDatabase {

    private final List<ScanRecord> records =
            new ArrayList<>();

    // =========================================================
    // ADD RECORD
    // =========================================================

    public void addRecord(ScanRecord record) {

        if (record != null) {
            records.add(record);
        }
    }

    // =========================================================
    // GET ALL RECORDS
    // =========================================================

    public List<ScanRecord> getAllRecords() {

        return Collections.unmodifiableList(
                records
        );
    }

    // =========================================================
    // GET RECORD COUNT
    // =========================================================

    public int getRecordCount() {

        return records.size();
    }

    // =========================================================
    // CLEAR RECORDS
    // =========================================================

    public void clear() {

        records.clear();
    }

    // =========================================================
    // CHECK IF EMPTY
    // =========================================================

    public boolean isEmpty() {

        return records.isEmpty();
    }
}