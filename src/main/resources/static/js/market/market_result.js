/* ============================================================
   market_result.js – 시장 & 경쟁 분석 결과 페이지
   ============================================================ */

(function () {
  /* ── URL 파라미터 ── */
  const urlParams = new URLSearchParams(window.location.search);
  const projectId = urlParams.get('projectId');
  const fromDetail = urlParams.get('from') === 'detail';

  /* ── 선택된 프로젝트 ── */
  let currentProject = null;

  /* ── 유틸 ── */
  function escHtml(str) {
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  /* ── 더미 분석 결과 생성 ── */
  function generateAnalysisResult(project) {
    // 실제 서비스에서는 API 호출 결과를 사용
    return {
      competitionLevel: '중간',
      saturation: 30,
      competitorCount: 4,
      marketSize: {
        tam: { value: '1000조원', desc: '글로벌 시장 조사 및 분석 소프트웨어의 시장 규모 (AI 기반 분석 도구 포함)' },
        sam: { value: '5000억원', desc: '국내 스타트업 및 중소기업 대상 시장 분석 솔루션 시장' },
        som: { value: '150억원', desc: '초기 3년 내 목표 점유 가능 시장' }
      },
      keywords: ['#AI 시뮬레이션', '#가상 사용자 테스트', '#시장 예측 AI', '#프로덕트 기획 자동화', '#데이터 기반 의사결정'],
      competitors: [
        {
          name: 'UserTesting',
          tags: ['사용자 행동 분석', '정성적 피드백'],
          weakness: 'AI 기반 가상 사용자 시뮬레이션 및 즉각적인 반응 예측 기능 부족'
        },
        {
          name: 'Qualtrics (Customer Experience)',
          tags: ['사용자 행동 분석', '설문 기반 분석', '정성적 피드백'],
          weakness: 'AI 기반 가상 사용자 시뮬레이션 및 즉각적인 반응 예측 기능 부족'
        },
        {
          name: 'Maze',
          tags: ['사용성 테스트', '프로토타입 검증', '정량적 피드백'],
          weakness: '시장·경쟁 분석 통합 기능 부재, 실시간 AI 예측 미지원'
        },
        {
          name: 'Hotjar',
          tags: ['히트맵', '사용자 행동 분석', '세션 녹화'],
          weakness: 'AI 기반 시뮬레이션 및 사전 예측 기능 없음, 정성 분석 한계'
        }
      ],
      differentiation: [
        'AI 기반 가상 사용자 시뮬레이션을 통한 즉각적인 아이디어 검증 및 반응 예측',
        '시장 분석 리포트, 재무 시뮬레이션(BEP) 등 통합적인 비즈니스 기획 지원',
        'A/B 테스트 시나리오 비교를 통한 최적의 기획안 도출 지원',
        '자동 생성되는 기획서 및 보고서 템플릿 제공으로 문서 작업 시간 단축'
      ],
      risks: [
        'AI 시뮬레이션의 정확도 및 신뢰성 검증의 어려움 (실제 시장과의 괴리 가능성)',
        '경쟁사의 유사 기능 도입 또는 더 강력한 AI 모델 출시',
        '개인정보 보호 및 데이터 보안 문제 (사용자 데이터 학습 시)',
        '높은 AI API 비용으로 인한 서비스 가격 경쟁력 확보의 어려움'
      ],
      opportunity: `신규 서비스 기획 및 출시를 앞둔 기업, 스타트업, 개인 사업자들이 실제 사용자 테스트의 시간과 비용 부담 없이 AI 기반으로 아이디어의 시장성, 사용자 반응, 수익성을 빠르게 검증하고 기획을 고도화할 수 있는 강력한 니즈가 존재합니다. 특히, AI 기술 발전에 따라 가상 사용자 시뮬레이션의 정확도가 높아지면서, 초기 단계의 의사결정 리스크를 줄이는 데 핵심적인 도구로 자리매김할 수 있는 시장 기회가 큽니다.`
    };
  }

  /* ── 브레드크럼 렌더링 ── */
  function renderBreadcrumb() {
    const nav = document.getElementById('breadcrumbNav');
    if (!nav) return;

    if (fromDetail && projectId) {
      const projectTitle = currentProject ? escHtml(currentProject.title) : '프로젝트';
      nav.innerHTML = `
        <a href="/project/all">프로젝트</a>
        <span class="bc-sep">›</span>
        <a href="/project/detail/${projectId}">${projectTitle}</a>
        <span class="bc-sep">›</span>
        <span class="bc-current">시장 &amp; 경쟁 분석</span>`;
    } else {
      nav.innerHTML = `
        <span>분석도구</span>
        <span class="bc-sep">›</span>
        <span class="bc-current">시장 &amp; 경쟁 분석</span>`;
    }
  }

  /* ── 결과 렌더링 ── */
  function renderResult(result) {
    // 경쟁 강도
    const badge = document.getElementById('competitionBadge');
    if (badge) badge.textContent = result.competitionLevel;

    // 시장 포화도
    const satVal = document.getElementById('saturationValue');
    const satBar = document.getElementById('saturationBar');
    if (satVal) satVal.textContent = `${result.saturation}%`;
    if (satBar) {
      setTimeout(() => { satBar.style.width = `${result.saturation}%`; }, 100);
    }

    // 주요 경쟁사 수
    const compCount = document.getElementById('competitorCount');
    if (compCount) compCount.textContent = `${result.competitorCount}개`;

    // 시장 규모
    const msCards = document.getElementById('marketSizeCards');
    if (msCards) {
      const { tam, sam, som } = result.marketSize;
      msCards.innerHTML = `
        <div class="market-size-card ms-tam">
          <div class="ms-tag">TAM</div>
          <div class="ms-sublabel">전체 시장</div>
          <div class="ms-value">${escHtml(tam.value)}</div>
          <div class="ms-desc">${escHtml(tam.desc)}</div>
        </div>
        <div class="market-size-card ms-sam">
          <div class="ms-tag">SAM</div>
          <div class="ms-sublabel">서비스 가능 시장</div>
          <div class="ms-value">${escHtml(sam.value)}</div>
          <div class="ms-desc">${escHtml(sam.desc)}</div>
        </div>
        <div class="market-size-card ms-som">
          <div class="ms-tag">SOM</div>
          <div class="ms-sublabel">획득 가능 시장</div>
          <div class="ms-value">${escHtml(som.value)}</div>
          <div class="ms-desc">${escHtml(som.desc)}</div>
        </div>`;
    }

    // 트렌드 키워드
    const keywordList = document.getElementById('keywordList');
    if (keywordList) {
      keywordList.innerHTML = result.keywords
        .map(k => `<span class="keyword-tag">${escHtml(k)}</span>`)
        .join('');
    }

    // 경쟁사 분석
    const competitorList = document.getElementById('competitorList');
    if (competitorList) {
      competitorList.innerHTML = result.competitors.map(c => `
        <div class="competitor-item">
          <div class="competitor-name">${escHtml(c.name)}</div>
          <div class="competitor-body">
            <div class="competitor-tags">
              ${c.tags.map(t => `<span class="comp-tag">${escHtml(t)}</span>`).join('')}
            </div>
            <div class="competitor-weakness">
              <div class="weakness-label">약점</div>
              ${escHtml(c.weakness)}
            </div>
          </div>
        </div>`).join('');
    }

    // 차별화 포인트
    const diffList = document.getElementById('diffList');
    if (diffList) {
      diffList.innerHTML = result.differentiation
        .map(d => `<li class="diff-item">${escHtml(d)}</li>`)
        .join('');
    }

    // 리스크
    const riskList = document.getElementById('riskList');
    if (riskList) {
      riskList.innerHTML = result.risks
        .map(r => `<li class="risk-item">${escHtml(r)}</li>`)
        .join('');
    }

    // 시장 기회 요약
    const oppText = document.getElementById('opportunityText');
    if (oppText) oppText.textContent = result.opportunity;
  }

  /* ── 결과 저장 (localStorage) ── */
  function saveResult(result) {
    if (!projectId) return;

    const allProjects = JSON.parse(localStorage.getItem('simu_projects') || '[]');
    const pIdx = allProjects.findIndex(p => String(p.id) === projectId);

    const analysisRecord = {
      id: 'market_' + Date.now(),
      type: 'market',
      title: '시장 & 경쟁 분석',
      createdAt: new Date().toISOString(),
      result: result,
      starred: false
    };

    if (pIdx !== -1) {
      if (!allProjects[pIdx].marketAnalyses) {
        allProjects[pIdx].marketAnalyses = [];
      }
      allProjects[pIdx].marketAnalyses.unshift(analysisRecord);
      localStorage.setItem('simu_projects', JSON.stringify(allProjects));
    }

    return analysisRecord;
  }

  /* ── 토스트 표시 ── */
  function showToast() {
    const toast = document.getElementById('saveToast');
    if (!toast) return;
    toast.classList.add('show');
    setTimeout(() => toast.classList.remove('show'), 3000);
  }

  /* ── 뒤로 가기 ── */
  function goBack() {
    if (fromDetail && projectId) {
      window.location.href = `/market?projectId=${projectId}&from=detail`;
    } else {
      window.location.href = '/market';
    }
  }

  /* ── 초기화 ── */
  async function init() {
    // 프로젝트 정보 로드
    const cached = sessionStorage.getItem('market_selected_project');
    if (cached) {
      try { currentProject = JSON.parse(cached); } catch {}
    }

    if (!currentProject && projectId) {
      try {
        const res = await fetch('/api/projects');
        if (res.ok) {
          const projects = await res.json();
          currentProject = projects.find(p => String(p.id) === projectId) || null;
        }
      } catch {
        const all = JSON.parse(localStorage.getItem('simu_projects') || '[]');
        currentProject = all.find(p => String(p.id) === projectId) || null;
      }
    }

    renderBreadcrumb();

    // 분석 결과 생성
    const result = generateAnalysisResult(currentProject);
    renderResult(result);

    // 저장 버튼
    const btnSave = document.getElementById('btnSave');
    if (btnSave) {
      btnSave.addEventListener('click', () => {
        saveResult(result);
        showToast();
        // 프로젝트 상세로 이동 (저장 후)
        if (projectId) {
          setTimeout(() => {
            window.location.href = `/project/detail/${projectId}`;
          }, 1800);
        }
      });
    }

    // 뒤로 가기 버튼
    const btnBack = document.getElementById('btnBack');
    if (btnBack) btnBack.addEventListener('click', goBack);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();