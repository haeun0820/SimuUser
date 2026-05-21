document.addEventListener("DOMContentLoaded", function () {
    const searchInput = document.getElementById('inquirySearchInput');
    const filterUnreadCheckbox = document.getElementById('filterUnread');
    const tableBody = document.getElementById('inquiryTableBody');
    const countDisplay = document.getElementById('inquiryCount');

    let inquiries = [];
    let searchQuery = '';
    let showOnlyUnread = false;

    function escapeHtml(value) {
        return String(value || '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function formatDate(value) {
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return '';
        }
        return new Intl.DateTimeFormat('ko-KR', {
            year: 'numeric',
            month: 'numeric',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        }).format(date);
    }

    function statusLabel(status) {
        return status === 'answered' ? '답변완료' : '미답변';
    }

    function render() {
        const filtered = inquiries.filter(item => {
            const title = (item.title || '').toLowerCase();
            const author = `${item.authorName || ''} ${item.authorEmail || ''}`.toLowerCase();
            const matchesSearch = !searchQuery || title.includes(searchQuery) || author.includes(searchQuery);
            const matchesStatus = !showOnlyUnread || item.status !== 'answered';
            return matchesSearch && matchesStatus;
        });

        countDisplay.textContent = filtered.length;
        tableBody.innerHTML = filtered.map((item, index) => {
            const statusClass = item.status === 'answered' ? 'status-answered' : 'status-unread';
            const author = item.authorEmail
                ? `${escapeHtml(item.authorName)}<br><span style="font-size:12px;color:#94a3b8;">${escapeHtml(item.authorEmail)}</span>`
                : escapeHtml(item.authorName);

            return `
                <tr class="inquiry-row" data-status="${escapeHtml(item.status)}" data-id="${item.id}" style="cursor: pointer;">
                    <td class="col-no">${filtered.length - index}</td>
                    <td class="col-title">${escapeHtml(item.title)}</td>
                    <td class="col-author">${author}</td>
                    <td class="col-date">${formatDate(item.createdAt)}</td>
                    <td class="col-status"><span class="status-badge ${statusClass}">${statusLabel(item.status)}</span></td>
                </tr>`;
        }).join('');
    }

    async function loadInquiries() {
        try {
            const response = await fetch('/api/admin/inquiries');
            if (!response.ok) {
                throw new Error('문의 목록을 불러오지 못했습니다.');
            }
            inquiries = await response.json();
        } catch (error) {
            console.error(error);
            inquiries = [];
        }
        render();
    }

    filterUnreadCheckbox.addEventListener('change', function (e) {
        showOnlyUnread = e.target.checked;
        render();
    });

    searchInput.addEventListener('input', function (e) {
        searchQuery = e.target.value.toLowerCase().trim();
        render();
    });

    tableBody.addEventListener('click', function (event) {
        const row = event.target.closest('.inquiry-row');
        if (!row) {
            return;
        }
        window.location.href = `/admin/inquiry/detail/${row.dataset.id}`;
    });

    loadInquiries();
});
