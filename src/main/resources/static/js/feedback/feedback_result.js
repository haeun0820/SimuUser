(function () {
  const saveButton = document.getElementById('btnSaveResult');
  const rerunButton = document.getElementById('btnRerunFeedback');
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

  function csrfHeaders() {
    return csrfToken && csrfHeader ? { [csrfHeader]: csrfToken } : {};
  }

  function getContext() {
    return window.feedbackAnalysisContext || { request: {}, result: {} };
  }

  function moveToProjectPlanTab(projectId) {
    window.location.href = `/project/detail/${encodeURIComponent(projectId)}?tab=tab-plan`;
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
      moveToProjectPlanTab(request.projectId);
      return;
    }

    saveButton.disabled = true;

    try {
      const response = await fetch('/feedback/results', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...csrfHeaders()
        },
        body: JSON.stringify({
          projectId: request.projectId,
          sourceType: request.sourceType || 'project',
          sourceContent: request.sourceType === 'file' ? '' : (request.sourceContent || ''),
          result
        })
      });

      const payload = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(payload.message || payload.error || '결과 저장에 실패했습니다.');
      }

      moveToProjectPlanTab(request.projectId);
    } catch (error) {
      alert(error.message || '결과 저장에 실패했습니다.');
    } finally {
      saveButton.disabled = false;
    }
  }

  function rerunAnalysis() {
    const context = getContext();
    const request = context.request || {};
    const draft = {
      projectId: request.projectId || '',
      sourceType: request.sourceType || 'project',
      textContent: request.sourceType === 'text' ? (request.sourceContent || '') : ''
    };

    sessionStorage.setItem('feedbackDraft', JSON.stringify(draft));
    const projectQuery = request.projectId ? `?projectId=${encodeURIComponent(request.projectId)}` : '';
    window.location.href = `/feedback${projectQuery}`;
  }

  if (saveButton) {
    saveButton.addEventListener('click', saveResult);
  }

  if (rerunButton) {
    rerunButton.addEventListener('click', rerunAnalysis);
  }
})();
