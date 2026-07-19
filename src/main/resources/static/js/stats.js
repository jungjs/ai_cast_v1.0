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
    document.getElementById('dateInput').valueAsDate = new Date();
}

function initTabs() {
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            e.target.classList.add('active');
            activeTab = e.target.dataset.tab;
            loadStats();
        });
    });
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
    dataTable = $('#statsTable').DataTable({
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
    const dateVal = document.getElementById('dateInput').value;
    if(!dateVal) return;

    const govId = document.getElementById('govFilter').value || 'ALL';
    let url = `/api/stats/${activeTab}?govId=${govId}`;
    if(activeTab === 'daily') url += `&date=${dateVal}`;
    else url += `&startDate=${dateVal}&endDate=${dateVal}`; // 단순화를 위해 동일일자 전송 (실제로는 주/월 계산 필요)

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
    let tot=0, ok=0, fail=0;
    data.forEach(d => {
        tot += d.tot_cnt || 0;
        ok += d.ok_cnt || 0;
        fail += d.fail_cnt || 0;
    });

    document.getElementById('stat-tot-cnt').textContent = tot.toLocaleString();
    document.getElementById('stat-ok-cnt').textContent = ok.toLocaleString();
    document.getElementById('stat-fail-cnt').textContent = fail.toLocaleString();
    
    const rateEl = document.getElementById('stat-ok-rate');
    const rate = tot > 0 ? ((ok / tot) * 100).toFixed(1) : 0;
    rateEl.textContent = `${rate}%`;
    
    rateEl.className = 'card-value';
    if(rate >= 95) rateEl.classList.add('text-green');
    else if(rate < 80) rateEl.classList.add('text-red');
}

function updateCharts(data) {
    // 1. SVC Ratio
    const svcData = {};
    data.forEach(d => {
        svcData[d.svc_type] = (svcData[d.svc_type] || 0) + (d.tot_cnt || 0);
    });
    svcRatioChart.data.labels = Object.keys(svcData);
    svcRatioChart.data.datasets[0].data = Object.values(svcData);
    svcRatioChart.data.datasets[0].backgroundColor = Object.keys(svcData).map(k => svcColors[k] || '#999');
    svcRatioChart.update();

    // 2. Ok/Fail Ratio
    let ok=0, fail=0;
    data.forEach(d => { ok+=d.ok_cnt||0; fail+=d.fail_cnt||0; });
    okFailRatioChart.data.datasets[0].data = [ok, fail];
    okFailRatioChart.update();

    // 3. Avg MS (Bar)
    const avgData = {};
    data.forEach(d => {
        if(d.avg_ms) avgData[d.svc_type] = d.avg_ms;
    });
    avgMsChart.data.labels = Object.keys(avgData);
    avgMsChart.data.datasets[0].data = Object.values(avgData);
    avgMsChart.data.datasets[0].backgroundColor = Object.keys(avgData).map(k => svcColors[k] || '#999');
    avgMsChart.update();
}

function updateTable(data) {
    dataTable.clear();
    
    data.forEach(d => {
        const rate = d.tot_cnt > 0 ? ((d.ok_cnt / d.tot_cnt) * 100).toFixed(1) : 0;
        const badgeClass = `svc-badge svc-${d.svc_type.toLowerCase()}`;
        
        dataTable.row.add([
            d.stat_dt || 'N/A',
            `<span class="${badgeClass}">${d.svc_type}</span>`,
            (d.tot_cnt||0).toLocaleString(),
            (d.ok_cnt||0).toLocaleString(),
            (d.fail_cnt||0).toLocaleString(),
            `${rate}%`,
            `${d.avg_ms||0}`,
            formatUsage(d.svc_type, d.tot_tokens)
        ]);
    });
    
    dataTable.draw();
}

function formatUsage(svcType, totTokens) {
    if (totTokens === undefined || totTokens === null || totTokens === 0) return '-';
    const formatted = totTokens.toLocaleString();
    if (svcType === 'NLP') return `${formatted} 토큰`;
    if (svcType === 'TRANSLATE') return `${formatted} 자`;
    if (svcType === 'STT' || svcType === 'OCR') return `${formatted} 자`;
    if (svcType === 'STORAGE') {
        if (totTokens >= 1024 * 1024) return `${(totTokens / (1024 * 1024)).toFixed(1)} MB`;
        if (totTokens >= 1024) return `${(totTokens / 1024).toFixed(1)} KB`;
        return `${formatted} B`;
    }
    return formatted;
}

