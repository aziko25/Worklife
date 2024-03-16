package telegram.bot.Controller.Admins;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import telegram.bot.Configuration.JWTAuthorization.Authorization;
import telegram.bot.Service.Departments.DepartmentsService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admins/departments")
@CrossOrigin(maxAge = 3600)
public class DepartmentsController {

    private final DepartmentsService departmentsService;

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @PostMapping("/create")
    public ResponseEntity<?> createDepartment(@RequestParam String name) {

        return ResponseEntity.ok(departmentsService.createDepartment(name));
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @GetMapping("/all")
    public ResponseEntity<?> allDepartments() {

        return ResponseEntity.ok(departmentsService.allDepartments());
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateDepartment(@PathVariable Integer id, @RequestParam String name) {

        return ResponseEntity.ok(departmentsService.updateDepartment(id, name));
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteDepartment(@PathVariable Integer id) {

        return ResponseEntity.ok(departmentsService.deleteDepartment(id));
    }
}