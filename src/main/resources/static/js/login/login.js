(function () {
  'use strict';

  const overlay   = document.getElementById('extraInfoOverlay');
  const icon      = document.getElementById('modalIcon');
  const cancelBtn = document.getElementById('modalCancel');
  const submitBtn = document.getElementById('modalSubmit');

  const fields = {
    email    : document.getElementById('m-email'),
    nickname : document.getElementById('m-nickname'),
    phone    : document.getElementById('m-phone'),
    birth    : document.getElementById('m-birth')
  };

  let targetUrl = '';

  /* ── 전화번호 자동 하이픈 ── */
  fields.phone.addEventListener('input', function () {
    let v = this.value.replace(/\D/g, '');
    if (v.length > 11) v = v.slice(0, 11);
    if (v.length <= 3)       this.value = v;
    else if (v.length <= 7)  this.value = v.slice(0, 3) + '-' + v.slice(3);
    else                     this.value = v.slice(0, 3) + '-' + v.slice(3, 7) + '-' + v.slice(7);
    checkValidity();
  });

  /* ── 유효성 검사 ── */
  function isValid() {
    const emailOk    = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(fields.email.value.trim());
    const nicknameOk = fields.nickname.value.trim().length > 0;
    const phoneOk    = /^\d{3}-\d{3,4}-\d{4}$/.test(fields.phone.value.trim());
    const birthOk    = fields.birth.value !== '';
    const genderOk   = document.querySelector('input[name="m-gender"]:checked') !== null;
    return emailOk && nicknameOk && phoneOk && birthOk && genderOk;
  }

  function checkValidity() {
    submitBtn.disabled = !isValid();
  }

  /* 모든 입력 필드에 검사 이벤트 등록 */
  Object.values(fields).forEach(el => el.addEventListener('input', checkValidity));
  document.querySelectorAll('input[name="m-gender"]').forEach(el => {
    el.addEventListener('change', checkValidity);
  });

  /* ── 모달 열기 ── */
  function openModal(provider, oauthUrl) {
    targetUrl = oauthUrl;
    icon.className = 'modal-icon ' + provider;

    /* 폼 초기화 */
    Object.values(fields).forEach(el => el.value = '');
    document.querySelectorAll('input[name="m-gender"]').forEach(el => el.checked = false);
    submitBtn.disabled = true;

    overlay.style.display = 'flex';
    fields.email.focus();
  }

  /* ── 모달 닫기 ── */
  function closeModal() {
    overlay.style.display = 'none';
    targetUrl = '';
  }

  /* ── 네이버 / 카카오 버튼 클릭 가로채기 ── */
  document.querySelectorAll('[data-provider]').forEach(function (btn) {
    btn.addEventListener('click', function (e) {
      e.preventDefault();
      openModal(this.dataset.provider, this.dataset.oauthUrl);
    });
  });

  /* ── 취소 ── */
  cancelBtn.addEventListener('click', closeModal);

  /* ── 오버레이 바깥 클릭 시 닫기 ── */
  overlay.addEventListener('click', function (e) {
    if (e.target === overlay) closeModal();
  });

  /* ── ESC 키 닫기 ── */
  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape' && overlay.style.display === 'flex') closeModal();
  });

  /* ── 로그인 제출 → sessionStorage에 저장 후 OAuth 이동 ── */
  submitBtn.addEventListener('click', function () {
    if (!isValid()) return;

    const extra = {
      email    : fields.email.value.trim(),
      nickname : fields.nickname.value.trim(),
      phone    : fields.phone.value.trim(),
      birth    : fields.birth.value,
      gender   : document.querySelector('input[name="m-gender"]:checked').value
    };

    sessionStorage.setItem('oauth_extra_info', JSON.stringify(extra));
    window.location.href = targetUrl;
  });

})();