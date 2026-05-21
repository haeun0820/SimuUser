document.addEventListener("DOMContentLoaded", function() {
    
    const searchInput = document.getElementById('userSearchInput');
    const radioFilters = document.querySelectorAll('input[name="loginMethodFilter"]');
    const userRows = document.querySelectorAll('.user-row');
    const userCountText = document.getElementById('userCountText');

    let currentMethodFilter = 'all';
    let searchQuery = '';

    // 1. 로그인 방식 변경 이벤트 매핑
    radioFilters.forEach(radio => {
        radio.addEventListener('change', function() {
            currentMethodFilter = this.value;
            applyFilters();
        });
    });

    // 2. 검색창 실시간 탐색 이벤트
    searchInput.addEventListener('input', function(e) {
        searchQuery = e.target.value.toLowerCase().trim();
        applyFilters();
    });

    // 3. 다중 조건 필터링 비즈니스 로직
    function applyFilters() {
        let visibleCount = 0;

        userRows.forEach(row => {
            const loginMethod = row.getAttribute('data-login-method'); // 각 행의 로그인 플랫폼 (google, naver 등)
            const textContent = row.innerText.toLowerCase(); 

            // 로그인 방식 부합 조건 검증
            const matchesMethod = (currentMethodFilter === 'all' || currentMethodFilter === loginMethod);
            // 텍스트 매칭 검색 조건 검증
            const matchesSearch = (searchQuery === '' || textContent.includes(searchQuery));

            if (matchesMethod && matchesSearch) {
                row.style.display = '';
                visibleCount++;
            } else {
                row.style.display = 'none';
            }
        });

        // 4. 동적 카운트 텍스트 갱신
        userCountText.textContent = `총 ${visibleCount}명의 사용자`;
    }
});

// 💡 데이터 내보내기 액션 핸들러
function exportUserData() {
    // 실무 백엔드 연결 시: window.location.href = '/admin/user/export'; 와 같이 연동하여 파일 스트림을 다운로드합니다.
    alert("현재 필터링된 사용자 데이터를 Excel 문서 포맷으로 추출합니다.\n(Apache POI 등 파일 라이브러리 연동 구간)");
}