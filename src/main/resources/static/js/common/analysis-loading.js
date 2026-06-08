(function () {
  'use strict';

  const DEFAULT_TITLE = 'AI 분석을 진행하고 있습니다';
  const DEFAULT_SUBTITLE = '결과를 준비하는 동안 잠시만 기다려 주세요.';

  function ensureOverlay() {
    let overlay = document.getElementById('analysisLoadingOverlay');
    if (overlay) return overlay;

    overlay = document.createElement('div');
    overlay.id = 'analysisLoadingOverlay';
    overlay.className = 'analysis-loading-overlay';
    overlay.setAttribute('role', 'status');
    overlay.setAttribute('aria-live', 'polite');
    overlay.innerHTML = `
      <div class="analysis-loading-panel">
        <div class="analysis-loading-spinner" aria-hidden="true"></div>
        <p class="analysis-loading-title" data-analysis-loading-title>${DEFAULT_TITLE}</p>
        <p class="analysis-loading-subtitle" data-analysis-loading-subtitle>${DEFAULT_SUBTITLE}</p>
      </div>
    `;
    document.body.appendChild(overlay);
    return overlay;
  }

  function show(options) {
    const config = typeof options === 'string' ? { title: options } : (options || {});
    const overlay = ensureOverlay();
    const title = overlay.querySelector('[data-analysis-loading-title]');
    const subtitle = overlay.querySelector('[data-analysis-loading-subtitle]');

    if (title) title.textContent = config.title || DEFAULT_TITLE;
    if (subtitle) subtitle.textContent = config.subtitle || DEFAULT_SUBTITLE;
    overlay.classList.add('active');
    document.body.classList.add('analysis-loading-lock');
    return overlay;
  }

  function hide() {
    document.getElementById('analysisLoadingOverlay')?.classList.remove('active');
    document.body.classList.remove('analysis-loading-lock');
  }

  window.AnalysisLoading = { show, hide };
})();

