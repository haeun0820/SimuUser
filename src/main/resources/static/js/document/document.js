/* ── 상태 관리 ── */
let generatedDocs = []; 
let currentDocType = '기획서';
let editMode = false;   // 현재 편집 중인지 확인
let editId = null;      // 편집 중인 문서의 ID

let allProjects = [];
let currentFilter = 'all'; // 프로젝트 필터 (전체/협업/개인)
let selectedProjectId = null;

let currentDocId = null; // 현재 상세보기 중인 문서 ID 저장용

/* ── 메인 실행부 (하나의 리스너로 통합) ── */
document.addEventListener('DOMContentLoaded', function() {
    // 1. 문서 관련 요소 및 이벤트
    const createModal = document.getElementById('createModal');
    const detailModal = document.getElementById('detailModal');
    const modalTitle = createModal.querySelector('.modal-title');
    const btnSubmit = document.getElementById('confirmGenerate');
    
    // 생성 모달 열기 함수
    function openCreateModal(isEdit = false) {
        editMode = isEdit;
        if (!isEdit) {
            modalTitle.innerText = "AI 문서 자동 생성";
            btnSubmit.innerText = "추가";
            document.getElementById('newDocTitle').value = '';
            document.getElementById('newDocDesc').value = '';
            editId = null;
        } else {
            modalTitle.innerText = "문서 정보 수정";
            btnSubmit.innerText = "수정 완료";
        }
        createModal.style.display = 'flex';
    }

    // 문서 관련 클릭 이벤트
    document.getElementById('openCreateModal').onclick = () => openCreateModal(false);
    
    const firstGenBtn = document.getElementById('btnFirstGenerate');
    if (firstGenBtn) firstGenBtn.onclick = () => openCreateModal(false);

    document.getElementById('closeCreateModal').onclick = () => { createModal.style.display = 'none'; };
    document.getElementById('closeDetailModal').onclick = () => { detailModal.style.display = 'none'; };

    // 💡 [핵심] 문서 생성/수정 확인 버튼 (오직 이 함수 하나만 실행됩니다!)
    btnSubmit.onclick = function() {
        const title = document.getElementById('newDocTitle').value;
        const desc = document.getElementById('newDocDesc').value;

        if(!title) return alert("제목을 입력해주세요.");

        if (editMode) {
            // [수동 편집 기능]
            const docIdx = generatedDocs.findIndex(d => d.id === editId);
            if (docIdx > -1) {
                generatedDocs[docIdx].title = title;
                generatedDocs[docIdx].description = desc;
                generatedDocs[docIdx].type = currentDocType;
            }
            renderDocuments();
            createModal.style.display = 'none';

        } else {
            // [AI 자동 생성 기능]
            if (!selectedProjectId) {
                return alert("먼저 왼쪽 목록에서 프로젝트를 선택해주세요.");
            }

            const requestData = {
                projectId: selectedProjectId, 
                documentType: currentDocType,
                title: title,
                description: desc
            };

            const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
            const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

            btnSubmit.innerText = "생성 중...";
            btnSubmit.disabled = true;

            fetch('/api/documents/generate', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [csrfHeader]: csrfToken
                },
                body: JSON.stringify(requestData)
            })
            .then(response => {
                if (!response.ok) {
                    throw new Error(`백엔드 서버 오류가 발생했습니다. (상태 코드: ${response.status})`);
                }
                return response.json();
            })
            .then(data => {
                alert("AI 문서가 성공적으로 생성되었습니다!");
                
                const newDoc = {
                    id: data.id, 
                    title: data.title,
                    description: data.description || desc, 
                    content: data.content,
                    type: currentDocType,
                    date: new Date().toLocaleDateString(),
                    author: 'AI 어시스턴트' 
                };
                generatedDocs.unshift(newDoc); 
                
                renderDocuments(); 
                createModal.style.display = 'none';
            })
            .catch(error => {
                console.error('Error:', error);
                alert(error.message);
            })
            .finally(() => {
                btnSubmit.innerText = "추가";
                btnSubmit.disabled = false;
            });
        }
    };

    // 문서 종류 버튼 선택
    document.querySelectorAll('.type-btn').forEach(btn => {
        btn.onclick = function() {
            document.querySelectorAll('.type-btn').forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            currentDocType = this.dataset.type;
        };
    });

    // 2. 프로젝트 관련 초기 로드 및 필터 이벤트
    loadProjects(); 

    document.querySelectorAll('input[name="projectFilter"]').forEach(radio => {
        radio.addEventListener('change', (e) => {
            currentFilter = e.target.value;
            renderProjects();
        });
    });

    // 탭 필터링
    document.querySelectorAll('.doc-tab').forEach(tab => {
        tab.onclick = function() {
            document.querySelectorAll('.doc-tab').forEach(t => t.classList.remove('active'));
            this.classList.add('active');
            renderDocuments(this.dataset.filter);
        };
    });
}); // DOMContentLoaded 끝

/* ── 헬퍼 함수 (전역 스코프) ── */
function getTypeClass(type) {
    if (type === '기획서') return 'tag-blue';
    if (type === '보고서') return 'tag-purple';
    if (type === '분석결과') return 'tag-green';
    return 'tag-blue';
}

/* ── 프로젝트 데이터 서버에서 가져오기 ── */
async function loadProjects() {
    try {
        const response = await fetch('/api/projects');
        if (!response.ok) throw new Error('프로젝트 로드 실패');
        allProjects = await response.json();
        renderProjects();
    } catch (error) {
        console.error('Error:', error);
        const container = document.getElementById('projectListScroll');
        if (container) {
            container.innerHTML = `<div class="no-projects" style="padding:20px; text-align:center; color:#ef4444;">서버 연결에 실패했습니다.</div>`;
        }
    }
}

/* ── 프로젝트 리스트 렌더링 ── */
function renderProjects() {
    const container = document.getElementById('projectListScroll');
    if (!container) return;

    const filtered = allProjects.filter(p => {
        if (currentFilter === 'all') return true;
        return p.type === currentFilter; 
    });

    if (filtered.length === 0) {
        container.innerHTML = `<div class="no-projects" style="padding:20px; text-align:center; color:#9ca3af;">등록된 프로젝트가 없습니다.</div>`;
        return;
    }

    container.innerHTML = filtered.map(p => {
        const isCollab = p.type === 'collab';
        const badgeClass = isCollab ? 'badge-collab' : 'badge-personal';
        const badgeText = isCollab ? '협업' : '개인';
        
        return `
            <div class="project-item ${selectedProjectId === p.id ? 'selected' : ''}" 
                 data-id="${p.id}" style="margin-bottom:10px; cursor:pointer;">
                <div class="project-item-head" style="display:flex; justify-content:space-between; align-items:center;">
                    <span class="project-item-title" style="font-weight:700;">${p.title}</span>
                    <span class="type-tag ${badgeClass}" style="font-size:11px; padding:2px 6px; border-radius:4px;">${badgeText}</span>
                </div>
                <p class="project-item-desc" style="font-size:13px; color:#6b7280; margin:4px 0;">${p.description || ''}</p>
                <div class="project-item-footer" style="font-size:12px; color:#9ca3af;">
                    <span>${p.createdAt ? new Date(p.createdAt).toLocaleDateString() : '방금 전'}</span>
                </div>
            </div>`;
    }).join('');

    container.querySelectorAll('.project-item').forEach(el => {
        el.onclick = () => {
            const id = parseInt(el.dataset.id);
            selectedProjectId = (selectedProjectId === id) ? null : id;
            updateProjectSelection();
        };
    });
}

function updateProjectSelection() {
    document.querySelectorAll('.project-item').forEach(el => {
        el.classList.toggle('selected', parseInt(el.dataset.id) === selectedProjectId);
    });

    if (selectedProjectId) {
        console.log("👉 선택된 프로젝트 ID:", selectedProjectId); // 로그 확인용
        loadDocumentsForProject(selectedProjectId); // 백엔드에 문서 요청!
    } else {
        generatedDocs = [];
        renderDocuments();
    }
}

async function loadDocumentsForProject(projectId) {
    try {
        // 💡 [CCTV 1] 화면에서 도대체 몇 번 프로젝트 ID를 요청하고 있는지 확인!
        console.log("👉 내가 클릭한 프로젝트 ID:", projectId);

        const response = await fetch(`/api/documents/project/${projectId}`);
        if (!response.ok) throw new Error('문서 목록을 불러오지 못했습니다.');
        
        const data = await response.json();
        
        // 💡 [CCTV 2] 백엔드가 진짜로 빈 깡통을 주는지, 아니면 데이터를 주는지 확인!
        console.log("👉 백엔드에서 보내준 문서 데이터:", data);

        // 백엔드 데이터를 프론트엔드 배열 형식에 맞게 변환
        // 💡 [수정] 백엔드 데이터를 프론트엔드 배열 형식에 맞게 안전하게 변환
        generatedDocs = data.map(doc => {
            let dateStr = '날짜 없음'; // 기본값 (DB에 날짜가 NULL일 경우)
            if (doc.createdAt) {
                if (Array.isArray(doc.createdAt)) {
                    // 자바가 배열 [2026, 4, 30] 형태로 보냈을 때
                    dateStr = new Date(doc.createdAt[0], doc.createdAt[1] - 1, doc.createdAt[2]).toLocaleDateString();
                } else {
                    // 자바가 문자열 "2026-04-30..." 형태로 보냈을 때
                    dateStr = new Date(doc.createdAt).toLocaleDateString();
                }
            }

            return {
                id: doc.id,
                title: doc.title,
                description: doc.description || '설명이 없습니다.', 
                content: doc.content,
                type: doc.type || '기획서', // type이 비어있으면 기본값으로 기획서 표시
                date: dateStr,
                author: 'AI 어시스턴트'
            };
        });
        
        renderDocuments(); // 화면 다시 그리기
    } catch (error) {
        console.error('Error:', error);
    }
}

/* ── 문서 리스트 렌더링 ── */
function renderDocuments(filter = 'all') {
    const container = document.getElementById('documentListContainer');
    if (!container) return;
    
    const filtered = filter === 'all' ? generatedDocs : generatedDocs.filter(d => d.type === filter);

    if (generatedDocs.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">📄</div>
                <p>아직 생성된 문서가 없습니다.<br>문서를 AI를 이용해 자동 생성해보세요</p>
            </div>`;
        return;
    }

    container.innerHTML = filtered.map(doc => `
        <div class="doc-item">
            <div class="doc-info">
                <h4>${doc.title}</h4>
                <p style="font-size:13px; color:#6b7280; margin-bottom:8px;">${doc.description || '설명이 없습니다.'}</p>
                <div class="doc-meta">
                    <span class="type-tag ${getTypeClass(doc.type)}">${doc.type}</span>
                    <span>${doc.date}</span>
                    <span>생성자 : ${doc.author}</span>
                </div>
            </div>
            <div class="doc-actions">
                <button class="btn-view" onclick="showDetail(${doc.id})">상세보기</button>
                <button class="btn-view" onclick="editDoc(${doc.id})">편집</button>
                <button class="btn-icon">📥</button>
                <button class="btn-icon" onclick="deleteDoc(${doc.id})">🗑️</button>
            </div>
        </div>
    `).join('');
}

/* ── 편집 기능 호출 ── */
function editDoc(id) {
    const doc = generatedDocs.find(d => d.id === id);
    if (!doc) return;

    editId = id;
    document.getElementById('newDocTitle').value = doc.title;
    document.getElementById('newDocDesc').value = doc.description;
    
    currentDocType = doc.type;
    document.querySelectorAll('.type-btn').forEach(btn => {
        btn.classList.toggle('active', btn.dataset.type === doc.type);
    });

    document.getElementById('confirmGenerate').innerText = "수정 완료";
    document.querySelector('#createModal .modal-title').innerText = "문서 정보 수정";
    editMode = true;
    document.getElementById('createModal').style.display = 'flex';
}

/* ── 상세보기 팝업 ── */
function showDetail(id) {
    const doc = generatedDocs.find(d => d.id === id);
    if(!doc) return;

    currentDocId = id; 

    document.getElementById('viewTitle').innerText = doc.title;
    document.getElementById('viewTypeTag').innerText = doc.type;
    document.getElementById('viewTypeTag').className = `type-tag ${getTypeClass(doc.type)}`;
    document.getElementById('viewDescription').innerText = doc.content || "내용 생성 중입니다...";
    
    const btnOpenEditor = document.getElementById('btnOpenEditor');
    if (btnOpenEditor) {
        btnOpenEditor.onclick = function() {
            openDocEditor(id); // 새 탭 열기 함수 호출!
        };
    }

    document.getElementById('detailModal').style.display = 'flex';
}

function openDocEditor(id) {
    const doc = generatedDocs.find(d => d.id === id);
    if (!doc) return;

    localStorage.setItem('currentEditDoc', JSON.stringify(doc));
    window.open('/document/editor?id=' + id, '_blank');
}

/* ── 삭제 기능 ── */
function deleteDoc(id) {
    if (confirm("문서를 삭제하시겠습니까?")) {
        generatedDocs = generatedDocs.filter(d => d.id !== id);
        renderDocuments();
    }
}