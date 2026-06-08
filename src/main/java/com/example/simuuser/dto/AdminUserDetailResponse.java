package com.example.simuuser.dto;

import java.util.List;

public class AdminUserDetailResponse {

    private final Long id;
    private final String name;
    private final String userId;
    private final String email;
    private final String createdAt;
    private final String loginMethod;
    private final String loginMethodLabel;
    private final String role;
    private final String roleLabel;
    private final String accountStatusLabel;
    private final String avatarText;
    private final int projectCount;
    private final int totalAnalysisCount;
    private final int inquiryCount;
    private final String lastActivityAt;
    private final List<ProjectItem> projects;
    private final List<RecentAnalysisItem> recentAnalyses;
    private final ChartData chart;

    public AdminUserDetailResponse(
            Long id,
            String name,
            String userId,
            String email,
            String createdAt,
            String loginMethod,
            String loginMethodLabel,
            String role,
            String roleLabel,
            String accountStatusLabel,
            String avatarText,
            int projectCount,
            int totalAnalysisCount,
            int inquiryCount,
            String lastActivityAt,
            List<ProjectItem> projects,
            List<RecentAnalysisItem> recentAnalyses,
            ChartData chart
    ) {
        this.id = id;
        this.name = name;
        this.userId = userId;
        this.email = email;
        this.createdAt = createdAt;
        this.loginMethod = loginMethod;
        this.loginMethodLabel = loginMethodLabel;
        this.role = role;
        this.roleLabel = roleLabel;
        this.accountStatusLabel = accountStatusLabel;
        this.avatarText = avatarText;
        this.projectCount = projectCount;
        this.totalAnalysisCount = totalAnalysisCount;
        this.inquiryCount = inquiryCount;
        this.lastActivityAt = lastActivityAt;
        this.projects = projects;
        this.recentAnalyses = recentAnalyses;
        this.chart = chart;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getCreatedAt() { return createdAt; }
    public String getLoginMethod() { return loginMethod; }
    public String getLoginMethodLabel() { return loginMethodLabel; }
    public String getRole() { return role; }
    public String getRoleLabel() { return roleLabel; }
    public String getAccountStatusLabel() { return accountStatusLabel; }
    public String getAvatarText() { return avatarText; }
    public int getProjectCount() { return projectCount; }
    public int getTotalAnalysisCount() { return totalAnalysisCount; }
    public int getInquiryCount() { return inquiryCount; }
    public String getLastActivityAt() { return lastActivityAt; }
    public List<ProjectItem> getProjects() { return projects; }
    public List<RecentAnalysisItem> getRecentAnalyses() { return recentAnalyses; }
    public ChartData getChart() { return chart; }

    public static class ProjectItem {
        private final Long id;
        private final String title;
        private final String description;
        private final String type;
        private final String typeLabel;
        private final String createdAt;

        public ProjectItem(Long id, String title, String description, String type, String typeLabel, String createdAt) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.type = type;
            this.typeLabel = typeLabel;
            this.createdAt = createdAt;
        }

        public Long getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getType() { return type; }
        public String getTypeLabel() { return typeLabel; }
        public String getCreatedAt() { return createdAt; }
    }

    public static class RecentAnalysisItem {
        private final String toolName;
        private final String projectTitle;
        private final String summary;
        private final String createdAt;

        public RecentAnalysisItem(String toolName, String projectTitle, String summary, String createdAt) {
            this.toolName = toolName;
            this.projectTitle = projectTitle;
            this.summary = summary;
            this.createdAt = createdAt;
        }

        public String getToolName() { return toolName; }
        public String getProjectTitle() { return projectTitle; }
        public String getSummary() { return summary; }
        public String getCreatedAt() { return createdAt; }
    }

    public static class ChartData {
        private final List<String> labels;
        private final List<Integer> aiSimulation;
        private final List<Integer> marketAnalysis;
        private final List<Integer> costAnalysis;
        private final List<Integer> feedbackAnalysis;
        private final List<Integer> documents;
        private final List<Integer> scenarioComparison;

        public ChartData(
                List<String> labels,
                List<Integer> aiSimulation,
                List<Integer> marketAnalysis,
                List<Integer> costAnalysis,
                List<Integer> feedbackAnalysis,
                List<Integer> documents,
                List<Integer> scenarioComparison
        ) {
            this.labels = labels;
            this.aiSimulation = aiSimulation;
            this.marketAnalysis = marketAnalysis;
            this.costAnalysis = costAnalysis;
            this.feedbackAnalysis = feedbackAnalysis;
            this.documents = documents;
            this.scenarioComparison = scenarioComparison;
        }

        public List<String> getLabels() { return labels; }
        public List<Integer> getAiSimulation() { return aiSimulation; }
        public List<Integer> getMarketAnalysis() { return marketAnalysis; }
        public List<Integer> getCostAnalysis() { return costAnalysis; }
        public List<Integer> getFeedbackAnalysis() { return feedbackAnalysis; }
        public List<Integer> getDocuments() { return documents; }
        public List<Integer> getScenarioComparison() { return scenarioComparison; }
    }
}
