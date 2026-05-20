document.addEventListener("DOMContentLoaded", function() {
    
    const searchInput = document.getElementById('inquirySearchInput');
    const filterUnreadCheckbox = document.getElementById('filterUnread');
    const inquiryRows = document.querySelectorAll('.inquiry-row');
    const countDisplay = document.getElementById('inquiryCount');

    let searchQuery = '';
    let showOnlyUnread = false;

    // 1. 체크박스 변경 이벤트
    filterUnreadCheckbox.addEventListener('change', function(e) {
        showOnlyUnread = e.target.checked;
        applyFilters();
    });

    // 2. 검색창 입력 이벤트
    searchInput.addEventListener('input', function(e) {
        searchQuery = e.target.value.toLowerCase().trim();
        applyFilters();
    });

    // 3. 필터 적용 로직
    function applyFilters() {
        let visibleCount = 0;

        inquiryRows.forEach(row => {
            const status = row.getAttribute('data-status'); // unread 또는 answered
            // 제목과 작성자 텍스트를 가져옴
            const title = row.querySelector('.col-title').innerText.toLowerCase();
            const author = row.querySelector('.col-author').innerText.toLowerCase();
            
            // 조건 검사
            const matchesSearch = (searchQuery === '' || title.includes(searchQuery) || author.includes(searchQuery));
            const matchesStatus = (!showOnlyUnread || status === 'unread');

            // 두 조건을 모두 만족하면 표시
            if (matchesSearch && matchesStatus) {
                row.style.display = '';
                visibleCount++;
            } else {
                row.style.display = 'none';
            }
        });

        // 결과 개수 텍스트 업데이트
        countDisplay.textContent = visibleCount;
    }
});