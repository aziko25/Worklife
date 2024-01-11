package telegram.bot.Controller.Admins;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.web.bind.annotation.*;
import telegram.bot.Configuration.Mail.EmailService;
import telegram.bot.Configuration.TokenBlackList;
import telegram.bot.Models.Employees;
import telegram.bot.Models.RowMappers.EmployeeRowMapper;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.mail.javamail.JavaMailSender;

@RestController
@CrossOrigin(maxAge = 3600)
@RequestMapping("/api")
public class Login {

    private final JdbcTemplate jdbcTemplate;
    private static TokenBlackList tokenBlackList;

    private final EmailService emailService;

    public static String secretKeyString;

    @Value("${jwt.secretKey}")
    public void setSecretKeyString(String secretKeyString) {

        Login.secretKeyString = secretKeyString;
    }

    public static SecretKey getSecretKey() {

        byte[] secretBytes = secretKeyString.getBytes(StandardCharsets.UTF_8);

        return Keys.hmacShaKeyFor(secretBytes);
    }

    public static String SCHEME_NAME;
    public static String USERNAME;
    public static String ROLE;

    public static HashMap<String, String> mapOfMailAndCodes = new HashMap<>();

    @Autowired
    public Login(JdbcTemplate jdbcTemplate, TokenBlackList tokenBlackList, EmailService emailService) {

        this.jdbcTemplate = jdbcTemplate;
        Login.tokenBlackList = tokenBlackList;
        this.emailService = emailService;
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<?> resetPassword(@RequestParam String username) {

        String prefix;
        int index = username.indexOf('_');

        if (index != -1) {

            prefix = username.substring(0, index);

            System.out.println(prefix + "\n" + username);

            String findUsersMail = "SELECT mail FROM " + prefix + ".employees WHERE username = ?;";
            String usersMail = jdbcTemplate.queryForObject(findUsersMail, String.class, username);

            if (usersMail == null) {

                return new ResponseEntity<>("Mail Not Found For This User!", HttpStatus.BAD_REQUEST);
            }

            System.out.println(usersMail);

            String code = generateUniqueCode();

            emailService.sendCodeToEmail(usersMail, code);

            mapOfMailAndCodes.put(usersMail, code);

            return ResponseEntity.ok(mapOfMailAndCodes);
        }
        else {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid username format");
        }
    }

    @PostMapping("/verifyCode")
    public ResponseEntity<?> verifyCodeAndResetPassword(@RequestParam String username,
                                                        @RequestParam String code, @RequestParam String newPassword) {

        String prefix;
        int index = username.indexOf('_');

        if (index != -1) {

            prefix = username.substring(0, index);

            String findUsersMail = "SELECT mail FROM " + prefix + ".employees WHERE username = ?;";
            String usersMail = jdbcTemplate.queryForObject(findUsersMail, String.class, username);

            String correctCode = mapOfMailAndCodes.get(usersMail);

            if (correctCode != null && correctCode.equals(code)) {

                String resetPasswordSql = "UPDATE " + prefix + ".employees SET password = ? WHERE username = ?;";
                jdbcTemplate.update(resetPasswordSql, newPassword, username);

                return ResponseEntity.ok("Password has been reset successfully.");
            }
            else {

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid code.");
            }
        }
        else {

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid username format");
        }
    }

    private String generateUniqueCode() {

        String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        StringBuilder builder = new StringBuilder();
        Random rnd = new Random();

        while (builder.length() < 5) {

            int index = (int) (rnd.nextFloat() * ALPHA_NUMERIC_STRING.length());

            builder.append(ALPHA_NUMERIC_STRING.charAt(index));
        }

        return builder.toString();
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(HttpServletRequest request) {

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        String username = null;
        String password = null;

        String prefix;

        if (authorizationHeader != null && authorizationHeader.toLowerCase().startsWith("basic")) {

            String base64Credentials = authorizationHeader.substring("Basic".length()).trim();
            byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);

            String credentials = new String(credDecoded, StandardCharsets.UTF_8);
            final String[] values = credentials.split(":", 2);

            username = values[0];
            password = values[1];
        }

        assert username != null;

        int index = username.indexOf('_');

        if (index != -1) {

            prefix = username.substring(0, index);

            String sql = "SELECT * FROM " + prefix + ".employees WHERE username = ? AND deleted = false;";
            List<Employees> employees = jdbcTemplate.query(sql, new Object[]{username}, new EmployeeRowMapper());

            if (!employees.isEmpty() && employees.get(0).getPassword().equals(password)) {

                Claims claims = Jwts.claims();

                claims.put("schemeName", prefix);
                claims.put("username", username);
                claims.put("role", employees.get(0).getRole());

                System.out.println(employees.get(0).getUsername() + " " + employees.get(0).getPassword() + " " +
                        prefix + " " + employees.get(0).getRole());

                // Set expiration time to 1 week from now
                Date expiration = new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000);

                return ResponseEntity.ok(Jwts.builder()
                        .setClaims(claims)
                        .setExpiration(expiration)
                        .signWith(getSecretKey())
                        .compact());
            }
            else {

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid username or password");
            }
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid username or password");
    }

    public static Map<String, Object> parseToken(HttpServletRequest request) throws Exception {

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader != null && authorizationHeader.toLowerCase().startsWith("bearer")) {

            String token = authorizationHeader.substring("Bearer".length()).trim();

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Date expiration = claims.getExpiration();

            if (expiration != null && expiration.before(new Date())) {

                throw new Exception("Token is expired");
            }

            Map<String, Object> result = new HashMap<>();

            result.put("schemeName", claims.get("schemeName"));
            result.put("username", claims.get("username"));
            result.put("role", claims.get("role"));

            SCHEME_NAME = (String) claims.get("schemeName");
            USERNAME = (String) claims.get("username");
            ROLE = (String) claims.get("role");

            return result;
        }

        return null;
    }

    public static ResponseEntity<?> checkAuthorizationAndReturnResult(HttpServletRequest request,
                                                                      List<String> requiredRoles,
                                                                      Supplier<Object> resultSupplier) {

        try {

            if (!isAuthorized(request, requiredRoles)) {

                return new ResponseEntity<>("Only Admin Can Access This Page!", HttpStatus.UNAUTHORIZED);
            }
            else {

                Object result = resultSupplier.get();

                if (result == null) {

                    return new ResponseEntity<>("Nothing To Show", HttpStatus.OK);
                }
                else {

                    return new ResponseEntity<>(result, HttpStatus.OK);
                }
            }
        }
        catch (IllegalArgumentException e) {

            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        catch (Exception e) {

            return new ResponseEntity<>("JWT EXPIRED: " + e, HttpStatus.UNAUTHORIZED);
        }
    }

    public static boolean isAuthorized(HttpServletRequest request, List<String> requiredRoles) throws Exception {

        Map<String, Object> tokenData = parseToken(request);

        if (tokenData != null) {

            String role = (String) tokenData.get("role");

            return !requiredRoles.contains(role);
        }

        return true;
    }
}