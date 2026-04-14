document.addEventListener('DOMContentLoaded', function() {
    // 1. 이전 페이지에서 넘겨준 데이터 로드 (localStorage 예시)
    // 실제로는 URL 파라미터를 통해 서버 API를 호출하는 것이 좋습니다.
    const docData = JSON.parse(localStorage.getItem('currentEditDoc'));

    if (docData) {
        document.getElementById('headerTitle').innerText = docData.title;
        document.getElementById('headerDesc').innerText = docData.description;
        document.getElementById('editorPage').innerText = docData.content || "";
    }

    // 2. 저장 버튼 클릭
    document.getElementById('btnSave').onclick = function() {
        alert("문서가 저장되었습니다.");
        // 실제로는 여기서 API를 통해 서버 DB에 내용을 저장해야 함
    };

    // 3. 탭 추가 로직
    const tabsContainer = document.getElementById('tabsContainer');
    document.getElementById('btnAddTab').onclick = function() {
        const newTabNum = tabsContainer.children.length + 1;
        const newTab = document.createElement('div');
        newTab.className = 'tab-item';
        newTab.innerText = `탭 ${newTabNum}`;
        newTab.dataset.tab = newTabNum;
        
        newTab.onclick = function() {
            document.querySelectorAll('.tab-item').forEach(t => t.classList.remove('active'));
            newTab.classList.add('active');
        };

        tabsContainer.appendChild(newTab);
    };

    // 4. 기존 탭 클릭 이벤트
    document.querySelectorAll('.tab-item').forEach(tab => {
        tab.onclick = function() {
            document.querySelectorAll('.tab-item').forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
        };
    });
});