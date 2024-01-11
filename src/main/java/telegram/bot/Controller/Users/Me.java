package telegram.bot.Controller.Users;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import telegram.bot.Configuration.JWTAuthorization.Authorization;
import telegram.bot.Configuration.TokenBlackList;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Pattern;

import static telegram.bot.Controller.Admins.Login.*;

@RestController
@CrossOrigin(maxAge = 3600)
@RequestMapping("/api/users/Me")
public class Me {

    private final JdbcTemplate jdbcTemplate;
    private final TokenBlackList tokenBlackList;

    @Autowired
    public Me(JdbcTemplate jdbcTemplate, TokenBlackList tokenBlackList) {

        this.jdbcTemplate = jdbcTemplate;
        this.tokenBlackList = tokenBlackList;
    }

    @Authorization(requiredRoles = {"ROLE_USER", "ROLE_ADMIN", "ROLE_WORKLY"})
    @GetMapping
    public ResponseEntity<?> me() {

        String sql = "SELECT * FROM " + SCHEME_NAME + ".company_working_times;";
        String sql1 = "SELECT * FROM " + SCHEME_NAME + ".employees WHERE username = ?;";

        Map<String, Object> result1 = jdbcTemplate.queryForMap(sql);
        Map<String, Object> result2 = jdbcTemplate.queryForMap(sql1, USERNAME);

        Map<String, Object> combinedResult = new LinkedHashMap<>();

        combinedResult.putAll(result1);
        combinedResult.putAll(result2);

        return ResponseEntity.ok(combinedResult);
    }

    @Authorization(requiredRoles = {"ROLE_USER", "ROLE_ADMIN"})
    @PostMapping("/completeTask")
    public ResponseEntity<?> completeTask(@RequestParam int id) {

        LocalDateTime now = LocalDateTime.now();

        try {

            int rowsAffected = jdbcTemplate.update("UPDATE " + SCHEME_NAME + ".tasks SET completed = true, done = ? WHERE id = ? AND responsible_name = ?",
                    now, id, USERNAME);

            if (rowsAffected == 0) {

                return new ResponseEntity<>("Task Not Found", HttpStatus.NOT_FOUND);
            }
        }
        catch (Exception e) {

            return new ResponseEntity<>("Task Not Found " + e, HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.ok("Task Completed Successfully");
    }

    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@(.+)$";
    private final Pattern pattern = Pattern.compile(EMAIL_PATTERN);

    @Authorization(requiredRoles = {"ROLE_ADMIN", "ROLE_USER"})
    @PostMapping("/data")
    public ResponseEntity<?> fillMyData(@RequestParam(required = false) String lastname,
                                        @RequestParam(required = false) String firstname,
                                        @RequestParam(required = false) String middlename,
                                        @RequestParam(required = false) String mail,
                                        @RequestParam(value = "image", required = false) MultipartFile imageFile) {

        if (mail != null && !pattern.matcher(mail).matches()) {
            return new ResponseEntity<>("Invalid email format", HttpStatus.BAD_REQUEST);
        }

        try {
            File file2 = null;

            if (imageFile != null && !imageFile.isEmpty()) {

                //file2 = new File("C:/Users/User/Downloads/" + imageFile.getOriginalFilename());
                file2 = new File("/var/www/html/images/" + imageFile.getOriginalFilename());

                try {

                    imageFile.transferTo(file2);
                } catch (IOException e) {

                    return new ResponseEntity<>("Error While Saving Image, Exception: " + e, HttpStatus.BAD_REQUEST);
                }
            }

            StringBuilder sqlBuilder = new StringBuilder("UPDATE " + SCHEME_NAME + ".employees SET");
            List<Object> params = new ArrayList<>();

            if (lastname != null) {
                sqlBuilder.append(" lastname = ?,");
                params.add(lastname);
            }
            if (firstname != null) {
                sqlBuilder.append(" firstname = ?,");
                params.add(firstname);
            }
            if (middlename != null) {
                sqlBuilder.append(" middlename = ?,");
                params.add(middlename);
            }
            if (mail != null) {
                sqlBuilder.append(" mail = ?,");
                params.add(mail);
            }
            if (file2 != null) {
                sqlBuilder.append(" image_link = ?,");
                params.add(file2.getAbsolutePath());
            }

            // Remove the trailing comma
            sqlBuilder.deleteCharAt(sqlBuilder.length() - 1);

            sqlBuilder.append(" WHERE username = ?");
            params.add(USERNAME);

            String sql = sqlBuilder.toString();
            jdbcTemplate.update(sql, params.toArray());

            return new ResponseEntity<>("Successfully Updated Users Data!", HttpStatus.OK);
        } catch (DataIntegrityViolationException e) {
            return new ResponseEntity<>("Email already exists", HttpStatus.CONFLICT);
        }
    }

    @Authorization(requiredRoles = {"ROLE_ADMIN", "ROLE_USER"})
    @PostMapping("/deleteAccount")
    public ResponseEntity<?> deleteMyAccount(HttpServletRequest request) {

        String sql = "UPDATE " + SCHEME_NAME + ".employees SET deleted = true WHERE username = ?;";
        jdbcTemplate.update(sql, USERNAME);

        String token = extractTokenFromRequest(request);
        tokenBlackList.add(token);

        return new ResponseEntity<>("You Successfully Deleted Your Account!", HttpStatus.OK);
    }

    /*@Authorization(requiredRoles = {"ROLE_ADMIN", "ROLE_USER"})
    @GetMapping("/companyInfo")
    public ResponseEntity<?> companyInfo() {

        String sql = "SELECT arrival_time, exit_time FROM " + SCHEME_NAME + ".company_working_times";

        return ResponseEntity.ok(jdbcTemplate.queryForMap(sql));
    }*/

    public static String extractTokenFromRequest(HttpServletRequest request) {

        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {

            return bearerToken.substring(7);
        }

        return null;
    }
}