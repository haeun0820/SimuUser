document.addEventListener('DOMContentLoaded', () => {
  const notiBtn = document.getElementById('notiBtn');
  const notiDropdown = document.getElementById('notiDropdown');

  if (!notiBtn || !notiDropdown) return;

  notiBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    notiDropdown.classList.toggle('active');
  });

  document.addEventListener('click', (e) => {
    if (!notiDropdown.contains(e.target) && e.target !== notiBtn) {
      notiDropdown.classList.remove('active');
    }
  });

  const clearBtn = document.querySelector('.clear-all');
  if (clearBtn) {
    clearBtn.addEventListener('click', () => {
      if (confirm('모든 알림을 삭제하시겠습니까?')) {
        document.querySelector('.noti-list').innerHTML =
          '<p style="padding:40px; text-align:center; color:#94a3b8;">새로운 알림이 없습니다.</p>';
        document.querySelector('.noti-badge').style.display = 'none';
      }
    });
  }
});