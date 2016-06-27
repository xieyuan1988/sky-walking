package com.ai.cloud.skywalking.alarm.util;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ai.cloud.skywalking.alarm.conf.Config;
import com.ai.cloud.skywalking.alarm.dao.SystemConfigDao;
import com.google.gson.Gson;

public class MailUtil {

    private static Logger logger = LogManager.getLogger(MailUtil.class);

    private static String mailSender;
    private static Properties config;

    static {
        try {
            String senderInfo = SystemConfigDao.getSystemConfig(Config.MailSenderInfo.configId);
            config = new Gson().fromJson(senderInfo, Properties.class);
            mailSender = config.getProperty("mail.sender");
        } catch (Exception e) {
            logger.error("Failed to load mail sender info.", e);
            System.exit(-1);
        }
    }


    public static void sendMail(String[] recipientAccounts, String[] ccList, String content, String title) {
        Session session = Session.getInstance(config);
        Transport ts = null;
        try {
            ts = session.getTransport();
            ts.connect(config.getProperty("mail.host"), config.getProperty("mail.username"), config.getProperty("mail.password"));
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(mailSender));
            InternetAddress[] recipientAccountArray = new InternetAddress[recipientAccounts.length];
            for (int i = 0; i < recipientAccounts.length; i++) {
                recipientAccountArray[i] = new InternetAddress(recipientAccounts[i]);
            }
            message.addRecipients(Message.RecipientType.TO, recipientAccountArray);
            if (ccList != null && ccList.length > 0) {
                InternetAddress[] ccAccountArray = new InternetAddress[ccList.length];
                for (int i = 0; i < ccList.length; i++) {
                    ccAccountArray[i] = new InternetAddress(ccList[i]);
                }
                message.addRecipients(Message.RecipientType.CC, ccAccountArray);
            }
            message.setSubject(title);
            message.setContent(content, "text/html;charset=UTF-8");

            ts.sendMessage(message, message.getAllRecipients());

        } catch (AddressException e) {
            logger.error("Recipient Account is not correct.", e);
        } catch (NoSuchProviderException e) {
            logger.error("Failed to send mail.", e);
        } catch (MessagingException e) {
            logger.error("Failed to send mail.", e);
        } finally {
            if (ts != null) {
                try {
                    ts.close();
                } catch (MessagingException e) {
                    logger.error("Failed to close transport.", e);
                }
            }
        }
    }


}
