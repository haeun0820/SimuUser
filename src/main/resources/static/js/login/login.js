(function () {
  'use strict';

  const overlay = document.getElementById('extraInfoOverlay');
  const icon = document.getElementById('modalIcon');
  const cancelBtn = document.getElementById('modalCancel');
  const submitBtn = document.getElementById('modalSubmit');
  const errorBox = document.getElementById('modalError');
  const params = new URLSearchParams(window.location.search);
  const onboardingRequired = params.get('socialSignupRequired') === 'true';
  const providerFromQuery = (params.get('provider') || '').toLowerCase();
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

  const fields = {
    email: document.getElementById('m-email'),
    nickname: document.getElementById('m-nickname'),
    phone: document.getElementById('m-phone'),
    birth: document.getElementById('m-birth')
  };

  let targetUrl = '';
  let modalLocked = false;

  fields.phone.addEventListener('input', function () {
    let v = this.value.replace(/\D/g, '');
    if (v.length > 11) v = v.slice(0, 11);
    if (v.length <= 3) this.value = v;
    else if (v.length <= 7) this.value = v.slice(0, 3) + '-' + v.slice(3);
    else this.value = v.slice(0, 3) + '-' + v.slice(3, 7) + '-' + v.slice(7);
    checkValidity();
  });

  function isValid() {
    const emailOk = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(fields.email.value.trim());
    const nicknameOk = fields.nickname.value.trim().length > 0;
    const phoneOk = /^\d{3}-\d{3,4}-\d{4}$/.test(fields.phone.value.trim());
    const birthOk = fields.birth.value !== '';
    const genderOk = document.querySelector('input[name="m-gender"]:checked') !== null;
    return emailOk && nicknameOk && phoneOk && birthOk && genderOk;
  }

  function checkValidity() {
    submitBtn.disabled = !isValid();
  }

  function setError(message) {
    errorBox.textContent = message || '';
    errorBox.style.display = message ? 'block' : 'none';
  }

  function resetFields() {
    Object.values(fields).forEach(el => {
      el.value = '';
    });
    document.querySelectorAll('input[name="m-gender"]').forEach(el => {
      el.checked = false;
    });
    setError('');
    submitBtn.disabled = true;
  }

  Object.values(fields).forEach(el => el.addEventListener('input', checkValidity));
  document.querySelectorAll('input[name="m-gender"]').forEach(el => {
    el.addEventListener('change', checkValidity);
  });

  function openModal(provider, oauthUrl, locked) {
    targetUrl = oauthUrl;
    icon.className = 'modal-icon ' + provider;
    modalLocked = Boolean(locked);
    cancelBtn.style.display = modalLocked ? 'none' : '';
    overlay.style.display = 'flex';
    fields.email.focus();
  }

  function closeModal() {
    if (modalLocked) {
      return;
    }

    overlay.style.display = 'none';
    targetUrl = '';
    resetFields();
  }

  document.querySelectorAll('[data-provider]').forEach(function (btn) {
    btn.addEventListener('click', function (e) {
      e.preventDefault();
      window.location.href = this.dataset.oauthUrl;
    });
  });

  cancelBtn.addEventListener('click', closeModal);

  overlay.addEventListener('click', function (e) {
    if (e.target === overlay) closeModal();
  });

  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape' && overlay.style.display === 'flex') closeModal();
  });

  async function prefillFromProfile() {
    try {
      const response = await fetch('/api/me', { credentials: 'same-origin' });
      if (!response.ok) {
        return;
      }

      const user = await response.json();
      fields.email.value = user.email || '';
      fields.nickname.value = user.name || '';
      fields.phone.value = user.phone && user.phone !== 'SOCIAL' ? user.phone : '';
      fields.birth.value = user.birthDate && user.birthDate !== '1900-01-01' ? user.birthDate : '';

      if (user.gender === 'male' || user.gender === 'female') {
        const genderInput = document.querySelector(`input[name="m-gender"][value="${user.gender}"]`);
        if (genderInput) {
          genderInput.checked = true;
        }
      }

      checkValidity();
    } catch (error) {
      console.error(error);
    }
  }

  async function submitOnboarding() {
    if (!isValid()) return;

    const payload = {
      email: fields.email.value.trim(),
      nickname: fields.nickname.value.trim(),
      phone: fields.phone.value.trim(),
      birthDate: fields.birth.value,
      gender: document.querySelector('input[name="m-gender"]:checked').value
    };

    const headers = { 'Content-Type': 'application/json' };
    if (csrfToken && csrfHeader) {
      headers[csrfHeader] = csrfToken;
    }

    submitBtn.disabled = true;
    setError('');

    try {
      const response = await fetch('/api/auth/social-onboarding', {
        method: 'POST',
        headers,
        credentials: 'same-origin',
        body: JSON.stringify(payload)
      });

      const result = await response.json().catch(() => ({}));
      if (!response.ok) {
        setError(result.message || '추가 정보 저장에 실패했습니다.');
        checkValidity();
        return;
      }

      window.location.replace(targetUrl || '/');
    } catch (error) {
      console.error(error);
      setError('추가 정보 저장 중 오류가 발생했습니다.');
      checkValidity();
    }
  }

  submitBtn.addEventListener('click', submitOnboarding);

  if (onboardingRequired) {
    resetFields();
    openModal(providerFromQuery || 'google', '/', true);
    prefillFromProfile();
  }
})();
