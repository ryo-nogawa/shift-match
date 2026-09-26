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

  @Nested
  @DisplayName("[F-10] CSV の完全性を検証する")
  class ValidateCsvCompleteness {

    @Test
    @DisplayName("[F-10] Given: 空のバイト列が与えられたとき, When: パースすると, Then: IllegalArgumentException を投げる")
    void throwsWhenCsvIsEmpty() {
      byte[] csvBytes = new byte[0];

      assertThrows(IllegalArgumentException.class, () -> parser.parse(csvBytes));
    }

    @Test
    @DisplayName(
        "[F-10] Given: 見出しだけで有効な祝日がない CSV のとき, When: パースすると, Then: IllegalArgumentException を投げる")
    void throwsWhenNoValidHolidaysAfterHeader() {
      String csv = "国民の祝日・休日月日,国民の祝日・休日名称\n";
      byte[] csvBytes = csv.getBytes(Charset.forName("Shift_JIS"));

      assertThrows(IllegalArgumentException.class, () -> parser.parse(csvBytes));
    }

    @Test
    @DisplayName("[F-10] Given: 見出しが異なる CSV のとき, When: パースすると, Then: IllegalArgumentException を投げる")
    void throwsWhenHeaderIsDifferent() {
      String csv = "日付,祝日名\n2026/1/1,元日\n";
      byte[] csvBytes = csv.getBytes(Charset.forName("Shift_JIS"));

      assertThrows(IllegalArgumentException.class, () -> parser.parse(csvBytes));
    }

    @Test
    @DisplayName(
        "[F-10] Given: 同じ日付が 2 回以上出現する CSV のとき, When: パースすると, Then: IllegalArgumentException を投げる")
    void throwsWhenDuplicateDates() {
      String csv = "国民の祝日・休日月日,国民の祝日・休日名称\n2026/1/1,元日\n2026/1/1,元日（重複）\n";
      byte[] csvBytes = csv.getBytes(Charset.forName("Shift_JIS"));

      assertThrows(IllegalArgumentException.class, () -> parser.parse(csvBytes));
    }

    @Test
    @DisplayName(
        "[F-10] Given: 祝日名が空の行がある CSV のとき, When: パースすると, Then: IllegalArgumentException を投げる")
    void throwsWhenHolidayNameIsEmpty() {
      String csv = "国民の祝日・休日月日,国民の祝日・休日名称\n2026/1/1,\n";
      byte[] csvBytes = csv.getBytes(Charset.forName("Shift_JIS"));

      assertThrows(IllegalArgumentException.class, () -> parser.parse(csvBytes));
    }

    @Test
    @DisplayName(
        "[F-10] Given: 祝日名が 65 文字の CSV のとき, When: パースすると, Then: IllegalArgumentException を投げる")
    void throwsWhenHolidayNameExceeds64Chars() {
      String name = "a".repeat(65);
      String csv = "国民の祝日・休日月日,国民の祝日・休日名称\n2026/1/1," + name + "\n";
      byte[] csvBytes = csv.getBytes(Charset.forName("Shift_JIS"));

      assertThrows(IllegalArgumentException.class, () -> parser.parse(csvBytes));
    }

    @Test
    @DisplayName("[F-10] Given: 見出しの前後に空白を含む CSV のとき, When: パースすると, Then: 見出しは正規化されて受け入れられる")
    void acceptsHeaderWithWhitespace() {
      String csv = "  国民の祝日・休日月日  ,  国民の祝日・休日名称  \n2026/1/1,元日\n";
      byte[] csvBytes = csv.getBytes(Charset.forName("Shift_JIS"));

      List<Holiday> result = parser.parse(csvBytes);

      assertEquals(1, result.size());
      assertEquals(LocalDate.of(2026, 1, 1), result.get(0).date());
    }

    @Test
    @DisplayName("[F-10] Given: 行末が \\r\\n の CSV のとき, When: パースすると, Then: 正常に読める")
    void parsesWithCrlfLineEndings() {
      String csv = "国民の祝日・休日月日,国民の祝日・休日名称\r\n2026/1/1,元日\r\n2026/10/12,スポーツの日\r\n";
      byte[] csvBytes = csv.getBytes(Charset.forName("Shift_JIS"));

      List<Holiday> result = parser.parse(csvBytes);

      assertEquals(2, result.size());
      assertEquals(LocalDate.of(2026, 1, 1), result.get(0).date());
      assertEquals(LocalDate.of(2026, 10, 12), result.get(1).date());
    }
  }
}
