// 格式化日期
function formatDate(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
}

// 显示消息 (增强版)
function showMessage(containerOrId, message, type) {
    let container = containerOrId;

    // 1. 兼容性处理：如果传入的是字符串ID，自动获取元素
    if (typeof containerOrId === 'string') {
        container = document.getElementById(containerOrId);
    }

    // 如果找不到元素，在控制台打印错误并返回，防止程序崩溃
    if (!container) {
        console.error('showMessage 错误: 找不到消息容器元素', containerOrId);
        return;
    }

    // 2. 设置图标 (可选)
    let iconHtml = '';
    if (type === 'success') {
        iconHtml = '<i class="fas fa-check-circle" style="margin-right:8px"></i>';
    } else if (type === 'error') {
        iconHtml = '<i class="fas fa-exclamation-circle" style="margin-right:8px"></i>';
    }

    // 3. 使用 innerHTML 支持图标
    container.innerHTML = iconHtml + message;

    // 4. 设置样式类名 (对应 CSS 中的 alert 样式)
    // 注意：这里要把之前的 class 覆盖掉，防止样式冲突
    if (type === 'error') {
        container.className = 'message-container alert alert-danger';
    } else if (type === 'success') {
        container.className = 'message-container alert alert-success';
    } else {
        container.className = 'message-container alert alert-info';
    }

    // 5. 显示元素
    container.style.display = 'block';

    // 6. 3秒后自动消失 (可选，提升体验)
    setTimeout(() => {
        container.style.display = 'none';
    }, 3000);
}

// 隐藏消息
function hideMessage(containerOrId) {
    let container = containerOrId;
    if (typeof containerOrId === 'string') {
        container = document.getElementById(containerOrId);
    }
    if (container) {
        container.style.display = 'none';
    }
}

// 导出到全局
window.formatDate = formatDate;
window.showMessage = showMessage;
window.hideMessage = hideMessage;