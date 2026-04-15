/* ============================================================
   cost_result.js – 수익성 분석 결과 페이지
   ============================================================ */

let formData = null;
let analysisResult = null;

document.addEventListener('DOMContentLoaded', () => {
  formData = loadFormData();
  if (!formData) {
    alert('분석 데이터가 없습니다. 분석 페이지로 돌아갑니다.');
    window.location.href = '/cost/cost';
    return;
  }

  updateBreadcrumb();
  initButtons();
  initNotification();
  initProfileMenu();

  // 로딩 효과 (분석 시뮬레이션)
  setTimeout(() => {
    analysisResult = simulateAnalysis(formData);
    renderResults(analysisResult);
    document.getElementById('loadingOverlay')?.classList.remove('active');
  }, 1200);
});

/* ── 폼 데이터 로드 ── */
function loadFormData() {
  try {
    const raw = sessionStorage.getItem('cost_form_data');
    return raw ? JSON.parse(raw) : null;
  } catch (e) {
    return null;
  }
}

/* ── 브레드크럼 ── */
function updateBreadcrumb() {
  if (!formData) return;
  const nav = document.getElementById('breadcrumbNav');
  if (!nav) return;

  if (formData.from === 'detail' && formData.projectId) {
    nav.innerHTML = `
      <a href="/project/all">프로젝트</a>
      <span class="bc-sep">›</span>
      <a href="/project/detail/${formData.projectId}">${escHtml(formData.projectTitle)}</a>
      <span class="bc-sep">›</span>
      <a href="/cost/cost?projectId=${formData.projectId}&from=detail">비용 &amp; 수익성 분석</a>
      <span class="bc-sep">›</span>
      <span class="bc-current">결과</span>
    `;
  } else {
    nav.innerHTML = `
      <a href="/dashboard">분석도구</a>
      <span class="bc-sep">›</span>
      <a href="/cost/cost">비용 &amp; 수익성 분석</a>
      <span class="bc-sep">›</span>
      <span class="bc-current">결과</span>
    `;
  }
}

/* ── 분석 시뮬레이션 ── */
function simulateAnalysis(data) {
  const { revenueModels, expectedUsers, pricePerUser } = data;
  const modelCount = revenueModels.length;

  // 최대 월 매출 (만원)
  const maxMRR = Math.round((expectedUsers * pricePerUser) / 10000);

  // 개발 비용 (수익 모델 수 + 규모 기반)
  const totalDevCost = 8000 + modelCount * 1000;
  const frontend  = Math.round(totalDevCost * 0.200 / 100) * 100;
  const backend   = Math.round(totalDevCost * 0.278 / 100) * 100;
  const aiml      = Math.round(totalDevCost * 0.389 / 100) * 100;
  const design    = totalDevCost - frontend - backend - aiml;

  // 월 운영 비용 (만원)
  const serverCost      = 200;
  const apiCost         = Math.max(100, Math.round(expectedUsers * 0.3 / 100) * 100);
  const maintenanceCost = 150;
  const marketingCost   = 100;
  const totalMonthlyCost = serverCost + apiCost + maintenanceCost + marketingCost;

  // 예상 수익 추이 (만원) — S커브 성장 모델
  const rev3  = Math.max(10, Math.round(maxMRR * 0.09 / 10) * 10);
  const rev6  = Math.max(20, Math.round(maxMRR * 0.27 / 10) * 10);
  const rev12 = Math.max(40, Math.round(maxMRR * 0.55 / 10) * 10);

  // BEP 기간 (개월) — 성장률 반영 단순 추정
  const effectiveMRR = Math.max(maxMRR * 0.325, totalMonthlyCost * 0.5);
  const bepMonths = Math.min(60, Math.max(6, Math.round(totalDevCost / effectiveMRR)));

  // BEP 최소 사용자 (플랫폼 수수료 10% 반영)
  const bepUsers = pricePerUser > 0
    ? Math.ceil((totalMonthlyCost * 10000) / (pricePerUser * 0.9))
    : 0;

  // 투자 점수 (0–100)
  const profitRatio  = maxMRR > 0 ? (maxMRR - totalMonthlyCost) / totalMonthlyCost : -1;
  const bepPenalty   = Math.max(0, bepMonths - 12) * 2;
  const score = Math.min(95, Math.max(20,
    Math.round(65 + profitRatio * 20 - bepPenalty + modelCount * 3)
  ));
  const grade = score >= 75 ? '우수' : score >= 50 ? '보통' : '주의';

  // 투자 제안
  const suggestions = buildSuggestions(revenueModels, score, bepMonths, maxMRR, totalMonthlyCost);

  return {
    grade, score, bepMonths, bepUsers,
    devCosts: { frontend, backend, aiml, design, total: totalDevCost },
    monthlyCosts: {
      server: serverCost, api: apiCost,
      maintenance: maintenanceCost, marketing: marketingCost,
      total: totalMonthlyCost
    },
    revenue: { m3: rev3, m6: rev6, m12: rev12 },
    maxMRR,
    suggestions
  };
}

/* ── 투자 제안 생성 ── */
function buildSuggestions(models, score, bepMonths, maxMRR, monthlyCost) {
  const list = [];

  if (!models.includes('광고 수익') && !models.includes('프리미엄')) {
    list.push('광고 수익 모델 외에 Freemium, Tiered Pricing 도입을 통해 사용자당 수익(ARPU)을 높이고 안정적인 현금 흐름을 확보해야 합니다.');
  }
  if (bepMonths > 18) {
    list.push('초기 사용자 확보를 위한 명확한 마케팅 전략(예: 베타 테스터 프로그램, 인플루언서 협업, 특정 산업군 타겟팅) 수립 및 실행이 필요합니다.');
  }
  list.push('AI API 비용 최적화를 위해 자체 모델 개발 또는 효율적인 프롬프트 엔지니어링 역량을 강화하여 변동 비용을 절감하는 방안을 모색해야 합니다.');
  list.push('시장 분석 리포트, 재무 시뮬레이션 등 핵심 기능의 차별점을 부각하여 경쟁 우위를 확보하고, 초기 고객의 이탈률을 낮추는 전략이 중요합니다.');
  if (models.includes('구독형') && maxMRR > monthlyCost * 1.5) {
    list.push('구독 모델의 연간 결제 할인 옵션을 추가하면 현금 흐름 안정화와 고객 이탈률(Churn) 감소에 효과적입니다.');
  }

  return list.slice(0, 4);
}

/* ── 결과 렌더링 ── */
function renderResults(r) {
  renderSummaryGrid(r);
  renderDevCosts(r.devCosts);
  renderMonthlyCosts(r.monthlyCosts);
  renderRevenue(r.revenue);
  renderBEP(r);
  renderSuggestions(r.suggestions);
}

/* 상단 3칸 요약 */
function renderSummaryGrid(r) {
  const grid = document.getElementById('summaryGrid');
  if (!grid) return;

  const gradeClass = r.grade === '우수' ? 'grade-excellent'
                   : r.grade === '보통' ? 'grade-normal' : 'grade-caution';

  grid.innerHTML = `
    <div class="summary-item">
      <div class="summary-label">투자 적합도</div>
      <div class="grade-badge ${gradeClass}">${r.grade}</div>
    </div>
    <div class="summary-item">
      <div class="summary-label">투자 점수 / 100</div>
      <div class="summary-value">${r.score}</div>
      <div class="score-bar-wrap">
        <div class="score-bar-fill" id="scoreBar" style="width:0%"></div>
      </div>
    </div>
    <div class="summary-item">
      <div class="summary-label">손익분기점 (BEP)</div>
      <div class="summary-value">${r.bepMonths}<span style="font-size:18px; font-weight:600;">개월</span></div>
    </div>
  `;

  // 애니메이션
  setTimeout(() => {
    const bar = document.getElementById('scoreBar');
    if (bar) bar.style.width = r.score + '%';
  }, 100);
}

/* 개발 비용 */
function renderDevCosts(d) {
  const grid = document.getElementById('devCostGrid');
  if (!grid) return;

  const items = [
    { label: '프론트엔드', value: d.frontend },
    { label: '백엔드',     value: d.backend  },
    { label: 'AI/ML',      value: d.aiml     },
    { label: '디자인',     value: d.design   },
  ];

  grid.innerHTML = items.map(it => `
    <div class="dev-cost-item">
      <div class="dev-cost-label">${it.label}</div>
      <div class="dev-cost-value">${fmtWon(it.value)}</div>
    </div>`
  ).join('');

  const totalEl = document.getElementById('totalDevCostVal');
  if (totalEl) totalEl.textContent = fmtWon(d.total);
}

/* 월 운영 비용 */
function renderMonthlyCosts(m) {
  const grid = document.getElementById('monthlyCostGrid');
  if (!grid) return;

  const items = [
    { label: '서버',    value: m.server      },
    { label: 'API',     value: m.api         },
    { label: '유지보수', value: m.maintenance },
    { label: '마케팅',  value: m.marketing   },
  ];

  grid.innerHTML = items.map(it => `
    <div class="monthly-cost-item">
      <div class="monthly-cost-label">${it.label}</div>
      <div class="monthly-cost-value">${fmtWon(it.value)}</div>
    </div>`
  ).join('');

  const totalEl = document.getElementById('totalMonthlyCostVal');
  if (totalEl) totalEl.textContent = fmtWon(m.total);
}

/* 예상 수익 */
function renderRevenue(rev) {
  const grid = document.getElementById('revenueGrid');
  if (!grid) return;

  grid.innerHTML = `
    <div class="revenue-item">
      <div class="revenue-period">3개월 후</div>
      <div class="revenue-value">${fmtWon(rev.m3)}</div>
    </div>
    <div class="revenue-item">
      <div class="revenue-period">6개월 후</div>
      <div class="revenue-value">${fmtWon(rev.m6)}</div>
    </div>
    <div class="revenue-item">
      <div class="revenue-period">12개월 후</div>
      <div class="revenue-value">${fmtWon(rev.m12)}</div>
    </div>
  `;
}

/* BEP 분석 */
function renderBEP(r) {
  const grid = document.getElementById('bepGrid');
  const desc = document.getElementById('bepDesc');
  if (!grid) return;

  grid.innerHTML = `
    <div class="bep-item">
      <div class="bep-label">BEP 달성 기간</div>
      <div class="bep-value">${r.bepMonths}개월</div>
    </div>
    <div class="bep-item">
      <div class="bep-label">필요 사용자 수</div>
      <div class="bep-value">${r.bepUsers.toLocaleString()}명</div>
    </div>
  `;

  if (desc) {
    desc.textContent =
      `손익분기점 도달까지 약 ${r.bepMonths}개월 소요되며, 이는 월간 고정 비용을 충당하기 위한 최소 사용자 수` +
      `(${r.bepUsers.toLocaleString()}명)를 확보하고, 총 개발 비용 ${fmtWon(r.devCosts.total)}을 회수하는 데 ` +
      `필요한 기간을 의미합니다.`;
  }
}

/* 투자 제안 */
function renderSuggestions(list) {
  const container = document.getElementById('suggestionList');
  if (!container) return;

  container.innerHTML = list.map((text, i) => `
    <div class="suggestion-item">
      <span class="suggestion-num">${i + 1}</span>
      <p class="suggestion-text">${escHtml(text)}</p>
    </div>`
  ).join('');
}

/* ── 버튼 초기화 ── */
function initButtons() {
  // 1. 설정으로 돌아가기
  const btnBack = document.getElementById('btnBack');
  if (btnBack) {
    btnBack.addEventListener('click', () => {
      if (formData?.from === 'detail' && formData.projectId) {
        window.location.href = `/cost/cost?projectId=${formData.projectId}&from=detail`;
      } else {
        window.location.href = '/cost/cost';
      }
    });
  }

  // 2. 다시 시뮬레이션
  const btnRetry = document.getElementById('btnRetry');
  if (btnRetry) {
    btnRetry.addEventListener('click', () => {
      const overlay = document.getElementById('loadingOverlay');
      if (overlay) overlay.classList.add('active');
      
      // 결과 재계산 및 렌더링
      setTimeout(() => {
        analysisResult = simulateAnalysis(formData);
        renderResults(analysisResult);
        
        // 다시 시뮬레이션하면 '저장' 버튼을 다시 활성화
        const btnSave = document.getElementById('btnSave');
        if (btnSave) {
          btnSave.disabled = false;
          btnSave.textContent = '저장하기';
          btnSave.style.background = ''; // 원래 스타일로 복구
        }
        
        if (overlay) overlay.classList.remove('active');
      }, 1000);
    });
  }

  // 3. 저장하기 (이 부분이 빠져있었습니다)
  const btnSave = document.getElementById('btnSave');
  if (btnSave) {
    btnSave.addEventListener('click', saveResult);
  }
} // initButtons 끝

/* ── 결과 저장 ── */
async function saveResult() {
  if (!formData || !analysisResult) return;

  const btnSave = document.getElementById('btnSave');
  if (btnSave) btnSave.disabled = true;

  try {
    const allProjects = JSON.parse(localStorage.getItem('simu_projects') || '[]');
    const pIdx = allProjects.findIndex(p => String(p.id) === String(formData.projectId));

    if (pIdx === -1) {
      alert('저장할 프로젝트를 찾을 수 없습니다.');
      if (btnSave) btnSave.disabled = false;
      return;
    }

    if (!allProjects[pIdx].costAnalyses) {
      allProjects[pIdx].costAnalyses = [];
    }

    const analysisId = 'cost_' + Date.now();
    const newEntry = {
      id: analysisId,
      type: 'cost',
      title: '비용 & 수익성 분석',
      createdAt: new Date().toISOString(),
      formData: formData,
      result: analysisResult,
      starred: false,
    };

    allProjects[pIdx].costAnalyses.unshift(newEntry);
    localStorage.setItem('simu_projects', JSON.stringify(allProjects));

    // 1. 버튼 상태 업데이트 (market_result 스타일)
    if (btnSave) {
      btnSave.textContent = '저장 완료';
      btnSave.style.background = '#059669'; // 녹색 유지
    }

    // 2. 브라우저 주소창 업데이트 (분석 ID 반영)
    const params = new URLSearchParams(window.location.search);
    params.set('analysisId', analysisId);
    window.history.replaceState(null, '', `/cost/result?${params.toString()}`);

    // 3. 토스트 알림 표시 (HTML에 toast 요소가 있다면)
    const toast = document.getElementById('saveToast');
    if (toast) {
      toast.classList.add('show');
      setTimeout(() => toast.classList.remove('show'), 3000);
    }

    // 4. 프로젝트 상세 페이지로 자동 이동 (잠시 후)
    setTimeout(() => {
      if (formData.projectId) {
        window.location.href = `/project/detail/${formData.projectId}`;
      }
    }, 1500);

  } catch (error) {
    console.error('저장 중 오류 발생:', error);
    alert('결과 저장에 실패했습니다.');
    if (btnSave) btnSave.disabled = false;
  }
}

/* ── 알림 드롭다운 ── */
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

  const clearBtn = notiDropdown.querySelector('.clear-all');
  if (clearBtn) {
    clearBtn.addEventListener('click', () => {
      if (confirm('모든 알림을 삭제하시겠습니까?')) {
        notiDropdown.querySelector('.noti-list').innerHTML =
          '<p style="padding:40px; text-align:center; color:#94a3b8;">새로운 알림이 없습니다.</p>';
        const badge = notiBtn.querySelector('.noti-badge');
        if (badge) badge.style.display = 'none';
      }
    });
  }
}

/* ── 프로필 메뉴 ── */
function initProfileMenu() {
  const trigger = document.querySelector('.profile-trigger');
  const dropdown = document.querySelector('.profile-menu .dropdown');
  if (!trigger || !dropdown) return;

  trigger.addEventListener('click', e => {
    e.stopPropagation();
    dropdown.style.display = dropdown.style.display === 'block' ? 'none' : 'block';
  });

  document.addEventListener('click', () => {
    if (dropdown) dropdown.style.display = 'none';
  });
}

/* ── 유틸 ── */
function fmtWon(value) {
  return `${value.toLocaleString()}만원`;
}

function escHtml(str) {
  return String(str || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}