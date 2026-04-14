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

    // 문서 생성/수정 확인 버튼
    btnSubmit.onclick = function() {
        const title = document.getElementById('newDocTitle').value;
        const desc = document.getElementById('newDocDesc').value;

        if(!title) return alert("제목을 입력해주세요.");

        if (editMode) {
            const docIdx = generatedDocs.findIndex(d => d.id === editId);
            if (docIdx > -1) {
                generatedDocs[docIdx].title = title;
                generatedDocs[docIdx].description = desc;
                generatedDocs[docIdx].type = currentDocType;
            }
        } else {
            const newDoc = {
                id: Date.now(),
                title: title,
                description: desc,
                content: "AI가 생성할 문서 내용입니다.", 
                type: currentDocType,
                date: new Date().toLocaleDateString(),
                author: '정유나'
            };
            generatedDocs.unshift(newDoc);
        }

        renderDocuments();
        createModal.style.display = 'none';

        const openEditorBtn = document.getElementById('btnOpenEditor');
        if (openEditorBtn) {
            openEditorBtn.onclick = () => {
                if (currentDocId) {
                    openDocEditor(currentDocId);
                }
            };
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

    // 탭 필터링 (전체/기획서/보고서/분석결과)
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
}

/* ── 문서 리스트 렌더링 ── */
function renderDocuments(filter = 'all') {
    const container = document.getElementById('documentListContainer');
    if (!container) return;
    
    const filtered = filter === 'all' ? generatedDocs : generatedDocs.filter(d => d.type === filter);

    if (generatedDocs.length === 0) return;

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

    currentDocId = id; // 전역 변수에 현재 문서 ID 저장

    document.getElementById('viewTitle').innerText = doc.title;
    document.getElementById('viewTypeTag').innerText = doc.type;
    document.getElementById('viewTypeTag').className = `type-tag ${getTypeClass(doc.type)}`;
    document.getElementById('viewDescription').innerText = doc.content || "내용 생성 중입니다...";
    
    document.getElementById('detailModal').style.display = 'flex';
}

// document.js 내부
function openDocEditor(id) {
    const doc = generatedDocs.find(d => d.id === id);
    if (!doc) return;

    // localStorage에 저장 (에디터 페이지에서 꺼내 쓰기용)
    localStorage.setItem('currentEditDoc', JSON.stringify(doc));
    
    // 스프링 부트가 관리하는 경로로 새 창 열기 (ID 포함)
    window.open('/document/editor?id=' + id, '_blank');
}

// 상세보기 팝업의 '문서 열기' 버튼 이벤트 바인딩 예시
// HTML에 id="btnOpenEditor"가 있다고 가정
// document.getElementById('btnOpenEditor').onclick = () => openDocEditor(currentDocId);

/* ── 삭제 기능 ── */
function deleteDoc(id) {
    if (confirm("문서를 삭제하시겠습니까?")) {
        generatedDocs = generatedDocs.filter(d => d.id !== id);
        if (generatedDocs.length === 0) {
            location.reload(); 
        } else {
            renderDocuments();
        }
    }
}