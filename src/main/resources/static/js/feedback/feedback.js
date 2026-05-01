(function () {
  let allProjects = [];
  let selectedProjectId = null;
  let currentFilter = 'all';
  const initialProjectId = new URLSearchParams(window.location.search).get('projectId');
  const draftKey = 'feedbackDraft';

  function escHtml(value) {
    return String(value ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function timeAgo(isoString) {
    if (!isoString) return '';
    const time = new Date(isoString).getTime();
    if (Number.isNaN(time)) return '';
    const diffMinutes = Math.floor((Date.now() - time) / 60000);
    if (diffMinutes < 1) return '방금 전';
    if (diffMinutes < 60) return `${diffMinutes}분 전`;
    const diffHours = Math.floor(diffMinutes / 60);
    if (diffHours < 24) return `${diffHours}시간 전`;
    return `${Math.floor(diffHours / 24)}일 전`;
  }

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
      <div class="project-item ${String(p.id) === String(selectedProjectId) ? 'selected' : ''}" data-id="${p.id}" role="button" tabindex="0">
        <div class="project-item-head">
          <span class="project-item-title">${escHtml(p.title)}</span>
          <span class="type-badge badge-${p.type}">${p.type === 'collab' ? '협업' : '개인'}</span>
          <div class="check-icon" style="${String(p.id) === String(selectedProjectId) ? 'display:flex' : 'display:none'}">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3">
              <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
          </div>
        </div>
        <p class="project-item-desc">${escHtml(p.description || '설명이 없습니다.')}</p>
        <div class="project-item-footer">
          <span>
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" style="vertical-align:middle;margin-right:3px;">
              <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.8"></circle>
              <path d="M12 7v5l3 3" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"></path>
            </svg>${timeAgo(p.createdAt)}
          </span>
        </div>
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

  function restoreDraft() {
    const raw = sessionStorage.getItem(draftKey);
    if (!raw) return;

    try {
      const draft = JSON.parse(raw);
      if (draft.projectId) {
        selectedProjectId = String(draft.projectId);
      }

      if (draft.sourceType === 'text') {
        const textRadio = document.querySelector('input[name="inputMethod"][value="text"]');
        const textArea = document.querySelector('textarea[name="textContent"]');
        if (textRadio && textArea) {
          textRadio.checked = true;
          document.getElementById('fileInputArea').classList.remove('active');
          document.getElementById('textInputArea').classList.add('active');
          textArea.value = draft.textContent || '';
        }
      }
    } catch (error) {
      console.error(error);
    } finally {
      sessionStorage.removeItem(draftKey);
    }
  }

  /* ── 피드백 실행 및 결과 이동 ── */
  function runFeedback(event) {
    if (!selectedProjectId) {
      event.preventDefault();
      return;
    }

    const projectIdInput = document.getElementById('selectedProjectIdInput');
    if (projectIdInput) projectIdInput.value = selectedProjectId;

    const selectedMethod = document.querySelector('input[name="inputMethod"]:checked')?.value || 'file';
    const textContent = document.querySelector('textarea[name="textContent"]')?.value || '';
    sessionStorage.setItem(draftKey, JSON.stringify({
      projectId: selectedProjectId,
      sourceType: selectedMethod,
      textContent
    }));

    const overlay = document.getElementById('loadingOverlay');
    overlay.classList.add('active');
  }

  function init() {
    initMethodTabs();
    restoreDraft();
    fetchProjects();
    
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
