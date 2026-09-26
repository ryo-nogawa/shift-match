package com.example.shiftmatch.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Schema initialization test. */
@SpringBootTest
@DisplayName("スキーマ検証")
class SchemaTest {

  @Autowired private JdbcClient jdbcClient;

  @BeforeEach
  void clearTables() {
    jdbcClient.sql("DELETE FROM saved_score").update();
    jdbcClient.sql("DELETE FROM saved_assignment").update();
    jdbcClient.sql("DELETE FROM saved_employee").update();
  }

  @Nested
  @DisplayName("保存用テーブル")
  class SchemaValidation {

    @Test
    @DisplayName("Given: テスト開始時, When: saved_employee テーブルを確認すると, Then: 空である")
    void savedEmployeeTableExistsAndIsEmpty() {
      Integer count =
          jdbcClient.sql("SELECT COUNT(*) FROM saved_employee").query(Integer.class).single();
      assertEquals(0, count);
    }

    @Test
    @DisplayName("Given: テスト開始時, When: saved_assignment テーブルを確認すると, Then: 空である")
    void savedAssignmentTableExistsAndIsEmpty() {
      Integer count =
          jdbcClient.sql("SELECT COUNT(*) FROM saved_assignment").query(Integer.class).single();
      assertEquals(0, count);
    }

    @Test
    @DisplayName("Given: テスト開始時, When: saved_score テーブルを確認すると, Then: 空である")
    void savedScoreTableExistsAndIsEmpty() {
      Integer count =
          jdbcClient.sql("SELECT COUNT(*) FROM saved_score").query(Integer.class).single();
      assertEquals(0, count);
    }
  }
}
