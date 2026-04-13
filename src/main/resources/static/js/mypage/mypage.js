document.addEventListener("DOMContentLoaded", function () {
    
    // --- 1. 유저 정보 설정 (소셜 로그인 연동 시나리오) ---
    // 실제 환경에서는 서버에서 넘어온 Thymeleaf 변수나 API를 통해 세팅합니다.
    // 임시 테스트용 데이터 (구글, 네이버 등 연동 이메일 가정)
    const currentUser = {
        nickname: "닉네임",
        email: "homerun@gmail.com" 
    };

    // 화면 상단 및 폼에 유저 정보 바인딩
    document.getElementById("displayNickname").textContent = currentUser.nickname;
    document.getElementById("displayEmail").textContent = currentUser.email;
    document.getElementById("inputNickname").value = currentUser.nickname;
    document.getElementById("inputEmail").value = currentUser.email;


    // --- 2. 탭 전환 로직 ---
    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            // 모든 탭 버튼과 콘텐츠에서 active 제거
            tabBtns.forEach(t => t.classList.remove('active'));
            tabContents.forEach(c => c.classList.remove('active'));

            // 클릭된 탭 활성화
            btn.classList.add('active');
            const targetId = btn.getAttribute('data-target');
            document.getElementById(targetId).classList.add('active');
        });
    });


    // --- 3. 내 프로젝트 렌더링 (all-projects 코드 응용) ---
    const allProjects = JSON.parse(localStorage.getItem('simu_projects') || '[]');
    
    // 배너 영역에 프로젝트 개수 업데이트
    document.getElementById('projectCountLabel').textContent = `${allProjects.length}개 프로젝트`;

    function timeAgo(isoStr) {
        const diff = Date.now() - new Date(isoStr).getTime();
        const min = Math.floor(diff / 60000);
        if (min < 1) return '방금 전';
        if (min < 60) return `${min}분 전`;
        const hr = Math.floor(min / 60);
        if (hr < 24) return `${hr}시간 전`;
        const day = Math.floor(hr / 24);
        return `${day}일 전`;
    }

    function escHtml(str) {
        return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
    }

    function renderProjectList() {
        const container = document.getElementById('myProjectsList');

        if (allProjects.length === 0) {
            container.innerHTML = `<p style="color:#94a3b8; text-align:center; padding: 20px;">프로젝트가 없습니다.</p>`;
            return;
        }

        container.innerHTML = allProjects.map(p => {
            const isCollab = p.type === 'collab';
            const badge = isCollab
                ? '<span class="badge-collab">협업</span>'
                : '<span class="badge-personal">개인</span>';

            const memberInfo = isCollab && p.members && p.members.length > 0
                ? `<span class="item-meta-badge">With. ${p.members.map(escHtml).join(', ')}</span>`
                : '';

            return `
                <div class="list-item" onclick="location.href='/project/${p.id}'">
                    <div class="list-item-content">
                        <div class="item-title-row">
                            <h3 class="item-title">${escHtml(p.title)}</h3>
                            ${badge}
                        </div>
                        <p class="item-desc">${p.description ? escHtml(p.description) : '프로젝트 설명'}</p>
                        <div class="item-meta">
                            <span>
                                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" style="vertical-align: middle;">
                                    <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.8"/>
                                    <path d="M12 7v5l3 3" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                                </svg>
                                ${timeAgo(p.createdAt || Date.now())}
                            </span>
                            ${memberInfo}
                        </div>
                    </div>
                    <div class="item-arrow">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M5 12h14M12 5l7 7-7 7"/>
                        </svg>
                    </div>
                </div>
            `;
        }).join('');
    }

    renderProjectList();


    // --- 4. 분석 기록 렌더링 (더미 데이터 사용 - 필요시 API 연동) ---
    const mockHistory = [
        { title: "AI 시뮬레이션", date: "2026. 4. 2." },
        { title: "AI 시뮬레이션", date: "2026. 4. 1." },
        { title: "AI 시뮬레이션", date: "2026. 4. 1." },
        { title: "시장 분석", date: "2026. 4. 1." }
    ];

    function renderHistoryList() {
        const container = document.getElementById('myHistoryList');
        container.innerHTML = mockHistory.map(h => `
            <div class="list-item">
                <div class="item-title-row" style="gap: 16px;">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#10b981" stroke-width="2">
                        <circle cx="12" cy="12" r="10"></circle>
                        <path d="M9 12l2 2 4-4"></path>
                    </svg>
                    <div style="display:flex; flex-direction:column; gap:4px;">
                        <span style="font-size:14px; font-weight:600; color:#1e293b;">${h.title}</span>
                        <span style="font-size:12px; color:#94a3b8;">${h.date}</span>
                    </div>
                </div>
                <div style="display:flex; align-items:center; gap:16px;">
                    <span class="badge-done">완료</span>
                    <div class="item-arrow">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M5 12h14M12 5l7 7-7 7"/>
                        </svg>
                    </div>
                </div>
            </div>
        `).join('');
    }

    renderHistoryList();


    // --- 5. 프로필 폼 저장 이벤트 ---
    const profileForm = document.getElementById('profileEditForm');
    profileForm.addEventListener('submit', function(e) {
        e.preventDefault();
        const newNickname = document.getElementById('inputNickname').value;
        
        // 여기에 API 통신 로직 추가
        alert(`${newNickname} 님, 프로필이 저장되었습니다.`);
        
        // 화면 반영
        document.getElementById('displayNickname').textContent = newNickname;
    });

});