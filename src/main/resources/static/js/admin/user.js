document.addEventListener("DOMContentLoaded", function() {
    
    const searchInput = document.getElementById('userSearchInput');
    const radioFilters = document.querySelectorAll('input[name="roleFilter"]');
    const userRows = document.querySelectorAll('.user-row');
    const userCountText = document.getElementById('userCountText');

    let currentRoleFilter = 'all';
    let searchQuery = '';

    // 1. 라디오 버튼 필터 이벤트
    radioFilters.forEach(radio => {
        radio.addEventListener('change', function() {
            currentRoleFilter = this.value;
            applyFilters();
        });
    });

    // 2. 검색창 입력 이벤트
    searchInput.addEventListener('input', function(e) {
        searchQuery = e.target.value.toLowerCase().trim();
        applyFilters();
    });

    // 3. 필터 및 검색 적용 로직
    function applyFilters() {
        let visibleCount = 0;

        userRows.forEach(row => {
            const role = row.getAttribute('data-role'); // USER 또는 ADMIN
            const textContent = row.innerText.toLowerCase(); // 행 내부의 모든 텍스트(이름, 이메일 등)

            // 권한 필터 검사
            const matchesRole = (currentRoleFilter === 'all' || currentRoleFilter === role);
            // 검색어 필터 검사
            const matchesSearch = (searchQuery === '' || textContent.includes(searchQuery));

            // 조건에 맞으면 보이고, 아니면 숨김
            if (matchesRole && matchesSearch) {
                row.style.display = '';
                visibleCount++;
            } else {
                row.style.display = 'none';
            }
        });

        // 4. 상단 '총 N명의 사용자' 텍스트 업데이트
        userCountText.textContent = `총 ${visibleCount}명의 사용자`;
    }
});