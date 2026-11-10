package ro.frit.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// GET /notifications — newest first; ?customerEmail= shows only one customer's (SkillForge's Bookings page).
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<Notification> notifications(@RequestParam(required = false) String customerEmail) {
        return notificationService.findAll(customerEmail);
    }
}
