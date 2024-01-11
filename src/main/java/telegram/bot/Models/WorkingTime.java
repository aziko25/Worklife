package telegram.bot.Models;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.sql.Date;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

public class WorkingTime {

    private int id;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime date;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime arrived_time;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime exited_time;
    private String employee_name;
    private String img_link;

    public WorkingTime(LocalDateTime date, LocalDateTime arrived_time, LocalDateTime exited_time) {

        this.date = date;
        this.arrived_time = arrived_time;
        this.exited_time = exited_time;
    }

    public WorkingTime(ZonedDateTime date1, ZonedDateTime arrivalTime, ZonedDateTime exitTime) {

        this.date = date1 != null ? date1.toLocalDateTime() : null;
        this.arrived_time = arrivalTime != null ? arrivalTime.toLocalDateTime() : null;
        this.exited_time = exitTime != null ? exitTime.toLocalDateTime() : null;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public LocalDateTime getArrived_time() {
        return arrived_time;
    }

    public void setArrived_time(LocalDateTime arrived_time) {
        this.arrived_time = arrived_time;
    }

    public LocalDateTime getExited_time() {
        return exited_time;
    }

    public void setExited_time(LocalDateTime exited_time) {
        this.exited_time = exited_time;
    }

    public String getEmployee_name() {
        return employee_name;
    }

    public void setEmployee_name(String employee_name) {
        this.employee_name = employee_name;
    }

    public String getImg_link() {
        return img_link;
    }

    public void setImg_link(String img_link) {
        this.img_link = img_link;
    }
}
