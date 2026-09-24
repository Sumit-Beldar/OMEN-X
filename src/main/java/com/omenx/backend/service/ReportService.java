package com.omenx.backend.service;

import com.omenx.database.model.ScanRecord;

import java.util.List;

public class ReportService {

    private final ScanService scanService;

    public ReportService(ScanService scanService) {
        this.scanService = scanService;
    }

    public List<ScanRecord> getRecords() {
        return scanService.getHistory();
    }

    public int getTotalScans() {
        return scanService.getScanCount();
    }

    public int getTotalFindings() {
        return scanService.getTotalFindings();
    }
}