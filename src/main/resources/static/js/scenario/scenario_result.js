(function () {
  const saveButton = document.getElementById('btnSaveScenarioResult');
  const rerunButton = document.getElementById('btnRerunScenario');
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
  const draftKey = 'scenarioDraft';

  function getContext() {
    return window.scenarioAnalysisContext || { request: {}, result: {} };
  }

  function csrfHeaders() {
    return csrfToken && csrfHeader ? { [csrfHeader]: csrfToken } : {};
  }

  function escHtml(value) {
    return String(value ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  function render() {
    const context = getContext();
    const result = context.result || {};
    const scenarios = Array.isArray(result.scenarios) ? result.scenarios : [];
    const criteria = Array.isArray(result.criteria) ? result.criteria : [];

    document.getElementById('recommendTitle').textContent = result.recommendedScenarioTitle || '-';
    document.getElementById('recommendReason').textContent = result.recommendationReason || '결과가 없습니다.';
    document.getElementById('finalSuggestionText').textContent = result.finalSuggestion || '-';
    document.getElementById('hybridSuggestionText').textContent = result.hybridSuggestion || '-';

    const summaryGrid = document.getElementById('scenarioSummaryGrid');
    if (summaryGrid) {
      summaryGrid.innerHTML = scenarios.map((scenario, index) => `
        <div class="summary-card ${index % 2 === 0 ? 'card-a' : 'card-b'}">
          <div class="card-head">
            <span class="plan-badge">${escHtml(scenario.key || `S${index + 1}`)}</span>
            <div class="score-info">
              <span class="score-num">${Number(scenario.totalScore || 0)}</span>
              <span class="score-label">종합 점수</span>
            </div>
          </div>
          <div style="font-weight:700;margin:8px 0 12px 0;">${escHtml(scenario.title || '')}</div>
          <div class="progress-bar"><div class="fill" style="width:${Number(scenario.totalScore || 0)}%;"></div></div>
          <div class="pros-cons-box">
            <div class="point-item pros">
              <div class="point-title">장점</div>
              <ul>${(scenario.pros || []).map(item => `<li>${escHtml(item)}</li>`).join('')}</ul>
            </div>
            <div class="point-item cons">
              <div class="point-title">단점</div>
              <ul>${(scenario.cons || []).map(item => `<li>${escHtml(item)}</li>`).join('')}</ul>
            </div>
          </div>
        </div>
      `).join('');
    }

    const head = document.getElementById('criteriaTableHead');
    const body = document.getElementById('criteriaTableBody');
    if (head && body) {
      head.innerHTML = `<tr><th>기준</th>${scenarios.map(s => `<th>${escHtml(s.key || '')}</th>`).join('')}<th>우세</th></tr>`;
      body.innerHTML = criteria.map(row => `
        <tr>
          <td>${escHtml(row.name || '')}</td>
          ${(row.values || []).map(value => `<td>${Number(value.score || 0)}</td>`).join('')}
          <td><span class="win-badge">${escHtml(row.winnerScenarioKey || '')}</span></td>
        </tr>
      `).join('');
    }
  }

  function moveToScenarioTab(projectId) {
    window.location.href = `/project/detail/${encodeURIComponent(projectId)}?tab=tab-scenario`;
  }

  async function saveResult() {
    const context = getContext();
    const request = context.request || {};
    const result = context.result || {};

    if (!request.projectId) {
      alert('저장할 프로젝트 정보가 없습니다.');
      return;
    }

    if (request.savedResultId) {
      moveToScenarioTab(request.projectId);
      return;
    }

    saveButton.disabled = true;
    try {
      const response = await fetch('/scenario/results', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...csrfHeaders()
        },
        body: JSON.stringify({
          projectId: request.projectId,
          compareTitle: request.compareTitle || '',
          result
        })
      });

      const payload = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(payload.message || payload.error || '결과 저장에 실패했습니다.');
      }

      moveToScenarioTab(request.projectId);
    } catch (error) {
      alert(error.message || '결과 저장에 실패했습니다.');
    } finally {
      saveButton.disabled = false;
    }
  }

  function rerunScenario() {
    const context = getContext();
    const request = context.request || {};
    sessionStorage.setItem(draftKey, JSON.stringify({
      projectId: request.projectId || '',
      compareTitle: request.compareTitle || '',
      scenarioCount: Array.isArray(request.scenarios) ? request.scenarios.length : 2
    }));
    sessionStorage.setItem(`${draftKey}:scenarios`, JSON.stringify(request.scenarios || []));

    const projectQuery = request.projectId ? `?projectId=${encodeURIComponent(request.projectId)}` : '';
    window.location.href = `/scenario${projectQuery}`;
  }

  document.addEventListener('DOMContentLoaded', () => {
    render();
    saveButton?.addEventListener('click', saveResult);
    rerunButton?.addEventListener('click', rerunScenario);
  });
})();
