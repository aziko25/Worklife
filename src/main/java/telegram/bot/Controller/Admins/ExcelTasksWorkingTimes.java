package telegram.bot.Controller.Admins;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import telegram.bot.Service.AdminService;
import telegram.bot.Service.Users.TasksService;
import telegram.bot.Service.Users.WorkingTimeService;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static telegram.bot.Controller.Admins.Login.*;

@RestController
@CrossOrigin(maxAge = 3600)
@RequestMapping("/api/admins/excel")
public class ExcelTasksWorkingTimes {

    private final AdminService adminService;
    private final TasksService tasksService;
    private final WorkingTimeService workingTimeService;

    public ExcelTasksWorkingTimes(AdminService adminService, TasksService tasksService, WorkingTimeService workingTimeService) {

        this.adminService = adminService;
        this.tasksService = tasksService;
        this.workingTimeService = workingTimeService;
    }

    @GetMapping("/all/{type}")
    public ResponseEntity<?> getExcel(HttpServletRequest request,
                                                        @RequestParam LocalDate start,
                                                        @RequestParam LocalDate end,
                                                        @PathVariable String type) throws Exception {

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

            data = getDataForAdmin(type, SCHEME_NAME, start, end);
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
                                                      LocalDate start, LocalDate end) {
        if ("tasks".equals(type)) {

            return adminService.seeAllTasksRange(schemeName, start, end);
        }
        else if ("times".equals(type)) {

            return adminService.seeWorkingTimeOfAllEmployeesBetweenDates(schemeName, start, end);
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

        // Create header row
        Row headerRow = sheet.createRow(0);

        String[] columns = {"ID", "Дата", "Пришедшнее Время", "Приход Дальность От Компании (м)", "Ушедшее Время", "Уход Дальность От Компании (м)", "Работник", "Опоздал (+) / Пришел Раньше (-)", "Ушел Раньше (-) / Позже (+)"};

        for (int i = 0; i < columns.length; i++) {

            Cell cell = headerRow.createCell(i);

            cell.setCellValue(columns[i]);
            cell.setCellStyle(style);
        }

        // Fill data rows
        for (int i = 0; i < data.size(); i++) {

            Row row = sheet.createRow(i + 1);

            Map<String, Object> time = data.get(i);

            Cell cell0 = row.createCell(0);
            cell0.setCellValue((Integer) time.get("id"));
            cell0.setCellStyle(style);

            Cell cell1 = row.createCell(1);
            cell1.setCellValue((String) time.get("date"));
            cell1.setCellStyle(style);

            Cell cell2 = row.createCell(2);
            cell2.setCellValue((String) time.get("arrived_time"));
            cell2.setCellStyle(style);

            Cell cell3 = row.createCell(3);
            if (time.get("arrival_radius") != null) {
                cell3.setCellValue((Integer) time.get("arrival_radius"));
            }
            cell3.setCellStyle(style);

            Cell cell4 = row.createCell(4);
            if (time.get("exited_time") != null) {
                cell4.setCellValue((String) time.get("exited_time"));
            }
            cell4.setCellStyle(style);

            Cell cell5 = row.createCell(5);
            if (time.get("exit_radius") != null) {
                cell5.setCellValue((Integer) time.get("exit_radius"));
            }
            cell5.setCellStyle(style);

            Cell cell6 = row.createCell(6);
            cell6.setCellValue((String) time.get("employee_name"));
            cell6.setCellStyle(style);

            Cell cell7 = row.createCell(7);
            if (time.get("late") != null) {
                cell7.setCellValue((Integer) time.get("late"));
            }
            cell7.setCellStyle(style);

            Cell cell8 = row.createCell(8);
            if (time.get("overtime") != null) {
                cell8.setCellValue((Integer) time.get("overtime"));
            }
            cell8.setCellStyle(style);
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
}