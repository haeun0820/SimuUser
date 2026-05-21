(function () {
  let inquiries = [];
  let searchQuery = "";
  let currentFilter = "all";
  let currentPage = 1;
  let editingInquiryId = null;
  let selectedInquiryId = null;
  const PAGE_SIZE = 5;

  const tableBody = document.getElementById("inquiryTableBody");
  const countDisplay = document.getElementById("inquiryCount");
  const emptyState = document.getElementById("emptyState");
  const pagination = document.getElementById("pagination");
  const searchInput = document.getElementById("inquirySearchInput");
  const formModal = document.getElementById("inquiryModal");
  const detailModal = document.getElementById("detailModal");
  const inquiryForm = document.getElementById("inquiryForm");
  const submitInquiryBtn = document.getElementById("submitInquiryBtn");
  const editInquiryBtn = document.getElementById("editInquiryBtn");
  const deleteInquiryBtn = document.getElementById("deleteInquiryBtn");
  const detailActions = document.getElementById("detailActions");
  inquiryForm.noValidate = true;

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

  function csrfHeaders() {
    const token = document.querySelector('meta[name="_csrf"]')?.getAttribute("content")
      || document.getElementById("csrfToken")?.value;
    const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute("content")
      || document.getElementById("csrfHeader")?.value;
    return token && header ? { [header]: token } : {};
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
    const totalPages = Math.ceil(list.length / PAGE_SIZE);

    if (currentPage > totalPages) {
      currentPage = Math.max(totalPages, 1);
    }

    const startIndex = (currentPage - 1) * PAGE_SIZE;
    const pageItems = list.slice(startIndex, startIndex + PAGE_SIZE);

    countDisplay.textContent = list.length;
    emptyState.style.display = list.length ? "none" : "flex";
    tableBody.innerHTML = pageItems.map((item, index) => {
      const statusClass = item.status === "answered" ? "status-answered" : "status-waiting";
      return `
        <tr data-id="${escapeHtml(item.id)}">
          <td class="col-no">${list.length - startIndex - index}</td>
          <td class="col-title">${escapeHtml(item.title)}</td>
          <td class="col-category">${escapeHtml(item.category)}</td>
          <td class="col-date">${formatDate(item.createdAt)}</td>
          <td class="col-status"><span class="status-badge ${statusClass}">${statusLabel(item.status)}</span></td>
        </tr>`;
    }).join("");
    renderPagination(totalPages);
  }

  function renderPagination(totalPages) {
    if (totalPages <= 1) {
      pagination.innerHTML = "";
      return;
    }

    pagination.innerHTML = Array.from({ length: totalPages }, (_, index) => {
      const page = index + 1;
      const activeClass = page === currentPage ? " active" : "";
      return `<button class="page-btn${activeClass}" type="button" data-page="${page}">${page}</button>`;
    }).join("");
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

  function validateInquiryForm(category, title, content) {
    const missing = [];
    if (!category) missing.push("문의 유형");
    if (!title) missing.push("제목");
    if (!content) missing.push("문의 내용");

    if (missing.length) {
      alert(`${missing.join(", ")}을(를) 입력해주세요.`);
      return false;
    }
    return true;
  }

  function openCreateForm() {
    editingInquiryId = null;
    inquiryForm.reset();
    document.getElementById("modalTitle").textContent = "문의하기";
    submitInquiryBtn.textContent = "등록";
    openModal(formModal);
  }

  function openEditForm(item) {
    editingInquiryId = item.id;
    document.getElementById("modalTitle").textContent = "문의 수정";
    document.getElementById("categoryInput").value = item.category || "";
    document.getElementById("titleInput").value = item.title || "";
    document.getElementById("contentInput").value = item.content || "";
    submitInquiryBtn.textContent = "수정";
    closeModal(detailModal);
    openModal(formModal);
  }

  function showDetail(id) {
    const item = inquiries.find((inquiry) => String(inquiry.id) === String(id));
    if (!item) {
      return;
    }
    selectedInquiryId = item.id;

    const detailStatus = document.getElementById("detailStatus");
    detailStatus.className = `status-badge ${item.status === "answered" ? "status-answered" : "status-waiting"}`;
    detailStatus.textContent = statusLabel(item.status);
    document.getElementById("detailTitle").textContent = item.title;
    document.getElementById("detailMeta").textContent = `${item.category} · ${formatDate(item.createdAt)}`;
    document.getElementById("detailContent").textContent = item.content;

    const answerBox = document.getElementById("answerBox");
    answerBox.textContent = item.answer || "아직 관리자 답변이 등록되지 않았습니다.";
    detailActions.style.display = item.status === "answered" ? "none" : "flex";
    openModal(detailModal);
  }

  async function loadInquiries() {
    try {
      const response = await fetch("/api/inquiries");
      if (response.ok) {
        inquiries = await response.json();
      } else {
        inquiries = [];
      }
    } catch (error) {
      console.error(error);
      inquiries = [];
    }
    render();
  }

  document.getElementById("openInquiryForm").addEventListener("click", openCreateForm);
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

  inquiryForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const category = document.getElementById("categoryInput").value;
    const title = document.getElementById("titleInput").value.trim();
    const content = document.getElementById("contentInput").value.trim();

    if (!validateInquiryForm(category, title, content)) {
      return;
    }

    try {
      const response = await fetch(editingInquiryId ? `/api/inquiries/${editingInquiryId}` : "/api/inquiries", {
        method: editingInquiryId ? "PUT" : "POST",
        headers: {
          "Content-Type": "application/json",
          ...csrfHeaders()
        },
        body: JSON.stringify({ category, title, content })
      });

      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || "문의 등록에 실패했습니다.");
      }

      inquiryForm.reset();
      closeModal(formModal);
      editingInquiryId = null;
      currentPage = 1;
      await loadInquiries();
    } catch (error) {
      alert(error.message);
    }
  });

  searchInput.addEventListener("input", (event) => {
    searchQuery = event.target.value.toLowerCase().trim();
    currentPage = 1;
    render();
  });

  document.querySelectorAll(".filter-tab").forEach((button) => {
    button.addEventListener("click", () => {
      document.querySelectorAll(".filter-tab").forEach((item) => item.classList.remove("active"));
      button.classList.add("active");
      currentFilter = button.dataset.filter;
      currentPage = 1;
      render();
    });
  });

  pagination.addEventListener("click", (event) => {
    const button = event.target.closest(".page-btn");
    if (!button) {
      return;
    }
    currentPage = Number(button.dataset.page) || 1;
    render();
  });

  tableBody.addEventListener("click", (event) => {
    const row = event.target.closest("tr");
    if (row) {
      showDetail(row.dataset.id);
    }
  });

  editInquiryBtn.addEventListener("click", () => {
    const item = inquiries.find((inquiry) => String(inquiry.id) === String(selectedInquiryId));
    if (!item) return;
    if (item.status === "answered") {
      alert("답변이 완료된 문의는 수정할 수 없습니다.");
      return;
    }
    openEditForm(item);
  });

  deleteInquiryBtn.addEventListener("click", async () => {
    const item = inquiries.find((inquiry) => String(inquiry.id) === String(selectedInquiryId));
    if (!item) return;
    if (item.status === "answered") {
      alert("답변이 완료된 문의는 삭제할 수 없습니다.");
      return;
    }
    if (!confirm("이 문의를 삭제하시겠습니까?")) {
      return;
    }

    try {
      const response = await fetch(`/api/inquiries/${item.id}`, {
        method: "DELETE",
        headers: csrfHeaders()
      });

      if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.message || "문의 삭제에 실패했습니다.");
      }

      closeModal(detailModal);
      selectedInquiryId = null;
      await loadInquiries();
    } catch (error) {
      alert(error.message);
    }
  });

  loadInquiries();
})();
