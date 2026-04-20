/* AI virtual user simulation result page */
(function () {
  'use strict';

  const ageLabels = {
    all: '전체',
    teens: '10대',
    twenties: '20대',
    thirties: '30대',
    forties: '40대',
    fifties: '50대 이상'
  };

  const genderLabels = {
    all: '전체',
    male: '남성',
    female: '여성'
  };

  const avatarColors = [
    'linear-gradient(135deg,#2563eb,#6366f1)',
    'linear-gradient(135deg,#0891b2,#2563eb)',
    'linear-gradient(135deg,#7c3aed,#db2777)',
    'linear-gradient(135deg,#059669,#0891b2)'
  ];

  let currentParams = null;
  let currentResult = null;
  let savedResultId = null;
  const urlParams = new URLSearchParams(window.location.search);
  const resultId = urlParams.get('resultId');

  function escHtml(value) {
    return String(value ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function loadParams() {
    try {
      return JSON.parse(localStorage.getItem('simu_params') || 'null');
    } catch (error) {
      return null;
    }
  }

  function loadCachedResult(params) {
    try {
      const cached = JSON.parse(localStorage.getItem('simu_result') || 'null');
      if (!cached || !cached.result) return null;
      return JSON.stringify(cached.params) === JSON.stringify(params) ? cached.result : null;
    } catch (error) {
      return null;
    }
  }

  function cacheResult(params, result) {
    localStorage.setItem('simu_result', JSON.stringify({ params, result }));
  }

  function markSaved(id) {
    savedResultId = id || savedResultId;
    const button = document.getElementById('btnSave');
    if (!button) return;

    button.disabled = true;
    button.textContent = '저장됨';
  }

  function initBreadcrumb(params) {
    const nav = document.getElementById('breadcrumbNav');
    if (!nav) return;

    const title = params?.project?.title;
    if (title) {
      nav.innerHTML = `
        <a href="/project/all">프로젝트</a>
        <span class="bc-sep">/</span>
        <span>${escHtml(title)}</span>
        <span class="bc-sep">/</span>
        <span class="bc-current">AI 가상 유저 시뮬레이션</span>`;
      return;
    }

    nav.innerHTML = `
      <a href="#">분석도구</a>
      <span class="bc-sep">/</span>
      <span class="bc-current">AI 가상 유저 시뮬레이션</span>`;
  }

  async function callSimulateAPI(params) {
    const { project, personaCount, gender, ages, job } = params;
    const ageText = Array.isArray(ages) && ages.includes('all')
      ? '전체'
      : (ages || []).map(age => ageLabels[age] || age).join(', ');
    const genderText = genderLabels[gender] || gender || '전체';

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    const headers = { 'Content-Type': 'application/json', Accept: 'application/json' };
    if (csrfToken && csrfHeader) {
      headers[csrfHeader] = csrfToken;
    }

    const response = await fetch('/aiuser/simulate', {
      method: 'POST',
      headers,
      body: JSON.stringify({
        projectId: project?.id,
        serviceIdea: project?.title || '',
        description: project?.description || '',
        targetUser: project?.targetUser || '',
        industry: project?.industry || '',
        personaCount,
        gender: genderText,
        ages: ageText,
        job: job || ''
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

  async function saveSimulationResult(params, result) {
    if (!params?.project?.id) {
      throw new Error('프로젝트 정보가 없습니다.');
    }
    if (!result) {
      throw new Error('저장할 분석 결과가 없습니다.');
    }

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');
    const headers = { 'Content-Type': 'application/json', Accept: 'application/json' };
    if (csrfToken && csrfHeader) {
      headers[csrfHeader] = csrfToken;
    }

    const response = await fetch('/aiuser/results', {
      method: 'POST',
      headers,
      body: JSON.stringify({
        projectId: params.project.id,
        personaCount: params.personaCount,
        gender: genderLabels[params.gender] || params.gender || '',
        ages: params.ages || [],
        job: params.job || '',
        result
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

  async function loadSavedSimulationResult(id) {
    const response = await fetch(`/aiuser/results/${id}`, {
      headers: { Accept: 'application/json' }
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

  function renderStats(result, personaCount) {
    const element = document.getElementById('statsGrid');
    if (!element) return;

    element.innerHTML = `
      <div class="stat-card">
        <div class="stat-value">${Number(result.avgPurchaseIntent || 0)}%</div>
        <div class="stat-label">평균 구매 의사</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">${personaCount}명</div>
        <div class="stat-label">생성된 페르소나</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">${(result.keyInsights || []).length}개</div>
        <div class="stat-label">핵심 인사이트</div>
      </div>`;
  }

  function renderOverallReaction(result) {
    const element = document.getElementById('overallReaction');
    if (element) {
      element.textContent = result.overallReaction || '';
    }
  }

  function renderPersonas(result, params) {
    const element = document.getElementById('personaGrid');
    const personas = Array.isArray(result.personas) ? result.personas : [];
    const displayPersonas = personas.slice(0, 3);
    if (!element) return;

    const selectedGender = params ? genderLabels[params.gender] : '전체';

    element.className = `persona-grid cols-${Math.max(2, Math.min(personas.length, 4))}`;
    element.innerHTML = displayPersonas.map((persona, index) => {
  const initial = persona.name?.charAt(0) || '?';
  const positive = (persona.positiveReactions || []).map(item => `<li>${escHtml(item)}</li>`).join('');
  const negative = (persona.negativeReactions || []).map(item => `<li>${escHtml(item)}</li>`).join('');
  const churn = (persona.churnPoints || []).map(item => `<li>${escHtml(item)}</li>`).join('');
  
  // 1. 성별 결정 로직 (displayGender 결정)
  let displayGender = persona.gender;
  if (!displayGender) {
    if (selectedGender === '남성' || selectedGender === '여성') {
      displayGender = selectedGender;
    } else {
      // '전체'일 경우 인덱스에 따라 임의 배정
      displayGender = (index % 2 === 0) ? '남성' : '여성';
    }
  }

  // 2. 최종 출력용 텍스트 생성 (이 변수를 사용해야 합니다)
  const genderText = ` · ${escHtml(displayGender)}`;

  return `
  <div class="persona-card" style="animation-delay:${index * 0.1}s">
    <div class="persona-head">
      <div class="persona-avatar" style="background:${avatarColors[index % avatarColors.length]}">${escHtml(initial)}</div>
      <div class="persona-info">
        <div class="persona-name">${escHtml(persona.name)}</div>
        <div class="persona-meta">
          ${escHtml(persona.age)}세 · ${escHtml(persona.job)}${genderText}
        </div>
      </div>
      <span class="persona-score">${Number(persona.purchaseScore || 0)}%</span>
    </div>
          <div><span class="consumer-badge">${escHtml(persona.consumerType)}</span></div>
          <div class="reaction-section">
            <div class="reaction-label label-positive">긍정 반응</div>
            <ul class="reaction-list">${positive}</ul>
          </div>
          <div class="reaction-section">
            <div class="reaction-label label-negative">부정 반응</div>
            <ul class="reaction-list">${negative}</ul>
          </div>
          <div class="reaction-section">
            <div class="reaction-label label-churn">이탈 포인트</div>
            <ul class="reaction-list">${churn}</ul>
          </div>
        </div>`;
    }).join('');
  }

  function renderInsights(result) {
    const keyInsights = document.getElementById('keyInsights');
    const improvements = document.getElementById('improvements');

    if (keyInsights) {
      keyInsights.innerHTML = (result.keyInsights || [])
        .map((item, index) => `<li><span class="insight-num">${index + 1}</span><span>${escHtml(item)}</span></li>`)
        .join('');
    }

    if (improvements) {
      improvements.innerHTML = (result.improvements || [])
        .map((item, index) => `<li><span class="insight-num">${index + 1}</span><span>${escHtml(item)}</span></li>`)
        .join('');
    }
  }

  function showLoading() {
    const resultContent = document.getElementById('resultContent');
    const errorState = document.getElementById('errorState');
    const loadingState = document.getElementById('loadingState');

    if (resultContent) resultContent.style.display = 'none';
    if (errorState) errorState.style.display = 'none';
    if (loadingState) loadingState.style.display = 'flex';
  }

  function hideLoading() {
    const loadingState = document.getElementById('loadingState');
    const resultContent = document.getElementById('resultContent');

    if (loadingState) loadingState.style.display = 'none';
    if (resultContent) resultContent.style.display = 'block';
  }

  function showError(message) {
    const loadingState = document.getElementById('loadingState');
    const resultContent = document.getElementById('resultContent');
    const errorState = document.getElementById('errorState');

    if (loadingState) loadingState.style.display = 'none';
    if (resultContent) resultContent.style.display = 'none';
    if (errorState) {
      errorState.style.display = 'block';
      const messageElement = errorState.querySelector('.error-msg');
      if (messageElement) {
        messageElement.textContent = message || '시뮬레이션 중 오류가 발생했습니다.';
      }
    }
  }

  function renderResult(result, params) {
    currentParams = params;
    currentResult = result;
    renderStats(result, params.personaCount);
    renderOverallReaction(result);
    renderPersonas(result, params);
    renderInsights(result);
    hideLoading();
  }

  async function runSimulation() {
    const params = loadParams();
    if (!params) {
      showError('시뮬레이션 설정값이 없습니다. 설정 페이지에서 다시 실행해 주세요.');
      return;
    }

    initBreadcrumb(params);
    showLoading();

    try {
      const result = await callSimulateAPI(params);
      cacheResult(params, result);
      renderResult(result, params);
    } catch (error) {
      console.error(error);
      showError(`AI 분석 중 오류가 발생했습니다. ${error.message}`);
    }
  }

  function initButtons() {
    document.getElementById('btnBack')?.addEventListener('click', event => {
      window.location.href = event.currentTarget.dataset.href || '/aiuser/ai_user';
    });
    document.getElementById('btnBackErr')?.addEventListener('click', event => {
      window.location.href = event.currentTarget.dataset.href || '/aiuser/ai_user';
    });
    document.getElementById('btnRetry')?.addEventListener('click', runSimulation);
    document.getElementById('btnRetryErr')?.addEventListener('click', runSimulation);

    document.getElementById('btnSave')?.addEventListener('click', async event => {
      if (savedResultId) return;

      const button = event.currentTarget;
      button.disabled = true;

      try {
        const params = currentParams || loadParams();
        const result = currentResult || loadCachedResult(params);
        const saved = await saveSimulationResult(params, result);
        markSaved(saved.id);
        alert('결과가 저장되었습니다!');
        if (params?.project?.id) {
          setTimeout(() => {
            window.location.href = `/project/detail/${params.project.id}`;
          }, 800);
        }
      } catch (error) {
        console.error(error);
        button.disabled = false;
        alert(`결과 저장에 실패했습니다. ${error.message}`);
      }
    });
  }

  function init() {
    initButtons();
    if (resultId) {
      showLoading();
      loadSavedSimulationResult(resultId)
        .then(saved => {
          savedResultId = saved.id;
          const params = {
            project: { id: saved.projectId },
            personaCount: saved.personaCount,
            gender: saved.gender,
            ages: saved.ages,
            job: saved.job
          };
          initBreadcrumb(params);
          renderResult(saved.result, params);
          markSaved(saved.id);
        })
        .catch(error => {
          console.error(error);
          showError(`저장된 결과를 불러오지 못했습니다. ${error.message}`);
        });
      return;
    }

    const params = loadParams();
    if (!params) {
      showError('Simulation settings are missing. Go back to settings and start again.');
      return;
    }

    initBreadcrumb(params);

    const cachedResult = loadCachedResult(params);
    if (cachedResult) {
      renderResult(cachedResult, params);
      return;
    }

    if (localStorage.getItem('simu_should_run') === '1') {
      localStorage.removeItem('simu_should_run');
      runSimulation();
      return;
    }

    showError('분석 결과가 없습니다. 설정 페이지에서 시뮬레이션을 다시 시작해 주세요.');
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
