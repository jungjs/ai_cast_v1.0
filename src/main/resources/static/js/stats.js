let activeTab = 'daily';
let dataTable;

let trendChart, svcRatioChart, okFailRatioChart, avgMsChart;

const svcColors = {
    'STT': '#3498db',
    'NLP': '#2ecc71',
    'TRANSLATE': '#e74c3c',
    'OCR': '#f39c12',
    'IMAGE_GEN': '#9b59b6'
};

document.addEventListener("DOMContentLoaded", () => {
    initDateInput();
    initTabs();
    initCharts();
    initTable();
    
    document.getElementById("btnSearch").addEventListener("click", loadStats);
    document.getElementById("govFilter").addEventListener("change", loadStats);

    if(getApiKey()) {
        loadStats();
    }
});

window.onApiKeyChanged = () => loadStats();

function initDateInput() {
    const startInput = document.getElementById('startDateInput');
    const endInput = document.getElementById('endDateInput');
    if (startInput && endInput) {
        const today = new Date();
        const yyyy = today.getFullYear();
        const mm = String(today.getMonth() + 1).padStart(2, '0');
        const dd = String(today.getDate()).padStart(2, '0');
        const todayStr = `${yyyy}-${mm}-${dd}`;
        
        startInput.setAttribute('max', todayStr);
        endInput.setAttribute('max', todayStr);
    }
}

function initTabs() {
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            e.target.classList.add('active');
            activeTab = e.target.dataset.tab;
            
            toggleDateInputs();
            loadStats();
        });
    });
    
    toggleDateInputs();
}

function formatDateLocal(date) {
    const yyyy = date.getFullYear();
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const dd = String(date.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
}

function toggleDateInputs() {
    const startInput = document.getElementById('startDateInput');
    const endInput = document.getElementById('endDateInput');
    const sep = document.getElementById('dateRangeSeparator');
    if (startInput && endInput && sep) {
        const today = new Date();
        const yyyy = today.getFullYear();
        
        if (activeTab === 'daily') {
            endInput.style.display = 'none';
            sep.style.display = 'none';
            
            startInput.value = formatDateLocal(today);
            endInput.value = formatDateLocal(today);
        } else if (activeTab === 'weekly') {
            endInput.style.display = 'inline-block';
            sep.style.display = 'inline-block';
            
            const thisMonth1st = new Date(yyyy, today.getMonth(), 1);
            startInput.value = formatDateLocal(thisMonth1st);
            endInput.value = formatDateLocal(today);
        } else if (activeTab === 'monthly') {
            endInput.style.display = 'inline-block';
            sep.style.display = 'inline-block';
            
            const thisYear1st = new Date(yyyy, 0, 1);
            startInput.value = formatDateLocal(thisYear1st);
            endInput.value = formatDateLocal(today);
        }
    }
}

function initCharts() {
    const commonOptions = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { labels: { color: '#94a3b8' } } }
    };

    trendChart = new Chart(document.getElementById('trendChart').getContext('2d'), {
        type: 'line',
        data: { labels: [], datasets: [] },
        options: { ...commonOptions, scales: { y: { beginAtZero: true } } }
    });

    svcRatioChart = new Chart(document.getElementById('svcRatioChart').getContext('2d'), {
        type: 'doughnut',
        data: { labels: [], datasets: [{ data: [], backgroundColor: Object.values(svcColors) }] },
        options: { ...commonOptions, cutout: '70%' }
    });

    okFailRatioChart = new Chart(document.getElementById('okFailRatioChart').getContext('2d'), {
        type: 'doughnut',
        data: { labels: ['성공', '실패'], datasets: [{ data: [], backgroundColor: ['#10b981', '#ef4444'] }] },
        options: { ...commonOptions, cutout: '70%' }
    });

    avgMsChart = new Chart(document.getElementById('avgMsChart').getContext('2d'), {
        type: 'bar',
        data: { labels: [], datasets: [{ data: [], backgroundColor: Object.values(svcColors) }] },
        options: { 
            ...commonOptions, 
            indexAxis: 'y', 
            plugins: { legend: { display: false } },
            scales: { x: { beginAtZero: true } }
        }
    });
}

function initTable() {
    apiTable = $('#apiStatsTable').DataTable({
        pageLength: 10,
        lengthChange: false,
        searching: true,
        ordering: true,
        info: true,
        language: {
            url: '//cdn.datatables.net/plug-ins/1.13.6/i18n/ko.json',
        }
    });

    aiTable = $('#aiStatsTable').DataTable({
        pageLength: 10,
        lengthChange: false,
        searching: true,
        ordering: true,
        info: true,
        language: {
            url: '//cdn.datatables.net/plug-ins/1.13.6/i18n/ko.json',
        }
    });
}

async function loadStats() {
    const startEl = document.getElementById('startDateInput');
    const endEl = document.getElementById('endDateInput');
    if (!startEl) return;

    const startDateVal = startEl.value;
    const endDateVal = endEl ? endEl.value : null;
    if(!startDateVal) return;

    const today = new Date();
    today.setHours(0,0,0,0);
    
    const startD = new Date(startDateVal);
    startD.setHours(0,0,0,0);
    
    if (startD > today) {
        showToast("시작일은 미래 날짜를 선택할 수 없습니다.", "error");
        startEl.value = formatDateLocal(today);
        return;
    }
    
    if (activeTab !== 'daily' && endDateVal) {
        const endD = new Date(endDateVal);
        endD.setHours(0,0,0,0);
        
        if (endD > today) {
            showToast("종료일은 미래 날짜를 선택할 수 없습니다.", "error");
            if (endEl) endEl.value = formatDateLocal(today);
            return;
        }
        
        if (startD > endD) {
            showToast("시작일이 종료일보다 클 수 없습니다.", "error");
            return;
        }
    }

    const govId = document.getElementById('govFilter').value || 'ALL';
    let url = `/api/stats/${activeTab}?govId=${govId}`;
    if(activeTab === 'daily') {
        url += `&date=${startDateVal}`;
    } else {
        url += `&startDate=${startDateVal}&endDate=${endDateVal}`;
    }

    try {
        const res = await fetchWithAuth(url);
        const data = await res.json();
        
        updateSummaryCards(data);
        updateCharts(data);
        updateTable(data);
    } catch(e) {
        console.error("Load stats failed", e);
    }
}

function updateSummaryCards(data) {
    // 1) API 요약 카드 셋팅
    let apiTot=0, apiOk=0, apiFail=0;
    if (data.apiStats && data.apiStats.length > 0) {
        data.apiStats.forEach(d => {
            apiTot += d.tot_cnt || 0;
            apiOk += d.ok_cnt || 0;
            apiFail += d.fail_cnt || 0;
        });
    }
    document.getElementById('api-tot-cnt').textContent = apiTot.toLocaleString();
    document.getElementById('api-ok-cnt').textContent = apiOk.toLocaleString();
    document.getElementById('api-fail-cnt').textContent = apiFail.toLocaleString();
    const apiRate = apiTot > 0 ? ((apiOk / apiTot) * 100).toFixed(1) : 0;
    document.getElementById('api-ok-rate').textContent = `${apiRate}%`;

    // 2) AI 서비스 요약 카드 셋팅
    let aiTot=0, aiOk=0, aiFail=0;
    if (data.aiStats && data.aiStats.length > 0) {
        data.aiStats.forEach(d => {
            aiTot += d.tot_cnt || 0;
            aiOk += d.ok_cnt || 0;
            aiFail += d.fail_cnt || 0;
        });
    }
    document.getElementById('ai-tot-cnt').textContent = aiTot.toLocaleString();
    document.getElementById('ai-ok-cnt').textContent = aiOk.toLocaleString();
    document.getElementById('ai-fail-cnt').textContent = aiFail.toLocaleString();
    const aiRate = aiTot > 0 ? ((aiOk / aiTot) * 100).toFixed(1) : 0;
    document.getElementById('ai-ok-rate').textContent = `${aiRate}%`;
}

function updateCharts(data) {
    const trendList = data.trendStats || [];
    const aiList = data.aiStats || [];

    // 1. Service Call Trend (Line Chart)
    const timeLabels = [...new Set(trendList.map(d => d.time_label || 'N/A'))].sort();
    const svcTypesForTrend = [...new Set(trendList.map(d => d.svc_type))];
    const trendDatasets = svcTypesForTrend.map(svcType => {
        const trendData = timeLabels.map(tl => {
            const matches = trendList.filter(d => (d.time_label || 'N/A') === tl && d.svc_type === svcType);
            return matches.reduce((sum, curr) => sum + (curr.tot_cnt || 0), 0);
        });
        return {
            label: svcType,
            data: trendData,
            borderColor: svcColors[svcType] || '#999',
            backgroundColor: (svcColors[svcType] || '#999') + '33',
            borderWidth: 2,
            tension: 0.3,
            fill: false
        };
    });
    trendChart.data.labels = timeLabels;
    trendChart.data.datasets = trendDatasets;
    trendChart.update();

    // 2. SVC Ratio
    const svcData = {};
    aiList.forEach(d => {
        svcData[d.svc_type] = (svcData[d.svc_type] || 0) + (d.tot_cnt || 0);
    });
    svcRatioChart.data.labels = Object.keys(svcData);
    svcRatioChart.data.datasets[0].data = Object.values(svcData);
    svcRatioChart.data.datasets[0].backgroundColor = Object.keys(svcData).map(k => svcColors[k] || '#999');
    svcRatioChart.update();

    // 3. Ok/Fail Ratio
    let ok=0, fail=0;
    aiList.forEach(d => { ok+=d.ok_cnt||0; fail+=d.fail_cnt||0; });
    okFailRatioChart.data.datasets[0].data = [ok, fail];
    okFailRatioChart.update();

    // 4. Avg MS (Bar)
    const avgData = {};
    aiList.forEach(d => {
        if(d.avg_ms) avgData[d.svc_type] = d.avg_ms;
    });
    avgMsChart.data.labels = Object.keys(avgData);
    avgMsChart.data.datasets[0].data = Object.values(avgData);
    avgMsChart.data.datasets[0].backgroundColor = Object.keys(avgData).map(k => svcColors[k] || '#999');
    avgMsChart.update();
}

function updateTable(data) {
    // 1) API 호출 상세 테이블
    apiTable.clear();
    if (data.apiStats && data.apiStats.length > 0) {
        data.apiStats.forEach(d => {
            const rate = d.tot_cnt > 0 ? ((d.ok_cnt / d.tot_cnt) * 100).toFixed(1) : 0;
            apiTable.row.add([
                d.endpoint || 'N/A',
                (d.tot_cnt||0).toLocaleString(),
                (d.ok_cnt||0).toLocaleString(),
                (d.fail_cnt||0).toLocaleString(),
                `${rate}%`
            ]);
        });
    }
    apiTable.draw();

    // 2) AI 서비스 상세 테이블
    aiTable.clear();
    if (data.aiStats && data.aiStats.length > 0) {
        data.aiStats.forEach(d => {
            const rate = d.tot_cnt > 0 ? ((d.ok_cnt / d.tot_cnt) * 100).toFixed(1) : 0;
            const badgeClass = `svc-badge svc-${d.svc_type.toLowerCase()}`;
            
            aiTable.row.add([
                `<span class="${badgeClass}">${d.svc_type}</span>`,
                (d.tot_cnt||0).toLocaleString(),
                (d.ok_cnt||0).toLocaleString(),
                (d.fail_cnt||0).toLocaleString(),
                `${rate}%`,
                `${d.avg_ms||0}`,
                formatUsage(d.svc_type, d.tot_tokens)
            ]);
        });
    }
    aiTable.draw();
}

function formatUsage(svcType, totTokens) {
    if (totTokens === undefined || totTokens === null || totTokens === 0) return '-';
    const formatted = totTokens.toLocaleString();
    if (svcType === 'NLP') return `${formatted} 토큰`;
    if (svcType === 'TRANSLATE') return `${formatted} 자`;
    if (svcType === 'STT') return `${formatted} 초`;
    if (svcType === 'OCR') return `${formatted} 건`;
    if (svcType === 'STORAGE') {
        if (totTokens >= 1024 * 1024 * 1024) return `${(totTokens / (1024 * 1024 * 1024)).toFixed(1)} GB`;
        if (totTokens >= 1024 * 1024) return `${(totTokens / (1024 * 1024)).toFixed(1)} MB`;
        if (totTokens >= 1024) return `${(totTokens / 1024).toFixed(1)} KB`;
        return `${formatted} B`;
    }
    return formatted;
}

