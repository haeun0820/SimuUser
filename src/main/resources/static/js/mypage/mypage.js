document.addEventListener("DOMContentLoaded", function () {
    let currentUser = null;

    const displayProfileImage = document.getElementById("displayProfileImage");
    const displayProfileFallback = document.getElementById("displayProfileFallback");
    const editProfileImage = document.getElementById("editProfileImage");
    const editProfileFallback = document.getElementById("editProfileFallback");
    const changeImageButton = document.getElementById("changeImageButton");
    const profileImageInput = document.getElementById("profileImageInput");
    const profileImageHelp = document.getElementById("profileImageHelp");

    function applyProfileImage(imageUrl) {
        toggleProfileImage(displayProfileImage, displayProfileFallback, imageUrl);
        toggleProfileImage(editProfileImage, editProfileFallback, imageUrl);
    }

    function toggleProfileImage(imageElement, fallbackElement, imageUrl) {
        if (imageUrl) {
            imageElement.src = imageUrl;
            imageElement.style.display = "block";
            fallbackElement.style.display = "none";
            return;
        }

        imageElement.removeAttribute("src");
        imageElement.style.display = "none";
        fallbackElement.style.display = "flex";
    }

    function bindUser(user) {
        currentUser = user;
        document.getElementById("displayNickname").textContent = user.name || "닉네임";
        document.getElementById("displayEmail").textContent = user.email || "";
        document.getElementById("inputName").value = user.name || "";
        document.getElementById("inputUserId").value = user.loginLabel || user.userId || "";
        document.getElementById("inputEmail").value = user.email || "";
        document.getElementById("inputPhone").value = user.phone === "SOCIAL" ? "" : (user.phone || "");
        document.getElementById("inputBirthDate").value = user.birthDate === "1900-01-01" ? "" : (user.birthDate || "");
        applyProfileImage(user.profileImage || "");

        document.querySelectorAll('input[name="inputGender"]').forEach(input => input.checked = false);
        if (user.gender && user.gender !== "UNKNOWN") {
            const genderInput = document.querySelector(`input[name="inputGender"][value="${user.gender}"]`);
            if (genderInput) {
                genderInput.checked = true;
            }
        }

        setPasswordFieldsEnabled(user.localLogin);
        setProfileImageControls(user.profileImageEditable, user.provider);
    }

    function setPasswordFieldsEnabled(enabled) {
        document.getElementById("inputCurrentPassword").disabled = !enabled;
        document.getElementById("inputNewPassword").disabled = !enabled;
        document.getElementById("inputNewPasswordConfirm").disabled = !enabled;

        if (!enabled) {
            document.getElementById("inputCurrentPassword").placeholder = "소셜 로그인 계정은 비밀번호를 변경할 수 없습니다.";
            document.getElementById("inputNewPassword").placeholder = "소셜 로그인 계정";
            document.getElementById("inputNewPasswordConfirm").placeholder = "소셜 로그인 계정";
        }
    }

    function setProfileImageControls(editable, provider) {
        changeImageButton.disabled = !editable;
        profileImageInput.disabled = !editable;

        if (editable) {
            profileImageHelp.textContent = "이미지 파일을 업로드하면 프로필 사진이 변경됩니다.";
            return;
        }

        if ((provider || "").toUpperCase() === "GOOGLE") {
            profileImageHelp.textContent = "구글 로그인 계정은 구글 프로필 이미지를 사용하므로 변경할 수 없습니다.";
        } else {
            profileImageHelp.textContent = "이 계정은 프로필 이미지를 변경할 수 없습니다.";
        }
    }

    async function loadProfile() {
        try {
            const response = await fetch('/api/me');
            if (!response.ok) {
                throw new Error('프로필을 불러오지 못했습니다.');
            }
            bindUser(await response.json());
        } catch (error) {
            console.error(error);
        }
    }

    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabContents = document.querySelectorAll('.tab-content');

    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            tabBtns.forEach(t => t.classList.remove('active'));
            tabContents.forEach(c => c.classList.remove('active'));

            btn.classList.add('active');
            const targetId = btn.getAttribute('data-target');
            document.getElementById(targetId).classList.add('active');
        });
    });

    let allProjects = [];

    function timeAgo(isoStr) {
        const diff = Date.now() - new Date(isoStr).getTime();
        const min = Math.floor(diff / 60000);
        if (min < 1) return '방금 전';
        if (min < 60) return `${min}분 전`;
        const hr = Math.floor(min / 60);
        if (hr < 24) return `${hr}시간 전`;
        const day = Math.floor(hr / 24);
        return `${day}일 전`;
    }

    function escHtml(str) {
        return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    async function loadProjects() {
        try {
            const response = await fetch('/api/projects');
            if (!response.ok) {
                throw new Error('프로젝트 목록을 불러오지 못했습니다.');
            }
            allProjects = await response.json();
        } catch (error) {
            allProjects = [];
            console.error(error);
        }

        document.getElementById('projectCountLabel').textContent = `${allProjects.length}개 프로젝트`;
        renderProjectList();
    }

    function renderProjectList() {
        const container = document.getElementById('myProjectsList');

        if (allProjects.length === 0) {
            container.innerHTML = `<p style="color:#94a3b8; text-align:center; padding: 20px;">프로젝트가 없습니다.</p>`;
            return;
        }

        container.innerHTML = allProjects.map(p => {
            const isCollab = p.type === 'collab';
            const badge = isCollab
                ? '<span class="badge-collab">협업</span>'
                : '<span class="badge-personal">개인</span>';

            const memberInfo = isCollab && p.members && p.members.length > 0
                ? `<span class="item-meta-badge">With. ${p.members.map(escHtml).join(', ')}</span>`
                : '';

            return `
                <div class="list-item" onclick="location.href='/project/detail/${p.id}'">
                    <div class="list-item-content">
                        <div class="item-title-row">
                            <h3 class="item-title">${escHtml(p.title)}</h3>
                            ${badge}
                        </div>
                        <p class="item-desc">${p.description ? escHtml(p.description) : '프로젝트 설명'}</p>
                        <div class="item-meta">
                            <span>
                                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" style="vertical-align: middle;">
                                    <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.8"/>
                                    <path d="M12 7v5l3 3" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                                </svg>
                                ${timeAgo(p.createdAt || Date.now())}
                            </span>
                            ${memberInfo}
                        </div>
                    </div>
                    <div class="item-arrow">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M5 12h14M12 5l7 7-7 7"/>
                        </svg>
                    </div>
                </div>
            `;
        }).join('');
    }

    loadProjects();

    const mockHistory = [
        { title: "AI 시뮬레이션", date: "2026. 4. 2." },
        { title: "AI 시뮬레이션", date: "2026. 4. 1." },
        { title: "AI 시뮬레이션", date: "2026. 4. 1." },
        { title: "시장 분석", date: "2026. 4. 1." }
    ];

    function renderHistoryList() {
        const container = document.getElementById('myHistoryList');
        container.innerHTML = mockHistory.map(h => `
            <div class="list-item">
                <div class="item-title-row" style="gap: 16px;">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#10b981" stroke-width="2">
                        <circle cx="12" cy="12" r="10"></circle>
                        <path d="M9 12l2 2 4-4"></path>
                    </svg>
                    <div style="display:flex; flex-direction:column; gap:4px;">
                        <span style="font-size:14px; font-weight:600; color:#1e293b;">${h.title}</span>
                        <span style="font-size:12px; color:#94a3b8;">${h.date}</span>
                    </div>
                </div>
                <div style="display:flex; align-items:center; gap:16px;">
                    <span class="badge-done">완료</span>
                    <div class="item-arrow">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M5 12h14M12 5l7 7-7 7"/>
                        </svg>
                    </div>
                </div>
            </div>
        `).join('');
    }

    renderHistoryList();

    changeImageButton.addEventListener("click", function () {
        if (changeImageButton.disabled) {
            return;
        }
        profileImageInput.click();
    });

    profileImageInput.addEventListener("change", async function () {
        const file = this.files && this.files[0];
        if (!file || !currentUser || !currentUser.profileImageEditable) {
            return;
        }

        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
        const headers = {};
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        const formData = new FormData();
        formData.append("file", file);

        changeImageButton.disabled = true;
        profileImageHelp.textContent = "업로드 중입니다...";

        try {
            const response = await fetch('/api/me/profile-image', {
                method: 'POST',
                headers,
                body: formData
            });

            const result = await response.json().catch(() => ({}));
            if (!response.ok) {
                throw new Error(result.message || '프로필 이미지 업로드에 실패했습니다.');
            }

            bindUser(result);
        } catch (error) {
            console.error(error);
            alert(error.message || '프로필 이미지 업로드에 실패했습니다.');
            setProfileImageControls(currentUser.profileImageEditable, currentUser.provider);
        } finally {
            profileImageInput.value = "";
        }
    });

    const profileForm = document.getElementById('profileEditForm');
    profileForm.addEventListener('submit', async function (e) {
        e.preventDefault();
        const passwordMessage = document.getElementById('passwordMismatchMessage');
        passwordMessage.style.display = 'none';
        passwordMessage.textContent = '';

        const gender = document.querySelector('input[name="inputGender"]:checked')?.value || "";
        const payload = {
            name: document.getElementById('inputName').value.trim(),
            phone: document.getElementById('inputPhone').value.trim(),
            birthDate: document.getElementById('inputBirthDate').value,
            gender,
            currentPassword: document.getElementById('inputCurrentPassword').value,
            newPassword: document.getElementById('inputNewPassword').value,
            newPasswordConfirm: document.getElementById('inputNewPasswordConfirm').value
        };

        const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;
        const headers = { 'Content-Type': 'application/json' };

        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        const response = await fetch('/api/me', {
            method: 'PUT',
            headers,
            body: JSON.stringify(payload)
        });

        const result = await response.json().catch(() => ({}));

        if (!response.ok) {
            const message = result.message || '프로필 수정에 실패했습니다.';
            if (message.includes('현재 비밀번호')) {
                passwordMessage.textContent = message;
                passwordMessage.style.display = 'block';
                return;
            }
            alert(message);
            return;
        }

        bindUser(result);
        document.getElementById('inputCurrentPassword').value = '';
        document.getElementById('inputNewPassword').value = '';
        document.getElementById('inputNewPasswordConfirm').value = '';
        alert('프로필이 저장되었습니다.');
    });

    loadProfile();
});
