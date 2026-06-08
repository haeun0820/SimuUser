let currentMethodFilter = "all";
let searchQuery = "";

document.addEventListener("DOMContentLoaded", function () {
    const searchInput = document.getElementById("userSearchInput");
    const radioFilters = document.querySelectorAll('input[name="loginMethodFilter"]');
    const tableBody = document.getElementById("userTableBody");
    const userCountText = document.getElementById("userCountText");

    let users = [];

    function escapeHtml(value) {
        return String(value || "")
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");
    }

    function formatDate(value) {
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

    function loginMethodLabel(method) {
        const labels = {
            google: "Google",
            naver: "Naver",
            kakao: "Kakao",
            apple: "Apple",
            site: "Site ID"
        };
        return labels[method] || method || "-";
    }

    function applyFilters() {
        const filtered = users.filter(user => {
            const matchesMethod = currentMethodFilter === "all" || user.loginMethod === currentMethodFilter;
            const searchableText = `${user.name || ""} ${user.email || ""} ${user.userId || ""}`.toLowerCase();
            const matchesSearch = !searchQuery || searchableText.includes(searchQuery);
            return matchesMethod && matchesSearch;
        });

        userCountText.textContent = `총 ${filtered.length}명의 사용자`;
        tableBody.innerHTML = filtered.map(user => `
            <tr class="user-row" data-login-method="${escapeHtml(user.loginMethod)}">
                <td style="font-weight: 600;">${escapeHtml(user.name)}</td>
                <td>${escapeHtml(user.email || "-")}</td>
                <td>${escapeHtml(loginMethodLabel(user.loginMethod))}</td>
                <td>${escapeHtml(formatDate(user.createdAt))}</td>
                <td>
                    <button class="btn-action" type="button" onclick="location.href='/admin/user/detail/${user.id}'">상세보기</button>
                </td>
            </tr>
        `).join("");
    }

    async function loadUsers() {
        try {
            const response = await fetch("/api/admin/users");
            if (!response.ok) {
                throw new Error("Failed to load admin users.");
            }
            users = await response.json();
        } catch (error) {
            console.error(error);
            users = [];
        }

        applyFilters();
    }

    radioFilters.forEach(radio => {
        radio.addEventListener("change", function () {
            currentMethodFilter = this.value;
            applyFilters();
        });
    });

    searchInput.addEventListener("input", function (event) {
        searchQuery = event.target.value.toLowerCase().trim();
        applyFilters();
    });

    loadUsers();
});

function exportUserData() {
    const params = new URLSearchParams();
    if (typeof searchQuery === "string" && searchQuery) {
        params.set("query", searchQuery);
    }
    if (typeof currentMethodFilter === "string" && currentMethodFilter && currentMethodFilter !== "all") {
        params.set("loginMethod", currentMethodFilter);
    }
    const queryString = params.toString();
    window.location.href = `/api/admin/users/export${queryString ? `?${queryString}` : ""}`;
}
