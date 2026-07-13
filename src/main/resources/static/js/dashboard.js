// 글로벌 차트 기본 설정 (Dark Mode 대응)
Chart.defaults.color = '#94a3b8';
Chart.defaults.borderColor = 'rgba(255,255,255,0.1)';
Chart.defaults.font.family = "'Inter', sans-serif";

const MAX_DATA_POINTS = 60; // 5초 간격으로 5분 (60개)
let cpuChart, memChart, netChart, diskChart;

window.onApiKeyChanged = () => {
    // API Key 변경 시 초기 데이터 강제 로드
    fetchResources();
    // API 현황 통계가 구현되어 있다면 여기 추가
};

document.addEventListener("DOMContentLoaded", () => {
    initCharts();
    
    // 폴링 시작 (API Key 있을 때만)
    if(getApiKey()) {
        fetchResources();
        setInterval(fetchResources, 5000); // 5초
    }
});

function initCharts() {
    const commonOptions = {
        responsive: true,
        maintainAspectRatio: false,
        animation: { duration: 0 },
        elements: {
            point: { radius: 0 },
            line: { tension: 0.4, borderWidth: 2 }
        },
        scales: {
            x: { display: false },
            y: { beginAtZero: true }
        },
        plugins: {
            legend: { display: false }
        }
    };

    // 1. CPU
    const ctxCpu = document.getElementById('cpuChart').getContext('2d');
    cpuChart = new Chart(ctxCpu, {
        type: 'line',
        data: { labels: [], datasets: [{ data: [], borderColor: '#3b82f6', backgroundColor: 'rgba(59, 130, 246, 0.1)', fill: true }] },
        options: { ...commonOptions, scales: { y: { min: 0, max: 100 } } }
    });

    // 2. Memory
    const ctxMem = document.getElementById('memChart').getContext('2d');
    memChart = new Chart(ctxMem, {
        type: 'line',
        data: { labels: [], datasets: [{ data: [], borderColor: '#10b981', backgroundColor: 'rgba(16, 185, 129, 0.1)', fill: true }] },
        options: { ...commonOptions }
    });

    // 3. Network
    const ctxNet = document.getElementById('netChart').getContext('2d');
    netChart = new Chart(ctxNet, {
        type: 'line',
        data: { labels: [], datasets: [{ data: [], borderColor: '#f59e0b', fill: true }] },
        options: { ...commonOptions }
    });

    // 4. Disk
    const ctxDisk = document.getElementById('diskChart').getContext('2d');
    diskChart = new Chart(ctxDisk, {
        type: 'line',
        data: { labels: [], datasets: [{ data: [], borderColor: '#ef4444', fill: true }] },
        options: { ...commonOptions }
    });
}

async function fetchResources() {
    try {
        const res = await fetchWithAuth('/api/monitor/resources?limit=1');
        const data = await res.json();
        
        if(data && data.length > 0) {
            updateCharts(data[0]);
            updateAlertBanner(data[0]);
        }
    } catch(e) {
        // 인증 실패 등 에러 무시 (콘솔 로깅)
    }
}

function updateCharts(latest) {
    const timeLabel = new Date().toLocaleTimeString();

    function pushData(chart, val) {
        chart.data.labels.push(timeLabel);
        chart.data.datasets[0].data.push(val);
        if (chart.data.labels.length > MAX_DATA_POINTS) {
            chart.data.labels.shift();
            chart.data.datasets[0].data.shift();
        }
        chart.update();
    }

    pushData(cpuChart, latest.cpu_pct || 0);
    pushData(memChart, latest.mem_mb || 0);
    pushData(netChart, latest.net_rx || 0);
    pushData(diskChart, latest.disk_rd || 0);

    // Update Badges
    const cpuBadge = document.getElementById('cpu-badge');
    cpuBadge.textContent = `${(latest.cpu_pct||0).toFixed(1)}%`;
    if(latest.cpu_pct >= 90) cpuBadge.className = 'badge crit';
    else if(latest.cpu_pct >= 75) cpuBadge.className = 'badge warn';
    else cpuBadge.className = 'badge';

    const memBadge = document.getElementById('mem-badge');
    const memPct = (latest.mem_mb / (latest.mem_lmt_mb||1)) * 100;
    memBadge.textContent = `${latest.mem_mb} MB (${memPct.toFixed(1)}%)`;
    if(memPct >= 90) memBadge.className = 'badge crit';
    else if(memPct >= 80) memBadge.className = 'badge warn';
    else memBadge.className = 'badge';
}

function updateAlertBanner(latest) {
    const banner = document.getElementById('alert-banner');
    const msgEl = document.getElementById('alert-msg');
    const timeEl = document.getElementById('alert-time');

    let isAlert = false;
    let msg = "";
    let levelClass = "";

    const memPct = (latest.mem_mb / (latest.mem_lmt_mb||1)) * 100;

    if (latest.cpu_pct >= 90) {
        isAlert = true; msg = `[CRITICAL] CPU 사용률이 ${latest.cpu_pct}%에 도달했습니다.`; levelClass = 'level-critical';
    } else if (memPct >= 90) {
        isAlert = true; msg = `[CRITICAL] 메모리 사용률이 ${memPct.toFixed(1)}%에 도달했습니다.`; levelClass = 'level-critical';
    }

    if (isAlert) {
        msgEl.textContent = msg;
        timeEl.textContent = new Date().toLocaleTimeString();
        banner.className = `alert-banner glass-card ${levelClass}`;
    } else {
        banner.classList.add('hidden');
    }
}
