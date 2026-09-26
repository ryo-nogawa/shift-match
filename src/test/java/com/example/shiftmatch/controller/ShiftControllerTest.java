package com.example.shiftmatch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.DailyShiftResult;
import com.example.shiftmatch.domain.DailyWish;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.EmployeeProfile;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.HolidayDataUnavailableError;
import com.example.shiftmatch.domain.InputError;
import com.example.shiftmatch.domain.InvalidMonthlyInputException;
import com.example.shiftmatch.domain.MonthlyShiftInput;
import com.example.shiftmatch.domain.MonthlyShiftResult;
import com.example.shiftmatch.domain.ShiftAdjustment;
import com.example.shiftmatch.domain.ShiftAssignment;
import com.example.shiftmatch.domain.ShiftSlot;
import com.example.shiftmatch.domain.ShiftStorageException;
import com.example.shiftmatch.persistence.SavedMonthlyShift;
import com.example.shiftmatch.service.HolidayService;
import com.example.shiftmatch.service.MonthlyShiftService;
import com.example.shiftmatch.service.SavedInput;
import com.example.shiftmatch.service.ShiftStorageService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
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
@Import({MonthlyFormConverter.class, MonthlyResultViewFactory.class, SavedInputFormConverter.class})
class ShiftControllerTest {

  /** Thymeleaf のフラグメント指定（区切りがメソッド参照の検査に誤検出されないよう分割）。 */
  private static final String RESULT_FRAGMENT = "fragments/result :" + ": resultPanel";

  /** 廃止した枠ごとの 3 段階の希望入力の名残を検出する語（ソース検索で誤検出しないよう分割）。 */
  private static final String LEGACY_TOKEN = "wi" + "sh";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private MonthlyShiftService monthlyShiftService;

  @MockitoBean private HolidayService holidayService;

  @MockitoBean private ShiftStorageService shiftStorageService;

  @BeforeEach
  void stubEmptySavedInput() {
    when(shiftStorageService.loadInput())
        .thenReturn(new SavedInput(List.of(), List.of(), Optional.empty()));
  }

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

  /** 8 名を枠 1 → 6 の順に割り当てた成立の日を作ります。名前は先頭から順に firstName、e2〜e8 です。 */
  private static DailyShiftResult feasibleDay(LocalDate date, String firstName) {
    return feasibleDay(date, firstName, List.of());
  }

  private static DailyShiftResult feasibleDay(
      LocalDate date, String firstName, List<Employee> unassigned) {
    List<ShiftAssignment> assignments = new ArrayList<>();
    for (int i = 0; i < SLOTS_IN_ORDER.size(); i++) {
      String name = i == 0 ? firstName : "e" + (i + 1);
      assignments.add(
          new ShiftAssignment(
              Employee.working(name, LocalTime.of(7, 30), LocalTime.of(18, 30)),
              SLOTS_IN_ORDER.get(i),
              LocalTime.of(12, 0),
              LocalTime.of(12, 45)));
    }
    return new DailyShiftResult(
        date, 8 + unassigned.size(), Optional.of(new AssignmentResult(assignments, 0, unassigned)));
  }

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
        "[F-4][7.1節] Given: 不成立 2 日の結果のとき, When: POST /shift の HTML を見ると,"
            + " Then: 集計・3 つのタブボタン・3 つのパネルがあり、初期はカレンダーだけ表示される")
    void rendersSummaryAndTabsOnScreen3() throws Exception {
      DailyShiftResult failed1 =
          new DailyShiftResult(LocalDate.of(2026, 10, 1), 5, Optional.empty());
      DailyShiftResult failed2 =
          new DailyShiftResult(LocalDate.of(2026, 10, 2), 6, Optional.empty());
      when(monthlyShiftService.create(any()))
          .thenReturn(new MonthlyShiftResult(YearMonth.of(2026, 10), List.of(failed1, failed2)));

      String html = bodyOf(perform(validRequest()));

      assertTrue(html.contains("data-initial-step=\"3\""));
      assertTrue(html.contains("id=\"result-summary\""));
      assertTrue(html.contains("営業日数 2"));
      assertTrue(html.contains("成立 0"));
      assertTrue(html.contains("不成立 2"));
      for (String tab : List.of("calendar", "employees", "detail")) {
        assertTrue(html.contains("data-tab=\"" + tab + "\""), tab);
        assertTrue(html.contains("id=\"tab-" + tab + "\""), tab);
      }
      assertTrue(html.contains(">カレンダー表示<"));
      assertTrue(html.contains(">従業員別表示<"));
      assertTrue(html.contains(">日別詳細<"));
      assertFalse(html.contains("<div id=\"tab-calendar\" class=\"tab-panel\" hidden"));
      assertTrue(html.contains("id=\"tab-employees\" class=\"tab-panel\" hidden"));
      assertTrue(html.contains("id=\"tab-detail\" class=\"tab-panel\" hidden"));
      assertTrue(html.contains("result-tabs.js"));
      assertFalse(html.contains("role=\"alert\""));
    }

    @Test
    @DisplayName(
        "[F-4][F-5][7.1節] Given: 成立の日・不成立の日・祝日がある月, When: POST /shift の HTML を見ると,"
            + " Then: カレンダーに勤務時間ごとの氏名・不成立（勤務可 n 名）・祝日名・日付ボタンが出る")
    void rendersCalendarTab() throws Exception {
      when(monthlyShiftService.create(any()))
          .thenReturn(
              new MonthlyShiftResult(
                  YearMonth.of(2026, 10),
                  List.of(
                      feasibleDay(LocalDate.of(2026, 10, 1), "e1"),
                      new DailyShiftResult(LocalDate.of(2026, 10, 2), 5, Optional.empty()))));
      when(holidayService.holidaysOf(YearMonth.of(2026, 10)))
          .thenReturn(Map.of(LocalDate.of(2026, 10, 12), "スポーツの日"));

      String html = bodyOf(perform(validRequest()));

      assertTrue(html.contains("<div class=\"work-group\">7:30–14:30 e1・e2</div>"));
      assertTrue(html.contains("<div class=\"work-group\">8:00–15:30 e3</div>"));
      assertTrue(html.contains("<div class=\"work-group\">9:00–18:30 e7・e8</div>"));
      assertTrue(html.contains("class=\"failed\""));
      assertTrue(html.contains("不成立（勤務可 5 名）"));
      assertTrue(html.contains("class=\"holiday\""));
      assertTrue(html.contains("スポーツの日"));
      assertTrue(html.contains("class=\"calendar-day\""));
      assertTrue(html.contains("data-date=\"2026-10-01\""));
      assertTrue(html.contains("data-date=\"2026-10-02\""));
      assertFalse(html.contains("data-date=\"2026-10-12\" class=\"calendar-day\""));
    }

    @Test
    @DisplayName(
        "[F-4] Given: 氏名に HTML タグを含む成立の日, When: POST /shift の HTML を見ると,"
            + " Then: カレンダーの氏名はエスケープされる")
    void escapesEmployeeNameInCalendar() throws Exception {
      when(monthlyShiftService.create(any()))
          .thenReturn(
              new MonthlyShiftResult(
                  YearMonth.of(2026, 10),
                  List.of(feasibleDay(LocalDate.of(2026, 10, 1), "<script>alert(1)</script>"))));

      String html = bodyOf(perform(validRequest()));

      assertFalse(html.contains("<script>alert(1)</script>"));
      assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;・e2"));
    }

    @Test
    @DisplayName(
        "[F-4] Given: 割り当て・休み・割り当てなし・不成立の日がある結果, When: POST /shift の HTML を見ると,"
            + " Then: 従業員別表示に従業員名・日付見出し・各セル・出勤日数・名前列の固定が出る")
    void rendersEmployeesTab() throws Exception {
      when(monthlyShiftService.create(any()))
          .thenReturn(
              new MonthlyShiftResult(
                  YearMonth.of(2026, 10),
                  List.of(
                      feasibleDay(
                          LocalDate.of(2026, 10, 1),
                          "e1",
                          List.of(
                              Employee.onLeave("休みさん"),
                              Employee.working("控えさん", LocalTime.of(7, 30), LocalTime.of(8, 0)))),
                      new DailyShiftResult(LocalDate.of(2026, 10, 2), 5, Optional.empty()))));

      String html =
          bodyOf(
              perform(
                  post("/shift")
                      .param("targetMonth", "2026-10")
                      .param("employees[0].name", "e1")
                      .param("employees[0].employmentType", "FULL_TIME")
                      .param("employees[1].name", "休みさん")
                      .param("employees[1].employmentType", "FULL_TIME")
                      .param("employees[2].name", "控えさん")
                      .param("employees[2].employmentType", "FULL_TIME")));

      int start = html.indexOf("id=\"tab-employees\"");
      int end = html.indexOf("id=\"tab-detail\"");
      String panel = html.substring(start, end);
      assertTrue(panel.contains(">10/1(木)<"));
      assertTrue(panel.contains(">10/2(金)<"));
      assertTrue(panel.contains(">e1<"));
      assertTrue(panel.contains(">休みさん<"));
      assertTrue(panel.contains(">7:30–14:30<"));
      assertTrue(panel.contains(">休<"));
      assertTrue(panel.contains(">–<"));
      assertTrue(panel.contains(">×<"));
      assertTrue(panel.contains("出勤日数"));
      assertTrue(panel.contains("class=\"sticky-col\""));
      assertTrue(panel.contains("class=\"work-days\">1<"));
    }

    @Test
    @DisplayName(
        "[F-4][F-5][7.2節] Given: 成立の日・未出勤者・不成立の日がある結果, When: POST /shift の HTML を見ると,"
            + " Then: 日別詳細に列見出しの順・勤務時間と休憩時間の形式・時間軸バー・凡例・不成立の文言・日付の選択がある")
    void rendersDetailTab() throws Exception {
      when(monthlyShiftService.create(any()))
          .thenReturn(
              new MonthlyShiftResult(
                  YearMonth.of(2026, 10),
                  List.of(
                      feasibleDay(
                          LocalDate.of(2026, 10, 1),
                          "e1",
                          List.of(Employee.onLeave("休みさん", EmploymentType.PART_TIME))),
                      new DailyShiftResult(LocalDate.of(2026, 10, 2), 5, Optional.empty()))));

      String html = bodyOf(perform(validRequest()));

      String panel = html.substring(html.indexOf("id=\"tab-detail\""), html.indexOf("</main>"));
      assertTrue(panel.contains("id=\"detail-date\""));
      assertTrue(panel.contains("class=\"day-detail\" hidden data-date=\"2026-10-01\""));
      assertTrue(panel.contains("class=\"day-detail\" hidden data-date=\"2026-10-02\""));
      int name = panel.indexOf("<th>氏名</th>");
      int type = panel.indexOf("<th>区分</th>");
      int work = panel.indexOf("<th>勤務時間</th>");
      int rest = panel.indexOf("<th>休憩時間</th>");
      int wish = panel.indexOf("<th>希望時間帯</th>");
      assertTrue(0 <= name && name < type && type < work && work < rest && rest < wish);
      assertTrue(panel.contains("07:30〜14:30"));
      assertTrue(panel.contains("12:00〜12:45"));
      assertTrue(panel.contains("07:30〜18:30"));
      assertTrue(panel.contains("class=\"duration\" data-start=\"07:30\" data-end=\"14:30\""));
      assertTrue(panel.contains("class=\"duration\" data-start=\"12:00\" data-end=\"12:45\""));
      assertTrue(panel.contains("class=\"tl-work\""));
      assertTrue(panel.contains("class=\"tl-break\""));
      assertTrue(panel.contains("left:0.00%;width:63.64%"));
      assertTrue(panel.contains("left:40.91%;width:6.82%"));
      assertTrue(panel.contains(">勤務<"));
      assertTrue(panel.contains(">休憩<"));
      assertTrue(panel.contains("休みさん（パート）"));
      assertTrue(panel.contains("休み"));
      assertTrue(panel.contains("不成立です。勤務できる人数：5 名"));
      assertFalse(panel.contains("枠"));
    }

    @Test
    @DisplayName(
        "[F-4][8.3節] Given: 保存済みのシフトがない初期表示のとき, When: GET / の HTML を見ると,"
            + " Then: 「この月のシフトはまだ作成されていません」が出て、タブや集計は出ない")
    void rendersNotCreatedMessageWithoutResult() throws Exception {
      String html = bodyOf(perform(get("/")));

      assertTrue(html.contains("この月のシフトはまだ作成されていません"));
      assertFalse(html.contains("結果はまだありません"));
      assertFalse(html.contains("id=\"result-summary\""));
      assertFalse(html.contains("data-tab="));
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

  @Nested
  class 保存 {

    private static final String SAVE_ERROR_MESSAGE = "保存に失敗しました。もう一度シフトを作成して保存し直してください";

    @Test
    @DisplayName(
        "[F-7][8.4節] Given: 算出が成功するとき, When: POST /shift を呼ぶと, Then: 入力と結果が 1 回保存され resultSource は"
            + " fresh になる")
    void savesInputAndResultOnce() throws Exception {
      MonthlyShiftResult monthly = new MonthlyShiftResult(YearMonth.of(2026, 10), List.of());
      when(monthlyShiftService.create(any())).thenReturn(monthly);

      MvcResult result = perform(validRequest());

      verify(shiftStorageService, times(1)).save(any(MonthlyShiftInput.class), same(monthly));
      assertEquals("fresh", modelOf(result).get("resultSource"));
      assertNull(modelOf(result).get("saveError"));
    }

    @Test
    @DisplayName("[F-7][8.4節] Given: 入力エラーになるとき, When: POST /shift を呼ぶと, Then: 保存は呼ばれない")
    void doesNotSaveOnInputError() throws Exception {
      throwInputErrors(new InputError("V-3", "エラー"));

      perform(validRequest());

      verify(shiftStorageService, never()).save(any(), any());
    }

    @Test
    @DisplayName(
        "[F-7][8.4節] Given: 保存に失敗するとき, When: POST /shift を呼ぶと,"
            + " Then: 結果は通常どおり表示され、リトライを促す文言が alert で出る")
    void showsResultAndRetryMessageWhenSaveFails() throws Exception {
      MonthlyShiftResult monthly =
          new MonthlyShiftResult(
              YearMonth.of(2026, 10), List.of(feasibleDay(LocalDate.of(2026, 10, 1), "A")));
      when(monthlyShiftService.create(any())).thenReturn(monthly);
      doThrow(new ShiftStorageException("失敗", new RuntimeException()))
          .when(shiftStorageService)
          .save(any(), any());

      MvcResult result = perform(validRequest());

      assertSame(monthly, modelOf(result).get("monthlyResult"));
      assertNotNull(modelOf(result).get("resultView"));
      assertEquals(3, modelOf(result).get("initialStep"));
      assertEquals(SAVE_ERROR_MESSAGE, modelOf(result).get("saveError"));
      String html = bodyOf(result);
      assertTrue(html.contains(SAVE_ERROR_MESSAGE));
      assertTrue(html.contains("id=\"result-summary\""));
      int screen3 = html.indexOf("id=\"screen-3\"");
      int alert = html.indexOf("role=\"alert\"", screen3);
      assertTrue(alert > screen3);
    }
  }

  @Nested
  class 復元 {

    private EmployeeProfile savedProfile(String name) {
      Map<DayOfWeek, DailyWish> shifts = new EnumMap<>(DayOfWeek.class);
      shifts.put(DayOfWeek.MONDAY, new DailyWish(true, null, null));
      shifts.put(DayOfWeek.TUESDAY, new DailyWish(false, LocalTime.of(8, 0), LocalTime.of(17, 0)));
      shifts.put(
          DayOfWeek.WEDNESDAY, new DailyWish(false, LocalTime.of(8, 0), LocalTime.of(17, 0)));
      shifts.put(DayOfWeek.THURSDAY, new DailyWish(false, LocalTime.of(8, 0), LocalTime.of(17, 0)));
      shifts.put(DayOfWeek.FRIDAY, new DailyWish(false, LocalTime.of(8, 0), LocalTime.of(17, 0)));
      return new EmployeeProfile(name, EmploymentType.PART_TIME, shifts);
    }

    @Test
    @DisplayName(
        "[F-7][8.1節] Given: 従業員・個別変更・最後の対象月が保存済み, When: GET / を呼ぶと,"
            + " Then: 先頭に復元され残りは空で計 12 行、個別変更と最後の対象月が入る")
    void restoresSavedInputOnGet() throws Exception {
      when(shiftStorageService.loadInput())
          .thenReturn(
              new SavedInput(
                  List.of(savedProfile("佐藤"), savedProfile("鈴木")),
                  List.of(
                      new ShiftAdjustment(
                          LocalDate.of(2026, 11, 2), "佐藤", new DailyWish(true, null, null))),
                  Optional.of(YearMonth.of(2026, 11))));

      MvcResult result = perform(get("/"));

      ShiftForm form = (ShiftForm) modelOf(result).get("shiftForm");
      assertEquals("2026-11", form.getTargetMonth());
      assertEquals(12, form.getEmployees().size());
      assertEquals("佐藤", form.getEmployees().get(0).getName());
      assertEquals("鈴木", form.getEmployees().get(1).getName());
      assertEquals("PART_TIME", form.getEmployees().get(1).getEmploymentType());
      assertTrue(form.getEmployees().get(0).getDays().get(0).isOff());
      assertEquals("", form.getEmployees().get(2).getName());
      assertEquals(1, form.getAdjustments().size());
      assertEquals("2026-11-02", form.getAdjustments().get(0).getDate());
      assertTrue(bodyOf(result).contains("value=\"佐藤\""));
    }

    @Test
    @DisplayName("[F-7][8.1節] Given: 何も保存していない, When: GET / を呼ぶと, Then: 空の 12 行と今月になる")
    void returnsEmptyRowsAndCurrentMonthWhenNothingSaved() throws Exception {
      MvcResult result = perform(get("/"));

      ShiftForm form = (ShiftForm) modelOf(result).get("shiftForm");
      assertEquals(
          YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM")), form.getTargetMonth());
      assertEquals(12, form.getEmployees().size());
      assertEquals("", form.getEmployees().get(0).getName());
      assertTrue(form.getAdjustments().isEmpty());
    }
  }

  @Nested
  class 結果の表示元 {

    private static final YearMonth MONTH = YearMonth.of(2026, 10);

    private void stubSavedShift() {
      when(shiftStorageService.loadInput())
          .thenReturn(new SavedInput(List.of(), List.of(), Optional.of(MONTH)));
      MonthlyShiftResult monthly =
          new MonthlyShiftResult(MONTH, List.of(feasibleDay(LocalDate.of(2026, 10, 1), "A")));
      when(shiftStorageService.load(MONTH))
          .thenReturn(Optional.of(new SavedMonthlyShift(monthly, List.of("A"))));
    }

    @Test
    @DisplayName(
        "[F-7][8.3節] Given: 対象月の保存済みシフトがある, When: GET / を呼ぶと,"
            + " Then: 結果が resultSource=saved で入り、initialStep は 1 のまま「保存済みのシフトを表示しています」が出る")
    void showsSavedShift() throws Exception {
      stubSavedShift();

      MvcResult result = perform(get("/"));

      assertEquals("saved", modelOf(result).get("resultSource"));
      assertNotNull(modelOf(result).get("monthlyResult"));
      assertNotNull(modelOf(result).get("resultView"));
      assertEquals(1, modelOf(result).get("initialStep"));
      String html = bodyOf(result);
      assertTrue(html.contains("保存済みのシフトを表示しています"));
      assertTrue(html.contains("id=\"result-summary\""));
      assertFalse(html.contains("この月のシフトはまだ作成されていません"));
    }

    @Test
    @DisplayName(
        "[F-7][8.3節] Given: 保存済みシフトがあるが祝日データが取得できない, When: GET / を呼ぶと," + " Then: 例外にならず祝日なしで表示される")
    void showsSavedShiftWhenHolidayDataUnavailable() throws Exception {
      stubSavedShift();
      when(holidayService.holidaysOf(MONTH)).thenThrow(new HolidayDataUnavailableError(MONTH));

      MvcResult result = perform(get("/"));

      assertEquals("saved", modelOf(result).get("resultSource"));
      assertTrue(bodyOf(result).contains("id=\"result-summary\""));
    }

    @Test
    @DisplayName(
        "[F-7][8.3節] Given: 対象月の保存済みシフトがない, When: GET / を呼ぶと,"
            + " Then: resultSource=none で「この月のシフトはまだ作成されていません」が出て、結果は出ない")
    void showsNotCreatedMessage() throws Exception {
      MvcResult result = perform(get("/"));

      assertEquals("none", modelOf(result).get("resultSource"));
      assertNull(modelOf(result).get("resultView"));
      String html = bodyOf(result);
      assertTrue(html.contains("この月のシフトはまだ作成されていません"));
      assertFalse(html.contains("保存済みのシフトを表示しています"));
    }

    @Test
    @DisplayName(
        "[F-7][8.3節] Given: 今回作成した結果, When: POST /shift の HTML を見ると,"
            + " Then: 保存済み・未作成のどちらの文言も付かず結果が出る")
    void showsFreshResultWithoutSourceMessage() throws Exception {
      when(monthlyShiftService.create(any()))
          .thenReturn(
              new MonthlyShiftResult(MONTH, List.of(feasibleDay(LocalDate.of(2026, 10, 1), "A"))));

      String html = bodyOf(perform(validRequest()));

      assertTrue(html.contains("id=\"result-summary\""));
      assertFalse(html.contains("保存済みのシフトを表示しています"));
      assertFalse(html.contains("この月のシフトはまだ作成されていません"));
    }
  }

  @Nested
  class 保存済みシフトの取得 {

    private static final YearMonth MONTH = YearMonth.of(2026, 10);

    @Test
    @DisplayName(
        "[F-7][8.3節] Given: 指定した月の保存済みシフトがある, When: GET /shift/saved を呼ぶと,"
            + " Then: 結果のフラグメントだけが「保存済みのシフトを表示しています」つきで返る")
    void returnsSavedFragment() throws Exception {
      MonthlyShiftResult monthly =
          new MonthlyShiftResult(MONTH, List.of(feasibleDay(LocalDate.of(2026, 10, 1), "A")));
      when(shiftStorageService.load(MONTH))
          .thenReturn(Optional.of(new SavedMonthlyShift(monthly, List.of("A"))));

      MvcResult result =
          mockMvc
              .perform(get("/shift/saved").param("month", "2026-10"))
              .andExpect(status().isOk())
              .andExpect(view().name(RESULT_FRAGMENT))
              .andReturn();

      String html = bodyOf(result);
      assertTrue(html.contains("保存済みのシフトを表示しています"));
      assertTrue(html.contains("id=\"result-summary\""));
      assertFalse(html.contains("id=\"screen-3\""));
      assertFalse(html.contains("<form"));
    }

    @Test
    @DisplayName(
        "[F-7][8.3節] Given: 指定した月の保存済みシフトがない, When: GET /shift/saved を呼ぶと,"
            + " Then: 「この月のシフトはまだ作成されていません」だけが返る")
    void returnsNotCreatedFragment() throws Exception {
      String html =
          bodyOf(
              mockMvc
                  .perform(get("/shift/saved").param("month", "2026-10"))
                  .andExpect(status().isOk())
                  .andReturn());

      assertTrue(html.contains("この月のシフトはまだ作成されていません"));
      assertFalse(html.contains("id=\"result-summary\""));
    }

    @Test
    @DisplayName(
        "[F-7][8.3節] Given: 形式が不正な month, When: GET /shift/saved を呼ぶと, Then: 400 で保存は参照しない")
    void returnsBadRequestWhenMonthIsInvalid() throws Exception {
      mockMvc
          .perform(get("/shift/saved").param("month", "2026-13"))
          .andExpect(status().isBadRequest());

      verify(shiftStorageService, never()).load(any());
    }

    @Test
    @DisplayName("[F-7][8.3節] Given: month がない, When: GET /shift/saved を呼ぶと, Then: 400 になる")
    void returnsBadRequestWhenMonthIsMissing() throws Exception {
      mockMvc.perform(get("/shift/saved")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName(
        "[F-7][8.3節] Given: 祝日データの収録範囲外の年, When: GET /shift/saved を呼ぶと, Then: 400 で保存は参照しない")
    void returnsBadRequestWhenYearIsOutOfRange() throws Exception {
      mockMvc
          .perform(get("/shift/saved").param("month", "1954-12"))
          .andExpect(status().isBadRequest());
      mockMvc
          .perform(get("/shift/saved").param("month", (Year.now().getValue() + 2) + "-01"))
          .andExpect(status().isBadRequest());

      verify(shiftStorageService, never()).load(any());
    }
  }

  @Nested
  class 結果パネルの属性 {

    @Test
    @DisplayName(
        "[F-7][8.3節] Given: 今回作成した結果, When: POST /shift の HTML を見ると,"
            + " Then: 結果パネルに data-result-source=fresh と data-result-month が付く")
    void freshPanelHasSourceAndMonth() throws Exception {
      when(monthlyShiftService.create(any()))
          .thenReturn(
              new MonthlyShiftResult(
                  YearMonth.of(2026, 10), List.of(feasibleDay(LocalDate.of(2026, 10, 1), "A"))));

      String html = bodyOf(perform(validRequest()));

      assertTrue(html.contains("id=\"result-panel\""));
      assertTrue(html.contains("data-result-source=\"fresh\""));
      assertTrue(html.contains("data-result-month=\"2026-10\""));
    }

    @Test
    @DisplayName(
        "[F-7][8.3節] Given: 保存済みシフトがない, When: GET /shift/saved の HTML を見ると,"
            + " Then: 結果パネルに data-result-source=none が付き、data-result-month はない")
    void nonePanelHasSourceOnly() throws Exception {
      String html =
          bodyOf(
              mockMvc
                  .perform(get("/shift/saved").param("month", "2026-10"))
                  .andExpect(status().isOk())
                  .andReturn());

      assertTrue(html.contains("id=\"result-panel\""));
      assertTrue(html.contains("data-result-source=\"none\""));
      assertFalse(html.contains("data-result-month"));
    }
  }
}
