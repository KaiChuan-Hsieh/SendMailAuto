package com.test.kai.sendmailauto;

import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

import java.net.InetAddress;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Created by kai-chuan on 7/18/16.
 */
public class GmailSender {
    final private String TAG = "GmailSender";
    private String username;
    private String password;
    private String recipient;
    private String subject;
    private String content;
    private PowerManager pm;
    private PowerManager.WakeLock wakeLock;
    private Context mContext;

    public GmailSender (Context context, String user, String pass, String recv, String sub, String con) {
        mContext = context;
        username = user;
        password = pass;
        recipient = recv;
        subject = sub;
        content = con;
    }

    public void send() {
        pm = (PowerManager) mContext.getSystemService(mContext.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        Thread thr = new Thread(sendThread);
        thr.start();
    }

    private void sendMailViaTLS() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = draftMessage(session);
            Transport.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMailViaSSL() {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");

        Session session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username,password);
            }
        });

        try {
            Message message = draftMessage(session);
            Transport.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    Message draftMessage(Session session) throws MessagingException {
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        Address toAddr = new InternetAddress(recipient);
        message.setRecipient(Message.RecipientType.TO, toAddr);
        message.setSubject(subject);
        message.setText(content);
        return message;
    }

    Runnable sendThread = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "mail send start");
            try {
                if (!wakeLock.isHeld()) wakeLock.acquire();
                Log.i(TAG, "wakelock acquired");
                sendMailViaTLS();
                if (wakeLock.isHeld()) wakeLock.release();
                Log.i(TAG, "wakelock released");
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.i(TAG, "mail send end");
        }
    };
}
