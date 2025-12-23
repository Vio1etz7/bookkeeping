console.log(">>> dashboard.js 文件已加载");

// DOM元素
const billForm = document.getElementById('billForm');
const billsList = document.getElementById('billsList');
const editModal = document.getElementById('editModal');
const editBillForm = document.getElementById('editBillForm');
const closeButtons = document.querySelectorAll('.close, #cancelEdit');
const saveEditBtn = document.getElementById('saveEdit');
const logoutBtn = document.getElementById('logoutBtn');
const periodFilter = document.getElementById('periodFilter');



const analysisSection = document.getElementById('analysis');
let incomeChart = null;
let expenseChart = null;
let trendChart = null;

let categories ={
    INCOME:[],
    EXPENSE:[]
}

let editingBillId = null;


// 消息容器
const formMessage = document.getElementById('formMessage');
const listMessage = document.getElementById('listMessage');
const editMessage = document.getElementById('editMessage');

// 统计信息元素
const totalIncomeEl = document.getElementById('totalIncome');
const totalExpenseEl = document.getElementById('totalExpense');
const balanceEl = document.getElementById('balance');
const totalCountEl = document.getElementById('totalCount');

// 文件操作元素

const billFileInput = document.getElementById('billFile');
const fileMessage = document.getElementById('fileMessage');

// 货币选择器
const currencySelect = document.getElementById('currency');
const editCurrencySelect = document.getElementById('editCurrency');

// 添加这个调试函数来检查分类加载状态
function debugCategoryState() {
    console.group('分类状态调试');
    console.log('categories 对象:', categories);
    console.log('INCOME 分类数量:', categories.INCOME ? categories.INCOME.length : 0);
    console.log('EXPENSE 分类数量:', categories.EXPENSE ? categories.EXPENSE.length : 0);

    const categorySelect = document.getElementById('category');
    const editCategorySelect = document.getElementById('editCategory');

    console.log('category 下拉框选项数量:', categorySelect ? categorySelect.children.length : '元素未找到');
    console.log('editCategory 下拉框选项数量:', editCategorySelect ? editCategorySelect.children.length : '元素未找到');
    console.groupEnd();
}

// 在分类初始化完成后调用
setTimeout(debugCategoryState, 1000);


// 监听类型
document.addEventListener('DOMContentLoaded', function() {

    // --- 移动端侧边栏逻辑 Start ---
        const sidebarToggle = document.getElementById('sidebarToggle');
        const sidebar = document.querySelector('.sidebar');
        const overlay = document.getElementById('sidebarOverlay');
        const navLink = document.querySelectorAll('.nav-link');

        // 切换侧边栏
        if (sidebarToggle) {
            sidebarToggle.addEventListener('click', function(e) {
                e.stopPropagation(); // 防止冒泡
                sidebar.classList.toggle('active');
                overlay.classList.toggle('active');
            });
        }

        // 点击遮罩层关闭
        if (overlay) {
            overlay.addEventListener('click', function() {
                sidebar.classList.remove('active');
                overlay.classList.remove('active');
            });
        }

        // 手机端点击菜单项后自动收起侧边栏，提升体验
        if (window.innerWidth <= 768) {
            navLink.forEach(link => {
                link.addEventListener('click', () => {
                    sidebar.classList.remove('active');
                    overlay.classList.remove('active');
                });
            });
        }


    // 绑定搜索按钮点击事件
        const searchBtn = document.getElementById('searchBtn');
        const searchInput = document.getElementById('searchInput');

    if (searchBtn) {
            searchBtn.addEventListener('click', function() {
                // 获取当前选中的时间范围
                const currentPeriod = periodFilter ? periodFilter.value : '';
                // 重新加载账单（loadBills 内部会自动获取 input 的值）
                loadBills(currentPeriod);
            });
        }

        // 绑定回车键搜索
        if (searchInput) {
            searchInput.addEventListener('keyup', function(e) {
                if (e.key === 'Enter') {
                    const currentPeriod = periodFilter ? periodFilter.value : '';
                    loadBills(currentPeriod);
                }
            });
        }



    // 初始化个人中心
        initProfile();
        initSettings(); // 包含修改密码

 const categorySection = document.getElementById('categories');
        if (categorySection) {
            initializeCategoryManagement();
        }


    const typeSelect = document.getElementById('type');
    const editTypeSelect = document.getElementById('editType');

    //类型变化监听器
    if (typeSelect) {
        typeSelect.addEventListener('change', function() {
            const type = this.value === '收入' ? 'INCOME' : 'EXPENSE';
            updateCategorySelect(type, 'category');
        });
    }

    if (editTypeSelect) {
        editTypeSelect.addEventListener('change', function() {
            const type = this.value === '收入' ? 'INCOME' : 'EXPENSE';
            updateCategorySelect(type, 'editCategory');
        });
    }

     // 检查用户认证状态（token是否存在）
        if (!checkAuthentication()) {
            return;
        }

        // 初始化分类数据
        initializeCategories().then(() => {
        console.log('分类初始化完成，开始加载其他数据...')

        // ★★★ 新增：强制设置下拉框 UI 为“支出”，防止浏览器缓存了“收入”选项 ★★★
                const typeSelect = document.getElementById('type');
                if (typeSelect) {
                    typeSelect.value = '支出';
                }

                // 确保下拉框加载“支出”的分类
                updateCategorySelect('EXPENSE', 'category');

                // 编辑框的默认状态也设为支出（防止空白）
                updateCategorySelect('EXPENSE', 'editCategory');

            const selectedPeriod = periodFilter ? periodFilter.value : '';
            loadBills(selectedPeriod);
            loadStatistics(selectedPeriod);
            loadCurrencies();

            // 初始化图表显示
            setupCharts();
            //加载图表数据
            loadChartData(selectedPeriod)
        }).catch(error=>{
            console.error('初始化失败：', error);
            //即使失败页设置默认分类
            updateCategorySelect('EXPENSE','category');
            updateCategorySelect('EXPENSE','editCategory');
        });


     // 获取所有导航链接
     const navLinks = document.querySelectorAll('.nav-link');
     // 获取所有页面内容区域
     const pageSections = document.querySelectorAll('.page-section');

     // 默认激活第一个导航项（记账页面）
     const defaultPage = document.getElementById('record');
     if (defaultPage) {
         defaultPage.classList.add('active');
     }

     // 为每个导航链接添加点击事件
     navLinks.forEach(link => {
         link.addEventListener('click', function(e) {
             e.preventDefault();

             // 获取目标页面ID
             const targetPage = this.getAttribute('data-page');

             // 移除所有页面的active类
             pageSections.forEach(section => {
                 section.classList.remove('active');
             });

             // 移除所有导航链接的active类
             navLinks.forEach(navLink => {
                 navLink.classList.remove('active');
             });

             // 为当前点击的导航链接添加active类
             this.classList.add('active');

             // 显示对应的目标页面
             const targetSection = document.getElementById(targetPage);
             if (targetSection) {
                 targetSection.classList.add('active');

                  // 如果是分析页面，加载图表数据
                                 if (targetPage === 'analysis') {
                                     // 读取分析页面筛选器的当前值
                                 const currentPeriod = document.getElementById('analysisPeriodFilter')
                                                     ? document.getElementById('analysisPeriodFilter').value
                                                     : 'month';
                                 loadChartData(currentPeriod);
                                 loadTrendChart(); // 趋势图保持默认7天，或根据需求传参
                                     // 为了防止太频繁消耗 Token，可以加个判断：如果内容是空的，或者用户手动点刷新才加载
                                     const reportContent = document.getElementById('aiReportContent');
                                     if (reportContent && (reportContent.innerText.includes('正在') || reportContent.innerText.trim() === '')) {
                                          loadAiReport();
                                     }
                                 }
                  // 如果是记录页面，重新加载账单列表
                                 if (targetPage === 'record') {
                                     const selectedPeriod = periodFilter ? periodFilter.value : '';
                                     loadBills(selectedPeriod);
                                 }
                  // 【新增】如果是汇率页面，初始化汇率功能
                                 if (targetPage === 'exchange') {
                                    initExchangePage();
                                 }
             }
         });
     });



       // 只在备份页面激活时加载备份列表
         const backupSection = document.getElementById('backup');
         if (backupSection && backupSection.classList.contains('active')) {
             loadBackupList();
         }
          // 添加时间筛选事件监听器
             if (periodFilter) {
                 periodFilter.addEventListener('change', function() {
                     const selectedPeriod = this.value;
                     loadBills(selectedPeriod);
                     loadStatistics(selectedPeriod);
                     // 如果分析页面是活动的，也更新图表
                     const analysisSection = document.getElementById('analysis');
                     if (analysisSection && analysisSection.classList.contains('active')) {
                         loadChartData(selectedPeriod);
                     }
                 });
             }



        // 分析页面的筛选器事件
           const analysisFilter = document.getElementById('analysisPeriodFilter');
           if (analysisFilter) {
               analysisFilter.addEventListener('change', function() {
                   const period = this.value;
                   loadChartData(period);

                   // 2. 刷新折线图 (传入 period，让函数内部决定查几天)
                           loadTrendChart(period);
               });
           }




});


//=======================
//个人中心相关函数
//=======================

function initProfile() {
    // 1. 加载用户信息
    loadUserProfile();

    // 2. 绑定保存按钮事件
    const saveBtn = document.getElementById('saveProfileBtn');
    if (saveBtn) {
        saveBtn.addEventListener('click', handleSaveProfile);
    }

    // 3. 绑定重置按钮事件
    const resetBtn = document.getElementById('resetProfileBtn');
    if (resetBtn) {
        resetBtn.addEventListener('click', loadUserProfile);
    }

    // 4. 头像上传相关
    const changeAvatarBtn = document.getElementById('changeAvatarBtn');
    const avatarInput = document.getElementById('avatarInput');

    if (changeAvatarBtn && avatarInput) {
        // 点击按钮触发文件选择
        changeAvatarBtn.addEventListener('click', () => avatarInput.click());

        // 文件选择后自动上传
        avatarInput.addEventListener('change', handleAvatarUpload);
    }
}

// 加载用户信息并填充表单
function loadUserProfile() {
    api.getCurrentUser()
        .then(user => {
            // 填充头部信息
            document.getElementById('welcomeUser').textContent = `欢迎, ${user.username}`;

            // 填充表单
            document.getElementById('username').value = user.username || '';
            document.getElementById('email').value = user.email || '';
            document.getElementById('phone').value = user.phone || '';
            document.getElementById('birthday').value = user.birthday || '';
            document.getElementById('gender').value = user.gender || ''; // 确保下拉框value对应 '男'/'女'
            document.getElementById('signature').value = user.signature || '';

            // 填充头像
            const avatarImg = document.getElementById('avatarPreview');
            if (user.avatarUrl) {
                // 如果是相对路径，可能需要补全，取决于你具体的部署
                // 这里假设 user.avatarUrl 已经是 /uploads/xxx.jpg
                avatarImg.src = user.avatarUrl;
            } else {
                avatarImg.src = '/images/default-avatar.png';
            }
        })
        .catch(err => {
            console.error('加载用户信息失败', err);
            showMessage('profileMessage', '加载用户信息失败', 'error');
        });
}

// 保存个人资料
function handleSaveProfile() {
    const profileData = {
        // username 和 email 通常后端不让改，或者只读，但为了DTO完整可以传
        username: document.getElementById('username').value,
        email: document.getElementById('email').value,
        phone: document.getElementById('phone').value,
        birthday: document.getElementById('birthday').value || null, // 空字符串转null
        gender: document.getElementById('gender').value,
        signature: document.getElementById('signature').value
    };

    api.updateProfile(profileData)
        .then(updatedUser => {
            showMessage('profileMessage', '个人资料保存成功', 'success');
            // 更新页面上的显示（如果需要）
            document.getElementById('welcomeUser').textContent = `欢迎, ${updatedUser.username}`;
        })
        .catch(err => {
            console.error('保存失败', err);
            showMessage('profileMessage', '保存失败: ' + err.message, 'error');
        });
}

// 处理头像上传
function handleAvatarUpload(event) {
    const file = event.target.files[0];
    if (!file) return;

    // 简单的前端校验
    if (file.size > 2 * 1024 * 1024) { // 2MB
        showMessage('profileMessage', '头像文件不能超过2MB', 'error');
        return;
    }

    const formData = new FormData();
    formData.append('file', file);

    const btn = document.getElementById('changeAvatarBtn');
    const originalText = btn.innerHTML;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 上传中...';
    btn.disabled = true;

    api.uploadAvatar(formData)
        .then(url => {
            document.getElementById('avatarPreview').src = url;
            showMessage('profileMessage', '头像更新成功', 'success');
        })
        .catch(err => {
            console.error('头像上传失败', err);
            showMessage('profileMessage', '头像上传失败: ' + err.message, 'error');
        })
        .finally(() => {
            btn.innerHTML = originalText;
            btn.disabled = false;
            // 清空input，允许重复上传同一张图片
            event.target.value = '';
        });
}





async function loadCategories(type) {
    try {
        console.log(`正在加载 ${type} 分类...`);

        // 直接使用统一的分类API
        const response = await api.getCategories(type);
        console.log(`分类API响应:`, response);

        if (Array.isArray(response)) {
            return response;
        } else {
            console.error(`分类API返回非数组数据:`, response);
            return [];
        }
    } catch (error) {
        console.error(`加载${type}分类失败:`, error);
        return [];
    }
}

function getDefaultCategories(type) {
    const defaultCategories = {
        INCOME: [
            { id: 1, name: '工资', icon: '💰', type: 'INCOME' },
            { id: 2, name: '奖金', icon: '🎁', type: 'INCOME' },
            { id: 3, name: '投资', icon: '📈', type: 'INCOME' }
        ],
        EXPENSE: [
            { id: 4, name: '餐饮', icon: '🍔', type: 'EXPENSE' },
            { id: 5, name: '交通', icon: '🚗', type: 'EXPENSE' },
            { id: 6, name: '购物', icon: '🛒', type: 'EXPENSE' }
        ]
    };
    return defaultCategories[type] || [];
}



// 确保 updateCategorySelect 函数正确处理数据
function updateCategorySelect(type, selectElementId) {
    const selectElement = document.getElementById(selectElementId);
    if (!selectElement) {
        console.error(`找不到分类下拉框: ${selectElementId}`);
        return;
    }

    const currentCategories = categories[type] || [];
    console.log(`更新 ${selectElementId} 下拉框,类型: ${type}, 分类数量:`, currentCategories.length);

    // 清空现有选项
    selectElement.innerHTML = '<option value="">请选择分类</option>';

    //如果没有分类数据，显示提示
    if(currentCategories.length === 0){
        const option = document.createElement('option');
        option.value = '';
        option.textContent = '暂无分类，请先添加分类';
        selectElement.appendChild(option);
        return;
    }

    // 添加分类选项
    currentCategories.forEach(category => {
        const option = document.createElement('option');
        option.value = category.id;

        // 显示分类名称和图标
        let displayText = category.name;
        if (category.icon && category.icon !== '0') {
            displayText = `${category.icon} ${category.name}`;
        }
        option.textContent = displayText;

        selectElement.appendChild(option);
    });

    console.log(`分类下拉框 ${selectElementId} 更新完成，现在有 ${selectElement.children.length} 个选项`);
}




// 修改 initializeCategories 函数
async function initializeCategories() {
    try {
        console.log('开始初始化分类数据...');

        // 分别获取系统分类和用户自定义分类
        const systemIncomeCategories = await api.getSystemCategories('INCOME');
        const userIncomeCategories = await api.getUserCategories('INCOME');
        const systemExpenseCategories = await api.getSystemCategories('EXPENSE');
        const userExpenseCategories = await api.getUserCategories('EXPENSE');

        // 合并分类数据，确保系统分类优先
        const incomeCategories = [...systemIncomeCategories, ...userIncomeCategories];
        const expenseCategories = [...systemExpenseCategories, ...userExpenseCategories];

        // 更新全局 categories 对象
        categories.INCOME = incomeCategories || [];
        categories.EXPENSE = expenseCategories || [];

        console.log('分类初始化完成:', {
            income: categories.INCOME,
            expense: categories.EXPENSE
        });

        // 初始化默认选择支出分类
        updateCategorySelect('EXPENSE', 'category');
        updateCategorySelect('EXPENSE', 'editCategory');

        console.log('分类下拉框初始化完成');
    } catch (error) {
        console.error('分类初始化失败:', error);
        // 即使失败也设置默认分类
        categories.INCOME = getDefaultCategories('INCOME');
        categories.EXPENSE = getDefaultCategories('EXPENSE');
        updateCategorySelect('EXPENSE', 'category');
        updateCategorySelect('EXPENSE', 'editCategory');
    }
}



(function() {
    // 检查认证状态，如果未认证则重定向到登录页
    function checkAuth() {
        const token = localStorage.getItem('authToken');
        const currentPage = window.location.pathname;

        // 如果当前页面是受保护页面且没有token，则重定向到登录页
        if (!token && currentPage.includes('/dashboard.html')) {
            window.location.href = '/html/login.html';
            return false;
        }
        return true;
    }

    // 页面加载时检查认证
    if (!checkAuth()) {
        return;
    }
})();


// 认证检查函数
function checkAuthentication() {
    const token = localStorage.getItem('authToken');
    if (!token) {
        window.location.href = '/html/login.html';
        return false;
    }
    return true;
}



// 加载支持的货币列表
function loadCurrencies() {
    api.getCurrencies()
        .then(data => {
            if (data && data.result && data.result.list) {
                const currencies = data.result.list;

                // 清空现有选项（除了默认的CNY）
                currencySelect.innerHTML = '<option value="CNY" selected>人民币 (CNY)</option>';
                editCurrencySelect.innerHTML = '<option value="CNY" selected>人民币 (CNY)</option>';

                // 添加其他货币选项
                currencies.forEach(currency => {
                    if (currency.code !== 'CNY') {
                        const option = document.createElement('option');
                        option.value = currency.code;
                        option.textContent = `${currency.name} (${currency.code})`;
                        currencySelect.appendChild(option.cloneNode(true));
                        editCurrencySelect.appendChild(option);
                    }
                });
            }
        })
        .catch(error => {
            console.error('加载货币列表失败:', error);
        });
}


// 添加账单
billForm.addEventListener('submit', function(e) {
    e.preventDefault();

    const categorySelect = document.getElementById('category');
    const selectedCategoryId = categorySelect.value;

    if (!selectedCategoryId) {
        showMessage(formMessage, '请选择分类', 'error');
        return;
    }



  const billData = {
      amount: parseFloat(document.getElementById('amount').value),
      billtype: document.getElementById('type').value === '收入' ? 'INCOME' : 'EXPENSE',
      currency: document.getElementById('currency').value,
      category: {
          id: parseInt(selectedCategoryId)
      },
      remark: document.getElementById('remark').value || '',
      // 确保 conversionStatus 不为 null
      conversionStatus: "NOT_NEEDED" // 或其他有效值
  };

    console.log('提交账单数据:', billData);

    api.addBill(billData)
        .then(data => {
            showMessage(formMessage, '账单添加成功！', 'success');
            billForm.reset();
            // 重置分类选择
            updateCategorySelect('EXPENSE', 'category');

            const selectedPeriod = periodFilter ? periodFilter.value : '';
            loadBills(selectedPeriod);
            loadStatistics(selectedPeriod);
            loadBudgetStatus();

            // 3秒后清除消息
            setTimeout(() => {
                hideMessage(formMessage);
            }, 3000);
        })
        .catch(error => {
            console.error('添加账单错误:', error);
            showMessage(formMessage, '添加账单失败：' + error.message, 'error');
        });
});

// 编辑账单
saveEditBtn.addEventListener('click', function() {
    const id = document.getElementById('editId').value;
    const categorySelect = document.getElementById('editCategory');
    const selectedCategoryId = categorySelect.value;

    if (!selectedCategoryId) {
        showMessage(editMessage, '请选择分类', 'error');
        return;
    }

    const billData = {
        amount: parseFloat(document.getElementById('editAmount').value),
        billtype: document.getElementById('editType').value === '收入' ? 'INCOME' : 'EXPENSE',
        currency: document.getElementById('editCurrency').value,
        category: {
            id: parseInt(selectedCategoryId)
        },
        remark: document.getElementById('editRemark').value || '',
        version: parseInt(document.getElementById('editVersion').value)
    };

    console.log('更新账单数据:', billData);

    api.updateBill(id, billData)
        .then(data => {
            showMessage(editMessage, '账单更新成功！', 'success');
            editModal.style.display = 'none';
            // 使用当前筛选条件重新加载账单
            const selectedPeriod = periodFilter ? periodFilter.value : '';
            loadBills(selectedPeriod);
            loadStatistics(selectedPeriod);
            loadBudgetStatus();

        })
        .catch(error => {
            console.error('更新账单错误:', error);
            showMessage(editMessage, '更新账单失败：' + error.message, 'error');
        });
});

// 删除账单
function deleteBill(id) {
    if (!confirm('确定要删除这个账单吗？')) return;

    api.deleteBill(id)
        .then(data => {
            showMessage(listMessage, '账单删除成功！', 'success');
            // 使用当前筛选条件重新加载账单
            const selectedPeriod = periodFilter ? periodFilter.value : '';
            loadBills(selectedPeriod);
            loadStatistics(selectedPeriod);
            loadBudgetStatus();


            // 3秒后清除消息
            setTimeout(() => {
                hideMessage(listMessage);
            }, 3000);
        })
        .catch(error => {
            console.error('删除账单失败:', error);
            showMessage(listMessage, '删除账单失败：' + error.message, 'error');
        });
}

// 显示编辑模态框
function editBill(id) {
    api.getBill(id)
        .then(bill => {
            console.log('编辑账单数据:', bill);

            document.getElementById('editId').value = bill.id;
            document.getElementById('editAmount').value = bill.amount;
            document.getElementById('editType').value = bill.billtype === 'INCOME' ? '收入' : '支出';
            document.getElementById('editCurrency').value = bill.currency || 'CNY';

            // 设置分类选择
            const categorySelect = document.getElementById('editCategory');
            const categoryId = bill.category?.id;
            categorySelect.value = categoryId || '';

            document.getElementById('editRemark').value = bill.remark || '';

            //设置隐藏字段保存version
                        if (bill.version !== undefined) {
                            let versionInput = document.getElementById('editVersion');
                            if (!versionInput) {
                                versionInput = document.createElement('input');
                                versionInput.type = 'hidden';
                                versionInput.id = 'editVersion';
                                document.getElementById('editBillForm').appendChild(versionInput);
                            }
                            versionInput.value = bill.version;
                        }

            editModal.style.display = 'flex';
        })
        .catch(error => {
            console.error('获取账单详情失败:', error);
            showMessage(listMessage, '获取账单信息失败：' + error.message, 'error');
        });
}

// 关闭模态框
closeButtons.forEach(button => {
    button.addEventListener('click', function() {
        editModal.style.display = 'none';
    });
});

// 点击模态框外部关闭
window.addEventListener('click', function(event) {
    if (event.target === editModal) {
        editModal.style.display = 'none';
    }
});

// 退出登录
logoutBtn.addEventListener('click', function() {
    localStorage.removeItem('authToken');
    window.location.href = '/html/login.html';
});



// loadBills 函数
function loadBills(period = '') {
    // 如果没有传入 period，尝试获取当前下拉框的值
    if (!period && periodFilter) {
        period = periodFilter.value;
    }

    // 获取搜索关键词
    const searchInput = document.getElementById('searchInput');
    const keyword = searchInput ? searchInput.value.trim() : '';

    const validPeriods = ['today', 'month', ''];
    if (!validPeriods.includes(period)) {
        console.warn(`无效的时间范围:${period},将使用默认值`);
        period = '';
    }

    // 调用 API 传入两个参数
    api.getBills(period, keyword)
        .then(bills => {
            billsList.innerHTML = '';

            if (!bills || bills.length === 0) {
                // 如果有关键词，提示未搜到
                const emptyText = keyword ? `没有找到包含 "${keyword}" 的账单` : '暂无账单记录';
                billsList.innerHTML = `<tr><td colspan="7" style="text-align: center;">${emptyText}</td></tr>`;
                return;
            }

            // ... (后续渲染代码保持不变) ...
            // 按时间倒序排列
            bills.sort((a, b) => new Date(b.createTime) - new Date(a.createTime));

            bills.forEach(bill => {
                const row = document.createElement('tr');
                const amountClass = bill.billtype === 'INCOME' ? 'stat-income' : 'stat-expense';
                const typeText = bill.billtype === 'INCOME' ? '收入' : '支出';
                const categoryName = bill.category?.name || '其他';

                // 1. 先准备好标签的 HTML
                let tagHtml = '';
                if (bill.tags) {
                    let tagClass = 'tag-normal';
                    if (bill.tags.includes('必要')) tagClass = 'tag-necessary';
                    else if (bill.tags.includes('冲动')) tagClass = 'tag-impulse';
                    else if (bill.tags.includes('社交')) tagClass = 'tag-social';
                    else if (bill.tags.includes('提升') || bill.tags.includes('自我')) tagClass = 'tag-self';

                    tagHtml = `<br><span class="ai-tag ${tagClass}"><i class="fas fa-tag"></i> ${bill.tags}</span>`;
                }

                // 2. 一次性生成完整的 HTML，把 tagHtml 插在备注后面，并保留按钮代码
                row.innerHTML = `
                    <td class="${amountClass}">¥${bill.amount.toFixed(2)}</td>
                    <td>${typeText}</td>
                    <td>${bill.currency || 'CNY'}</td>
                    <td>${categoryName}</td>
                    <td>
                        ${bill.remark || '-'}
                        ${tagHtml}
                    </td>
                    <td>${formatDate(bill.createTime)}</td>
                    <td>
                        <div class="action-buttons">
                            <button class="action-btn btn-warning" onclick="editBill(${bill.id})">
                                <i class="fas fa-edit"></i> 编辑
                            </button>
                            <button class="action-btn btn-danger" onclick="deleteBill(${bill.id})">
                                <i class="fas fa-trash"></i> 删除
                            </button>
                        </div>
                    </td>
                `;

                billsList.appendChild(row);
            });
        })
        .catch(error => {
            console.error('加载账单失败:', error);
            showMessage(listMessage, '加载账单失败：' + error.message, 'error');
        });
}

// 加载统计信息
function loadStatistics(period = '') {
    api.getStatistics(period)
        .then(stats => {
            console.log('统计信息:', stats);

            totalIncomeEl.textContent = `¥${stats.totalIncome.toFixed(2)}`;
            totalExpenseEl.textContent = `¥${stats.totalExpense.toFixed(2)}`;
            balanceEl.textContent = `¥${(stats.totalIncome - stats.totalExpense).toFixed(2)}`;
            totalCountEl.textContent = stats.totalCount;
        })
        .catch(error => {
            console.error('加载统计信息失败：', error);
        });
}


// 将函数暴露到全局作用域，以便在HTML中调用
window.editBill = editBill;
window.deleteBill = deleteBill;


// 添加图表设置函数
function setupCharts() {
    // 创建图表容器
    const chartContainer = document.createElement('div');
    chartContainer.className = 'chart-container';
    chartContainer.style.display = 'flex';
    chartContainer.style.justifyContent = 'space-around';
    chartContainer.style.flexWrap = 'wrap';
    chartContainer.style.gap = '20px';
    chartContainer.style.marginTop = '20px';

    // 收入图表容器
    const incomeChartDiv = document.createElement('div');
    incomeChartDiv.style.flex = '1';
    incomeChartDiv.style.minWidth = '300px';

    const incomeChartTitle = document.createElement('h3');
    incomeChartTitle.textContent = '收入分类';
    incomeChartTitle.style.textAlign = 'center';

    const incomeCanvas = document.createElement('canvas');
    incomeCanvas.id = 'incomeChart';

    incomeChartDiv.appendChild(incomeChartTitle);
    incomeChartDiv.appendChild(incomeCanvas);

    // 支出图表容器
    const expenseChartDiv = document.createElement('div');
    expenseChartDiv.style.flex = '1';
    expenseChartDiv.style.minWidth = '300px';

    const expenseChartTitle = document.createElement('h3');
    expenseChartTitle.textContent = '支出分类';
    expenseChartTitle.style.textAlign = 'center';

    const expenseCanvas = document.createElement('canvas');
    expenseCanvas.id = 'expenseChart';

    expenseChartDiv.appendChild(expenseChartTitle);
    expenseChartDiv.appendChild(expenseCanvas);

    // 添加到分析页面
    chartContainer.appendChild(incomeChartDiv);
    chartContainer.appendChild(expenseChartDiv);

    // 查找分析页面的.card元素并添加图表
    const container = document.getElementById('pieChartsContainer');
    if (container) {
            // 清空容器防止重复添加
            container.innerHTML = '';
            container.appendChild(chartContainer);
        }
}

// 添加加载图表数据的函数
function loadChartData(period = 'month') {
    let url = API_BASE;
    if (period) {
        url += `?period=${period}`;
    }

    // 使用带认证的fetch
    api.getBills(period)
        .then(bills => {
        console.log(`加载图表数据: period=${period}, 数量=${bills.length}`);

            // 分别计算收入和支出的分类统计
            const incomeCategories = {};
            const expenseCategories = {};

            bills.forEach(bill => {

                const categoryName = bill.category ? bill.category.name : '其他';
                const amount = bill.amount || 0;

                if (bill.billtype === 'INCOME') {
                     incomeCategories[categoryName] = (incomeCategories[categoryName] || 0) + amount;
                      } else if (bill.billtype === 'EXPENSE') {
                     expenseCategories[categoryName] = (expenseCategories[categoryName] || 0) + amount;
                     }
            });

            // 渲染收入图表
            renderChart('incomeChart', incomeCategories, `收入分类 (${getPeriodText(period)})`, '#4CAF50');

                       // 渲染支出图表
             renderChart('expenseChart', expenseCategories, `支出分类 (${getPeriodText(period)})`, '#F44336');

                       // 如果此时趋势图也在显示，顺便刷新趋势图（可选，看需求）
                       // loadTrendChart(period === 'month' ? 30 : 7);
                   })
                   .catch(error => {
                       console.error('加载图表数据失败:', error);
                   });
}

// 辅助函数：把 period 代码转成中文，用于图表标题
function getPeriodText(period) {
    if (period === 'today') return '今日';
    if (period === 'month') return '本月';
    return '全部';
}


// 添加渲染图表的函数
function renderChart(canvasId, data, title, color) {
    const ctx = document.getElementById(canvasId);
        if (!ctx) return;

    // 销毁已存在的图表实例
    if (canvasId === 'incomeChart' && incomeChart) {
        incomeChart.destroy();
    } else if (canvasId === 'expenseChart' && expenseChart) {
        expenseChart.destroy();
    }

    // 准备图表数据
    const labels = Object.keys(data);
    const values = Object.values(data);

    // 如果没有数据，显示提示信息
    if (labels.length === 0) {
          // 如果没有 Chart 实例，可以画个文字
           const context = ctx.getContext('2d');
           context.clearRect(0, 0, ctx.width, ctx.height);
           context.font = "14px Arial";
           context.fillStyle = "#999";
           context.textAlign = "center";
           context.fillText("暂无" + title + "数据", ctx.width / 2, ctx.height / 2);
           return;
    }

    // 生成颜色数组
    const backgroundColors = generateColors(labels.length);

    // 创建新图表
    const chart = new Chart(ctx, {
        type: 'pie',
        data: {
            labels: labels,
            datasets: [{
                data: values,
                backgroundColor: backgroundColors,
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            plugins: {
                legend: {
                    position: 'bottom',
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            const label = context.label || '';
                            const value = context.raw || 0;
                            const total = context.dataset.data.reduce((a, b) => a + b, 0);
                            const percentage = Math.round((value / total) * 100);
                            return `${label}: ¥${value.toFixed(2)} (${percentage}%)`;
                        }
                    }
                }
            }
        }
    });

    // 保存图表实例
    if (canvasId === 'incomeChart') {
        incomeChart = chart;
    } else if (canvasId === 'expenseChart') {
        expenseChart = chart;
    }
}

// 添加生成颜色的辅助函数
function generateColors(count) {
    const colors = [
        '#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0',
        '#9966FF', '#FF9F40', '#FF6384', '#C9CBCF',
        '#4BC0C0', '#FF6384', '#36A2EB', '#FFCE56'
    ];

    // 如果需要的颜色数量超过预定义颜色数量，则重复使用
    const result = [];
    for (let i = 0; i < count; i++) {
        result.push(colors[i % colors.length]);
    }
    return result;
}

// 加载并渲染趋势图
function loadTrendChart(period = 'month') {
let days = 7; // 默认值

    // 根据筛选策略决定天数
    if (period === 'month') {
        days = 30; // 本月 -> 最近30天
    } else if (period === '') { // 全部
        days = 90; // 全部 -> 最近90天 (为了图表美观，不建议无限长)
    } else if (period === 'today') {
        days = 7;  // 今日 -> 依然显示最近7天作为参考，或者设为 1
    }
    // 默认查询最近 7 天
    api.getTrendData(days).then(data => {
        const ctx = document.getElementById('trendChart');
        if (!ctx) return; // 防止页面元素没加载

        // 提取日期和金额数组
        const labels = data.map(item => item.date);
        const amounts = data.map(item => item.amount);

        // 如果旧图表存在，销毁它以防止重影
        if (trendChart) {
            trendChart.destroy();
        }

        // 创建新图表
        trendChart = new Chart(ctx, {
            type: 'line', // 折线图
            data: {
                labels: labels,
                datasets: [{
                    label: '每日支出 (¥)',
                    data: amounts,
                    borderColor: '#673ab7', // 紫色线条
                    backgroundColor: 'rgba(103, 58, 183, 0.1)', // 紫色填充背景
                    borderWidth: 3,
                    pointBackgroundColor: '#fff',
                    pointBorderColor: '#673ab7',
                    pointRadius: 5,
                    pointHoverRadius: 7,
                    fill: true, // 开启线下填充
                    tension: 0.4 // 0.4 是一条平滑的曲线，0 是直线
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: false // 只有一个数据集，隐藏图例更简洁
                    },
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                return `支出: ¥${context.raw.toFixed(2)}`;
                            }
                        },
                        backgroundColor: 'rgba(0,0,0,0.8)',
                        padding: 10,
                        displayColors: false
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        grid: {
                            color: '#f0f0f0'
                        },
                        ticks: {
                            callback: function(value) {
                                return '¥' + value;
                            }
                        }
                    },
                    x: {
                        grid: {
                            display: false // 隐藏X轴网格线更美观
                        }
                    }
                }
            }
        });
    }).catch(err => {
        console.error("加载趋势图失败", err);
    });
}



// =======================
// 设置与安全相关函数
// =======================

function initSettings() {
    initThemeSettings();
    initPasswordSettings();
}

// 1. 初始化主题设置
function initThemeSettings() {
    const themeSelect = document.getElementById('theme');

    // 读取本地存储的主题
    const savedTheme = localStorage.getItem('theme') || 'light';

    // 应用主题
    applyTheme(savedTheme);

    // 设置下拉框的值
    if (themeSelect) {
        themeSelect.value = savedTheme;

        // 监听变化
        themeSelect.addEventListener('change', function() {
            const newTheme = this.value;
            applyTheme(newTheme);
            localStorage.setItem('theme', newTheme);
            showMessage('preferenceMessage', `已切换到${newTheme === 'dark' ? '暗黑' : '明亮'}模式`, 'success');
        });
    }
}

// 应用主题的辅助函数
function applyTheme(theme) {
    document.documentElement.setAttribute('data-theme', theme);
}

function initPasswordSettings() {
    const changePasswordBtn = document.getElementById('changePasswordBtn');
    const securityMessage = document.getElementById('securityMessage');

    if (changePasswordBtn) {
            changePasswordBtn.addEventListener('click', async function() {
                // A. 获取输入值
                const currentPasswordInput = document.getElementById('currentPassword');
                const newPasswordInput = document.getElementById('newPassword');
                const confirmPasswordInput = document.getElementById('confirmPassword');

                const currentPassword = currentPasswordInput.value;
                const newPassword = newPasswordInput.value;
                const confirmPassword = confirmPasswordInput.value;

                // B. 前端基础校验
                if (!currentPassword || !newPassword || !confirmPassword) {
                    showMessage(securityMessage, '请填写所有密码字段', 'error');
                    return;
                }

                if (newPassword !== confirmPassword) {
                    showMessage(securityMessage, '两次输入的新密码不一致', 'error');
                    return;
                }

                if (newPassword.length < 8) {
                    showMessage(securityMessage, '新密码长度不能少于8位', 'error');
                    return;
                }

                // C. UI 状态切换：禁用按钮，显示 Loading 图标 (保持你的风格)
                const originalBtnText = changePasswordBtn.innerHTML;
                changePasswordBtn.disabled = true;
                changePasswordBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 修改中...';

                try {
                    // D. 调用 API
                    await api.changePassword({
                        oldPassword: currentPassword,
                        newPassword: newPassword
                    });

                    // E. 成功处理
                    showMessage(securityMessage, '密码修改成功！为了安全，即将退出登录...', 'success');

                    // 清空表单
                    currentPasswordInput.value = '';
                    newPasswordInput.value = '';
                    confirmPasswordInput.value = '';

                    // F. 3秒后强制登出
                    setTimeout(() => {
                        localStorage.removeItem('authToken'); // 清除 Token
                        window.location.href = '/html/login.html'; // 跳转回登录页
                    }, 3000);

                } catch (error) {
                    // G. 错误处理
                    console.error('修改密码失败:', error);
                    // 显示后端返回的具体错误信息（如"旧密码错误"）
                    showMessage(securityMessage, error.message, 'error');
                } finally {
                    // H. 恢复按钮状态
                    changePasswordBtn.disabled = false;
                    changePasswordBtn.innerHTML = originalBtnText;
                }
            });
        }
}



// =======================
// 数据备份相关函数
// =======================

const backupMessage = document.getElementById('backupMessage');
const createBackupBtn = document.getElementById('createBackupBtn');
const refreshBackupBtn = document.getElementById('refreshBackupBtn');
const restoreBackupBtn = document.getElementById('restoreBackupBtn');
const backupFileInput = document.getElementById('backupFile');
const backupList = document.getElementById('backupList');



if (createBackupBtn) {
    createBackupBtn.addEventListener('click', function() {
        showMessage(backupMessage, '正在创建备份文件...', 'info');

        api.createBackup()
            .then(data => {
                showMessage(backupMessage, data, 'success');
                loadBackupList(); // 刷新备份列表
            })
            .catch(error => {
                showMessage(backupMessage, '备份创建失败: ' + error.message, 'error');
            })
            .finally(() => {
                setTimeout(() => hideMessage(backupMessage), 5000);
            });
    });
}

// 刷新备份列表
if (refreshBackupBtn) {
    refreshBackupBtn.addEventListener('click', function() {
        loadBackupList();
        showMessage(backupMessage, '备份列表已刷新', 'info');
        setTimeout(() => hideMessage(backupMessage), 3000);
    });
}


// 恢复备份（从上传的文件）
if (restoreBackupBtn) {
    restoreBackupBtn.addEventListener('click', function() {
        const file = backupFileInput.files[0];

        if (!file) {
            showMessage(backupMessage, '请选择要恢复的备份文件', 'error');
            setTimeout(() => hideMessage(backupMessage), 3000);
            return;
        }

        // 检查文件扩展名
        if (!file.name.endsWith('.xlsx') && !file.name.endsWith('.xls')) {
            showMessage(backupMessage, '请选择Excel格式的备份文件 (.xlsx 或 .xls)', 'error');
            setTimeout(() => hideMessage(backupMessage), 3000);
            return;
        }

        if (!confirm('确定要从备份文件恢复数据吗？这将覆盖当前所有数据！')) {
            return;
        }

        const formData = new FormData();
        formData.append('file', file);

        showMessage(backupMessage, '正在恢复数据...', 'info');

        api.uploadBills(formData)
            .then(data => {
                showMessage(backupMessage, data, 'success');
                backupFileInput.value = '';
                const selectedPeriod = periodFilter ? periodFilter.value : '';
                loadBills(selectedPeriod);
                loadStatistics(selectedPeriod);
                loadBudgetStatus();
            })
            .catch(error => {
                showMessage(backupMessage, '数据恢复失败: ' + error.message, 'error');
            })
            .finally(() => {
                setTimeout(() => hideMessage(backupMessage), 5000);
            });
    });
}



// 加载备份列表
function loadBackupList() {
    api.listBackups()
        .then(backups => {
            renderBackupList(backups);
        })
        .catch(error => {
            console.error('加载备份列表失败:', error);
            backupList.innerHTML = '<tr><td colspan="4" class="no-backup">加载备份列表失败</td></tr>';
        });
}

// 渲染备份列表
function renderBackupList(backups) {
    if (backups.length === 0) {
        backupList.innerHTML = '<tr><td colspan="4" class="no-backup">暂无备份文件</td></tr>';
        return;
    }

    backupList.innerHTML = '';

    backups.forEach(backup => {
        const row = document.createElement('tr');
        // 格式化文件大小
        const fileSize = formatFileSize(backup.fileSize);
        // 格式化时间
        const createTime = backup.createTime ? backup.createTime.replace('T', ' ').substring(0, 16) : '未知';

        row.innerHTML = `            <td>${backup.fileName}</td>
            <td>${createTime}</td>
            <td>${fileSize}</td>
            <td class="backup-actions">
                <button class="backup-btn download" onclick="downloadBackup('${backup.fileName}')">
                    <i class="fas fa-download"></i> 下载
                </button>
                <button class="backup-btn restore" onclick="restoreFromBackup('${backup.fileName}')">
                    <i class="fas fa-file-import"></i> 恢复
                </button>
                <button class="backup-btn delete" onclick="deleteBackup('${backup.fileName}')">
                    <i class="fas fa-trash"></i> 删除
                </button>
            </td>
        `;
        backupList.appendChild(row);
    });
}

// 格式化文件大小
function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// 下载备份文件
function downloadBackup(filename) {
    showMessage(backupMessage, `正在准备下载备份文件: ${filename}`, 'info');

    api.downloadBackup(filename)
        .then(response => {
            if (response.ok) {
                const disposition = response.headers.get('Content-Disposition');
                let downloadFilename = filename;

                if (disposition && disposition.indexOf('attachment') !== -1) {
                    const filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
                    const matches = filenameRegex.exec(disposition);
                    if (matches != null && matches[1]) {
                        downloadFilename = matches[1].replace(/['"]/g, '');
                    }
                }

                return response.blob().then(blob => {
                    const url = window.URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.style.display = 'none';
                    a.href = url;
                    a.download = downloadFilename;
                    document.body.appendChild(a);
                    a.click();
                    window.URL.revokeObjectURL(url);

                    showMessage(backupMessage, `备份文件下载成功: ${filename}`, 'success');
                });
            } else {
                throw new Error('下载失败');
            }
        })
        .catch(error => {
            showMessage(backupMessage, `备份文件下载失败: ${error.message}`, 'error');
        })
        .finally(() => {
            setTimeout(() => hideMessage(backupMessage), 3000);
        });
}

// 从备份恢复数据
function restoreFromBackup(filename) {
    if (!confirm(`确定要从备份文件 ${filename} 恢复数据吗？这将覆盖当前所有数据！`)) {
        return;
    }

    showMessage(backupMessage, `正在从备份文件恢复数据: ${filename}`, 'info');

    api.restoreBackup(filename)
        .then(data => {
            showMessage(backupMessage, data, 'success');
            const selectedPeriod = periodFilter ? periodFilter.value : '';
            loadBills(selectedPeriod);
            loadStatistics(selectedPeriod);
        })
        .catch(error => {
            showMessage(backupMessage, `数据恢复失败: ${error.message}`, 'error');
        })
        .finally(() => {
            setTimeout(() => hideMessage(backupMessage), 5000);
        });
}

// 删除备份文件
function deleteBackup(filename) {
    if (!confirm(`确定要删除备份文件 ${filename} 吗？`)) {
        return;
    }

    api.deleteBackup(filename)
        .then(data => {
            showMessage(backupMessage, data, 'success');
            loadBackupList(); // 刷新备份列表
        })
        .catch(error => {
            showMessage(backupMessage, `删除备份失败: ${error.message}`, 'error');
        })
        .finally(() => {
            setTimeout(() => hideMessage(backupMessage), 3000);
        });
}

// 将函数暴露到全局作用域
window.downloadBackup = downloadBackup;
window.restoreFromBackup = restoreFromBackup;
window.deleteBackup = deleteBackup;

// 自动备份功能
const autoBackupMessage = document.getElementById('autoBackupMessage');
const saveAutoBackupBtn = document.getElementById('saveAutoBackupBtn');
const autoBackupEnabled = document.getElementById('autoBackupEnabled');
const autoBackupOptions = document.getElementById('autoBackupOptions');
const autoBackupInterval = document.getElementById('autoBackupInterval');

// 启用/禁用自动备份时切换显示选项
if (autoBackupEnabled) {
    autoBackupEnabled.addEventListener('change', function() {
        if (this.checked) {
            autoBackupOptions.style.display = 'block';
        } else {
            autoBackupOptions.style.display = 'none';
        }
    });
}

// 加载自动备份配置
function loadAutoBackupConfig() {
    api.getBackupSetting()
        .then(setting => {
            document.getElementById('autoBackupEnabled').checked = setting.enabled || false;

            // 根据是否启用自动备份来显示/隐藏选项
            if (setting.enabled) {
                autoBackupOptions.style.display = 'block';
            } else {
                autoBackupOptions.style.display = 'none';
            }

            // 设置备份周期
            if (setting.intervalHours) {
                document.getElementById('autoBackupInterval').value = setting.intervalHours;
            }

            if (setting.lastBackupTime) {
                document.getElementById('lastBackupTime').textContent =
                    new Date(setting.lastBackupTime).toLocaleString();
            } else {
                document.getElementById('lastBackupTime').textContent = '从未备份';
            }
        })
        .catch(error => {
            console.error('加载自动备份配置失败:', error);
            showMessage(autoBackupMessage, '加载自动备份配置失败: ' + error.message, 'error');
            setTimeout(() => hideMessage(autoBackupMessage), 3000);
        });
}

// 保存自动备份配置
if (saveAutoBackupBtn) {
    saveAutoBackupBtn.addEventListener('click', function() {
        const setting = {
            enabled: document.getElementById('autoBackupEnabled').checked,
            intervalHours: parseInt(document.getElementById('autoBackupInterval').value)
        };

        api.updateBackupSetting(setting)
            .then(data => {
                showMessage(autoBackupMessage, '自动备份设置保存成功！', 'success');
                // 重新加载配置以更新上次备份时间
                loadAutoBackupConfig();
            })
            .catch(error => {
                showMessage(autoBackupMessage, '设置保存失败: ' + error.message, 'error');
            })
            .finally(() => {
                setTimeout(() => hideMessage(autoBackupMessage), 3000);
            });
    });
}


//======================================================
//=====================分类管理相关逻辑 (修复版)==========
//======================================================

let currentCategoryType = 'EXPENSE';
let categoryManagementList = [];

// 1. 定义图标库 (这里定义了，页面上才会显示)
const ICON_LIBRARY = [
    '🍽️', '🍜', '🍔', '🍟', '🍕', '🍱', '🍦', '☕', // 餐饮
    '🚗', '🚕', '🚌', '🚇', '✈️', '⛽', '🅿️',       // 交通
    '🛍️', '🛒', '👗', '🧢', '👠', '💄', '💍',       // 购物
    '🏠', '⚡', '💧', '📱', '🌐', '🧹', '🛋️',       // 居家
    '💊', '🏥', '🏋️', '🧘', '💇',                   // 医疗/健康
    '🎮', '🎬', '🎤', '🎳', '🎲',                   // 娱乐
    '📚', '🎓', '🖊️', '💼', '💻',                   // 学习/工作
    '💰', '📈', '🧧', '🎁', '🏧',                   // 财务
    '🐶', '🐱', '👶', '🌹', '🔧', '📦', '🧾'        // 其他
];

// 分类管理初始化
function initializeCategoryManagement() {
    setupCategoryTabs();
    loadCategories();
    setupCategoryModal();
    renderIconGrid(); // <--- 新增：初始化时渲染图标网格
}

// 2. 新增：渲染图标网格函数
function renderIconGrid() {
    const container = document.getElementById('iconGridContainer');
    // 防止报错：如果HTML还没改好，找不到容器就直接返回
    if (!container) {
        console.warn('找不到 id="iconGridContainer" 的元素，图标网格无法加载');
        return;
    }

    container.innerHTML = ''; // 清空现有内容

    // 为每个图标创建 DOM 元素
    ICON_LIBRARY.forEach(icon => {
        const div = document.createElement('div');
        div.className = 'icon-option'; // 对应 CSS 样式
        div.textContent = icon;

        // 绑定点击事件
        div.addEventListener('click', function() {
            // A. 移除其他图标的选中样式
            document.querySelectorAll('.icon-option').forEach(el => el.classList.remove('selected'));
            // B. 给当前点击的图标添加选中样式
            this.classList.add('selected');
            // C. 关键一步：将选中的图标字符赋值给隐藏的 Input
            const input = document.getElementById('categoryIcon');
            if(input) input.value = icon;
        });

        container.appendChild(div);
    });
}

// 设置分类标签切换 (保持不变)
function setupCategoryTabs() {
    const tabs = document.querySelectorAll('.category-tab');
    tabs.forEach(tab => {
        tab.addEventListener('click', function() {
            tabs.forEach(t => t.classList.remove('active'));
            this.classList.add('active');
            currentCategoryType = this.getAttribute('data-type');
            loadCategories();
        });
    });
}

// 加载分类数据 (保持不变)
function loadCategories() {
    const categoryList = document.getElementById('categoryList');
    if(categoryList) categoryList.innerHTML = '<div style="text-align: center; padding: 20px;">加载中...</div>';

    Promise.all([
        api.getSystemCategories(currentCategoryType),
        api.getUserCategories(currentCategoryType)
    ]).then(([systemCats, userCats]) => {
        categoryManagementList = [...systemCats, ...userCats];
        renderCategoryList();
    }).catch(error => {
        console.error('加载分类失败:', error);
        const msgEl = document.getElementById('categoryMessage');
        if(msgEl) showMessage(msgEl, '加载分类失败: ' + error.message, 'error');
    });
}

// 渲染分类列表 (保持不变)
function renderCategoryList() {
    const categoryList = document.getElementById('categoryList');
    if (!categoryList) return;

    if (categoryManagementList.length === 0) {
        categoryList.innerHTML = '<div style="text-align: center; padding: 40px; color: #666;">暂无分类数据</div>';
        return;
    }

    let html = '';
    categoryManagementList.forEach(category => {
        const isSystemCategory = category.user === null;
        const typeText = category.type === 'INCOME' ? '收入' : '支出';

        html += `
            <div class="category-item ${isSystemCategory ? 'system-category' : ''}">
                <div class="category-info">
                    <div class="category-icon" style="background-color: ${category.color || '#f3e5f5'}">
                        ${category.icon || '💰'}
                    </div>
                    <div class="category-details">
                        <div class="category-name">${category.name}</div>
                        <div class="category-meta">
                            ${typeText} • ${isSystemCategory ? '默认分类' : '自定义分类'}
                        </div>
                    </div>
                </div>
                <div class="category-actions">
                    <button class="category-btn edit" onclick="editCategory(${category.id})"
                            ${isSystemCategory ? 'disabled' : ''}>
                        <i class="fas fa-edit"></i> 编辑
                    </button>
                    <button class="category-btn delete" onclick="deleteCategory(${category.id})"
                            ${isSystemCategory ? 'disabled' : ''}>
                        <i class="fas fa-trash"></i> 删除
                    </button>
                </div>
            </div>
        `;
    });
    categoryList.innerHTML = html;
}

// 设置分类模态框 (保持不变)
function setupCategoryModal() {
    const modal = document.getElementById('categoryModal');
    const closeBtn = document.querySelector('#categoryModal .close');
    const cancelBtn = document.getElementById('cancelCategory');
    const saveBtn = document.getElementById('saveCategory');
    const addBtn = document.getElementById('addCategoryBtn');

    if(addBtn) {
        addBtn.addEventListener('click', function() {
            openCategoryModal();
        });
    }

    [closeBtn, cancelBtn].forEach(btn => {
        if(btn) {
            btn.addEventListener('click', function() {
                modal.style.display = 'none';
            });
        }
    });

    if(saveBtn) saveBtn.addEventListener('click', saveCategory);

    window.addEventListener('click', function(event) {
        if (event.target === modal) {
            modal.style.display = 'none';
        }
    });
}

// 3. 修改：打开分类模态框（处理图标高亮逻辑）
function openCategoryModal(category = null) {
    const modal = document.getElementById('categoryModal');
    const title = document.getElementById('categoryModalTitle');
    const form = document.getElementById('categoryForm');
    const messageContainer = document.getElementById('categoryFormMessage');
    const iconInput = document.getElementById('categoryIcon'); // 隐藏输入框

    // 重置表单
    form.reset();
    hideMessage(messageContainer);

    // 清除所有网格图标的选中状态
    document.querySelectorAll('.icon-option').forEach(el => el.classList.remove('selected'));

    if (category) {
        // === 编辑模式 ===
        title.textContent = '编辑分类';
        document.getElementById('categoryId').value = category.id;
        document.getElementById('categoryName').value = category.name;
        document.getElementById('categoryColor').value = category.color || '#667eea';
        document.getElementById('categoryType').value = category.type;

        // 设置并高亮图标
        const currentIcon = category.icon || '💰';
        if(iconInput) iconInput.value = currentIcon;
        highlightSelectedIcon(currentIcon);

    } else {
        // === 添加模式 ===
        title.textContent = '添加分类';
        document.getElementById('categoryId').value = '';
        document.getElementById('categoryType').value = currentCategoryType;
        document.getElementById('categoryColor').value = '#667eea';

        // 默认选中第一个图标
        const defaultIcon = ICON_LIBRARY[0];
        if(iconInput) iconInput.value = defaultIcon;
        highlightSelectedIcon(defaultIcon);
    }

    modal.style.display = 'flex';
}

// 4. 新增：辅助函数 - 高亮选中的图标
function highlightSelectedIcon(iconChar) {
    const options = document.querySelectorAll('.icon-option');
    options.forEach(opt => {
        if (opt.textContent === iconChar) {
            opt.classList.add('selected');
            // 自动滚动到该图标位置（提升体验）
            opt.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
        }
    });
}

// 保存分类 (核心逻辑不变，但依赖隐藏Input的值)
function saveCategory(e) {
    if(e) e.preventDefault(); // 防止表单默认提交

    const messageContainer = document.getElementById('categoryFormMessage');
    const categoryId = document.getElementById('categoryId').value;

    // 获取隐藏 Input 的值
    const iconInput = document.getElementById('categoryIcon');

    const categoryData = {
        name: document.getElementById('categoryName').value,
        icon: iconInput ? iconInput.value : '💰', // 从隐藏域取值
        color: document.getElementById('categoryColor').value,
        type: document.getElementById('categoryType').value || currentCategoryType
    };

    if (!categoryData.name.trim()) {
        showMessage(messageContainer, '请输入分类名称', 'error');
        return;
    }
    if (!categoryData.icon) {
        showMessage(messageContainer, '请选择一个图标', 'error');
        return;
    }

    let promise;
    if (categoryId) {
        promise = api.updateCategory(categoryId, categoryData);
    } else {
        promise = api.createCategory(categoryData);
    }

    promise
        .then(data => {
            showMessage(messageContainer, `分类${categoryId ? '更新' : '添加'}成功！`, 'success');
            setTimeout(() => {
                document.getElementById('categoryModal').style.display = 'none';
                loadCategories();
                // 刷新记账页面的下拉框（如果有这个函数的话）
                if(typeof initializeCategories === 'function') initializeCategories();
            }, 500);
        })
        .catch(error => {
            console.error('保存分类失败:', error);
            showMessage(messageContainer, `保存分类失败: ${error.message}`, 'error');
        });
}

// 编辑分类 (保持不变)
function editCategory(id) {
    const category = categoryManagementList.find(cat => cat.id === id);
    if (category) {
        openCategoryModal(category);
    }
}

// 删除分类 (保持不变)
function deleteCategory(id) {
    const category = categoryManagementList.find(cat => cat.id === id);
    if (!category) return;
    if (!confirm(`确定要删除分类 "${category.name}" 吗？`)) return;

    api.deleteCategory(id)
        .then(() => {
            showMessage(document.getElementById('categoryMessage'), '分类删除成功！', 'success');
            loadCategories();
             // 刷新记账页面的下拉框
             if(typeof initializeCategories === 'function') initializeCategories();
        })
        .catch(error => {
            console.error('删除分类失败:', error);
            showMessage(document.getElementById('categoryMessage'), `删除分类失败: ${error.message}`, 'error');
        });
}

// =======================
// 预算监控模块逻辑
// =======================

// DOM 元素获取
const toggleBudgetEditBtn = document.getElementById('toggleBudgetEditBtn');
const budgetEditArea = document.getElementById('budgetEditArea');
const dashboardBudgetInput = document.getElementById('dashboardBudgetInput');
const saveDashboardBudgetBtn = document.getElementById('saveDashboardBudgetBtn');
const cancelBudgetEditBtn = document.getElementById('cancelBudgetEditBtn');

// 1. 加载预算状态函数
function loadBudgetStatus() {
    api.getBudgetStatus()
        .then(data => {
            // 解构后端返回的数据 (BudgetStatusDto)
            const { totalBudget, totalSpent, remaining, percentage } = data;

            // 更新页面上的金额显示
            const elUsed = document.getElementById('dashUsed');
            const elTotal = document.getElementById('dashTotal');
            const elRemaining = document.getElementById('dashRemaining');

            if(elUsed) elUsed.textContent = `¥${totalSpent.toFixed(2)}`;
            if(elTotal) elTotal.textContent = `¥${totalBudget.toFixed(2)}`;
            if(elRemaining) elRemaining.textContent = `¥${remaining.toFixed(2)}`;

            // 更新进度条
            const progressBar = document.getElementById('dashProgressBar');
            const percentText = document.getElementById('dashPercentText');

            if(progressBar && percentText) {
                // 限制进度条显示最大为 100% (即使实际超支，进度条也不要溢出容器)
                const displayPercent = percentage > 100 ? 100 : percentage;

                progressBar.style.width = `${displayPercent}%`;
                percentText.textContent = `${percentage}%`;

                // 根据使用率改变颜色
                progressBar.className = ''; // 清空可能存在的类
                if (percentage >= 100) {
                    progressBar.style.backgroundColor = '#f44336'; // 红色 - 超支
                    if(elRemaining) {
                        elRemaining.style.color = '#f44336';
                        elRemaining.textContent = '已超支'; // 提示文字变更
                    }
                } else if (percentage >= 80) {
                    progressBar.style.backgroundColor = '#ff9800'; // 橙色 - 预警
                    if(elRemaining) elRemaining.style.color = '#ff9800';
                } else {
                    progressBar.style.backgroundColor = '#4caf50'; // 绿色 - 正常
                    if(elRemaining) elRemaining.style.color = '#4caf50';
                }
            }

            // 在输入框中预填当前预算，方便用户修改
            if (dashboardBudgetInput) {
                dashboardBudgetInput.value = totalBudget;
            }
        })
        .catch(err => {
            console.error('加载预算失败', err);
            // 只有不是 404 的时候才报错，因为新用户可能还没设置预算
            if (!err.message.includes('404')) {
                // 如果你有 showMessage 函数可以调用它，否则忽略
            }
        });
}

// 2. 按钮事件绑定
if (toggleBudgetEditBtn) {
    // 点击“设置/修改”按钮，显示输入框
    toggleBudgetEditBtn.addEventListener('click', () => {
        if(budgetEditArea) budgetEditArea.style.display = 'block';
        toggleBudgetEditBtn.style.display = 'none';
        if(dashboardBudgetInput) dashboardBudgetInput.focus();
    });
}

if (cancelBudgetEditBtn) {
    // 点击“取消”按钮，隐藏输入框
    cancelBudgetEditBtn.addEventListener('click', () => {
        if(budgetEditArea) budgetEditArea.style.display = 'none';
        toggleBudgetEditBtn.style.display = 'inline-block';
    });
}

if (saveDashboardBudgetBtn) {
    // 点击“保存”按钮
    saveDashboardBudgetBtn.addEventListener('click', () => {
        const amount = parseFloat(dashboardBudgetInput.value);

        // 简单校验
        if (isNaN(amount) || amount < 0) {
            alert('请输入有效的金额');
            return;
        }

        // 按钮 Loading 状态
        const originalText = saveDashboardBudgetBtn.innerText;
        saveDashboardBudgetBtn.innerText = '保存中...';
        saveDashboardBudgetBtn.disabled = true;

        // 调用 API 保存
        api.setBudget(amount)
            .then(() => {
                // 保存成功后刷新显示
                loadBudgetStatus();
                // 隐藏编辑框
                if(budgetEditArea) budgetEditArea.style.display = 'none';
                toggleBudgetEditBtn.style.display = 'inline-block';

                // 提示成功
                if(window.showMessage) {
                    window.showMessage('formMessage', '预算设置成功', 'success');
                } else {
                    alert('预算设置成功');
                }
            })
            .catch(err => {
                alert('设置失败: ' + err.message);
            })
            .finally(() => {
                // 恢复按钮状态
                saveDashboardBudgetBtn.innerText = originalText;
                saveDashboardBudgetBtn.disabled = false;
            });
    });
}

// 3. 页面加载时自动触发
document.addEventListener('DOMContentLoaded', function() {
    // 只有当页面上存在预算卡片时才去加载数据
    if (document.querySelector('.budget-dashboard-card')) {
        loadBudgetStatus();
    }
});

// =======================
// 汇率查询页面逻辑
// =======================

let isExchangePageInitialized = false; // 防止重复绑定事件和加载

function initExchangePage() {
    if (isExchangePageInitialized) return;

    console.log("初始化汇率查询页面...");

    // 1. 加载货币下拉框 (复用已有的API，但填充到新页面的select)
    loadExchangeCurrencies();

    // 2. 绑定按钮事件
    const btnQueryRate = document.getElementById('btnQueryRate');
    const btnConvert = document.getElementById('btnConvert');
    const btnSwap = document.getElementById('swapCurrencyBtn');

    if (btnQueryRate) btnQueryRate.addEventListener('click', () => handleExchangeAction('rate'));
    if (btnConvert) btnConvert.addEventListener('click', () => handleExchangeAction('convert'));
    if (btnSwap) btnSwap.addEventListener('click', handleSwapCurrencies);

    isExchangePageInitialized = true;
}

// 加载汇率页面的货币列表
function loadExchangeCurrencies() {
    const fromSelect = document.getElementById('exchangeFrom');
    const toSelect = document.getElementById('exchangeTo');

    if (!fromSelect || !toSelect) return;

    // 调用已有的 api.getCurrencies
    api.getCurrencies()
        .then(data => {
            if (data && data.result && data.result.list) {
                const currencies = data.result.list;

                // 生成选项HTML
                let optionsHtml = '';
                currencies.forEach(c => {
                    optionsHtml += `<option value="${c.code}">${c.name} (${c.code})</option>`;
                });

                // 填充 From 下拉框 (默认选 USD)
                fromSelect.innerHTML = optionsHtml;
                // 尝试选中 USD，如果列表中没有则保持默认
                if(currencies.some(c => c.code === 'USD')) fromSelect.value = 'USD';

                // 填充 To 下拉框 (默认选 CNY)
                toSelect.innerHTML = optionsHtml;
                toSelect.value = 'CNY';
            }
        })
        .catch(err => {
            console.error('加载货币列表失败', err);
            showMessage('exchangeMessage', '加载货币列表失败', 'error');
        });
}

// 处理“交换货币”按钮
function handleSwapCurrencies() {
    const fromSelect = document.getElementById('exchangeFrom');
    const toSelect = document.getElementById('exchangeTo');

    const tempVal = fromSelect.value;
    fromSelect.value = toSelect.value;
    toSelect.value = tempVal;

    // 交换后隐藏结果，避免误导
    document.getElementById('exchangeResultArea').style.display = 'none';
}

// 统一处理查询和换算
function handleExchangeAction(actionType) {
    const from = document.getElementById('exchangeFrom').value;
    const to = document.getElementById('exchangeTo').value;
    const amountInput = document.getElementById('exchangeAmount');
    const amount = amountInput.value;

    const msgBoxId = 'exchangeMessage';
    const resultArea = document.getElementById('exchangeResultArea');
    const btnId = actionType === 'rate' ? 'btnQueryRate' : 'btnConvert';
    const btn = document.getElementById(btnId);

    // 校验金额 (仅换算模式)
    if (actionType === 'convert' && (!amount || amount <= 0)) {
        showMessage(msgBoxId, '请输入有效的金额', 'error');
        return;
    }

    // UI Loading 状态
    const originalText = btn.innerHTML;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 处理中...';
    btn.disabled = true;
    hideMessage(msgBoxId);

    // 根据类型调用不同的 API 方法
    let promise;
    if (actionType === 'rate') {
        promise = api.getExchangeRate(from, to);
    } else {
        promise = api.calculateExchange(from, to, amount);
    }

    promise
        .then(data => {
            if (data.success) {
                // 显示结果区域
                resultArea.style.display = 'block';

                // 填充公共汇率信息
                // 注意：后端返回的字段根据接口不同略有差异，这里做统一处理
                // queryRealTimeRate 返回的是 exchangeRate
                // calculateConversionForFrontend 返回的是 rate
                const rate = data.exchangeRate || data.rate;
                const fromCode = data.fromCode || data.from;
                const toCode = data.toCode || data.to;

                document.getElementById('resFromCode').textContent = fromCode;
                document.getElementById('resToCode').textContent = toCode;
                document.getElementById('resRate').textContent = rate;
                document.getElementById('resUpdateTime').textContent = data.updateTime;

                // 控制“换算结果”部分的显示
                const calcBox = document.getElementById('calcResultBox');
                if (actionType === 'convert') {
                    calcBox.style.display = 'block';
                    document.getElementById('resAmount').textContent = parseFloat(data.convertedAmount).toFixed(2);
                    document.getElementById('resSourceAmount').textContent = data.sourceAmount;
                    document.getElementById('resSourceCode').textContent = fromCode;

                    // 简单的符号映射
                    const symbolMap = {'CNY': '¥', 'USD': '$', 'EUR': '€', 'GBP': '£', 'JPY': '¥'};
                    document.getElementById('resSymbol').textContent = symbolMap[toCode] || toCode;
                } else {
                    calcBox.style.display = 'none';
                }
            } else {
                showMessage(msgBoxId, data.message || '操作失败', 'error');
            }
        })
        .catch(err => {
            console.error(err);
            showMessage(msgBoxId, '网络请求失败: ' + err.message, 'error');
        })
        .finally(() => {
            // 恢复按钮
            btn.innerHTML = originalText;
            btn.disabled = false;
        });
}



// =================================================
//  AI 助手逻辑
// =================================================

document.addEventListener('DOMContentLoaded', function() {
    const aiFloatBtn = document.getElementById('aiFloatBtn');
    const aiChatWindow = document.getElementById('aiChatWindow');
    const closeAiChat = document.getElementById('closeAiChat');
    const aiInput = document.getElementById('aiInput');
    const sendAiBtn = document.getElementById('sendAiBtn');
    const aiMessages = document.getElementById('aiMessages');

    if (!aiFloatBtn) return; // 防止页面元素缺失报错

    // 1. 窗口开关
    aiFloatBtn.addEventListener('click', () => {
        aiChatWindow.classList.add('active');
        aiInput.focus();
    });
    closeAiChat.addEventListener('click', () => {
        aiChatWindow.classList.remove('active');
    });

    //2.发送消息逻辑
  async function handleSendAi() {
      const text = aiInput.value.trim();
      if (!text) return;

      addMessage(text, 'user-msg');
      aiInput.value = '';
      sendAiBtn.disabled = true;

      // 显示思考中...
      const loadingElement = addMessage('<i class="fas fa-spinner fa-spin"></i> 思考中...', 'ai-msg');

      try {
          const result = await api.analyzeBill(text);

          // 移除思考中
          if (loadingElement) loadingElement.remove();

          if (result.action === 'RECORD' && result.data) {
              const data = result.data;

              // 1. 生成消息 HTML
              const html = `已识别：${data.categoryName} ${data.amount}元。<br>
                            <a href="#" class="auto-fill-link" style="color:#673ab7;font-weight:bold;text-decoration:underline;">
                                点击此处自动填单
                            </a>`;

              // 2. 添加消息到界面，并获取该消息的 DOM 元素
              const msgElement = addMessage(html, 'ai-msg');

              // 3. ★★★ 手动绑定点击事件 ★★★
              // 因为 data 变量只在这个作用域里有效，必须在这里绑定
              const link = msgElement.querySelector('.auto-fill-link');
              if (link) {
                  link.addEventListener('click', (e) => {
                      e.preventDefault(); // 阻止链接默认跳转
                      fillBillForm(data); // 调用填单函数
                  });
              }

              // 可选：如果你想 AI 回复完直接自动跳转，把下面这行注释打开
              fillBillForm(data);

          } else {
              // 闲聊回复
              let reply = result.reply || "我没太听懂，请再说详细点";
              reply = reply.replace(/\n/g, '<br>');
              addMessage(reply, 'ai-msg');
          }

      } catch (error) {
          console.error(error);
          if (loadingElement) loadingElement.innerHTML = `<span style="color:red">请求失败: ${error.message}</span>`;
      } finally {
          sendAiBtn.disabled = false;
          const aiMessages = document.getElementById('aiMessages');
          if(aiMessages) aiMessages.scrollTop = aiMessages.scrollHeight;
      }
  }

    // 3. 辅助函数：自动填单并跳转
    // === 辅助函数：自动填单并跳转 ===
    function fillBillForm(data) {
        console.log("开始自动填单:", data);

        // 1. 切换到记账页面 (Tab)
        const recordLink = document.querySelector('.nav-link[data-page="record"]');
        if (recordLink) {
            recordLink.click();
        }

        // 延迟一点点，确保页面切换完成
        setTimeout(() => {
            // 2. 填金额
            const amountInput = document.getElementById('amount');
            if(amountInput) amountInput.value = data.amount;

            // 3. 填类型 (支出/收入)
            const typeSelect = document.getElementById('type');
            if(typeSelect) {
                // 后端返回的是 EXPENSE/INCOME，前端对应 支出/收入
                const typeMap = { 'EXPENSE': '支出', 'INCOME': '收入' };
                const targetValue = typeMap[data.billType] || '支出';

                // 只有当值不一样时才切换并触发 change 事件
                if (typeSelect.value !== targetValue) {
                    typeSelect.value = targetValue;
                    // 手动触发 change 事件，让分类下拉框重新加载
                    typeSelect.dispatchEvent(new Event('change'));
                }
            }

            // 4. 填备注
            const remarkInput = document.getElementById('remark');
            if(remarkInput) remarkInput.value = data.remark || '';

            // 5. 填分类 (需要延迟更多，等待 type change 事件导致的分类加载完成)
            setTimeout(() => {
                const categorySelect = document.getElementById('category');
                if(categorySelect && data.categoryName) {
                    let found = false;
                    // 遍历下拉框选项进行模糊匹配
                    for(let i = 0; i < categorySelect.options.length; i++) {
                        const opt = categorySelect.options[i];
                        // 比如 AI 返回 "餐饮"，选项是 "🍔 餐饮"，用 includes 匹配
                        if(opt.text.includes(data.categoryName)) {
                            categorySelect.selectedIndex = i;
                            found = true;
                            break;
                        }
                    }
                    if(!found) console.warn('未找到匹配的分类:', data.categoryName);
                }

                // 6. 滚动到表单并高亮金额框
                const form = document.getElementById('billForm');
                if(form) form.scrollIntoView({ behavior: 'smooth' });
                if(amountInput) amountInput.focus();

            }, 500); // 等待分类加载的时间

            // 7. 手机端自动关闭聊天窗口
            if(window.innerWidth < 768) {
                 const aiChatWindow = document.getElementById('aiChatWindow');
                 if(aiChatWindow) aiChatWindow.classList.remove('active');
            }

        }, 100);
    }

    // 4.界面工具函数：添加消息并返回 DOM 元素
        function addMessage(html, className) {
            const div = document.createElement('div');
            div.className = `message ${className}`;
            div.innerHTML = html;

            // 这里的 ID 逻辑可以保留用于调试，但删除时不依赖它了
            div.id = 'msg-' + Date.now() + Math.random().toString(36).substr(2, 9);

            aiMessages.appendChild(div);
            aiMessages.scrollTop = aiMessages.scrollHeight;

            // 绑定自动填单链接点击 (保持原有逻辑)
            const link = div.querySelector('.auto-fill-link');
            if(link) {
                link.addEventListener('click', (e) => {
                    e.preventDefault();
                });
            }

            return div; // ★★★ 关键修改：直接返回 DOM 元素对象
        }

    // 事件绑定
    if(sendAiBtn) sendAiBtn.addEventListener('click', handleSendAi);
    if(aiInput) {
        aiInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') handleSendAi();
        });
    }
});

// ================= AI 月度报告逻辑 =================

// 加载 AI 报告
function loadAiReport() {
    const reportContainer = document.getElementById('aiReportContent');
    if (!reportContainer) return;

    // 设置 Loading 状态
    reportContainer.innerHTML = '<div style="color:#666; display:flex; align-items:center; gap:10px;"><i class="fas fa-spinner fa-spin fa-lg"></i> <span>正在召唤 AI 理财师分析您的账单...</span></div>';

    // 禁用刷新按钮防止连点
    const refreshBtn = document.getElementById('refreshReportBtn');
    if(refreshBtn) refreshBtn.disabled = true;

    api.getAiMonthlyReport()
        .then(data => {
            if (data.error) {
                reportContainer.innerHTML = `<span style="color:red">分析失败: ${data.error}</span>`;
            } else {
                // 成功显示，添加打字机效果或渐显效果
                reportContainer.innerHTML = data.content.replace(/\n/g, '<br>');
            }
        })
        .catch(err => {
            console.error(err);
            reportContainer.innerHTML = '<span style="color:red">网络连接失败，无法获取报告</span>';
        })
        .finally(() => {
            if(refreshBtn) refreshBtn.disabled = false;
        });
}

// 绑定刷新按钮事件
const refreshReportBtn = document.getElementById('refreshReportBtn');
if (refreshReportBtn) {
    refreshReportBtn.addEventListener('click', loadAiReport);
}






// 将函数暴露到全局作用域
window.downloadBackup = downloadBackup;
window.deleteBackup = deleteBackup;
window.editCategory = editCategory;
window.deleteCategory = deleteCategory;
