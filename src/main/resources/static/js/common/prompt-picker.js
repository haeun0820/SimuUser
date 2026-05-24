(function () {
  const categoryLabels = {
    simulation: "AI 시뮬레이션",
    market: "시장 분석",
    profit: "수익성 분석",
    feedback: "기획 피드백",
    scenario: "시나리오 비교",
    document: "자동 문서화"
  };

  function escapeHtml(value) {
    return String(value || "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  async function initPromptPicker(card) {
    const category = (card.dataset.promptCategory || "").trim().toLowerCase();
    const select = card.querySelector(".prompt-select");
    const helper = card.querySelector(".prompt-helper");

    if (!category || !select) {
      return;
    }

    select.innerHTML = '<option value="">기본 프롬프트 사용</option>';

    try {
      const response = await fetch(`/api/prompts?category=${encodeURIComponent(category)}`, {
        credentials: "same-origin"
      });
      if (!response.ok) {
        throw new Error("prompt load failed");
      }

      const prompts = await response.json();
      prompts.forEach(prompt => {
        const option = document.createElement("option");
        option.value = prompt.id;
        option.textContent = `${prompt.name} (${prompt.model || "gemini-2.5-flash"})`;
        select.appendChild(option);
      });

      if (helper) {
        helper.textContent = prompts.length
          ? `${categoryLabels[category] || category} 프롬프트 ${prompts.length}개 중에서 선택할 수 있습니다.`
          : "등록된 프롬프트가 없어 기본 프롬프트로 진행합니다.";
      }
    } catch (error) {
      if (helper) {
        helper.textContent = "프롬프트를 불러오지 못해 기본 프롬프트로 진행합니다.";
      }
    }
  }

  window.getSelectedPromptId = function () {
    const select = document.querySelector(".prompt-select");
    return select && select.value ? select.value : "";
  };

  window.renderPromptPicker = function (category) {
    return `
      <section class="prompt-select-card unified-prompt-picker" data-prompt-category="${escapeHtml(category)}">
        <div class="prompt-select-header">
          <div>
            <h2 class="prompt-title">프롬프트 선택</h2>
            <p class="prompt-helper">프롬프트를 불러오는 중입니다.</p>
          </div>
          <select class="prompt-select" name="promptId" aria-label="프롬프트 선택">
            <option value="">기본 프롬프트 사용</option>
          </select>
        </div>
      </section>`;
  };

  document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll(".unified-prompt-picker").forEach(initPromptPicker);
  });
})();
