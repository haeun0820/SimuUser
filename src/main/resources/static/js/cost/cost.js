(function () {
  'use strict';

  let selectedProjectId = null;
  let selectedProjectTitle = null;
  let fromDetail = false;
  let presetProjectId = null;
  let projectsCache = [];

  function escHtml(value) {
    return String(value ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function csrfHeaders() {
    const headers = { 'Content-Type': 'application/json', Accept: 'application/json' };
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    if (csrfToken && csrfHeader) {
      headers[csrfHeader] = csrfToken;
    }
    return headers;
  }

  function normalizeProject(project) {
    return {
      id: Number(project.id ?? project.projectId),
      title: project.title ?? project.name ?? '프로젝트',
      description: project.description ?? '',
      type: project.type ?? 'personal',
      createdAt: project.createdAt ?? ''
    };
  }

  function timeAgo(isoString) {
    if (!isoString) return '최근';
    const time = new Date(isoString).getTime();
    if (Number.isNaN(time)) return '최근';
    const diffMinutes = Math.floor((Date.now() - time) / 60000);
    if (diffMinutes < 1) return '방금 전';
    if (diffMinutes < 60) return `${diffMinutes}분 전`;
    const diffHours = Math.floor(diffMinutes / 60);
    if (diffHours < 24) return `${diffHours}시간 전`;
    return `${Math.floor(diffHours / 24)}일 전`;
  }

  function parseUrlParams() {
    const params = new URLSearchParams(window.location.search);
    const projectId = params.get('projectId');
    const from = params.get('from');

    if (projectId && from === 'detail') {
      fromDetail = true;
      presetProjectId = String(projectId);

      const typeFilter = document.getElementById('typeFilter');
      if (typeFilter) typeFilter.style.display = 'none';

      const btnNew = document.getElementById('btnNewProject');
      if (btnNew) btnNew.style.display = 'none';
    }
  }

  async function loadProjects() {
    let projects = [];

    try {
      const response = await fetch('/api/projects', { headers: { Accept: 'application/json' } });
      if (response.ok) {
        projects = await response.json();
      } else {
        throw new Error(`HTTP ${response.status}`);
      }
    } catch (error) {
      console.error(error);
      projects = JSON.parse(localStorage.getItem('simu_projects') || '[]');
    }

    projectsCache = Array.isArray(projects)
      ? projects.map(normalizeProject).filter(project => Number.isFinite(project.id))
      : [];

    if (fromDetail && presetProjectId) {
      const target = projectsCache.find(project => String(project.id) === presetProjectId);
      if (target) {
        selectedProjectId = target.id;
        selectedProjectTitle = target.title;
        renderProjects([target]);
        updateBreadcrumbForDetail(target);
        setExecuteEnabled(true);
        return;
      }
    }

    renderProjects(projectsCache);

    if (fromDetail && presetProjectId) {
      updateBreadcrumbForDetail(projectsCache.find(project => String(project.id) === presetProjectId));
    }
  }

  function updateBreadcrumbForDetail(project) {
    const nav = document.getElementById('breadcrumbNav');
    if (!nav) return;

    const title = project ? escHtml(project.title) : '프로젝트';
    const id = project ? project.id : presetProjectId;
    nav.innerHTML = `
      <a href="/project/all">프로젝트</a>
      <span class="bc-sep">/</span>
      <a href="/project/detail/${id}">${title}</a>
      <span class="bc-sep">/</span>
      <span class="bc-current">비용 &amp; 수익성 분석</span>
    `;
  }

  function renderProjects(projects) {
    const listEl = document.getElementById('projectList');
    if (!listEl) return;

    if (!projects || projects.length === 0) {
      listEl.innerHTML = '<div class="empty-state">프로젝트를 불러오는 중...</div>';
      return;
    }

    listEl.innerHTML = projects.map(project => {
      const isSelected = String(project.id) === String(selectedProjectId);
      return `
        <div class="project-item ${isSelected ? 'selected' : ''}" data-id="${project.id}">
          <div class="project-item-header">
            <span class="project-item-title">${escHtml(project.title)}</span>
            <span class="badge-type ${project.type === 'collab' ? 'badge-collab' : 'badge-personal'}">
              ${project.type === 'collab' ? '협업' : '개인'}
            </span>
            <div class="check-icon" style="${isSelected ? 'display:flex' : 'display:none'}">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3">
                <polyline points="20 6 9 17 4 12"></polyline>
              </svg>
            </div>
          </div>
          <p class="project-item-desc">${escHtml(project.description || '')}</p>
          <div class="project-item-meta">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="9"></circle>
              <polyline points="12 7 12 12 15 15"></polyline>
            </svg>
            ${timeAgo(project.createdAt)}
          </div>
        </div>
      `;
    }).join('');

    listEl.querySelectorAll('.project-item').forEach(item => {
      item.addEventListener('click', () => {
        const project = projects.find(entry => String(entry.id) === String(item.dataset.id));
        if (project) selectProject(project);
      });
    });
  }

  function selectProject(project) {
    selectedProjectId = project.id;
    selectedProjectTitle = project.title;

    document.querySelectorAll('.project-item').forEach(element => {
      const isTarget = String(element.dataset.id) === String(project.id);
      element.classList.toggle('selected', isTarget);
      const check = element.querySelector('.check-icon');
      if (check) check.style.display = isTarget ? 'flex' : 'none';
    });

    setExecuteEnabled(true);
  }

  function setExecuteEnabled(enabled) {
    const btn = document.getElementById('btnExecute');
    if (btn) btn.disabled = !enabled;
  }

  function initTypeFilter() {
    document.querySelectorAll('input[name="projectType"]').forEach(radio => {
      radio.addEventListener('change', () => {
        if (fromDetail) return;
        const type = radio.value;
        const filtered = type === 'all'
          ? projectsCache
          : projectsCache.filter(project => project.type === type);
        selectedProjectId = null;
        selectedProjectTitle = null;
        renderProjects(filtered);
        setExecuteEnabled(false);
      });
    });
  }

  function initNotification() {
    const btn = document.getElementById('notiBtn');
    const drop = document.getElementById('notiDropdown');
    if (btn && drop) {
      btn.onclick = e => {
        e.stopPropagation();
        drop.classList.toggle('active');
      };
    }

    document.addEventListener('click', () => {
      if (drop) drop.classList.remove('active');
    });
  }

  function initProfileMenu() {
    const trigger = document.querySelector('.profile-trigger');
    const dropdown = document.querySelector('.profile-menu .dropdown');
    if (trigger && dropdown) {
      trigger.onclick = e => {
        e.stopPropagation();
        dropdown.style.display = dropdown.style.display === 'block' ? 'none' : 'block';
      };
    }

    document.addEventListener('click', () => {
      if (dropdown) dropdown.style.display = 'none';
    });
  }

  function readForm() {
    const revenueModels = Array.from(document.querySelectorAll('input[name="revenueModel"]:checked'))
      .map(input => input.value)
      .filter(Boolean);
    const expectedUsers = Number(document.getElementById('expectedUsers')?.value || 0);
    const pricePerUser = Number(document.getElementById('pricePerUser')?.value || 0);
    return { revenueModels, expectedUsers, pricePerUser };
  }

  function validateForm() {
    if (!selectedProjectId) {
      alert('프로젝트를 선택해주세요.');
      return false;
    }

    const { revenueModels, expectedUsers, pricePerUser } = readForm();
    if (revenueModels.length === 0) {
      alert('수익 모델을 하나 이상 선택해주세요.');
      return false;
    }
    if (!Number.isFinite(expectedUsers) || expectedUsers <= 0) {
      alert('예상 사용자 수를 입력해주세요.');
      return false;
    }
    if (!Number.isFinite(pricePerUser) || pricePerUser < 0) {
      alert('사용자당 가격을 확인해주세요.');
      return false;
    }

    return true;
  }

  async function postJson(url, payload) {
    const response = await fetch(url, {
      method: 'POST',
      headers: csrfHeaders(),
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      const text = await response.text();
      try {
        const parsed = JSON.parse(text);
        throw new Error(parsed.error || text || `Server error ${response.status}`);
      } catch (error) {
        throw error instanceof Error ? error : new Error(text || `Server error ${response.status}`);
      }
    }

    return response.json();
  }

  async function runAnalysis() {
    if (!validateForm()) return;

    const project = projectsCache.find(entry => String(entry.id) === String(selectedProjectId));
    if (!project) {
      alert('선택한 프로젝트를 찾을 수 없습니다.');
      return;
    }

    const { revenueModels, expectedUsers, pricePerUser } = readForm();
    const btn = document.getElementById('btnExecute');
    if (btn) btn.disabled = true;

    try {
      const result = await postJson('/cost/analyze', {
        projectId: project.id,
        revenueModels,
        expectedUsers,
        pricePerUser
      });

      sessionStorage.setItem('cost_form_data', JSON.stringify({
        projectId: project.id,
        projectTitle: project.title,
        from: fromDetail ? 'detail' : 'menu',
        revenueModels,
        expectedUsers,
        pricePerUser
      }));
      sessionStorage.setItem('cost_analysis_result', JSON.stringify(result));
      sessionStorage.removeItem('cost_result_id');

      const params = new URLSearchParams();
      params.set('projectId', String(project.id));
      if (fromDetail) {
        params.set('from', 'detail');
      }
      window.location.href = `/cost/result?${params.toString()}`;
    } catch (error) {
      console.error(error);
      alert(`비용 분석에 실패했습니다. ${error.message}`);
      if (btn) btn.disabled = false;
    }
  }

  function initExecuteBtn() {
    document.getElementById('btnExecute')?.addEventListener('click', runAnalysis);
  }

  function init() {
    parseUrlParams();
    initNotification();
    initProfileMenu();
    initTypeFilter();
    initExecuteBtn();
    loadProjects();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
