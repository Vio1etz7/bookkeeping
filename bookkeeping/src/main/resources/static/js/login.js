// 全局 Toast 提示函数
function showToast(message, type = 'success') {
    const toast = document.getElementById('toast');
    
    // 设置图标和样式
    const icon = type === 'success' ? '<i class="fas fa-check-circle"></i>' : '<i class="fas fa-exclamation-circle"></i>';
    toast.innerHTML = `${icon} ${message}`;
    
    // 清除之前的样式并添加新样式
    toast.className = 'toast'; 
    toast.classList.add(type);
    
    // 显示
    setTimeout(() => {
        toast.classList.add('show');
    }, 10);

    // 3秒后自动隐藏
    setTimeout(() => {
        toast.classList.remove('show');
    }, 3000);
}

document.addEventListener('DOMContentLoaded', function() {
    
    // === 1. 密码可见性切换 (通用逻辑) ===
    // 我们可以使用事件委托，或者直接给所有 toggle-password 绑定
    document.querySelectorAll('.toggle-password').forEach(btn => {
        btn.addEventListener('click', function() {
            const input = this.previousElementSibling; // 获取前一个元素(input)
            if (input.type === 'password') {
                input.type = 'text';
                this.classList.replace('fa-eye-slash', 'fa-eye');
            } else {
                input.type = 'password';
                this.classList.replace('fa-eye', 'fa-eye-slash');
            }
        });
    });

    // === 2. 表单切换动画 (修正版) ===
        const loginForm = document.getElementById('loginForm');
        const registerForm = document.getElementById('registerForm');
        const showRegisterBtn = document.getElementById('showRegister');
        const backToLoginBtn = document.getElementById('backToLogin');

        // 切换到注册页面
        showRegisterBtn.addEventListener('click', (e) => {
            e.preventDefault();

            // 1. 移除登录表单的激活状态（触发 CSS 淡出或隐藏）
            loginForm.classList.remove('active');

            // 2. 等待过渡时间（配合 CSS 动画，这里设为 300ms）
            setTimeout(() => {
                loginForm.style.display = 'none'; // 彻底隐藏登录框

                registerForm.style.display = 'block'; // 准备显示注册框
                // 强制浏览器重绘，确保动画能触发
                void registerForm.offsetWidth;

                registerForm.classList.add('active'); // 添加激活状态（触发 CSS 淡入）
            }, 300);
        });

        // 切换回登录页面
        backToLoginBtn.addEventListener('click', (e) => {
            e.preventDefault();

            // 1. 移除注册表单的激活状态
            registerForm.classList.remove('active');

            // 2. 等待过渡时间
            setTimeout(() => {
                registerForm.style.display = 'none'; // 彻底隐藏注册框

                loginForm.style.display = 'block'; // 准备显示登录框
                // 强制浏览器重绘
                void loginForm.offsetWidth;

                loginForm.classList.add('active'); // 添加激活状态
            }, 300);
        });

    backToLoginBtn.addEventListener('click', (e) => {
        e.preventDefault();
        registerForm.classList.remove('active');
        // 由于CSS动画方向，这里直接切回即可，或者加个延迟
        loginForm.classList.add('active');
    });


    // === 3. 登录逻辑 ===
    document.getElementById('loginFormElement').addEventListener('submit', function(e) {
        e.preventDefault();
        const btn = document.getElementById('loginBtn');
        const originalText = btn.innerHTML;
        
        // 锁定按钮状态
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 登录中...';

        const data = {
            username: document.getElementById('username').value,
            password: document.getElementById('password').value
        };

        fetch('/api/login', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(data)
        })
        .then(res => res.json())
        .then(data => {
            if (data.token && data.token !== "用户名或密码错误") { // 建议后端统一返回格式，这里沿用你的逻辑
                showToast('登录成功，正在跳转...', 'success');
                localStorage.setItem('authToken', data.token);
                setTimeout(() => window.location.href = '/html/dashboard.html', 1000);
            } else {
                showToast(data.token || '用户名或密码错误', 'error');
                btn.disabled = false;
                btn.innerHTML = originalText;
            }
        })
        .catch(err => {
            console.error(err);
            showToast('网络连接失败，请稍后重试', 'error');
            btn.disabled = false;
            btn.innerHTML = originalText;
        });
    });

    // === 4. 注册逻辑 ===
    document.getElementById('registerFormElement').addEventListener('submit', function(e) {
        e.preventDefault();
        
        // 简单的客户端校验
        const pwd = document.getElementById('regPassword').value;
        const confirm = document.getElementById('regConfirmPassword').value;
        
        if(pwd !== confirm) {
            showToast('两次输入的密码不一致', 'error');
            return;
        }

        const btn = document.getElementById('registerBtn');
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 注册中...';

        const data = {
            username: document.getElementById('regUsername').value,
            email: document.getElementById('regEmail').value,
            password: pwd,
            confirmPassword: confirm // 后端可能不需要这个字段，看你Controller怎么写的
        };

        fetch('/api/register', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(data)
        })
        .then(res => res.text()) // 你的后端似乎返回的是字符串 "注册成功"
        .then(msg => {
            if (msg === '注册成功') {
                showToast('注册成功！请登录', 'success');
                setTimeout(() => {
                    document.getElementById('backToLogin').click();
                    document.getElementById('username').value = data.username;
                    // 重置注册按钮
                    btn.disabled = false;
                    btn.innerHTML = '<span>注册账户</span>';
                }, 1500);
            } else {
                showToast(msg, 'error');
                btn.disabled = false;
                btn.innerHTML = '<span>注册账户</span>';
            }
        })
        .catch(err => {
            showToast('注册服务暂不可用', 'error');
            btn.disabled = false;
            btn.innerHTML = '<span>注册账户</span>';
        });
    });

});