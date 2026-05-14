document.addEventListener("DOMContentLoaded", function() {
    
    const filterBtns = document.querySelectorAll('.log-filter-btn');
    const searchInput = document.getElementById('logSearchInput');
    const logRows = document.querySelectorAll('.log-row');

    let currentFilter = 'all';
    let searchQuery = '';

    // 1. 필터 탭 클릭 이벤트
    filterBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            // 모든 버튼에서 active 제거 후 클릭한 버튼에만 추가
            filterBtns.forEach(b => b.classList.remove('active'));
            this.classList.add('active');

            // data-filter 속성값 저장
            currentFilter = this.getAttribute('data-filter');
            applyFilters();
        });
    });

    // 2. 검색창 입력 이벤트
    searchInput.addEventListener('input', function(e) {
        searchQuery = e.target.value.toLowerCase().trim();
        applyFilters();
    });

    // 3. 테이블 필터링 적용 함수
    function applyFilters() {
        logRows.forEach(row => {
            const type = row.getAttribute('data-type');
            // 행 안에 있는 모든 텍스트(타입, 메시지, 시간)를 소문자로 가져와 검색에 활용
            const textContent = row.innerText.toLowerCase(); 

            // 카테고리 일치 여부 확인
            const matchesFilter = (currentFilter === 'all' || currentFilter === type);
            // 검색어 포함 여부 확인
            const matchesSearch = (searchQuery === '' || textContent.includes(searchQuery));

            // 두 조건을 모두 만족하면 표시, 아니면 숨김 처리
            if (matchesFilter && matchesSearch) {
                row.style.display = '';
            } else {
                row.style.display = 'none';
            }
        });
    }
});