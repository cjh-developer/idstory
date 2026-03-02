/* ============================================================
   IDStory - Login Page JavaScript
   src/main/resources/static/js/login.js
   ============================================================ */

'use strict';

/* ── 초기화 링크 복사 버튼 (password-reset-sent.html) ───────── */
function copyResetLink() {
    const urlEl = document.querySelector('.link-url');
    const btnEl = document.querySelector('.btn-copy');
    if (!urlEl) return;
    copyToClipboard(urlEl.href || urlEl.textContent, btnEl, '복사됨');
}

/* ── 비밀번호 폼 초기화 (password-reset-form.html) ──────────── */
document.addEventListener('DOMContentLoaded', function () {

    /* 비밀번호 강도 표시 */
    const pwField = document.getElementById('newPassword');
    if (pwField) {
        pwField.addEventListener('input', function () {
            checkPasswordStrength(this.value, 'strengthFill');
        });
    }

    /* 비밀번호 확인 일치 표시 */
    const confirmField = document.getElementById('confirmPassword');
    if (pwField && confirmField) {
        confirmField.addEventListener('input', function () {
            const isMatch = this.value === pwField.value;
            this.style.borderColor = this.value
                ? (isMatch ? 'var(--green-600)' : 'var(--red-600)')
                : '';
        });
    }
});
