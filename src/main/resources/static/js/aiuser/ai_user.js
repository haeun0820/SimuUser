/* ============================================================
   ai_user.js  –  AI 가상 유저 시뮬레이션 설정 페이지
   ============================================================ */

(function () {
  'use strict';

  /* ── 산업 분야 라벨 맵 ── */
  const industryMap = {
    commerce: '커머스/쇼핑', fintech: '금융/핀테크', health: '의료/헬스케어',
    edu: '교육/에듀테크', content: '콘텐츠/엔터테인먼트', social: '소셜/커뮤니티',
    mobility: '모빌리티/여행', productivity: '생산성/비즈니스', '': ''
  };

  /* ── 시간 포맷 ── */
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
      if (currentFilter === 'collab') return p.type === 'collab';
      if (currentFilter === 'personal') return p.type === 'personal';
      return true;
    });

    if (filtered.length === 0) {
      container.innerHTML = `<div class="no-projects">조건에 맞는 프로젝트가 없습니다.</div>`;
      selectedProjectId = null;
      return;
    }

    container.innerHTML = filtered.map(p => {
      const isCollab = p.type === 'collab';
      const badge = isCollab
        ? '<span class="type-badge badge-collab">협업</span>'
        : '<span class="type-badge badge-personal">개인</span>';
      const memberHtml = isCollab && p.members && p.members.length
        ? `<span class="members">With. ${p.members.map(escHtml).join(', ')}</span>`
        : '';

      return `
        <div class="project-item${selectedProjectId === p.id ? ' selected' : ''}"
             data-id="${p.id}" role="button" tabindex="0">
          <div class="project-item-head">
            <span class="project-item-title">${escHtml(p.title)}</span>
            ${badge}
          </div>
          ${p.description ? `<p class="project-item-desc">${escHtml(p.description)}</p>` : ''}
          <div class="project-item-footer">
            <span>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" style="vertical-align:middle;margin-right:3px;">
                <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.8"/>
                <path d="M12 7v5l3 3" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
              </svg>${timeAgo(p.createdAt)}
            </span>
            ${memberHtml}
          </div>
        </div>`;
    }).join('');

    // 클릭 이벤트 바인딩
    container.querySelectorAll('.project-item').forEach(el => {
      el.addEventListener('click', () => selectProject(Number(el.dataset.id)));
      el.addEventListener('keydown', e => {
        if (e.key === 'Enter' || e.key === ' ') selectProject(Number(el.dataset.id));
      });
    });

    // 이전 선택 복원
    if (selectedProjectId !== null) {
      const found = filtered.find(p => p.id === selectedProjectId);
      if (!found) selectedProjectId = null;
    }
    updateSelection();
  }

  function selectProject(id) {
    selectedProjectId = id;
    updateSelection();
    // 에러 숨기기
    const err = document.getElementById('err-project');
    if (err) err.classList.remove('show');
  }

  function updateSelection() {
    document.querySelectorAll('.project-item').forEach(el => {
      el.classList.toggle('selected', Number(el.dataset.id) === selectedProjectId);
    });
  }

  /* ── 필터 탭 ── */
  function initFilterTabs() {
    document.querySelectorAll('input[name="projectFilter"]').forEach(radio => {
      radio.addEventListener('change', () => {
        currentFilter = radio.value;
        renderProjects();
      });
    });
  }

  /* ── 나이대 체크박스: "전체" 선택 시 나머지 비활성화 ── */
  function initAgeCheckboxes() {
    const allBox = document.getElementById('age-all');
    const otherBoxes = document.querySelectorAll('.age-check:not(#age-all)');

    if (!allBox) return;

    allBox.addEventListener('change', () => {
      if (allBox.checked) {
        otherBoxes.forEach(cb => { cb.checked = false; cb.disabled = true; });
      } else {
        otherBoxes.forEach(cb => { cb.disabled = false; });
      }
    });

    otherBoxes.forEach(cb => {
      cb.addEventListener('change', () => {
        if (cb.checked) { allBox.checked = false; }
        const anyChecked = [...otherBoxes].some(b => b.checked);
        // 전체 disable 해제는 개별이 선택될 때
      });
    });
  }

  /* ── 폼 유효성 검사 ── */
  function validateForm() {
    let valid = true;

    // 프로젝트 선택
    const errProject = document.getElementById('err-project');
    if (selectedProjectId === null) {
      if (errProject) errProject.classList.add('show');
      valid = false;
    } else {
      if (errProject) errProject.classList.remove('show');
    }

    // 페르소나 수
    const personaSelect = document.getElementById('personaCount');
    const errPersona = document.getElementById('err-persona');
    if (!personaSelect || !personaSelect.value) {
      if (errPersona) errPersona.classList.add('show');
      valid = false;
    } else {
      if (errPersona) errPersona.classList.remove('show');
    }

    // 성별
    const genderSelect = document.getElementById('genderSelect');
    const errGender = document.getElementById('err-gender');
    if (!genderSelect || !genderSelect.value) {
      if (errGender) errGender.classList.add('show');
      valid = false;
    } else {
      if (errGender) errGender.classList.remove('show');
    }

    // 나이대
    const anyAge = document.querySelector('.age-check:checked');
    const errAge = document.getElementById('err-age');
    if (!anyAge) {
      if (errAge) errAge.classList.add('show');
      valid = false;
    } else {
      if (errAge) errAge.classList.remove('show');
    }

    return valid;
  }

  /* ── 폼 제출 ── */
  function handleSubmit(e) {
    e.preventDefault();
    if (!validateForm()) return;

    // 선택된 프로젝트 정보
    const project = allProjects.find(p => p.id === selectedProjectId);

    // 나이대 수집
    const selectedAges = [...document.querySelectorAll('.age-check:checked')]
      .map(cb => cb.value);

    const params = {
      project: project,
      personaCount: Number(document.getElementById('personaCount').value),
      gender: document.getElementById('genderSelect').value,
      ages: selectedAges,
      job: (document.getElementById('jobInput')?.value || '').trim(),
    };

    // localStorage에 시뮬레이션 파라미터 저장
    localStorage.setItem('simu_params', JSON.stringify(params));

    // 결과 페이지로 이동
    // Thymeleaf 환경: /aiuser/result  /  순수 HTML: ai_user_result.html
    const resultUrl = document.getElementById('resultPageUrl')?.value || 'ai_user_result.html';
    window.location.href = resultUrl;
  }

  /* ── 새로 만들기 버튼 ── */
  function initNewProjectBtn() {
    const btn = document.getElementById('btnNewProject');
    if (btn) {
      btn.addEventListener('click', () => {
        const url = btn.dataset.href || '../project/new-project.html';
        window.location.href = url;
      });
    }
  }

  /* ── 브레드크럼 동적 설정 ── */
  function initBreadcrumb() {
    const fromProject = new URLSearchParams(window.location.search).get('from') === 'project';
    const projectTitle = new URLSearchParams(window.location.search).get('projectTitle');
    const nav = document.getElementById('breadcrumbNav');
    if (!nav) return;

    if (fromProject && projectTitle) {
      nav.innerHTML = `
        <a href="#">프로젝트</a>
        <span class="bc-sep">›</span>
        <span>${escHtml(decodeURIComponent(projectTitle))}</span>
        <span class="bc-sep">›</span>
        <span class="bc-current">AI 가상 유저 시뮬레이션</span>`;
    } else {
      nav.innerHTML = `
        <a href="#">분석도구</a>
        <span class="bc-sep">›</span>
        <span class="bc-current">AI 가상 유저 시뮬레이션</span>`;
    }
  }

  /* ── 초기화 ── */
  function init() {
    loadProjects();
    initBreadcrumb();
    initFilterTabs();
    renderProjects();
    initAgeCheckboxes();
    initNewProjectBtn();

    const form = document.getElementById('simulationForm');
    if (form) form.addEventListener('submit', handleSubmit);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();