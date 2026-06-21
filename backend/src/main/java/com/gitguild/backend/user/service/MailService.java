package com.gitguild.backend.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 邮件发送封装。
 *
 * <p>密码重置验证码邮件以异步方式发送：
 * <ul>
 *   <li>SMTP 较慢时不阻塞 HTTP 响应。</li>
 *   <li>响应耗时不随“邮箱是否存在”而变化，避免被旁路探测注册邮箱。</li>
 * </ul>
 * 发送失败仅记录日志，不向调用方抛出，保持对外响应一致。
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public MailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Async
    public void sendPasswordResetCode(String to, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (fromAddress != null && !fromAddress.isBlank()) {
                message.setFrom(fromAddress);
            }
            message.setTo(to);
            message.setSubject("【Git Guild】密码重置验证码");
            message.setText("你正在重置 Git Guild 账号的密码。\n\n"
                    + "验证码：" + code + "\n"
                    + "该验证码 5 分钟内有效，请勿泄露给他人。\n\n"
                    + "如非本人操作，请忽略本邮件，你的账号仍然安全。");
            mailSender.send(message);
        } catch (Exception e) {
            // 邮件失败不影响接口语义（防邮箱枚举），仅记录便于排查 SMTP 配置。
            log.warn("发送密码重置验证码邮件失败: to={}, reason={}", to, e.getMessage());
        }
    }
}
