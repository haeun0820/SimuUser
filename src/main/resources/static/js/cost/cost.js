/* ============================================================
   cost.js – 비용 & 수익성 분석 입력 페이지
   ============================================================ */

let selectedProjectId = null;
let selectedProjectTitle = null;
let fromDetail = false;
let presetProjectId = null;

document.addEventListener('DOMContentLoaded', () => {
  parseUrlParams();
  initNotification();
  initProfileMenu();
  initTypeFilter();
  loadProjects(); // 상세 진입 시 여기서 하나만 렌더링하도록 제어함
  initExecuteBtn();
});

function parseUrlParams() {
  const params = new URLSearchParams(window.location.search);
  const projectId = params.get('projectId');
  const from = params.get('from');

  if (projectId && from === 'detail') {
    fromDetail = true;
    presetProjectId = String(projectId);
    
    // 필터 숨기기
    const typeFilter = document.getElementById('typeFilter');
    if (typeFilter) typeFilter.style.display = 'none';

    const btnNew = document.getElementById('btnNewProject');
    if (btnNew) btnNew.style.display = 'none';
    
    updateBreadcrumbForDetail(projectId);
  }
}

async function loadProjects() {
  let projects = JSON.parse(localStorage.getItem('simu_projects') || '[]');

  if (!projects.length) {
    try {
      const res = await fetch('/api/projects');
      if (res.ok) {
        projects = await res.json();
        localStorage.setItem('simu_projects', JSON.stringify(projects));
      }
    } catch (e) { console.error(e); }
  }

  // ★ 이 부분이 핵심: 상세에서 왔으면 필터링된 배열만 renderProjects에 전달
  if (fromDetail && presetProjectId) {
    const target = projects.find(p => String(p.id) === presetProjectId);
    if (target) {
      renderProjects([target]); // 리스트에 하나만 담아서 보냄
      selectProject(target.id, target.title);
      return; // 더 이상 아래로 내려가지 않음
    }
  }

  renderProjects(projects);
}

function renderProjects(projects) {
  const listEl = document.getElementById('projectList');
  if (!listEl) return;

  if (!projects || projects.length === 0) {
    listEl.innerHTML = `<div class="empty-state">프로젝트가 없습니다.</div>`;
    return;
  }

  // ★ 주의: 여기서 다시 localStorage를 읽거나 필터를 적용하면 안 됨 (받은 매개변수 projects만 사용)
  listEl.innerHTML = projects.map(p => {
    const isSelected = String(p.id) === String(selectedProjectId);
    return `
      <div class="project-item ${isSelected ? 'selected' : ''}" 
           data-id="${p.id}" 
           onclick="selectProject('${p.id}', '${escHtml(p.title)}')">
        <div class="project-item-header">
          <span class="project-item-title">${escHtml(p.title)}</span>
          <span class="badge-type ${p.type === 'collab' ? 'badge-collab' : 'badge-personal'}">
            ${p.type === 'collab' ? '협업' : '개인'}
          </span>
          <div class="check-icon" style="${isSelected ? 'display:flex' : 'display:none'}">
             <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3">
                <polyline points="20 6 9 17 4 12"></polyline>
             </svg>
          </div>
        </div>
        <p class="project-item-desc">${escHtml(p.description || '')}</p>
        <div class="project-item-meta">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="9"></circle><polyline points="12 7 12 12 15 15"></polyline>
          </svg>
          ${p.createdAt ? timeAgo(p.createdAt) : '최근'}
        </div>
      </div>`;
  }).join('');
}

function selectProject(id, title) {
  selectedProjectId = String(id);
  selectedProjectTitle = title;

  document.querySelectorAll('.project-item').forEach(el => {
    const isTarget = el.dataset.id === String(id);
    el.classList.toggle('selected', isTarget);
    const check = el.querySelector('.check-icon');
    if (check) check.style.display = isTarget ? 'flex' : 'none';
  });

  const btn = document.getElementById('btnExecute');
  if (btn) btn.disabled = false;
}

// -- 이하 유틸리티 함수들 (기존과 동일) --
function initTypeFilter() {
  const radios = document.querySelectorAll('input[name="projectType"]');
  radios.forEach(r => {
    r.addEventListener('change', () => {
      if(!fromDetail) loadProjects(); // 상세 모드가 아닐 때만 필터 작동
    });
  });
}

function initExecuteBtn() {
  const btn = document.getElementById('btnExecute');
  if (!btn) return;
  btn.addEventListener('click', () => {
    if (!selectedProjectId) return alert('프로젝트를 선택해주세요.');
    const revenueModels = Array.from(document.querySelectorAll('input[name="revenueModel"]:checked')).map(cb => cb.value);
    if (revenueModels.length === 0) return alert('수익 모델을 선택해주세요.');
    const expectedUsers = document.getElementById('expectedUsers').value;
    const pricePerUser = document.getElementById('pricePerUser').value;
    if (!expectedUsers || expectedUsers <= 0) return alert('사용자 수를 입력해주세요.');

    sessionStorage.setItem('cost_form_data', JSON.stringify({
      projectId: selectedProjectId,
      projectTitle: selectedProjectTitle,
      from: fromDetail ? 'detail' : 'menu',
      revenueModels,
      expectedUsers: parseInt(expectedUsers),
      pricePerUser: parseInt(pricePerUser || 0),
      timestamp: new Date().toISOString()
    }));
    window.location.href = '/cost/result';
  });
}

function updateBreadcrumbForDetail(projectId) {
  const allProjects = JSON.parse(localStorage.getItem('simu_projects') || '[]');
  const project = allProjects.find(p => String(p.id) === String(projectId));
  const nav = document.getElementById('breadcrumbNav');
  if (nav && project) {
    nav.innerHTML = `<a href="/project/all">프로젝트</a><span class="bc-sep">›</span><a href="/project/detail/${projectId}">${escHtml(project.title)}</a><span class="bc-sep">›</span><span class="bc-current">비용 & 수익성 분석</span>`;
  }
}

function initNotification() {
  const btn = document.getElementById('notiBtn');
  const drop = document.getElementById('notiDropdown');
  if (btn && drop) btn.onclick = (e) => { e.stopPropagation(); drop.classList.toggle('active'); };
  document.onclick = () => { if(drop) drop.classList.remove('active'); };
}

function initProfileMenu() {
  const tri = document.querySelector('.profile-trigger');
  const drop = document.querySelector('.profile-menu .dropdown');
  if (tri && drop) tri.onclick = (e) => { e.stopPropagation(); drop.style.display = (drop.style.display === 'block') ? 'none' : 'block'; };
  document.addEventListener('click', () => { if(drop) drop.style.display = 'none'; });
}

function timeAgo(s) { 
  const d = Date.now() - new Date(s).getTime();
  const m = Math.floor(d / 60000);
  if (m < 1) return '방금 전';
  if (m < 60) return m + '분 전';
  const h = Math.floor(m / 60);
  if (h < 24) return h + '시간 전';
  return Math.floor(h / 24) + '일 전';
}

function escHtml(s) { return String(s || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;'); }