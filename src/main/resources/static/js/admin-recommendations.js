let elderlyList = [];
let selectedElderly = null;

const categoryLabels = {
  HEALTH_CHECK: '健康记录',
  SAFETY: '安全提醒',
  CARE_SERVICE: '生活服务',
  WELLNESS: '日常关怀',
  FAMILY_SUPPORT: '家庭陪伴'
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
      onSelectElderly();
      await previewRecommendations();
    }
  } catch (error) {
    showStatus(error.message || '老人列表加载失败', true);
  }
}

function onSelectElderly() {
  const id = Number(document.getElementById('elderlySelect').value);
  selectedElderly = elderlyList.find(item => Number(item.id) === id) || null;
  document.getElementById('selectedName').textContent = selectedElderly?.name || '未选择';
  if (!selectedElderly) {
    document.getElementById('recommendationFeed').innerHTML = '<div class="recommend-empty">选择老人后预览推荐。</div>';
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
    showStatus(`已根据 ${selectedElderly.name || '当前老人'} 的系统信号生成可解释 Top 3。`);
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
      ? `已创建 ${count} 条站内投放。关联家属现在可以在 C 端看到推荐。`
      : '今天没有新增投放：可能尚未关联家属，或同一内容已被幂等拦截。');
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

function formatScore(value) {
  const n = Number(value);
  return Number.isFinite(n) ? n.toFixed(n % 1 === 0 ? 0 : 1) : '-';
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
