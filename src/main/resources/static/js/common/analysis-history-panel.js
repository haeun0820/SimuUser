(function () {
  'use strict';

  function escHtml(value) {
    return String(value ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function formatDate(value) {
    if (!value) return '';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    return date.toLocaleDateString();
  }

  function ensureStyle() {
    if (document.getElementById('analysis-history-panel-style')) return;
    const style = document.createElement('style');
    style.id = 'analysis-history-panel-style';
    style.textContent = `
      .analysis-history-panel{margin-top:18px;background:#fff;border:1px solid #e5e7eb;border-radius:12px;padding:18px;box-shadow:0 2px 8px rgba(15,23,42,.04)}
      .analysis-history-head{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:12px}
      .analysis-history-title{margin:0;font-size:16px;font-weight:800;color:#111827}
      .analysis-history-count{font-size:12px;font-weight:700;color:#64748b;background:#f1f5f9;border-radius:999px;padding:4px 9px}
      .analysis-history-list{display:flex;flex-direction:column;gap:10px}
      .analysis-history-item{width:100%;display:flex;align-items:center;justify-content:space-between;gap:14px;text-align:left;background:#fff;border:1px solid #e5e7eb;border-radius:10px;padding:13px 14px;cursor:pointer;transition:border-color .15s ease,box-shadow .15s ease,transform .15s ease}
      .analysis-history-item:hover{border-color:#93c5fd;box-shadow:0 8px 18px rgba(37,99,235,.09);transform:translateY(-1px)}
      .analysis-history-main{min-width:0}
      .analysis-history-name{display:block;font-size:14px;font-weight:800;color:#111827;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
      .analysis-history-meta{display:flex;gap:8px;flex-wrap:wrap;margin-top:5px;font-size:12px;color:#64748b}
      .analysis-history-badge{color:#2563eb;background:#eff6ff;border-radius:999px;padding:2px 7px;font-weight:700}
      .analysis-history-arrow{flex:0 0 auto;color:#94a3b8;font-size:18px}
      .analysis-history-empty{padding:24px 10px;text-align:center;color:#94a3b8;font-size:14px}
    `;
    document.head.appendChild(style);
  }

  function defaultTitle(item, config) {
    if (typeof config.title === 'function') return config.title(item);
    return item.title || item.compareTitle || config.fallbackTitle || '분석 내역';
  }

  function defaultMeta(item, config) {
    if (typeof config.meta === 'function') return config.meta(item);
    return formatDate(item.createdAt);
  }

  function create(config) {
    ensureStyle();

    const anchor = typeof config.anchor === 'string'
      ? document.querySelector(config.anchor)
      : config.anchor;
    if (!anchor) return { load: function () {}, clear: function () {} };

    const panel = document.createElement('section');
    panel.className = 'analysis-history-panel';
    panel.innerHTML = `
      <div class="analysis-history-head">
        <h2 class="analysis-history-title">${escHtml(config.heading || '분석 내역')}</h2>
        <span class="analysis-history-count" data-history-count>0개</span>
      </div>
      <div class="analysis-history-list" data-history-list>
        <div class="analysis-history-empty">프로젝트를 선택하면 분석 내역이 표시됩니다.</div>
      </div>
    `;

    if (config.position === 'append') {
      anchor.appendChild(panel);
    } else if (config.position === 'before') {
      anchor.parentNode.insertBefore(panel, anchor);
    } else {
      anchor.insertAdjacentElement('afterend', panel);
    }

    const countEl = panel.querySelector('[data-history-count]');
    const listEl = panel.querySelector('[data-history-list]');

    function render(items, projectId) {
      const list = Array.isArray(items) ? items : [];
      countEl.textContent = `${list.length}개`;

      if (list.length === 0) {
        listEl.innerHTML = `<div class="analysis-history-empty">${escHtml(config.emptyText || '아직 저장된 분석 내역이 없습니다.')}</div>`;
        return;
      }

      listEl.innerHTML = list.map(item => {
        const title = defaultTitle(item, config);
        const meta = defaultMeta(item, config);
        const badge = item.starred ? '<span class="analysis-history-badge">즐겨찾기</span>' : '';
        return `
          <button type="button" class="analysis-history-item" data-id="${escHtml(item.id)}">
            <span class="analysis-history-main">
              <span class="analysis-history-name">${escHtml(title)}</span>
              <span class="analysis-history-meta">${badge}${meta ? `<span>${escHtml(meta)}</span>` : ''}</span>
            </span>
            <span class="analysis-history-arrow">›</span>
          </button>
        `;
      }).join('');

      listEl.querySelectorAll('.analysis-history-item').forEach(button => {
        button.addEventListener('click', () => {
          const id = button.dataset.id;
          if (!id || typeof config.resultUrl !== 'function') return;
          window.location.href = config.resultUrl(id, projectId);
        });
      });
    }

    async function load(projectId) {
      if (!projectId) {
        countEl.textContent = '0개';
        listEl.innerHTML = '<div class="analysis-history-empty">프로젝트를 선택하면 분석 내역이 표시됩니다.</div>';
        return;
      }

      listEl.innerHTML = '<div class="analysis-history-empty">분석 내역을 불러오는 중입니다.</div>';

      try {
        const response = await fetch(config.endpoint(projectId), { headers: { Accept: 'application/json' } });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        render(await response.json(), projectId);
      } catch (error) {
        console.error(error);
        countEl.textContent = '0개';
        listEl.innerHTML = '<div class="analysis-history-empty">분석 내역을 불러오지 못했습니다.</div>';
      }
    }

    return {
      load,
      clear: function () {
        load(null);
      }
    };
  }

  window.AnalysisHistoryPanel = { create };
})();
