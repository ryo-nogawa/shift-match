package com.example.shiftmatch.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Schema initialization test. */
@SpringBootTest(properties = "holiday.refresh-on-startup=false")
@DisplayName("スキーマ検証")
class SchemaTest {

  private static final List<String> SAVED_TABLES =
      List.of(
          "saved_day_unassigned",
          "saved_day_assignment",
          "saved_day",
          "saved_month_employee",
          "saved_adjustment",
          "saved_input_meta",
          "saved_input_base_shift",
          "saved_input_employee");

  @Autowired private JdbcClient jdbcClient;

  @BeforeEach
  void clearTables() {
    SAVED_TABLES.forEach(table -> jdbcClient.sql("DELETE FROM " + table).update());
    jdbcClient.sql("DELETE FROM holiday").update();
  }

  @Nested
  @DisplayName("保存用テーブル")
  class SchemaValidation {

    @ParameterizedTest(name = "{0}")
    @ValueSource(
        strings = {
          "saved_input_employee",
          "saved_input_base_shift",
          "saved_input_meta",
          "saved_adjustment",
          "saved_month_employee",
          "saved_day",
          "saved_day_assignment",
          "saved_day_unassigned"
        })
    @DisplayName("[F-7] Given: テスト開始時, When: 保存用テーブルを確認すると, Then: 存在し空である")
    void savedTableExistsAndIsEmpty(String table) {
      Integer count = jdbcClient.sql("SELECT COUNT(*) FROM " + table).query(Integer.class).single();
      assertEquals(0, count);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"employee", "assignment", "score"})
    @DisplayName("[F-7] Given: 旧 1 日分のテーブル, When: 存在を確認すると, Then: 撤去されている")
    void legacyTablesAreDropped(String suffix) {
      String table = "saved_" + suffix;
      Integer count =
          jdbcClient
              .sql("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?")
              .param(table.toUpperCase())
              .query(Integer.class)
              .single();
      assertEquals(0, count);
    }

    @Test
    @DisplayName("[F-10] Given: テスト開始時, When: holiday テーブルを確認すると, Then: 空である")
    void holidayTableExistsAndIsEmpty() {
      Integer count = jdbcClient.sql("SELECT COUNT(*) FROM holiday").query(Integer.class).single();
      assertEquals(0, count);
    }
  }
}
