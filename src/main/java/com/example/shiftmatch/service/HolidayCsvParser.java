package com.example.shiftmatch.service;

import com.example.shiftmatch.domain.Holiday;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 祝日 CSV をパースするクラス。
 *
 * <p>内閣府の「国民の祝日」CSV を解析し、祝日データのリストを返します。
 */
public class HolidayCsvParser {

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
    String csvString = new String(csv, Charset.forName("Shift_JIS"));
    String[] lines = csvString.split("\n");
    List<Holiday> holidays = new ArrayList<>();

    for (int i = 1; i < lines.length; i++) {
      String line = lines[i].trim();

      // 空行をスキップ
      if (line.isEmpty()) {
        continue;
      }

      // カンマで分割
      String[] parts = line.split(",");
      if (parts.length < 2) {
        throw new IllegalArgumentException("列が不足しています: " + line);
      }

      // 日付をパース
      String dateStr = parts[0];
      LocalDate date;
      try {
        // yyyy/M/d 形式をパース
        String[] dateParts = dateStr.split("/");
        if (dateParts.length != 3) {
          throw new IllegalArgumentException("日付形式が不正です: " + dateStr);
        }
        int year = Integer.parseInt(dateParts[0]);
        int month = Integer.parseInt(dateParts[1]);
        int day = Integer.parseInt(dateParts[2]);
        date = LocalDate.of(year, month, day);
      } catch (Exception e) {
        throw new IllegalArgumentException("日付が読めません: " + dateStr, e);
      }

      String name = parts[1];
      holidays.add(new Holiday(date, name));
    }

    return holidays;
  }
}
