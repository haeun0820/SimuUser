document.addEventListener("DOMContentLoaded", function() {
    const submitBtn = document.getElementById('submitReplyBtn');
    const replyTextarea = document.getElementById('replyContent');

    if (submitBtn) {
        submitBtn.addEventListener('click', function() {
            const content = replyTextarea.value.trim();

            if (content === '') {
                alert("답변 내용을 입력해주세요.");
                replyTextarea.focus();
                return;
            }

            // 실제 개발 시 여기에서 fetch/AJAX로 서버에 데이터를 전송합니다.
            if (confirm("답변을 등록하시겠습니까? 등록 후에는 사용자에게 알림이 전송됩니다.")) {
                
                // 임시 성공 알림 및 목록 페이지로 이동
                alert("성공적으로 답변이 등록되었습니다.");
                
                // 스프링 컨트롤러에서 매핑해둔 목록 페이지 URL로 리다이렉트
                window.location.href = '/admin/inquiry'; 
            }
        });
    }
});