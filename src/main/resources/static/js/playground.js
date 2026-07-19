(function () {
    "use strict";

    let selectedAudioFile = null;
    let selectedImageFile = null;

    document.addEventListener("DOMContentLoaded", function () {
        initGovSelector();
        initTabs();
        initAudioTab();
        initTextTab();
        initImageTab();
    });

    /* ==================== Gov Selector ==================== */
    function initGovSelector() {
        var govSelect = document.getElementById("gov-select");
        var badge = document.getElementById("gov-badge");

        govSelect.addEventListener("change", function () {
            var opt = govSelect.options[govSelect.selectedIndex];
            if (govSelect.value) {
                var govName = opt.textContent;
                badge.textContent = govName + " 연결됨";
                badge.style.color = "var(--success)";
            } else {
                badge.textContent = "";
            }
        });
    }

    function getSelectedApiKey() {
        var govSelect = document.getElementById("gov-select");
        return govSelect.value || "";
    }

    function getSelectedGovName() {
        var govSelect = document.getElementById("gov-select");
        var opt = govSelect.options[govSelect.selectedIndex];
        return opt && govSelect.value ? opt.textContent : "";
    }

    function requireGov() {
        if (!getSelectedApiKey()) {
            showToast("⚠️ 지자체를 먼저 선택해주세요.");
            return false;
        }
        return true;
    }

    /* ==================== Tab Switching ==================== */
    function initTabs() {
        document.querySelectorAll(".tab-btn").forEach(function (btn) {
            btn.addEventListener("click", function () {
                var target = btn.getAttribute("data-tab");

                document.querySelectorAll(".tab-btn").forEach(function (b) {
                    b.classList.remove("active");
                });
                btn.classList.add("active");

                document.querySelectorAll(".tab-panel").forEach(function (p) {
                    p.classList.remove("active");
                });
                document.getElementById("panel-" + target).classList.add("active");

                hideResult();
            });
        });
    }

    /* ==================== Audio Tab ==================== */
    function initAudioTab() {
        var dropZone = document.getElementById("audio-drop-zone");
        var fileInput = document.getElementById("audio-file-input");
        var fileInfo = document.getElementById("audio-file-info");
        var btnSubmit = document.getElementById("btn-audio-submit");
        var btnRemove = document.getElementById("audio-file-remove");

        dropZone.addEventListener("click", function () {
            fileInput.click();
        });

        dropZone.addEventListener("dragover", function (e) {
            e.preventDefault();
            dropZone.classList.add("dragover");
        });

        dropZone.addEventListener("dragleave", function () {
            dropZone.classList.remove("dragover");
        });

        dropZone.addEventListener("drop", function (e) {
            e.preventDefault();
            dropZone.classList.remove("dragover");
            if (e.dataTransfer.files.length > 0) {
                setAudioFile(e.dataTransfer.files[0]);
            }
        });

        fileInput.addEventListener("change", function () {
            if (fileInput.files.length > 0) {
                setAudioFile(fileInput.files[0]);
            }
        });

        btnRemove.addEventListener("click", function () {
            clearAudioFile();
        });

        btnSubmit.addEventListener("click", function () {
            if (!selectedAudioFile) return;
            var langs = getSelectedLangs("audio-lang-chips");
            submitAudio(selectedAudioFile, langs);
        });
    }

    function setAudioFile(file) {
        selectedAudioFile = file;
        var info = document.getElementById("audio-file-info");
        info.querySelector(".file-name").textContent = file.name + " (" + formatSize(file.size) + ")";
        info.classList.remove("hidden");
        document.getElementById("audio-drop-zone").classList.add("hidden");
        document.getElementById("btn-audio-submit").disabled = false;
    }

    function clearAudioFile() {
        selectedAudioFile = null;
        document.getElementById("audio-file-info").classList.add("hidden");
        document.getElementById("audio-drop-zone").classList.remove("hidden");
        document.getElementById("btn-audio-submit").disabled = true;
        document.getElementById("audio-file-input").value = "";
    }

    async function submitAudio(file, langs) {
        if (!requireGov()) return;
        showLoading("음성 파일 분석 중...");
        try {
            var formData = new FormData();
            formData.append("file", file);
            formData.append("langs", langs.join(","));

            var res = await fetchWithAuth("/api/process_audio", {
                method: "POST",
                body: formData
            }, getSelectedApiKey());
            var data = await res.json();
            showResult(data);
        } catch (e) {
            showError(e.message || "요청 처리 중 오류가 발생했습니다.");
        } finally {
            hideLoading();
        }
    }

    /* ==================== Text Tab ==================== */
    function initTextTab() {
        var textInput = document.getElementById("text-input");
        var btnSubmit = document.getElementById("btn-text-submit");
        var dropZone = document.getElementById("text-drop-zone");
        var fileInput = document.getElementById("text-file-input");
        var btnRemove = document.getElementById("text-file-remove");

        dropZone.addEventListener("click", function () {
            fileInput.click();
        });

        dropZone.addEventListener("dragover", function (e) {
            e.preventDefault();
            dropZone.classList.add("dragover");
        });

        dropZone.addEventListener("dragleave", function () {
            dropZone.classList.remove("dragover");
        });

        dropZone.addEventListener("drop", function (e) {
            e.preventDefault();
            dropZone.classList.remove("dragover");
            if (e.dataTransfer.files.length > 0) {
                handleTextFile(e.dataTransfer.files[0]);
            }
        });

        fileInput.addEventListener("change", function () {
            if (fileInput.files.length > 0) {
                handleTextFile(fileInput.files[0]);
            }
        });

        btnRemove.addEventListener("click", function () {
            clearTextFile();
        });

        textInput.addEventListener("input", function () {
            btnSubmit.disabled = textInput.value.trim().length === 0;
        });

        btnSubmit.addEventListener("click", function () {
            var text = textInput.value.trim();
            if (!text) return;
            var langs = getSelectedLangs("text-lang-chips");
            submitText(text, langs);
        });
     }

     function handleTextFile(file) {
         var reader = new FileReader();
         reader.onload = function (e) {
             var content = e.target.result;
             document.getElementById("text-input").value = content;
             document.getElementById("btn-text-submit").disabled = content.trim().length === 0;

             var info = document.getElementById("text-file-info");
             info.querySelector(".file-name").textContent = file.name + " (" + formatSize(file.size) + ")";
             info.classList.remove("hidden");
             document.getElementById("text-drop-zone").classList.add("hidden");
         };
         reader.readAsText(file, "UTF-8");
     }

     function clearTextFile() {
         document.getElementById("text-file-info").classList.add("hidden");
         document.getElementById("text-drop-zone").classList.remove("hidden");
         document.getElementById("text-file-input").value = "";
         document.getElementById("text-input").value = "";
         document.getElementById("btn-text-submit").disabled = true;
     }

    async function submitText(text, langs) {
        if (!requireGov()) return;
        showLoading("텍스트 분석 중...");
        try {
            var params = new URLSearchParams();
            params.append("text", text);
            params.append("langs", langs.join(","));

            var res = await fetchWithAuth("/api/process_text", {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: params.toString()
            }, getSelectedApiKey());
            var data = await res.json();
            showResult(data);
        } catch (e) {
            showError(e.message || "요청 처리 중 오류가 발생했습니다.");
        } finally {
            hideLoading();
        }
    }

    /* ==================== Image Tab ==================== */
    function initImageTab() {
        var dropZone = document.getElementById("image-drop-zone");
        var fileInput = document.getElementById("image-file-input");
        var fileInfo = document.getElementById("image-file-info");
        var btnSubmit = document.getElementById("btn-image-submit");
        var btnRemove = document.getElementById("image-file-remove");

        dropZone.addEventListener("click", function () {
            fileInput.click();
        });

        dropZone.addEventListener("dragover", function (e) {
            e.preventDefault();
            dropZone.classList.add("dragover");
        });

        dropZone.addEventListener("dragleave", function () {
            dropZone.classList.remove("dragover");
        });

        dropZone.addEventListener("drop", function (e) {
            e.preventDefault();
            dropZone.classList.remove("dragover");
            if (e.dataTransfer.files.length > 0) {
                setImageFile(e.dataTransfer.files[0]);
            }
        });

        fileInput.addEventListener("change", function () {
            if (fileInput.files.length > 0) {
                setImageFile(fileInput.files[0]);
            }
        });

        btnRemove.addEventListener("click", function () {
            clearImageFile();
        });

        btnSubmit.addEventListener("click", function () {
            if (!selectedImageFile) return;
            var langs = getSelectedLangs("image-lang-chips");
            submitImage(selectedImageFile, langs);
        });
    }

    function setImageFile(file) {
        selectedImageFile = file;
        var info = document.getElementById("image-file-info");
        info.querySelector(".file-name").textContent = file.name + " (" + formatSize(file.size) + ")";
        info.classList.remove("hidden");
        document.getElementById("image-drop-zone").classList.add("hidden");
        document.getElementById("btn-image-submit").disabled = false;
    }

    function clearImageFile() {
        selectedImageFile = null;
        document.getElementById("image-file-info").classList.add("hidden");
        document.getElementById("image-drop-zone").classList.remove("hidden");
        document.getElementById("btn-image-submit").disabled = true;
        document.getElementById("image-file-input").value = "";
    }

    async function submitImage(file, langs) {
        if (!requireGov()) return;
        showLoading("이미지 분석 중...");
        try {
            var formData = new FormData();
            formData.append("file", file);
            formData.append("langs", langs.join(","));

            var res = await fetchWithAuth("/api/process_img", {
                method: "POST",
                body: formData
            }, getSelectedApiKey());
            var data = await res.json();
            showResult(data);
        } catch (e) {
            showError(e.message || "요청 처리 중 오류가 발생했습니다.");
        } finally {
            hideLoading();
        }
    }

    /* ==================== Helpers ==================== */
    function getSelectedLangs(containerId) {
        var chips = document.querySelectorAll("#" + containerId + " input:checked");
        var langs = [];
        chips.forEach(function (c) { langs.push(c.value); });
        return langs.length > 0 ? langs : ["en"];
    }

    function formatSize(bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
        return (bytes / (1024 * 1024)).toFixed(1) + " MB";
    }

    function showLoading(msg) {
        document.getElementById("loading-msg").textContent = msg || "처리 중...";
        var overlay = document.getElementById("loading-overlay");
        overlay.classList.remove("hidden");
        overlay.style.display = "flex";
    }

    function hideLoading() {
        var overlay = document.getElementById("loading-overlay");
        overlay.classList.add("hidden");
        overlay.style.display = "none";
    }

    function hideResult() {
        var area = document.getElementById("result-area");
        area.classList.add("hidden");
        area.style.display = "none";
    }

    function showResult(data) {
        var area = document.getElementById("result-area");
        area.classList.remove("hidden");
        area.style.display = "block";

        /* Status badge */
        var statusEl = document.getElementById("result-status");
        if (data.status === "SUCCESS") {
            statusEl.textContent = "SUCCESS";
            statusEl.className = "badge badge-success";
        } else {
            statusEl.textContent = data.status || "FAILED";
            statusEl.className = "badge badge-fail";
        }

        /* Processing times */
        var timesEl = document.getElementById("processing-times");
        timesEl.innerHTML = "";
        if (data.processingTimesMs) {
            Object.keys(data.processingTimesMs).forEach(function (key) {
                var chip = document.createElement("div");
                chip.className = "time-chip";
                chip.innerHTML = key + ": <span>" + data.processingTimesMs[key] + "ms</span>";
                timesEl.appendChild(chip);
            });
        }

        /* Original text */
        var origEl = document.getElementById("original-text");
        origEl.textContent = data.originalText || "-";
        toggleSection("result-original", !!data.originalText);

        /* Refined text */
        var refinedEl = document.getElementById("refined-text");
        refinedEl.textContent = data.refinedText || "-";
        toggleSection("result-refined", !!data.refinedText);

        /* Summary */
        var summaryEl = document.getElementById("summary-text");
        summaryEl.textContent = data.summary || "-";
        toggleSection("result-summary", !!data.summary);

        /* Translations */
        var transEl = document.getElementById("translations-body");
        transEl.innerHTML = "";
        if (data.translations && Object.keys(data.translations).length > 0) {
            Object.keys(data.translations).forEach(function (lang) {
                var item = document.createElement("div");
                item.className = "trans-item";
                item.innerHTML = '<div class="trans-lang">' + escapeHtml(lang) + '</div>' +
                    '<div class="trans-text">' + escapeHtml(data.translations[lang]) + '</div>';
                transEl.appendChild(item);
            });
            toggleSection("result-translations", true);
        } else {
            toggleSection("result-translations", false);
        }

        /* Error */
        var errorBox = document.getElementById("result-error");
        if (data.errorMessage) {
            document.getElementById("error-message").textContent = data.errorMessage;
            errorBox.classList.remove("hidden");
        } else {
            errorBox.classList.add("hidden");
        }

        /* Scroll to result */
        area.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    function toggleSection(id, show) {
        var el = document.getElementById(id);
        if (show) {
            el.style.display = "";
        } else {
            el.style.display = "none";
        }
    }

    function showError(msg) {
        showToast("❌ " + msg);
    }

    function escapeHtml(str) {
        var div = document.createElement("div");
        div.appendChild(document.createTextNode(str));
        return div.innerHTML;
    }
})();
