package com.example.shiftmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.shiftmatch.domain.Holiday;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("HolidayCsvParser")
class HolidayCsvParserTest {

  private HolidayCsvParser parser;

  @BeforeEach
  void setUp() {
    parser = new HolidayCsvParser();
  }

  @Nested
  @DisplayName("[F-10] 祝日 CSV をパースする")
  class ParseCsv {

    @Test
    @DisplayName("Given: 見出し行とデータ行がある CSV のとき, When: パースすると, Then: 見出し行は読み飛ばされ、データだけが返される")
    void skipsHeaderRow() {
      String csv = "国民の祝日・休日月日,国民の祝日・休日名称\n2026/1/1,元日\n2026/10/12,スポーツの日\n";
      byte[] csvBytes = csv.getBytes(Charset.forName("Shift_JIS"));

      List<Holiday> result = parser.parse(csvBytes);

      assertEquals(2, result.size());
      assertEquals(LocalDate.of(2026, 1, 1), result.get(0).date());
      assertEquals("元日", result.get(0).name());
      assertEquals(LocalDate.of(2026, 10, 12), result.get(1).date());
      assertEquals("スポーツの日", result.get(1).name());
    }

    @Test
    @DisplayName("Given: 日本語の祝日名が含まれる CSV のとき, When: パースすると, Then: 祝日名が文字化けせずに返される")
    void parseJapaneseName() {
      String csv = "国民の祝日・休日月日,国民の祝日・休日名称\n2026/1/1,元日\n2026/2/11,建国記念の日\n2026/9/21,敬老の日\n";
      byte[] csvBytes = csv.getBytes(Charset.forName("Shift_JIS"));

      List<Holiday> result = parser.parse(csvBytes);

      assertEquals(3, result.size());
      assertEquals("元日", result.get(0).name());
      assertEquals("建国記念の日", result.get(1).name());
      assertEquals("敬老の日", result.get(2).name());
    }

    @Test
    @DisplayName("Given: 空行を含む CSV のとき, When: パースすると, Then: 空行は無視される")
    void ignoresEmptyLines() {
      String csv = "国民の祝日・休日月日,国民の祝日・休日名称\n2026/1/1,元日\n\n2026/10/12,スポーツの日\n";
      byte[] csvBytes = csv.getBytes(Charset.forName("Shift_JIS"));

      List<Holiday> result = parser.parse(csvBytes);

      assertEquals(2, result.size());
      assertEquals(LocalDate.of(2026, 1, 1), result.get(0).date());
      assertEquals(LocalDate.of(2026, 10, 12), result.get(1).date());
    }

    @Test
    @DisplayName("Given: 日付が yyyy/M/d 形式の CSV のとき, When: パースすると, Then: LocalDate に正しく変換される")
    void parsesDateInYyyyMdFormat() {
      String csv = "国民の祝日・休日月日,国民の祝日・休日名称\n2026/1/1,元日\n2026/10/12,スポーツの日\n";
      byte[] csvBytes = csv.getBytes(Charset.forName("Shift_JIS"));

      List<Holiday> result = parser.parse(csvBytes);

      assertEquals(LocalDate.of(2026, 1, 1), result.get(0).date());
      assertEquals(LocalDate.of(2026, 10, 12), result.get(1).date());
    }

    @Test
    @DisplayName("Given: 日付が読めない行がある CSV のとき, When: パースすると, Then: IllegalArgumentException を投げる")
    void throwsWhenInvalidDate() {
      String csv = "国民の祝日・休日月日,国民の祝日・休日名称\n2026-01-01,元日\n";
      byte[] csvBytes = csv.getBytes(Charset.forName("Shift_JIS"));

      assertThrows(IllegalArgumentException.class, () -> parser.parse(csvBytes));
    }

    @Test
    @DisplayName("Given: 列が不足している行がある CSV のとき, When: パースすると, Then: IllegalArgumentException を投げる")
    void throwsWhenMissingColumn() {
      String csv = "国民の祝日・休日月日,国民の祝日・休日名称\n2026/1/1\n";
      byte[] csvBytes = csv.getBytes(Charset.forName("Shift_JIS"));

      assertThrows(IllegalArgumentException.class, () -> parser.parse(csvBytes));
    }

    @Test
    @DisplayName(
        "[F-10] Given: 暦にない日付 2026/2/30 がある CSV のとき, When: パースすると, Then: IllegalArgumentException"
            + " を投げる")
    void throwsWhenInvalidDay() {
      String csv = "国民の祝日・休日月日,国民の祝日・休日名称\n2026/2/30,不正な日付\n";
      byte[] csvBytes = csv.getBytes(Charset.forName("Shift_JIS"));

      assertThrows(IllegalArgumentException.class, () -> parser.parse(csvBytes));
    }

    @Test
    @DisplayName(
        "[F-10] Given: 月が範囲外 2026/13/1 の CSV のとき, When: パースすると, Then: IllegalArgumentException"
            + " を投げる")
    void throwsWhenInvalidMonth() {
      String csv = "国民の祝日・休日月日,国民の祝日・休日名称\n2026/13/1,不正な月\n";
      byte[] csvBytes = csv.getBytes(Charset.forName("Shift_JIS"));

      assertThrows(IllegalArgumentException.class, () -> parser.parse(csvBytes));
    }

    @Test
    @DisplayName(
        "[F-10] Given: 日付形式が不正 abcd/1/1 の CSV のとき, When: パースすると, Then: IllegalArgumentException"
            + " を投げる")
    void throwsWhenInvalidDateFormat() {
      String csv = "国民の祝日・休日月日,国民の祝日・休日名称\nabcd/1/1,不正な形式\n";
      byte[] csvBytes = csv.getBytes(Charset.forName("Shift_JIS"));

      assertThrows(IllegalArgumentException.class, () -> parser.parse(csvBytes));
    }

    @Test
    @DisplayName(
        "[F-10] Given: 日付の部分が不足 2026/1 の CSV のとき, When: パースすると, Then: IllegalArgumentException"
            + " を投げる")
    void throwsWhenIncompleteDateFormat() {
      String csv = "国民の祝日・休日月日,国民の祝日・休日名称\n2026/1,不正な形式\n";
      byte[] csvBytes = csv.getBytes(Charset.forName("Shift_JIS"));

      assertThrows(IllegalArgumentException.class, () -> parser.parse(csvBytes));
    }
  }
}
