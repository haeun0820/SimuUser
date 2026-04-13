/* ============================================================
   ai_user_result.js  –  AI 가상 유저 시뮬레이션 결과 페이지
   Spring Boot 프록시 엔드포인트(/aiuser/simulate)를 통해 Claude API 호출
   ============================================================ */

(function () {
  'use strict';

  function escHtml(str) {
    return String(str)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;')
      .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }

  const industryMap = {
    commerce: '커머스/쇼핑', fintech: '금융/핀테크', health: '의료/헬스케어',
    edu: '교육/에듀테크', content: '콘텐츠/엔터테인먼트', social: '소셜/커뮤니티',
    mobility: '모빌리티/여행', productivity: '생산성/비즈니스', '': ''
  };

  const ageLabels = {
    all: '전체', teens: '10대', twenties: '20대',
    thirties: '30대', forties: '40대', fifties: '50대 이상'
  };

  const genderLabels = { all: '전체', male: '남자', female: '여자' };

  function loadParams() {
    try { return JSON.parse(localStorage.getItem('simu_params') || 'null'); }
    catch (e) { return null; }
  }

  function initBreadcrumb(params) {
    const nav = document.getElementById('breadcrumbNav');
    if (!nav || !params) return;
    const fromProject = new URLSearchParams(window.location.search).get('from') === 'project';
    const title = params.project?.title || '';
    if (fromProject && title) {
      nav.innerHTML = `
        <a href="#">프로젝트</a><span class="bc-sep">›</span>
        <span>${escHtml(title)}</span><span class="bc-sep">›</span>
        <span class="bc-current">AI 가상 유저 시뮬레이션</span>`;
    } else {
      nav.innerHTML = `
        <a href="#">분석도구</a><span class="bc-sep">›</span>
        <span class="bc-current">AI 가상 유저 시뮬레이션</span>`;
    }
  }

/* ── 백엔드 프록시 호출 ── */
  async function callSimulateAPI(params) {
    const { project, personaCount, gender, ages, job } = params;

    const ageText = ages.includes('all') ? '전체' : ages.map(a => ageLabels[a] || a).join(', ');
    const genderText = genderLabels[gender] || gender;
    const industryText = industryMap[project?.industry] || project?.industry || '미지정';

    // 👇 1. HTML 메타 태그에서 CSRF 토큰 읽어오기
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    // 👇 2. 헤더 객체 생성 및 CSRF 토큰 추가
    const headers = {
      'Content-Type': 'application/json'
    };
    if (csrfToken && csrfHeader) {
      headers[csrfHeader] = csrfToken; 
    }

    // Spring Boot 프록시 엔드포인트 호출 (CORS 우회)
    const response = await fetch('/aiuser/simulate', {
      method: 'POST',
      headers: headers, // 👈 수정된 헤더를 여기에 적용
      body: JSON.stringify({
        serviceIdea:   project?.title       || '',
        description:   project?.description || '',
        targetUser:    project?.targetUser  || '',
        industry:      industryText,
        personaCount:  personaCount,
        gender:        genderText,
        ages:          ageText,
        job:           job || ''
      })
    });

    if (!response.ok) {
      const errText = await response.text();
      throw new Error(`서버 오류 ${response.status}: ${errText}`);
    }

    return await response.json();
  }

  
  /* ── 렌더: 통계 ── */
  function renderStats(result, personaCount) {
    const el = document.getElementById('statsGrid');
    if (!el) return;
    el.innerHTML = `
      <div class="stat-card">
        <div class="stat-value">${result.avgPurchaseIntent}%</div>
        <div class="stat-label">평균 구매 의사</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">${personaCount}명</div>
        <div class="stat-label">생성된 페르소나</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">${result.keyInsights?.length || 4}개</div>
        <div class="stat-label">핵심 인사이트</div>
      </div>`;
  }

  /* ── 렌더: 전반 반응 ── */
  function renderOverallReaction(result) {
    const el = document.getElementById('overallReaction');
    if (el) el.textContent = result.overallReaction || '';
  }

  const AVATAR_COLORS = [
    'linear-gradient(135deg,#2563eb,#6366f1)',
    'linear-gradient(135deg,#0891b2,#2563eb)',
    'linear-gradient(135deg,#7c3aed,#db2777)',
    'linear-gradient(135deg,#059669,#0891b2)',
  ];

  /* ── 렌더: 페르소나 카드 ── */
  function renderPersonas(result) {
    const el = document.getElementById('personaGrid');
    if (!el || !result.personas) return;
    const count = result.personas.length;
    el.className = `persona-grid cols-${count}`;

    el.innerHTML = result.personas.map((p, i) => {
      const initial = p.name?.charAt(0) || '?';
      const positive = (p.positiveReactions || []).map(r => `<li>${escHtml(r)}</li>`).join('');
      const negative = (p.negativeReactions || []).map(r => `<li>${escHtml(r)}</li>`).join('');
      const churn    = (p.churnPoints       || []).map(r => `<li>${escHtml(r)}</li>`).join('');

      return `
        <div class="persona-card" style="animation-delay:${i * 0.1}s">
          <div class="persona-head">
            <div class="persona-avatar" style="background:${AVATAR_COLORS[i % AVATAR_COLORS.length]}">${escHtml(initial)}</div>
            <div class="persona-info">
              <div class="persona-name">${escHtml(p.name)}</div>
              <div class="persona-meta">${p.age}세 ${escHtml(p.job)}</div>
            </div>
            <span class="persona-score">${p.purchaseScore}%</span>
          </div>
          <div><span class="consumer-badge">${escHtml(p.consumerType)}</span></div>
          <div class="reaction-section">
            <div class="reaction-label label-positive">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none">
                <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3H14z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
                <path d="M7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3" stroke="currentColor" stroke-width="2"/>
              </svg>
              긍정 반응
            </div>
            <ul class="reaction-list">${positive}</ul>
          </div>
          <div class="reaction-section">
            <div class="reaction-label label-negative">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none">
                <path d="M10 15v4a3 3 0 0 0 3 3l4-9V2H5.72a2 2 0 0 0-2 1.7l-1.38 9a2 2 0 0 0 2 2.3H10z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
                <path d="M17 2h2.67A2.31 2.31 0 0 1 22 4v7a2.31 2.31 0 0 1-2.33 2H17" stroke="currentColor" stroke-width="2"/>
              </svg>
              부정 반응
            </div>
            <ul class="reaction-list">${negative}</ul>
          </div>
          <div class="reaction-section">
            <div class="reaction-label label-churn">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none">
                <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
                <line x1="12" y1="9" x2="12" y2="13" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                <line x1="12" y1="17" x2="12.01" y2="17" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
              이탈 포인트
            </div>
            <ul class="reaction-list">${churn}</ul>
          </div>
        </div>`;
    }).join('');
  }

  /* ── 렌더: 인사이트 & 개선 제안 ── */
  function renderInsights(result) {
    const insightEl = document.getElementById('keyInsights');
    const improveEl = document.getElementById('improvements');

    if (insightEl && result.keyInsights) {
      insightEl.innerHTML = result.keyInsights.map((item, i) => `
        <li><span class="insight-num">${i + 1}</span><span>${escHtml(item)}</span></li>`).join('');
    }
    if (improveEl && result.improvements) {
      improveEl.innerHTML = result.improvements.map((item, i) => `
        <li><span class="insight-num">${i + 1}</span><span>${escHtml(item)}</span></li>`).join('');
    }
  }

  function showLoading() {
    document.getElementById('resultContent').style.display = 'none';
    document.getElementById('errorState').style.display   = 'none';
    document.getElementById('loadingState').style.display = 'flex';
  }

  function hideLoading() {
    document.getElementById('loadingState').style.display = 'none';
    document.getElementById('resultContent').style.display = 'block';
  }

  function showError(msg) {
    document.getElementById('loadingState').style.display  = 'none';
    document.getElementById('resultContent').style.display = 'none';
    const errEl = document.getElementById('errorState');
    errEl.style.display = 'block';
    const msgEl = errEl.querySelector('.error-msg');
    if (msgEl) msgEl.textContent = msg || '시뮬레이션 중 오류가 발생했습니다.';
  }

  async function runSimulation() {
    const params = loadParams();
    if (!params) {
      showError('시뮬레이션 파라미터가 없습니다. 설정 페이지로 돌아가 주세요.');
      return;
    }

    initBreadcrumb(params);
    showLoading();

    try {
      const result = await callSimulateAPI(params);
      renderStats(result, params.personaCount);
      renderOverallReaction(result);
      renderPersonas(result);
      renderInsights(result);
      hideLoading();
    } catch (err) {
      console.error('Simulation error:', err);
      showError('AI 분석 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.\n' + err.message);
    }
  }

  function initButtons() {
    // 정상 결과 화면 버튼
    document.getElementById('btnBack')?.addEventListener('click', () => {
      window.location.href = document.getElementById('btnBack').dataset.href || '/aiuser/ai_user';
    });
    document.getElementById('btnRetry')?.addEventListener('click', () => {
      runSimulation();
    });

    // 에러 화면 버튼
    document.getElementById('btnBackErr')?.addEventListener('click', () => {
      window.location.href = document.getElementById('btnBackErr').dataset.href || '/aiuser/ai_user';
    });
    document.getElementById('btnRetryErr')?.addEventListener('click', () => {
      runSimulation();
    });
  }

  function init() {
    initButtons();
    runSimulation();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();