package telegram.bot.Controller.Admins;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import telegram.bot.Configuration.JWTAuthorization.Authorization;
import telegram.bot.Service.Admins.CompanyWorkingTimesService;

import java.time.LocalTime;
import java.util.List;

import static telegram.bot.Controller.Admins.Login.checkAuthorizationAndReturnResult;

@RestController
@RequestMapping("/api/admins/Companies/Working-Times")
@CrossOrigin(maxAge = 3600)
public class CompanyWorkingTimesController {

    private final CompanyWorkingTimesService service;

    @Autowired
    public CompanyWorkingTimesController(CompanyWorkingTimesService service) {

        this.service = service;
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @PostMapping("/insert")
    public ResponseEntity<?> insertCompanyWorkingTimes(@RequestParam LocalTime start,
                                                       @RequestParam LocalTime end) {

        return ResponseEntity.ok(service.insertWorkingTimes(start, end));
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @GetMapping()
    public ResponseEntity<?> myCompanyWorkingTimes() {

        return ResponseEntity.ok(service.fetchCompanyWorkingTimes());
    }
}
