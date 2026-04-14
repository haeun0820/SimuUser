/* AI virtual user simulation setup page */
(function () {
  'use strict';

  const industryMap = {
    commerce: '커머스/리테일',
    fintech: '금융/핀테크',
    health: '의료/헬스케어',
    edu: '교육/에듀테크',
    content: '콘텐츠/엔터테인먼트',
    social: '소셜/커뮤니티',
    mobility: '모빌리티/여행',
    productivity: '생산성/비즈니스',
    '': ''
  };

  let allProjects = [];
  let currentFilter = 'all';
  let selectedProjectId = null;

  function escHtml(value) {
    return String(value ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
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

  function setProjects(projects) {
    allProjects = Array.isArray(projects)
      ? projects.map(normalizeProject).filter(project => Number.isFinite(project.id))
      : [];
    renderProjects();
  }

  async function loadProjects() {
    if (Array.isArray(window.simuInitialProjects) && window.simuInitialProjects.length > 0) {
      setProjects(window.simuInitialProjects);
    } else {
      const container = document.getElementById('projectListScroll');
      if (container) {
        container.innerHTML = '<div class="no-projects">프로젝트를 불러오는 중입니다.</div>';
      }
    }

    try {
      const response = await fetch('/api/projects', {
        headers: { Accept: 'application/json' }
      });

      if (!response.ok) {
        throw new Error(`프로젝트 조회 실패: ${response.status}`);
      }

      setProjects(await response.json());
    } catch (error) {
      console.error(error);
      if (allProjects.length === 0) {
        const container = document.getElementById('projectListScroll');
        if (container) {
          container.innerHTML = '<div class="no-projects">프로젝트를 불러오지 못했습니다.</div>';
        }
      }
    }
  }

  function renderProjects() {
    const container = document.getElementById('projectListScroll');
    if (!container) return;

    const filtered = allProjects.filter(project => {
      if (currentFilter === 'all') return true;
      return project.type === currentFilter;
    });

    if (filtered.length === 0) {
      container.innerHTML = '<div class="no-projects">조건에 맞는 프로젝트가 없습니다.</div>';
      selectedProjectId = null;
      return;
    }

    if (selectedProjectId !== null && !filtered.some(project => project.id === selectedProjectId)) {
      selectedProjectId = null;
    }

    container.innerHTML = filtered.map(project => {
      const isCollab = project.type === 'collab';
      const badge = isCollab
        ? '<span class="type-badge badge-collab">협업</span>'
        : '<span class="type-badge badge-personal">개인</span>';
      const memberHtml = isCollab && project.members.length
        ? `<span class="members">With. ${project.members.map(escHtml).join(', ')}</span>`
        : '';

      return `
        <div class="project-item${selectedProjectId === project.id ? ' selected' : ''}"
             data-id="${project.id}" role="button" tabindex="0">
          <div class="project-item-head">
            <span class="project-item-title">${escHtml(project.title)}</span>
            ${badge}
          </div>
          ${project.description ? `<p class="project-item-desc">${escHtml(project.description)}</p>` : ''}
          <div class="project-item-footer">
            <span>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" style="vertical-align:middle;margin-right:3px;">
                <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.8"></circle>
                <path d="M12 7v5l3 3" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"></path>
              </svg>${timeAgo(project.createdAt)}
            </span>
            ${memberHtml}
          </div>
        </div>`;
    }).join('');

    container.querySelectorAll('.project-item').forEach(element => {
      const select = () => selectProject(Number(element.dataset.id));
      element.addEventListener('click', select);
      element.addEventListener('keydown', event => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault();
          select();
        }
      });
    });
  }

  function selectProject(id) {
    selectedProjectId = id;
    document.querySelectorAll('.project-item').forEach(element => {
      element.classList.toggle('selected', Number(element.dataset.id) === selectedProjectId);
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
        if (checkbox.checked) {
          allBox.checked = false;
          otherBoxes.forEach(box => { box.disabled = false; });
        }
      });
    });
  }

  function validateForm() {
    let valid = true;
    const checks = [
      { invalid: selectedProjectId === null, errorId: 'err-project' },
      { invalid: !document.getElementById('personaCount')?.value, errorId: 'err-persona' },
      { invalid: !document.getElementById('genderSelect')?.value, errorId: 'err-gender' },
      { invalid: !document.querySelector('.age-check:checked'), errorId: 'err-age' }
    ];

    checks.forEach(({ invalid, errorId }) => {
      const errorElement = document.getElementById(errorId);
      if (invalid) {
        errorElement?.classList.add('show');
        valid = false;
      } else {
        errorElement?.classList.remove('show');
      }
    });

    return valid;
  }

  function handleSubmit(event) {
    event.preventDefault();
    if (!validateForm()) return;

    const project = allProjects.find(item => item.id === selectedProjectId);
    if (!project) {
      document.getElementById('err-project')?.classList.add('show');
      return;
    }

    const selectedAges = [...document.querySelectorAll('.age-check:checked')]
      .map(checkbox => checkbox.value);

    const params = {
      project: {
        id: project.id,
        title: project.title,
        description: project.description,
        targetUser: project.targetUser,
        industry: industryMap[project.industry] || project.industry || '',
        type: project.type
      },
      personaCount: Number(document.getElementById('personaCount').value),
      gender: document.getElementById('genderSelect').value,
      ages: selectedAges,
      job: (document.getElementById('jobInput')?.value || '').trim()
    };

    localStorage.setItem('simu_params', JSON.stringify(params));

    const resultUrl = document.getElementById('resultPageUrl')?.value || '/aiuser/result';
    window.location.href = resultUrl;
  }

  function initNewProjectButton() {
    const button = document.getElementById('btnNewProject');
    button?.addEventListener('click', () => {
      window.location.href = button.dataset.href || '/project/new';
    });
  }

  function init() {
    initFilterTabs();
    initAgeCheckboxes();
    initNewProjectButton();
    document.getElementById('simulationForm')?.addEventListener('submit', handleSubmit);
    loadProjects();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
