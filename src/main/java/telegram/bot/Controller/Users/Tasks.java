package telegram.bot.Controller.Users;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import telegram.bot.Service.Users.TasksService;

import java.io.IOException;
import java.util.List;

import static telegram.bot.Controller.Admins.Login.*;

@RestController
@CrossOrigin(maxAge = 3600)
@RequestMapping("/api/users/Tasks")
public class Tasks {

    private final TasksService tasksService;

    @Autowired
    public Tasks(TasksService tasksService) {

        this.tasksService = tasksService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createTask(HttpServletRequest request,
                                        @RequestParam(required = false) String description,
                                        @RequestParam(required = false) MultipartFile audioFile) {

        return checkAuthorizationAndReturnResult(request, List.of("ROLE_USER", "ROLE_ADMIN"),
                () -> {

                    if ((description == null || description.isEmpty()) && (audioFile == null || audioFile.isEmpty())) {

                        return new ResponseEntity<>("Either a description or an audio file must be provided", HttpStatus.BAD_REQUEST);
                    }

                    try {

                        tasksService.createTask(SCHEME_NAME, description, USERNAME, audioFile);
                    }
                    catch (IOException e) {

                        return e.getMessage();
                    }

                    return "Task Created Successfully";
                });
    }
}