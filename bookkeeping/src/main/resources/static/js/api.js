// API基础URL
const API_BASE = '/api/bill';
const CURRENCY_API_BASE = '/api/currency';
const CATEGORY_API_BASE = '/api/categories';


function authFetch(url, options = {}) {
    const token = localStorage.getItem('authToken');

    console.log('🔐 JWT认证调试:', {
        url: url,
        method: options.method || 'GET',
        token存在: !!token,
        token前20位: token ? token.substring(0, 20) + '...' : '无token',
        localStorage内容: JSON.stringify(localStorage)
    });

    const defaultOptions = {
        headers: {
            'Content-Type': 'application/json',
            ...options.headers
        }
    };

    if (token) {
        defaultOptions.headers['Authorization'] = `Bearer ${token}`;
        console.log('✅ 已设置Authorization头');
    } else {
        console.warn('❌ 未找到JWT token，请求将无认证信息');
    }

    const finalOptions = {
        ...defaultOptions,
        ...options
    };

    return fetch(url, finalOptions)
        .then(response => {
            console.log('📡 API响应:', {
                url: url,
                状态码: response.status,
                状态文本: response.statusText,
                是否成功: response.ok,
                认证头: response.headers.get('WWW-Authenticate')
            });

            if (response.status === 401) {
                console.error('❌ 401未认证，清除token并重定向');
                localStorage.removeItem('authToken');
                window.location.href = '/html/login.html';
                return Promise.reject(new Error('未认证'));
            }

            if (response.status === 403) {
                console.error('❌ 403禁止访问，权限不足');
                return response.text().then(text => {
                    console.error('403错误详情:', text);
                    throw new Error(`权限不足: ${response.statusText}`);
                });
            }

            if (!response.ok) {
                return response.text().then(text => {
                    console.error('❌ 请求失败:', text);
                    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                });
            }

            return response;
        })
        .catch(error => {
            console.error('🚨 网络请求错误:', error);
            throw error;
        });
}

// API调用函数
const api = {
    // 获取账单列表 (修改后支持搜索)
        getBills: (period = '', keyword = '') => {
            // 构建查询参数
            const params = new URLSearchParams();
            if (period) params.append('period', period);
            if (keyword) params.append('keyword', keyword);

            // 拼接 URL
            const queryString = params.toString();
            const url = queryString ? `${API_BASE}?${queryString}` : API_BASE;

            return authFetch(url)
                .then(response => response.json());
        },

    // 添加账单 - 使用BillDto
    addBill: (billData) => {
        return authFetch(API_BASE, {
            method: 'POST',
            body: JSON.stringify(billData)
        })
        .then(response => response.json());
    },

    // 获取单个账单
    getBill: (id) => {
        return authFetch(`${API_BASE}/${id}`)
            .then(response => response.json());
    },

    // 更新账单 - 使用BillDto
    updateBill: (id, billData) => {
        return authFetch(`${API_BASE}/${id}`, {
            method: 'PUT',
            body: JSON.stringify(billData)
        })
        .then(response => response.json());
    },

    // 删除账单
    deleteBill: (id) => {
        return authFetch(`${API_BASE}/${id}`, {
            method: 'DELETE'
        })
        .then(response => response.json());
    },

    // 创建分类 - 使用CategoryDto
    createCategory: (categoryData) => {
        return authFetch(CATEGORY_API_BASE, {
            method: 'POST',
            body: JSON.stringify(categoryData)
        })
        .then(response => response.json());
    },

    // 获取统计信息
    getStatistics: (period = '') => {
        const url = period ? `${API_BASE}/statistics?period=${period}` : `${API_BASE}/statistics`;
        return authFetch(url)
            .then(response => response.json());
    },

    // 获取货币列表
    getCurrencies: () => {
        return authFetch(CURRENCY_API_BASE + '/list')
            .then(response => response.json());
    },

    // 查询实时汇率 (只查询，不计算)
        getExchangeRate: (from, to) => {
            return authFetch(`${CURRENCY_API_BASE}/rate?from=${from}&to=${to}`)
                .then(response => response.json());
        },

    // 货币换算计算器 (带金额计算)
        calculateExchange: (from, to, amount) => {
            return authFetch(`${CURRENCY_API_BASE}/convert?from=${from}&to=${to}&amount=${amount}`)
                .then(response => response.json());
        },

    // 下载账单
    downloadBills: () => {
        return authFetch('/api/files/download/bills');
    },

   // 上传账单
   uploadBills: (formData) => {
       // 注意：不要手动设置 Content-Type，让浏览器自动设置 multipart/form-data
       const token = localStorage.getItem('authToken');

       // 创建一个新的 headers 对象，但不包含 Content-Type
       const headers = {};
       if (token) {
           headers['Authorization'] = `Bearer ${token}`;
       }

       return authFetch('/api/files/upload/bills', {
           method: 'POST',
           headers: headers, // 不包含 Content-Type
           body: formData
       })
       .then(response => response.text());
   },


     // 获取分类列表
        getCategories: (type) => {
            return authFetch(`${CATEGORY_API_BASE}?type=${type}`)
                .then(response => response.json());
        },

    // 获取系统分类
    getSystemCategories: (type) => {
        return authFetch(`${CATEGORY_API_BASE}/system?type=${type}`)
            .then(response => response.json());
    },

    // 获取用户分类
    getUserCategories: (type) => {
        return authFetch(`${CATEGORY_API_BASE}/user?type=${type}`)
            .then(response => response.json());
    },

    // 更新分类
    updateCategory: (id, categoryData) => {
        return authFetch(`${CATEGORY_API_BASE}/${id}`, {
            method: 'PUT',
            body: JSON.stringify(categoryData)
        })
        .then(response => response.json());
    },

    // 删除分类
    deleteCategory: (id) => {
        return authFetch(`${CATEGORY_API_BASE}/${id}`, {
            method: 'DELETE'
        });
    },

     // 获取备份列表
         listBackups: () => {
             return authFetch('/api/backup/list')
                 .then(response => response.json());
         },

         // 创建备份
         createBackup: () => {
             return authFetch('/api/backup/create', {
                 method: 'POST'
             })
             .then(response => response.text());
         },

         // 下载备份
         downloadBackup: (fileName) => {
             const token = localStorage.getItem('authToken');

             // 对于文件下载，直接使用原生fetch
             return fetch(`/api/backup/download?fileName=${encodeURIComponent(fileName)}`, {
                 headers: {
                     'Authorization': `Bearer ${token}`
                 }
             });
         },

         // 删除备份
         deleteBackup: (fileName) => {
             return authFetch(`/api/backup/delete?fileName=${encodeURIComponent(fileName)}`, {
                 method: 'DELETE'
             })
             .then(response => response.text());
         },

         // 恢复备份
         restoreBackup: (fileName) => {
             return authFetch(`/api/backup/restore?fileName=${encodeURIComponent(fileName)}`, {
                 method: 'POST'
             })
             .then(response => response.text());
         },

         // 获取自动备份设置
         getBackupSetting: () => {
             return authFetch('/api/backupsetting/get')
                 .then(response => response.json());
         },

         // 更新自动备份设置
         updateBackupSetting: (setting) => {
             return authFetch('/api/backupsetting/update', {
                 method: 'POST',
                 body: JSON.stringify(setting)
             })
             .then(response => response.text());
         },


         // 获取当前用户信息
             getCurrentUser: () => {
                 return authFetch('/api/user/current')
                     .then(response => response.json());
             },

         // 更新个人资料
             updateProfile: (userData) => {
                 return authFetch('/api/user/profile', {
                     method: 'PUT',
                     body: JSON.stringify(userData)
                 })
                 .then(response => response.json());
             },

             // 上传头像
             uploadAvatar: (formData) => {
                 const token = localStorage.getItem('authToken');
                 const headers = {};
                 if (token) {
                     headers['Authorization'] = `Bearer ${token}`;
                 }

                 // 不需要手动设置 Content-Type，浏览器会自动处理 Multipart
                 return fetch('/api/user/avatar', {
                     method: 'POST',
                     headers: headers,
                     body: formData
                 })
                 .then(async response => {
                     const text = await response.text();
                     if (!response.ok) {
                         throw new Error(text);
                     }
                     return text; // 返回头像URL
                 });
             },

         //修改密码
          changePassword: (passwordData) => {
                 // passwordData 包含: { oldPassword: "...", newPassword: "..." }
                 return authFetch('/api/user/password', {
                     method: 'PUT',
                     body: JSON.stringify(passwordData)
                 })
                 .then(async response => {
                      // 因为后端返回的是 String 简单文本，不是 JSON 对象，所以用 text() 解析
                      // 如果你 Controller 用的是 Result 包装类，这里要改回 json()
                      const text = await response.text();
                      if (!response.ok) {
                          throw new Error(text || '修改失败');
                      }
                      return text;
                 });
             },

          // 获取预算状态
              getBudgetStatus: () => {
                  // 对应 UserController 的 @GetMapping("/budget")
                  return authFetch('/api/user/budget')
                      .then(response => response.json());
              },

          // 设置预算金额
              setBudget: (amount) => {
                  // 对应 UserController 的 @PostMapping("/budget")
                  return authFetch('/api/user/budget', {
                      method: 'POST',
                      body: JSON.stringify({ amount: amount })
                  })
                  .then(async response => {
                       const text = await response.text();
                       if (!response.ok) throw new Error(text);
                       return text;
                  });
              },


          // 获取趋势数据
              getTrendData: (days = 7) => {
                  return authFetch(`${API_BASE}/trend?days=${days}`)
                      .then(response => response.json());
              },

          //AI智能分析
          // AI 智能分析
          analyzeBill: (text) => {
              return authFetch('/api/ai/chat', {
                  method: 'POST',
                  body: JSON.stringify({
                  text: text
                  })
              })
              .then(response => response.json());
          },

     // 获取 AI 月度分析报告
         getAiMonthlyReport: () => {
             return authFetch('/api/ai/report')
                 .then(response => response.json());
         }


};

// 导出API对象
window.api = api;
