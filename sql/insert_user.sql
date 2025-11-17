-- 针对 SQL Server 的 sys_user 表的 INSERT 语句
-- 请将 '生成的加密密码粘贴到这里' 替换为你实际生成的密码哈希值
INSERT INTO sys_user (
    username,
    password,
    real_name,
    phone,
    email,
    role,
    status,
    create_time,
    update_time,
    deleted
)
VALUES (
           '3745',                              -- 用户名 (username)
           '$2a$10$rlKhMMomyaZDQego4LyEOOVbZZSk6McpAnyqvdYo13kjyV5Clxm8W',                 -- 【重要】粘贴你用 Java 代码生成的密码 (password)
           '刘陈张',                             -- 真实姓名 (real_name)
           '18812345678',                            -- 手机号 (phone)
           'testadmin@example.com',                  -- 邮箱 (email)
           'MEDICAL',                                  -- 角色 (role), 可选值: ADMIN, FAMILY, MEDICAL, ELDERLY
           1,                                        -- 状态 (status), 1-启用, 0-禁用
           GETDATE(),                                -- 创建时间 (create_time)
           GETDATE(),                                -- 更新时间 (update_time)
           0                                         -- 删除标志 (deleted)
       );
