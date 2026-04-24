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
    /* ── 수정된 시나리오 카드 생성 로직 ── */
function renderScenarioInputs() {
    const count = parseInt(scenarioCountSelect.value);
    scenarioContainer.innerHTML = '';
    scenarioContainer.className = `scenario-grid grid-${count}`;

    for (let i = 1; i <= count; i++) {
        const card = document.createElement('div');
        card.className = 'scenario-card';
        card.innerHTML = `
            <div class="form-group">
                <label class="form-label">시나리오 ${i} 이름</label>
                <input type="text" class="form-input" placeholder="예: A안 - 사용자 중심 UI">
            </div>

            <div class="form-group">
                <label class="form-label">기획안 파일 업로드</label>
                <div class="file-upload-wrapper">
                    <input type="file" id="file-scenario-${i}" class="file-input" style="display:none;" 
                           onchange="handleFileSelect(this, ${i})">
                    <button type="button" class="btn-file-trigger" onclick="document.getElementById('file-scenario-${i}').click()">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:5px;">
                            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M17 8l-5-5-5 5M12 3v12"/>
                        </svg>
                        파일 선택 (PDF, DOCX)
                    </button>
                    <div id="file-name-${i}" class="file-name-display">선택된 파일 없음</div>
                </div>
            </div>

            <div class="divider"><span>또는 직접 입력</span></div>

            <div class="form-group">
                <label class="form-label">시나리오 설명</label>
                <textarea class="form-input" rows="3" placeholder="파일이 없거나 추가 설명이 필요하면 입력하세요.."></textarea>
            </div>
            <div class="form-group feature-list">
                <label class="form-label">주요 기능</label>
                <div class="feature-item">
                    <input type="text" class="form-input" placeholder="기능 입력">
                </div>
            </div>
            <div class="add-feature-btn" onclick="addFeatureInput(this)">기능 추가 +</div>
        `;
        scenarioContainer.appendChild(card);
    }
}

/* ── 파일 선택 시 이름 표시 함수 ── */
window.handleFileSelect = function(input, index) {
    const fileNameDisplay = document.getElementById(`file-name-${index}`);
    if (input.files && input.files[0]) {
        fileNameDisplay.textContent = input.files[0].name;
        fileNameDisplay.style.color = "#2563eb"; // 선택 시 강조
    } else {
        fileNameDisplay.textContent = "선택된 파일 없음";
        fileNameDisplay.style.color = "#9ca3af";
    }
};

    scenarioCountSelect.addEventListener('change', renderScenarioInputs);
    renderScenarioInputs();
});


document.addEventListener('DOMContentLoaded', function() {
    const btnRunCompare = document.getElementById('btnRunCompare');
    const compareForm = document.getElementById('compareForm');

    compareForm.addEventListener('submit', function(e) {
        // 필요한 로직 처리 후 전송
        console.log("분석 시작...");
    });

    if (btnRunCompare) {
        btnRunCompare.addEventListener('click', function() {
            // 1. 선택된 프로젝트가 있는지, 시나리오 제목이 입력됐는지 등 유효성 검사를 추가할 수 있습니다.
            const compareTitle = document.getElementById('compareTitle').value;
            if (!compareTitle) {
                alert('비교 제목을 입력해주세요!');
                return;
            }

            // 2. 결과 페이지로 이동 (Thymeleaf 경로 또는 실제 파일 경로)
            // 예: /scenario/result 경로로 이동하고 싶을 때
            location.href = '/scenario/result'; 
        });
    }
});