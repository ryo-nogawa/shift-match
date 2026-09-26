package com.example.shiftmatch.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Schema initialization test. */
@SpringBootTest
class SchemaTest {

  @Autowired private JdbcClient jdbcClient;

  @Test
  void savedEmployeeTableExistsAndIsEmpty() {
    Integer count =
        jdbcClient.sql("SELECT COUNT(*) FROM saved_employee").query(Integer.class).single();
    assertEquals(0, count);
  }

  @Test
  void savedAssignmentTableExistsAndIsEmpty() {
    Integer count =
        jdbcClient.sql("SELECT COUNT(*) FROM saved_assignment").query(Integer.class).single();
    assertEquals(0, count);
  }

  @Test
  void savedScoreTableExistsAndIsEmpty() {
    Integer count =
        jdbcClient.sql("SELECT COUNT(*) FROM saved_score").query(Integer.class).single();
    assertEquals(0, count);
  }
}
