package com.example.shiftmatch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.DailyShiftResult;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.InputError;
import com.example.shiftmatch.domain.InvalidMonthlyInputException;
import com.example.shiftmatch.domain.MonthlyShiftInput;
import com.example.shiftmatch.domain.MonthlyShiftResult;
import com.example.shiftmatch.service.HolidayService;
import com.example.shiftmatch.service.MonthlyShiftService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** 変換は実物の {@link MonthlyFormConverter} を使い、算出（{@link MonthlyShiftService}）だけをモックにします。 */
@WebMvcTest(ShiftController.class)
@Import({MonthlyFormConverter.class, MonthlyResultViewFactory.class})
class ShiftControllerTest {

  /** 廃止した枠ごとの 3 段階の希望入力の名残を検出する語（ソース検索で誤検出しないよう分割）。 */
  private static final String LEGACY_TOKEN = "wi" + "sh";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private MonthlyShiftService monthlyShiftService;

  @MockitoBean private HolidayService holidayService;

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

  private MvcResult perform(MockHttpServletRequestBuilder request) throws Exception {
    return mockMvc.perform(request).andExpect(status().isOk()).andReturn();
  }

  private static String bodyOf(MvcResult result) throws Exception {
    return result.getResponse().getContentAsString();
  }

  private static Map<String, Object> modelOf(MvcResult result) {
    return result.getModelAndView().getModel();
  }

  private void throwInputErrors(InputError... errors) {
    when(monthlyShiftService.create(any()))
        .thenThrow(new InvalidMonthlyInputException(List.of(errors)));
  }

  @Nested
  class 正常系 {

    @Test
    @DisplayName("[F-1] Given: 初めて画面を開くとき, When: GET / を呼ぶと, Then: 200 で index ビューを返す")
    void returnsIndexViewOnGet() throws Exception {
      mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("index"));
    }

    @Test
    @DisplayName(
        "[F-1][F-2] Given: 初めて画面を開くとき, When: GET / を呼ぶと,"
            + " Then: 対象月が今月で、従業員 12 行が常勤・休みなし 07:30〜18:30 になる")
    void preparesDefaultFormOnGet() throws Exception {
      MvcResult result = perform(get("/"));

      ShiftForm form = (ShiftForm) modelOf(result).get("shiftForm");
      assertNotNull(form);
      assertEquals(
          YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM")), form.getTargetMonth());
      assertEquals(12, form.getEmployees().size());
      for (EmployeeForm employee : form.getEmployees()) {
        assertEquals("FULL_TIME", employee.getEmploymentType());
        assertEquals(5, employee.getDays().size());
        for (DayForm day : employee.getDays()) {
          assertFalse(day.isOff());
          assertEquals("07:30", day.getStart());
          assertEquals("18:30", day.getEnd());
        }
      }
      assertEquals(1, modelOf(result).get("initialStep"));
    }

    @Test
    @DisplayName(
        "[F-1][F-9] Given: 初めて画面を開くとき, When: GET / を呼ぶと," + " Then: 時刻の選択肢 23 件と区分の選択肢 3 件がモデルにある")
    void providesOptionsOnGet() throws Exception {
      MvcResult result = perform(get("/"));

      assertEquals(23, ((List<?>) modelOf(result).get("timeOptions")).size());
      assertEquals("07:30", ((List<?>) modelOf(result).get("timeOptions")).get(0));
      assertEquals("18:30", ((List<?>) modelOf(result).get("timeOptions")).get(22));
      assertEquals(3, ((List<?>) modelOf(result).get("employmentTypes")).size());
    }

    @Test
    @DisplayName(
        "[F-1][F-2][F-6][F-8] Given: 初めて画面を開くとき, When: GET / の HTML を見ると,"
            + " Then: 12 行分の入力・並べ替え・削除ボタンと基本シフトパネルがあり、13 行目はない")
    void rendersTwelveRowsOnGet() throws Exception {
      String html = bodyOf(perform(get("/")));

      assertTrue(html.contains("name=\"targetMonth\""));
      assertTrue(html.contains("name=\"employees[0].name\""));
      assertTrue(html.contains("name=\"employees[11].name\""));
      assertTrue(html.contains("name=\"employees[11].employmentType\""));
      assertTrue(html.contains("name=\"employees[0].days[0].off\""));
      assertTrue(html.contains("name=\"employees[0].days[0].start\""));
      assertTrue(html.contains("name=\"employees[11].days[4].end\""));
      assertFalse(html.contains("name=\"employees[12].name\""));
      assertTrue(html.contains("id=\"base-panels\""));
      assertEquals(12, html.split("class=\"move-up-btn\"", -1).length - 1);
      assertEquals(12, html.split("class=\"move-down-btn\"", -1).length - 1);
      assertEquals(12, html.split("class=\"delete-btn\"", -1).length - 1);
    }

    @Test
    @DisplayName(
        "[F-9][F-11] Given: 初めて画面を開くとき, When: GET / の HTML を見ると,"
            + " Then: 3 画面・ステップ操作・対象月の切り替え・カレンダーの置き場がある")
    void rendersScreensAndNavigationOnGet() throws Exception {
      String html = bodyOf(perform(get("/")));

      for (String id :
          List.of(
              "screen-1",
              "screen-2",
              "screen-3",
              "prev-btn",
              "next-btn",
              "prev-month-btn",
              "next-month-btn",
              "month-summary",
              "calendar",
              "day-panel",
              "adjustment-inputs")) {
        assertTrue(html.contains("id=\"" + id + "\""), id);
      }
      assertTrue(html.contains("data-step=\"1\""));
      assertTrue(html.contains("data-step=\"2\""));
      assertTrue(html.contains("data-step=\"3\""));
      assertFalse(html.contains("role=\"alert\""));
    }

    @Test
    @DisplayName("[F-1] Given: 初めて画面を開くとき, When: GET / の HTML を見ると, Then: 廃止した希望入力の語が含まれない")
    void doesNotContainLegacyInputTerms() throws Exception {
      String html = bodyOf(perform(get("/")));

      assertFalse(html.toLowerCase(Locale.ROOT).contains(LEGACY_TOKEN));
    }

    @Test
    @DisplayName(
        "[F-3][F-11] Given: 月間の入力が送信されたとき, When: POST /shift を呼ぶと,"
            + " Then: フォームの内容が月間入力として create に渡る")
    void passesFormContentToService() throws Exception {
      when(monthlyShiftService.create(any()))
          .thenReturn(new MonthlyShiftResult(YearMonth.of(2026, 10), List.of()));

      perform(validRequest());

      ArgumentCaptor<MonthlyShiftInput> captor = ArgumentCaptor.forClass(MonthlyShiftInput.class);
      verify(monthlyShiftService).create(captor.capture());
      MonthlyShiftInput input = captor.getValue();
      assertEquals(YearMonth.of(2026, 10), input.month());
      assertEquals("A", input.employees().get(0).name());
      assertEquals(EmploymentType.PART_TIME, input.employees().get(0).employmentType());
      assertEquals(
          LocalTime.of(8, 0), input.employees().get(0).baseShifts().get(DayOfWeek.MONDAY).start());
      assertEquals(
          LocalTime.of(17, 0), input.employees().get(0).baseShifts().get(DayOfWeek.MONDAY).end());
      assertTrue(input.employees().get(0).baseShifts().get(DayOfWeek.TUESDAY).off());
      assertEquals(LocalDate.of(2026, 10, 20), input.adjustments().get(0).date());
      assertEquals("A", input.adjustments().get(0).employeeName());
      assertTrue(input.adjustments().get(0).wish().off());
    }

    @Test
    @DisplayName(
        "[F-3] Given: 算出が成功するとき, When: POST /shift を呼ぶと,"
            + " Then: monthlyResult と initialStep=3 がモデルに入り、入力エラーはない")
    void putsResultAndStep3OnSuccess() throws Exception {
      MonthlyShiftResult monthly = new MonthlyShiftResult(YearMonth.of(2026, 10), List.of());
      when(monthlyShiftService.create(any())).thenReturn(monthly);

      MvcResult result = perform(validRequest());

      assertSame(monthly, modelOf(result).get("monthlyResult"));
      assertEquals(3, modelOf(result).get("initialStep"));
      assertNull(modelOf(result).get("inputErrors"));
    }

    @Test
    @DisplayName(
        "[F-4] Given: 不成立 2 日の結果と名前が空の従業員行, When: POST /shift を呼ぶと,"
            + " Then: resultView に営業日数・成立・不成立・祝日と、名前が空でない従業員だけの行が入力順に入る")
    void putsResultViewOnSuccess() throws Exception {
      DailyShiftResult failed =
          new DailyShiftResult(LocalDate.of(2026, 10, 1), 5, Optional.empty());
      DailyShiftResult failed2 =
          new DailyShiftResult(LocalDate.of(2026, 10, 2), 6, Optional.empty());
      when(monthlyShiftService.create(any()))
          .thenReturn(new MonthlyShiftResult(YearMonth.of(2026, 10), List.of(failed, failed2)));
      when(holidayService.holidaysOf(YearMonth.of(2026, 10)))
          .thenReturn(Map.of(LocalDate.of(2026, 10, 12), "スポーツの日"));

      MvcResult result =
          perform(
              validRequest()
                  .param("employees[1].name", "  ")
                  .param("employees[1].employmentType", "FULL_TIME")
                  .param("employees[2].name", "B")
                  .param("employees[2].employmentType", "FULL_TIME"));

      MonthlyResultView view = (MonthlyResultView) modelOf(result).get("resultView");
      assertNotNull(view);
      assertEquals(2, view.businessDayCount());
      assertEquals(0, view.successCount());
      assertEquals(2, view.failureCount());
      assertEquals(List.of("A", "B"), view.employeeRows().stream().map(row -> row.name()).toList());
      assertEquals(1, view.holidayCells().size());
    }

    @Test
    @DisplayName(
        "[F-5] Given: 成立の日と不成立の日がある結果のとき, When: POST /shift を呼ぶと,"
            + " Then: 画面 3 に日付・成立可否・勤務できる人数が出る")
    void rendersMonthlyResultOnScreen3() throws Exception {
      DailyShiftResult feasible =
          new DailyShiftResult(
              LocalDate.of(2026, 10, 1), 9, Optional.of(mock(AssignmentResult.class)));
      DailyShiftResult infeasible =
          new DailyShiftResult(LocalDate.of(2026, 10, 2), 5, Optional.empty());
      when(monthlyShiftService.create(any()))
          .thenReturn(
              new MonthlyShiftResult(YearMonth.of(2026, 10), List.of(feasible, infeasible)));

      String html = bodyOf(perform(validRequest()));

      assertTrue(html.contains("data-initial-step=\"3\""));
      assertTrue(html.contains("2026-10-01"));
      assertTrue(html.contains("2026-10-02"));
      assertTrue(html.contains(">不成立<"));
      assertTrue(html.contains(">成立<"));
      assertTrue(html.contains("勤務できる人数: 5"));
      assertFalse(html.contains("role=\"alert\""));
    }

    @Test
    @DisplayName("[F-6] Given: 従業員を 1 行も送らないとき, When: POST /shift を呼ぶと, Then: 空の行が 1 行補われる")
    void addsOneEmptyRowWhenNoEmployeesAreSent() throws Exception {
      when(monthlyShiftService.create(any()))
          .thenReturn(new MonthlyShiftResult(YearMonth.of(2026, 10), List.of()));

      MvcResult result = perform(post("/shift").param("targetMonth", "2026-10"));

      ShiftForm form = (ShiftForm) modelOf(result).get("shiftForm");
      assertEquals(1, form.getEmployees().size());
      assertEquals("FULL_TIME", form.getEmployees().get(0).getEmploymentType());
      assertEquals(5, form.getEmployees().get(0).getDays().size());
    }
  }

  @Nested
  class 異常系 {

    @Test
    @DisplayName(
        "[V-2][V-3][V-9] Given: 入力エラーが 3 件あるとき, When: POST /shift を呼ぶと,"
            + " Then: inputErrors に同じ順で入り initialStep=1 で monthlyResult がない")
    void putsInputErrorsInOrderOnError() throws Exception {
      InputError v2 = new InputError("V-2", "氏名「A」が重複しています");
      InputError v3 = new InputError("V-3", "Aの月曜の時間帯が不正です");
      InputError v9 = new InputError("V-9", "2026-10-03 は営業日ではありません");
      throwInputErrors(v2, v3, v9);

      MvcResult result = perform(validRequest());

      assertEquals(List.of(v2, v3, v9), modelOf(result).get("inputErrors"));
      assertEquals(1, modelOf(result).get("initialStep"));
      assertNull(modelOf(result).get("monthlyResult"));
    }

    @Test
    @DisplayName("[V-3] Given: 入力エラーになるとき, When: POST /shift を呼ぶと, Then: resultView はモデルに入らない")
    void doesNotPutResultViewOnError() throws Exception {
      throwInputErrors(new InputError("V-3", "エラー"));

      MvcResult result = perform(validRequest());

      assertNull(modelOf(result).get("resultView"));
    }

    @Test
    @DisplayName(
        "[V-2][V-3] Given: 入力エラーがあるとき, When: POST /shift の HTML を見ると,"
            + " Then: 画面 1 より前に全エラーが集約され、入力値が保持される")
    void rendersInputErrorsAboveScreen1() throws Exception {
      throwInputErrors(
          new InputError("V-2", "氏名「A」が重複しています"), new InputError("V-3", "Aの月曜の時間帯が不正です"));

      String html = bodyOf(perform(validRequest()));

      assertTrue(html.contains("data-initial-step=\"1\""));
      assertTrue(html.contains("role=\"alert\""));
      assertTrue(html.contains("V-2") && html.contains("氏名「A」が重複しています"));
      assertTrue(html.contains("V-3") && html.contains("Aの月曜の時間帯が不正です"));
      assertTrue(html.indexOf("role=\"alert\"") < html.indexOf("id=\"screen-1\""));
      assertTrue(html.contains("value=\"A\""));
      assertFalse(html.contains("シフト作成結果"));
    }

    @Test
    @DisplayName(
        "[F-11] Given: 個別変更を送信して入力エラーになったとき, When: POST /shift の HTML を見ると,"
            + " Then: 個別変更が日付・従業員名つきの hidden 入力に復元される")
    void restoresAdjustmentsOnError() throws Exception {
      throwInputErrors(new InputError("V-3", "エラー"));

      String html = bodyOf(perform(validRequest()));

      assertTrue(
          html.contains("type=\"hidden\" name=\"adjustments[0].date\" value=\"2026-10-20\""));
      assertTrue(html.contains("type=\"hidden\" name=\"adjustments[0].employeeName\" value=\"A\""));
      assertTrue(html.contains("name=\"adjustments[0].off\""));
    }

    @Test
    @DisplayName(
        "[V-3] Given: 従業員名に HTML タグを含めて入力エラーになったとき, When: POST /shift の HTML を見ると,"
            + " Then: 氏名はエスケープされ、タグとして出力されない")
    void escapesEmployeeNameOnError() throws Exception {
      throwInputErrors(new InputError("V-3", "エラー"));
      String name = "<img src=x onerror=alert(1)>";

      String html =
          bodyOf(
              perform(
                  post("/shift")
                      .param("targetMonth", "2026-10")
                      .param("employees[0].name", name)
                      .param("employees[0].employmentType", "FULL_TIME")
                      .param("adjustments[0].date", "2026-10-20")
                      .param("adjustments[0].employeeName", name)));

      assertFalse(html.contains("<img"));
      assertTrue(html.contains("&lt;img src=x onerror=alert(1)&gt;"));
    }
  }
}
