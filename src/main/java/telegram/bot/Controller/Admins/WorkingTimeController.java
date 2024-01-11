package telegram.bot.Controller.Admins;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import telegram.bot.Configuration.JWTAuthorization.Authorization;
import telegram.bot.Service.AdminService;
import telegram.bot.Service.Users.WorkingTimeService;
import telegram.bot.Service.WorkingTimes.WorkingTimesService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static telegram.bot.Controller.Admins.Login.*;

@RestController
@CrossOrigin(maxAge = 3600)
@RequestMapping("/api/admins/Working-Time")
@RequiredArgsConstructor
public class WorkingTimeController {

    private final AdminService adminService;
    private final WorkingTimeService workingTimeService;
    private final WorkingTimesService workingTimesService;

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @GetMapping("/employeeToday")
    public ResponseEntity<?> workingTimeToday(@RequestParam String username) {

        return ResponseEntity.ok(adminService.seeWorkingTimeToday(SCHEME_NAME, username));
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @GetMapping("/employeeWeek")
    public ResponseEntity<?> workingTimeWeek(HttpServletRequest request,
                                             @RequestParam String username) {

        return ResponseEntity.ok(adminService.seeWorkingTimeThisWeek(SCHEME_NAME, username));
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @GetMapping("/employeeMonth")
    public ResponseEntity<?> workingTimeMonth(HttpServletRequest request,
                                              @RequestParam String username) {

        return ResponseEntity.ok(adminService.seeWorkingTimeThisMonth(SCHEME_NAME, username));
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @GetMapping("/employeeAnyDay")
    public ResponseEntity<?> workingTimeAnyDay(HttpServletRequest request,
                                               @RequestParam String username,
                                               @RequestParam LocalDate date) {

        return ResponseEntity.ok(adminService.seeWorkingTimeAnyDay(SCHEME_NAME, username, date));
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @GetMapping("/employeeRange")
    public ResponseEntity <?> workingTimeRange(HttpServletRequest request,
                                               @RequestParam String username,
                                               @RequestParam LocalDate start,
                                               @RequestParam LocalDate end) {

        return ResponseEntity.ok(adminService.seeWorkingTimeBetweenDates(SCHEME_NAME, username, start, end));
    }

    @Authorization(requiredRoles = {"ROLE_USER", "ROLE_ADMIN"})
    @GetMapping("/allToday")
    public ResponseEntity<?> workingTimeAllToday(HttpServletRequest request) throws Exception {

        Map<String, Object> tokenData = parseToken(request);

        assert tokenData != null;

        String role = (String) tokenData.get("role");

        if ("ROLE_USER".equals(role)) {

            String photoUrl = "http://161.35.75.184/imaj_Test2_2023-08-21_arrival.jpg";

            Map<String, Object> result = new HashMap<>();

            result.put("data", workingTimeService.myWorkingTimeToday(SCHEME_NAME, USERNAME));
            result.put("photoUrl", photoUrl);

            return ResponseEntity.ok(result);
        }
        else {

            return ResponseEntity.ok(adminService.seeWorkingTimeOfAllEmployeesToday(SCHEME_NAME));
        }
    }

    @Authorization(requiredRoles = {"ROLE_USER", "ROLE_ADMIN"})
    @GetMapping("/allWeek")
    public ResponseEntity<?> workingTimeAllWeek(HttpServletRequest request) throws Exception {

        Map<String, Object> tokenData = parseToken(request);

        assert tokenData != null;

        String role = (String) tokenData.get("role");

        if ("ROLE_USER".equals(role)) {

            return ResponseEntity.ok(workingTimeService.myWorkingTimeWeek(SCHEME_NAME, USERNAME));
        }
        else {

            return ResponseEntity.ok(adminService.seeWorkingTimeOfAllEmployeesThisWeek(SCHEME_NAME));
        }
    }

    @Authorization(requiredRoles = {"ROLE_USER", "ROLE_ADMIN"})
    @GetMapping("/allMonth")
    public ResponseEntity<?> workingTimeAllMonth(HttpServletRequest request) throws Exception {

        Map<String, Object> tokenData = parseToken(request);

        assert tokenData != null;

        String role = (String) tokenData.get("role");

        if ("ROLE_USER".equals(role)) {

            return ResponseEntity.ok(workingTimeService.myWorkingTimeMonth(SCHEME_NAME, USERNAME));
        }
        else {

            return ResponseEntity.ok(adminService.seeWorkingTimeOfAllEmployeesThisMonth(SCHEME_NAME));
        }
    }

    @Authorization(requiredRoles = {"ROLE_USER", "ROLE_ADMIN"})
    @GetMapping("/allAnyDay")
    public ResponseEntity<?> workingTimeAllAnyDay(HttpServletRequest request,
                                                  @RequestParam LocalDate date) throws Exception {

        Map<String, Object> tokenData = parseToken(request);

        assert tokenData != null;

        String role = (String) tokenData.get("role");

        if ("ROLE_USER".equals(role)) {

            return ResponseEntity.ok(workingTimeService.myWorkingTimeAnyDay(SCHEME_NAME, USERNAME, date));
        }
        else {

            return ResponseEntity.ok(adminService.seeWorkingTimeOfAllEmployeesAnyDay(SCHEME_NAME, date));
        }
    }

    @Authorization(requiredRoles = {"ROLE_USER", "ROLE_ADMIN"})
    @GetMapping("/allRange")
    public ResponseEntity<?> workingTimeAllRange(HttpServletRequest request,
                                                 @RequestParam LocalDate start,
                                                 @RequestParam LocalDate end) throws Exception {

        Map<String, Object> tokenData = parseToken(request);

        assert tokenData != null;

        String role = (String) tokenData.get("role");

        if ("ROLE_USER".equals(role)) {

            return ResponseEntity.ok(workingTimeService.myWorkingTimeRange(SCHEME_NAME, USERNAME, start, end));
        }
        else {

            return ResponseEntity.ok(adminService.seeWorkingTimeOfAllEmployeesBetweenDates(SCHEME_NAME, start, end));
        }
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @GetMapping("/attendanceByDateRange")
    public ResponseEntity<?> allLateAtTimeGraph(@RequestParam(required = false) @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate start,
                                                @RequestParam(required = false) @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate end) {

        return ResponseEntity.ok(workingTimesService.lateAndAtTimeGraphForDateRanges(start, end));
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @GetMapping("/leaderboard")
    public ResponseEntity<?> leaderboard() {

        return ResponseEntity.ok(workingTimesService.createLeaderboard());
    }
}