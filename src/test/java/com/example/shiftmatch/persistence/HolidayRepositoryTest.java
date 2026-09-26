package com.example.shiftmatch.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.shiftmatch.domain.Holiday;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest
@DisplayName("HolidayRepository")
class HolidayRepositoryTest {

  @Autowired private HolidayRepository repository;

  @Autowired private JdbcClient jdbcClient;

  @BeforeEach
  void clearHolidayTable() {
    jdbcClient.sql("DELETE FROM holiday").update();
  }

  @Nested
  @DisplayName("[F-10] 祝日データを保存・取得できる")
  class SaveAndFind {

    @Test
    @DisplayName("Given: 複数の祝日があるとき, When: replaceAll で保存して findByYear で取得すると, Then: 年ごとに日付順で返される")
    void savesAndFindsHolidaysByYear() {
      List<Holiday> holidays =
          List.of(
              new Holiday(LocalDate.of(2026, 1, 1), "元日"),
              new Holiday(LocalDate.of(2026, 10, 12), "スポーツの日"),
              new Holiday(LocalDate.of(2026, 2, 11), "建国記念の日"));

      repository.replaceAll(holidays);

      List<Holiday> found2026 = repository.findByYear(2026);
      assertEquals(3, found2026.size());
      // 日付順で返される
      assertEquals(LocalDate.of(2026, 1, 1), found2026.get(0).date());
      assertEquals("元日", found2026.get(0).name());
      assertEquals(LocalDate.of(2026, 2, 11), found2026.get(1).date());
      assertEquals("建国記念の日", found2026.get(1).name());
      assertEquals(LocalDate.of(2026, 10, 12), found2026.get(2).date());
      assertEquals("スポーツの日", found2026.get(2).name());
    }

    @Test
    @DisplayName("Given: 異なる年の祝日があるとき, When: findByYear で取得すると, Then: 指定した年のデータだけが返される")
    void findsByYearCorrectly() {
      List<Holiday> holidays =
          List.of(
              new Holiday(LocalDate.of(2026, 1, 1), "元日"),
              new Holiday(LocalDate.of(2025, 1, 1), "元日"),
              new Holiday(LocalDate.of(2026, 10, 12), "スポーツの日"));

      repository.replaceAll(holidays);

      List<Holiday> found2026 = repository.findByYear(2026);
      assertEquals(2, found2026.size());
      assertEquals(LocalDate.of(2026, 1, 1), found2026.get(0).date());
      assertEquals(LocalDate.of(2026, 10, 12), found2026.get(1).date());

      List<Holiday> found2025 = repository.findByYear(2025);
      assertEquals(1, found2025.size());
      assertEquals(LocalDate.of(2025, 1, 1), found2025.get(0).date());
    }

    @Test
    @DisplayName("Given: データが保存されていないとき, When: findByYear で取得すると, Then: 空のリストが返される")
    void returnsEmptyListWhenNoData() {
      List<Holiday> found = repository.findByYear(2026);
      assertTrue(found.isEmpty());
    }
  }

  @Nested
  @DisplayName("[F-10] replaceAll で全件置き換える")
  class ReplaceAll {

    @Test
    @DisplayName("Given: 既存データがあるとき, When: replaceAll で新しいデータを保存すると, Then: 既存データが全て置き換わる")
    void replacesAllData() {
      List<Holiday> holidays1 =
          List.of(
              new Holiday(LocalDate.of(2026, 1, 1), "元日"),
              new Holiday(LocalDate.of(2026, 10, 12), "スポーツの日"));
      repository.replaceAll(holidays1);

      List<Holiday> holidays2 = List.of(new Holiday(LocalDate.of(2025, 1, 1), "元日"));
      repository.replaceAll(holidays2);

      // 2026 年のデータは消えている
      List<Holiday> found2026 = repository.findByYear(2026);
      assertTrue(found2026.isEmpty());

      // 2025 年のデータだけ存在
      List<Holiday> found2025 = repository.findByYear(2025);
      assertEquals(1, found2025.size());
      assertEquals(LocalDate.of(2025, 1, 1), found2025.get(0).date());
    }
  }

  @Nested
  @DisplayName("[F-10] 年が保存済みかチェックできる")
  class ExistsInYear {

    @Test
    @DisplayName("Given: 祝日が保存されているとき, When: existsInYear で確認すると, Then: true が返される")
    void returnsTrueWhenYearExists() {
      List<Holiday> holidays =
          List.of(
              new Holiday(LocalDate.of(2026, 1, 1), "元日"),
              new Holiday(LocalDate.of(2026, 10, 12), "スポーツの日"));
      repository.replaceAll(holidays);

      assertTrue(repository.existsInYear(2026));
    }

    @Test
    @DisplayName("Given: 祝日が保存されていないとき, When: existsInYear で確認すると, Then: false が返される")
    void returnsFalseWhenYearNotExists() {
      List<Holiday> holidays = List.of(new Holiday(LocalDate.of(2026, 1, 1), "元日"));
      repository.replaceAll(holidays);

      assertFalse(repository.existsInYear(2025));
    }

    @Test
    @DisplayName("Given: テーブルが空のとき, When: existsInYear で確認すると, Then: false が返される")
    void returnsFalseWhenTableEmpty() {
      assertFalse(repository.existsInYear(2026));
    }
  }
}
