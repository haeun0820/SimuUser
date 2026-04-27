package com.example.simuuser.dto;

import java.util.List;

public class DashboardAnalyticsResponse {

    private final int totalAnalysisCount;
    private final List<ToolUsageItem> toolUsage;
    private final ChartSeries year;
    private final ChartSeries month;
    private final ChartSeries week;

    public DashboardAnalyticsResponse(
            int totalAnalysisCount,
            List<ToolUsageItem> toolUsage,
            ChartSeries year,
            ChartSeries month,
            ChartSeries week
    ) {
        this.totalAnalysisCount = totalAnalysisCount;
        this.toolUsage = toolUsage;
        this.year = year;
        this.month = month;
        this.week = week;
    }

    public int getTotalAnalysisCount() {
        return totalAnalysisCount;
    }

    public List<ToolUsageItem> getToolUsage() {
        return toolUsage;
    }

    public ChartSeries getYear() {
        return year;
    }

    public ChartSeries getMonth() {
        return month;
    }

    public ChartSeries getWeek() {
        return week;
    }

    public static class ToolUsageItem {
        private final String key;
        private final String label;
        private final int value;
        private final String color;

        public ToolUsageItem(String key, String label, int value, String color) {
            this.key = key;
            this.label = label;
            this.value = value;
            this.color = color;
        }

        public String getKey() {
            return key;
        }

        public String getLabel() {
            return label;
        }

        public int getValue() {
            return value;
        }

        public String getColor() {
            return color;
        }
    }

    public static class ChartSeries {
        private final List<String> labels;
        private final List<Integer> data;

        public ChartSeries(List<String> labels, List<Integer> data) {
            this.labels = labels;
            this.data = data;
        }

        public List<String> getLabels() {
            return labels;
        }

        public List<Integer> getData() {
            return data;
        }
    }
}
