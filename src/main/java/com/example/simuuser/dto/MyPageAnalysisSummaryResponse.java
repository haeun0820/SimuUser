package com.example.simuuser.dto;

import java.util.List;

public class MyPageAnalysisSummaryResponse {

    private final int analysisCount;
    private final List<MyPageAnalysisHistoryResponse> histories;

    public MyPageAnalysisSummaryResponse(int analysisCount, List<MyPageAnalysisHistoryResponse> histories) {
        this.analysisCount = analysisCount;
        this.histories = histories;
    }

    public int getAnalysisCount() {
        return analysisCount;
    }

    public List<MyPageAnalysisHistoryResponse> getHistories() {
        return histories;
    }
}
