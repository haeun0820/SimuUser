/* ── 상태 관리 변수 ── */
let versionHistory = [];
let currentDocType = '기획서';

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

document.addEventListener('DOMContentLoaded', function() {
    const editorPage = document.getElementById('editorPage');
    const tabsContainer = document.getElementById('tabsContainer');
    const timelineContainer = document.getElementById('timelineContainer');
    const commentList = document.getElementById('commentList');
    const commentInput = document.getElementById('commentInput');
    
    // --- 1. 데이터 로드 로직 ---
    const urlParams = new URLSearchParams(window.location.search);
    const docId = urlParams.get('id');
    const storedData = localStorage.getItem('currentEditDoc');
    
    if (storedData) {
        const docData = JSON.parse(storedData);
        if (docData.id == docId) {
            document.getElementById('headerTitle').innerText = docData.title;
            document.getElementById('headerDesc').innerText = docData.description;
            editorPage.innerHTML = docData.content || "<div><br></div>";
            
            setTimeout(() => {
                document.querySelectorAll('.img-resizable-container').forEach(makeResizable);
            }, 100);
        }
    }

    // --- 2. 댓글 입력 기능 (엔터 처리) ---
    if (commentInput) {
        commentInput.addEventListener('keydown', function(e) {
            if (e.key === 'Enter' && !e.shiftKey) { // 쉬프트+엔터는 줄바꿈, 그냥 엔터는 전송
                e.preventDefault();
                const text = this.value.trim();
                if (text) {
                    addComment(text);
                    this.value = ''; // 입력창 초기화
                }
            }
        });
    }

    function addComment(text) {
        const now = new Date();
        const timeStr = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        const dateStr = `${now.getMonth() + 1}월 ${now.getDate()}일`;
        
        const commentItem = document.createElement('div');
        commentItem.className = 'comment-item';
        commentItem.innerHTML = `
            <div class="comment-user">나는윤냥 <span class="comment-date">${timeStr} ${dateStr}</span></div>
            <div class="comment-bubble">${text}</div>
        `;
        
        commentList.appendChild(commentItem);
        commentList.scrollTop = commentList.scrollHeight; // 최하단으로 스크롤
    }

    // --- 3. 툴바 및 사이드바 버튼 연결 ---
    const toolbarBtns = document.querySelectorAll('.toolbar-group.right .tool-btn');
    if(toolbarBtns[0]) toolbarBtns[0].onclick = () => toggleSidebar('comment');
    if(toolbarBtns[1]) toolbarBtns[1].onclick = () => toggleSidebar('version');

    // --- 4. 이미지 삽입 및 리사이즈 로직 ---
    function makeResizable(container) {
        const img = container.querySelector('img');
        const handle = container.querySelector('.resize-handle');
        
        container.onclick = function(e) {
            e.stopPropagation();
            document.querySelectorAll('.img-resizable-container').forEach(c => c.classList.remove('selected'));
            container.classList.add('selected');
        };

        handle.onmousedown = function(e) {
            e.preventDefault();
            e.stopPropagation();
            const startX = e.clientX;
            const startWidth = container.offsetWidth;

            function doResize(e) {
                const newWidth = startWidth + (e.clientX - startX);
                if (newWidth > 50) {
                    container.style.width = newWidth + 'px';
                    img.style.width = '100%';
                }
            }

            function stopResize() {
                window.removeEventListener('mousemove', doResize);
                window.removeEventListener('mouseup', stopResize);
            }

            window.addEventListener('mousemove', doResize);
            window.addEventListener('mouseup', stopResize);
        };
    }

    window.insertImageAction = function() {
        const url = prompt("이미지 URL을 입력하세요:");
        if (url) {
            const container = document.createElement('div');
            container.className = 'img-resizable-container';
            container.contentEditable = "false";
            container.innerHTML = `<img src="${url}" class="resizable-img"><div class="resize-handle"></div>`;
            
            const range = window.getSelection().getRangeAt(0);
            range.insertNode(container);
            makeResizable(container);

            const br = document.createElement('br');
            container.after(br);
        }
    };

    // --- 5. 에디터 폰트 및 포맷 기능 ---
    document.querySelectorAll('.format-btn').forEach(btn => {
        btn.addEventListener('click', function(e) {
            e.preventDefault();
            const command = this.getAttribute('data-command');
            if (command === 'insertImage') insertImageAction();
            else document.execCommand(command, false, null);
            editorPage.focus();
        });
    });

    window.changeFontSize = function(type) {
        const sizeInput = document.getElementById('fontSizeDisplay');
        let currentSize = parseInt(sizeInput.value);
        if(type === 'plus') currentSize += 2;
        else if(type === 'minus' && currentSize > 8) currentSize -= 2;
        sizeInput.value = currentSize;

        const selection = window.getSelection();
        if (selection.rangeCount > 0) {
            const range = selection.getRangeAt(0);
            if (range.collapsed) {
                const span = document.createElement("span");
                span.style.fontSize = currentSize + "px";
                span.innerHTML = "&#8203;"; 
                range.insertNode(span);
                const newRange = document.createRange();
                newRange.setStart(span, 1);
                newRange.collapse(true);
                selection.removeAllRanges();
                selection.addRange(newRange);
            } else {
                document.execCommand('fontSize', false, "1");
                const fonts = editorPage.getElementsByTagName("font");
                for (let f of fonts) {
                    if (f.size === "1") {
                        f.removeAttribute("size");
                        f.style.fontSize = currentSize + "px";
                    }
                }
            }
        }
        editorPage.focus();
    };

    window.changeColor = function(color) {
        document.execCommand('styleWithCSS', false, true);
        document.execCommand('foreColor', false, color);
        document.querySelector('.color-indicator').style.backgroundColor = color;
        editorPage.focus();
    };

    // --- 6. 버전 기록 저장 및 복구 ---
    document.getElementById('btnSave').onclick = function() {
        const now = new Date();
        const v = {
            id: Date.now(),
            date: `${now.getMonth() + 1}월 ${now.getDate()}일`,
            time: now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
            content: editorPage.innerHTML,
            author: '나는윤냥'
        };
        versionHistory.unshift(v);
        renderTimeline();
        alert("저장되었습니다.");
    };

    function renderTimeline() {
        if(!timelineContainer) return;
        timelineContainer.innerHTML = versionHistory.map(v => `
            <div class="version-item" onclick="restoreVersion(${v.id})">
                <div class="version-info">
                    <span class="version-time">${v.date} ${v.time}</span>
                    <span class="version-author">● ${v.author}</span>
                </div>
            </div>
        `).join('');
    }

    window.restoreVersion = function(id) {
        const v = versionHistory.find(v => v.id === id);
        if(v && confirm("이 버전으로 복구하시겠습니까?")) {
            editorPage.innerHTML = v.content;
            document.querySelectorAll('.img-resizable-container').forEach(makeResizable);
        }
    };

    // --- 7. 하단 탭 로직 ---
    function bindTabEvents(tab) {
        tab.onclick = () => {
            document.querySelectorAll('.tab-item').forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
        };
        tab.ondblclick = function() {
            const old = this.innerText;
            const input = document.createElement('input');
            input.className = 'tab-edit-input';
            input.value = old;
            input.onblur = () => { this.innerText = input.value || old; };
            input.onkeydown = (e) => { if(e.key === 'Enter') input.blur(); };
            this.innerText = '';
            this.appendChild(input);
            input.focus();
        };
        tab.oncontextmenu = (e) => {
            e.preventDefault();
            if (confirm("삭제할까요?") && tabsContainer.children.length > 1) {
                tab.remove();
                tabsContainer.children[0].classList.add('active');
            }
        };
    }
    
    const addTabBtn = document.getElementById('btnAddTab');
    if(addTabBtn) {
        addTabBtn.onclick = () => {
            const t = document.createElement('div');
            t.className = 'tab-item';
            t.innerText = `탭 ${tabsContainer.children.length + 1}`;
            bindTabEvents(t);
            tabsContainer.appendChild(t);
            t.click();
        };
    }
    document.querySelectorAll('.tab-item').forEach(bindTabEvents);
});