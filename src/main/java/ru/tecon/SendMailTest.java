package ru.tecon;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class SendMailTest {

    public static void main(String[] args) throws MessagingException, IOException {
        Properties prop = new Properties();
        prop.put("mail.smtp.host", "mail.nic.ru");
        prop.put("mail.smtp.port", "465");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.ssl.enable", "true");

        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("maksim@tnn.ru", "Vfrcvfrc1");
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress("maksim@tnn.ru"));
        message.setRecipients(
                Message.RecipientType.TO, InternetAddress.parse("shchelkonogov@mail.ru"));
        message.setSubject("Mail Subject");

        String msg = "This is my first email using JavaMailer";

        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(msg, "text/html");

        MimeBodyPart attachmentBodyPart = new MimeBodyPart();
        attachmentBodyPart.attachFile(new File("path/to/file"));

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);

        message.setContent(multipart);

        Transport.send(message);
    }
}
