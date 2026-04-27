(function () {
  let allProjects = [];
  let currentFilter = 'all';
  let selectedProjectId = null;
  const initialProjectId = new URLSearchParams(window.location.search).get('projectId');
  const draftKey = 'scenarioDraft';

  function escHtml(value) {
    return String(value ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

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

  async function fetchProjects() {
    try {
      const response = await fetch('/api/projects');
      allProjects = response.ok ? await response.json() : [];
    } catch (error) {
      console.error(error);
      allProjects = [];
    }

    restoreDraft();
    if (!selectedProjectId && initialProjectId && allProjects.some(project => String(project.id) === String(initialProjectId))) {
      selectedProjectId = String(initialProjectId);
    }

    renderProjects();
    updateSelection();
  }

  function renderProjects() {
    const container = document.getElementById('projectListScroll');
    if (!container) return;

    const filtered = allProjects.filter(project => currentFilter === 'all' || project.type === currentFilter);
    if (!filtered.length) {
      container.innerHTML = '<div class="no-projects" style="padding:20px;text-align:center;color:#9ca3af;">선택 가능한 프로젝트가 없습니다.</div>';
      return;
    }

    container.innerHTML = filtered.map(project => `
      <div class="project-item ${String(project.id) === String(selectedProjectId) ? 'selected' : ''}" data-id="${project.id}" style="margin-bottom:10px;cursor:pointer;">
        <div class="project-item-head">
          <span class="project-item-title" style="font-weight:700;">${escHtml(project.title)}</span>
          <span class="type-badge badge-${project.type === 'collab' ? 'collab' : 'personal'}">${project.type === 'collab' ? '협업' : '개인'}</span>
        </div>
        <p class="project-item-desc" style="font-size:13px;color:#6b7280;margin:4px 0;">${escHtml(project.description || '')}</p>
        <div class="project-item-footer" style="font-size:12px;color:#9ca3af;">${timeAgo(project.createdAt)}</div>
      </div>
    `).join('');

    container.querySelectorAll('.project-item').forEach(item => {
      item.addEventListener('click', () => {
        selectedProjectId = item.dataset.id;
        updateSelection();
      });
    });
  }

  function updateSelection() {
    document.querySelectorAll('.project-item').forEach(item => {
      item.classList.toggle('selected', String(item.dataset.id) === String(selectedProjectId));
    });

    const projectInput = document.getElementById('selectedProjectIdInput');
    if (projectInput) {
      projectInput.value = selectedProjectId || '';
    }
  }

  function restoreDraft() {
    const raw = sessionStorage.getItem(draftKey);
    if (!raw) return;

    try {
      const draft = JSON.parse(raw);
      selectedProjectId = draft.projectId ? String(draft.projectId) : selectedProjectId;
      const titleInput = document.getElementById('compareTitle');
      if (titleInput) {
        titleInput.value = draft.compareTitle || '';
      }
      if (draft.scenarioCount) {
        const countSelect = document.getElementById('scenarioCount');
        if (countSelect) {
          countSelect.value = String(draft.scenarioCount);
        }
      }
    } catch (error) {
      console.error(error);
    } finally {
      sessionStorage.removeItem(draftKey);
    }
  }

  function getUploadUI(index) {
    if (!window.scenarioFiles[index]) window.scenarioFiles[index] = [];
    return `
      <div class="upload-limit-warning">파일은 최대 5개까지 업로드할 수 있습니다.</div>
      <div class="file-drop-zone" id="drop-zone-${index}">
        <input type="file" id="file-${index}" multiple style="display:none" onchange="handleMultiFiles(event, ${index})">
        <div class="file-drop-content" onclick="document.getElementById('file-${index}').click()">
          <p style="margin:0 0 4px 0;color:#4b5563;font-weight:500;">파일을 클릭해서 선택하세요</p>
          <span class="file-info" style="color:#9ca3af;font-size:12px;">PDF, DOCX 이름만 비교 정보에 반영됩니다.</span>
        </div>
      </div>
      <div class="file-list-container" id="file-list-${index}"></div>
    `;
  }

  function getProjectTreeUI(index) {
    return `
      <div class="project-tree-view">
        <p class="tree-guide">프로젝트 문서 모드는 현재 프로젝트 설명 기반 비교로 처리됩니다.</p>
        <div class="form-group">
          <label class="form-label">비교 메모</label>
          <textarea class="form-input scenario-summary" rows="4" placeholder="이 시나리오에서 강조할 포인트를 적어주세요."></textarea>
        </div>
      </div>
    `;
  }

  function getDirectInputUI() {
    return `
      <div class="form-group">
        <label class="form-label">기획 설명</label>
        <textarea class="form-input scenario-summary" rows="4" placeholder="기획 내용을 간단히 입력해주세요."></textarea>
      </div>
      <div class="form-group feature-list">
        <label class="form-label">핵심 기능</label>
        <div class="feature-item"><input type="text" class="form-input scenario-feature" placeholder="주요 기능 입력"></div>
      </div>
      <div class="add-feature-btn" onclick="addFeatureInput(this)">+ 기능 추가</div>
    `;
  }

  function renderScenarioInputs() {
    const scenarioCount = Number(document.getElementById('scenarioCount')?.value || 2);
    const container = document.getElementById('scenarioContainer');
    if (!container) return;

    container.innerHTML = '';
    container.className = `scenario-grid grid-${scenarioCount}`;
    window.scenarioFiles = {};

    for (let index = 1; index <= scenarioCount; index++) {
      const card = document.createElement('div');
      card.className = 'scenario-card';
      card.dataset.index = String(index);
      card.innerHTML = `
        <div class="scenario-card-header">
          <span class="scenario-num">시나리오 ${index}</span>
          <input type="text" class="form-input-title scenario-title" placeholder="시나리오 제목 입력">
        </div>
        <div class="input-mode-selector">
          <label class="mode-radio">
            <input type="radio" name="mode-${index}" value="upload" onchange="toggleInputMode(${index}, 'upload')" checked>
            <span class="radio-btn">파일 업로드</span>
          </label>
          <label class="mode-radio">
            <input type="radio" name="mode-${index}" value="project" onchange="toggleInputMode(${index}, 'project')">
            <span class="radio-btn">프로젝트 문서</span>
          </label>
          <label class="mode-radio">
            <input type="radio" name="mode-${index}" value="direct" onchange="toggleInputMode(${index}, 'direct')">
            <span class="radio-btn">직접 입력</span>
          </label>
        </div>
        <div id="mode-content-${index}" class="mode-content-area">${getUploadUI(index)}</div>
      `;
      container.appendChild(card);
    }

    restoreScenarioDraftData();
  }

  function restoreScenarioDraftData() {
    const raw = sessionStorage.getItem(`${draftKey}:scenarios`);
    if (!raw) return;

    try {
      const scenarios = JSON.parse(raw);
      scenarios.forEach((scenario, index) => {
        const cardIndex = index + 1;
        const titleInput = document.querySelector(`.scenario-card[data-index="${cardIndex}"] .scenario-title`);
        if (titleInput) {
          titleInput.value = scenario.title || '';
        }

        const mode = scenario.mode || 'upload';
        const modeRadio = document.querySelector(`input[name="mode-${cardIndex}"][value="${mode}"]`);
        if (modeRadio) {
          modeRadio.checked = true;
          window.toggleInputMode(cardIndex, mode);
        }

        const card = document.querySelector(`.scenario-card[data-index="${cardIndex}"]`);
        if (!card) return;
        const summary = card.querySelector('.scenario-summary');
        if (summary) {
          summary.value = scenario.summary || '';
        }

        const featureList = card.querySelector('.feature-list');
        if (featureList && Array.isArray(scenario.features) && scenario.features.length) {
          featureList.innerHTML = '<label class="form-label">핵심 기능</label>';
          scenario.features.forEach(feature => {
            const item = document.createElement('div');
            item.className = 'feature-item';
            item.style.marginTop = '8px';
            item.innerHTML = `<input type="text" class="form-input scenario-feature" value="${escHtml(feature)}">`;
            featureList.appendChild(item);
          });
        }
      });
    } catch (error) {
      console.error(error);
    } finally {
      sessionStorage.removeItem(`${draftKey}:scenarios`);
    }
  }

  function collectScenarios() {
    return Array.from(document.querySelectorAll('.scenario-card')).map(card => {
      const index = Number(card.dataset.index);
      const title = card.querySelector('.scenario-title')?.value?.trim() || `시나리오 ${index}`;
      const mode = card.querySelector(`input[name="mode-${index}"]:checked`)?.value || 'upload';
      const summary = card.querySelector('.scenario-summary')?.value?.trim() || '';
      const features = Array.from(card.querySelectorAll('.scenario-feature'))
        .map(input => input.value.trim())
        .filter(Boolean);
      const references = mode === 'upload'
        ? (window.scenarioFiles[index] || []).map(file => file.name)
        : [];

      return { title, mode, summary, features, references };
    });
  }

  function validateBeforeSubmit() {
    if (!selectedProjectId) {
      alert('프로젝트를 먼저 선택해주세요.');
      return false;
    }

    const compareTitle = document.getElementById('compareTitle')?.value?.trim() || '';
    if (!compareTitle) {
      alert('비교 제목을 입력해주세요.');
      return false;
    }

    const scenarios = collectScenarios();
    const validScenarioCount = scenarios.filter(item => item.title || item.summary || item.features.length || item.references.length).length;
    if (validScenarioCount < 2) {
      alert('최소 2개의 시나리오를 채워주세요.');
      return false;
    }

    document.getElementById('selectedProjectIdInput').value = selectedProjectId;
    document.getElementById('compareTitleHiddenInput').value = compareTitle;
    document.getElementById('scenarioPayloadInput').value = JSON.stringify(scenarios);
    sessionStorage.setItem(draftKey, JSON.stringify({
      projectId: selectedProjectId,
      compareTitle,
      scenarioCount: scenarios.length
    }));
    sessionStorage.setItem(`${draftKey}:scenarios`, JSON.stringify(scenarios));
    return true;
  }

  window.addFeatureInput = function (btn) {
    const featureList = btn.previousElementSibling;
    if (!featureList) return;
    const item = document.createElement('div');
    item.className = 'feature-item';
    item.style.marginTop = '8px';
    item.innerHTML = '<input type="text" class="form-input scenario-feature" placeholder="추가 기능 입력">';
    featureList.appendChild(item);
  };

  window.toggleInputMode = function (index, mode) {
    const container = document.getElementById(`mode-content-${index}`);
    if (!container) return;
    if (mode === 'upload') {
      container.innerHTML = getUploadUI(index);
    } else if (mode === 'project') {
      container.innerHTML = getProjectTreeUI(index);
    } else {
      container.innerHTML = getDirectInputUI(index);
    }
  };

  window.scenarioFiles = {};

  window.handleMultiFiles = function (event, index) {
    const newFiles = Array.from(event.target.files || []);
    const currentFiles = window.scenarioFiles[index] || [];
    window.scenarioFiles[index] = currentFiles.concat(newFiles).slice(0, 5);
    event.target.value = '';
    renderFileList(index);
  };

  window.removeMultiFile = function (scenarioIndex, fileIndex) {
    window.scenarioFiles[scenarioIndex].splice(fileIndex, 1);
    renderFileList(scenarioIndex);
  };

  function renderFileList(index) {
    const listContainer = document.getElementById(`file-list-${index}`);
    const dropZone = document.getElementById(`drop-zone-${index}`);
    const files = window.scenarioFiles[index] || [];
    if (!listContainer || !dropZone) return;

    dropZone.classList.toggle('has-file', files.length > 0);
    listContainer.innerHTML = files.map((file, fileIndex) => `
      <div class="file-list-item">
        <div class="file-item-info">
          <span class="file-item-name">${escHtml(file.name)}</span>
        </div>
        <button type="button" class="btn-item-remove" onclick="removeMultiFile(${index}, ${fileIndex})" title="삭제">X</button>
      </div>
    `).join('');
  }

  function init() {
    document.querySelectorAll('input[name="projectFilter"]').forEach(radio => {
      radio.addEventListener('change', event => {
        currentFilter = event.target.value;
        renderProjects();
      });
    });

    document.getElementById('scenarioCount')?.addEventListener('change', renderScenarioInputs);
    document.getElementById('btnNewProject')?.addEventListener('click', function () {
      const url = this.getAttribute('data-href');
      if (url) window.location.href = url;
    });

    document.getElementById('compareForm')?.addEventListener('submit', event => {
      if (!validateBeforeSubmit()) {
        event.preventDefault();
      }
    });

    renderScenarioInputs();
    fetchProjects();
  }

  document.addEventListener('DOMContentLoaded', init);
})();
