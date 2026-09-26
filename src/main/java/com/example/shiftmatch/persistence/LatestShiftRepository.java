package com.example.shiftmatch.persistence;

import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.Employee;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 最新 1 件のシフト決定結果を永続化するリポジトリ。
 *
 * <p>従業員入力と割り当て結果を H2 データベースに保存・復元します。
 */
@Repository
public class LatestShiftRepository {

  private final JdbcClient jdbcClient;

  /**
   * データベースクライアントを注入してインスタンスを生成します。
   *
   * @param jdbcClient データベースアクセス用のクライアント
   */
  public LatestShiftRepository(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  /**
   * 従業員入力と割り当て結果を保存します。
   *
   * <p>既存データは全削除して保存するため、常に最新 1 件だけを保持します。
   *
   * @param employees 従業員リスト（名前が空でない従業員のみ）
   * @param result 割り当て結果（空の場合は従業員入力のみ保存）
   */
  @Transactional
  public void save(List<Employee> employees, Optional<AssignmentResult> result) {
    jdbcClient.sql("DELETE FROM saved_employee").update();
    jdbcClient.sql("DELETE FROM saved_assignment").update();
    jdbcClient.sql("DELETE FROM saved_score").update();

    for (int i = 0; i < employees.size(); i++) {
      Employee employee = employees.get(i);
      jdbcClient
          .sql(
              "INSERT INTO saved_employee (row_index, name, off, start_time, end_time)"
                  + " VALUES (?, ?, ?, ?, ?)")
          .params(
              i,
              employee.name(),
              employee.off(),
              employee.off() ? null : employee.start(),
              employee.off() ? null : employee.end())
          .update();
    }

    // 入力エラー時に前回の保存を維持するため、割り当て結果がある場合のみ保存
    if (result.isPresent()) {
      AssignmentResult assignmentResult = result.get();
      var assignments = assignmentResult.assignments();
      for (int i = 0; i < assignments.size(); i++) {
        var assignment = assignments.get(i);
        jdbcClient
            .sql(
                "INSERT INTO saved_assignment"
                    + " (assignment_index, employee_name, slot, break_start, break_end)"
                    + " VALUES (?, ?, ?, ?, ?)")
            .params(
                i,
                assignment.employee().name(),
                assignment.slot().name(),
                assignment.breakStart(),
                assignment.breakEnd())
            .update();
      }

      jdbcClient
          .sql("INSERT INTO saved_score (id, score) VALUES (?, ?)")
          .params(1, assignmentResult.score())
          .update();
    }
  }

  /**
   * 保存済みの従業員入力を読み出します。
   *
   * <p>保存がない場合は空のリストを返します。
   *
   * @return 従業員リスト（row_index の昇順）
   */
  public List<Employee> findEmployees() {
    return jdbcClient
        .sql(
            "SELECT row_index, name, off, start_time, end_time FROM saved_employee"
                + " ORDER BY row_index")
        .query(
            (rs, rowNum) ->
                new Employee(
                    rs.getString("name"),
                    rs.getBoolean("off"),
                    rs.getObject("start_time", LocalTime.class),
                    rs.getObject("end_time", LocalTime.class)))
        .list();
  }
}
