package com.omenx.service;

import com.omenx.model.ScanRecord;

import java.util.List;

// =========================================================
// REPORT SERVICE
// Provides investigation data for reports/export features.
// =========================================================

public class ReportService {

    private final ScanService scanService;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public ReportService(
            ScanService scanService
    ) {

        this.scanService = scanService;
    }

    // =========================================================
    // GET RECORDS
    // =========================================================

    public List<ScanRecord> getRecords() {

        return scanService.getHistory();
    }

    // =========================================================
    // GET TOTAL SCANS
    // =========================================================

    public int getTotalScans() {

        return scanService.getScanCount();
    }

    // =========================================================
    // GET TOTAL FINDINGS
    // =========================================================

    public int getTotalFindings() {

        return scanService.getTotalFindings();
    }
}