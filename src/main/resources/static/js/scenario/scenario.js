/* ── 시간 포맷 & 유틸리티 ── */
function timeAgo(isoStr) {
    if (!isoStr) return '';
    const diff = Date.now() - new Date(isoStr).getTime();
    const min = Math.floor(diff / 60000);
    if (min < 1) return '방금 전';
    if (min < 60) return `${min}분 전`;
    const hr = Math.floor(min / 60);
    if (hr < 24) return `${hr}시간 전`;
    return `${Math.floor(hr / 24)}일 전`;
}

function escHtml(str) {
    return String(str)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;')
        .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

/* ── 상태 ── */
let allProjects = [];
let currentFilter = 'all';
let selectedProjectId = null;

/* ── 프로젝트 로드 ── */
function loadProjects() {
    try {
        // localStorage에서 데이터를 가져옵니다. 
        // 데이터가 없다면 빈 배열을 기본값으로 사용합니다.
        allProjects = JSON.parse(localStorage.getItem('simu_projects') || '[]');
    } catch (e) {
        allProjects = [];
    }
}

/* ── 프로젝트 렌더링 ── */
function renderProjects() {
    const container = document.getElementById('projectListScroll');
    if (!container) return;

    const filtered = allProjects.filter(p => {
        if (currentFilter === 'all') return true;
        return p.type === currentFilter;
    });

    if (filtered.length === 0) {
        container.innerHTML = `<div class="no-projects" style="padding:20px; text-align:center; color:#9ca3af;">등록된 프로젝트가 없습니다.</div>`;
        return;
    }

    container.innerHTML = filtered.map(p => {
        const isCollab = p.type === 'collab';
        const badge = isCollab
            ? '<span class="type-badge badge-collab">협업</span>'
            : '<span class="type-badge badge-personal">개인</span>';
        const memberHtml = isCollab && p.members && p.members.length
            ? `<span class="members" style="margin-left:10px; font-size:12px; color:#6b7280;">With. ${p.members.map(escHtml).join(', ')}</span>`
            : '';

        return `
            <div class="project-item${selectedProjectId === p.id ? ' selected' : ''}" 
                 data-id="${p.id}" style="margin-bottom:10px; cursor:pointer;">
                <div class="project-item-head">
                    <span class="project-item-title" style="font-weight:700;">${escHtml(p.title)}</span>
                    ${badge}
                </div>
                <p class="project-item-desc" style="font-size:13px; color:#6b7280; margin:4px 0;">${escHtml(p.description || '')}</p>
                <div class="project-item-footer" style="font-size:12px; color:#9ca3af;">
                    <span>${timeAgo(p.createdAt)}</span>
                    ${memberHtml}
                </div>
            </div>`;
    }).join('');

    // 클릭 이벤트 바인딩
    container.querySelectorAll('.project-item').forEach(el => {
        el.addEventListener('click', () => {
            const id = Number(el.dataset.id);
            // 이미 선택된 걸 다시 누르면 선택 취소, 아니면 새로 선택
            selectedProjectId = (selectedProjectId === id) ? null : id;
            updateSelection();
        });
    });
}

function updateSelection() {
    document.querySelectorAll('.project-item').forEach(el => {
        el.classList.toggle('selected', Number(el.dataset.id) === selectedProjectId);
    });
}

/* ── 기능 입력창 추가 (전역) ── */
window.addFeatureInput = function(btn) {
    const featureList = btn.closest('.scenario-card').querySelector('.feature-list');
    if (featureList) {
        const newItem = document.createElement('div');
        newItem.className = 'feature-item';
        newItem.style.marginTop = '8px';
        newItem.innerHTML = `<input type="text" class="form-input" placeholder="새 기능 입력">`;
        featureList.appendChild(newItem);
    }
};

/* ── 메인 실행부 ── */
document.addEventListener('DOMContentLoaded', function() {
    const scenarioCountSelect = document.getElementById('scenarioCount');
    const scenarioContainer = document.getElementById('scenarioContainer');
    const btnNewProject = document.getElementById('btnNewProject');

    // 1. 초기 데이터 로드 및 렌더링 (이 부분이 누락되었었습니다)
    loadProjects();
    renderProjects();

    // 2. 필터 이벤트 등록
    document.querySelectorAll('input[name="projectFilter"]').forEach(radio => {
        radio.addEventListener('change', () => {
            currentFilter = radio.value;
            renderProjects();
        });
    });

    // 3. 새로 만들기 버튼 클릭 이벤트
    if (btnNewProject) {
        btnNewProject.addEventListener('click', function() {
            const url = this.getAttribute('data-href');
            if (url) location.href = url;
        });
    }

    // 4. 시나리오 카드 생성 로직
    function renderScenarioInputs() {
        const count = parseInt(scenarioCountSelect.value);
        scenarioContainer.innerHTML = '';
        scenarioContainer.className = `scenario-grid grid-${count}`;

        for (let i = 1; i <= count; i++) {
            const card = document.createElement('div');
            card.className = 'scenario-card';
            card.innerHTML = `
                <div class="form-group">
                    <label class="form-label">시나리오 이름</label>
                    <input type="text" class="form-input" placeholder="시나리오 이름을 적어주세요..">
                </div>
                <div class="form-group">
                    <label class="form-label">시나리오 설명</label>
                    <textarea class="form-input" rows="4" placeholder="핵심 접근 방식을 설명 해주세요.."></textarea>
                </div>
                <div class="form-group feature-list">
                    <label class="form-label">주요 기능</label>
                    <div class="feature-item">
                        <input type="text" class="form-input" placeholder="기능을 입력하세요">
                    </div>
                </div>
                <div class="add-feature-btn" onclick="addFeatureInput(this)" style="cursor:pointer; color:#2563eb;">기능 추가 +</div>
            `;
            scenarioContainer.appendChild(card);
        }
    }

    scenarioCountSelect.addEventListener('change', renderScenarioInputs);
    renderScenarioInputs();
});