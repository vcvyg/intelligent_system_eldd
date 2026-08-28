let elderlyList = [];
let selectedElderly = null;

const categoryLabels = {
  HEALTH_CHECK: '健康记录',
  SAFETY: '安全提醒',
  CARE_SERVICE: '生活服务',
  WELLNESS: '日常关怀',
  FAMILY_SUPPORT: '家庭陪伴'
};

const triggerStatusLabels = {
  PENDING_REVIEW: '待人工复核',
  APPROVED: '已批准 · 待投放',
  REJECTED: '已拒绝',
  DELIVERED: '已投放'
};

document.addEventListener('DOMContentLoaded', async () => {
  const user = checkLogin();
  if (!user || user.role !== 'ADMIN') {
    alert('仅管理员可以访问推荐投放工作台');
    logout();
    return;
  }
  document.getElementById('welcomeText').textContent = `欢迎，${user.username || '管理员'}！`;
  document.getElementById('elderlySelect').addEventListener('change', onSelectElderly);
  document.getElementById('previewBtn').addEventListener('click', previewRecommendations);
  document.getElementById('deliverBtn').addEventListener('click', deliverRecommendations);
  await loadElderly();
});

async function loadElderly() {
  try {
    const response = await get('/admin/elderly/all');
    elderlyList = Array.isArray(response?.data) ? response.data : [];
    const select = document.getElementById('elderlySelect');
    select.innerHTML = '<option value="">选择老人</option>' + elderlyList.map(item =>
      `<option value="${item.id}">${escapeHtml(item.name || `老人#${item.id}`)}${item.roomNumber ? ` · ${escapeHtml(item.roomNumber)}房` : ''}</option>`
    ).join('');
    if (elderlyList.length === 1) {
      select.value = elderlyList[0].id;
      await onSelectElderly();
      await previewRecommendations();
    }
  } catch (error) {
    showStatus(error.message || '老人列表加载失败', true);
  }
}

async function onSelectElderly() {
  const id = Number(document.getElementById('elderlySelect').value);
  selectedElderly = elderlyList.find(item => Number(item.id) === id) || null;
  document.getElementById('selectedName').textContent = selectedElderly?.name || '未选择';
  if (!selectedElderly) {
    document.getElementById('pendingTriggerCount').textContent = '0';
    document.getElementById('triggerFeed').innerHTML = '<div class="recommend-empty">选择老人后查看待复核事件。</div>';
    document.getElementById('recommendationFeed').innerHTML = '<div class="recommend-empty">选择老人后预览推荐。</div>';
    return;
  }
  await loadTriggers();
}

async function loadTriggers() {
  if (!selectedElderly) return;
  try {
    const response = await get(`/admin/recommendations/triggers?elderlyId=${selectedElderly.id}`);
    const triggers = Array.isArray(response?.data) ? response.data : [];
    const pendingCount = triggers.filter(item => item.status === 'PENDING_REVIEW').length;
    document.getElementById('pendingTriggerCount').textContent = String(pendingCount);
    renderTriggers(triggers);
  } catch (error) {
    document.getElementById('pendingTriggerCount').textContent = '-';
    document.getElementById('triggerFeed').innerHTML = '<div class="recommend-empty">触发队列暂时不可用，请确认已执行最新推荐中心 SQL。</div>';
  }
}

function renderTriggers(items) {
  const feed = document.getElementById('triggerFeed');
  if (!items.length) {
    feed.innerHTML = '<div class="recommend-empty">当前没有待复核业务事件。仍可手动预览推荐。</div>';
    return;
  }

  feed.innerHTML = items.map(item => {
    const status = item.status || 'PENDING_REVIEW';
    const isPending = status === 'PENDING_REVIEW';
    const reviewInfo = item.reviewedAt
      ? `<div class="recommend-reason"><strong>复核：</strong>${escapeHtml(formatTime(item.reviewedAt))}${item.decisionReason ? ` · ${escapeHtml(item.decisionReason)}` : ''}</div>`
      : '';
    const actions = isPending
      ? `<div class="recommend-actions">
           <button class="recommend-btn primary" type="button" onclick="reviewTrigger(${Number(item.id)}, 'approve')">批准候选</button>
           <button class="recommend-btn secondary" type="button" onclick="reviewTrigger(${Number(item.id)}, 'reject')">拒绝本次触发</button>
         </div>`
      : '<div class="recommend-actions"><span class="recommend-badge">等待人工确认投放</span></div>';

    return `
      <article class="recommend-card">
        <div class="recommend-card-head">
          <div>
            <span class="recommend-badge">${escapeHtml(signalTypeLabel(item.signalType))}</span>
            <h3>${escapeHtml(item.signalLabel || '业务变化触发关怀复核')}</h3>
          </div>
          <span class="recommend-badge">${escapeHtml(triggerStatusLabels[status] || status)}</span>
        </div>
        <p>触发时间：${escapeHtml(formatTime(item.triggerTime))}</p>
        <div class="recommend-reason"><strong>隐私边界：</strong>事件队列只保存最小业务标识，不复制告警正文、健康测量值或家属沟通内容。</div>
        ${reviewInfo}
        ${actions}
      </article>`;
  }).join('');
}

async function reviewTrigger(triggerId, action) {
  if (!selectedElderly || !Number.isFinite(Number(triggerId))) return;
  const isApprove = action === 'approve';
  const reason = window.prompt(
    isApprove ? '可选：填写批准原因（不填写也可以）' : '请填写拒绝原因（建议简短说明）',
    ''
  );
  if (reason === null) return;

  setBusy(true);
  try {
    const query = reason.trim() ? `?reason=${encodeURIComponent(reason.trim())}` : '';
    await post(`/admin/recommendations/triggers/${triggerId}/${isApprove ? 'approve' : 'reject'}${query}`, {});
    showStatus(isApprove ? '已完成复核：该事件已批准，可继续预览并人工确认投放。' : '已拒绝该触发事件，本次不会进入投放确认。');
    await loadTriggers();
  } catch (error) {
    showStatus(error.message || '事件复核失败', true);
  } finally {
    setBusy(false);
  }
}

async function previewRecommendations() {
  if (!selectedElderly) {
    showStatus('请先选择老人', true);
    return;
  }
  setBusy(true);
  try {
    const response = await get(`/admin/recommendations/preview/${selectedElderly.id}`);
    renderRecommendations(response?.data || []);
    await loadTriggers();
    showStatus(`已根据 ${selectedElderly.name || '当前老人'} 的系统信号生成可解释 Top 3，事件驱动场景需先完成复核再人工确认投放。`);
  } catch (error) {
    showStatus(error.message || '推荐预览失败', true);
  } finally {
    setBusy(false);
  }
}

async function deliverRecommendations() {
  if (!selectedElderly) {
    showStatus('请先选择老人', true);
    return;
  }
  setBusy(true);
  try {
    const response = await post(`/admin/recommendations/deliver/${selectedElderly.id}`, {});
    const count = Number(response?.data || 0);
    showStatus(count > 0
      ? `已人工确认并创建 ${count} 条站内投放；已批准事件已进入 DELIVERED。`
      : '今天没有新增投放：可能尚未关联家属，或同一内容已被幂等拦截；事件状态不会被误消费。');
    await loadTriggers();
    await previewRecommendations();
  } catch (error) {
    showStatus(error.message || '投放失败', true);
  } finally {
    setBusy(false);
  }
}

function renderRecommendations(items) {
  const feed = document.getElementById('recommendationFeed');
  if (!items.length) {
    feed.innerHTML = '<div class="recommend-empty">内容池暂无可用推荐，请先执行 sql/add_recommendation_center.sql 初始化演示内容。</div>';
    return;
  }
  feed.innerHTML = items.map((item, index) => `
    <article class="recommend-card">
      <div class="recommend-card-head">
        <div>
          <span class="recommend-badge">${escapeHtml(categoryLabels[item.category] || item.category || '关怀')}</span>
          <h3>${escapeHtml(item.title || '关怀内容')}</h3>
        </div>
        <div class="recommend-score">${escapeHtml(formatScore(item.score))}<small> 分</small></div>
      </div>
      <p>${escapeHtml(item.summary || '')}</p>
      <div class="recommend-reason"><strong>为什么推荐：</strong>${escapeHtml(item.reason || '基础关怀优先级')}</div>
      <div class="recommend-actions">
        <span class="recommend-badge">候选 #${index + 1}</span>
        ${item.actionLabel ? `<span class="recommend-badge">${escapeHtml(item.actionLabel)}</span>` : ''}
      </div>
    </article>`).join('');
}

function signalTypeLabel(value) {
  return ({
    ALERT_RAISED: '告警事件',
    HEALTH_RECORDED: '健康记录事件',
    SERVICE_SCHEDULED: '服务安排事件'
  })[value] || '业务事件';
}

function formatScore(value) {
  const n = Number(value);
  return Number.isFinite(n) ? n.toFixed(n % 1 === 0 ? 0 : 1) : '-';
}

function formatTime(value) {
  if (!value) return '-';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString('zh-CN', { hour12: false });
}

function setBusy(value) {
  document.getElementById('previewBtn').disabled = value;
  document.getElementById('deliverBtn').disabled = value;
}

function showStatus(message, error = false) {
  const bar = document.getElementById('statusBar');
  bar.textContent = message;
  bar.classList.toggle('error', error);
  bar.style.display = 'block';
}

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}
