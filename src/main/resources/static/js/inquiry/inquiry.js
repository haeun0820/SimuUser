(function () {
  let currentUserKey = "guest";
  let inquiries = [];
  let searchQuery = "";
  let currentFilter = "all";

  const tableBody = document.getElementById("inquiryTableBody");
  const countDisplay = document.getElementById("inquiryCount");
  const emptyState = document.getElementById("emptyState");
  const searchInput = document.getElementById("inquirySearchInput");
  const formModal = document.getElementById("inquiryModal");
  const detailModal = document.getElementById("detailModal");
  const inquiryForm = document.getElementById("inquiryForm");

  function storageKey() {
    return `simuuser:inquiries:${currentUserKey}`;
  }

  function escapeHtml(value) {
    return String(value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function formatDate(value) {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return "";
    }
    return new Intl.DateTimeFormat("ko-KR", {
      year: "numeric",
      month: "numeric",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit"
    }).format(date);
  }

  function statusLabel(status) {
    return status === "answered" ? "답변완료" : "미답변";
  }

  function filteredInquiries() {
    return inquiries.filter((item) => {
      const matchesStatus = currentFilter === "all" || item.status === currentFilter;
      const haystack = `${item.title} ${item.category} ${item.content}`.toLowerCase();
      const matchesSearch = !searchQuery || haystack.includes(searchQuery);
      return matchesStatus && matchesSearch;
    });
  }

  function render() {
    const list = filteredInquiries().sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    countDisplay.textContent = list.length;
    emptyState.style.display = list.length ? "none" : "block";
    tableBody.innerHTML = list.map((item, index) => {
      const statusClass = item.status === "answered" ? "status-answered" : "status-waiting";
      return `
        <tr data-id="${escapeHtml(item.id)}">
          <td class="col-no">${list.length - index}</td>
          <td class="col-title">${escapeHtml(item.title)}</td>
          <td class="col-category">${escapeHtml(item.category)}</td>
          <td class="col-date">${formatDate(item.createdAt)}</td>
          <td class="col-status"><span class="status-badge ${statusClass}">${statusLabel(item.status)}</span></td>
        </tr>`;
    }).join("");
  }

  function save() {
    localStorage.setItem(storageKey(), JSON.stringify(inquiries));
  }

  function load() {
    const saved = localStorage.getItem(storageKey());
    inquiries = saved ? JSON.parse(saved) : [];
  }

  function openModal(modal) {
    modal.hidden = false;
    document.body.style.overflow = "hidden";
  }

  function closeModal(modal) {
    modal.hidden = true;
    if (formModal.hidden && detailModal.hidden) {
      document.body.style.overflow = "";
    }
  }

  function showDetail(id) {
    const item = inquiries.find((inquiry) => inquiry.id === id);
    if (!item) {
      return;
    }

    const detailStatus = document.getElementById("detailStatus");
    detailStatus.className = `status-badge ${item.status === "answered" ? "status-answered" : "status-waiting"}`;
    detailStatus.textContent = statusLabel(item.status);
    document.getElementById("detailTitle").textContent = item.title;
    document.getElementById("detailMeta").textContent = `${item.category} · ${formatDate(item.createdAt)}`;
    document.getElementById("detailContent").textContent = item.content;

    const answerBox = document.getElementById("answerBox");
    answerBox.textContent = item.answer || "아직 관리자 답변이 등록되지 않았습니다.";
    openModal(detailModal);
  }

  async function initUser() {
    try {
      const response = await fetch("/api/me");
      if (response.ok) {
        const user = await response.json();
        currentUserKey = user.email || user.userId || user.name || "guest";
      }
    } catch (error) {
      currentUserKey = "guest";
    }
  }

  document.getElementById("openInquiryForm").addEventListener("click", () => openModal(formModal));
  document.getElementById("closeInquiryForm").addEventListener("click", () => closeModal(formModal));
  document.getElementById("cancelInquiryForm").addEventListener("click", () => closeModal(formModal));
  document.getElementById("closeDetail").addEventListener("click", () => closeModal(detailModal));

  formModal.addEventListener("click", (event) => {
    if (event.target === formModal) {
      closeModal(formModal);
    }
  });

  detailModal.addEventListener("click", (event) => {
    if (event.target === detailModal) {
      closeModal(detailModal);
    }
  });

  inquiryForm.addEventListener("submit", (event) => {
    event.preventDefault();
    const category = document.getElementById("categoryInput").value;
    const title = document.getElementById("titleInput").value.trim();
    const content = document.getElementById("contentInput").value.trim();

    if (!title || !content) {
      alert("제목과 문의 내용을 입력해주세요.");
      return;
    }

    inquiries.unshift({
      id: `inquiry-${Date.now()}`,
      category,
      title,
      content,
      status: "waiting",
      answer: "",
      createdAt: new Date().toISOString()
    });
    save();
    inquiryForm.reset();
    closeModal(formModal);
    render();
  });

  searchInput.addEventListener("input", (event) => {
    searchQuery = event.target.value.toLowerCase().trim();
    render();
  });

  document.querySelectorAll(".filter-tab").forEach((button) => {
    button.addEventListener("click", () => {
      document.querySelectorAll(".filter-tab").forEach((item) => item.classList.remove("active"));
      button.classList.add("active");
      currentFilter = button.dataset.filter;
      render();
    });
  });

  tableBody.addEventListener("click", (event) => {
    const row = event.target.closest("tr");
    if (row) {
      showDetail(row.dataset.id);
    }
  });

  initUser().then(() => {
    load();
    render();
  });
})();
