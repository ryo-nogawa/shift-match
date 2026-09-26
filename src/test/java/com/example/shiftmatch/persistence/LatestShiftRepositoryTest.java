package com.example.shiftmatch.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.ShiftAssignment;
import com.example.shiftmatch.domain.ShiftSlot;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest
class LatestShiftRepositoryTest {

  @Autowired private LatestShiftRepository repository;

  @Autowired private JdbcClient jdbcClient;

  @BeforeEach
  void clearTables() {
    jdbcClient.sql("DELETE FROM saved_employee").update();
    jdbcClient.sql("DELETE FROM saved_assignment").update();
    jdbcClient.sql("DELETE FROM saved_score").update();
  }

  @Nested
  @DisplayName("従業員入力を保存して読み出せる")
  class SaveAndFind {

    @Test
    @DisplayName("従業員入力を保存して読み出せる")
    void savesAndFindsEmployees() {
      List<Employee> employees =
          List.of(
              Employee.working("Alice", LocalTime.of(9, 0), LocalTime.of(17, 0)),
              Employee.onLeave("Bob"),
              Employee.working("Charlie", LocalTime.of(8, 30), LocalTime.of(16, 30)));

      repository.save(employees, Optional.empty());

      List<Employee> found = repository.findEmployees();
      assertEquals(3, found.size());
      assertEquals("Alice", found.get(0).name());
      assertEquals(false, found.get(0).off());
      assertEquals(LocalTime.of(9, 0), found.get(0).start());
      assertEquals(LocalTime.of(17, 0), found.get(0).end());

      assertEquals("Bob", found.get(1).name());
      assertEquals(true, found.get(1).off());
      assertEquals(null, found.get(1).start());
      assertEquals(null, found.get(1).end());

      assertEquals("Charlie", found.get(2).name());
      assertEquals(false, found.get(2).off());
      assertEquals(LocalTime.of(8, 30), found.get(2).start());
      assertEquals(LocalTime.of(16, 30), found.get(2).end());
    }

    @Test
    @DisplayName("保存がなければ空")
    void returnsEmptyWhenNoSave() {
      List<Employee> found = repository.findEmployees();
      assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("2 回目の保存は最新データで上書きされる")
    void secondSaveOverwritesPreviousData() {
      // 1 回目: 3 名を保存
      List<Employee> employees1 =
          List.of(
              Employee.working("Alice", LocalTime.of(9, 0), LocalTime.of(17, 0)),
              Employee.onLeave("Bob"),
              Employee.working("Charlie", LocalTime.of(8, 30), LocalTime.of(16, 30)));
      repository.save(employees1, Optional.empty());

      // 2 回目: 2 名を保存
      List<Employee> employees2 =
          List.of(
              Employee.working("David", LocalTime.of(10, 0), LocalTime.of(18, 0)),
              Employee.onLeave("Emma"));
      repository.save(employees2, Optional.empty());

      // 確認: 2 回目のデータだけが残っている
      List<Employee> found = repository.findEmployees();
      assertEquals(2, found.size());
      assertEquals("David", found.get(0).name());
      assertEquals("Emma", found.get(1).name());
    }
  }

  @Nested
  @DisplayName("保存がトランザクションで保護されている")
  class Transaction {

    @Test
    @DisplayName("Given: 既存データがあるとき, When: 256文字の氏名で保存に失敗すると, Then: 既存データが保持される")
    void rollsBackWhenSaveFails() {
      // 初期データを保存（8人の従業員と対応する割り当て）
      List<Employee> initialEmployees =
          List.of(
              Employee.working("Alice", LocalTime.of(9, 0), LocalTime.of(17, 0)),
              Employee.working("Bob", LocalTime.of(8, 0), LocalTime.of(16, 0)),
              Employee.working("Charlie", LocalTime.of(8, 30), LocalTime.of(16, 30)),
              Employee.working("David", LocalTime.of(9, 0), LocalTime.of(17, 0)),
              Employee.working("Emma", LocalTime.of(8, 0), LocalTime.of(16, 0)),
              Employee.working("Frank", LocalTime.of(8, 30), LocalTime.of(16, 30)),
              Employee.working("Grace", LocalTime.of(9, 0), LocalTime.of(17, 0)),
              Employee.working("Henry", LocalTime.of(8, 0), LocalTime.of(16, 0)));

      List<ShiftAssignment> initialAssignments = new ArrayList<>();
      for (int i = 0; i < ShiftSlot.totalEmployees(); i++) {
        ShiftSlot slot = ShiftSlot.values()[i % ShiftSlot.values().length];
        initialAssignments.add(
            new ShiftAssignment(
                initialEmployees.get(i), slot, LocalTime.of(12, 0), LocalTime.of(12, 45)));
      }
      AssignmentResult initialResult = new AssignmentResult(initialAssignments, 50, List.of());
      repository.save(initialEmployees, Optional.of(initialResult));

      // 256 文字の氏名を含む従業員リストで保存を試みる（失敗する）
      String longName = "A".repeat(256);
      List<Employee> invalidEmployees =
          List.of(Employee.working(longName, LocalTime.of(9, 0), LocalTime.of(17, 0)));
      assertThrows(
          DataAccessException.class, () -> repository.save(invalidEmployees, Optional.empty()));

      // 既存データが保持されていることを確認
      List<Employee> foundEmployees = repository.findEmployees();
      assertEquals(8, foundEmployees.size());
      assertEquals("Alice", foundEmployees.get(0).name());

      // 割り当てとスコアも保持されていることを確認
      Integer assignmentCount =
          jdbcClient.sql("SELECT COUNT(*) FROM saved_assignment").query(Integer.class).single();
      assertEquals(8, assignmentCount);

      Integer score =
          jdbcClient
              .sql("SELECT score FROM saved_score WHERE id = 1")
              .query(Integer.class)
              .single();
      assertEquals(50, score);
    }
  }

  @Nested
  @DisplayName("決定したシフト（割り当てとスコア）を保存し、不成立のときは消す")
  class SaveAssignmentAndScore {

    @Test
    @DisplayName("保存すると 8 件と得点が入る")
    void savesAssignmentAndScore() {
      List<Employee> employees =
          List.of(
              Employee.working("Alice", LocalTime.of(9, 0), LocalTime.of(17, 0)),
              Employee.working("Bob", LocalTime.of(8, 0), LocalTime.of(16, 0)),
              Employee.working("Charlie", LocalTime.of(8, 30), LocalTime.of(16, 30)),
              Employee.working("David", LocalTime.of(9, 0), LocalTime.of(17, 0)),
              Employee.working("Emma", LocalTime.of(8, 0), LocalTime.of(16, 0)),
              Employee.working("Frank", LocalTime.of(8, 30), LocalTime.of(16, 30)),
              Employee.working("Grace", LocalTime.of(9, 0), LocalTime.of(17, 0)),
              Employee.working("Henry", LocalTime.of(8, 0), LocalTime.of(16, 0)));

      List<ShiftAssignment> assignments = new ArrayList<>();
      for (int i = 0; i < ShiftSlot.totalEmployees(); i++) {
        ShiftSlot slot = ShiftSlot.values()[i % ShiftSlot.values().length];
        assignments.add(
            new ShiftAssignment(employees.get(i), slot, LocalTime.of(12, 0), LocalTime.of(12, 45)));
      }
      AssignmentResult result = new AssignmentResult(assignments, 100, List.of());

      repository.save(employees, Optional.of(result));

      // 8 件の割り当てを確認
      Integer assignmentCount =
          jdbcClient.sql("SELECT COUNT(*) FROM saved_assignment").query(Integer.class).single();
      assertEquals(8, assignmentCount);

      // 1 件のスコアを確認
      Integer scoreCount =
          jdbcClient.sql("SELECT COUNT(*) FROM saved_score").query(Integer.class).single();
      assertEquals(1, scoreCount);

      Integer score =
          jdbcClient
              .sql("SELECT score FROM saved_score WHERE id = 1")
              .query(Integer.class)
              .single();
      assertEquals(100, score);
    }

    @Test
    @DisplayName("2 回保存しても 8 件のまま")
    void multiplesSavesKeepEightAssignments() {
      List<Employee> employees =
          List.of(
              Employee.working("Alice", LocalTime.of(9, 0), LocalTime.of(17, 0)),
              Employee.working("Bob", LocalTime.of(8, 0), LocalTime.of(16, 0)),
              Employee.working("Charlie", LocalTime.of(8, 30), LocalTime.of(16, 30)),
              Employee.working("David", LocalTime.of(9, 0), LocalTime.of(17, 0)),
              Employee.working("Emma", LocalTime.of(8, 0), LocalTime.of(16, 0)),
              Employee.working("Frank", LocalTime.of(8, 30), LocalTime.of(16, 30)),
              Employee.working("Grace", LocalTime.of(9, 0), LocalTime.of(17, 0)),
              Employee.working("Henry", LocalTime.of(8, 0), LocalTime.of(16, 0)));

      // 1 回目の保存
      List<ShiftAssignment> assignments1 = new ArrayList<>();
      for (int i = 0; i < ShiftSlot.totalEmployees(); i++) {
        ShiftSlot slot = ShiftSlot.values()[i % ShiftSlot.values().length];
        assignments1.add(
            new ShiftAssignment(employees.get(i), slot, LocalTime.of(12, 0), LocalTime.of(12, 45)));
      }
      AssignmentResult result1 = new AssignmentResult(assignments1, 100, List.of());
      repository.save(employees, Optional.of(result1));

      // 2 回目の保存
      List<ShiftAssignment> assignments2 = new ArrayList<>();
      for (int i = 0; i < ShiftSlot.totalEmployees(); i++) {
        ShiftSlot slot = ShiftSlot.values()[(i + 1) % ShiftSlot.values().length];
        assignments2.add(
            new ShiftAssignment(employees.get(i), slot, LocalTime.of(13, 0), LocalTime.of(13, 45)));
      }
      AssignmentResult result2 = new AssignmentResult(assignments2, 150, List.of());
      repository.save(employees, Optional.of(result2));

      // 確認: 8 件のまま、スコアは更新
      Integer assignmentCount =
          jdbcClient.sql("SELECT COUNT(*) FROM saved_assignment").query(Integer.class).single();
      assertEquals(8, assignmentCount);

      Integer score =
          jdbcClient
              .sql("SELECT score FROM saved_score WHERE id = 1")
              .query(Integer.class)
              .single();
      assertEquals(150, score);
    }

    @Test
    @DisplayName("不成立（空）で割り当て・得点が消え、従業員入力は残る")
    void clearsAssignmentAndScoreWhenResultIsEmpty() {
      // 従業員と割り当て結果を保存
      List<Employee> employees =
          List.of(
              Employee.working("Alice", LocalTime.of(9, 0), LocalTime.of(17, 0)),
              Employee.working("Bob", LocalTime.of(8, 0), LocalTime.of(16, 0)),
              Employee.working("Charlie", LocalTime.of(8, 30), LocalTime.of(16, 30)),
              Employee.working("David", LocalTime.of(9, 0), LocalTime.of(17, 0)),
              Employee.working("Emma", LocalTime.of(8, 0), LocalTime.of(16, 0)),
              Employee.working("Frank", LocalTime.of(8, 30), LocalTime.of(16, 30)),
              Employee.working("Grace", LocalTime.of(9, 0), LocalTime.of(17, 0)),
              Employee.working("Henry", LocalTime.of(8, 0), LocalTime.of(16, 0)));

      List<ShiftAssignment> assignments = new ArrayList<>();
      for (int i = 0; i < ShiftSlot.totalEmployees(); i++) {
        ShiftSlot slot = ShiftSlot.values()[i % ShiftSlot.values().length];
        assignments.add(
            new ShiftAssignment(employees.get(i), slot, LocalTime.of(12, 0), LocalTime.of(12, 45)));
      }
      AssignmentResult result = new AssignmentResult(assignments, 100, List.of());
      repository.save(employees, Optional.of(result));

      // 不成立として再保存
      repository.save(employees, Optional.empty());

      // 確認: 割り当てとスコアは消え、従業員入力は残る
      Integer assignmentCount =
          jdbcClient.sql("SELECT COUNT(*) FROM saved_assignment").query(Integer.class).single();
      assertEquals(0, assignmentCount);

      Integer scoreCount =
          jdbcClient.sql("SELECT COUNT(*) FROM saved_score").query(Integer.class).single();
      assertEquals(0, scoreCount);

      List<Employee> foundEmployees = repository.findEmployees();
      assertEquals(8, foundEmployees.size());
    }
  }
}
