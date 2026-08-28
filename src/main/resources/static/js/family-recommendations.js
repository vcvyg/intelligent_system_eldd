let familyElderlyList = [];
let selectedElderly = null;
let feedbackBusy = false;

const categoryLabels = {
  HEALTH_CHECK: '健康记录',
  SAFETY: '安全提醒',
  CARE_SERVICE: '生活服务',
  WELLNESS: '日常关怀',
  FAMILY_SUPPORT: '家庭陪伴'
};

document.addEventListener('DOMContentLoaded', async () => {
  const user = checkLogin();
  if (!user || user.role !== 'FAMILY') {
    alert('仅家属账号可以访问关怀推荐');
    logout();
    return;
  }
  document.getElementById('welcomeText').textContent = `欢迎，${user.username || '家属'}！`;
  document.getElementById('elderlySelect').addEventListener('change', onSelectElderly);
  document.getElementById('refreshBtn').addEventListener('click', loadFeed);
  await loadElderly();
});

async function loadElderly() {
  try {
    const response = await get('/family/health/dashboard?range=today');
    familyElderlyList = Array.isArray(response?.data?.elderlyList) ? response.data.elderlyList : [];
    const select = document.getElementById('elderlySelect');
    select.innerHTML = '<option value="">选择家人</option>' + familyElderlyList.map(item => {
      const id = item.elderly_id || item.elderlyId || item.id;
      return `<option value="${id}">${escapeHtml(item.name || `老人#${id}`)}</option>`;
    }).join('');
    if (familyElderlyList.length > 0) {
      const firstId = familyElderlyList[0].elderly_id || familyElderlyList[0].elderlyId || familyElderlyList[0].id;
      select.value = firstId;
      onSelectElderly();
      await loadFeed();
    }
  } catch (error) {
    showStatus(error.message || '关联家人加载失败', true);
  }
}

function onSelectElderly() {
  const id = Number(document.getElementById('elderlySelect').value);
  selectedElderly = familyElderlyList.find(item => Number(item.elderly_id || item.elderlyId || item.id) === id) || null;
  document.getElementById('selectedName').textContent = selectedElderly?.name || '未选择';
  if (selectedElderly) loadFeed();
}

async function loadFeed() {
  if (!selectedElderly) {
    document.getElementById('recommendationFeed').innerHTML = '<div class="recommend-empty">请选择家人查看已投放内容。</div>';
    return;
  }
  const elderlyId = selectedElderly.elderly_id || selectedElderly.elderlyId || selectedElderly.id;
  try {
    const response = await get(`/family/recommendations/${elderlyId}`);
    renderFeed(response?.data || [], elderlyId);
  } catch (error) {
    showStatus(error.message || '推荐加载失败', true);
  }
}

function renderFeed(items, elderlyId) {
  const feed = document.getElementById('recommendationFeed');
  if (!items.length) {
    feed.innerHTML = '<div class="recommend-empty">暂时还没有站内推荐。管理员完成一次“预览 → 投放”后，这里会收到真实投放记录。</div>';
    return;
  }

  feed.innerHTML = items.map(item => `
    <article class="recommend-card family-recommend-card" data-delivery-id="${item.deliveryId}">
      <div class="recommend-card-head">
        <div>
          <span class="recommend-badge">${escapeHtml(categoryLabels[item.category] || item.category || '关怀')}</span>
          <h3>${escapeHtml(item.title || '关怀内容')}</h3>
        </div>
      </div>
      <p>${escapeHtml(item.summary || '')}</p>
      <div class="recommend-reason"><strong>推荐依据：</strong>${escapeHtml(item.reason || '基础关怀优先级')}</div>
      ${item.feedbackState && item.feedbackState !== 'DELIVERED' ? `<div class="feedback-state">当前状态：${escapeHtml(statusLabel(item.feedbackState))}</div>` : ''}
      <div class="recommend-actions">
        <button class="recommend-btn secondary" data-feedback="USEFUL" data-id="${item.deliveryId}" data-elderly="${elderlyId}">👍 有用</button>
        <button class="recommend-btn danger-soft" data-feedback="NOT_INTERESTED" data-id="${item.deliveryId}" data-elderly="${elderlyId}">不感兴趣</button>
        ${item.actionUrl ? `<button class="recommend-btn ghost" data-action-url="${escapeHtml(item.actionUrl)}" data-id="${item.deliveryId}" data-elderly="${elderlyId}">${escapeHtml(item.actionLabel || '查看')}</button>` : ''}
      </div>
    </article>`).join('');

  feed.querySelectorAll('[data-feedback]').forEach(button => {
    button.addEventListener('click', () => sendFeedback(Number(button.dataset.elderly), Number(button.dataset.id), button.dataset.feedback));
  });
  feed.querySelectorAll('[data-action-url]').forEach(button => {
    button.addEventListener('click', async () => {
      await sendFeedback(Number(button.dataset.elderly), Number(button.dataset.id), 'CLICK', false);
      window.location.href = button.dataset.actionUrl;
    });
  });
}

async function sendFeedback(elderlyId, deliveryId, feedbackType, refresh = true) {
  if (feedbackBusy) return;
  feedbackBusy = true;
  try {
    await post('/family/recommendations/feedback', { elderlyId, deliveryId, feedbackType });
    if (feedbackType === 'NOT_INTERESTED') {
      showStatus('已记住“不感兴趣”。这条内容会退出推荐，同类别偏好也会降低。');
    } else if (feedbackType === 'USEFUL') {
      showStatus('已记住“有用”。同类别内容下一轮会获得正向权重。');
    }
    if (refresh) await loadFeed();
  } catch (error) {
    showStatus(error.message || '反馈失败', true);
  } finally {
    feedbackBusy = false;
  }
}

function statusLabel(status) {
  return ({ USEFUL: '已标记有用', CLICKED: '已查看', DELIVERED: '已投放' })[status] || status;
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
