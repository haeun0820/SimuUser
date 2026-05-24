package com.example.simuuser.dto;

import java.util.List;

public class AdminDashboardAnalyticsResponse {

    private final Summary summary;
    private final ChartPeriod year;
    private final ChartPeriod month;
    private final ChartPeriod day;

    public AdminDashboardAnalyticsResponse(Summary summary, ChartPeriod year, ChartPeriod month, ChartPeriod day) {
        this.summary = summary;
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public Summary getSummary() {
        return summary;
    }

    public ChartPeriod getYear() {
        return year;
    }

    public ChartPeriod getMonth() {
        return month;
    }

    public ChartPeriod getDay() {
        return day;
    }

    public static class Summary {
        private final int totalUsers;
        private final int totalProjects;
        private final int totalAnalyses;

        public Summary(int totalUsers, int totalProjects, int totalAnalyses) {
            this.totalUsers = totalUsers;
            this.totalProjects = totalProjects;
            this.totalAnalyses = totalAnalyses;
        }

        public int getTotalUsers() {
            return totalUsers;
        }

        public int getTotalProjects() {
            return totalProjects;
        }

        public int getTotalAnalyses() {
            return totalAnalyses;
        }
    }

    public static class ChartPeriod {
        private final List<String> labels;
        private final List<Integer> users;
        private final List<Integer> projects;
        private final List<Integer> analyses;
        private final List<Integer> sim;
        private final List<Integer> market;
        private final List<Integer> cost;
        private final List<Integer> feedback;
        private final List<Integer> doc;
        private final List<Integer> scenario;

        public ChartPeriod(
                List<String> labels,
                List<Integer> users,
                List<Integer> projects,
                List<Integer> analyses,
                List<Integer> sim,
                List<Integer> market,
                List<Integer> cost,
                List<Integer> feedback,
                List<Integer> doc,
                List<Integer> scenario
        ) {
            this.labels = labels;
            this.users = users;
            this.projects = projects;
            this.analyses = analyses;
            this.sim = sim;
            this.market = market;
            this.cost = cost;
            this.feedback = feedback;
            this.doc = doc;
            this.scenario = scenario;
        }

        public List<String> getLabels() {
            return labels;
        }

        public List<Integer> getUsers() {
            return users;
        }

        public List<Integer> getProjects() {
            return projects;
        }

        public List<Integer> getAnalyses() {
            return analyses;
        }

        public List<Integer> getSim() {
            return sim;
        }

        public List<Integer> getMarket() {
            return market;
        }

        public List<Integer> getCost() {
            return cost;
        }

        public List<Integer> getFeedback() {
            return feedback;
        }

        public List<Integer> getDoc() {
            return doc;
        }

        public List<Integer> getScenario() {
            return scenario;
        }
    }
}
