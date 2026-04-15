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

    // --- 1. 데이터 로드 로직 (설명 연동 포함) ---
    if (docId) {
        fetch(`/api/documents/${docId}`)
            .then(res => res.json())
            .then(data => {
                if (data) {
                    // 제목 연동
                    document.getElementById('headerTitle').innerText = data.title;
                    
                    // 설명(Description) 연동 - 이 부분이 핵심입니다!
                    const headerDesc = document.getElementById('headerDesc');
                    if (headerDesc) {
                        headerDesc.innerText = data.description || '설명이 없습니다.';
                    }
                    
                    // 내용 연동
                    editorPage.innerHTML = data.content || "<div><br></div>";

                    // 리사이즈 로직 재적용
                    setTimeout(() => {
                        document.querySelectorAll('.img-resizable-container').forEach(makeResizable);
                    }, 100);
                }
            })
            .catch(err => {
                console.error("데이터 로드 실패:", err);
            });
    }

    // --- 2. 저장 기능 (CSRF 및 PUT 연동) ---
    const btnSave = document.getElementById('btnSave');
    if (btnSave) {
        btnSave.onclick = async function() {
            const title = document.getElementById('headerTitle').innerText;
            const content = editorPage.innerHTML;
            
            const token = getCsrfToken();
            const header = getCsrfHeader();

            if (!token || !header) {
                console.error("CSRF 토큰을 찾을 수 없습니다.");
                alert("보안 토큰 오류가 발생했습니다. 새로고침 후 다시 시도해주세요.");
                return;
            }

            try {
                const response = await fetch(`/api/documents/${docId}`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                        [header]: token
                    },
                    body: JSON.stringify({ title, content })
                });

                if (response.ok) {
                    alert("성공적으로 저장되었습니다!");
                    
                    // 버전 기록 추가
                    const now = new Date();
                    const v = {
                        id: Date.now(),
                        date: `${now.getMonth() + 1}월 ${now.getDate()}일`,
                        time: now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
                        content: content,
                        author: '나'
                    };
                    versionHistory.unshift(v);
                    renderTimeline();
                } else {
                    alert("저장에 실패했습니다.");
                }
            } catch (error) {
                console.error("저장 중 오류 발생:", error);
            }
        };
    }

    // --- 3. 댓글 입력 기능 ---
    if (commentInput) {
        commentInput.addEventListener('keydown', function(e) {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                const text = this.value.trim();
                if (text) {
                    addComment(text);
                    this.value = '';
                }
            }
        });
    }

    function addComment(text) {
        const now = new Date();
        const timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        const commentItem = document.createElement('div');
        commentItem.className = 'comment-item';
        commentItem.innerHTML = `<div class="comment-user">사용자 <span class="comment-date">${timeStr}</span></div><div class="comment-bubble">${text}</div>`;
        commentList.appendChild(commentItem);
        commentList.scrollTop = commentList.scrollHeight;
    }

    // 툴바 버튼 연결
    const toolbarBtns = document.querySelectorAll('.toolbar-group.right .tool-btn');
    if(toolbarBtns[0]) toolbarBtns[0].onclick = () => toggleSidebar('comment');
    if(toolbarBtns[1]) toolbarBtns[1].onclick = () => toggleSidebar('version');

    // 이미지 리사이즈 로직
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

    // 버전 타임라인 렌더링
    function renderTimeline() {
        if(!timelineContainer) return;
        timelineContainer.innerHTML = versionHistory.map(v => `
            <div class="version-item" onclick="restoreVersion(${v.id})">
                <div class="version-info">
                    <span class="version-time">${v.date} ${v.time}</span>
                    <span class="version-author">● ${v.author}</span>
                </div>
            </div>`).join('');
    }

    window.restoreVersion = function(id) {
        const v = versionHistory.find(v => v.id === id);
        if(v && confirm("이 버전으로 복구하시겠습니까?")) {
            editorPage.innerHTML = v.content;
            document.querySelectorAll('.img-resizable-container').forEach(makeResizable);
        }
    };
});