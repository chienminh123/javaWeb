package com.example.demo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.example.demo.model.Orders;
import com.example.demo.model.Sizes;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private SpringTemplateEngine templateEngine; 

    // ĐỊA CHỈ EMAIL MẶC ĐỊNH
    private final String FROM_EMAIL = "nguyenminhchien8424@gmail.com"; 
    // ĐỊA CHỈ ADMIN NHẬN THÔNG BÁO
    private final String ADMIN_EMAIL = "nguyenminhchien8424@gmail.com"; 

    /**
     * Gửi email xác nhận đơn hàng cho KHÁCH HÀNG (User)
     */
    public void sendOrderConfirmation(Orders order) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(FROM_EMAIL);
        // Giả định User model có trường getEmail()
        helper.setTo(order.getUser().getEmail()); 
        helper.setSubject("Xác nhận Đơn hàng #" + order.getOrderId() + " thành công!");
        
        // Tính tổng tiền cho email (từ order details)
        double totalPrice = order.getOrderDetails().stream()
                                            .mapToDouble(d -> d.getPrice() * d.getQuantity())
                                            .sum();
        
        Context context = new Context();
        context.setVariable("order", order);
        context.setVariable("totalPrice", totalPrice);

        // Xử lý template HTML: Sử dụng template "email/order-confirmation.html"
        String htmlContent = templateEngine.process("email/order-confirmation", context);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
    
    /**
     * Gửi email thông báo đơn hàng mới cho ADMIN
     */
    public void sendNewOrderNotification(Orders order) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(FROM_EMAIL);
        helper.setTo(ADMIN_EMAIL);
        helper.setSubject("[QUAN TRỌNG] Đơn hàng MỚI #" + order.getOrderId() + " vừa được đặt.");
        
        String content = "Đơn hàng mới từ khách hàng " + order.getUser().getUserName() + " (" + order.getPhone() + ") cần xử lý. Mã đơn hàng: #" + order.getOrderId() + ".";
        helper.setText(content, true);

        mailSender.send(message);
    }
   /**
     * Gửi email thông báo trạng thái đơn hàng thay đổi cho KHÁCH HÀNG (User)
     */
    public void sendOrderStatusUpdate(Orders order, String newStatus) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(FROM_EMAIL);
        helper.setTo(order.getUser().getEmail()); 
        helper.setSubject("Cập nhật Trạng thái Đơn hàng #" + order.getOrderId());
        
        Context context = new Context();
        context.setVariable("order", order);
        context.setVariable("newStatus", newStatus);

        String htmlContent = templateEngine.process("email/order-status-update", context); 
        helper.setText(htmlContent, true); 

        mailSender.send(message);
    }
    /**
     * Gửi email cảnh báo cho ADMIN khi có sản phẩm tồn kho thấp
     * @param lowStockItems Danh sách các Sizes có số lượng < 5
     */
    public void sendLowStockNotification(List<Sizes> lowStockItems) throws MessagingException {
        if (lowStockItems == null || lowStockItems.isEmpty()) {
            return;
        }
        
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(FROM_EMAIL);
        helper.setTo(ADMIN_EMAIL);
        helper.setSubject("[CẢNH BÁO TỒN KHO] Có " + lowStockItems.size() + " mục sắp hết hàng!");
        
        Context context = new Context();
        context.setVariable("lowStockItems", lowStockItems);

        String htmlContent = templateEngine.process("email/low-stock-notification", context); 
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
    public void sendUserReturnNotification(Orders order) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(FROM_EMAIL);
        helper.setTo(ADMIN_EMAIL); // Gửi cho Admin
        helper.setSubject("[THÔNG BÁO] Khách hàng yêu cầu trả hàng - Đơn #" + order.getOrderId());
        
        Context context = new Context();
        context.setVariable("order", order);
        String htmlContent = templateEngine.process("email/user-return-notification", context); 
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    public void sendResetPasswordEmail(String toEmail, String resetUrl) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(FROM_EMAIL);
        helper.setTo(toEmail);
        helper.setSubject("Yêu cầu đặt lại mật khẩu - Bibo Mart");

        String content = "<p>Xin chào,</p>"
                + "<p>Bạn đã yêu cầu đặt lại mật khẩu. Vui lòng nhấp vào link bên dưới để đổi mật khẩu mới:</p>"
                + "<p><a href=\"" + resetUrl + "\">Đổi mật khẩu ngay</a></p>"
                + "<p>Link này sẽ hết hạn sau 15 phút.</p>"
                + "<br><p>Bỏ qua email này nếu bạn không yêu cầu.</p>";

        helper.setText(content, true);
        mailSender.send(message);
    }
}