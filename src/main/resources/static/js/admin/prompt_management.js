document.addEventListener("DOMContentLoaded", function () {
    const tabs = document.querySelectorAll(".prompt-tab");
    const searchInput = document.getElementById("promptSearchInput");
    const promptList = document.getElementById("promptList");
    const createBtn = document.querySelector(".btn-create");
    const promptModal = document.getElementById("promptModal");
    const closeBtn = document.getElementById("closePromptModal");
    const submitBtn = document.getElementById("submitPromptBtn");
    const titleEl = document.querySelector(".modal-title");
    const subtitleEl = document.querySelector(".modal-sub");

    const promptNameInput = document.getElementById("promptNameInput");
    const promptCategoryInput = document.getElementById("promptCategoryInput");
    const promptModelInput = document.getElementById("promptModelInput");
    const systemPromptInput = document.getElementById("systemPromptInput");
    const userPromptTemplateInput = document.getElementById("userPromptTemplateInput");

    let prompts = [];
    let currentFilter = "all";
    let searchQuery = "";
    let editingPromptId = null;

    const categoryLabels = {
        simulation: "AI 시뮬레이션",
        market: "시장 분석",
        profit: "수익성 분석",
        feedback: "기획 피드백",
        scenario: "시나리오 비교",
        document: "자동 문서화"
    };

    function csrfHeaders() {
        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute("content");
        const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content");
        return token && header ? { [header]: token } : {};
    }

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
            return "";
        }
        return new Intl.DateTimeFormat("ko-KR", {
            year: "numeric",
            month: "2-digit",
            day: "2-digit"
        }).format(date);
    }

    function resetForm() {
        editingPromptId = null;
        promptNameInput.value = "";
        promptCategoryInput.value = "simulation";
        promptModelInput.value = "gemini-2.5-flash";
        systemPromptInput.value = "";
        userPromptTemplateInput.value = "";
        titleEl.textContent = "AI 프롬프트 생성";
        subtitleEl.textContent = "새로운 AI 프롬프트를 등록합니다.";
        submitBtn.textContent = "생성";
    }

    function openModal() {
        promptModal.classList.add("active");
    }

    function closeModal() {
        promptModal.classList.remove("active");
    }

    function render() {
        const normalizedQuery = searchQuery.toLowerCase();
        const filtered = prompts.filter(prompt => {
            const matchesCategory = currentFilter === "all" || prompt.category === currentFilter;
            const searchTarget = [
                prompt.name,
                prompt.category,
                prompt.model,
                prompt.systemPrompt,
                prompt.userPromptTemplate
            ].join(" ").toLowerCase();
            const matchesSearch = !normalizedQuery || searchTarget.includes(normalizedQuery);
            return matchesCategory && matchesSearch;
        });

        if (!filtered.length) {
            promptList.innerHTML = '<div style="padding:40px;text-align:center;color:#94a3b8;">등록된 프롬프트가 없습니다.</div>';
            return;
        }

        promptList.innerHTML = filtered.map(prompt => `
            <div class="prompt-card" data-category="${escapeHtml(prompt.category)}">
                <div class="card-header">
                    <div class="card-title-group">
                        <h3 class="card-title">${escapeHtml(prompt.name)}</h3>
                        <span class="card-model">${escapeHtml(prompt.model || "gemini-2.5-flash")}</span>
                    </div>
                    <div class="card-actions">
                        <button class="btn-edit" data-action="edit" data-id="${prompt.id}">
                            <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"></path>
                            </svg>
                            수정
                        </button>
                        <button class="btn-edit" data-action="delete" data-id="${prompt.id}">삭제</button>
                        <span class="badge badge-blue">${escapeHtml(categoryLabels[prompt.category] || prompt.category)}</span>
                    </div>
                </div>
                <div class="card-body">
                    <div>
                        <div class="prompt-section-title">시스템 프롬프트</div>
                        <p>${escapeHtml(prompt.systemPrompt)}</p>
                    </div>
                    <div>
                        <div class="prompt-section-title">사용자 프롬프트 템플릿</div>
                        <p>${escapeHtml(prompt.userPromptTemplate)}</p>
                    </div>
                </div>
                <div class="card-footer">${formatDate(prompt.createdAt)}</div>
            </div>
        `).join("");
    }

    async function loadPrompts() {
        try {
            const response = await fetch("/api/prompts", { credentials: "same-origin" });
            if (!response.ok) {
                throw new Error("프롬프트 목록을 불러오지 못했습니다.");
            }
            prompts = await response.json();
        } catch (error) {
            console.error(error);
            prompts = [];
        }
        render();
    }

    function fillForm(prompt) {
        editingPromptId = prompt.id;
        promptNameInput.value = prompt.name || "";
        promptCategoryInput.value = prompt.category || "simulation";
        promptModelInput.value = prompt.model || "gemini-2.5-flash";
        systemPromptInput.value = prompt.systemPrompt || "";
        userPromptTemplateInput.value = prompt.userPromptTemplate || "";
        titleEl.textContent = "AI 프롬프트 수정";
        subtitleEl.textContent = "기존 프롬프트 내용을 수정합니다.";
        submitBtn.textContent = "수정";
    }

    tabs.forEach(tab => {
        tab.addEventListener("click", function () {
            tabs.forEach(item => item.classList.remove("active"));
            this.classList.add("active");
            currentFilter = this.dataset.filter || "all";
            render();
        });
    });

    searchInput.addEventListener("input", function (event) {
        searchQuery = event.target.value.trim();
        render();
    });

    createBtn?.addEventListener("click", function () {
        resetForm();
        openModal();
    });

    closeBtn?.addEventListener("click", closeModal);

    window.addEventListener("click", function (event) {
        if (event.target === promptModal) {
            closeModal();
        }
    });

    promptList.addEventListener("click", async function (event) {
        const button = event.target.closest("button[data-action]");
        if (!button) {
            return;
        }

        const prompt = prompts.find(item => String(item.id) === String(button.dataset.id));
        if (!prompt) {
            return;
        }

        if (button.dataset.action === "edit") {
            fillForm(prompt);
            openModal();
            return;
        }

        if (!window.confirm("이 프롬프트를 삭제하시겠습니까?")) {
            return;
        }

        try {
            const response = await fetch(`/api/admin/prompts/${prompt.id}`, {
                method: "DELETE",
                headers: csrfHeaders(),
                credentials: "same-origin"
            });

            if (!response.ok) {
                const error = await response.json().catch(() => ({}));
                throw new Error(error.message || "프롬프트 삭제에 실패했습니다.");
            }

            await loadPrompts();
        } catch (error) {
            window.alert(error.message);
        }
    });

    submitBtn?.addEventListener("click", async function () {
        const body = {
            name: promptNameInput.value.trim(),
            category: promptCategoryInput.value,
            model: promptModelInput.value.trim() || "gemini-2.5-flash",
            systemPrompt: systemPromptInput.value.trim(),
            userPromptTemplate: userPromptTemplateInput.value.trim()
        };

        if (!body.name || !body.category || !body.systemPrompt || !body.userPromptTemplate) {
            window.alert("프롬프트 이름, 카테고리, 시스템 프롬프트, 사용자 프롬프트 템플릿을 모두 입력해주세요.");
            return;
        }

        try {
            const response = await fetch(
                editingPromptId ? `/api/admin/prompts/${editingPromptId}` : "/api/admin/prompts",
                {
                    method: editingPromptId ? "PUT" : "POST",
                    headers: {
                        "Content-Type": "application/json",
                        ...csrfHeaders()
                    },
                    credentials: "same-origin",
                    body: JSON.stringify(body)
                }
            );

            if (!response.ok) {
                const error = await response.json().catch(() => ({}));
                throw new Error(error.message || "프롬프트 저장에 실패했습니다.");
            }

            closeModal();
            resetForm();
            await loadPrompts();
        } catch (error) {
            window.alert(error.message);
        }
    });

    resetForm();
    loadPrompts();
});
