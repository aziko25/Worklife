package telegram.bot.Controller.Admins;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import telegram.bot.Configuration.JWTAuthorization.Authorization;
import telegram.bot.Service.TimeOffService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/time-off")
@CrossOrigin(maxAge = 3600)
public class TimeOffController {

    private final TimeOffService service;

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @GetMapping("/all")
    public ResponseEntity<?> allReasons() {

        return ResponseEntity.ok(service.allReasons());
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @PostMapping("/create")
    public ResponseEntity<?> createReason(@RequestParam String name) {

        return ResponseEntity.ok(service.createReason(name));
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateReason(@PathVariable Integer id, @RequestParam String name) {

        return ResponseEntity.ok(service.updateReason(id, name));
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteReason(@PathVariable Integer id) {

        return ResponseEntity.ok(service.deleteReason(id));
    }
}