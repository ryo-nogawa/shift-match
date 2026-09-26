package com.example.shiftmatch.service;

import com.example.shiftmatch.domain.Holiday;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 祝日 CSV をパースするクラス。
 *
 * <p>内閣府の「国民の祝日」CSV を解析し、祝日データのリストを返します。
 */
public class HolidayCsvParser {

  private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}/\\d{1,2}/\\d{1,2}");
  private static final String EXPECTED_HEADER = "国民の祝日・休日月日,国民の祝日・休日名称";
  private static final int MAX_HOLIDAY_NAME_LENGTH = 64;

  /**
   * Shift_JIS でエンコードされた祝日 CSV をパースします。
   *
   * <p>1 行目の見出し行は読み飛ばします。2 行目以降の各行は、日付（yyyy/M/d 形式）と祝日名をカンマで
   * 区切ったフォーマットとして解析します。空行は無視されます。
   *
   * <p>形式が不正な行（日付が読めない、列が足りない）がある場合は、{@link IllegalArgumentException}
   * を投げます。
   *
   * @param csv Shift_JIS のバイト列
   * @return パースされた祝日のリスト
   * @throws IllegalArgumentException 形式が不正な行がある場合
   */
  public List<Holiday> parse(byte[] csv) {
    if (csv.length == 0) {
      throw new IllegalArgumentException("CSV が空です");
    }

    String csvString = new String(csv, Charset.forName("Shift_JIS"));
    String[] lines = csvString.split("\n");

    if (lines.length == 0) {
      throw new IllegalArgumentException("CSV が空です");
    }

    String headerLine = lines[0].trim();
    String[] headerParts = headerLine.split(",");
    String[] expectedParts = EXPECTED_HEADER.split(",");
    if (headerParts.length != expectedParts.length
        || !headerParts[0].trim().equals(expectedParts[0].trim())
        || !headerParts[1].trim().equals(expectedParts[1].trim())) {
      throw new IllegalArgumentException("見出し行が不正です: " + headerLine);
    }

    List<Holiday> holidays = new ArrayList<>();
    Set<LocalDate> seenDates = new HashSet<>();

    for (int i = 1; i < lines.length; i++) {
      String line = lines[i].trim();

      if (line.isEmpty()) {
        continue;
      }

      String[] parts = line.split(",");
      if (parts.length < 2) {
        throw new IllegalArgumentException("列が不足しています: " + line);
      }

      String dateStr = parts[0];

      if (!DATE_PATTERN.matcher(dateStr).matches()) {
        throw new IllegalArgumentException("日付形式が不正です: " + dateStr);
      }

      String[] dateParts = dateStr.split("/");
      int year = Integer.parseInt(dateParts[0]);
      int month = Integer.parseInt(dateParts[1]);
      int day = Integer.parseInt(dateParts[2]);

      if (month < 1 || month > 12) {
        throw new IllegalArgumentException("日付が読めません: " + dateStr);
      }

      if (!YearMonth.of(year, month).isValidDay(day)) {
        throw new IllegalArgumentException("日付が読めません: " + dateStr);
      }

      LocalDate date = LocalDate.of(year, month, day);

      if (seenDates.contains(date)) {
        throw new IllegalArgumentException("同じ日付が複数回出現しています: " + dateStr);
      }
      seenDates.add(date);

      String name = parts[1];
      if (name.isEmpty()) {
        throw new IllegalArgumentException("祝日名が空です: " + dateStr);
      }
      if (name.length() > MAX_HOLIDAY_NAME_LENGTH) {
        throw new IllegalArgumentException(
            "祝日名が 64 文字を超えています: " + dateStr + " (" + name.length() + " 文字)");
      }

      holidays.add(new Holiday(date, name));
    }

    if (holidays.isEmpty()) {
      throw new IllegalArgumentException("有効な祝日が 1 件もありません");
    }

    return holidays;
  }
}
