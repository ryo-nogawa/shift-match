package com.example.shiftmatch.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalTime;
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

    @Test
    @DisplayName("Given: saved_employee テーブルのとき, When: employment_type 列を確認すると, Then: 列が存在する")
    void savedEmployeeTableHasEmploymentTypeColumn() {
      // employment_type列を持つ行を挿入
      jdbcClient
          .sql(
              "INSERT INTO saved_employee (row_index, name, employment_type, off, start_time,"
                  + " end_time) VALUES (?, ?, ?, ?, ?, ?)")
          .params(0, "TestEmployee", "MANAGER", false, LocalTime.of(9, 0), LocalTime.of(17, 0))
          .update();

      // 挿入された値を確認
      String employmentType =
          jdbcClient
              .sql("SELECT employment_type FROM saved_employee WHERE row_index = 0")
              .query(String.class)
              .single();
      assertEquals("MANAGER", employmentType);
    }

    @Test
    @DisplayName("Given: employment_type 列がデフォルト値を持つとき, When: 列を指定しずに挿入すると, Then: 'FULL_TIME' になる")
    void defaultEmploymentTypeIsFullTime() {
      // employment_typeを指定せずに挿入
      jdbcClient
          .sql(
              "INSERT INTO saved_employee (row_index, name, off, start_time, end_time)"
                  + " VALUES (?, ?, ?, ?, ?)")
          .params(1, "DefaultEmployee", false, LocalTime.of(8, 0), LocalTime.of(16, 0))
          .update();

      // DEFAULT で 'FULL_TIME' になっていることを確認
      String employmentType =
          jdbcClient
              .sql("SELECT employment_type FROM saved_employee WHERE row_index = 1")
              .query(String.class)
              .single();
      assertEquals("FULL_TIME", employmentType);
    }

    @Test
    @DisplayName(
        "Given: 旧テーブル（employment_type列がない）データを挿入するとき, When: 直後に SELECT するとき, Then: DEFAULT の"
            + " 'FULL_TIME' になっている")
    void alterTableAddColumnWorksWithExistingData() {
      // 既存データを挿入（employment_typeなし）
      jdbcClient
          .sql(
              "INSERT INTO saved_employee (row_index, name, off, start_time, end_time)"
                  + " VALUES (?, ?, ?, ?, ?)")
          .params(2, "ExistingEmployee", false, LocalTime.of(8, 0), LocalTime.of(16, 0))
          .update();

      // ALTER TABLE は既に実行されているはずだが、既存行のデータを確認
      String employmentType =
          jdbcClient
              .sql("SELECT employment_type FROM saved_employee WHERE row_index = 2")
              .query(String.class)
              .single();
      assertEquals("FULL_TIME", employmentType, "既存行の employment_type は FULL_TIME になるべき");
    }
  }
}
