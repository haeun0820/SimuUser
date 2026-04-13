// ============================================================
//  sidebar.js  –  SimuUser 공통 사이드바 컴포넌트
//  사용법: <script src="../project/sidebar.js"></script>
//          <div id="sidebar-root"></div>
//  옵션: data-active 속성으로 활성 메뉴 지정
//        e.g. <div id="sidebar-root" data-active="new-project"></div>
// ============================================================

(function () {
    const MENU_ITEMS = [
    {
        type: "link",
        id: "dashboard",
        icon: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <rect x="3" y="3" width="7" height="7" rx="1.5" stroke="currentColor" stroke-width="1.8"/>
                <rect x="14" y="3" width="7" height="7" rx="1.5" stroke="currentColor" stroke-width="1.8"/>
                <rect x="3" y="14" width="7" height="7" rx="1.5" stroke="currentColor" stroke-width="1.8"/>
                <rect x="14" y="14" width="7" height="7" rx="1.5" stroke="currentColor" stroke-width="1.8"/>
                </svg>`,
        label: "대시보드",
        href: "/dashboard"
    },
    { type: "section", label: "프로젝트" },
    {
        type: "link",
        id: "new-project",
        icon: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.8"/>
                <path d="M12 8v8M8 12h8" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                </svg>`,
        label: "새 프로젝트",
        href: "/project/new",
    },
    {
        type: "link",
        id: "all-projects",
        icon: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <rect x="3" y="5" width="18" height="14" rx="2" stroke="currentColor" stroke-width="1.8"/>
                <path d="M3 9h18" stroke="currentColor" stroke-width="1.8"/>
                <path d="M8 5V3M16 5V3" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                </svg>`,
        label: "전체 프로젝트",
        href: "/project/all",
    },
    { type: "section", label: "분석 도구" },
    {
        type: "link",
        id: "simulation",
        icon: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <path d="M12 3L3 8l9 5 9-5-9-5z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
                <path d="M3 16l9 5 9-5" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
                <path d="M3 12l9 5 9-5" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
                </svg>`,
        label: "AI 가상 유저 시뮬레이션",
        href: "#",
    },
    {
        type: "link",
        id: "market",
        icon: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <path d="M3 17l4-8 4 4 3-6 4 10" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>`,
        label: "시장 & 경쟁 분석",
        href: "#",
    },
    {
        type: "link",
        id: "cost",
        icon: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.8"/>
                <path d="M12 7v1.5M12 15.5V17M9.5 10a2.5 2 0 0 1 5 0c0 1.5-2.5 2-2.5 3.5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                </svg>`,
        label: "비용 & 수익성 분석",
        href: "#",
    },
    {
        type: "link",
        id: "feedback",
        icon: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <path d="M12 2a10 10 0 1 1 0 20 10 10 0 0 1 0-20z" stroke="currentColor" stroke-width="1.8"/>
                <path d="M12 8v4l3 3" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                </svg>`,
        label: "기획 & 피드백 AI",
        href: "#",
    },
    {
        type: "link",
        id: "scenario",
        icon: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <path d="M4 6h16M4 12h10M4 18h13" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                </svg>`,
        label: "시나리오 비교",
        href: "#",
    },
    {
        type: "link",
        id: "docs",
        icon: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <path d="M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9l-6-6z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
                <path d="M14 3v6h6M9 13h6M9 17h4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                </svg>`,
        label: "자동 문서화",
        href: "#",
        },
    ];

    function renderSidebar(activeId) {
        let menuHtml = "";

        MENU_ITEMS.forEach((item) => {
        if (item.type === "section") {
            menuHtml += `<div class="sb-section-label">${item.label}</div>`;
        } else {
            const isActive = item.id === activeId;
            menuHtml += `
            <a href="${item.href}" class="sb-menu-item${isActive ? " active" : ""}" data-id="${item.id}">
                <span class="sb-icon">${item.icon}</span>
                <span class="sb-label">${item.prefix ? item.prefix + " " : ""}${item.label}</span>
            </a>`;
        }
        });

        return `
        <aside class="sidebar">
            <div class="sb-top">
            <div class="sb-logo">
                <div class="sb-logo-icon">
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
                    <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                </div>
                <span class="sb-logo-text">SimuUser</span>
            </div>
            <nav class="sb-nav">
                ${menuHtml}
            </nav>
            </div>
            <div class="sb-bottom">
            <a href="#" class="sb-menu-item">
                <span class="sb-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
                </svg>
                </span>
                <span class="sb-label">문의</span>
            </a>
            <div class="sb-user">
                <div class="sb-avatar">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                    <circle cx="12" cy="8" r="4" stroke="white" stroke-width="1.8"/>
                    <path d="M4 20c0-4 4-6 8-6s8 2 8 6" stroke="white" stroke-width="1.8"/>
                </svg>
                </div>
                <div class="sb-user-info">
                <div class="sb-user-name">닉네임</div>
                <div class="sb-user-email">dkdlrh12@gmail.com</div>
                </div>
            </div>
            </div>
        </aside>`;
    }

    const sidebarCSS = `
        .sidebar {
        width: 240px;
        min-width: 240px;
        background: #0f1b2d;
        color: #a8b8cc;
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        height: 100vh;
        position: sticky;
        top: 0;
        overflow-y: auto;
        flex-shrink: 0;
        }
        .sb-top { padding: 20px 0 12px; }
        .sb-logo {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 0 20px 24px;
        }
        .sb-logo-icon {
        width: 36px;
        height: 36px;
        background: #2563eb;
        border-radius: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
        }
        .sb-logo-text {
        font-size: 17px;
        font-weight: 700;
        color: #ffffff;
        letter-spacing: -0.3px;
        }
        .sb-section-label {
        font-size: 10.5px;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.8px;
        color: #4a6080;
        padding: 16px 20px 6px;
        }
        .sb-menu-item {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 9px 16px;
        margin: 1px 8px;
        border-radius: 8px;
        text-decoration: none;
        color: #7a94b0;
        font-size: 13.5px;
        font-weight: 500;
        transition: background 0.15s, color 0.15s;
        cursor: pointer;
        }
        .sb-menu-item:hover {
        background: rgba(255,255,255,0.06);
        color: #c8d8e8;
        }
        .sb-menu-item.active {
        background: rgba(37,99,235,0.18);
        color: #60a5fa;
        }
        .sb-icon { flex-shrink: 0; display: flex; align-items: center; }
        .sb-label { line-height: 1; }
        .sb-bottom { padding: 0 0 16px; border-top: 1px solid rgba(255,255,255,0.06); }
        .sb-bottom > .sb-menu-item { margin-top: 8px; }
        .sb-user {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 10px 16px;
        margin: 4px 8px 0;
        }
        .sb-avatar {
        width: 32px;
        height: 32px;
        background: #2a3f58;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
        }
        .sb-user-name { font-size: 13px; font-weight: 600; color: #c8d8e8; }
        .sb-user-email { font-size: 11px; color: #4a6080; margin-top: 1px; }
    `;

    function init() {
        const root = document.getElementById("sidebar-root");
        if (!root) return;

        const activeId = root.dataset.active || "";

        // Inject CSS
        if (!document.getElementById("sidebar-style")) {
        const style = document.createElement("style");
        style.id = "sidebar-style";
        style.textContent = sidebarCSS;
        document.head.appendChild(style);
        }

        root.outerHTML = renderSidebar(activeId);
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
    })();