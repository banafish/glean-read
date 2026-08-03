// ==UserScript==
// @name         GleanRead 划词摘录
// @namespace    com.gleanread.android
// @version      1.0.4
// @description  选中网页文本后浮出摘录按钮，点击通过 deep link 直达 GleanRead 快摘弹窗（URL/标题/选中文本随 intent 原子送达）
// @match        *://*/*
// @match        file:///*
// @include      *
// @run-at       document-end
// @grant        none
// ==/UserScript==

(function () {
    'use strict';

    // ========== 配置（真机验证后定稿） ==========
    var CONFIG = {
        // true：Chromium 标准 intent URI 包装（带 package= 防劫持、免确认框）
        // false：gleanread:// scheme 直跳（WebView 壳浏览器不识别 intent: 语法时的降级形式）
        useIntentUri: true,
        // 目标应用包名（intent URI 形式使用）
        packageName: 'com.gleanread.android',
        // 选中文本截断长度：URL 编码后约 18KB，各环节长度安全
        maxTextLength: 6000,
        // 按钮相对选区底部的垂直距离（px）：下方定位避让系统文本选择菜单，被遮挡时可调大
        buttonOffsetY: 24,
        // selectionchange 防抖间隔（ms）
        debounceMillis: 250,
        // 真机调试开关：在浏览器控制台输出 [GleanRead] 前缀日志，定位「按钮不出现」卡在哪一步；定稿后改 false
        debug: true,
    };

    // MVP 不处理 iframe 内选区：仅在顶层 frame 运行
    if (window.top !== window.self) return;
    // 防止脚本被重复注入
    if (window.__gleanWebCaptureInstalled) return;
    window.__gleanWebCaptureInstalled = true;

    function log(message) {
        if (!CONFIG.debug) return;
        try { console.log('[GleanRead] ' + message); } catch (ignored) { }
    }

    var BUTTON_SIZE = 44;
    var button = null;
    var debounceTimer = null;
    // 按钮展示时缓存的选中文本：触摸按钮瞬间选区可能已被浏览器折叠，不能现取
    var capturedText = '';
    // 只在首个选区事件时打一条日志，证明事件通道是通的
    var loggedFirstSelectionEvent = false;

    /** 创建浮动圆钮（惰性单例，内联样式避免污染页面 CSS） */
    function ensureButton() {
        if (button) return button;
        button = document.createElement('div');
        button.textContent = '摘';
        button.setAttribute('style', [
            'position: fixed',
            'z-index: 2147483647',
            'width: ' + BUTTON_SIZE + 'px',
            'height: ' + BUTTON_SIZE + 'px',
            'border-radius: 50%',
            'background: #6750A4',
            'color: #FFFFFF',
            'font-size: 18px',
            'line-height: ' + BUTTON_SIZE + 'px',
            'text-align: center',
            'font-family: sans-serif',
            'box-shadow: 0 2px 8px rgba(0,0,0,0.3)',
            'cursor: pointer',
            'user-select: none',
            '-webkit-user-select: none',
            '-webkit-tap-highlight-color: transparent',
            'display: none',
        ].join(';'));

        // touchstart 阻断默认行为：防止触摸按钮时页面选区被清除、焦点转移
        button.addEventListener('touchstart', function (event) {
            event.preventDefault();
            event.stopPropagation();
        }, { passive: false });
        // touchstart preventDefault 后不会再合成 click，触发逻辑放 touchend
        button.addEventListener('touchend', function (event) {
            event.preventDefault();
            event.stopPropagation();
            triggerCapture();
        }, { passive: false });
        // 桌面/鼠标场景兜底
        button.addEventListener('mousedown', function (event) {
            event.preventDefault();
            event.stopPropagation();
        });
        button.addEventListener('click', function (event) {
            event.preventDefault();
            event.stopPropagation();
            triggerCapture();
        });

        document.documentElement.appendChild(button);
        return button;
    }

    function hideButton() {
        if (button) button.style.display = 'none';
    }

    /** 按钮定位：选区 bounding rect 下方居中，水平/垂直方向钳制在视口内 */
    function showButtonBelow(rect) {
        var target = ensureButton();
        var left = rect.left + rect.width / 2 - BUTTON_SIZE / 2;
        left = Math.max(8, Math.min(left, window.innerWidth - BUTTON_SIZE - 8));
        var top = rect.bottom + CONFIG.buttonOffsetY;
        if (top + BUTTON_SIZE > window.innerHeight - 8) {
            // 视口底部放不下时改到选区上方
            top = rect.top - CONFIG.buttonOffsetY - BUTTON_SIZE;
        }
        top = Math.max(8, Math.min(top, window.innerHeight - BUTTON_SIZE - 8));
        target.style.left = left + 'px';
        target.style.top = top + 'px';
        target.style.display = 'block';
    }

    /** 选区是否落在输入控件里（输入框划词是编辑动作，不弹摘录按钮） */
    function isEditingContext() {
        var active = document.activeElement;
        if (!active) return false;
        var tag = (active.tagName || '').toUpperCase();
        return tag === 'INPUT' || tag === 'TEXTAREA' || active.isContentEditable === true;
    }

    function onSelectionSettled() {
        var selection = window.getSelection();
        if (!selection || selection.isCollapsed || selection.rangeCount === 0) {
            hideButton();
            return;
        }
        if (isEditingContext()) {
            log('hide: selection inside editable element');
            hideButton();
            return;
        }
        var text = selection.toString().trim();
        if (!text) {
            hideButton();
            return;
        }
        var rect = selection.getRangeAt(0).getBoundingClientRect();
        // 部分内核对不可见选区返回全 0 rect，无法定位则不显示
        if (rect.width === 0 && rect.height === 0) {
            log('hide: selection rect is all-zero, cannot position button');
            hideButton();
            return;
        }
        capturedText = text.slice(0, CONFIG.maxTextLength);
        log('selection: ' + capturedText.length + ' chars, show button below rect(' +
            Math.round(rect.left) + ',' + Math.round(rect.bottom) + ')');
        showButtonBelow(rect);
    }

    /** 点击摘录按钮：组装 deep link（url/title/text 全部 encodeURIComponent）并当前页直跳 */
    function triggerCapture() {
        if (!capturedText) return;
        var query = 'url=' + encodeURIComponent(location.href) +
            '&title=' + encodeURIComponent(document.title || '') +
            '&text=' + encodeURIComponent(capturedText);
        var uri = CONFIG.useIntentUri
            ? 'intent://capture?' + query + '#Intent;scheme=gleanread;package=' + CONFIG.packageName + ';end'
            : 'gleanread://capture?' + query;
        capturedText = '';
        hideButton();
        var selection = window.getSelection();
        if (selection) selection.removeAllRanges();
        log('navigate: ' + uri.slice(0, 120) + (uri.length > 120 ? '...' : ''));
        // X浏览器对网页发起的外部应用跳转统一弹「是否允许打开外部应用」确认条且不记住选择，
        // 实测隐藏 iframe、GM_openInTab 路径同样被拦——无法绕过，点「允许」即可完成摘录（已知限制）
        location.href = uri;
    }

    /** 防抖调度选区检查：selectionchange 与 touch/mouse 兜底共用同一入口 */
    function scheduleSelectionCheck(eventType) {
        if (!loggedFirstSelectionEvent) {
            loggedFirstSelectionEvent = true;
            log('first selection-related event received: ' + eventType);
        }
        if (debounceTimer) clearTimeout(debounceTimer);
        debounceTimer = setTimeout(onSelectionSettled, CONFIG.debounceMillis);
    }

    document.addEventListener('selectionchange', function () {
        scheduleSelectionCheck('selectionchange');
    });

    // 兜底：部分 WebView 内核对原生长按选区不派发 selectionchange，
    // 触摸/鼠标手势结束后主动检查一次选区（capture 阶段注册，不受页面 stopPropagation 影响）
    ['touchend', 'mouseup'].forEach(function (type) {
        document.addEventListener(type, function () {
            scheduleSelectionCheck(type);
        }, true);
    });

    // 滚动即隐藏（capture 捕获嵌套滚动容器），下次选区事件再重新定位
    window.addEventListener('scroll', hideButton, { passive: true, capture: true });

    log('script v1.0.4 installed @ ' + location.href.slice(0, 200));
})();
