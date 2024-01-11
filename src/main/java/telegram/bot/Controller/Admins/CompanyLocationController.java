package telegram.bot.Controller.Admins;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import telegram.bot.Configuration.JWTAuthorization.Authorization;
import telegram.bot.Service.Admins.CompanyLocationService;

import java.util.List;

import static telegram.bot.Controller.Admins.Login.checkAuthorizationAndReturnResult;

@RestController
@RequestMapping("/api/admins/Companies/Location")
@CrossOrigin(maxAge = 3600)
public class CompanyLocationController {

    private final CompanyLocationService service;

    @Autowired
    public CompanyLocationController(CompanyLocationService service) {

        this.service = service;
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @PostMapping("/insert")
    public ResponseEntity<?> insertCompanyLocation(@RequestParam Double longitude,
                                                   @RequestParam Double latitude) {

         return ResponseEntity.ok(service.insertLocation(longitude, latitude));
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @GetMapping()
    public ResponseEntity<?> myCompanyLocation() {

        return ResponseEntity.ok(service.fetchCompanyLocation());
    }
}
