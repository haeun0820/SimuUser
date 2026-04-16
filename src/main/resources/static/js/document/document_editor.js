/* ── 상태 관리 변수 ── */
let versionHistory = [];
let currentDocType = '기획서';

/* ── CSRF 토큰 헬퍼 ── */
const getCsrfToken = () => document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
const getCsrfHeader = () => document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

/* ── 전역 함수: 사이드바 토글 ── */
window.toggleSidebar = function(type) {
    const rightSidebar = document.getElementById('rightSidebar');
    const commentContent = document.getElementById('commentSidebar');
    const versionContent = document.getElementById('versionSidebar');

    if (!rightSidebar || !commentContent || !versionContent) return;

    if (rightSidebar.classList.contains('active')) {
        const isCommentActive = (type === 'comment' && commentContent.classList.contains('active'));
        const isVersionActive = (type === 'version' && versionContent.classList.contains('active'));
        
        if (isCommentActive || isVersionActive) {
            rightSidebar.classList.remove('active');
            commentContent.classList.remove('active');
            versionContent.classList.remove('active');
            return;
        }
    }

    rightSidebar.classList.add('active');
    if (type === 'comment') {
        commentContent.classList.add('active');
        versionContent.classList.remove('active');
    } else {
        versionContent.classList.add('active');
        commentContent.classList.remove('active');
    }
};

/* ── 메인 로직 ── */
document.addEventListener('DOMContentLoaded', function() {
    const editorPage = document.getElementById('editorPage');
    const timelineContainer = document.getElementById('timelineContainer');
    const commentList = document.getElementById('commentList');
    const commentInput = document.getElementById('commentInput');
    
    const urlParams = new URLSearchParams(window.location.search);
    const docId = urlParams.get('id');

    // --- 1. 초기 실행 로직 ---
    if (docId) {
        loadDocumentData();   // 문서 내용 불러오기
        loadComments();       // 댓글 불러오기
        loadVersionHistory(); // 버전 기록 불러오기
    }

    // [함수 정의] 문서 데이터 불러오기
    async function loadDocumentData() {
        try {
            const res = await fetch(`/api/documents/${docId}`);
            const data = await res.json();
            if (data) {
                document.getElementById('headerTitle').innerText = data.title;
                const headerDesc = document.getElementById('headerDesc');
                if (headerDesc) headerDesc.innerText = data.description || '설명이 없습니다.';
                editorPage.innerHTML = data.content || "<div><br></div>";
                
                // 이미지 리사이즈 적용
                setTimeout(() => {
                    document.querySelectorAll('.img-resizable-container').forEach(makeResizable);
                }, 100);
            }
        } catch (err) { console.error("문서 로드 실패:", err); }
    }

    // [함수 정의] 댓글 불러오기
    async function loadComments() {
        try {
            const res = await fetch(`/api/documents/${docId}/comments`);
            const comments = await res.json();
            commentList.innerHTML = ''; 
            comments.forEach(c => renderCommentUI(c));
        } catch (err) { console.error("댓글 로드 실패:", err); }
    }

    // [함수 정의] 댓글 UI 그리기
    function renderCommentUI(comment) {
    // 이메일 주소에서 @ 앞부분만 추출 (예: example@naver.com -> example)
        const displayName = comment.author.split('@')[0]; 
        const timeStr = new Date(comment.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        
        const commentItem = document.createElement('div');
        commentItem.className = 'comment-item';
        commentItem.innerHTML = `
            <div class="comment-user">${displayName} <span class="comment-date">${timeStr}</span></div>
            <div class="comment-bubble">${comment.content}</div>
        `;
        commentList.appendChild(commentItem);
        commentList.scrollTop = commentList.scrollHeight;
    }

    // [함수 정의] 버전 기록 불러오기
    async function loadVersionHistory() {
        try {
            const res = await fetch(`/api/documents/${docId}/versions`); // 필요 시 API 확인
            if(res.ok) {
                versionHistory = await res.json();
                renderTimeline();
            }
        } catch (err) { console.error("버전 기록 로드 실패:", err); }
    }

    // --- 2. 저장 기능 (서버 연동) ---
    const btnSave = document.getElementById('btnSave');
    if (btnSave) {
        btnSave.onclick = async function() {
            const title = document.getElementById('headerTitle').innerText;
            const content = editorPage.innerHTML;
            const token = getCsrfToken();
            const header = getCsrfHeader();

            if (!token || !header) {
                alert("보안 토큰 오류가 발생했습니다.");
                return;
            }

            try {
                const response = await fetch(`/api/documents/${docId}`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json', [header]: token },
                    body: JSON.stringify({ title, content })
                });

                if (response.ok) {
                    alert("성공적으로 저장되었습니다!");
                    loadVersionHistory(); // 저장 후 목록 새로고침
                }
            } catch (error) { console.error("저장 오류:", error); }
        };
    }

    // --- 3. 댓글 입력 기능 ---
    if (commentInput) {
        commentInput.addEventListener('keydown', async function(e) {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                const text = this.value.trim();
                if (text) {
                    await saveComment(text);
                    this.value = '';
                }
            }
        });
    }

    async function saveComment(text) {
        const token = getCsrfToken();
        const header = getCsrfHeader();
        try {
            const response = await fetch(`/api/documents/${docId}/comments`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', [header]: token },
                body: JSON.stringify({ author: "나는윤냥", content: text })
            });
            if (response.ok) loadComments(); // 다시 불러오기
        } catch (err) { console.error("댓글 저장 실패:", err); }
    }

    // 툴바 버튼 연결
    const toolbarBtns = document.querySelectorAll('.toolbar-group.right .tool-btn');
    if(toolbarBtns[0]) toolbarBtns[0].onclick = () => toggleSidebar('comment');
    if(toolbarBtns[1]) toolbarBtns[1].onclick = () => toggleSidebar('version');

    // 이미지 리사이즈 로직 (기존 유지)
    function makeResizable(container) {
        const img = container.querySelector('img');
        const handle = container.querySelector('.resize-handle');
        if(!handle) return;
        container.onclick = (e) => {
            e.stopPropagation();
            document.querySelectorAll('.img-resizable-container').forEach(c => c.classList.remove('selected'));
            container.classList.add('selected');
        };
        handle.onmousedown = function(e) {
            e.preventDefault(); e.stopPropagation();
            const startX = e.clientX;
            const startWidth = container.offsetWidth;
            function doResize(ev) {
                const newWidth = startWidth + (ev.clientX - startX);
                if (newWidth > 50) container.style.width = newWidth + 'px';
            }
            function stopResize() {
                window.removeEventListener('mousemove', doResize);
                window.removeEventListener('mouseup', stopResize);
            }
            window.addEventListener('mousemove', doResize);
            window.addEventListener('mouseup', stopResize);
        };
    }

    function renderTimeline() {
        if(!timelineContainer) return;
        timelineContainer.innerHTML = versionHistory.length === 0 ? '<p style="padding:10px; color:#94a3b8;">기록이 없습니다.</p>' : 
            versionHistory.map(v => `
            <div class="version-item" onclick="restoreVersion(${v.id})">
                <div class="version-info">
                    <span class="version-time">${new Date(v.createdAt).toLocaleString()}</span>
                    <span class="version-author">● 나</span>
                </div>
            </div>`).join('');
    }

    window.restoreVersion = async function(id) {
    const v = versionHistory.find(v => v.id === id);
    if (v && confirm("이 버전으로 복구하시겠습니까? (새 기록은 생성되지 않습니다)")) {
        
        const title = document.getElementById('headerTitle').innerText;
        const token = getCsrfToken();
        const header = getCsrfHeader();

        try {
            // 일반 저장(/api/documents/${docId})이 아닌 restore 주소로 요청
            const response = await fetch(`/api/documents/${docId}/restore`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json', [header]: token },
                body: JSON.stringify({ title, content: v.content })
            });

            if (response.ok) {
                // 화면 내용 즉시 교체
                document.getElementById('editorPage').innerHTML = v.content;
                alert("복구되었습니다.");
                // 타임라인을 새로 고침하지 않아도 화면 본문은 유지됩니다.
            }
        } catch (error) {
            console.error("복구 실패:", error);
        }
    }
};
});