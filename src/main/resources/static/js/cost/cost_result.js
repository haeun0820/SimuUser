(function () {
  'use strict';

  let formData = null;
  let analysisResult = null;
  let savedResultId = null;

  function escHtml(value) {
    return String(value ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function fmtWon(value) {
    const number = Number(value || 0);
    return `${number.toLocaleString()}만원`;
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

  function parseQuery() {
    return new URLSearchParams(window.location.search);
  }

  async function fetchJson(url) {
    const response = await fetch(url, { headers: { Accept: 'application/json' } });
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

  function loadSessionData() {
    try {
      const rawForm = sessionStorage.getItem('cost_form_data');
      const rawResult = sessionStorage.getItem('cost_analysis_result');
      const rawResultId = sessionStorage.getItem('cost_result_id');
      formData = rawForm ? JSON.parse(rawForm) : null;
      analysisResult = rawResult ? JSON.parse(rawResult) : null;
      savedResultId = rawResultId || null;
    } catch (error) {
      formData = null;
      analysisResult = null;
      savedResultId = null;
    }
  }

  function persistSessionData() {
    if (formData) {
      sessionStorage.setItem('cost_form_data', JSON.stringify(formData));
    }
    if (analysisResult) {
      sessionStorage.setItem('cost_analysis_result', JSON.stringify(analysisResult));
    }
    if (savedResultId) {
      sessionStorage.setItem('cost_result_id', String(savedResultId));
    }
  }

  function updateBreadcrumb() {
    const nav = document.getElementById('breadcrumbNav');
    if (!nav || !formData) return;

    const projectId = formData.projectId;
    const projectTitle = escHtml(formData.projectTitle || 'Project');

    if (formData.from === 'detail' && projectId) {
      nav.innerHTML = `
        <a href="/project/all">Project</a>
        <span class="bc-sep">/</span>
        <a href="/project/detail/${projectId}">${projectTitle}</a>
        <span class="bc-sep">/</span>
        <a href="/cost/cost?projectId=${projectId}&from=detail">Cost &amp; Revenue Analysis</a>
        <span class="bc-sep">/</span>
        <span class="bc-current">Result</span>
      `;
      return;
    }

    nav.innerHTML = `
      <a href="/dashboard">Dashboard</a>
      <span class="bc-sep">/</span>
      <a href="/cost/cost">Cost &amp; Revenue Analysis</a>
      <span class="bc-sep">/</span>
      <span class="bc-current">Result</span>
    `;
  }

  function renderResults(result) {
    renderSummaryGrid(result);
    renderDevCosts(result.devCosts || {});
    renderMonthlyCosts(result.monthlyCosts || {});
    renderRevenue(result.revenue || {});
    renderBEP(result);
    renderSuggestions(result.suggestions || []);
  }

  function renderSummaryGrid(result) {
    const grid = document.getElementById('summaryGrid');
    if (!grid) return;

    const grade = result.grade || '보통';
    const gradeClass = grade === 'Excellent' || grade === '우수'
      ? 'grade-excellent'
      : grade === 'Normal' || grade === '보통'
        ? 'grade-normal'
        : 'grade-caution';

    grid.innerHTML = `
      <div class="summary-item">
        <div class="summary-label">투자 적합도</div>
        <div class="grade-badge ${gradeClass}">${escHtml(grade)}</div>
      </div>
      <div class="summary-item">
        <div class="summary-label">분석 점수 / 100</div>
        <div class="summary-value">${Number(result.score || 0)}</div>
        <div class="score-bar-wrap">
          <div class="score-bar-fill" id="scoreBar" style="width:0%"></div>
        </div>
      </div>
      <div class="summary-item">
        <div class="summary-label">손익분기점(BEP)</div>
        <div class="summary-value">${Number(result.bepMonths || 0)}<span style="font-size:18px; font-weight:600;">개월</span></div>
      </div>
    `;

    setTimeout(() => {
      const bar = document.getElementById('scoreBar');
      if (bar) bar.style.width = `${Math.max(0, Math.min(100, Number(result.score || 0)))}%`;
    }, 100);
  }

  function renderDevCosts(costs) {
    const grid = document.getElementById('devCostGrid');
    if (!grid) return;

    const items = [
      { label: '프론트엔드', value: costs.frontend },
      { label: '백엔드', value: costs.backend },
      { label: 'AI/ML', value: costs.aiml },
      { label: '디자인', value: costs.design }
    ];

    grid.innerHTML = items.map(item => `
      <div class="dev-cost-item">
        <div class="dev-cost-label">${item.label}</div>
        <div class="dev-cost-value">${fmtWon(item.value)}</div>
      </div>
    `).join('');

    const totalEl = document.getElementById('totalDevCostVal');
    if (totalEl) totalEl.textContent = fmtWon(costs.total);
  }

  function renderMonthlyCosts(costs) {
    const grid = document.getElementById('monthlyCostGrid');
    if (!grid) return;

    const items = [
      { label: '서버', value: costs.server },
      { label: 'API', value: costs.api },
      { label: '유지보수', value: costs.maintenance },
      { label: '마케팅', value: costs.marketing }
    ];

    grid.innerHTML = items.map(item => `
      <div class="monthly-cost-item">
        <div class="monthly-cost-label">${item.label}</div>
        <div class="monthly-cost-value">${fmtWon(item.value)}</div>
      </div>
    `).join('');

    const totalEl = document.getElementById('totalMonthlyCostVal');
    if (totalEl) totalEl.textContent = fmtWon(costs.total);
  }

  function renderRevenue(revenue) {
    const grid = document.getElementById('revenueGrid');
    if (!grid) return;

    grid.innerHTML = `
      <div class="revenue-item">
        <div class="revenue-period">3개월 후</div>
        <div class="revenue-value">${fmtWon(revenue.m3)}</div>
      </div>
      <div class="revenue-item">
        <div class="revenue-period">6개월 후</div>
        <div class="revenue-value">${fmtWon(revenue.m6)}</div>
      </div>
      <div class="revenue-item">
        <div class="revenue-period">12개월 후</div>
        <div class="revenue-value">${fmtWon(revenue.m12)}</div>
      </div>
    `;
  }

  function renderBEP(result) {
    const grid = document.getElementById('bepGrid');
    const desc = document.getElementById('bepDesc');
    if (!grid) return;

    grid.innerHTML = `
      <div class="bep-item">
        <div class="bep-label">BEP 예상 기간</div>
        <div class="bep-value">${Number(result.bepMonths || 0)}개월</div>
      </div>
      <div class="bep-item">
        <div class="bep-label">필요 사용자 수</div>
        <div class="bep-value">${Number(result.bepUsers || 0).toLocaleString()}명</div>
      </div>
    `;

    if (desc) {
      desc.textContent = `손익분기점까지 약 ${Number(result.bepMonths || 0)}개월이 필요합니다. 필요한 최소 사용자 수는 ${Number(result.bepUsers || 0).toLocaleString()}명입니다.`;
    }
  }

  function renderSuggestions(list) {
    const container = document.getElementById('suggestionList');
    if (!container) return;

    container.innerHTML = list.map((text, index) => `
      <div class="suggestion-item">
        <span class="suggestion-num">${index + 1}</span>
        <p class="suggestion-text">${escHtml(text)}</p>
      </div>
    `).join('');
  }

  function setSaveButtonState(saved) {
    const btn = document.getElementById('btnSave');
    if (!btn) return;

    if (saved) {
      btn.disabled = true;
      btn.textContent = '저장 완료';
    } else {
      btn.disabled = false;
      btn.textContent = '저장하기';
    }
  }

  async function loadResultFromBackend(resultId) {
    const data = await fetchJson(`/cost/results/${resultId}`);
    formData = data.formData || formData;
    analysisResult = data.result || analysisResult;
    savedResultId = data.id || resultId;
    persistSessionData();
    const params = new URLSearchParams();
    if (formData?.projectId) params.set('projectId', String(formData.projectId));
    if (formData?.from === 'detail') params.set('from', 'detail');
    params.set('analysisId', String(savedResultId));
    history.replaceState(null, '', `/cost/result?${params.toString()}`);
  }

  async function runAnalysisAgain() {
    if (!formData) return;

    const overlay = document.getElementById('loadingOverlay');
    overlay?.classList.add('active');

    try {
      const result = await postJson('/cost/analyze', {
        projectId: formData.projectId,
        revenueModels: formData.revenueModels || [],
        expectedUsers: Number(formData.expectedUsers || 0),
        pricePerUser: Number(formData.pricePerUser || 0)
      });

      analysisResult = result;
      savedResultId = null;
      persistSessionData();
      renderResults(analysisResult);
      setSaveButtonState(false);
      const params = new URLSearchParams();
      if (formData?.projectId) params.set('projectId', String(formData.projectId));
      if (formData?.from === 'detail') params.set('from', 'detail');
      const queryString = params.toString();
      history.replaceState(null, '', queryString ? `/cost/result?${queryString}` : '/cost/result');
    } catch (error) {
      console.error(error);
      alert(`Re-analysis failed. ${error.message}`);
    } finally {
      overlay?.classList.remove('active');
    }
  }

  async function saveResult() {
    if (!formData || !analysisResult) return;

    const btn = document.getElementById('btnSave');
    if (btn) btn.disabled = true;

    try {
      const response = await postJson('/cost/results', {
        projectId: Number(formData.projectId),
        revenueModels: formData.revenueModels || [],
        expectedUsers: Number(formData.expectedUsers || 0),
        pricePerUser: Number(formData.pricePerUser || 0),
        result: analysisResult
      });

      savedResultId = response.id || savedResultId;
      if (response.formData) {
        formData = response.formData;
      }
      if (response.result) {
        analysisResult = response.result;
      }

      persistSessionData();
      if (savedResultId) {
        const params = new URLSearchParams();
        if (formData?.projectId) params.set('projectId', String(formData.projectId));
        if (formData?.from === 'detail') params.set('from', 'detail');
        params.set('analysisId', String(savedResultId));
        history.replaceState(null, '', `/cost/result?${params.toString()}`);
      }
      setSaveButtonState(true);

      const toast = document.getElementById('saveToast');
      if (toast) {
        toast.classList.add('show');
        setTimeout(() => toast.classList.remove('show'), 3000);
      }

      setTimeout(() => {
        if (formData?.projectId) {
          window.location.href = `/project/detail/${formData.projectId}`;
        }
      }, 1500);
    } catch (error) {
      console.error(error);
      alert(`Save failed. ${error.message}`);
      if (btn) btn.disabled = false;
    }
  }

  function updateBreadcrumbFromState() {
    updateBreadcrumb();
  }

  function initButtons() {
    document.getElementById('btnBack')?.addEventListener('click', () => {
      if (formData?.from === 'detail' && formData.projectId) {
        window.location.href = `/cost/cost?projectId=${formData.projectId}&from=detail`;
      } else {
        window.location.href = '/cost/cost';
      }
    });

    document.getElementById('btnRetry')?.addEventListener('click', runAnalysisAgain);
    document.getElementById('btnSave')?.addEventListener('click', saveResult);
  }

  function initNotification() {
    const notiBtn = document.getElementById('notiBtn');
    const notiDropdown = document.getElementById('notiDropdown');
    if (!notiBtn || !notiDropdown) return;

    notiBtn.addEventListener('click', e => {
      e.stopPropagation();
      notiDropdown.classList.toggle('active');
    });

    document.addEventListener('click', e => {
      if (!notiDropdown.contains(e.target) && e.target !== notiBtn) {
        notiDropdown.classList.remove('active');
      }
    });
  }

  function initProfileMenu() {
    const trigger = document.querySelector('.profile-trigger');
    const dropdown = document.querySelector('.profile-menu .dropdown');
    if (!trigger || !dropdown) return;

    trigger.addEventListener('click', e => {
      e.stopPropagation();
      dropdown.style.display = dropdown.style.display === 'block' ? 'none' : 'block';
    });

    document.addEventListener('click', () => {
      dropdown.style.display = 'none';
    });
  }

  async function init() {
    const query = parseQuery();
    const resultId = query.get('analysisId') || query.get('resultId');

    loadSessionData();

    if (resultId) {
      try {
        await loadResultFromBackend(resultId);
        if (query.get('projectId')) {
          formData = { ...formData, projectId: query.get('projectId'), from: query.get('from') || formData.from };
        }
      } catch (error) {
        console.error(error);
        if (!formData || !analysisResult) {
          alert('Could not load the saved result. Please analyze again.');
          window.location.href = '/cost/cost';
          return;
        }
      }
    }

    if (!formData || !analysisResult) {
      if (formData && !analysisResult) {
        try {
          analysisResult = await postJson('/cost/analyze', {
            projectId: formData.projectId,
            revenueModels: formData.revenueModels || [],
            expectedUsers: Number(formData.expectedUsers || 0),
            pricePerUser: Number(formData.pricePerUser || 0)
          });
          persistSessionData();
        } catch (error) {
          console.error(error);
          alert(`AI 분석 결과를 불러오지 못했습니다. ${error.message}`);
          window.location.href = '/cost/cost';
          return;
        }
      }
    }

    if (!formData || !analysisResult) {
      alert('No analysis data found. Please analyze again.');
      window.location.href = '/cost/cost';
      return;
    }

    updateBreadcrumbFromState();
    initButtons();
    initNotification();
    initProfileMenu();
    renderResults(analysisResult);
    setSaveButtonState(Boolean(savedResultId));
    document.getElementById('loadingOverlay')?.classList.remove('active');
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
