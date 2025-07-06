package com.aipiccloud.utils;

import com.aipiccloud.exception.BusinessException;
import com.aipiccloud.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Async
public class EmailUtils {
    @Resource
    private JavaMailSender mailSender;
    @Value("${spring.mail.username}")
    private String from;
    private static final Logger logger = LoggerFactory.getLogger(EmailUtils.class);
    private static final String EMAIL_SUBJECT = "AiPicCloud验证码";
    private static final String EMAIL_TEXT_TEMPLATE = "您的验证码是：%s，5分钟内有效";
    public void sendVerificationCode(String toEmail, String code) {
        try{
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(toEmail);
            message.setSubject(EMAIL_SUBJECT);
            message.setText(String.format(EMAIL_TEXT_TEMPLATE, code));
            mailSender.send(message);
            logger.info("邮件发送成功，收件人：{}", toEmail);
        }catch (MailException e){
            logger.error("邮件发送失败，收件人：{}，错误原因：{}", toEmail, e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"邮件发送失败，请稍后重试");
        }
    }
}
