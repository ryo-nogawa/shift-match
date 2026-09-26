package com.example.shiftmatch.persistence;

import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.DailyShiftResult;
import com.example.shiftmatch.domain.DailyWish;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.EmployeeProfile;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.MonthlyShiftInput;
import com.example.shiftmatch.domain.MonthlyShiftResult;
import com.example.shiftmatch.domain.ShiftAdjustment;
import com.example.shiftmatch.domain.ShiftAssignment;
import com.example.shiftmatch.domain.ShiftSlot;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 月間シフトの入力と決定したシフトを永続化するリポジトリ。
 *
 * <p>保存するのは対象年（暦年）の分だけです。
 */
@Repository
public class MonthlyShiftRepository {

  private final JdbcClient jdbcClient;

  /**
   * データベースクライアントを注入してインスタンスを生成します。
   *
   * @param jdbcClient データベースアクセス用のクライアント
   */
  public MonthlyShiftRepository(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  /**
   * 従業員入力を全置換で保存します。
   *
   * <p>名前が空の行は保存しません。あわせて最後の対象月を更新します。
   *
   * @param employees 従業員のリスト
   * @param month 対象月
   */
  @Transactional
  public void saveInput(List<EmployeeProfile> employees, YearMonth month) {
    jdbcClient.sql("DELETE FROM saved_input_base_shift").update();
    jdbcClient.sql("DELETE FROM saved_input_employee").update();
    int rowIndex = 0;
    for (EmployeeProfile employee : employees) {
      if (employee.name() == null || employee.name().isBlank()) {
        continue;
      }
      jdbcClient
          .sql(
              "INSERT INTO saved_input_employee (row_index, name, employment_type)"
                  + " VALUES (?, ?, ?)")
          .params(rowIndex, employee.name(), employee.employmentType().name())
          .update();
      for (Map.Entry<DayOfWeek, DailyWish> entry : employee.baseShifts().entrySet()) {
        DailyWish wish = entry.getValue();
        jdbcClient
            .sql(
                "INSERT INTO saved_input_base_shift (row_index, day_index, off, start_time,"
                    + " end_time) VALUES (?, ?, ?, ?, ?)")
            .params(rowIndex, entry.getKey().ordinal(), wish.off(), wish.start(), wish.end())
            .update();
      }
      rowIndex++;
    }
    jdbcClient.sql("DELETE FROM saved_input_meta").update();
    jdbcClient
        .sql("INSERT INTO saved_input_meta (id, last_target_month) VALUES (1, ?)")
        .params(month.toString())
        .update();
  }

  /**
   * 保存済みの従業員入力を保存順に復元します。
   *
   * @return 従業員のリスト（未保存なら空）
   */
  public List<EmployeeProfile> findEmployees() {
    List<EmployeeRow> rows =
        jdbcClient
            .sql(
                "SELECT row_index, name, employment_type FROM saved_input_employee ORDER BY"
                    + " row_index")
            .query(
                (rs, rowNum) ->
                    new EmployeeRow(
                        rs.getInt("row_index"),
                        rs.getString("name"),
                        EmploymentType.valueOf(rs.getString("employment_type"))))
            .list();
    List<EmployeeProfile> employees = new ArrayList<>();
    for (EmployeeRow row : rows) {
      Map<DayOfWeek, DailyWish> shifts = new EnumMap<>(DayOfWeek.class);
      List<DayRow> dayRows =
          jdbcClient
              .sql(
                  "SELECT day_index, off, start_time, end_time FROM saved_input_base_shift"
                      + " WHERE row_index = ? ORDER BY day_index")
              .params(row.rowIndex())
              .query(
                  (rs, rowNum) ->
                      new DayRow(
                          DayOfWeek.of(rs.getInt("day_index") + 1),
                          new DailyWish(
                              rs.getBoolean("off"),
                              rs.getObject("start_time", LocalTime.class),
                              rs.getObject("end_time", LocalTime.class))))
              .list();
      for (DayRow dayRow : dayRows) {
        shifts.put(dayRow.day(), dayRow.wish());
      }
      employees.add(new EmployeeProfile(row.name(), row.employmentType(), shifts));
    }
    return employees;
  }

  private record AdjustmentKey(LocalDate date, String employeeName) {}

  private record DayRow(DayOfWeek day, DailyWish wish) {}

  private record EmployeeRow(int rowIndex, String name, EmploymentType employmentType) {}

  /**
   * 保存済みの最後の対象月を返します。
   *
   * @return 最後の対象月（未保存なら空）
   */
  public Optional<YearMonth> findLastTargetMonth() {
    return jdbcClient
        .sql("SELECT last_target_month FROM saved_input_meta WHERE id = 1")
        .query(String.class)
        .optional()
        .map(value -> YearMonth.parse(value));
  }

  /**
   * 対象月の個別変更を上書き保存します。
   *
   * @param month 対象月
   * @param adjustments 個別変更のリスト
   */
  @Transactional
  public void saveAdjustments(YearMonth month, List<ShiftAdjustment> adjustments) {
    jdbcClient
        .sql("DELETE FROM saved_adjustment WHERE adjust_date BETWEEN ? AND ?")
        .params(month.atDay(1), month.atEndOfMonth())
        .update();
    Map<AdjustmentKey, ShiftAdjustment> latest = new LinkedHashMap<>();
    for (ShiftAdjustment adjustment : adjustments) {
      latest.put(new AdjustmentKey(adjustment.date(), adjustment.employeeName()), adjustment);
    }
    for (ShiftAdjustment adjustment : latest.values()) {
      DailyWish wish = adjustment.wish();
      jdbcClient
          .sql(
              "INSERT INTO saved_adjustment (adjust_date, employee_name, off, start_time,"
                  + " end_time) VALUES (?, ?, ?, ?, ?)")
          .params(
              adjustment.date(), adjustment.employeeName(), wish.off(), wish.start(), wish.end())
          .update();
    }
  }

  /**
   * 保存済みの個別変更をすべて、日付・従業員名順に復元します。
   *
   * @return 個別変更のリスト
   */
  public List<ShiftAdjustment> findAdjustments() {
    return jdbcClient
        .sql(
            "SELECT adjust_date, employee_name, off, start_time, end_time FROM saved_adjustment"
                + " ORDER BY adjust_date, employee_name")
        .query(
            (rs, rowNum) ->
                new ShiftAdjustment(
                    rs.getObject("adjust_date", LocalDate.class),
                    rs.getString("employee_name"),
                    new DailyWish(
                        rs.getBoolean("off"),
                        rs.getObject("start_time", LocalTime.class),
                        rs.getObject("end_time", LocalTime.class))))
        .list();
  }

  /**
   * 対象月の決定したシフトを置き換えて保存します。
   *
   * @param result 月間シフトの結果
   * @param employeeNames シフトを作成した時点の従業員名（入力順）
   */
  @Transactional
  public void saveShift(MonthlyShiftResult result, List<String> employeeNames) {
    String targetMonth = result.month().toString();
    deleteShift(targetMonth);
    for (int index = 0; index < employeeNames.size(); index++) {
      jdbcClient
          .sql("INSERT INTO saved_month_employee (target_month, row_index, name) VALUES (?, ?, ?)")
          .params(targetMonth, index, employeeNames.get(index))
          .update();
    }
    for (DailyShiftResult day : result.days()) {
      Optional<AssignmentResult> assignment = day.assignment();
      jdbcClient
          .sql(
              "INSERT INTO saved_day (day_date, target_month, available_count, score)"
                  + " VALUES (?, ?, ?, ?)")
          .params(
              day.date(),
              targetMonth,
              day.availableCount(),
              assignment.map(value -> value.score()).orElse(null))
          .update();
      assignment.ifPresent(
          value -> {
            insertAssignments(day.date(), value.assignments());
            insertUnassigned(day.date(), value.unassignedEmployees());
          });
    }
  }

  private void deleteShift(String targetMonth) {
    String dayDates = "(SELECT day_date FROM saved_day WHERE target_month = ?)";
    jdbcClient
        .sql("DELETE FROM saved_day_assignment WHERE day_date IN " + dayDates)
        .param(targetMonth)
        .update();
    jdbcClient
        .sql("DELETE FROM saved_day_unassigned WHERE day_date IN " + dayDates)
        .param(targetMonth)
        .update();
    jdbcClient.sql("DELETE FROM saved_day WHERE target_month = ?").param(targetMonth).update();
    jdbcClient
        .sql("DELETE FROM saved_month_employee WHERE target_month = ?")
        .param(targetMonth)
        .update();
  }

  private void insertAssignments(LocalDate date, List<ShiftAssignment> assignments) {
    for (int index = 0; index < assignments.size(); index++) {
      ShiftAssignment assignment = assignments.get(index);
      Employee employee = assignment.employee();
      jdbcClient
          .sql(
              "INSERT INTO saved_day_assignment (day_date, assignment_index, employee_name,"
                  + " employment_type, wish_start, wish_end, slot, break_start, break_end)"
                  + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
          .params(
              date,
              index,
              employee.name(),
              employee.employmentType().name(),
              employee.start(),
              employee.end(),
              assignment.slot().name(),
              assignment.breakStart(),
              assignment.breakEnd())
          .update();
    }
  }

  private void insertUnassigned(LocalDate date, List<Employee> unassignedEmployees) {
    for (int index = 0; index < unassignedEmployees.size(); index++) {
      Employee employee = unassignedEmployees.get(index);
      jdbcClient
          .sql(
              "INSERT INTO saved_day_unassigned (day_date, unassigned_index, employee_name,"
                  + " employment_type, off, wish_start, wish_end, reason)"
                  + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)")
          .params(
              date,
              index,
              employee.name(),
              employee.employmentType().name(),
              employee.off(),
              employee.start(),
              employee.end(),
              employee.unassignedReason().name())
          .update();
    }
  }

  /**
   * 指定した月の保存済みシフトを復元します。
   *
   * @param month 対象月
   * @return 保存済みのシフト（保存がなければ空）
   */
  public Optional<SavedMonthlyShift> findShift(YearMonth month) {
    String targetMonth = month.toString();
    List<String> employeeNames =
        jdbcClient
            .sql("SELECT name FROM saved_month_employee WHERE target_month = ? ORDER BY row_index")
            .param(targetMonth)
            .query(String.class)
            .list();
    List<DailyShiftResult> days =
        jdbcClient
            .sql(
                "SELECT day_date, available_count, score FROM saved_day WHERE target_month = ?"
                    + " ORDER BY day_date")
            .param(targetMonth)
            .query(
                (rs, rowNum) -> {
                  LocalDate date = rs.getObject("day_date", LocalDate.class);
                  Integer score = rs.getObject("score", Integer.class);
                  Optional<AssignmentResult> assignment =
                      score == null ? Optional.empty() : Optional.of(findAssignment(date, score));
                  return new DailyShiftResult(date, rs.getInt("available_count"), assignment);
                })
            .list();
    if (days.isEmpty() && employeeNames.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(new SavedMonthlyShift(new MonthlyShiftResult(month, days), employeeNames));
  }

  private AssignmentResult findAssignment(LocalDate date, int score) {
    List<ShiftAssignment> assignments =
        jdbcClient
            .sql(
                "SELECT employee_name, employment_type, wish_start, wish_end, slot, break_start,"
                    + " break_end FROM saved_day_assignment WHERE day_date = ?"
                    + " ORDER BY assignment_index")
            .param(date)
            .query(
                (rs, rowNum) ->
                    new ShiftAssignment(
                        Employee.working(
                            rs.getString("employee_name"),
                            EmploymentType.valueOf(rs.getString("employment_type")),
                            rs.getObject("wish_start", LocalTime.class),
                            rs.getObject("wish_end", LocalTime.class)),
                        ShiftSlot.valueOf(rs.getString("slot")),
                        rs.getObject("break_start", LocalTime.class),
                        rs.getObject("break_end", LocalTime.class)))
            .list();
    List<Employee> unassigned =
        jdbcClient
            .sql(
                "SELECT employee_name, employment_type, off, wish_start, wish_end"
                    + " FROM saved_day_unassigned WHERE day_date = ? ORDER BY unassigned_index")
            .param(date)
            .query(
                (rs, rowNum) ->
                    new Employee(
                        rs.getString("employee_name"),
                        EmploymentType.valueOf(rs.getString("employment_type")),
                        rs.getBoolean("off"),
                        rs.getObject("wish_start", LocalTime.class),
                        rs.getObject("wish_end", LocalTime.class)))
            .list();
    return new AssignmentResult(assignments, score, unassigned);
  }

  /**
   * 入力と決定したシフトを 1 トランザクションで保存します。
   *
   * @param input 月間シフトの入力
   * @param result 月間シフトの結果
   * @param employeeNames シフトを作成した時点の従業員名（入力順）
   */
  @Transactional
  public void save(MonthlyShiftInput input, MonthlyShiftResult result, List<String> employeeNames) {
    deleteOtherYears(input.month().getYear());
    saveInput(input.employees(), input.month());
    saveAdjustments(input.month(), input.adjustments());
    saveShift(result, employeeNames);
  }

  private void deleteOtherYears(int year) {
    String otherYearMonths = "(SELECT day_date FROM saved_day WHERE LEFT(target_month, 4) <> ?)";
    String yearText = String.valueOf(year);
    jdbcClient
        .sql("DELETE FROM saved_day_assignment WHERE day_date IN " + otherYearMonths)
        .param(yearText)
        .update();
    jdbcClient
        .sql("DELETE FROM saved_day_unassigned WHERE day_date IN " + otherYearMonths)
        .param(yearText)
        .update();
    jdbcClient
        .sql("DELETE FROM saved_day WHERE LEFT(target_month, 4) <> ?")
        .param(yearText)
        .update();
    jdbcClient
        .sql("DELETE FROM saved_month_employee WHERE LEFT(target_month, 4) <> ?")
        .param(yearText)
        .update();
    jdbcClient
        .sql("DELETE FROM saved_adjustment WHERE YEAR(adjust_date) <> ?")
        .param(year)
        .update();
  }
}
