package com.example.shiftmatch.persistence;

import com.example.shiftmatch.domain.DailyWish;
import com.example.shiftmatch.domain.EmployeeProfile;
import com.example.shiftmatch.domain.EmploymentType;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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

  private record DayRow(DayOfWeek day, DailyWish wish) {}

  private record EmployeeRow(int rowIndex, String name, EmploymentType employmentType) {}
}
