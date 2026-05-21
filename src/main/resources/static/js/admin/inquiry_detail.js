document.addEventListener("DOMContentLoaded", function () {
    const detailCard = document.querySelector('.detail-card');
    const inquiryId = detailCard ? detailCard.dataset.inquiryId : '';
    const submitBtn = document.getElementById('submitReplyBtn');
    const replyTextarea = document.getElementById('replyContent');

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

    function csrfHeaders() {
        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content')
            || document.getElementById('csrfToken')?.value;
        const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content')
            || document.getElementById('csrfHeader')?.value;
        return token && header ? { [header]: token } : {};
    }

    function applyDetail(inquiry) {
        const isAnswered = inquiry.status === 'answered';
        const status = document.getElementById('detailStatus');

        status.textContent = isAnswered ? '답변완료' : '미답변';
        status.className = `status-badge ${isAnswered ? 'status-answered' : 'status-unread'}`;
        document.getElementById('detailTitle').textContent = inquiry.title || '';
        document.getElementById('detailAuthor').innerHTML =
            `<span style="font-weight: 600; color: #475569;">작성자:</span> ${inquiry.authorName || '-'} (${inquiry.authorEmail || '-'})`;
        document.getElementById('detailDate').innerHTML =
            `<span style="font-weight: 600; color: #475569;">등록일시:</span> ${formatDate(inquiry.createdAt)}`;
        document.getElementById('detailContent').textContent = inquiry.content || '';

        if (isAnswered) {
            replyTextarea.value = inquiry.answer || '';
            submitBtn.textContent = '답변 수정';
        }
    }

    async function loadDetail() {
        if (!inquiryId) {
            alert('문의 번호를 확인할 수 없습니다.');
            window.location.href = '/admin/inquiry';
            return;
        }

        try {
            const response = await fetch(`/api/admin/inquiries/${inquiryId}`);
            if (!response.ok) {
                throw new Error('문의 상세를 불러오지 못했습니다.');
            }
            applyDetail(await response.json());
        } catch (error) {
            console.error(error);
            alert('문의 상세를 불러오지 못했습니다.');
            window.location.href = '/admin/inquiry';
        }
    }

    if (submitBtn) {
        submitBtn.addEventListener('click', async function () {
            const answer = replyTextarea.value.trim();

            if (answer === '') {
                alert("답변 내용을 입력해주세요.");
                replyTextarea.focus();
                return;
            }

            if (!confirm("답변을 등록하시겠습니까?")) {
                return;
            }

            try {
                const response = await fetch(`/api/admin/inquiries/${inquiryId}/answer`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                        ...csrfHeaders()
                    },
                    body: JSON.stringify({ answer })
                });

                if (!response.ok) {
                    const error = await response.json().catch(() => ({}));
                    throw new Error(error.message || '답변 등록에 실패했습니다.');
                }

                alert("성공적으로 답변이 등록되었습니다.");
                window.location.href = '/admin/inquiry';
            } catch (error) {
                alert(error.message);
            }
        });
    }

    loadDetail();
});
