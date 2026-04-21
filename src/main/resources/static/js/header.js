document.addEventListener('DOMContentLoaded', () => {
  const notiBtn = document.getElementById('notiBtn');
  const notiDropdown = document.getElementById('notiDropdown');
  const friendBtn = document.getElementById('friendBtn');
  const friendDrawer = document.getElementById('friendDrawer');
  const closeFriendDrawer = document.getElementById('closeFriendDrawer');
  const drawerOverlay = document.getElementById('drawerOverlay');

  if (notiBtn && notiDropdown) {
    notiBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      e.stopImmediatePropagation();
      notiDropdown.classList.toggle('active');
      loadProjectInvitations();

      if (friendDrawer && drawerOverlay) {
        friendDrawer.classList.remove('active');
        drawerOverlay.classList.remove('active');
      }
    });
  }

  if (friendBtn && friendDrawer && drawerOverlay) {
    friendBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      e.stopImmediatePropagation();
      friendDrawer.classList.add('active');
      drawerOverlay.classList.add('active');
      if (notiDropdown) notiDropdown.classList.remove('active');
    });
  }

  if (closeFriendDrawer && drawerOverlay && friendDrawer) {
    [closeFriendDrawer, drawerOverlay].forEach(el => {
      el.addEventListener('click', () => {
        friendDrawer.classList.remove('active');
        drawerOverlay.classList.remove('active');
      });
    });
  }

  document.addEventListener('click', (e) => {
    if (notiDropdown && !notiDropdown.contains(e.target) && e.target !== notiBtn && !notiBtn?.contains(e.target)) {
      notiDropdown.classList.remove('active');
    }
  });

  const clearBtn = document.querySelector('.clear-all');
  if (clearBtn) {
    clearBtn.addEventListener('click', async (e) => {
      e.stopImmediatePropagation();
      if (!confirm('모든 알림을 삭제하시겠습니까?')) return;

      try {
        const response = await fetch('/api/notifications', {
          method: 'DELETE',
          headers: csrfHeaders()
        });
        if (!response.ok) throw new Error('알림 삭제에 실패했습니다.');

        const notiList = document.querySelector('.noti-list');
        if (notiList) notiList.innerHTML = '<p class="noti-empty">새로운 알림이 없습니다.</p>';
        updateNotificationBadge(0);
      } catch (error) {
        alert(error.message);
      }
    });
  }

  loadProjectInvitations();
});

function csrfHeaders() {
  const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content')
    || document.getElementById('csrfToken')?.value;
  const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content')
    || document.getElementById('csrfHeader')?.value;

  return token && header ? { [header]: token } : {};
}

async function loadProjectInvitations() {
  const notiList = document.querySelector('.noti-list');
  if (!notiList) return;

  try {
    const response = await fetch('/api/notifications', {
      headers: { Accept: 'application/json' }
    });

    if (!response.ok) throw new Error('초대 알림을 불러오지 못했습니다.');

    const notifications = await response.json();
    renderProjectInvitations(notifications);
  } catch (error) {
    notiList.innerHTML = `<p class="noti-empty">${escapeHtml(error.message)}</p>`;
    updateNotificationBadge(0);
  }
}

function renderProjectInvitations(notifications) {
  const notiList = document.querySelector('.noti-list');
  if (!notiList) return;

  const inviteNotifications = (notifications || []).filter(notification => notification.type === 'PROJECT_INVITE' && notification.invite);

  if (inviteNotifications.length === 0) {
    notiList.innerHTML = '<p class="noti-empty">새로운 알림이 없습니다.</p>';
    updateNotificationBadge(0);
    return;
  }

  notiList.innerHTML = inviteNotifications.map(notification => {
    const invite = notification.invite;
    return `
    <article class="noti-item project-invite" data-notification-id="${notification.id}" data-invite-id="${invite.id}">
      <div class="noti-title-row">
        <span class="noti-title">${escapeHtml(notification.title || '프로젝트 초대 요청')}</span>
        <span class="invite-role">${escapeHtml(invite.role || '편집자')}</span>
      </div>
      <p class="noti-text">
        <strong>${escapeHtml(invite.inviterName || invite.inviterEmail || '팀원')}</strong>님이
        <strong>${escapeHtml(invite.projectTitle || '프로젝트')}</strong> 프로젝트에 초대했습니다.
      </p>
      <span class="noti-time">${formatRelativeTime(invite.createdAt)}</span>
      <div class="invite-actions">
        <button type="button" class="invite-btn accept" onclick="respondProjectInvite(${invite.id}, 'accept')">수락</button>
        <button type="button" class="invite-btn decline" onclick="respondProjectInvite(${invite.id}, 'decline')">거절</button>
      </div>
    </article>
  `;
  }).join('');

  updateNotificationBadge(inviteNotifications.length);
}

async function respondProjectInvite(inviteId, action) {
  try {
    const response = await fetch(`/api/project-invitations/${inviteId}/${action}`, {
      method: 'POST',
      headers: {
        Accept: 'application/json',
        ...csrfHeaders()
      }
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new Error(error.message || '초대 요청 처리에 실패했습니다.');
    }

    await loadProjectInvitations();
    if (action === 'accept') {
      window.dispatchEvent(new CustomEvent('projectInviteAccepted'));
    }
  } catch (error) {
    alert(error.message);
  }
}

function updateNotificationBadge(count) {
  const badge = document.querySelector('.noti-badge');
  if (!badge) return;

  badge.style.display = count > 0 ? 'block' : 'none';
}

function formatRelativeTime(value) {
  if (!value) return '방금 전';

  const diffMs = Date.now() - new Date(value).getTime();
  const minutes = Math.max(0, Math.floor(diffMs / 60000));
  if (minutes < 1) return '방금 전';
  if (minutes < 60) return `${minutes}분 전`;

  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}시간 전`;

  return `${Math.floor(hours / 24)}일 전`;
}

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}


document.addEventListener('DOMContentLoaded', () => {
    const chatBtn = document.getElementById('chatBtn');

    if (chatBtn) {
        chatBtn.addEventListener('click', (e) => {
            e.preventDefault();
            
            // 새 창의 설정 (너비, 높이, 위치 등)
            const width = 360;
            const height = 500;
            const left = (window.screen.width / 2) - (width / 2);
            const top = (window.screen.height / 2) - (height / 2);
            
            const windowFeatures = `width=${width},height=${height},left=${left},top=${top},resizable=yes,scrollbars=yes`;
            
            // window.open('경로', '창이름', '설정');
            // 'chatWindow'라는 이름을 주면 여러번 눌러도 하나의 창에서만 바뀝니다.
            window.open('/chat/window', 'chatWindow', windowFeatures); 
        });
    }
});