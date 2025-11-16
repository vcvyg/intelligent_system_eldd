package org.example.persion.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.example.persion.common.utils.CodeUtil;
import org.example.persion.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 邮件服务实现类
 */
@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${spring.mail.username}")
    private String from;

    // Redis中验证码的key前缀
    private static final String CODE_PREFIX = "email:code:";
    // 验证码有效期(5分钟)
    private static final long CODE_EXPIRE_TIME = 5;

    @Override
    public String sendVerificationCode(String email) {
        // 生成6位数字验证码
        String code = CodeUtil.generateCode();

        // 先存储到Redis,有效期5分钟
        String key = CODE_PREFIX + email;
        redisTemplate.opsForValue().set(key, code, CODE_EXPIRE_TIME, TimeUnit.MINUTES);

        // 异步发送邮件,不阻塞主线程
        sendEmailAsync(email, code);

        log.info("验证码已生成并存储, 邮箱: {}, 验证码: {}", email, code);
        return code;
    }

    /**
     * 异步发送邮件
     */
    @Async
    public void sendEmailAsync(String email, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from);
            helper.setTo(email);
            helper.setSubject("智慧养老系统 - 注册验证码");

            // HTML邮件内容
            String content = buildEmailContent(code);
            helper.setText(content, true);

            mailSender.send(message);

            log.info("验证码邮件发送成功, 邮箱: {}, 验证码: {}", email, code);

        } catch (MessagingException e) {
            log.error("验证码邮件发送失败, 邮箱: {}, 验证码: {}", email, code, e);
            // 邮件发送失败不影响验证码的使用,用户可以稍后再试
        }
    }

    @Override
    public boolean verifyCode(String email, String code) {
        String key = CODE_PREFIX + email;
        String savedCode = redisTemplate.opsForValue().get(key);

        if (savedCode == null) {
            log.warn("验证码已过期或不存在, 邮箱: {}", email);
            return false;
        }

        boolean isValid = savedCode.equals(code);

        if (isValid) {
            // 验证成功后删除验证码(防止重复使用)
            redisTemplate.delete(key);
            log.info("验证码验证成功, 邮箱: {}", email);
        } else {
            log.warn("验证码错误, 邮箱: {}, 输入: {}, 正确: {}", email, code, savedCode);
        }

        return isValid;
    }

    /**
     * 构建邮件HTML内容
     */
    private String buildEmailContent(String code) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; background-color: #f5f5f5; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { padding: 40px; }
                    .code-box { background-color: #f8f9fa; border: 2px dashed #667eea; border-radius: 8px; padding: 20px; text-align: center; margin: 30px 0; }
                    .code { font-size: 32px; font-weight: bold; color: #667eea; letter-spacing: 5px; }
                    .tips { color: #666; font-size: 14px; line-height: 1.6; margin-top: 20px; }
                    .footer { text-align: center; padding: 20px; color: #999; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>智慧养老系统</h1>
                        <p>Intelligent Elderly Care System</p>
                    </div>
                    <div class="content">
                        <h2>验证码</h2>
                        <p>您好!</p>
                        <p>您正在注册智慧养老系统账号,您的验证码是:</p>
                        <div class="code-box">
                            <div class="code">""" + code + """
                            </div>
                        </div>
                        <div class="tips">
                            <p>温馨提示:</p>
                            <ul>
                                <li>验证码有效期为 <strong>5分钟</strong></li>
                                <li>请勿将验证码告诉他人</li>
                                <li>如非本人操作,请忽略此邮件</li>
                            </ul>
                        </div>
                    </div>
                    <div class="footer">
                        <p>此邮件由系统自动发送,请勿回复</p>
                        <p>© 2025 智慧养老系统</p>
                    </div>
                </div>
            </body>
            </html>
            """;
    }
}
