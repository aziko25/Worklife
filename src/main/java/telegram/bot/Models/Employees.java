package telegram.bot.Models;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Employees {

    private int id;
    private Integer worklyCode;
    private Integer worklyPass;
    private String username;
    private String password;
    private String role;
    private Long chat_id;
    private Boolean deleted;
    private String lastname;
    private String middlename;
    private String firstname;
    private String mail;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthdate;

    private LocalTime arrivalTime;
    private LocalTime exitTime;
    private String departmentName;
}