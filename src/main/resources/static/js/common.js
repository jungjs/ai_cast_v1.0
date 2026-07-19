const StorageKey = "aicast_api_key";

// DOMContentLoaded
document.addEventListener("DOMContentLoaded", () => {
    initApiKeyModal();
    if (window.location.pathname !== "/playground") {
        checkApiKey();
    }
    initActiveNavLink();
});

function initActiveNavLink() {
    const currentPath = window.location.pathname;
    document.querySelectorAll(".nav-links a").forEach(link => {
        const href = link.getAttribute("href");
        if (currentPath === href || (href === "/dashboard" && currentPath === "/")) {
            link.classList.add("active");
        } else {
            link.classList.remove("active");
        }
    });
}

function initApiKeyModal() {
    const modal = document.getElementById("apiKeyModal");
    const btnOpen = document.getElementById("btn-api-key");
    const btnSave = document.getElementById("btnSaveKey");
    const btnCancel = document.getElementById("btnCancelKey");
    const input = document.getElementById("inputApiKey");

    btnOpen.addEventListener("click", () => {
        input.value = localStorage.getItem(StorageKey) || "";
        modal.classList.remove("hidden");
    });

    btnCancel.addEventListener("click", () => {
        modal.classList.add("hidden");
    });

    btnSave.addEventListener("click", () => {
        const val = input.value.trim();
        if(val) {
            localStorage.setItem(StorageKey, val);
            showToast("✅ API Key가 저장되었습니다.");
            modal.classList.add("hidden");
            // API Key 변경 시 화면 새로고침 또는 데이터 리로드
            if(window.onApiKeyChanged) {
                window.onApiKeyChanged();
            } else {
                location.reload();
            }
        }
    });
}

function checkApiKey() {
    if (window.location.pathname === "/playground") return;
    const key = localStorage.getItem(StorageKey);
    if (!key) {
        document.getElementById("apiKeyModal").classList.remove("hidden");
    }
}

function getApiKey() {
    return localStorage.getItem(StorageKey) || "";
}

// Custom Fetch with API Key Header
async function fetchWithAuth(url, options = {}, customKey = null) {
    const key = customKey || getApiKey();
    const headers = {
        ...options.headers,
        "X-API-KEY": key
    };
    
    try {
        const response = await fetch(url, { ...options, headers });
        if(response.status === 401 || response.status === 403) {
            showToast("❌ 인증에 실패했습니다. API Key를 확인해주세요.");
            document.getElementById("apiKeyModal").classList.remove("hidden");
            throw new Error("Unauthorized");
        }
        return response;
    } catch (e) {
        console.error("Fetch error:", e);
        throw e;
    }
}

function showToast(message) {
    const container = document.getElementById("toast-container");
    const toast = document.createElement("div");
    toast.className = "toast";
    toast.textContent = message;
    
    container.appendChild(toast);
    
    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}
