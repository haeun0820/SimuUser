// 1. 탭 전환 기능
function switchDetailTab(clickedTab, targetId) {
    // 모든 탭 버튼 비활성화
    document.querySelectorAll('.admin-tab').forEach(tab => {
        tab.classList.remove('active');
    });
    // 클릭한 탭 활성화
    clickedTab.classList.add('active');

    // 모든 컨텐츠 숨기기
    document.querySelectorAll('.tab-pane').forEach(pane => {
        pane.style.display = 'none';
        pane.classList.remove('active');
    });

    // 타겟 컨텐츠 보여주기
    const targetPane = document.getElementById('tab-' + targetId);
    if (targetPane) {
        targetPane.style.display = 'flex';
        targetPane.classList.add('active');
    }

    // 💡 분석 탭을 눌렀을 때 차트가 깨지지 않도록 리사이즈 강제 호출
    if (targetId === 'analysis' && window.userChart) {
        window.userChart.resize();
    }
}

// 2. 6가지 분석 도구 차트 (Chart.js) 초기화
document.addEventListener("DOMContentLoaded", function() {
    const ctx = document.getElementById('userAnalysisChart');
    if (!ctx) return;

    window.userChart = new Chart(ctx.getContext('2d'), {
        // 여러 항목을 한눈에 보기 좋게 선 그래프(line)로 표현 (막대로 원하시면 'bar'로 변경 가능)
        type: 'line', 
        data: {
            labels: ['11월', '12월', '1월', '2월', '3월', '4월'],
            datasets: [
                { label: '시뮬레이션', data: [2, 3, 5, 4, 6, 8], borderColor: '#8b5cf6', backgroundColor: '#8b5cf6', tension: 0.3 },
                { label: '시장 분석', data: [1, 2, 2, 3, 4, 5], borderColor: '#ec4899', backgroundColor: '#ec4899', tension: 0.3 },
                { label: '수익성 분석', data: [0, 1, 1, 2, 2, 3], borderColor: '#f97316', backgroundColor: '#f97316', tension: 0.3 },
                { label: '기획 피드백', data: [1, 1, 3, 2, 4, 5], borderColor: '#06b6d4', backgroundColor: '#06b6d4', tension: 0.3 },
                { label: '자동 문서화', data: [0, 0, 1, 2, 3, 4], borderColor: '#ef4444', backgroundColor: '#ef4444', tension: 0.3 },
                { label: '시나리오 비교', data: [0, 1, 1, 1, 2, 3], borderColor: '#eab308', backgroundColor: '#eab308', tension: 0.3 }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { 
                    position: 'bottom', 
                    labels: { usePointStyle: true, boxWidth: 8, font: { size: 12 } } 
                }
            },
            scales: {
                y: { beginAtZero: true, grid: { color: '#f1f5f9' }, ticks: { stepSize: 2 } },
                x: { grid: { display: false } }
            }
        }
    });
});