package com.example.shiftmatch.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.EmploymentType;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.ScriptUtils;

/** 初期データ（seed.sql）の検証。 */
@SpringBootTest(properties = "holiday.refresh-on-startup=false")
@DisplayName("初期データ（seed.sql）")
class SeedDataTest {

  @Autowired private JdbcClient jdbcClient;
  @Autowired private DataSource dataSource;
  @Autowired private LatestShiftRepository repository;

  @BeforeEach
  void clearTables() {
    jdbcClient.sql("DELETE FROM saved_score").update();
    jdbcClient.sql("DELETE FROM saved_assignment").update();
    jdbcClient.sql("DELETE FROM saved_employee").update();
  }

  private void runSeed() throws SQLException {
    try (var connection = dataSource.getConnection()) {
      ScriptUtils.executeSqlScript(connection, new ClassPathResource("seed.sql"));
    }
  }

  @Test
  @DisplayName("Given: 保存が空, When: seed.sql を実行すると, Then: 重複しない氏名の従業員が 12 名入る")
  void seedsTwelveUniqueEmployees() throws SQLException {
    runSeed();

    List<Employee> employees = repository.findEmployees();
    assertEquals(12, employees.size());
    assertEquals(12, employees.stream().map(e -> e.name()).distinct().count());
  }

  @Test
  @DisplayName("Given: 保存が空, When: seed.sql を実行すると, Then: 休みと勤務時間帯がまばらに混在する")
  void seedsMixOfOffAndVariedRanges() throws SQLException {
    runSeed();

    List<Employee> employees = repository.findEmployees();
    long offCount = employees.stream().filter(e -> e.off()).count();
    assertTrue(offCount >= 2 && offCount <= 4, "休みは 2〜4 名: " + offCount);
    assertTrue(
        employees.stream().filter(e -> !e.off()).map(e -> e.start()).distinct().count() >= 4,
        "開始時刻が 4 種類以上");
    assertTrue(
        employees.stream().filter(e -> !e.off()).map(e -> e.end()).distinct().count() >= 4,
        "終了時刻が 4 種類以上");
  }

  @Test
  @DisplayName("Given: 保存済みの入力がある, When: seed.sql を実行すると, Then: 既存の保存内容を上書きしない")
  void doesNotOverwriteExistingData() throws SQLException {
    repository.save(
        List.of(
            new Employee(
                "既存 太郎", EmploymentType.FULL_TIME, false, LocalTime.of(9, 0), LocalTime.of(18, 0))),
        Optional.empty());

    runSeed();

    List<Employee> employees = repository.findEmployees();
    assertEquals(1, employees.size());
    assertEquals("既存 太郎", employees.get(0).name());
  }

  @Test
  @DisplayName("Given: 保存が空, When: seed.sql を実行すると, Then: 常勤・パート・管理職が各 1 名以上含まれる")
  void seedContainsMixedEmploymentTypes() throws SQLException {
    runSeed();

    List<Employee> employees = repository.findEmployees();
    long fullTimeCount =
        employees.stream().filter(e -> e.employmentType() == EmploymentType.FULL_TIME).count();
    long partTimeCount =
        employees.stream().filter(e -> e.employmentType() == EmploymentType.PART_TIME).count();
    long managerCount =
        employees.stream().filter(e -> e.employmentType() == EmploymentType.MANAGER).count();

    assertTrue(fullTimeCount >= 1, "常勤は 1 名以上: " + fullTimeCount);
    assertTrue(partTimeCount >= 1, "パートは 1 名以上: " + partTimeCount);
    assertTrue(managerCount >= 1, "管理職は 1 名以上: " + managerCount);
  }
}
