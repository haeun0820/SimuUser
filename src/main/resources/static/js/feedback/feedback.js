(function () {
  let allProjects = [];
  let selectedProjectId = null;
  let currentFilter = 'all';
  const initialProjectId = new URLSearchParams(window.location.search).get('projectId');

  /* ── 프로젝트 리스트 관련 (기존 로직 유지) ── */
  async function fetchProjects() {
    try {
      const res = await fetch('/api/projects');
      allProjects = res.ok ? await res.json() : JSON.parse(localStorage.getItem('simu_projects') || '[]');
    } catch {
      allProjects = JSON.parse(localStorage.getItem('simu_projects') || '[]');
    }
    if (initialProjectId && allProjects.some(p => String(p.id) === String(initialProjectId))) {
      selectedProjectId = initialProjectId;
    }
    renderProjectList();
    updateSubmitButton();
  }

  function renderProjectList() {
    const listEl = document.getElementById('projectList');
    if (!listEl) return;
    
    let filtered = currentFilter === 'all' ? allProjects : allProjects.filter(p => p.type === currentFilter);
    
    listEl.innerHTML = filtered.map(p => `
      <div class="project-item ${String(p.id) === String(selectedProjectId) ? 'selected' : ''}" data-id="${p.id}">
        <div class="project-item-head">
          <span class="project-item-title">${p.title}</span>
          <span class="project-type-badge badge-${p.type}">${p.type === 'collab' ? '협업' : '개인'}</span>
        </div>
        <p class="project-item-desc">${p.description || '설명이 없습니다.'}</p>
        <div class="item-check"><svg width="14" height="14" viewBox="0 0 24 24" fill="none"><polyline points="20 6 9 17 4 12" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/></svg></div>
      </div>
    `).join('');

    listEl.querySelectorAll('.project-item').forEach(item => {
      item.addEventListener('click', () => {
        selectedProjectId = item.dataset.id;
        renderProjectList();
        updateSubmitButton();
      });
    });
  }

  /* ── 분석 방법 전환 로직 ── */
  function initMethodTabs() {
    const radios = document.querySelectorAll('input[name="inputMethod"]');
    radios.forEach(r => {
      r.addEventListener('change', (e) => {
        const method = e.target.value;
        document.getElementById('fileInputArea').classList.toggle('active', method === 'file');
        document.getElementById('textInputArea').classList.toggle('active', method === 'text');
      });
    });

    // 파일 선택 시 이름 표시
    const filePicker = document.getElementById('filePicker');
    const dropZone = document.getElementById('dropZone');
    
    dropZone.addEventListener('click', () => filePicker.click());
    filePicker.addEventListener('change', (e) => {
      if(e.target.files.length > 0) {
        document.getElementById('fileNameDisplay').innerText = e.target.files[0].name;
      }
    });
  }

  function updateSubmitButton() {
    const btn = document.getElementById('btnRunFeedback');
    const projectIdInput = document.getElementById('selectedProjectIdInput');
    if (projectIdInput) projectIdInput.value = selectedProjectId || '';
    btn.disabled = !selectedProjectId;
  }

  /* ── 피드백 실행 및 결과 이동 ── */
  function runFeedback(event) {
    if (!selectedProjectId) {
      event.preventDefault();
      return;
    }

    const projectIdInput = document.getElementById('selectedProjectIdInput');
    if (projectIdInput) projectIdInput.value = selectedProjectId;

    const overlay = document.getElementById('loadingOverlay');
    overlay.classList.add('active');
  }

  function init() {
    fetchProjects();
    initMethodTabs();
    
    document.querySelectorAll('input[name="projectFilter"]').forEach(radio => {
      radio.addEventListener('change', (e) => {
        currentFilter = e.target.value;
        selectedProjectId = null;
        renderProjectList();
        updateSubmitButton();
      });
    });

    updateSubmitButton();
    document.getElementById('feedbackForm').addEventListener('submit', runFeedback);
  }

  document.addEventListener('DOMContentLoaded', init);
})();
