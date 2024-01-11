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

    public SortedMap<LocalDate, Map<String, List<Map<String, Object>>>> lateAndAtTimeGraphForDateRanges(LocalDate start, LocalDate end) {

        // If start and end dates are not provided, set them to today's date
        if (start == null || end == null) {

            start = LocalDate.now();
            end = LocalDate.now();
        }

        String fetchData = "SELECT wt.date, e.id, e.username, wt.arrived_time, wt.exited_time, wt.late FROM " + SCHEME_NAME + ".working_time wt INNER JOIN " + SCHEME_NAME + ".employees e ON wt.employee_name = e.username WHERE wt.date BETWEEN ? AND ?;";

        List<Map<String, Object>> results = jdbcTemplate.queryForList(fetchData, start, end);

        SortedMap<LocalDate, Map<String, List<Map<String, Object>>>> output = new TreeMap<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (Map<String, Object> result : results) {

            LocalDate date = ((Timestamp) result.get("date")).toLocalDateTime().toLocalDate();
            Integer id = (Integer) result.get("id");
            String username = (String) result.get("username");
            Integer late = (Integer) result.get("late");

            Map<String, Object> employeeData = new LinkedHashMap<>();
            employeeData.put("id", id);
            employeeData.put("username", username);

            Timestamp arrival_time = (Timestamp) result.get("arrived_time");
            if (arrival_time != null) {
                employeeData.put("arrival_time", arrival_time.toLocalDateTime().format(formatter));
            }

            Timestamp exit_time = (Timestamp) result.get("exited_time");
            if (exit_time != null) {
                employeeData.put("exit_time", exit_time.toLocalDateTime().format(formatter));
            }

            if (!output.containsKey(date)) {
                output.put(date, new LinkedHashMap<>());
            }

            Map<String, List<Map<String, Object>>> dateData = output.get(date);

            if (late > 0) {

                employeeData.put("late", late);

                if (!dateData.containsKey("late")) {

                    dateData.put("late", new ArrayList<>());
                }

                dateData.get("late").add(employeeData);
            }
            else {

                if (!dateData.containsKey("onTime")) {

                    dateData.put("onTime", new ArrayList<>());
                }

                dateData.get("onTime").add(employeeData);
            }
        }

        List<Map<String, Object>> employees = jdbcTemplate.queryForList("SELECT id, username FROM " + SCHEME_NAME + ".employees WHERE role IN ('ROLE_ADMIN', 'ROLE_EMPLOYEE', 'ROLE_USER');");

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {

            if (!output.containsKey(date)) {

                Map<String, List<Map<String, Object>>> data = new LinkedHashMap<>();

                data.put("came", new ArrayList<>());
                data.put("late", new ArrayList<>());
                data.put("onTime", new ArrayList<>());
                data.put("absent", new ArrayList<>(employees));

                output.put(date, data);
            }
            else {

                Map<String, List<Map<String, Object>>> data = output.get(date);

                List<Map<String, Object>> came = data.getOrDefault("came", new ArrayList<>());

                List<Map<String, Object>> absent = new ArrayList<>(employees);
                absent.removeAll(came);

                data.put("absent", absent);
            }
        }

        return output;
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