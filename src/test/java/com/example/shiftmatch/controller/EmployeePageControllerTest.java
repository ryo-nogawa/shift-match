package com.example.shiftmatch.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.DailyShiftResult;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.HolidayDataUnavailableError;
import com.example.shiftmatch.domain.MonthEmployee;
import com.example.shiftmatch.domain.MonthlyShiftResult;
import com.example.shiftmatch.domain.ShiftAssignment;
import com.example.shiftmatch.domain.ShiftSlot;
import com.example.shiftmatch.persistence.SavedMonthlyShift;
import com.example.shiftmatch.service.HolidayService;
import com.example.shiftmatch.service.ShiftStorageService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

@WebMvcTest(EmployeePageController.class)
@Import(EmployeePageViewFactory.class)
class EmployeePageControllerTest {

  private static final YearMonth MONTH = YearMonth.of(2026, 10);

  private static final List<ShiftSlot> SLOTS_IN_ORDER =
      List.of(
          ShiftSlot.SLOT_1,
          ShiftSlot.SLOT_1,
          ShiftSlot.SLOT_2,
          ShiftSlot.SLOT_3,
          ShiftSlot.SLOT_4,
          ShiftSlot.SLOT_5,
          ShiftSlot.SLOT_6,
          ShiftSlot.SLOT_6);

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ShiftStorageService shiftStorageService;

  @MockitoBean private HolidayService holidayService;

  /** 山田が 10/1 に枠 1 で勤務し、10/6 が不成立の月を保存済みにします。 */
  @BeforeEach
  void stubSavedShift() {
    List<ShiftAssignment> assignments = new ArrayList<>();
    for (int i = 0; i < SLOTS_IN_ORDER.size(); i++) {
      assignments.add(
          new ShiftAssignment(
              Employee.working(
                  i == 0 ? "山田" : "e" + (i + 1), LocalTime.of(7, 30), LocalTime.of(18, 30)),
              SLOTS_IN_ORDER.get(i),
              LocalTime.of(12, 0),
              LocalTime.of(12, 45)));
    }
    MonthlyShiftResult result =
        new MonthlyShiftResult(
            MONTH,
            List.of(
                new DailyShiftResult(
                    LocalDate.of(2026, 10, 1),
                    8,
                    Optional.of(new AssignmentResult(assignments, 0, List.of()))),
                new DailyShiftResult(LocalDate.of(2026, 10, 6), 5, Optional.empty())));
    when(shiftStorageService.load(MONTH))
        .thenReturn(
            Optional.of(
                new SavedMonthlyShift(
                    result, List.of(new MonthEmployee("山田", EmploymentType.PART_TIME)))));
    when(holidayService.holidaysOf(MONTH)).thenReturn(Map.of(LocalDate.of(2026, 10, 12), "スポーツの日"));
  }

  @Nested
  class 正常系 {

    @Test
    @DisplayName(
        "[7.3] Given: 保存済みのシフト, When: GET /employee, Then: 200 で氏名・区分・対象月・勤務時間・合計・戻るリンク・注記を表示する")
    void showsEmployeePage() throws Exception {
      mockMvc
          .perform(get("/employee").param("month", "2026-10").param("name", "山田"))
          .andExpect(status().isOk())
          .andExpect(view().name("employee"))
          .andExpect(bodyContains("山田"))
          .andExpect(bodyContains("パート"))
          .andExpect(bodyContains("2026年10月"))
          .andExpect(bodyContains("7:30–14:30"))
          .andExpect(bodyContains("(06:15)"))
          .andExpect(bodyContains("スポーツの日"))
          .andExpect(bodyContains("結果画面へ戻る"))
          .andExpect(bodyContains(BACK_HREF))
          .andExpect(bodyNotContains("シフト作成画面へ戻る"))
          .andExpect(bodyContains("不成立の日が 1 日あります（この日は 0 時間として集計しています）"));
    }

    @Test
    @DisplayName("[8.3.1] Given: 保存済みのシフト, When: GET /employee, Then: 個別ページ用の CSS を読み込む")
    void loadsEmployeePageStylesheet() throws Exception {
      mockMvc
          .perform(get("/employee").param("month", "2026-10").param("name", "山田"))
          .andExpect(bodyContains("href=\"/css/employee-page.css\""));
    }

    @Test
    @DisplayName("[7.3] Given: 祝日データが取得できない, When: GET /employee, Then: 200 で表示する")
    void showsPageWhenHolidayDataUnavailable() throws Exception {
      when(holidayService.holidaysOf(any(YearMonth.class)))
          .thenThrow(new HolidayDataUnavailableError(MONTH));

      mockMvc
          .perform(get("/employee").param("month", "2026-10").param("name", "山田"))
          .andExpect(status().isOk())
          .andExpect(view().name("employee"))
          .andExpect(bodyContains("7:30–14:30"));
    }
  }

  @Nested
  class 異常系 {

    @Test
    @DisplayName("[8.3.1] Given: month が YYYY-MM 形式でない, When: GET /employee, Then: 400")
    void rejectsMalformedMonth() throws Exception {
      mockMvc
          .perform(get("/employee").param("month", "2026/10").param("name", "山田"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("[8.3.1] Given: month がない, When: GET /employee, Then: 400")
    void rejectsMissingMonth() throws Exception {
      mockMvc.perform(get("/employee").param("name", "山田")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("[8.3.1] Given: 祝日データの収録範囲外の month, When: GET /employee, Then: 400")
    void rejectsMonthOutOfHolidayDataRange() throws Exception {
      mockMvc
          .perform(get("/employee").param("month", "1900-01").param("name", "山田"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("[8.3.1] Given: 保存済みシフトがない月, When: GET /employee, Then: 200 で未作成の文言を表示する")
    void showsNotCreatedMessageWhenNoSavedShift() throws Exception {
      when(shiftStorageService.load(YearMonth.of(2026, 11))).thenReturn(Optional.empty());

      mockMvc
          .perform(get("/employee").param("month", "2026-11").param("name", "山田"))
          .andExpect(status().isOk())
          .andExpect(view().name("employee"))
          .andExpect(bodyContains("この月のシフトはまだ作成されていません"))
          .andExpect(bodyContains("結果画面へ戻る"))
          .andExpect(bodyContains("href=\"/?month=2026-11&amp;step=3\""))
          .andExpect(bodyNotContains("シフト作成画面へ戻る"));
    }

    @Test
    @DisplayName("[8.3.1] Given: name がない, When: GET /employee, Then: 404 で該当なしの文言を表示する")
    void showsNotFoundWhenNameMissing() throws Exception {
      mockMvc
          .perform(get("/employee").param("month", "2026-10"))
          .andExpect(status().isNotFound())
          .andExpect(view().name("employee"))
          .andExpect(bodyContains("この月のシフトに該当する従業員はいません"));
    }

    @Test
    @DisplayName("[8.3.1] Given: name が空, When: GET /employee, Then: 404 で該当なしの文言を表示する")
    void showsNotFoundWhenNameEmpty() throws Exception {
      mockMvc
          .perform(get("/employee").param("month", "2026-10").param("name", ""))
          .andExpect(status().isNotFound())
          .andExpect(bodyContains("この月のシフトに該当する従業員はいません"));
    }

    @Test
    @DisplayName("[8.3.1] Given: 保存済みにいない name, When: GET /employee, Then: 404 で該当なしの文言を表示する")
    void showsNotFoundWhenNameUnknown() throws Exception {
      mockMvc
          .perform(get("/employee").param("month", "2026-10").param("name", "存在しない人"))
          .andExpect(status().isNotFound())
          .andExpect(view().name("employee"))
          .andExpect(bodyContains("この月のシフトに該当する従業員はいません"))
          .andExpect(bodyContains("結果画面へ戻る"))
          .andExpect(bodyContains(BACK_HREF))
          .andExpect(bodyNotContains("シフト作成画面へ戻る"));
    }
  }

  private static final String BACK_HREF = "href=\"/?month=2026-10&amp;step=3\"";

  private static ResultMatcher bodyNotContains(String unexpected) {
    return result -> {
      String html = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
      assertFalse(html.contains(unexpected), () -> "本文に含まれてはならない: " + unexpected);
    };
  }

  /**
   * レスポンス本文（UTF-8）に指定した文字列が含まれることを検証する {@link ResultMatcher} を返します。
   *
   * @param expected 含まれるべき文字列
   * @return 検証用の {@link ResultMatcher}
   */
  private static ResultMatcher bodyContains(String expected) {
    return result -> {
      String html = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
      assertTrue(html.contains(expected), () -> "本文に含まれない: " + expected);
    };
  }
}
