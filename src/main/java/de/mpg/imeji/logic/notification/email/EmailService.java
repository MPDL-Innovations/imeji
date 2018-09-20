package de.mpg.imeji.logic.notification.email;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.logging.log4j.Logger; 
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;

/**
 * Client to send email
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class EmailService {
  private static final Logger LOGGER = LogManager.getLogger(EmailService.class);

  /**
   * Is true if the Email is valid
   */
  public static boolean isValidEmail(String email) {
    final String regexEmailMatch = "([^.@]+)(\\.[^.@]+)*@([^.@]+\\.)+([^.@]+)";
    return email.matches(regexEmailMatch);
  }

  /**
   * Send an email according to the properties define in imeji.properties
   *
   * @throws ImejiException
   */
  public void sendMail(String to, String from, Email email)
      throws IOException, URISyntaxException, ImejiException {
    sendMail(to, from, null, email.getSubject(), email.getBody());
  }

  /**
   * Send an email according to the properties define in imeji.properties
   *
   * @throws ImejiException
   */
  public void sendMail(String to, String from, String subject, String message)
      throws ImejiException {
    try {
      // Just to not generate an error while testing
      if (Imeji.CONFIG.getEmailServer() != null && !Imeji.CONFIG.getEmailServer().equals("")) {
        sendMail(to, from, null, subject, message);
      } else {
        System.out.println("Email to " + to + "\nSubject: " + subject + "\nMessage: " + message);
      }
    } catch (IOException | URISyntaxException e) {
      throw new ImejiException("Error sending email");
    }
  }

  /**
   * Send an email according to the properties define in imeji.properties
   *
   * @throws ImejiException
   */
  private void sendMail(String to, String from, String[] replyTo, String subject, String message)
      throws IOException, URISyntaxException, ImejiException {
    final String emailUser = Imeji.CONFIG.getEmailServerUser();
    final String password = Imeji.CONFIG.getEmailServerPassword();
    final String server = Imeji.CONFIG.getEmailServer();
    String port = Imeji.CONFIG.getEmailServerPort();
    if (isNullOrEmpty(port)) {
      port = "25";
    }
    final String auth = Boolean.toString(Imeji.CONFIG.getEmailServerEnableAuthentication());
    String sender = Imeji.CONFIG.getEmailServerSender();
    if (from != null) {
      sender = from;
    }
    final String[] recipientsAdress = {to};
    sendMail(server, port, auth, emailUser, password, sender, recipientsAdress, null, null, replyTo,
        subject, message);
  }

  /**
   * Send an email according to the properties define in imeji.properties
   */

  /**
   * Send an email according to the properties define in imeji.properties
   */

  /**
   * Send an email
   *
   * @throws ImejiException
   */
  public String sendMail(String smtpHost, String port, String withAuth, String usr, String pwd,
      String senderAddress, String[] recipientsAddresses, String[] recipientsCCAddresses,
      String[] recipientsBCCAddresses, String[] replytoAddresses, String subject, String text)
      throws ImejiException {
    LOGGER.debug("EmailHandlingBean sendMail...");
    String status = "not sent";
    String to = "";
    try {
      // Setup mail server
      final Properties props = System.getProperties();
      props.put("mail.smtp.host", smtpHost);
      props.put("mail.smtp.auth", withAuth);
      props.put("mail.smtp.starttls.enable", "true");
      props.put("mail.smtp.port", port);
      // Get a mail session with authentication
      final MailAuthenticator authenticator = new MailAuthenticator(usr, pwd);
      final Session mailSession = Session.getInstance(props, authenticator);
      // Define a new mail message
      final Message message = new MimeMessage(mailSession);
      message.setFrom(new InternetAddress(senderAddress));
      // add TO recipients
      for (final String ra : recipientsAddresses) {
        if (ra != null && !ra.trim().equals("")) {
          message.addRecipient(Message.RecipientType.TO, new InternetAddress(ra));
          to = ra;
          LOGGER.debug(">>> recipientTO: " + ra);
        }
      }
      // add CC recipients
      if (recipientsCCAddresses != null) {
        for (final String racc : recipientsCCAddresses) {
          if (racc != null && !racc.trim().equals("")) {
            message.addRecipient(Message.RecipientType.CC, new InternetAddress(racc));
            LOGGER.debug(">>> recipientCC  " + racc);
          }
        }
      }
      // add BCC recipients
      if (recipientsBCCAddresses != null) {
        for (final String rabcc : recipientsBCCAddresses) {
          if (rabcc != null && !rabcc.trim().equals("")) {
            message.addRecipient(Message.RecipientType.BCC, new InternetAddress(rabcc));
            LOGGER.debug(">>> recipientBCC  " + rabcc);
          }
        }
      }
      // add replyTo
      if (replytoAddresses != null) {
        final InternetAddress[] adresses = new InternetAddress[recipientsAddresses.length];
        int i = 0;
        for (final String a : replytoAddresses) {
          if (a != null && !a.trim().equals("")) {
            adresses[i] = new InternetAddress(a);
            i++;
            LOGGER.debug(">>> replyToaddress  " + a);
          }
        }
        if (i > 0) {
          message.setReplyTo(adresses);
        }
      }
      message.setSubject(subject);
      final Date date = new Date();
      message.setSentDate(date);
      // Create a message part to represent the body text
      final BodyPart messageBodyPart = new MimeBodyPart();
      messageBodyPart.setText(text);
      // use a MimeMultipart as we need to handle the file attachments
      final Multipart multipart = new MimeMultipart();
      // add the message body to the mime message
      multipart.addBodyPart(messageBodyPart);
      // Put all message parts in the message
      message.setContent(multipart);
      LOGGER.debug("Transport will send now....  ");
      // Send the message
      Transport.send(message);
      status = "sent";
      LOGGER.debug("Email sent!");
    } catch (final MessagingException e) {
      LOGGER.error("Error in sendMail(...)", e);
      throw new ImejiException("Error sending Email", e);
    }
    return status;
  }

  /**
   * {@link Authenticator} for imeji
   *
   * @author saquet (initial creation)
   * @author $Author$ (last modification)
   * @version $Revision$ $LastChangedDate$
   */
  public class MailAuthenticator extends Authenticator {
    private final String user;
    private final String password;

    /**
     * Public constructor.
     */
    public MailAuthenticator(String usr, String pwd) {
      this.user = usr;
      this.password = pwd;
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
      final PasswordAuthentication pwdAut = new PasswordAuthentication(this.user, this.password);
      return pwdAut;
    }
  }


}
