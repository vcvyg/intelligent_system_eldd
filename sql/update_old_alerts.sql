-- 更新旧的告警记录，修改为新的格式
-- 注意：这是简化版本，如果内容格式不匹配可能需要手动调整

-- 1. 更新告警等级：将"警告"改为"中"
UPDATE alert_record
SET alert_level = '中'
WHERE alert_level = '警告';

-- 2. 更新告警类型：将"健康告警"改为具体类型
UPDATE alert_record
SET alert_type = '心率异常'
WHERE alert_type = '健康告警'
  AND alert_content LIKE '%心率%';

UPDATE alert_record
SET alert_type = '血压异常'
WHERE alert_type = '健康告警'
  AND (alert_content LIKE '%血压%' OR alert_content LIKE '%收缩压%' OR alert_content LIKE '%舒张压%');

UPDATE alert_record
SET alert_type = '体温异常'
WHERE alert_type = '健康告警'
  AND alert_content LIKE '%体温%';

-- 3. 简化告警内容（去掉"老人 [xxx]"前缀和"，检测值为 xxx"后缀）
-- 心率告警
UPDATE alert_record
SET alert_content = '心率偏高，' + alert_value + ' bpm'
WHERE alert_type = '心率异常'
  AND alert_content LIKE '%过高%';

UPDATE alert_record
SET alert_content = '心率偏低，' + alert_value + ' bpm'
WHERE alert_type = '心率异常'
  AND alert_content LIKE '%过低%';

-- 血压告警（需要根据实际情况调整）
UPDATE alert_record
SET alert_content = '血压偏高，收缩压' + alert_value + 'mmHg'
WHERE alert_type = '血压异常'
  AND alert_content LIKE '%收缩压%过高%';

UPDATE alert_record
SET alert_content = '血压偏低，收缩压' + alert_value + 'mmHg'
WHERE alert_type = '血压异常'
  AND alert_content LIKE '%收缩压%过低%';

-- 体温告警
UPDATE alert_record
SET alert_content = '体温偏高，' + alert_value + '°C'
WHERE alert_type = '体温异常'
  AND alert_content LIKE '%过高%';

UPDATE alert_record
SET alert_content = '体温偏低，' + alert_value + '°C'
WHERE alert_type = '体温异常'
  AND alert_content LIKE '%过低%';

-- 4. 查看更新结果
SELECT id, alert_type, alert_level, alert_content, alert_value, status
FROM alert_record
WHERE deleted = 0
ORDER BY alert_time DESC;

GO
