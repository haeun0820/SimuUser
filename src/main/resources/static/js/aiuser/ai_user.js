/* AI virtual user simulation setup page */
(function () {
  'use strict';

  const industryMap = {
    commerce: '커머스/리테일', fintech: '금융/핀테크', health: '의료/헬스케어',
    edu: '교육/에듀테크', content: '콘텐츠/엔터테인먼트', social: '소셜/커뮤니티',
    mobility: '모빌리티/여행', productivity: '생산성/비즈니스', '': ''
  };

  let allProjects = [];
  let currentFilter = 'all';
  let selectedProjectId = null;

  /* ── [수정] URL 파라미터 파싱 ── */
  const urlParams = new URLSearchParams(window.location.search);
  const presetProjectId = urlParams.get('projectId');
  const fromDetail = urlParams.get('from') === 'detail';

  function escHtml(value) {
    return String(value ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }

  function normalizeProject(project) {
    const id = project.id ?? project.projectId;
    return {
      id: Number(id),
      title: project.title ?? project.name ?? '',
      description: project.description ?? '',
      targetUser: project.targetUser ?? '',
      industry: project.industry ?? '',
      type: project.type ?? 'personal',
      members: Array.isArray(project.members) ? project.members : [],
      createdAt: project.createdAt ?? project.updatedAt ?? ''
    };
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

  /* ── [수정] 데이터 세팅 시 필터링 로직 ── */
  function setProjects(projects) {
    let mapped = Array.isArray(projects) ? projects.map(normalizeProject) : [];
    
    if (fromDetail && presetProjectId) {
      const target = mapped.find(p => String(p.id) === String(presetProjectId));
      allProjects = target ? [target] : [];
      if (target) selectedProjectId = target.id;
      
      // 필터 탭 숨기기
      const filterArea = document.querySelector('.project-filter-tabs');
      if (filterArea) filterArea.style.display = 'none';
    } else {
      allProjects = mapped.filter(project => Number.isFinite(project.id));
    }
    renderProjects();
  }

  async function loadProjects() {
    if (Array.isArray(window.simuInitialProjects) && window.simuInitialProjects.length > 0) {
      setProjects(window.simuInitialProjects);
      if (fromDetail && presetProjectId && allProjects.length > 0) return;
    }

    try {
      const response = await fetch('/api/projects', { headers: { Accept: 'application/json' } });
      if (response.ok) {
        setProjects(await response.json());
      }
    } catch (error) {
      console.error(error);
    }
  }

  function renderProjects() {
    const container = document.getElementById('projectListScroll');
    if (!container) return;

    const filtered = (fromDetail && presetProjectId) ? allProjects : allProjects.filter(project => {
      if (currentFilter === 'all') return true;
      return project.type === currentFilter;
    });

    if (filtered.length === 0) {
      container.innerHTML = '<div class="no-projects">프로젝트가 없습니다.</div>';
      return;
    }

    container.innerHTML = filtered.map(project => {
      const isSelected = selectedProjectId === project.id;
      return `
        <div class="project-item${isSelected ? ' selected' : ''}" data-id="${project.id}" role="button" tabindex="0">
          <div class="project-item-head">
            <span class="project-item-title">${escHtml(project.title)}</span>
            <span class="type-badge badge-${project.type}">${project.type === 'collab' ? '협업' : '개인'}</span>
            <div class="check-icon" style="${isSelected ? 'display:flex' : 'display:none'}">
               <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="3">
                  <polyline points="20 6 9 17 4 12"></polyline>
               </svg>
            </div>
          </div>
          ${project.description ? `<p class="project-item-desc">${escHtml(project.description)}</p>` : ''}
          <div class="project-item-footer">
            <span>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" style="vertical-align:middle;margin-right:3px;">
                <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.8"></circle>
                <path d="M12 7v5l3 3" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"></path>
              </svg>${timeAgo(project.createdAt)}
            </span>
          </div>
        </div>`;
    }).join('');

    container.querySelectorAll('.project-item').forEach(element => {
      element.addEventListener('click', () => selectProject(Number(element.dataset.id)));
    });
  }

  function selectProject(id) {
    selectedProjectId = id;
    document.querySelectorAll('.project-item').forEach(el => {
      const isTarget = Number(el.dataset.id) === selectedProjectId;
      el.classList.toggle('selected', isTarget);
      const check = el.querySelector('.check-icon');
      if (check) check.style.display = isTarget ? 'flex' : 'none';
    });
    document.getElementById('err-project')?.classList.remove('show');
  }

  function initFilterTabs() {
    document.querySelectorAll('input[name="projectFilter"]').forEach(radio => {
      radio.addEventListener('change', () => {
        currentFilter = radio.value;
        renderProjects();
      });
    });
  }

  function initAgeCheckboxes() {
    const allBox = document.getElementById('age-all');
    const otherBoxes = document.querySelectorAll('.age-check:not(#age-all)');
    if (!allBox) return;
    allBox.addEventListener('change', () => {
      otherBoxes.forEach(checkbox => {
        checkbox.checked = false;
        checkbox.disabled = allBox.checked;
      });
    });
    otherBoxes.forEach(checkbox => {
      checkbox.addEventListener('change', () => {
        if (checkbox.checked) allBox.checked = false;
      });
    });
  }

  function initPersonaInput() {
      const input = document.getElementById('personaCount');
      const notice = document.getElementById('personaNotice');
      const noticeCount = document.getElementById('noticeCount');
      if (!input || !notice) return;

      input.addEventListener('input', () => {
        const val = Number(input.value);
        if (input.value !== '' && Number.isFinite(val) && val > 3) {
          noticeCount.textContent = val.toLocaleString();
          notice.style.display = 'flex';
        } else {
          notice.style.display = 'none';
        }
      });
    }

  function validateForm() {
    let valid = true;
    const checks = [
      { invalid: selectedProjectId === null, errorId: 'err-project' },
            {
        invalid: (() => {
          const v = Number(document.getElementById('personaCount')?.value);
          return !document.getElementById('personaCount')?.value ||
                !Number.isFinite(v) || v < 1 || v > 100000 || !Number.isInteger(v);
        })(),
        errorId: 'err-persona'
      },
      { invalid: !document.getElementById('genderSelect')?.value, errorId: 'err-gender' },
      { invalid: !document.querySelector('.age-check:checked'), errorId: 'err-age' }
    ];
    checks.forEach(({ invalid, errorId }) => {
      const err = document.getElementById(errorId);
      if (invalid) { err?.classList.add('show'); valid = false; }
      else { err?.classList.remove('show'); }
    });
    return valid;
  }

  function handleSubmit(event) {
    event.preventDefault();
    if (!validateForm()) return;

    const project = allProjects.find(item => item.id === selectedProjectId);
    const selectedAges = [...document.querySelectorAll('.age-check:checked')].map(cb => cb.value);
    const params = {
      project,
      personaCount: Number(document.getElementById('personaCount').value),
      gender: document.getElementById('genderSelect').value,
      ages: selectedAges,
      job: (document.getElementById('jobInput')?.value || '').trim()
    };

    localStorage.setItem('simu_params', JSON.stringify(params));
    localStorage.setItem('simu_should_run', '1');
    window.location.href = document.getElementById('resultPageUrl')?.value || '/aiuser/result';
  }

  function init() {
    initFilterTabs();
    initAgeCheckboxes();
    initPersonaInput();
    document.getElementById('simulationForm')?.addEventListener('submit', handleSubmit);
    loadProjects();
  }

  init();
})();