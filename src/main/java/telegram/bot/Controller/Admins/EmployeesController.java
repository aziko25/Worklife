package telegram.bot.Controller.Admins;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import telegram.bot.Configuration.JWTAuthorization.Authorization;
import telegram.bot.Models.Employees;
import telegram.bot.Service.AdminService;
import telegram.bot.Service.Employees.EmployeesService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static telegram.bot.Controller.Admins.Login.SCHEME_NAME;
import static telegram.bot.Controller.Admins.Login.USERNAME;

@RestController
@CrossOrigin(maxAge = 3600)
@RequestMapping("/api/admins/Employees")
@RequiredArgsConstructor
public class EmployeesController {

    private final AdminService adminService;
    private final JdbcTemplate jdbcTemplate;
    private final EmployeesService employeesService;

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @GetMapping("/birthdaysIn5Days")
    public ResponseEntity<?> birthdaysIn5Days() {

        return ResponseEntity.ok(employeesService.allEmployeesBirthdaysIn5Days());
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @GetMapping("/birthdaysByTimeRange")
    public ResponseEntity<?> birthdaysByTimeRange(@RequestParam LocalDate start,
                                                  @RequestParam LocalDate end) {

        return ResponseEntity.ok(employeesService.allEmployeesBirthdaysByDateRange(start, end));
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @PutMapping("/updateBirthday/{id}")
    public ResponseEntity<?> updateEmployeesBirthday(@PathVariable int id, @RequestParam LocalDate birthdate) {

        return ResponseEntity.ok(employeesService.setEmployeesBirthday(id, birthdate));
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @PostMapping("/create")
    public ResponseEntity<?> createEmployee(@RequestParam String username,
                                            @RequestParam String password,
                                            @RequestParam String role,
                                            @RequestParam int worklyCode,
                                            @RequestParam int worklyPass,
                                            @RequestParam LocalTime arrivalTime,
                                            @RequestParam LocalTime exitTime) {

        try {

            if (username.contains(SCHEME_NAME + "_")) {

                adminService.createEmployee(SCHEME_NAME, username, password, role, worklyCode, worklyPass, arrivalTime, exitTime);

                return ResponseEntity.ok("Employee " + username + " Created");
            }
            else {

                return new ResponseEntity<>("Username Should Start With " + SCHEME_NAME + "_{username}. \nExample: " + SCHEME_NAME + "_Akiko18", HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {

            return new ResponseEntity<>("Username already exists", HttpStatus.BAD_REQUEST);
        }
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateEmployee(@PathVariable Integer id, @RequestBody Employees employee) throws SQLException {

        return ResponseEntity.ok(employeesService.updateWholeEmployeesData(id, employee));
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @GetMapping("/all")
    public ResponseEntity<?> seeEmployees() {

        return ResponseEntity.ok(adminService.seeEmployees(SCHEME_NAME));
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteEmployee(@RequestParam String username) {

        try {

            adminService.deleteEmployee(SCHEME_NAME, username);

            return ResponseEntity.ok("Employee " + username + " deleted successfully");
        }
        catch (IllegalArgumentException e) {

            return new ResponseEntity<>("Username Does Not Exist!", HttpStatus.NOT_FOUND);
        }
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @PutMapping("/giveAdmin")
    public ResponseEntity<?> giveAdmin(@RequestParam String username) {

        try {

            adminService.giveAdmin(SCHEME_NAME, username);
        }
        catch (Exception e) {

            return new ResponseEntity<>("Username Does Not Exist!", HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.ok("Employee " + username + " is now an admin");
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @PutMapping("/updateWorklyCodeAndPassword/{id}")
    public ResponseEntity<?> changeWorklyCodeAndPass(@PathVariable int id, @RequestParam(required = false) Integer worklyCode, @RequestParam(required = false) Integer worklyPass) {

        try {
            String sql = "SELECT id, username FROM " + SCHEME_NAME + ".employees WHERE id = ?;";

            Map<String, Object> map = jdbcTemplate.queryForMap(sql, id);

            String username = (String) map.get("username");
            int userId = (int) map.get("id");

            if (username != null) {

                StringBuilder updateSql = new StringBuilder("UPDATE " + SCHEME_NAME + ".employees SET");

                List<Object> params = new ArrayList<>();

                if (worklyCode != null) {
                    updateSql.append(" workly_code = ?,");
                    params.add(worklyCode);
                }

                if (worklyPass != null) {
                    updateSql.append(" workly_password = ?,");
                    params.add(worklyPass);
                }

                updateSql.deleteCharAt(updateSql.length() - 1);

                updateSql.append(" WHERE id = ?;");
                params.add(userId);

                jdbcTemplate.update(updateSql.toString(), params.toArray());
            }

            return ResponseEntity.ok("You Successfully Updated Employees Workly Code And Password!");
        }
        catch (EmptyResultDataAccessException e) {

            throw new IllegalArgumentException("User Not Found!");
        }
        catch (DataIntegrityViolationException e) {

            throw new IllegalArgumentException(worklyCode + " Workly Code Already Exists!");
        }
    }

    @Authorization(requiredRoles = {"ROLE_USER", "ROLE_ADMIN"})
    @PutMapping("/changeUsernameOrPassword")
    public ResponseEntity<?> changeUsernameOrPassword(@RequestParam String new_username,
                                                      @RequestParam String new_password) {

        try {

            if (!new_username.startsWith(SCHEME_NAME + "_")) {

                return new ResponseEntity<>("Invalid username format", HttpStatus.BAD_REQUEST);
            }

            adminService.changeUsernameAndPassword(SCHEME_NAME, USERNAME, new_username, new_password);
        }
        catch (Exception e) {

            return new ResponseEntity<>("Username Should Start With " + SCHEME_NAME + "_{username}. \nExample: " + SCHEME_NAME + "_Akiko18", HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.ok("Username and password changed successfully");
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @GetMapping("/all/deleted")
    public ResponseEntity<?> allDeletedEmployees() {

        return ResponseEntity.ok(adminService.seeDeletedEmployees(SCHEME_NAME));
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @PostMapping("/deleteAccount")
    public ResponseEntity<?> deleteMyAccount(@RequestParam String username) {

        String sql = "UPDATE " + SCHEME_NAME + ".employees SET deleted = true WHERE username = ?;";
        int rowsUpdated = jdbcTemplate.update(sql, username);

        if (rowsUpdated == 0) {

            return new ResponseEntity<>("User Not Found!", HttpStatus.BAD_REQUEST);
        }
        else {

            return new ResponseEntity<>("You Successfully Blocked " + username + " Account!", HttpStatus.OK);
        }
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @PostMapping("/recoverAccount")
    public ResponseEntity<?> recoverUsersAccount(@RequestParam String username) {

        String sql = "UPDATE " + SCHEME_NAME + ".employees SET deleted = false WHERE username = ?;";

        int rowsUpdated = jdbcTemplate.update(sql, username);

        if (rowsUpdated == 0) {

            return new ResponseEntity<>("Employee With Username " + username + " Not Found", HttpStatus.BAD_REQUEST);
        }
        else {

            return new ResponseEntity<>("Successfully Returned " + username + " Employee", HttpStatus.OK);
        }
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @PutMapping("/updateWorkingTimes")
    public ResponseEntity<?> updateEmployeesWorkingTimes(@RequestParam List<Integer> ids, @RequestParam LocalTime start, @RequestParam LocalTime end) {

        return ResponseEntity.ok(employeesService.setEmployeesWorkingTimesGraphic(ids, start, end));
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @GetMapping("/allTimeOffs/{id}")
    public ResponseEntity<?> allEmployeesTimeOffs(@PathVariable Integer id) {

        return ResponseEntity.ok(employeesService.employeesTimeOffs(id));
    }

    @Getter
    @Setter
    public static class Dates {

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private List<LocalDateTime> date;
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @DeleteMapping("/deleteTimeOffs/{id}")
    public ResponseEntity<?> deleteEmployeesTimeOffs(@PathVariable Integer id, @RequestBody Dates dates) {

        return ResponseEntity.ok(employeesService.deleteEmployeesTimeOffs(id, dates.getDate()));
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @PostMapping("/giveTimeOff")
    public ResponseEntity<?> giveEmployeesTimeOff(@RequestBody EmployeesService.TimeOff timeOff) {

        return ResponseEntity.ok(employeesService.giveEmployeeTimeOff(timeOff));
    }
}