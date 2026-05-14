document.addEventListener("DOMContentLoaded", function() {
    
    const tabs = document.querySelectorAll('.prompt-tab');
    const cards = document.querySelectorAll('.prompt-card');
    const searchInput = document.getElementById('promptSearchInput');

    let currentFilter = 'all';
    let searchQuery = '';

    // 1. 카테고리 탭 필터링 이벤트
    tabs.forEach(tab => {
        tab.addEventListener('click', function() {
            // 탭 활성화 상태 변경
            tabs.forEach(t => t.classList.remove('active'));
            this.classList.add('active');

            // 선택된 카테고리 저장 (data-filter 속성값)
            currentFilter = this.getAttribute('data-filter');
            
            // 필터 적용 함수 호출
            applyFilters();
        });
    });

    // 2. 실시간 검색 이벤트
    searchInput.addEventListener('input', function(e) {
        searchQuery = e.target.value.toLowerCase().trim();
        applyFilters();
    });

    // 3. 카드 필터링 처리 함수
    function applyFilters() {
        cards.forEach(card => {
            const category = card.getAttribute('data-category');
            
            // 카드 내부의 텍스트들 (제목, 본문, 뱃지 텍스트 등) 추출하여 검색에 활용
            const textContent = card.innerText.toLowerCase();
            
            // 조건 검사: 카테고리가 일치(또는 전체)하고, 검색어가 포함되어 있는지 확인
            const matchesCategory = (currentFilter === 'all' || currentFilter === category);
            const matchesSearch = (searchQuery === '' || textContent.includes(searchQuery));

            if (matchesCategory && matchesSearch) {
                card.style.display = 'flex'; // 조건에 맞으면 표시
            } else {
                card.style.display = 'none'; // 조건에 안 맞으면 숨김
            }
        });
    }

    // -----------------------------------------
    // 모달(팝업) 열기 및 닫기 로직
    // -----------------------------------------
    const createBtn = document.querySelector('.btn-create'); // 새 프롬프트 생성 버튼
    const promptModal = document.getElementById('promptModal'); // 모달 창 전체
    const closeBtn = document.getElementById('closePromptModal'); // X 닫기 버튼

    // 1. 생성 버튼 클릭 시 모달 열기
    if (createBtn) {
        createBtn.addEventListener('click', function() {
            promptModal.classList.add('active');
        });
    }

    // 2. X 버튼 클릭 시 모달 닫기
    if (closeBtn) {
        closeBtn.addEventListener('click', function() {
            promptModal.classList.remove('active');
        });
    }

    // 3. 모달의 어두운 배경(오버레이) 클릭 시 창 닫기
    window.addEventListener('click', function(e) {
        if (e.target === promptModal) {
            promptModal.classList.remove('active');
        }
    });
});
