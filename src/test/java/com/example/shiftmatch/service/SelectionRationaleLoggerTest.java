package com.example.shiftmatch.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.DailyShiftResult;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.ShiftAssignment;
import com.example.shiftmatch.domain.ShiftSlot;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

@DisplayName("[7.2 節] 選定根拠ログ")
class SelectionRationaleLoggerTest {

  private SelectionRationaleLogger logger;
  private ListAppender<ILoggingEvent> listAppender;

  @BeforeEach
  void setUp() {
    logger = new SelectionRationaleLogger();

    // Logback の ListAppender でログをキャプチャ
    Logger logbackLogger = (Logger) LoggerFactory.getLogger(SelectionRationaleLogger.class);
    listAppender = new ListAppender<>();
    listAppender.start();
    logbackLogger.addAppender(listAppender);
    logbackLogger.setLevel(Level.INFO);
  }

  @Test
  @DisplayName("[7.2 節] 成立日の各ログ行に日付が含まれる")
  void testLogIncludesDateForSuccessfulDay() {
    LocalDate date = LocalDate.of(2024, 9, 2);

    // モックの AssignmentResult を作成
    ShiftAssignment mockAssignment = mock(ShiftAssignment.class);
    Employee mockEmployee = mock(Employee.class);
    when(mockEmployee.name()).thenReturn("Taro");
    when(mockEmployee.start()).thenReturn(LocalTime.of(7, 30));
    when(mockEmployee.end()).thenReturn(LocalTime.of(14, 30));
    when(mockEmployee.workableSlots()).thenReturn(List.of(ShiftSlot.SLOT_1));
    when(mockAssignment.employee()).thenReturn(mockEmployee);
    when(mockAssignment.slot()).thenReturn(ShiftSlot.SLOT_1);
    when(mockAssignment.gapMinutes()).thenReturn(0);

    AssignmentResult mockResult = mock(AssignmentResult.class);
    when(mockResult.assignments()).thenReturn(List.of(mockAssignment));
    when(mockResult.unassignedEmployees()).thenReturn(List.of());
    when(mockResult.gapMinutesList()).thenReturn(List.of(0));
    when(mockResult.score()).thenReturn(0);

    DailyShiftResult dailyResult = new DailyShiftResult(date, 8, Optional.of(mockResult));

    listAppender.list.clear();
    logger.log(date, dailyResult);

    // ログが出力されたことを確認
    assertTrue(listAppender.list.size() > 0, "ログが出力されるべき");

    // すべてのログ行に日付が含まれる
    String dateStr = "2024-09-02";
    for (ILoggingEvent event : listAppender.list) {
      assertTrue(
          event.getFormattedMessage().contains(dateStr),
          "ログ行に日付が含まれるべき: " + event.getFormattedMessage());
    }
  }

  @Test
  @DisplayName("[7.2 節] 不成立日に日付と勤務できる人数が含まれる")
  void testLogIncludesDateAndAvailableCountForUnsuccessfulDay() {
    LocalDate date = LocalDate.of(2024, 9, 3);
    int availableCount = 7;

    DailyShiftResult dailyResult = new DailyShiftResult(date, availableCount, Optional.empty());

    listAppender.list.clear();
    logger.log(date, dailyResult);

    // ログが出力されたことを確認
    assertTrue(listAppender.list.size() > 0, "ログが出力されるべき");

    String dateStr = "2024-09-03";
    String log = listAppender.list.get(0).getFormattedMessage();

    assertTrue(log.contains(dateStr), "ログに日付が含まれるべき: " + log);
    assertTrue(log.contains("7"), "ログに勤務できる人数が含まれるべき: " + log);
  }

  @Test
  @DisplayName("[7.2 節] 氏名の改行が \\n に変換され、偽のログ行が作られない")
  void testControlCharactersEscaped() {
    LocalDate date = LocalDate.of(2024, 9, 2);

    // 改行を含む氏名のモック
    ShiftAssignment mockAssignment = mock(ShiftAssignment.class);
    Employee mockEmployee = mock(Employee.class);
    when(mockEmployee.name()).thenReturn("Taro\nFake");
    when(mockEmployee.start()).thenReturn(LocalTime.of(7, 30));
    when(mockEmployee.end()).thenReturn(LocalTime.of(14, 30));
    when(mockEmployee.workableSlots()).thenReturn(List.of(ShiftSlot.SLOT_1));
    when(mockAssignment.employee()).thenReturn(mockEmployee);
    when(mockAssignment.slot()).thenReturn(ShiftSlot.SLOT_1);
    when(mockAssignment.gapMinutes()).thenReturn(0);

    AssignmentResult mockResult = mock(AssignmentResult.class);
    when(mockResult.assignments()).thenReturn(List.of(mockAssignment));
    when(mockResult.unassignedEmployees()).thenReturn(List.of());
    when(mockResult.gapMinutesList()).thenReturn(List.of(0));
    when(mockResult.score()).thenReturn(0);

    DailyShiftResult dailyResult = new DailyShiftResult(date, 8, Optional.of(mockResult));

    listAppender.list.clear();
    logger.log(date, dailyResult);

    // 改行がエスケープされていることを確認
    String log = listAppender.list.get(0).getFormattedMessage();
    assertTrue(log.contains("Taro\\nFake"), "改行がエスケープ表記で含まれるべき: " + log);
  }

  @Test
  @DisplayName("[7.2 節] 異なる 2 日のログが日付で区別できる")
  void testDifferentDatesDistinguishable() {
    LocalDate date1 = LocalDate.of(2024, 9, 2);
    LocalDate date2 = LocalDate.of(2024, 9, 3);

    // モックの AssignmentResult を作成
    ShiftAssignment mockAssignment = mock(ShiftAssignment.class);
    Employee mockEmployee = mock(Employee.class);
    when(mockEmployee.name()).thenReturn("Taro");
    when(mockEmployee.start()).thenReturn(LocalTime.of(7, 30));
    when(mockEmployee.end()).thenReturn(LocalTime.of(14, 30));
    when(mockEmployee.workableSlots()).thenReturn(List.of(ShiftSlot.SLOT_1));
    when(mockAssignment.employee()).thenReturn(mockEmployee);
    when(mockAssignment.slot()).thenReturn(ShiftSlot.SLOT_1);
    when(mockAssignment.gapMinutes()).thenReturn(0);

    AssignmentResult mockResult = mock(AssignmentResult.class);
    when(mockResult.assignments()).thenReturn(List.of(mockAssignment));
    when(mockResult.unassignedEmployees()).thenReturn(List.of());
    when(mockResult.gapMinutesList()).thenReturn(List.of(0));
    when(mockResult.score()).thenReturn(0);

    DailyShiftResult dailyResult1 = new DailyShiftResult(date1, 8, Optional.of(mockResult));
    DailyShiftResult dailyResult2 = new DailyShiftResult(date2, 8, Optional.of(mockResult));

    listAppender.list.clear();
    logger.log(date1, dailyResult1);
    int logCountAfterDay1 = listAppender.list.size();

    logger.log(date2, dailyResult2);

    // 日付 1 のログと日付 2 のログが区別できることを確認
    String dateStr1 = "2024-09-02";
    String dateStr2 = "2024-09-03";

    boolean hasDate1 = false;
    boolean hasDate2 = false;

    for (int i = 0; i < logCountAfterDay1; i++) {
      if (listAppender.list.get(i).getFormattedMessage().contains(dateStr1)) {
        hasDate1 = true;
      }
    }

    for (int i = logCountAfterDay1; i < listAppender.list.size(); i++) {
      if (listAppender.list.get(i).getFormattedMessage().contains(dateStr2)) {
        hasDate2 = true;
      }
    }

    assertTrue(hasDate1, "日付 1 のログが含まれるべき");
    assertTrue(hasDate2, "日付 2 のログが含まれるべき");
  }
}
