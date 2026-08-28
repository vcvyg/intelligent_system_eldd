/*
 * 主动关怀推荐中心（SQL Server）
 * 仅使用脱敏、通用业务字段，不包含任何生产系统接口或数据。
 */

IF OBJECT_ID('dbo.recommendation_content', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.recommendation_content (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        code NVARCHAR(64) NOT NULL UNIQUE,
        title NVARCHAR(120) NOT NULL,
        summary NVARCHAR(500) NULL,
        category NVARCHAR(64) NOT NULL,
        base_score DECIMAL(8,2) NOT NULL DEFAULT 50,
        action_label NVARCHAR(64) NULL,
        action_url NVARCHAR(255) NULL,
        enabled INT NOT NULL DEFAULT 1,
        create_time DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        update_time DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        deleted INT NOT NULL DEFAULT 0
    );
END;
GO

IF OBJECT_ID('dbo.recommendation_delivery', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.recommendation_delivery (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        elderly_id BIGINT NOT NULL,
        family_user_id BIGINT NOT NULL,
        content_id BIGINT NOT NULL,
        channel NVARCHAR(32) NOT NULL DEFAULT 'IN_APP',
        status NVARCHAR(32) NOT NULL DEFAULT 'DELIVERED',
        score DECIMAL(8,2) NULL,
        reason NVARCHAR(1000) NULL,
        exposed_at DATETIME2 NULL,
        clicked_at DATETIME2 NULL,
        create_time DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        update_time DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        deleted INT NOT NULL DEFAULT 0
    );
    CREATE INDEX idx_recommend_delivery_family_elderly
        ON dbo.recommendation_delivery(family_user_id, elderly_id, create_time DESC);
END;
GO

IF OBJECT_ID('dbo.recommendation_feedback', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.recommendation_feedback (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        elderly_id BIGINT NOT NULL,
        family_user_id BIGINT NOT NULL,
        content_id BIGINT NOT NULL,
        delivery_id BIGINT NOT NULL,
        feedback_type NVARCHAR(32) NOT NULL,
        weight INT NOT NULL DEFAULT 0,
        create_time DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        update_time DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        deleted INT NOT NULL DEFAULT 0
    );
    CREATE INDEX idx_recommend_feedback_family_elderly
        ON dbo.recommendation_feedback(family_user_id, elderly_id, content_id);
END;
GO

IF OBJECT_ID('dbo.recommendation_trigger', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.recommendation_trigger (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        elderly_id BIGINT NOT NULL,
        signal_type NVARCHAR(64) NOT NULL,
        reference_id BIGINT NULL,
        status NVARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW',
        trigger_time DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        reviewer_id BIGINT NULL,
        reviewed_at DATETIME2 NULL,
        decision_reason NVARCHAR(300) NULL,
        delivered_at DATETIME2 NULL,
        create_time DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        update_time DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        deleted INT NOT NULL DEFAULT 0
    );
    CREATE INDEX idx_recommend_trigger_status_elderly
        ON dbo.recommendation_trigger(status, elderly_id, trigger_time DESC);
    CREATE UNIQUE INDEX uk_recommend_trigger_reference
        ON dbo.recommendation_trigger(elderly_id, signal_type, reference_id)
        WHERE reference_id IS NOT NULL AND deleted = 0;
END;
GO

/* Existing installations: evolve the review queue without requiring a destructive rebuild. */
IF COL_LENGTH('dbo.recommendation_trigger', 'reviewer_id') IS NULL
    ALTER TABLE dbo.recommendation_trigger ADD reviewer_id BIGINT NULL;
GO
IF COL_LENGTH('dbo.recommendation_trigger', 'reviewed_at') IS NULL
    ALTER TABLE dbo.recommendation_trigger ADD reviewed_at DATETIME2 NULL;
GO
IF COL_LENGTH('dbo.recommendation_trigger', 'decision_reason') IS NULL
    ALTER TABLE dbo.recommendation_trigger ADD decision_reason NVARCHAR(300) NULL;
GO
IF COL_LENGTH('dbo.recommendation_trigger', 'delivered_at') IS NULL
    ALTER TABLE dbo.recommendation_trigger ADD delivered_at DATETIME2 NULL;
GO

/*
 * Transactional Outbox: commits with the business write and is drained asynchronously.
 * The payload contains only event identifiers, never health values or alert text.
 */
IF OBJECT_ID('dbo.care_signal_outbox', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.care_signal_outbox (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        event_key NVARCHAR(180) NOT NULL,
        elderly_id BIGINT NOT NULL,
        signal_type NVARCHAR(64) NOT NULL,
        reference_id BIGINT NULL,
        occurred_at DATETIME2 NOT NULL,
        status NVARCHAR(32) NOT NULL DEFAULT 'PENDING',
        retry_count INT NOT NULL DEFAULT 0,
        next_retry_at DATETIME2 NULL,
        last_error_type NVARCHAR(80) NULL,
        processed_at DATETIME2 NULL,
        create_time DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        update_time DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        deleted INT NOT NULL DEFAULT 0
    );
    CREATE UNIQUE INDEX uk_care_signal_outbox_event_key
        ON dbo.care_signal_outbox(event_key)
        WHERE deleted = 0;
    CREATE INDEX idx_care_signal_outbox_delivery
        ON dbo.care_signal_outbox(status, next_retry_at, create_time, id);
END;
GO

IF NOT EXISTS (SELECT 1 FROM dbo.recommendation_content WHERE code = 'HEALTH_CHECK_REMINDER')
    INSERT INTO dbo.recommendation_content
        (code, title, summary, category, base_score, action_label, action_url, enabled)
    VALUES
        ('HEALTH_CHECK_REMINDER', N'健康测量提醒', N'保持规律记录，有助于医护人员了解近期变化。', 'HEALTH_CHECK', 65, N'查看健康记录', 'family-health.html', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.recommendation_content WHERE code = 'SAFETY_ALERT_FOLLOWUP')
    INSERT INTO dbo.recommendation_content
        (code, title, summary, category, base_score, action_label, action_url, enabled)
    VALUES
        ('SAFETY_ALERT_FOLLOWUP', N'关注近期安全提醒', N'及时查看并跟进尚未闭环的提醒，必要时联系负责医护。', 'SAFETY', 62, N'联系医护', 'family-chat.html', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.recommendation_content WHERE code = 'CARE_SERVICE_SCHEDULE')
    INSERT INTO dbo.recommendation_content
        (code, title, summary, category, base_score, action_label, action_url, enabled)
    VALUES
        ('CARE_SERVICE_SCHEDULE', N'看看近期生活服务安排', N'了解待执行或进行中的生活服务，方便家属掌握近期照护节奏。', 'CARE_SERVICE', 58, N'查看服务', 'family-services.html', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.recommendation_content WHERE code = 'WELLNESS_ROUTINE')
    INSERT INTO dbo.recommendation_content
        (code, title, summary, category, base_score, action_label, action_url, enabled)
    VALUES
        ('WELLNESS_ROUTINE', N'保持轻量活动与规律作息', N'结合个人状态安排适量活动和规律作息；如有不适请联系医护人员。', 'WELLNESS', 52, N'查看健康信息', 'family-health.html', 1);

IF NOT EXISTS (SELECT 1 FROM dbo.recommendation_content WHERE code = 'FAMILY_SUPPORT')
    INSERT INTO dbo.recommendation_content
        (code, title, summary, category, base_score, action_label, action_url, enabled)
    VALUES
        ('FAMILY_SUPPORT', N'和家人保持联系', N'一条问候、一段语音，也可以成为日常陪伴的一部分。', 'FAMILY_SUPPORT', 48, N'去聊天', 'family-chat.html', 1);
GO
