function switchDetailTab(clickedTab, targetId) {
    document.querySelectorAll(".admin-tab").forEach(tab => {
        tab.classList.remove("active");
    });
    clickedTab.classList.add("active");

    document.querySelectorAll(".tab-pane").forEach(pane => {
        pane.style.display = "none";
        pane.classList.remove("active");
    });

    const targetPane = document.getElementById("tab-" + targetId);
    if (targetPane) {
        targetPane.style.display = "flex";
        targetPane.classList.add("active");
    }

    if (targetId === "analysis" && window.userChart) {
        window.userChart.resize();
    }
}

document.addEventListener("DOMContentLoaded", function () {
    const userId = document.body.dataset.userId;
    if (!userId) {
        return;
    }

    function escapeHtml(value) {
        return String(value || "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");
    }

    function formatDateTime(value) {
        if (!value) {
            return "-";
        }
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return "-";
        }
        return new Intl.DateTimeFormat("ko-KR", {
            year: "numeric",
            month: "numeric",
            day: "numeric",
            hour: "2-digit",
            minute: "2-digit"
        }).format(date);
    }

    function formatDate(value) {
        if (!value) {
            return "-";
        }
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return "-";
        }
        return new Intl.DateTimeFormat("ko-KR", {
            year: "numeric",
            month: "numeric",
            day: "numeric"
        }).format(date);
    }

    function renderProjects(projects) {
        const body = document.getElementById("detailProjectsBody");
        if (!projects.length) {
            body.innerHTML = '<tr><td colspan="4" style="text-align:center; color:#64748b;">프로젝트가 없습니다.</td></tr>';
            return;
        }

        body.innerHTML = projects.map(project => `
            <tr>
                <td style="font-weight: 600;">${escapeHtml(project.title)}</td>
                <td style="color: var(--text-sub);">${escapeHtml(project.description || "-")}</td>
                <td><span class="project-status-badge ${projectStatusClass(project.type)}">${escapeHtml(project.typeLabel || project.type || "-")}</span></td>
                <td>${escapeHtml(formatDate(project.createdAt))}</td>
            </tr>
        `).join("");
    }

    function projectStatusClass(type) {
        const normalized = String(type || "").trim().toUpperCase();
        if (normalized === "TEAM" || normalized === "COLLAB" || normalized === "COLLABORATION") {
            return "project-status-collab";
        }
        return "project-status-personal";
    }

    function renderRecentAnalyses(items) {
        const body = document.getElementById("detailAnalysesBody");
        if (!items.length) {
            body.innerHTML = '<tr><td colspan="4" style="text-align:center; color:#64748b;">분석 이력이 없습니다.</td></tr>';
            return;
        }

        body.innerHTML = items.map(item => `
            <tr>
                <td><span class="analysis-tool-badge ${analysisToolClass(item.toolName)}">${escapeHtml(item.toolName)}</span></td>
                <td style="font-weight: 600;">${escapeHtml(item.projectTitle || "-")}</td>
                <td style="color: var(--text-sub);">${escapeHtml(item.summary || "-")}</td>
                <td>${escapeHtml(formatDateTime(item.createdAt))}</td>
            </tr>
        `).join("");
    }

    function analysisToolClass(toolName) {
        const normalized = String(toolName || "").trim();
        const classMap = {
            "AI 시뮬레이션": "analysis-tool-simulation",
            "시장 분석": "analysis-tool-market",
            "수익성 분석": "analysis-tool-profit",
            "기획 피드백": "analysis-tool-feedback",
            "자동 문서화": "analysis-tool-document",
            "시나리오 비교": "analysis-tool-scenario"
        };
        return classMap[normalized] || "analysis-tool-default";
    }

    function renderChart(chartData) {
        const ctx = document.getElementById("userAnalysisChart");
        if (!ctx) {
            return;
        }

        if (window.userChart) {
            window.userChart.destroy();
        }

        window.userChart = new Chart(ctx.getContext("2d"), {
            type: "line",
            data: {
                labels: chartData.labels || [],
                datasets: [
                    { label: "시뮬레이션", data: chartData.aiSimulation || [], borderColor: "#8b5cf6", backgroundColor: "#8b5cf6", pointBackgroundColor: "#8b5cf6", tension: 0.35 },
                    { label: "시장 분석", data: chartData.marketAnalysis || [], borderColor: "#d2529c", backgroundColor: "#d2529c", pointBackgroundColor: "#d2529c", tension: 0.35 },
                    { label: "수익성 분석", data: chartData.costAnalysis || [], borderColor: "#e07c2b", backgroundColor: "#e07c2b", pointBackgroundColor: "#e07c2b", tension: 0.35 },
                    { label: "기획 피드백", data: chartData.feedbackAnalysis || [], borderColor: "#64b4d3", backgroundColor: "#64b4d3", pointBackgroundColor: "#64b4d3", tension: 0.35 },
                    { label: "자동 문서화", data: chartData.documents || [], borderColor: "#d5534c", backgroundColor: "#d5534c", pointBackgroundColor: "#d5534c", tension: 0.35 },
                    { label: "시나리오 비교", data: chartData.scenarioComparison || [], borderColor: "#d8ad2e", backgroundColor: "#d8ad2e", pointBackgroundColor: "#d8ad2e", tension: 0.35 }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: "bottom",
                        labels: { usePointStyle: true, boxWidth: 8, font: { size: 12 } }
                    }
                },
                scales: {
                    y: { beginAtZero: true, grid: { color: "#f1f5f9" }, ticks: { stepSize: 1 } },
                    x: { grid: { display: false } }
                }
            }
        });
    }

    function applyDetail(data) {
        document.getElementById("detailAvatar").textContent = data.avatarText || "-";
        document.getElementById("detailName").textContent = data.name || "-";
        document.getElementById("detailEmail").textContent = data.email || "-";
        document.getElementById("detailUserId").textContent = data.userId || "-";
        document.getElementById("detailCreatedAt").textContent = formatDateTime(data.createdAt);
        document.getElementById("detailLoginMethod").textContent = data.loginMethodLabel || "-";
        document.getElementById("detailRoleBadge").textContent = data.roleLabel || "-";
        document.getElementById("detailAccountStatus").textContent = data.accountStatusLabel || "-";
        document.getElementById("detailProjectCount").textContent = `${data.projectCount || 0}개`;
        document.getElementById("detailAnalysisCount").textContent = `${data.totalAnalysisCount || 0}회`;
        document.getElementById("detailInquiryCount").textContent = `${data.inquiryCount || 0}건`;
        document.getElementById("detailLastActivity").textContent = formatDateTime(data.lastActivityAt);

        renderProjects(data.projects || []);
        renderRecentAnalyses(data.recentAnalyses || []);
        renderChart(data.chart || {});
    }

    async function loadDetail() {
        try {
            const response = await fetch(`/api/admin/users/${userId}`);
            if (!response.ok) {
                throw new Error("Failed to load admin user detail.");
            }

            const data = await response.json();
            applyDetail(data);
        } catch (error) {
            console.error(error);
        }
    }

    loadDetail();
});
