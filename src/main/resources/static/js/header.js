document.addEventListener('DOMContentLoaded', () => {
  // 1. 요소 가져오기
  const notiBtn = document.getElementById('notiBtn');
  const notiDropdown = document.getElementById('notiDropdown');
  const friendBtn = document.getElementById('friendBtn'); // 추가
  const friendDrawer = document.getElementById('friendDrawer'); // 추가
  const closeFriendDrawer = document.getElementById('closeFriendDrawer'); // 추가
  const drawerOverlay = document.getElementById('drawerOverlay'); // 추가

  // 2. 알림 버튼 클릭 시
  if (notiBtn && notiDropdown) {
    notiBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      notiDropdown.classList.toggle('active');
      // 알림 열 때 친구 드로어는 닫기
      if(friendDrawer) {
          friendDrawer.classList.remove('active');
          drawerOverlay.classList.remove('active');
      }
    });
  }

  // 3. 친구 버튼 클릭 시 (이 부분이 없어서 안 떴을 거예요!)
  if (friendBtn && friendDrawer) {
    friendBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      friendDrawer.classList.add('active'); // active 클래스 추가
      drawerOverlay.classList.add('active'); // 배경 어둡게
      // 친구 열 때 알림창은 닫기
      if(notiDropdown) notiDropdown.classList.remove('active');
    });
  }

  // 4. 친구 드로어 닫기 (X 버튼 및 배경 클릭 시)
  if (closeFriendDrawer && drawerOverlay) {
    [closeFriendDrawer, drawerOverlay].forEach(el => {
      el.addEventListener('click', () => {
        friendDrawer.classList.remove('active');
        drawerOverlay.classList.remove('active');
      });
    });
  }

  // 5. 화면 다른 곳 클릭 시 알림창 닫기
  document.addEventListener('click', (e) => {
    if (notiDropdown && !notiDropdown.contains(e.target) && e.target !== notiBtn) {
      notiDropdown.classList.remove('active');
    }
  });

  // 6. 알림 삭제 로직 (기존 유지)
  const clearBtn = document.querySelector('.clear-all');
  if (clearBtn) {
    clearBtn.addEventListener('click', () => {
      if (confirm('모든 알림을 삭제하시겠습니까?')) {
        const notiList = document.querySelector('.noti-list');
        if(notiList) notiList.innerHTML = '<p style="padding:40px; text-align:center; color:#94a3b8;">새로운 알림이 없습니다.</p>';
        const badge = document.querySelector('.noti-badge');
        if(badge) badge.style.display = 'none';
      }
    });
  }


});