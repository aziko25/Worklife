package telegram.bot.Controller.Admins;

import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import telegram.bot.Configuration.JWTAuthorization.Authorization;
import telegram.bot.Service.AdminService;
import telegram.bot.Service.Users.TasksService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static telegram.bot.Controller.Admins.Login.*;

@RestController
@CrossOrigin(maxAge = 3600)
@RequestMapping("/api/admins/Tasks")
public class TasksController {

    private final AdminService adminService;
    private final TasksService tasksService;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public TasksController(AdminService adminService, TasksService tasksService, JdbcTemplate jdbcTemplate) {

        this.adminService = adminService;
        this.tasksService = tasksService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateTask(@PathVariable int id,
                                        @RequestParam(required = false) String description,
                                        @RequestParam(required = false) MultipartFile audioFile) {

        try {

            return new ResponseEntity<>(adminService.updateTask(id, description, audioFile), HttpStatus.OK);
        } catch (Exception e) {

            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN", "ROLE_USER"})
    @GetMapping("/audio")
    public ResponseEntity<?> fetchAudioById(@RequestParam int id) {

        try {

            return new ResponseEntity<>(adminService.seeAudioById(SCHEME_NAME, id), HttpStatus.OK);
        } catch (NoSuchElementException e) {

            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @PostMapping("/create")
    public ResponseEntity<?> createTask(@RequestParam(required = false) String description,
                                        @RequestParam String responsible_name,
                                        @RequestParam(required = false) MultipartFile audioFile) {

        if ((description == null || description.isEmpty()) && (audioFile == null || audioFile.isEmpty())) {

            return new ResponseEntity<>("Either a description or an audio file must be provided", HttpStatus.BAD_REQUEST);
        }

        try {

            String fcmToken = adminService.getFcmToken(SCHEME_NAME, responsible_name);

            if (fcmToken == null) {

                adminService.createTask(SCHEME_NAME, description, responsible_name, audioFile);

                return ResponseEntity.ok("Task Created Successfully, But FCM Token Not Found");
            }

            sendNotification("Task Assigned", "Task Has Been Assigned To You", fcmToken);

            adminService.createTask(SCHEME_NAME, description, responsible_name, audioFile);

            return ResponseEntity.ok("Task Assigned Successfully To " + responsible_name);
        }
        catch (IllegalArgumentException e) {

            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
        catch (IOException e) {

            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    private void sendNotification(String title, String body, String token) {

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "key=AAAAG7MjHV0:APA91bGqvFLv3u16jjtoUJD_NKv3UHcik1ogAJgGr4gvgi_d7MGYY93Pg0i0tepQKY3HZQZzgdABDjVizVHxRtcqQYTyqiKPPV5CyXVIFMHXGwyMF63HU5XWtoWJrCfjQXDZ5uv7yQIk");

        JSONObject notification = new JSONObject();

        notification.put("title", title);
        notification.put("body", body);

        JSONObject message = new JSONObject();

        message.put("notification", notification);
        message.put("to", token);

        HttpEntity<String> request = new HttpEntity<>(message.toString(), headers);

        restTemplate.postForObject("https://fcm.googleapis.com/fcm/send", request, String.class);
    }

    @Authorization(requiredRoles = {"ROLE_USER", "ROLE_ADMIN"})
    @GetMapping("/allToday")
    public ResponseEntity<?> seeAllTasksToday(HttpServletRequest request) throws Exception {

        Map<String, Object> tokenData = parseToken(request);

        assert tokenData != null;

        String role = (String) tokenData.get("role");

        if ("ROLE_USER".equals(role)) {

            return checkAuthorizationAndReturnResult(request, List.of("ROLE_USER"),
                    () -> tasksService.myTasksToday(SCHEME_NAME, USERNAME));
        }
        else {

            return checkAuthorizationAndReturnResult(request, List.of("ROLE_ADMIN"),
                    () -> adminService.seeAllTasksToday(SCHEME_NAME));
        }
    }

    @Authorization(requiredRoles = {"ROLE_USER", "ROLE_ADMIN"})
    @GetMapping("/allWeek")
    public ResponseEntity<?> seeAllTasksThisWeek(HttpServletRequest request) throws Exception {

        Map<String, Object> tokenData = parseToken(request);

        assert tokenData != null;

        String role = (String) tokenData.get("role");

        if ("ROLE_USER".equals(role)) {

            return ResponseEntity.ok(tasksService.myTasksWeek(SCHEME_NAME, USERNAME));
        }
        else if ("ROLE_ADMIN".equals(role)) {

            return ResponseEntity.ok(adminService.seeAllTasksThisWeek(SCHEME_NAME));
        }
        else {

            throw new IllegalArgumentException("You Are Nor Admin Nor User!");
        }
    }

    @Authorization(requiredRoles = {"ROLE_USER", "ROLE_ADMIN"})
    @GetMapping("/allMonth")
    public ResponseEntity<?> seeAllTasksThisMonth(HttpServletRequest request) throws Exception {

        Map<String, Object> tokenData = parseToken(request);

        assert tokenData != null;

        String role = (String) tokenData.get("role");

        if ("ROLE_USER".equals(role)) {

            return ResponseEntity.ok(tasksService.myTasksMonth(SCHEME_NAME, USERNAME));
        }
        else {

            return ResponseEntity.ok(adminService.seeAllTasksThisMonth(SCHEME_NAME));
        }
    }

    @Authorization(requiredRoles = {"ROLE_USER", "ROLE_ADMIN"})
    @GetMapping("/allAnyDay")
    public ResponseEntity<?> seeAllTasksAnyDay(HttpServletRequest request,
                                               @RequestParam LocalDate done) throws Exception {

        Map<String, Object> tokenData = parseToken(request);

        assert tokenData != null;

        String role = (String) tokenData.get("role");

        if ("ROLE_USER".equals(role)) {

            return ResponseEntity.ok(tasksService.myTasksAnyDay(SCHEME_NAME, USERNAME, done));
        }
        else {

            return ResponseEntity.ok(adminService.seeAllTasksAnyDay(SCHEME_NAME, done));
        }
    }

    @Authorization(requiredRoles = {"ROLE_USER", "ROLE_ADMIN"})
    @GetMapping("/allRange")
    public ResponseEntity<?> seeAllTasksBetweenDates(HttpServletRequest request,
                                                     @RequestParam LocalDate start,
                                                     @RequestParam LocalDate end) throws Exception {

        Map<String, Object> tokenData = parseToken(request);

        assert tokenData != null;

        String role = (String) tokenData.get("role");

        if ("ROLE_USER".equals(role)) {

            return ResponseEntity.ok(tasksService.myTasksRange(SCHEME_NAME, USERNAME, start, end));
        }
        else {

            return ResponseEntity.ok(adminService.seeAllTasksRange(SCHEME_NAME, start, end));
        }
    }

    @Authorization(requiredRoles = {"ROLE_USER", "ROLE_ADMIN"})
    @GetMapping("/allTasks")
    public ResponseEntity<?> seeAllTasks(HttpServletRequest request) throws Exception {

        Map<String, Object> tokenData = parseToken(request);

        assert tokenData != null;

        String role = (String) tokenData.get("role");

        if ("ROLE_USER".equals(role)) {

            return ResponseEntity.ok(tasksService.allMyTasks(SCHEME_NAME, USERNAME));
        }
        else {

            return ResponseEntity.ok(adminService.seeAllTasks(SCHEME_NAME));
        }
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @GetMapping("/employeeToday")
    public ResponseEntity<?> seeEmployeeTasksToday(@RequestParam String employee_name) {

        return ResponseEntity.ok(adminService.seeAllTasksOfEmployeeToday(SCHEME_NAME, employee_name));
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @GetMapping("/employeeWeek")
    public ResponseEntity<?> seeEmployeeTasksThisWeek(@RequestParam String employee_name) {

        return ResponseEntity.ok(adminService.seeAllTasksOfEmployeeThisWeek(SCHEME_NAME, employee_name));
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @GetMapping("/employeeMonth")
    public ResponseEntity<?> seeEmployeeTasksThisMonth(HttpServletRequest request,
                                                       @RequestParam String employee_name) {

        return ResponseEntity.ok(adminService.seeAllTasksOfEmployeeThisMonth(SCHEME_NAME, employee_name));
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @GetMapping("/employeeAnyDay")
    public ResponseEntity<?> seeEmployeeTasksAnyDay(@RequestParam String employee_name,
                                                    @RequestParam LocalDate done) {

        return ResponseEntity.ok(adminService.seeAllTasksOfEmployeeAnyDay(SCHEME_NAME, employee_name, done));
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @GetMapping("/employeeRange")
    public ResponseEntity<?> seeEmployeeTasksBetweenDates(@RequestParam String employee_name,
                                                          @RequestParam LocalDate start,
                                                          @RequestParam LocalDate end) {

        return ResponseEntity.ok(adminService.seeAllTasksOfEmployeeRange(SCHEME_NAME, employee_name, start, end));
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @GetMapping("/employeeUncompleted")
    public ResponseEntity<?> seeEmployeeUncompletedTasks(@RequestParam String employee_name) {

        return ResponseEntity.ok(adminService.seeAllUnCompletedTasksOfEmployee(SCHEME_NAME, employee_name));
    }

    @Authorization(requiredRoles = {"ROLE_USER", "ROLE_ADMIN"})
    @GetMapping("/allUncompleted")
    public ResponseEntity<?> seeAllUncompletedTasks(HttpServletRequest request) throws Exception {

        Map<String, Object> tokenData = parseToken(request);

        assert tokenData != null;

        String role = (String) tokenData.get("role");

        if ("ROLE_USER".equals(role)) {

            return ResponseEntity.ok(tasksService.myUncompletedTasks(SCHEME_NAME, USERNAME));
        }
        else {

            return ResponseEntity.ok(adminService.seeAllUnCompletedTasksOfAllEmployees(SCHEME_NAME));
        }
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @PutMapping("/grade/{id}")
    public ResponseEntity<?> gradeTask(@PathVariable("id") int id, @RequestParam String grade) {

        try {

            int gradeInt = Integer.parseInt(grade);

            if (gradeInt < 0 || gradeInt > 10) {

                return new ResponseEntity<>("Grade Should Be From 0-10", HttpStatus.BAD_REQUEST);
            }
            else {

                adminService.gradeTask(SCHEME_NAME, gradeInt, id);

                String sql = "SELECT responsible_name FROM " + SCHEME_NAME + ".Tasks WHERE id = ?";

                String responsibleName = jdbcTemplate.queryForObject(sql, new Object[]{id}, String.class);

                if (responsibleName != null) {

                    String fcmToken = adminService.getFcmToken(SCHEME_NAME, responsibleName);

                    if (fcmToken != null) {

                        sendNotification("Task Graded", "Your Task Has Been Graded", fcmToken);
                    }
                }

                return ResponseEntity.ok("Task Successfully Graded!");
            }
        }
        catch (NumberFormatException e) {

            return new ResponseEntity<>("Grade Should Be An Integer", HttpStatus.BAD_REQUEST);
        }
        catch (Exception e) {

            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN"})
    @PutMapping("/check")
    public ResponseEntity<?> checkTask(@RequestParam int id) {

        try {

            return new ResponseEntity<>(adminService.startCheckTask(SCHEME_NAME, id), HttpStatus.OK);
        }
        catch (Exception e) {

            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
}