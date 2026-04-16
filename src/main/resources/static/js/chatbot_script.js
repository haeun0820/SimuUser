/* =============================================
   챗봇 JS - </body> 바로 위 <script> 태그 안에 추가
   또는 chatbot.js 로 분리 후 <script src> 로 불러오기
   ============================================= */

(function () {

    /* ---------- 상태 정의 ---------- */
    const FLOW = {
        main: {
            msg: '안녕하세요! SimuUser 고객지원입니다 👋\n어떤 부분에서 도움이 필요하신가요?',
            options: [
                { label: '📊 분석 관련 문의',       next: 'analytics' },
                { label: '🧠 AI 기능 문의',          next: 'ai'        },
                { label: '📝 프로젝트 / 문서 관련',  next: 'project'   },
                { label: '🐞 오류 / 버그 신고',      next: 'bug'       },
                { label: '🙋 기타 문의',              next: 'other'     },
            ],
        },

        analytics: {
            msg: '분석 관련 문의를 선택하셨군요.\n어떤 부분에서 불편함을 느끼셨나요?',
            options: [
                { label: '분석 결과가 이상하게 나와요.', next: 'analytics_result' },
            ],
        },

        analytics_result: {
            msg: '불편을 드려 죄송합니다 😥\n\n분석 결과가 예상과 다른 경우, 아래 사항을 먼저 확인해 주세요:\n\n• 입력한 타겟 유저 정보가 충분히 구체적인지 확인\n• 아이디어 설명이 300자 이상인지 확인\n\n문제가 지속되면 고객지원 이메일(support@simuuser.io)로 분석 결과 화면 캡처를 보내주시면 빠르게 확인해 드리겠습니다!',
            options: [{ label: '✅ 해결됐어요', next: 'resolved' }, { label: '🔙 처음으로', next: 'main' }],
        },

        ai: {
            msg: 'AI 기능에 대해 궁금한 점이 있으시군요.\n세부 항목을 선택해 주세요.',
            options: [
                { label: '추천 결과 기준이 궁금해요',   next: 'ai_criteria'  },
                { label: '페르소나 생성 방식이 궁금해요', next: 'ai_persona' },
            ],
        },

        ai_criteria: {
            msg: '📌 추천 결과 기준 안내\n\nSimuUser의 AI는 입력된 아이디어와 타겟 정보를 바탕으로 다음 요소를 종합 분석합니다:\n\n• 시장 수요 및 경쟁 강도\n• 유사 서비스의 성공/실패 패턴\n• 타겟 유저의 구매 행동 데이터\n\n분석 결과는 최신 AI 모델 기반으로 실시간 생성되며, 주관적 편향 없이 데이터 중심으로 제공됩니다.',
            options: [{ label: '✅ 이해했어요', next: 'resolved' }, { label: '🔙 처음으로', next: 'main' }],
        },

        ai_persona: {
            msg: '📌 페르소나 생성 방식 안내\n\nAI 가상 유저(페르소나)는 입력된 서비스 특성과 타겟 조건을 기반으로 자동 생성됩니다:\n\n• 나이, 직업, 소득 수준 등 인구통계 자동 설정\n• 서비스 사용 동기 및 기대 효과 분석\n• 구매 의사 확률을 0~100% 수치로 제공\n\n페르소나는 실제 사용자 행동 데이터 학습 결과를 반영하며, 프로젝트마다 독립적으로 생성됩니다.',
            options: [{ label: '✅ 이해했어요', next: 'resolved' }, { label: '🔙 처음으로', next: 'main' }],
        },

        project: {
            msg: '프로젝트 / 문서 관련 문의군요.\n세부 항목을 선택해 주세요.',
            options: [
                { label: '프로젝트 저장 / 불러오기',  next: 'project_save'   },
                { label: '문서 생성 (기획서/보고서)', next: 'project_docs'   },
                { label: '문서 다운로드',              next: 'project_dl'    },
                { label: '협업 / 공유 기능',          next: 'project_share'  },
                { label: '프로젝트 삭제',             next: 'project_delete' },
            ],
        },

        project_save: {
            msg: '프로젝트는 분석 완료 후 자동으로 저장됩니다.\n\n저장된 프로젝트는 상단 메뉴 [전체 프로젝트]에서 확인 및 불러오기가 가능합니다.\n문제가 있으시면 브라우저 쿠키가 활성화되어 있는지 확인해 주세요.',
            options: [{ label: '✅ 해결됐어요', next: 'resolved' }, { label: '🔙 처음으로', next: 'main' }],
        },

        project_docs: {
            msg: '문서 생성은 분석 완료 후 [자동 문서화] 메뉴에서 이용 가능합니다.\n\n기획서 / 보고서 형식 중 원하는 양식을 선택하면 AI가 자동으로 완성본을 생성해 드립니다.',
            options: [{ label: '✅ 이해했어요', next: 'resolved' }, { label: '🔙 처음으로', next: 'main' }],
        },

        project_dl: {
            msg: '문서 다운로드는 [자동 문서화] 페이지에서 PDF 내보내기 버튼을 통해 가능합니다.\n\n다운로드가 안 되는 경우 팝업 차단 설정을 확인해 주세요.',
            options: [{ label: '✅ 해결됐어요', next: 'resolved' }, { label: '🔙 처음으로', next: 'main' }],
        },

        project_share: {
            msg: '협업 / 공유 기능은 현재 개발 중으로 곧 출시 예정입니다! 🚀\n\n출시 알림을 받으시려면 마이페이지에서 이메일 수신 동의를 활성화해 주세요.',
            options: [{ label: '✅ 이해했어요', next: 'resolved' }, { label: '🔙 처음으로', next: 'main' }],
        },

        project_delete: {
            msg: '[전체 프로젝트] 페이지에서 삭제할 프로젝트를 선택 후\n[삭제] 버튼을 클릭하시면 됩니다.\n\n⚠️ 삭제된 프로젝트는 복구가 불가능하니 신중히 진행해 주세요.',
            options: [{ label: '✅ 이해했어요', next: 'resolved' }, { label: '🔙 처음으로', next: 'main' }],
        },

        bug: {
            msg: '불편을 드려 죄송합니다 😥\n오류 유형을 선택해 주세요.\n아래 스크린샷 첨부 버튼도 활용해 주시면 더 빠르게 확인이 가능합니다!',
            options: [
                { label: '페이지가 안 열림',   next: 'bug_page'    },
                { label: '분석 실행 안됨',     next: 'bug_analysis'},
                { label: '데이터 저장 오류',   next: 'bug_save'    },
                { label: '로그인 문제',        next: 'bug_login'   },
                { label: '기타 오류',          next: 'bug_other'   },
            ],
            showAttach: true,
        },

        bug_page: {
            msg: '페이지 접속 오류 시 아래를 시도해 주세요:\n\n1. 브라우저 캐시/쿠키 삭제 후 재접속\n2. 다른 브라우저(Chrome 권장)로 접속\n3. 문제 지속 시 support@simuuser.io로 문의\n\n스크린샷을 첨부해 주시면 더 빠른 처리가 가능합니다.',
            options: [{ label: '✅ 해결됐어요', next: 'resolved' }, { label: '🔙 처음으로', next: 'main' }],
            showAttach: true,
        },

        bug_analysis: {
            msg: '분석 실행 오류 시 아래를 확인해 주세요:\n\n1. 모든 필수 입력값이 채워졌는지 확인\n2. 페이지 새로고침 후 재시도\n3. 문제 지속 시 오류 발생 화면을 캡처하여 support@simuuser.io로 보내주세요.',
            options: [{ label: '✅ 해결됐어요', next: 'resolved' }, { label: '🔙 처음으로', next: 'main' }],
            showAttach: true,
        },

        bug_save: {
            msg: '저장 오류가 발생하셨군요.\n\n브라우저의 로컬 스토리지 설정을 확인하시고, 시크릿 모드 사용 중이라면 일반 탭으로 전환 후 재시도해 주세요.\n\n문제가 지속되면 스크린샷과 함께 support@simuuser.io로 문의해 주세요.',
            options: [{ label: '✅ 해결됐어요', next: 'resolved' }, { label: '🔙 처음으로', next: 'main' }],
            showAttach: true,
        },

        bug_login: {
            msg: '로그인 문제 시 아래를 시도해 주세요:\n\n1. 비밀번호 재설정 기능 이용\n2. 이메일 인증 메일 재발송\n3. 소셜 로그인(Google/Kakao) 연동 확인\n\n그래도 안 되신다면 support@simuuser.io로 가입 이메일과 함께 문의해 주세요.',
            options: [{ label: '✅ 해결됐어요', next: 'resolved' }, { label: '🔙 처음으로', next: 'main' }],
        },

        bug_other: {
            msg: '불편을 드려 죄송합니다.\n아래 스크린샷 첨부 기능을 이용하거나, 오류 내용을 직접 입력해 주시면 빠르게 확인하겠습니다!',
            options: [{ label: '🔙 처음으로', next: 'main' }],
            showAttach: true,
            showInput: true,
        },

        other: {
            msg: '기타 문의 사항을 아래에 자유롭게 입력해 주세요.\n최대한 빠르게 답변 드리겠습니다! 😊',
            options: [],
            showInput: true,
        },

        resolved: {
            msg: '도움이 되셨으면 좋겠어요 😊\n추가로 궁금한 점이 있으시면 언제든지 문의해 주세요!',
            options: [{ label: '🔙 처음으로 돌아가기', next: 'main' }],
        },

        submitted: {
            msg: '문의가 접수되었습니다 ✅\n영업일 기준 1~2일 내로 입력하신 내용을 검토 후 답변 드리겠습니다.\n감사합니다!',
            options: [{ label: '🔙 처음으로 돌아가기', next: 'main' }],
        },
    };

    /* ---------- DOM ---------- */
    const toggleBtn    = document.getElementById('chatbotToggle');
    const window_      = document.getElementById('chatbotWindow');
    const closeBtn     = document.getElementById('chatbotClose');
    const messagesEl   = document.getElementById('chatbotMessages');
    const inputArea    = document.getElementById('chatbotInputArea');
    const inputEl      = document.getElementById('chatbotInput');
    const sendBtn      = document.getElementById('chatbotSendBtn');
    const attachArea   = document.getElementById('chatbotAttachArea');
    const fileInput    = document.getElementById('screenshotInput');
    const fileNameEl   = document.getElementById('attachedFileName');
    const homeBtn      = document.getElementById('chatbotHomeBtn');
    const iconChat     = toggleBtn.querySelector('.icon-chat');
    const iconClose    = toggleBtn.querySelector('.icon-close');

    let isOpen = false;

    /* ---------- 열기/닫기 ---------- */
    function toggleChat() {
        isOpen = !isOpen;
        window_.classList.toggle('open', isOpen);
        iconChat.style.display  = isOpen ? 'none'  : '';
        iconClose.style.display = isOpen ? ''      : 'none';
        if (isOpen && messagesEl.children.length === 0) {
            setTimeout(() => goTo('main'), 200);
        }
    }

    toggleBtn.addEventListener('click', toggleChat);
    closeBtn.addEventListener('click', toggleChat);
    homeBtn.addEventListener('click', () => {
        messagesEl.innerHTML = '';
        hideExtras();
        setTimeout(() => goTo('main'), 100);
    });

    /* ---------- 메시지 추가 ---------- */
    function addBotMsg(text) {
        const row = document.createElement('div');
        row.className = 'msg-row bot';

        const avatar = document.createElement('div');
        avatar.className = 'msg-avatar-bot';
        avatar.textContent = 'S';

        const bubble = document.createElement('div');
        bubble.className = 'msg-bubble';
        bubble.style.whiteSpace = 'pre-wrap';
        bubble.textContent = text;

        row.appendChild(avatar);
        row.appendChild(bubble);
        messagesEl.appendChild(row);
        scrollBottom();
        return row;
    }

    function addUserMsg(text) {
        const row = document.createElement('div');
        row.className = 'msg-row user';

        const bubble = document.createElement('div');
        bubble.className = 'msg-bubble';
        bubble.textContent = text;

        row.appendChild(bubble);
        messagesEl.appendChild(row);
        scrollBottom();
    }

    function addOptions(options) {
        const group = document.createElement('div');
        group.className = 'chatbot-options';

        options.forEach(opt => {
            const btn = document.createElement('button');
            btn.className = 'chatbot-option-btn';
            btn.textContent = opt.label;
            btn.addEventListener('click', () => {
                // 모든 선택지 비활성화
                group.querySelectorAll('.chatbot-option-btn').forEach(b => b.disabled = true);
                addUserMsg(opt.label);
                setTimeout(() => goTo(opt.next), 400);
            });
            group.appendChild(btn);
        });

        messagesEl.appendChild(group);
        scrollBottom();
    }

    function showTyping(cb) {
        const row = document.createElement('div');
        row.className = 'msg-row bot';

        const avatar = document.createElement('div');
        avatar.className = 'msg-avatar-bot';
        avatar.textContent = 'S';

        const indicator = document.createElement('div');
        indicator.className = 'typing-indicator';
        [1,2,3].forEach(() => {
            const dot = document.createElement('div');
            dot.className = 'typing-dot';
            indicator.appendChild(dot);
        });

        row.appendChild(avatar);
        row.appendChild(indicator);
        messagesEl.appendChild(row);
        scrollBottom();

        setTimeout(() => {
            messagesEl.removeChild(row);
            cb();
        }, 900);
    }

    function scrollBottom() {
        messagesEl.scrollTop = messagesEl.scrollHeight;
    }

    /* ---------- 부가 영역 표시 ---------- */
    function hideExtras() {
        inputArea.classList.remove('visible');
        attachArea.classList.remove('visible');
    }

    /* ---------- 상태 이동 ---------- */
    function goTo(stateKey) {
        const state = FLOW[stateKey];
        if (!state) return;

        hideExtras();

        showTyping(() => {
            addBotMsg(state.msg);

            if (state.options && state.options.length > 0) {
                addOptions(state.options);
            }

            if (state.showInput) {
                inputArea.classList.add('visible');
                inputEl.focus();
            }

            if (state.showAttach) {
                attachArea.classList.add('visible');
            }
        });
    }

    /* ---------- 자유 텍스트 전송 ---------- */
    function sendFreeText() {
        const text = inputEl.value.trim();
        if (!text) return;
        addUserMsg(text);
        inputEl.value = '';
        inputArea.classList.remove('visible');
        setTimeout(() => goTo('submitted'), 400);
    }

    sendBtn.addEventListener('click', sendFreeText);
    inputEl.addEventListener('keydown', e => {
        if (e.key === 'Enter') sendFreeText();
    });

    /* ---------- 스크린샷 첨부 ---------- */
    fileInput.addEventListener('change', () => {
        const file = fileInput.files[0];
        if (file) {
            fileNameEl.textContent = file.name;
            addBotMsg('📎 스크린샷이 첨부되었습니다.\n내용을 함께 입력해 주시면 빠르게 검토하겠습니다!');
            inputArea.classList.add('visible');
            inputEl.focus();
        }
    });

})();