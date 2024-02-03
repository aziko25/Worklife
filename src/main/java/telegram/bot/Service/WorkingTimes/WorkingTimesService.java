package telegram.bot.Service.WorkingTimes;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static telegram.bot.Controller.Admins.Login.SCHEME_NAME;

@Service
@RequiredArgsConstructor
public class WorkingTimesService {

    private final JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> lateAndAtTimeGraphForDateRanges(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            start = LocalDate.now();
            end = LocalDate.now();
        }

        List<Map<String, Object>> allEmployees = jdbcTemplate.queryForList("SELECT id, username FROM " + SCHEME_NAME + ".employees WHERE role != 'ROLE_WORKLY';");
        Map<String, Map<String, Object>> employeeMap = new HashMap<>();
        for (Map<String, Object> employee : allEmployees) {
            employeeMap.put((String) employee.get("username"), new HashMap<>(employee));
        }

        String fetchData = "SELECT wt.date, e.id, e.username, wt.arrived_time, wt.exited_time, wt.late, wt.time_off FROM " +
                SCHEME_NAME + ".working_time wt INNER JOIN " + SCHEME_NAME + ".employees e ON wt.employee_name = e.username WHERE wt.date BETWEEN ? AND ?;";

        List<Map<String, Object>> outputList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            List<Map<String, Object>> dailyAttendance = jdbcTemplate.queryForList(fetchData, date, date);

            Map<String, List<Map<String, Object>>> dateData = new LinkedHashMap<>();
            dateData.put("came", new ArrayList<>());
            dateData.put("late", new ArrayList<>());
            dateData.put("onTime", new ArrayList<>());
            dateData.put("timeOff", new ArrayList<>());
            dateData.put("absent", new ArrayList<>());

            for (Map<String, Object> attendance : dailyAttendance) {
                String username = (String) attendance.get("username");
                Integer late = (Integer) attendance.get("late");
                Boolean timeOff = (Boolean) attendance.get("time_off");

                Map<String, Object> employeeData = new LinkedHashMap<>(employeeMap.get(username));
                Timestamp arrival_time = (Timestamp) attendance.get("arrived_time");
                if (arrival_time != null) {
                    employeeData.put("arrival_time", arrival_time.toLocalDateTime().format(formatter));
                }
                Timestamp exit_time = (Timestamp) attendance.get("exited_time");
                if (exit_time != null) {
                    employeeData.put("exit_time", exit_time.toLocalDateTime().format(formatter));
                }

                if (timeOff != null && timeOff) {
                    dateData.get("timeOff").add(employeeData);
                } else {
                    dateData.get("came").add(employeeData);
                    if (late > 0) {
                        dateData.get("late").add(employeeData);
                    } else {
                        dateData.get("onTime").add(employeeData);
                    }
                }

                employeeMap.get(username).put("came", true);
            }

            for (String empUsername : employeeMap.keySet()) {
                if (!employeeMap.get(empUsername).containsKey("came")) {
                    dateData.get("absent").add(employeeMap.get(empUsername));
                } else {
                    employeeMap.get(empUsername).remove("came");
                }
            }

            Map<String, Object> dateEntry = new LinkedHashMap<>();
            dateEntry.put("date", date.toString());
            dateEntry.putAll(dateData);
            outputList.add(dateEntry);
        }

        return outputList;
    }


    public List<Map<String, Object>> createLeaderboard() {

        String fetchData = "SELECT e.id, e.username, COUNT(wt.date) as total_days, SUM(CASE WHEN wt.late > 0 THEN 1 ELSE 0 END) as late, SUM(CASE WHEN wt.late <= 0 THEN 1 ELSE 0 END) as onTime FROM " + SCHEME_NAME + ".employees e LEFT JOIN " + SCHEME_NAME + ".working_time wt ON e.username = wt.employee_name GROUP BY e.id, e.username;";

        List<Map<String, Object>> results = jdbcTemplate.queryForList(fetchData);

        for (Map<String, Object> result : results) {

            Long total_days = (Long) result.get("total_days");
            Long late = (Long) result.get("late");
            Long onTime = (Long) result.get("onTime");

            result.put("absent", total_days - late - onTime);
        }

        // Sort the results based on your criteria for the leaderboard
        results.sort((a, b) -> {
            int compare = ((Long) b.get("onTime")).compareTo((Long) a.get("onTime")); // Sort by onTime in descending order
            if (compare == 0) {
                compare = ((Long) a.get("late")).compareTo((Long) b.get("late")); // If onTime is equal, sort by late in ascending order
            }
            return compare;
        });

        return results;
    }
}