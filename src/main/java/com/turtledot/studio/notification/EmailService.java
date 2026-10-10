package com.turtledot.studio.notification;

import com.turtledot.studio.client.Client;
import com.turtledot.studio.employee.Employee;
import com.turtledot.studio.event.Event;
import com.turtledot.studio.studio.Studio;
import com.turtledot.studio.user.User;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.portal-url:http://localhost:3000}")
    private String portalUrl;

    @Value("${spring.mail.username:}")
    private String defaultFromEmail;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Trigger welcome email to newly created employee with their login credentials.
     */
    @Async
    public void sendEmployeeWelcomeEmail(Studio studio, User employeeUser, Employee employee, String plainPassword) {
        if (employeeUser == null || employeeUser.getEmail() == null || employeeUser.getEmail().isBlank()) {
            log.warn("[EMAIL] Skipping welcome email: employee has no email address");
            return;
        }

        String studioName = (studio != null && studio.getName() != null) ? studio.getName() : "Turtledot Studio";
        String subject = "Welcome to " + studioName + " - Your Staff Account Credentials";

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'></head><body style='margin:0;padding:0;background-color:#f5f5f4;font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Roboto,sans-serif;color:#1c1917;'>");
        sb.append("<table width='100%' cellspacing='0' cellpadding='0' border='0' style='background-color:#f5f5f4;padding:30px 10px;'><tr><td align='center'>");
        sb.append("<table width='600' cellspacing='0' cellpadding='0' border='0' style='background-color:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.06);border:1px solid #e7e5e4;'>");
        sb.append("<tr><td style='background-color:#1c1917;padding:28px 36px;border-bottom:3px solid #d97706;'>");
        sb.append("<div style='color:#d97706;font-size:11px;font-weight:700;letter-spacing:2px;text-transform:uppercase;'>Staff Onboarding</div>");
        sb.append("<div style='color:#ffffff;font-size:22px;font-weight:800;margin-top:4px;'>").append(escapeHtml(studioName)).append("</div></td></tr>");
        sb.append("<tr><td style='padding:36px;'><h2 style='font-size:19px;font-weight:700;color:#1c1917;margin:0 0 12px 0;'>Welcome to the team, ").append(escapeHtml(employeeUser.getFullName())).append("!</h2>");
        sb.append("<p style='font-size:14px;line-height:1.6;color:#57534e;margin:0 0 24px 0;'>Your staff profile has been registered on the Turtledot Studio CRM platform. Below are your account credentials and system assignment details.</p>");
        sb.append("<div style='background-color:#fafaf9;border-radius:12px;padding:20px 24px;border:1px solid #e7e5e4;margin-bottom:24px;'>");
        sb.append("<div style='font-size:12px;font-weight:700;color:#d97706;text-transform:uppercase;letter-spacing:1px;margin-bottom:14px;'>Account Credentials</div>");
        sb.append("<table width='100%' cellspacing='0' cellpadding='6' border='0' style='font-size:13px;'>");
        sb.append("<tr><td width='35%' style='color:#78716c;font-weight:600;'>Staff Code:</td><td style='color:#1c1917;font-weight:700;font-family:monospace;'>").append(employee != null ? escapeHtml(employee.getEmployeeCode()) : "N/A").append("</td></tr>");
        sb.append("<tr><td style='color:#78716c;font-weight:600;'>Designation:</td><td style='color:#1c1917;font-weight:600;'>").append(employee != null ? escapeHtml(employee.getDesignation()) : "Team Member").append("</td></tr>");
        sb.append("<tr><td style='color:#78716c;font-weight:600;'>Department:</td><td style='color:#1c1917;font-weight:600;'>").append(employee != null ? escapeHtml(employee.getDepartment()) : "Production").append("</td></tr>");
        sb.append("<tr><td style='color:#78716c;font-weight:600;'>Portal Email:</td><td style='color:#1c1917;font-weight:700;'>").append(escapeHtml(employeeUser.getEmail())).append("</td></tr>");
        sb.append("<tr><td style='color:#78716c;font-weight:600;'>Temporary Password:</td><td><span style='color:#b45309;font-weight:800;font-family:monospace;font-size:14px;background:#fef3c7;padding:4px 8px;border-radius:6px;display:inline-block;'>").append(escapeHtml(plainPassword)).append("</span></td></tr>");
        sb.append("</table></div>");
        sb.append("<div style='text-align:center;margin:28px 0 20px 0;'><a href='").append(portalUrl).append("/login' style='background:linear-gradient(135deg,#d97706,#b45309);color:#ffffff;text-decoration:none;padding:13px 32px;border-radius:12px;font-size:14px;font-weight:700;display:inline-block;box-shadow:0 4px 12px rgba(217,119,6,0.3);'>Sign In to Staff Portal &rarr;</a></div>");
        sb.append("<p style='font-size:12px;line-height:1.5;color:#78716c;margin-top:24px;border-top:1px solid #f5f5f4;padding-top:16px;'><strong>Security Note:</strong> Please sign in to verify your account. For security purposes, consider changing your password in your profile settings upon your first login.</p></td></tr>");
        sb.append("<tr><td style='background-color:#fafaf9;padding:20px 36px;border-top:1px solid #e7e5e4;text-align:center;font-size:11px;color:#a8a29e;'>").append(escapeHtml(studioName)).append(" &bull; Photography Studio CRM &amp; Client Delivery Platform</td></tr>");
        sb.append("</table></td></tr></table></body></html>");

        sendHtmlEmail(employeeUser.getEmail(), getFromAddress(studio), studioName, subject, sb.toString());
    }

    /**
     * Trigger email notification to an assigned employee when an event is created/assigned.
     */
    @Async
    public void sendEventAssignmentEmailToEmployee(Studio studio, User employeeUser, Employee employee, Event event, Client client) {
        if (employeeUser == null || employeeUser.getEmail() == null || employeeUser.getEmail().isBlank()) {
            return;
        }

        String studioName = (studio != null && studio.getName() != null) ? studio.getName() : "Turtledot Studio";
        String eventDateStr = event.getEventDate() != null ? event.getEventDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")) : "Date TBD";
        String subject = "New Shoot Assignment: " + event.getEventName() + " (" + event.getEventCode() + ")";

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'></head><body style='margin:0;padding:0;background-color:#f5f5f4;font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Roboto,sans-serif;color:#1c1917;'>");
        sb.append("<table width='100%' cellspacing='0' cellpadding='0' border='0' style='background-color:#f5f5f4;padding:30px 10px;'><tr><td align='center'>");
        sb.append("<table width='600' cellspacing='0' cellpadding='0' border='0' style='background-color:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.06);border:1px solid #e7e5e4;'>");
        sb.append("<tr><td style='background-color:#1c1917;padding:28px 36px;border-bottom:3px solid #d97706;'>");
        sb.append("<div style='color:#d97706;font-size:11px;font-weight:700;letter-spacing:2px;text-transform:uppercase;'>Production Assignment</div>");
        sb.append("<div style='color:#ffffff;font-size:22px;font-weight:800;margin-top:4px;'>").append(escapeHtml(studioName)).append("</div></td></tr>");
        sb.append("<tr><td style='padding:36px;'><h2 style='font-size:19px;font-weight:700;color:#1c1917;margin:0 0 10px 0;'>Hi ").append(escapeHtml(employeeUser.getFullName())).append(",</h2>");
        sb.append("<p style='font-size:14px;line-height:1.6;color:#57534e;margin:0 0 24px 0;'>You have been assigned to cover a scheduled event. Below are the shoot specifications and client details:</p>");
        sb.append("<div style='background-color:#fafaf9;border-radius:12px;padding:22px 24px;border:1px solid #e7e5e4;margin-bottom:24px;'>");
        sb.append("<div style='font-size:16px;font-weight:800;color:#1c1917;margin-bottom:6px;'>").append(escapeHtml(event.getEventName())).append("</div>");
        sb.append("<div style='font-size:12px;font-family:monospace;color:#d97706;font-weight:700;margin-bottom:16px;'>Reference: ").append(escapeHtml(event.getEventCode())).append("</div>");
        sb.append("<table width='100%' cellspacing='0' cellpadding='6' border='0' style='font-size:13px;'>");
        sb.append("<tr><td width='32%' style='color:#78716c;font-weight:600;'>Shoot Date:</td><td style='color:#1c1917;font-weight:700;'>").append(escapeHtml(eventDateStr)).append("</td></tr>");
        sb.append("<tr><td style='color:#78716c;font-weight:600;'>Event Type:</td><td style='color:#1c1917;font-weight:600;'>").append(escapeHtml(event.getEventType() != null ? event.getEventType() : "General Shoot")).append("</td></tr>");
        sb.append("<tr><td style='color:#78716c;font-weight:600;'>Venue / Location:</td><td style='color:#1c1917;font-weight:600;'>").append(escapeHtml(event.getLocation() != null && !event.getLocation().isBlank() ? event.getLocation() : "Location to be finalized")).append("</td></tr>");
        sb.append("<tr><td style='color:#78716c;font-weight:600;'>Client Name:</td><td style='color:#1c1917;font-weight:700;'>").append(escapeHtml(client != null ? client.getName() : "Client")).append("</td></tr>");
        sb.append("<tr><td style='color:#78716c;font-weight:600;'>Client Contact:</td><td style='color:#1c1917;font-weight:600;'>").append(escapeHtml(client != null && client.getPhone() != null ? client.getPhone() : "Contact Studio Admin")).append("</td></tr>");
        sb.append("<tr><td style='color:#78716c;font-weight:600;'>Notes / Agenda:</td><td style='color:#44403c;'>").append(escapeHtml(event.getDescription() != null && !event.getDescription().isBlank() ? event.getDescription() : "Standard studio shoot guidelines apply.")).append("</td></tr>");
        sb.append("</table></div>");
        sb.append("<div style='text-align:center;margin:28px 0 20px 0;'><a href='").append(portalUrl).append("/employee/upload' style='background:linear-gradient(135deg,#d97706,#b45309);color:#ffffff;text-decoration:none;padding:13px 32px;border-radius:12px;font-size:14px;font-weight:700;display:inline-block;box-shadow:0 4px 12px rgba(217,119,6,0.3);'>Open Workspace &amp; Upload Media &rarr;</a></div>");
        sb.append("</td></tr>");
        sb.append("<tr><td style='background-color:#fafaf9;padding:20px 36px;border-top:1px solid #e7e5e4;text-align:center;font-size:11px;color:#a8a29e;'>").append(escapeHtml(studioName)).append(" &bull; Photography Studio CRM &amp; Production Management</td></tr>");
        sb.append("</table></td></tr></table></body></html>");

        sendHtmlEmail(employeeUser.getEmail(), getFromAddress(studio), studioName, subject, sb.toString());
    }

    /**
     * Trigger confirmation email to client when their event is created and assigned to the production team.
     */
    @Async
    public void sendEventBookingEmailToClient(Studio studio, Client client, Event event, List<String> assignedEmployeeNames) {
        if (client == null || client.getEmail() == null || client.getEmail().isBlank()) {
            log.info("[EMAIL] Skipping client booking email: client has no email registered");
            return;
        }

        String studioName = (studio != null && studio.getName() != null) ? studio.getName() : "Turtledot Studio";
        String eventDateStr = event.getEventDate() != null ? event.getEventDate().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")) : "Date TBD";
        String subject = "Booking Confirmed: " + event.getEventName() + " - " + studioName;

        String assignedTeamText = (assignedEmployeeNames != null && !assignedEmployeeNames.isEmpty())
                ? String.join(", ", assignedEmployeeNames)
                : "Dedicated Studio Creative Team";

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'></head><body style='margin:0;padding:0;background-color:#f5f5f4;font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Roboto,sans-serif;color:#1c1917;'>");
        sb.append("<table width='100%' cellspacing='0' cellpadding='0' border='0' style='background-color:#f5f5f4;padding:30px 10px;'><tr><td align='center'>");
        sb.append("<table width='600' cellspacing='0' cellpadding='0' border='0' style='background-color:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.06);border:1px solid #e7e5e4;'>");
        sb.append("<tr><td style='background-color:#1c1917;padding:28px 36px;border-bottom:3px solid #d97706;'>");
        sb.append("<div style='color:#d97706;font-size:11px;font-weight:700;letter-spacing:2px;text-transform:uppercase;'>Booking Confirmation</div>");
        sb.append("<div style='color:#ffffff;font-size:22px;font-weight:800;margin-top:4px;'>").append(escapeHtml(studioName)).append("</div></td></tr>");
        sb.append("<tr><td style='padding:36px;'><h2 style='font-size:19px;font-weight:700;color:#1c1917;margin:0 0 10px 0;'>Dear ").append(escapeHtml(client.getName())).append(",</h2>");
        sb.append("<p style='font-size:14px;line-height:1.6;color:#57534e;margin:0 0 24px 0;'>Thank you for partnering with <strong>").append(escapeHtml(studioName)).append("</strong>! We are delighted to confirm that your photography session has been successfully booked and assigned to our professional creative team.</p>");
        sb.append("<div style='background-color:#fafaf9;border-radius:12px;padding:22px 24px;border:1px solid #e7e5e4;margin-bottom:24px;'>");
        sb.append("<div style='font-size:16px;font-weight:800;color:#1c1917;margin-bottom:6px;'>").append(escapeHtml(event.getEventName())).append("</div>");
        sb.append("<div style='font-size:12px;font-family:monospace;color:#d97706;font-weight:700;margin-bottom:16px;'>Booking Reference: ").append(escapeHtml(event.getEventCode())).append("</div>");
        sb.append("<table width='100%' cellspacing='0' cellpadding='6' border='0' style='font-size:13px;'>");
        sb.append("<tr><td width='35%' style='color:#78716c;font-weight:600;'>Event Date:</td><td style='color:#1c1917;font-weight:700;'>").append(escapeHtml(eventDateStr)).append("</td></tr>");
        sb.append("<tr><td style='color:#78716c;font-weight:600;'>Category:</td><td style='color:#1c1917;font-weight:600;'>").append(escapeHtml(event.getEventType() != null ? event.getEventType() : "Photo Shoot")).append("</td></tr>");
        sb.append("<tr><td style='color:#78716c;font-weight:600;'>Location:</td><td style='color:#1c1917;font-weight:600;'>").append(escapeHtml(event.getLocation() != null && !event.getLocation().isBlank() ? event.getLocation() : "Venue specified on contract")).append("</td></tr>");
        sb.append("<tr><td style='color:#78716c;font-weight:600;'>Assigned Photographers:</td><td style='color:#b45309;font-weight:700;'>").append(escapeHtml(assignedTeamText)).append("</td></tr>");
        sb.append("<tr><td style='color:#78716c;font-weight:600;'>Notes:</td><td style='color:#44403c;'>").append(escapeHtml(event.getDescription() != null && !event.getDescription().isBlank() ? event.getDescription() : "Client portrait / event coverage")).append("</td></tr>");
        sb.append("</table></div>");
        sb.append("<p style='font-size:13px;line-height:1.6;color:#57534e;margin:0 0 20px 0;'>Our crew will connect with you prior to the date for timing synchronizations, shot lists, and moodboard alignment. Once your session concludes, your high-resolution proofs and private QR gallery will be delivered directly to your account.</p>");
        sb.append("<div style='border-top:1px solid #e7e5e4;padding-top:18px;margin-top:24px;font-size:12px;color:#78716c;'>");
        sb.append("<div><strong>Studio Support:</strong> ").append(escapeHtml(studioName)).append("</div>");
        sb.append("<div><strong>Phone:</strong> ").append(escapeHtml(studio != null && studio.getPhone() != null ? studio.getPhone() : "Support Line")).append(" &bull; <strong>Email:</strong> ").append(escapeHtml(studio != null && studio.getEmail() != null ? studio.getEmail() : "studio@turtledot.com")).append("</div>");
        sb.append("</div></td></tr>");
        sb.append("<tr><td style='background-color:#fafaf9;padding:20px 36px;border-top:1px solid #e7e5e4;text-align:center;font-size:11px;color:#a8a29e;'>").append(escapeHtml(studioName)).append(" &bull; Luxury Photography &amp; Client Delivery Platform</td></tr>");
        sb.append("</table></td></tr></table></body></html>");

        sendHtmlEmail(client.getEmail(), getFromAddress(studio), studioName, subject, sb.toString());
    }

    private String getFromAddress(Studio studio) {
        if (studio != null && studio.getEmail() != null && !studio.getEmail().isBlank()) {
            return studio.getEmail();
        }
        if (defaultFromEmail != null && !defaultFromEmail.isBlank()) {
            return defaultFromEmail;
        }
        return "noreply@turtledotstudio.com";
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    private void sendHtmlEmail(String toEmail, String fromEmail, String fromName, String subject, String htmlContent) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("[EMAIL] Target address is empty. Aborting email send.");
            return;
        }

        try {
            if (mailSender != null && defaultFromEmail != null && !defaultFromEmail.isBlank()) {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setTo(toEmail.trim());
                helper.setFrom(new InternetAddress(fromEmail, fromName != null ? fromName : "Turtledot Studio"));
                helper.setSubject(subject);
                helper.setText(htmlContent, true);
                mailSender.send(message);
                log.info("[EMAIL SUCCESS] Live email sent successfully to '{}' | Subject: '{}'", toEmail, subject);
                return;
            }
        } catch (Exception ex) {
            log.warn("[EMAIL WARNING] Live SMTP delivery encountered an issue: {}. Falling back to simulation log.", ex.getMessage());
        }

        // Graceful logging for development/simulation when SMTP is not configured
        log.info("\n================================================================================\n" +
                 "[EMAIL DISPATCH SIMULATED & LOGGED]\n" +
                 "TO: " + toEmail + "\n" +
                 "FROM: " + fromEmail + " (" + fromName + ")\n" +
                 "SUBJECT: " + subject + "\n" +
                 "STATUS: Ready / Dispatched\n" +
                 "================================================================================\n");
    }
}
