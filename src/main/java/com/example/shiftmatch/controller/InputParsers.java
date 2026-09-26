package com.example.shiftmatch.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * フォームの文字列を年月・日付・時刻へ変換します。
 */
final class InputParsers {

  private static final Pattern YEAR_MONTH = Pattern.compile("(\\d{4})-(0[1-9]|1[0-2])");

  private static final Pattern DATE = Pattern.compile("(\\d{4})-(0[1-9]|1[0-2])-(\\d{2})");

  private static final Pattern TIME = Pattern.compile("([01]\\d|2[0-3]):([0-5]\\d)");

  private InputParsers() {}

  /**
   * {@code YYYY-MM} 形式の文字列を年月へ変換します。
   *
   * @param text 文字列
   * @return 年月。形式や値が不正なときは空
   */
  static Optional<YearMonth> parseYearMonth(String text) {
    if (text == null) {
      return Optional.empty();
    }
    Matcher matcher = YEAR_MONTH.matcher(text);
    if (!matcher.matches()) {
      return Optional.empty();
    }
    return Optional.of(
        YearMonth.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))));
  }

  /**
   * {@code YYYY-MM-DD} 形式の文字列を日付へ変換します。
   *
   * @param text 文字列
   * @return 日付。形式や値（存在しない日付を含む）が不正なときは空
   */
  static Optional<LocalDate> parseDate(String text) {
    if (text == null) {
      return Optional.empty();
    }
    Matcher matcher = DATE.matcher(text);
    if (!matcher.matches()) {
      return Optional.empty();
    }
    YearMonth month =
        YearMonth.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
    int day = Integer.parseInt(matcher.group(3));
    if (!month.isValidDay(day)) {
      return Optional.empty();
    }
    return Optional.of(month.atDay(day));
  }

  /**
   * {@code HH:mm} 形式の文字列を時刻へ変換します。
   *
   * @param text 文字列
   * @return 時刻。形式や値が不正なときは空
   */
  static Optional<LocalTime> parseTime(String text) {
    if (text == null) {
      return Optional.empty();
    }
    Matcher matcher = TIME.matcher(text);
    if (!matcher.matches()) {
      return Optional.empty();
    }
    return Optional.of(
        LocalTime.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))));
  }
}
