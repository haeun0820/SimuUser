let currentFilter = "all";
let searchQuery = "";
let logs = [];

document.addEventListener("DOMContentLoaded", function () {
    const filterBtns = document.querySelectorAll(".log-filter-btn");
    const searchInput = document.getElementById("logSearchInput");
    const tableBody = document.getElementById("logTableBody");

    function escapeHtml(value) {
        return String(value || "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");
    }

    function formatDateTime(value) {
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return "-";
        }
        return new Intl.DateTimeFormat("ko-KR", {
            year: "numeric",
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit",
            hour12: false
        }).format(date);
    }

    function render() {
        const filtered = logs.filter(log => {
            const matchesFilter = currentFilter === "all" || log.type === currentFilter;
            const textContent = `${log.typeLabel || ""} ${log.message || ""} ${log.createdAt || ""}`.toLowerCase();
            const matchesSearch = !searchQuery || textContent.includes(searchQuery);
            return matchesFilter && matchesSearch;
        });

        if (!filtered.length) {
            tableBody.innerHTML = '<tr><td colspan="3" style="text-align:center; color:#64748b;">표시할 로그가 없습니다.</td></tr>';
            return;
        }

        tableBody.innerHTML = filtered.map(log => `
            <tr class="log-row" data-type="${escapeHtml(log.type)}">
                <td class="log-type-col">${escapeHtml(log.typeLabel)}</td>
                <td>${escapeHtml(log.message)}</td>
                <td>${escapeHtml(formatDateTime(log.createdAt))}</td>
            </tr>
        `).join("");
    }

    async function loadLogs() {
        try {
            const response = await fetch("/api/admin/logs");
            if (!response.ok) {
                throw new Error("Failed to load admin logs.");
            }
            logs = await response.json();
        } catch (error) {
            console.error(error);
            logs = [];
        }
        render();
    }

    filterBtns.forEach(btn => {
        btn.addEventListener("click", function () {
            filterBtns.forEach(button => button.classList.remove("active"));
            this.classList.add("active");
            currentFilter = this.getAttribute("data-filter");
            render();
        });
    });

    searchInput.addEventListener("input", function (event) {
        searchQuery = event.target.value.toLowerCase().trim();
        render();
    });

    loadLogs();
});

function exportLogData() {
    const params = new URLSearchParams();
    if (searchQuery) {
        params.set("query", searchQuery);
    }
    if (currentFilter && currentFilter !== "all") {
        params.set("filter", currentFilter);
    }
    const queryString = params.toString();
    window.location.href = `/api/admin/logs/export${queryString ? `?${queryString}` : ""}`;
}
