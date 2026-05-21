document.addEventListener("DOMContentLoaded", function () {
    const tabs = document.querySelectorAll('.prompt-tab');
    const searchInput = document.getElementById('promptSearchInput');
    const promptList = document.getElementById('promptList');
    const createBtn = document.querySelector('.btn-create');
    const promptModal = document.getElementById('promptModal');
    const closeBtn = document.getElementById('closePromptModal');
    const submitBtn = document.getElementById('submitPromptBtn');

    let prompts = [];
    let currentFilter = 'all';
    let searchQuery = '';
    let editingPromptId = null;

    const categoryLabels = {
        simulation: 'AI 시뮬레이션',
        market: '시장 분석',
        profit: '수익성 분석',
        feedback: '기획 피드백',
        scenario: '시나리오 비교',
        document: '자동 문서화'
    };

    function csrfHeaders() {
        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content')
            || document.getElementById('csrfToken')?.value;
        const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content')
            || document.getElementById('csrfHeader')?.value;
        return token && header ? { [header]: token } : {};
    }

    function escapeHtml(value) {
        return String(value || '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function formatDate(value) {
        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? '' : new Intl.DateTimeFormat('ko-KR').format(date);
    }

    function render() {
        const filtered = prompts.filter(prompt => {
            const matchesCategory = currentFilter === 'all' || prompt.category === currentFilter;
            const text = `${prompt.name} ${prompt.model} ${prompt.systemPrompt} ${prompt.userPromptTemplate}`.toLowerCase();
            return matchesCategory && (!searchQuery || text.includes(searchQuery));
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
                        <span class="card-model">${escapeHtml(prompt.model)}</span>
                    </div>
                    <div class="card-actions">
                        <button class="btn-edit" data-action="edit" data-id="${prompt.id}">
                            <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"></path></svg>
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
        `).join('');
    }

    async function loadPrompts() {
        try {
            const response = await fetch('/api/prompts');
            if (!response.ok) throw new Error('프롬프트 목록을 불러오지 못했습니다.');
            prompts = await response.json();
        } catch (error) {
            console.error(error);
            prompts = [];
        }
        render();
    }

    tabs.forEach(tab => {
        tab.addEventListener('click', function () {
            tabs.forEach(t => t.classList.remove('active'));
            this.classList.add('active');
            currentFilter = this.getAttribute('data-filter');
            render();
        });
    });

    searchInput.addEventListener('input', function (e) {
        searchQuery = e.target.value.toLowerCase().trim();
        render();
    });

    createBtn?.addEventListener('click', function () {
        editingPromptId = null;
        document.querySelector('.modal-title').textContent = 'AI 프롬프트 생성';
        submitBtn.textContent = '생성';
        document.getElementById('promptNameInput').value = '';
        document.getElementById('promptCategoryInput').value = 'simulation';
        document.getElementById('systemPromptInput').value = '';
        document.getElementById('userPromptTemplateInput').value = '';
        promptModal.classList.add('active');
    });

    closeBtn?.addEventListener('click', function () {
        promptModal.classList.remove('active');
    });

    window.addEventListener('click', function (e) {
        if (e.target === promptModal) {
            promptModal.classList.remove('active');
        }
    });

    promptList.addEventListener('click', async function (event) {
        const button = event.target.closest('button[data-action]');
        if (!button) return;

        const prompt = prompts.find(item => String(item.id) === String(button.dataset.id));
        if (!prompt) return;

        if (button.dataset.action === 'edit') {
            editingPromptId = prompt.id;
            document.querySelector('.modal-title').textContent = 'AI 프롬프트 수정';
            submitBtn.textContent = '수정';
            document.getElementById('promptNameInput').value = prompt.name || '';
            document.getElementById('promptCategoryInput').value = prompt.category || 'simulation';
            document.getElementById('systemPromptInput').value = prompt.systemPrompt || '';
            document.getElementById('userPromptTemplateInput').value = prompt.userPromptTemplate || '';
            promptModal.classList.add('active');
            return;
        }

        if (!confirm('이 프롬프트를 삭제하시겠습니까?')) return;

        try {
            const response = await fetch(`/api/admin/prompts/${prompt.id}`, {
                method: 'DELETE',
                headers: csrfHeaders()
            });
            if (!response.ok) {
                const error = await response.json().catch(() => ({}));
                throw new Error(error.message || '프롬프트 삭제에 실패했습니다.');
            }
            await loadPrompts();
        } catch (error) {
            alert(error.message);
        }
    });

    submitBtn?.addEventListener('click', async function () {
        const body = {
            name: document.getElementById('promptNameInput').value.trim(),
            category: document.getElementById('promptCategoryInput').value,
            model: 'gemini-2.5-flash',
            systemPrompt: document.getElementById('systemPromptInput').value.trim(),
            userPromptTemplate: document.getElementById('userPromptTemplateInput').value.trim()
        };

        if (!body.name || !body.category || !body.systemPrompt || !body.userPromptTemplate) {
            alert('프롬프트 이름, 카테고리, 시스템 프롬프트, 사용자 프롬프트 템플릿을 모두 입력해주세요.');
            return;
        }

        try {
            const response = await fetch(editingPromptId ? `/api/admin/prompts/${editingPromptId}` : '/api/admin/prompts', {
                method: editingPromptId ? 'PUT' : 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    ...csrfHeaders()
                },
                body: JSON.stringify(body)
            });
            if (!response.ok) {
                const error = await response.json().catch(() => ({}));
                throw new Error(error.message || '프롬프트 생성에 실패했습니다.');
            }
            promptModal.classList.remove('active');
            editingPromptId = null;
            document.getElementById('promptNameInput').value = '';
            document.getElementById('systemPromptInput').value = '';
            document.getElementById('userPromptTemplateInput').value = '';
            await loadPrompts();
        } catch (error) {
            alert(error.message);
        }
    });

    loadPrompts();
});
