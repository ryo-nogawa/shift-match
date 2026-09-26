package com.example.shiftmatch.service;

import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.DailyShiftResult;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.ShiftAssignment;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 営業日ごとの選定根拠ログを出力します。
 *
 * <p>成立日には各人の割当・希望・差・入れる枠を出力し、不成立日には日付と勤務できる人数を出力します。
 */
@Component
public class SelectionRationaleLogger {

  private static final Logger LOGGER = LoggerFactory.getLogger(SelectionRationaleLogger.class);
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");

  /**
   * 営業日ごとの選定根拠をログ出力します。
   *
   * @param date 営業日
   * @param result その日のシフト割り当て結果
   */
  public void log(LocalDate date, DailyShiftResult result) {
    String dateStr = date.format(DATE_FORMATTER);

    if (result.assignment().isPresent()) {
      // 成立日：割り当て結果をログ出力
      AssignmentResult assignment = result.assignment().get();
      logSuccessfulDay(dateStr, assignment);
    } else {
      // 不成立日：日付と勤務できる人数をログ出力
      LOGGER.info("日付={} 勤務できる人数={}", dateStr, result.availableCount());
    }
  }

  private void logSuccessfulDay(String dateStr, AssignmentResult result) {
    // 割り当てられた従業員のログ
    for (ShiftAssignment assignment : result.assignments()) {
      Employee employee = assignment.employee();
      LOGGER.info(
          "日付={} 割当 {} 希望={}〜{} 割当={} 差={}分 入れる枠={}",
          dateStr,
          escapeControlCharacters(employee.name()),
          employee.start().format(TIME_FORMATTER),
          employee.end().format(TIME_FORMATTER),
          formatSlot(assignment.slot()),
          assignment.gapMinutes(),
          formatSlots(employee.workableSlots()));
    }

    // 未出勤の従業員のログ
    for (Employee employee : result.unassignedEmployees()) {
      LOGGER.info(
          "日付={} 未出勤 {} 理由={} 入れる枠={}",
          dateStr,
          escapeControlCharacters(employee.name()),
          escapeControlCharacters(result.unassignedReasonLabel(employee)),
          formatSlots(employee.workableSlots()));
    }

    // スコア合計
    LOGGER.info("日付={} 合計 = {} = {} 分", dateStr, join(result.gapMinutesList()), result.score());
  }

  /**
   * ログ出力用に、改行などの制御文字を可視文字列へ変換します。
   *
   * <p>氏名は利用者が入力した任意の文字列のため、そのまま出力すると偽のログ行を挿入できてしまいます。
   * 画面や保存する値は変えず、ログへ渡す直前にだけ変換します。
   *
   * @param value 変換前の文字列
   * @return 制御文字を 改行は {@code \r}・{@code \n}、その他は 16 進数 4 桁のエスケープ表記に変換した文字列
   */
  private String escapeControlCharacters(String value) {
    StringBuilder escaped = new StringBuilder();
    for (char c : value.toCharArray()) {
      if (c == '\r') {
        escaped.append("\\r");
      } else if (c == '\n') {
        escaped.append("\\n");
      } else if (Character.isISOControl(c)) {
        escaped.append(String.format("\\u%04x", (int) c));
      } else {
        escaped.append(c);
      }
    }
    return escaped.toString();
  }

  private String join(List<Integer> values) {
    return values.stream().map(value -> String.valueOf(value)).collect(Collectors.joining(" + "));
  }

  private String formatSlots(List<com.example.shiftmatch.domain.ShiftSlot> slots) {
    return slots.stream()
        .map(slot -> this.formatSlot(slot))
        .collect(Collectors.joining(", ", "[", "]"));
  }

  private String formatSlot(com.example.shiftmatch.domain.ShiftSlot slot) {
    return slot.startTime().format(TIME_FORMATTER) + "〜" + slot.endTime().format(TIME_FORMATTER);
  }
}
