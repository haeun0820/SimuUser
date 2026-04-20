/* ── 상태 관리 변수 ── */
let versionHistory = [];
let currentDocType = '기획서';

let tabs = [{ id: Date.now(), title: '탭 1', content: '<div><br></div>' }];
let activeTabId = tabs[0].id;

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

    
    const tabsContainer = document.getElementById('tabsContainer');
    const btnAddTab = document.getElementById('btnAddTab');

    /* --- [A] 탭 시스템 로직 --- */
    function renderTabs() {
        if (!tabsContainer) return;
        tabsContainer.innerHTML = '';
        tabs.forEach(tab => {
            const tabEl = document.createElement('div');
            tabEl.className = `tab-item ${tab.id === activeTabId ? 'active' : ''}`;
            tabEl.innerText = tab.title;

            // 클릭: 전환 / 더블클릭: 수정 / 우클릭: 삭제
            tabEl.onclick = () => switchTab(tab.id);
            tabEl.ondblclick = (e) => { e.stopPropagation(); renameTab(tab.id); };
            tabEl.oncontextmenu = (e) => {
                e.preventDefault();
                if (confirm(`'${tab.title}' 탭을 삭제하시겠습니까?`)) deleteTab(tab.id);
            };
            tabsContainer.appendChild(tabEl);
        });
    }

    function switchTab(id) {
        const currentTab = tabs.find(t => t.id === activeTabId);
        if (currentTab) currentTab.content = editorPage.innerHTML;

        activeTabId = id;
        const nextTab = tabs.find(t => t.id === activeTabId);
        editorPage.innerHTML = nextTab.content;
        renderTabs();
    }

    function renameTab(id) {
        const tab = tabs.find(t => t.id === id);
        const newTitle = prompt('새 탭 이름을 입력하세요:', tab.title);
        if (newTitle && newTitle.trim()) {
            tab.title = newTitle;
            renderTabs();
        }
    }

    function deleteTab(id) {
        if (tabs.length <= 1) return alert("최소 하나의 탭은 있어야 합니다.");
        const index = tabs.findIndex(t => t.id === id);
        tabs = tabs.filter(t => t.id !== id);
        if (activeTabId === id) {
            const nextTab = tabs[index] || tabs[tabs.length - 1];
            activeTabId = nextTab.id;
            editorPage.innerHTML = nextTab.content;
        }
        renderTabs();
    }

    if (btnAddTab) {
        btnAddTab.onclick = () => {
            const newId = Date.now();
            tabs.push({ id: newId, title: `탭 ${tabs.length + 1}`, content: '<div><br></div>' });
            switchTab(newId);
        };
    }

    // 이미지 삽입 버튼을 위한 별도 로직 (예시)
    const imgBtn = document.querySelector('[data-command="insertImage"]');
    imgBtn.addEventListener('click', () => {
        const imgBtn = document.querySelector('[data-command="insertImage"]');
        const imageInput = document.getElementById('imageInput');

// 1. 탭 렌더링 함수
function renderTabs() {
    tabsContainer.innerHTML = '';
    tabs.forEach(tab => {
        const tabEl = document.createElement('div');
        tabEl.className = `tab-item ${tab.id === activeTabId ? 'active' : ''}`;
        tabEl.dataset.id = tab.id;
        tabEl.innerText = tab.title;

        // 클릭: 탭 전환
        tabEl.onclick = () => switchTab(tab.id);

        // 더블클릭: 이름 수정
        tabEl.ondblclick = () => renameTab(tab.id);

        // 우클릭: 삭제 팝업
        tabEl.oncontextmenu = (e) => {
            e.preventDefault();
            if (confirm(`'${tab.title}' 탭을 삭제하시겠습니까?`)) {
                deleteTab(tab.id);
            }
        };

        tabsContainer.appendChild(tabEl);
    });
}

// 2. 탭 전환 (내용 저장 및 교체)
function switchTab(id) {
    // 현재 탭 내용 저장
    const currentTab = tabs.find(t => t.id === activeTabId);
    if (currentTab) currentTab.content = editorPage.innerHTML;

    // 새 탭으로 교체
    activeTabId = id;
    const nextTab = tabs.find(t => t.id === activeTabId);
    editorPage.innerHTML = nextTab.content;

    renderTabs();
}

// 3. 탭 추가 (+)
btnAddTab.onclick = () => {
    const newId = tabs.length > 0 ? Math.max(...tabs.map(t => t.id)) + 1 : 1;
    const newTab = {
        id: newId,
        title: `탭 ${newId}`,
        content: '<div><br></div>'
    };
    tabs.push(newTab);
    switchTab(newId); // 생성 후 바로 이동
};

// 4. 탭 이름 수정
function renameTab(id) {
    const tab = tabs.find(t => t.id === id);
    const newTitle = prompt('새 탭 이름을 입력하세요:', tab.title);
    if (newTitle && newTitle.trim() !== '') {
        tab.title = newTitle;
        renderTabs();
    }
}

// 5. 탭 삭제
function deleteTab(id) {
    if (tabs.length <= 1) {
        alert("최소 하나의 탭은 있어야 합니다.");
        return;
    }
    
    tabs = tabs.filter(t => t.id !== id);
    
    // 삭제한 탭이 활성화된 탭이었다면 첫 번째 탭으로 이동
    if (activeTabId === id) {
        activeTabId = tabs[0].id;
        editorPage.innerHTML = tabs[0].content;
    }
    renderTabs();
}

// 초기 실행
renderTabs();

        // 1. 이미지 버튼 클릭 시 파일 선택창 띄우기
        imgBtn.addEventListener('click', () => imageInput.click());

        // 2. 파일이 선택되면 실행
        imageInput.addEventListener('change', function(e) {
            const file = e.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = function(event) {
                    const imageUrl = event.target.result;
                    insertResizableImage(imageUrl); // 리사이즈 가능한 이미지 삽입
                };
                reader.readAsDataURL(file);
            }
            // 같은 파일을 다시 올릴 수 있도록 초기화
            this.value = ''; 
        });

        // 3. 에디터에 이미지를 감싸는 컨테이너와 함께 삽입
        function insertResizableImage(url) {
            const editorPage = document.getElementById('editorPage');
            
            // 한글/워드처럼 리사이즈 핸들이 포함된 구조
            const container = document.createElement('div');
            container.className = 'img-resizable-container';
            container.contentEditable = "false"; // 컨테이너 자체는 수정 불가 (드래그용)
            container.style.width = '300px'; // 기본 너비

            const img = document.createElement('img');
            img.src = url;
            img.style.width = '100%';

            const handle = document.createElement('div');
            handle.className = 'resize-handle';

            container.appendChild(img);
            container.appendChild(handle);
            
            // 에디터의 현재 커서 위치 혹은 맨 뒤에 삽입
            editorPage.appendChild(container);
            
            // 리사이즈 기능 적용
            makeResizable(container);
        }
    });

    // --- 1. 초기 실행 로직 ---
    if (docId) {
        loadDocumentData().then(() => {
            renderTabs(); 
        });
        loadComments();       // 댓글 불러오기
        loadVersionHistory(); // 버전 기록 불러오기
    } else {
        // 새 문서라면 즉시 탭 1 생성
        renderTabs();
    }

    // 툴바 포맷 버튼 이벤트 리스너
    document.querySelectorAll('.format-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const command = this.getAttribute('data-command');
            
            // execCommand는 현재 선택된 영역에 서식을 적용합니다.
            document.execCommand(command, false, null);
            
            // 포커스를 다시 에디터로 돌려줍니다.
            editorPage.focus();
        });
    });

    // 글자 색상 변경 함수 (전역 window 객체에 연결)
    window.changeColor = function(color) {
        document.execCommand('foreColor', false, color);
        // 버튼 아래 색상 표시 바 업데이트
        const indicator = document.querySelector('.color-indicator');
        if (indicator) indicator.style.backgroundColor = color;
    };

    // 글자 크기 변경 함수 (전역 window 객체에 연결)
    let currentFontSize = 3; // 기본값 (1~7 사이의 값)
    window.changeFontSize = function(type) {
        if (type === 'plus' && currentFontSize < 7) currentFontSize++;
        else if (type === 'minus' && currentFontSize > 1) currentFontSize--;
        
        document.execCommand('fontSize', false, currentFontSize);
        
        // 화면에 현재 크기 표시 (사용자 편의용)
        const display = document.getElementById('fontSizeDisplay');
        if (display) {
            // 실제 px 단위와 브라우저 fontSize(1-7)는 다르므로 단순 매핑 시각화
            const pxMap = [10, 13, 16, 18, 24, 32, 48];
            display.value = pxMap[currentFontSize - 1];
        }
    };

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

        // 클릭 시 선택 표시
    container.addEventListener('click', (e) => {
        e.stopPropagation();
        document.querySelectorAll('.img-resizable-container').forEach(c => c.classList.remove('selected'));
        container.classList.add('selected');
    });

    handle.onmousedown = function(e) {
        e.preventDefault();
        e.stopPropagation();

        const startX = e.clientX;
        const startWidth = parseInt(document.defaultView.getComputedStyle(container).width, 10);

        function doResize(ev) {
            const currentWidth = startWidth + (ev.clientX - startX);
            if (currentWidth > 50) { // 최소 크기 제한
                container.style.width = currentWidth + 'px';
            }
        }

        function stopResize() {
            window.removeEventListener('mousemove', doResize);
            window.removeEventListener('mouseup', stopResize);
        }

        window.addEventListener('mousemove', doResize);
        window.addEventListener('mouseup', stopResize);
    };

        if(!handle) return;
        container.onclick = (e) => {
            e.stopPropagation();
            document.querySelectorAll('.img-resizable-container').forEach(c => c.classList.remove('selected'));
            container.classList.add('selected');
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