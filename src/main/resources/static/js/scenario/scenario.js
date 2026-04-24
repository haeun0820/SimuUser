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
            <div class="scenario-card-header">
                <span class="scenario-num">시나리오 ${i}</span>
                <input type="text" class="form-input-title" placeholder="시나리오 제목 입력">
            </div>

            <div class="input-mode-selector">
                <label class="mode-radio">
                    <input type="radio" name="mode-${i}" value="upload" onchange="toggleInputMode(${i}, 'upload')" checked>
                    <span class="radio-btn">내 PC 업로드</span>
                </label>
                <label class="mode-radio">
                    <input type="radio" name="mode-${i}" value="project" onchange="toggleInputMode(${i}, 'project')">
                    <span class="radio-btn">프로젝트 문서</span>
                </label>
                <label class="mode-radio">
                    <input type="radio" name="mode-${i}" value="direct" onchange="toggleInputMode(${i}, 'direct')">
                    <span class="radio-btn">직접 입력</span>
                </label>
            </div>

            <div id="mode-content-${i}" class="mode-content-area">
                ${getUploadUI(i)}
            </div>
        `;
        scenarioContainer.appendChild(card);
    }
}

/* ── 파일 선택 시 이름 표시 함수 (이름 통일) ── */
window.updateFileName = function(index) {
    const fileInput = document.getElementById(`file-${index}`);
    const fileNameDisplay = document.getElementById(`file-name-${index}`);
    
    if (fileInput.files && fileInput.files[0]) {
        const fileName = fileInput.files[0].name;
        fileNameDisplay.textContent = fileName;
        fileNameDisplay.style.color = "#2563eb"; // 파란색으로 포인트
        fileNameDisplay.style.fontWeight = "600";
    } else {
        fileNameDisplay.textContent = "파일을 드래그하거나 클릭하여 업로드하세요";
        fileNameDisplay.style.color = "#6b7280";
        fileNameDisplay.style.fontWeight = "400";
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

/* ── 모드 전환 함수 ── */
window.toggleInputMode = function(index, mode) {
    const container = document.getElementById(`mode-content-${index}`);
    if (mode === 'upload') {
        container.innerHTML = getUploadUI(index);
    } else if (mode === 'project') {
        container.innerHTML = getProjectTreeUI(index);
    } else if (mode === 'direct') {
        container.innerHTML = getDirectInputUI(index);
    }
};

/* ── 다중 파일 상태 관리를 위한 전역 객체 ── */
window.scenarioFiles = {}; // 예: { 1: [File, File], 2: [File] }

/* ── 모드 전환 1. 내 PC 다중 업로드 UI ── */
function getUploadUI(i) {
    // 해당 시나리오의 파일 배열 초기화
    if (!window.scenarioFiles[i]) window.scenarioFiles[i] = [];

    return `
        <div class="upload-limit-warning">* 파일은 최대 5개까지 업로드 가능합니다.</div>

        <div class="file-drop-zone" id="drop-zone-${i}">
            <input type="file" id="file-${i}" multiple style="display:none" onchange="handleMultiFiles(event, ${i})">
            
            <div class="file-drop-content" onclick="document.getElementById('file-${i}').click()">
                <p style="margin: 0 0 4px 0; color: #4b5563; font-weight: 500;">파일을 드래그하거나 클릭하여 업로드하세요</p>
                <span class="file-info" style="color: #9ca3af; font-size: 12px;">PDF, DOCX (각 20MB 제한)</span>
            </div>
        </div>

        <div class="file-list-container" id="file-list-${i}"></div>
    `;
}

/* ── 다중 파일 추가 로직 ── */
window.handleMultiFiles = function(event, index) {
    const newFiles = Array.from(event.target.files);
    let currentFiles = window.scenarioFiles[index] || [];
    
    // 최대 5개 개수 제한 체크
    if (currentFiles.length + newFiles.length > 5) {
        alert("하나의 시나리오당 파일은 최대 5개까지만 업로드할 수 있습니다.");
        // 5개가 넘어가면 들어갈 수 있는 만큼만 자르기
        const allowedCount = 5 - currentFiles.length;
        currentFiles = currentFiles.concat(newFiles.slice(0, allowedCount));
    } else {
        currentFiles = currentFiles.concat(newFiles);
    }
    
    window.scenarioFiles[index] = currentFiles;
    event.target.value = ""; // 입력창 초기화 (같은 파일 반복 선택 가능하도록)
    
    renderFileList(index);
};

/* ── 특정 파일 삭제 로직 ── */
window.removeMultiFile = function(scenarioIndex, fileIndex) {
    window.scenarioFiles[scenarioIndex].splice(fileIndex, 1);
    renderFileList(scenarioIndex);
};

/* ── 파일 리스트 UI 그리기 ── */
window.renderFileList = function(index) {
    const listContainer = document.getElementById(`file-list-${index}`);
    const dropZone = document.getElementById(`drop-zone-${index}`);
    const files = window.scenarioFiles[index] || [];
    
    // 파일이 하나라도 있으면 드롭존 테두리 파란색 유지
    if (files.length > 0) {
        dropZone.classList.add("has-file");
    } else {
        dropZone.classList.remove("has-file");
    }
    
    // 파일 목록 HTML 생성 (파란색 문서 아이콘 + 파일명 + X 버튼)
    listContainer.innerHTML = files.map((file, fIndex) => `
        <div class="file-list-item">
            <div class="file-item-info">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#2563eb" stroke-width="2">
                    <path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"></path>
                    <polyline points="13 2 13 9 20 9"></polyline>
                </svg>
                <span class="file-item-name">${file.name}</span>
            </div>
            <button type="button" class="btn-item-remove" onclick="removeMultiFile(${index}, ${fIndex})" title="삭제">✕</button>
        </div>
    `).join('');
};

/* 2. 프로젝트 문서 트리뷰 UI */
function getProjectTreeUI(i) {
    // 실제 데이터는 allProjects 등에서 가져와서 매핑 가능
    return `
        <div class="project-tree-view">
            <p class="tree-guide">분석할 문서를 선택하세요</p>
            <details open>
                <summary>📁 기획안 탭</summary>
                <ul>
                    <li><label><input type="checkbox"> 📄 기능명세서_v1.pdf</label></li>
                    <li><label><input type="checkbox"> 📄 와이어프레임_최종.docx</label></li>
                </ul>
            </details>
            <details>
                <summary>📁 리서치 탭</summary>
                <ul>
                    <li><label><input type="checkbox"> 📄 경쟁사분석.pdf</label></li>
                </ul>
            </details>
        </div>`;
}

/* ── 모드 전환 3. 직접 입력 UI (소제목 추가) ── */
function getDirectInputUI(i) {
    return `
        <div class="form-group">
            <label class="form-label">기획 설명</label>
            <textarea class="form-input" rows="4" placeholder="기획 내용을 상세히 입력해주세요.."></textarea>
        </div>
        <div class="form-group feature-list" id="feature-list-${i}">
            <label class="form-label">기획 기능</label>
            <div class="feature-item"><input type="text" class="form-input" placeholder="주요 기능 입력"></div>
        </div>
        <div class="add-feature-btn" onclick="addFeatureInput(this)">+ 기능 추가</div>`;
}