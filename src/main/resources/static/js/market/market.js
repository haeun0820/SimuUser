/* ============================================================
   market.js – 시장 & 경쟁 분석 (프로젝트 선택) 페이지
   ============================================================ */

(function () {
  /* ── 상태 ── */
  let allProjects = [];
  let selectedProjectId = null;
  let currentFilter = 'all';
  
  // URL 파라미터 파싱
  const urlParams = new URLSearchParams(window.location.search);
  const fromProjectId = urlParams.get('projectId');  // 상세에서 진입 시
  const fromDetail = urlParams.get('from') === 'detail';

  /* ── 유틸 ── */
  function escHtml(str) {
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function timeAgo(isoStr) {
    const diff = Date.now() - new Date(isoStr).getTime();
    const min = Math.floor(diff / 60000);
    if (min < 1) return '방금 전';
    if (min < 60) return `${min}분 전`;
    const hr = Math.floor(min / 60);
    if (hr < 24) return `${hr}시간 전`;
    return `${Math.floor(hr / 24)}일 전`;
  }

  /* ── 브레드크럼 렌더링 ── */
  function renderBreadcrumb() {
    const nav = document.getElementById('breadcrumbNav');
    if (!nav) return;

    if (fromDetail && fromProjectId) {
      const project = allProjects.find(p => String(p.id) === fromProjectId);
      const projectTitle = project ? escHtml(project.title) : '프로젝트';
      nav.innerHTML = `
        <a href="/project/all">프로젝트</a>
        <span class="bc-sep">›</span>
        <a href="/project/detail/${fromProjectId}">${projectTitle}</a>
        <span class="bc-sep">›</span>
        <span class="bc-current">시장 &amp; 경쟁 분석</span>`;
    } else {
      nav.innerHTML = `
        <span>분석도구</span>
        <span class="bc-sep">›</span>
        <span class="bc-current">시장 &amp; 경쟁 분석</span>`;
    }
  }

  /* ── 프로젝트 카드 렌더 ── */
  function renderProjectItem(p) {
    const isCollab = p.type === 'collab';
    const badge = isCollab
      ? '<span class="type-badge badge-collab">협업</span>'
      : '<span class="type-badge badge-personal">개인</span>';

    const isSelected = String(p.id) === String(selectedProjectId);

    return `
      <div class="project-item${isSelected ? ' selected' : ''}" data-id="${p.id}" role="button" tabindex="0">
        <div class="project-item-head">
          <span class="project-item-title">${escHtml(p.title)}</span>
          ${badge}
          <div class="check-icon" style="${isSelected ? 'display:flex' : 'display:none'}">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3">
              <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
          </div>
        </div>
        <p class="project-item-desc">${escHtml(p.description || '설명이 없습니다.')}</p>
        <div class="project-item-footer">
          <span class="project-time">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.8"/>
              <path d="M12 7v5l3 3" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            </svg>
            ${timeAgo(p.createdAt)}
          </span>
        </div>
      </div>`;
  }

  /* ── 리스트 렌더링 ── */
  function renderProjectList() {
  const listEl = document.getElementById('projectList');
  if (!listEl) return;

  let filtered = allProjects;

  // 상세 페이지에서 진입 시 해당 프로젝트만 표시
  if (fromDetail && fromProjectId) {
    filtered = allProjects.filter(p => String(p.id) === fromProjectId);
    
    // [추가] 라디오 필터 및 새로 만들기 버튼 숨기기
    const filterRadios = document.getElementById('filterRadios');
    if (filterRadios) filterRadios.style.display = 'none';

    const btnNew = document.getElementById('btnNewProject');
    if (btnNew) btnNew.style.display = 'none';

  } else {
    // 라디오 필터 적용
    if (currentFilter !== 'all') {
      filtered = allProjects.filter(p => p.type === currentFilter);
    }
  }

  if (filtered.length === 0) {
    listEl.innerHTML = `<div class="empty-project">해당하는 프로젝트가 없습니다.</div>`;
    return;
  }

  listEl.innerHTML = filtered.map(renderProjectItem).join('');

  // 클릭 이벤트 (이하 동일)
  listEl.querySelectorAll('.project-item').forEach(item => {
    item.addEventListener('click', () => {
      const id = item.dataset.id;
      selectedProjectId = id;
      renderProjectList();
      updateRunButton();
    });
  });
}

/* ── 새로 만들기 버튼 이벤트 ── */
function initNewProjectButton() {
  const btnNew = document.getElementById('btnNewProject');
  if (btnNew) {
    btnNew.addEventListener('click', () => {
      const targetUrl = btnNew.dataset.href || '/project/new';
      window.location.href = targetUrl;
    });
  }
}

  /* ── 분석 실행 버튼 활성화 ── */
  function updateRunButton() {
    const btn = document.getElementById('btnRunAnalysis');
    if (btn) btn.disabled = !selectedProjectId;
  }

  async function callMarketAnalyzeAPI(project) {
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    const headers = { 'Content-Type': 'application/json', Accept: 'application/json' };
    if (csrfToken && csrfHeader) {
      headers[csrfHeader] = csrfToken;
    }

    const response = await fetch('/market/analyze', {
      method: 'POST',
      headers,
      body: JSON.stringify({
        projectId: project.id,
        title: project.title,
        description: project.description,
        targetUser: project.targetUser,
        industry: project.industry
      })
    });

    if (!response.ok) {
      const errorText = await response.text();
      let errorMessage = errorText || `Server error ${response.status}`;
      try {
        const errorJson = JSON.parse(errorText);
        errorMessage = errorJson.error || errorMessage;
      } catch (error) {
        // Keep the raw response text when the server does not return JSON.
      }
      throw new Error(errorMessage);
    }

    return response.json();
  }

  /* ── 분석 실행 ── */
  async function runAnalysis() {
    if (!selectedProjectId) return;

    const project = allProjects.find(p => String(p.id) === selectedProjectId);
    if (!project) return;

    const btn = document.getElementById('btnRunAnalysis');
    if (btn) btn.disabled = true;

    // 로딩 오버레이 표시
    let overlay = document.getElementById('loadingOverlay');
    if (!overlay) {
      overlay = document.createElement('div');
      overlay.id = 'loadingOverlay';
      overlay.className = 'loading-overlay active';
      overlay.innerHTML = `
        <div class="loading-spinner"></div>
        <div class="loading-text">시장 & 경쟁 분석 중...</div>`;
      document.body.appendChild(overlay);
    } else {
      overlay.classList.add('active');
    }

    // 선택한 프로젝트 정보를 sessionStorage에 저장
    sessionStorage.setItem('market_selected_project', JSON.stringify(project));
    sessionStorage.setItem('market_from_detail', fromDetail ? 'true' : 'false');
    sessionStorage.removeItem('market_analysis_result');

    try {
      const result = await callMarketAnalyzeAPI(project);
      sessionStorage.setItem('market_analysis_result', JSON.stringify(result));

      const params = new URLSearchParams();
      params.set('projectId', selectedProjectId);
      if (fromDetail) params.set('from', 'detail');
      window.location.href = `/market/result?${params.toString()}`;
    } catch (error) {
      console.error(error);
      if (overlay) overlay.classList.remove('active');
      if (btn) btn.disabled = false;
      alert(`AI 분석 중 오류가 발생했습니다. ${error.message}`);
    }
  }

  /* ── 라디오 필터 이벤트 ── */
  function initFilters() {
    document.querySelectorAll('input[name="projectFilter"]').forEach(radio => {
      radio.addEventListener('change', () => {
        currentFilter = radio.value;
        selectedProjectId = null;
        renderProjectList();
        updateRunButton();
      });
    });
  }

  /* ── 분석 버튼 이벤트 ── */
  function initRunButton() {
    const btn = document.getElementById('btnRunAnalysis');
    if (btn) btn.addEventListener('click', runAnalysis);
  }

  /* ── 초기화 ── */
  async function init() {
  try {
    const response = await fetch('/api/projects');
    if (response.ok) {
      allProjects = await response.json();
    } else {
      throw new Error('fetch failed');
    }
  } catch {
    allProjects = JSON.parse(localStorage.getItem('simu_projects') || '[]');
  }

  if (fromDetail && fromProjectId) {
    selectedProjectId = fromProjectId;
  }

  renderBreadcrumb();
  renderProjectList();
  initFilters();
  initRunButton();
  initNewProjectButton();
  updateRunButton();
}

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
