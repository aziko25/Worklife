package telegram.bot.Controller.Admins;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import telegram.bot.Service.AdminService;
import telegram.bot.Service.Users.TasksService;
import telegram.bot.Service.Users.WorkingTimeService;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static telegram.bot.Controller.Admins.Login.*;

@RestController
@CrossOrigin(maxAge = 3600)
@RequestMapping("/api/admins/excel")
public class ExcelTasksWorkingTimes {

    private final AdminService adminService;
    private final TasksService tasksService;
    private final WorkingTimeService workingTimeService;
    private final JdbcTemplate jdbcTemplate;

    public ExcelTasksWorkingTimes(AdminService adminService, TasksService tasksService, WorkingTimeService workingTimeService, JdbcTemplate jdbcTemplate) {

        this.adminService = adminService;
        this.tasksService = tasksService;
        this.workingTimeService = workingTimeService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/all/{type}")
    public ResponseEntity<?> getExcel(HttpServletRequest request,
                                                        @RequestParam LocalDate start,
                                                        @RequestParam LocalDate end,
                                                        @PathVariable String type,
                                                        @RequestParam(required = false) String departmentName) throws Exception {

        type = type.toLowerCase();

        if (!type.equals("tasks") && !type.equals("times")) {

            return ResponseEntity.badRequest().body("Invalid type parameter. It must be either 'tasks' or 'times'.");
        }

        Map<String, Object> tokenData = parseToken(request);
        assert tokenData != null;

        String role = (String) tokenData.get("role");
        List<Map<String, Object>> data;

        if ("ROLE_USER".equals(role)) {

            data = getDataForUser(type, SCHEME_NAME, USERNAME, start, end);
        }
        else {

            data = getDataForAdmin(type, SCHEME_NAME, start, end, departmentName);
        }

        if (data != null) {

            String fileName = type + ".xlsx";

            createExcelFile(data, fileName);

            InputStreamResource file = new InputStreamResource(new FileInputStream(fileName));

            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

            executor.schedule(() -> {

                try {

                    Files.deleteIfExists(Paths.get(fileName));
                }
                catch (IOException e) {

                    e.printStackTrace();
                }
            }, 1, TimeUnit.MILLISECONDS);

            executor.shutdown();

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=" + fileName + ".xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                    .body(file);
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    private List<Map<String, Object>> getDataForUser(String type, String schemeName, String username,
                                                     LocalDate start, LocalDate end) {
        if ("tasks".equals(type)) {

            return tasksService.myTasksRange(schemeName, username, start, end);
        }
        else if ("times".equals(type)) {

            return workingTimeService.myWorkingTimeRange(schemeName, username, start, end);
        }

        return null;
    }

    private List<Map<String, Object>> getDataForAdmin(String type, String schemeName,
                                                      LocalDate start, LocalDate end, String departmentName) {
        if ("tasks".equals(type)) {

            return adminService.seeAllTasksRange(schemeName, start, end);
        }
        else if ("times".equals(type)) {

            return adminService.seeWorkingTimeOfAllEmployeesBetweenDatesUpdatedForExcel(schemeName, start, end, departmentName);
        }

        return null;
    }

    private void createExcelFile(List<Map<String, Object>> data, String fileName) {

        if ("tasks.xlsx".equals(fileName)) {

            ExcelForTask(data, fileName);
        }
        else if ("times.xlsx".equals(fileName)) {

            ExcelForTimes(data, fileName);
        }
    }

    public void ExcelForTask(List<Map<String, Object>> data, String fileName) {

        Workbook workbook = new XSSFWorkbook();

        Sheet sheet = workbook.createSheet("Tasks");

        // Create header row
        Row headerRow = sheet.createRow(0);

        String[] columns = {"ID", "Описание Задачи", "Ответственный За Задачу", "Выполненное Время", "Выполнено", "Дата Постановления Задачи", "Задачу Постановил"};

        for (int i = 0; i < columns.length; i++) {

            Cell cell = headerRow.createCell(i);

            cell.setCellValue(columns[i]);
        }

        // Fill data rows
        for (int i = 0; i < data.size(); i++) {

            Row row = sheet.createRow(i + 1);

            Map<String, Object> task = data.get(i);

            row.createCell(0).setCellValue((Integer) task.get("id"));
            row.createCell(1).setCellValue((String) task.get("description"));
            row.createCell(2).setCellValue((String) task.get("responsible_name"));
            row.createCell(3).setCellValue((String) task.get("done"));

            if ((Boolean) task.get("completed")) {

                row.createCell(4).setCellValue("Да");
            }
            else {

                row.createCell(4).setCellValue("Нет");
            }

            row.createCell(5).setCellValue((String) task.get("creation_date"));

            if (task.get("appointed") != null) {

                row.createCell(6).setCellValue((String) task.get("appointed"));
            }
        }

        // Adjust column width to fit the contents
        for (int i = 0; i < columns.length; i++) {

            sheet.autoSizeColumn(i);
        }

        // Write to file
        try (FileOutputStream fileOut = new FileOutputStream(fileName)) {

            workbook.write(fileOut);
        }
        catch (IOException e) {

            e.printStackTrace();
        }
    }

    public void ExcelForTimes(List<Map<String, Object>> data, String fileName) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Times");

        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);

        CellStyle nameStyle = workbook.createCellStyle();
        nameStyle.setAlignment(HorizontalAlignment.CENTER);
        Font font = workbook.createFont();
        font.setBold(true);
        nameStyle.setFont(font);

        String[] columns = {"Дата", "Приход", "Уход", "Норма", "Приход", "Уход", "Отработано", "Опоздание", "Ранний Уход",
                "Переработка", "Недоработка", "Работник", "Отпросился"};

        createHeaderRow(sheet, style, columns);

        int rowIndex = 1;

        // Group data by employee_name
        Map<String, List<Map<String, Object>>> groupedData = data.stream()
                .collect(Collectors.groupingBy(e -> (String) e.get("employee_name")));

        for (Map.Entry<String, List<Map<String, Object>>> entry : groupedData.entrySet()) {
            String employeeName = entry.getKey();
            List<Map<String, Object>> employeeData = entry.getValue();

            // Add an empty row before each employee_name
            sheet.createRow(rowIndex++);

            // Create a row for the employee_name
            Row nameRow = sheet.createRow(rowIndex++);
            Cell nameCell = nameRow.createCell(0);
            nameCell.setCellValue(employeeName);
            nameCell.setCellStyle(nameStyle);

            // Merge the cells in the name row
            sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, columns.length - 1));

            // Create data rows for the current employee
            for (Map<String, Object> time : employeeData) {
                Row row = sheet.createRow(rowIndex++);
                createDataRow(time, row, style, columns);
            }
        }

        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        try (FileOutputStream fileOut = new FileOutputStream(fileName)) {
            workbook.write(fileOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void createHeaderRow(Sheet sheet, CellStyle style, String[] columns) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(style);
        }
    }

    private void createDataRow(Map<String, Object> time, Row row, CellStyle style, String[] columns) {

        for (int j = 0; j < columns.length; j++) {
            Cell cell = row.createCell(j);
            cell.setCellStyle(style);

            switch (j) {
                case 0 -> cell.setCellValue((String) time.get("date")); // Дата
                case 1 -> cell.setCellValue(toLocalTimeString(time.get("arrival_time"))); // Приход
                case 2 -> cell.setCellValue(toLocalTimeString(time.get("exit_time"))); // Уход
                case 3 -> cell.setCellValue(calculateWorkedHours(time.get("arrival_time"), time.get("exit_time"))); // Норма
                case 4 -> cell.setCellValue(toLocalTimeString(time.get("arrived_time"))); // Приход
                case 5 -> cell.setCellValue(toLocalTimeString(time.get("exited_time"))); // Уход
                case 6 -> cell.setCellValue(calculateWorkedHours(time.get("arrived_time"), time.get("exited_time"))); // Отработано

                case 7 -> {
                    Integer late = (Integer) time.get("late");
                    if (late != null) {
                        if (late < 0) {
                            cell.setCellValue("-");
                        } else {
                            Duration duration = Duration.ofMinutes(late);
                            String formattedTime = String.format("%02d:%02d", duration.toHoursPart(), duration.toMinutesPart());
                            cell.setCellValue(formattedTime);
                        }
                    } else {
                        cell.setCellValue("-");
                    }
                } // Опоздание
                case 8 -> {
                    Integer overtime = (Integer) time.get("overtime");
                    if (overtime != null) {
                        if (overtime > 0) {
                            cell.setCellValue("-");
                        } else {
                            Duration duration = Duration.ofMinutes(Math.abs(overtime));
                            String formattedTime = String.format("%02d:%02d", duration.toHoursPart(), duration.toMinutesPart());
                            cell.setCellValue(formattedTime);
                        }
                    } else {
                        cell.setCellValue("-");
                    }
                } // Ранний Уход

                case 9 -> {

                    LocalTime arrivedTime = toLocalTime(time.get("arrived_time"));
                    LocalTime arrivalTime = toLocalTime(time.get("arrival_time"));

                    LocalTime exitedTime = toLocalTime(time.get("exited_time"));
                    LocalTime exitTime = toLocalTime(time.get("exit_time"));

                    if (exitedTime == null || exitedTime.equals(LocalTime.of(0, 0))) {
                        cell.setCellValue("-");
                    } else {
                        assert arrivalTime != null;
                        long diffArrivalExit = Duration.between(arrivalTime, exitTime).toMinutes();
                        assert arrivedTime != null;
                        long diffArrivedExited = Duration.between(arrivedTime, exitedTime).toMinutes();

                        if (diffArrivedExited > diffArrivalExit) {
                            LocalTime diffTime = LocalTime.of((int) ((diffArrivedExited - diffArrivalExit) / 60), (int) ((diffArrivedExited - diffArrivalExit) % 60));
                            cell.setCellValue(diffTime.toString()); // Переработка
                        } else {
                            cell.setCellValue("-");
                        }
                    }
                } // Переработка

                case 10 -> {
                    LocalTime arrivedTime = toLocalTime(time.get("arrived_time"));
                    LocalTime arrivalTime = toLocalTime(time.get("arrival_time"));

                    LocalTime exitedTime = toLocalTime(time.get("exited_time"));
                    LocalTime exitTime = toLocalTime(time.get("exit_time"));

                    if (exitedTime == null || exitedTime.equals(LocalTime.of(0, 0))) {
                        cell.setCellValue("-");
                    } else {
                        assert arrivalTime != null;
                        long diffArrivalExit = Duration.between(arrivalTime, exitTime).toMinutes();
                        assert arrivedTime != null;
                        long diffArrivedExited = Duration.between(arrivedTime, exitedTime).toMinutes();

                        if (diffArrivalExit > diffArrivedExited) {
                            LocalTime diffTime = LocalTime.of((int) ((diffArrivalExit - diffArrivedExited) / 60), (int) ((diffArrivalExit - diffArrivedExited) % 60));
                            cell.setCellValue(diffTime.toString()); // Недоработка
                        } else {
                            cell.setCellValue("-");
                        }
                    }
                } // Недоработка
                case 11 -> cell.setCellValue(time.get("time_off") != null ? "Да" : ""); // Отпросился
            }
        }
    }

    private String calculateWorkedHours(Object startTimeObj, Object endTimeObj) {
        LocalTime startTime = toLocalTime(startTimeObj);
        LocalTime endTime = toLocalTime(endTimeObj);
        if (startTime != null && endTime != null) {
            long minutes = ChronoUnit.MINUTES.between(startTime, endTime);
            return String.format("%dч %dм", minutes / 60, minutes % 60);
        }
        return "";
    }

    private String toLocalTimeString(Object timeObject) {
        LocalTime time = toLocalTime(timeObject);
        return time != null ? time.toString() : "";
    }

    private LocalTime toLocalTime(Object timeObject) {
        if (timeObject instanceof java.sql.Time) {
            return ((java.sql.Time) timeObject).toLocalTime();
        } else if (timeObject instanceof LocalDateTime) {
            return ((LocalDateTime) timeObject).toLocalTime();
        } else if (timeObject instanceof Timestamp) {
            return ((Timestamp) timeObject).toLocalDateTime().toLocalTime();
        } else if (timeObject instanceof String timeString) {
            if (timeString.contains(" ")) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime dateTime = LocalDateTime.parse(timeString, formatter);
                return dateTime.toLocalTime();
            } else {
                return LocalTime.parse(timeString);
            }
        }
        return null;
    }
}