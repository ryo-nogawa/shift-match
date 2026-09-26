package com.example.shiftmatch.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.shiftmatch.domain.Employee;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
  }
}
