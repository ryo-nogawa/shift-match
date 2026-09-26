package com.example.shiftmatch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.DailyShiftResult;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.InputError;
import com.example.shiftmatch.domain.InvalidMonthlyInputException;
import com.example.shiftmatch.domain.MonthlyShiftInput;
import com.example.shiftmatch.domain.MonthlyShiftResult;
import com.example.shiftmatch.service.MonthlyShiftService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * POST /shift の入力の受け渡しと、結果・エラーの描画のテスト。
 *
 * <p>変換は実物の {@link MonthlyFormConverter} を使い、算出だけをモックにします。
 */
@WebMvcTest(ShiftController.class)
@Import(MonthlyFormConverter.class)
@DisplayName("ShiftController POST /shift")
class ShiftControllerPostTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private MonthlyShiftService monthlyShiftService;

  private MockHttpServletRequestBuilder validRequest() {
    return post("/shift")
        .param("targetMonth", "2026-10")
        .param("employees[0].name", "A")
        .param("employees[0].employmentType", "PART_TIME")
        .param("employees[0].days[0].start", "08:00")
        .param("employees[0].days[0].end", "17:00")
        .param("employees[0].days[1].off", "true")
        .param("adjustments[0].date", "2026-10-20")
        .param("adjustments[0].employeeName", "A")
        .param("adjustments[0].off", "true");
  }

  private String bodyOf(MockHttpServletRequestBuilder request) throws Exception {
    return mockMvc
        .perform(request)
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  @Test
  @DisplayName("[F-3] Given: 入力が送信された, When: POST すると, Then: フォームの内容が月間入力として create に渡る")
  void passesFormContentToService() throws Exception {
    when(monthlyShiftService.create(any()))
        .thenReturn(new MonthlyShiftResult(YearMonth.of(2026, 10), List.of()));

    bodyOf(validRequest());

    ArgumentCaptor<MonthlyShiftInput> captor = ArgumentCaptor.forClass(MonthlyShiftInput.class);
    verify(monthlyShiftService).create(captor.capture());
    MonthlyShiftInput input = captor.getValue();
    assertEquals(YearMonth.of(2026, 10), input.month());
    assertEquals("A", input.employees().get(0).name());
    assertEquals(EmploymentType.PART_TIME, input.employees().get(0).employmentType());
    assertEquals(
        LocalTime.of(8, 0), input.employees().get(0).baseShifts().get(DayOfWeek.MONDAY).start());
    assertTrue(input.employees().get(0).baseShifts().get(DayOfWeek.TUESDAY).off());
    assertEquals(LocalDate.of(2026, 10, 20), input.adjustments().get(0).date());
    assertTrue(input.adjustments().get(0).wish().off());
  }

  @Test
  @DisplayName("[F-5] Given: 成立の日と不成立の日がある, When: POST すると, Then: 画面 3 に日付・成立可否・人数が出る")
  void rendersMonthlyResultOnScreen3() throws Exception {
    DailyShiftResult feasible =
        new DailyShiftResult(
            LocalDate.of(2026, 10, 1), 9, Optional.of(mock(AssignmentResult.class)));
    DailyShiftResult infeasible =
        new DailyShiftResult(LocalDate.of(2026, 10, 2), 5, Optional.empty());
    when(monthlyShiftService.create(any()))
        .thenReturn(new MonthlyShiftResult(YearMonth.of(2026, 10), List.of(feasible, infeasible)));

    String html = bodyOf(validRequest());

    assertTrue(html.contains("data-initial-step=\"3\""));
    assertTrue(html.contains("2026-10-01"));
    assertTrue(html.contains("2026-10-02"));
    assertTrue(html.contains(">不成立<"));
    assertTrue(html.contains(">成立<"));
    assertTrue(html.contains("勤務できる人数: 5"));
    assertFalse(html.contains("role=\"alert\""));
  }

  @Test
  @DisplayName("[V-3] Given: 入力エラーがある, When: POST すると, Then: 画面 1 に全エラーを集約し、入力を保持する")
  void rendersInputErrorsOnScreen1() throws Exception {
    when(monthlyShiftService.create(any()))
        .thenThrow(
            new InvalidMonthlyInputException(
                List.of(
                    new InputError("V-2", "氏名「A」が重複しています"),
                    new InputError("V-3", "Aの月曜の時間帯が不正です"))));

    String html = bodyOf(validRequest());

    assertTrue(html.contains("data-initial-step=\"1\""));
    assertTrue(html.contains("role=\"alert\""));
    assertTrue(html.contains("V-2") && html.contains("氏名「A」が重複しています"));
    assertTrue(html.contains("V-3") && html.contains("Aの月曜の時間帯が不正です"));
    assertTrue(html.indexOf("role=\"alert\"") < html.indexOf("id=\"screen-2\""));
    assertTrue(html.contains("value=\"A\""));
    assertFalse(html.contains("シフト作成結果"));
  }

  @Test
  @DisplayName("[F-11] Given: 個別変更が送信された, When: エラーで戻ると, Then: 個別変更が hidden 入力に復元される")
  void restoresAdjustmentsOnError() throws Exception {
    when(monthlyShiftService.create(any()))
        .thenThrow(new InvalidMonthlyInputException(List.of(new InputError("V-3", "エラー"))));

    String html = bodyOf(validRequest());

    assertTrue(html.contains("name=\"adjustments[0].date\""));
    assertTrue(html.contains("2026-10-20"));
    assertTrue(html.contains("name=\"adjustments[0].off\""));
  }
}
