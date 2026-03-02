/* ============================================================
   IDStory - Common JavaScript
   src/main/resources/static/js/common.js
   ============================================================ */

'use strict';

/* ── 클립보드 복사 ───────────────────────────────────────────── */
function copyToClipboard(text, btnEl, successLabel) {
    if (!navigator.clipboard) {
        // Fallback for older browsers
        const ta = document.createElement('textarea');
        ta.value = text;
        ta.style.position = 'fixed';
        ta.style.opacity = '0';
        document.body.appendChild(ta);
        ta.focus(); ta.select();
        try { document.execCommand('copy'); } catch (e) { console.error('Copy failed', e); }
        document.body.removeChild(ta);
        return;
    }

    navigator.clipboard.writeText(text).then(() => {
        if (!btnEl) return;
        const original = btnEl.innerHTML;
        btnEl.innerHTML = '<i class="fas fa-check"></i> ' + (successLabel || '복사됨');
        btnEl.style.background = '#2563eb';
        btnEl.style.color = '#fff';
        setTimeout(() => {
            btnEl.innerHTML = original;
            btnEl.style.background = '';
            btnEl.style.color = '';
        }, 2000);
    });
}

/* ── 비밀번호 표시/숨기기 ────────────────────────────────────── */
function togglePassword(fieldId, btnEl) {
    const field = document.getElementById(fieldId);
    if (!field) return;
    const icon = btnEl ? btnEl.querySelector('i') : null;
    if (field.type === 'password') {
        field.type = 'text';
        if (icon) icon.className = 'fas fa-eye-slash';
    } else {
        field.type = 'password';
        if (icon) icon.className = 'fas fa-eye';
    }
}

/* ── 비밀번호 강도 체크 ──────────────────────────────────────── */
function checkPasswordStrength(password, fillId) {
    const fill = document.getElementById(fillId);
    if (!fill) return;

    let score = 0;
    if (password.length >= 4)          score++;
    if (password.length >= 8)          score++;
    if (/[A-Z]/.test(password))        score++;
    if (/[0-9]/.test(password))        score++;
    if (/[^A-Za-z0-9]/.test(password)) score++;

    const pct   = (score / 5) * 100;
    const color = score <= 1 ? '#ef4444' : score <= 3 ? '#f59e0b' : '#22c55e';
    fill.style.width      = pct + '%';
    fill.style.background = color;
}

/* ── 모바일 사이드바 토글 ────────────────────────────────────── */
function toggleSidebar() {
    const sidebar = document.querySelector('.app-sidebar');
    if (sidebar) sidebar.classList.toggle('open');
}

/* ── CSRF 헤더 설정 (Ajax용) ─────────────────────────────────── */
function getCsrfToken() {
    const meta = document.querySelector('meta[name="_csrf"]');
    return meta ? meta.getAttribute('content') : '';
}
function getCsrfHeader() {
    const meta = document.querySelector('meta[name="_csrf_header"]');
    return meta ? meta.getAttribute('content') : 'X-CSRF-TOKEN';
}

/* ── 페이지 로드 후 초기화 ───────────────────────────────────── */
document.addEventListener('DOMContentLoaded', function () {
    const currentPath = window.location.pathname;

    // ── 트리 메뉴 토글 ───────────────────────────────────────────
    document.querySelectorAll('.tree-toggle').forEach(function (btn) {
        btn.addEventListener('click', function () {
            const item = this.closest('.tree-item');
            if (item) item.classList.toggle('open');
        });
    });

    // ── 현재 URL로 활성 항목 감지 ────────────────────────────────
    // 직접 링크 (하위 없음)
    document.querySelectorAll('.tree-direct-link').forEach(function (link) {
        const href = link.getAttribute('href');
        if (href && currentPath === href) {
            link.classList.add('active');
        }
    });

    // 하위 링크 — 활성 표시 + 부모 자동 펼침
    document.querySelectorAll('.tree-sublink').forEach(function (link) {
        const href = link.getAttribute('href');
        if (href && href !== '#' && currentPath.startsWith(href)) {
            link.classList.add('active');
            const treeItem = link.closest('.tree-item');
            if (treeItem) treeItem.classList.add('open');
        }
    });

    // ── 바깥 클릭 시 모바일 사이드바 닫기 ────────────────────────
    document.addEventListener('click', function (e) {
        const sidebar = document.querySelector('.app-sidebar');
        const toggle  = document.querySelector('.sidebar-toggle');
        if (sidebar && sidebar.classList.contains('open')) {
            if (!sidebar.contains(e.target) && (!toggle || !toggle.contains(e.target))) {
                sidebar.classList.remove('open');
            }
        }
    });
});
