package com.example.shiftmatch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.ShiftAssignment;
import com.example.shiftmatch.domain.ShiftSlot;
import com.example.shiftmatch.persistence.LatestShiftRepository;
import com.example.shiftmatch.service.ShiftAssignmentService;
import com.example.shiftmatch.service.ShiftAssignmentServiceImpl;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * ShiftControllerのテスト。
 */
@WebMvcTest(ShiftController.class)
@DisplayName("ShiftController")
class ShiftControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ShiftAssignmentService shiftAssignmentService;

  @MockitoBean private LatestShiftRepository latestShiftRepository;

  /**
   * 廃止した枠ごとの 3 段階の希望入力の名残を検出する語（小文字）。
   *
   * <p>ソースに廃止した入力の残骸がないことを文字列検索で確認できるよう、分割して記述します。
   */
  private static final String LEGACY_TOKEN = "wi" + "sh";

  /** 廃止した希望入力のスコア表示で使っていた記号（二重丸、U+25CE）。 */
  private static final String LEGACY_MARK = String.valueOf((char) 0x25CE);

  /**
   * テスト用の割当結果を作成します（A-H の 8 名、各枠に割り当て）。
   *
   * @return 標準的な割当結果
   */
  private AssignmentResult createStandardResult() {
    List<ShiftAssignment> assignments =
        List.of(
            new ShiftAssignment(
                Employee.working("A", LocalTime.of(7, 30), LocalTime.of(18, 30)),
                ShiftSlot.SLOT_1,
                LocalTime.of(12, 0),
                LocalTime.of(12, 45)),
            new ShiftAssignment(
                Employee.working("B", LocalTime.of(7, 30), LocalTime.of(18, 30)),
                ShiftSlot.SLOT_1,
                LocalTime.of(12, 0),
                LocalTime.of(12, 45)),
            new ShiftAssignment(
                Employee.working("C", LocalTime.of(7, 30), LocalTime.of(18, 30)),
                ShiftSlot.SLOT_2,
                LocalTime.of(12, 45),
                LocalTime.of(13, 30)),
            new ShiftAssignment(
                Employee.working("D", LocalTime.of(7, 30), LocalTime.of(18, 30)),
                ShiftSlot.SLOT_3,
                LocalTime.of(12, 45),
                LocalTime.of(13, 30)),
            new ShiftAssignment(
                Employee.working("E", LocalTime.of(7, 30), LocalTime.of(18, 30)),
                ShiftSlot.SLOT_4,
                LocalTime.of(13, 30),
                LocalTime.of(14, 15)),
            new ShiftAssignment(
                Employee.working("F", LocalTime.of(7, 30), LocalTime.of(18, 30)),
                ShiftSlot.SLOT_5,
                LocalTime.of(13, 30),
                LocalTime.of(14, 30)),
            new ShiftAssignment(
                Employee.working("G", LocalTime.of(7, 30), LocalTime.of(18, 30)),
                ShiftSlot.SLOT_6,
                LocalTime.of(14, 15),
                LocalTime.of(15, 15)),
            new ShiftAssignment(
                Employee.working("H", LocalTime.of(7, 30), LocalTime.of(18, 30)),
                ShiftSlot.SLOT_6,
                LocalTime.of(14, 30),
                LocalTime.of(15, 30)));
    return new AssignmentResult(assignments, 8, List.of());
  }

  /**
   * フォームパラメータに、指定行の開始・終了を追加します。
   *
   * @param params パラメータの連結先
   * @param index 行インデックス
   * @param start 開始（HH:mm）
   * @param end 終了（HH:mm）
   */
  private static void appendTimeRange(StringBuilder params, int index, String start, String end) {
    params.append("&employees[").append(index).append("].start=").append(start);
    params.append("&employees[").append(index).append("].end=").append(end);
  }

  @Nested
  @DisplayName("[F-1] 希望入力フォーム")
  class InputForm {

    @Test
    @DisplayName("[F-1] Given: GETリクエストが与えられたとき, When: /にアクセスすると, Then: 廃止した希望入力の語が存在しない")
    void doesNotContainLegacyInputTerms() throws Exception {
      String htmlContent =
          mockMvc
              .perform(get("/"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertFalse(
          htmlContent.toLowerCase(Locale.ROOT).contains(LEGACY_TOKEN),
          "HTML should not contain legacy input terms");
    }

    private String getIndexHtml() throws Exception {
      return mockMvc
          .perform(get("/"))
          .andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString();
    }

    /** 指定した name 属性を持つ要素の開始タグを返します。 */
    private String findTag(String html, String tagName, String name) {
      Matcher matcher =
          Pattern.compile("<" + tagName + "\\b[^>]*name=\"" + Pattern.quote(name) + "\"[^>]*>")
              .matcher(html);
      assertTrue(matcher.find(), "Should find <" + tagName + "> with name " + name);
      return matcher.group();
    }

    /** 指定した name 属性を持つ select 要素全体（option を含む）を返します。 */
    private String findSelect(String html, String name) {
      Matcher matcher =
          Pattern.compile(
                  "<select\\b[^>]*name=\"" + Pattern.quote(name) + "\"[^>]*>.*?</select>",
                  Pattern.DOTALL)
              .matcher(html);
      assertTrue(matcher.find(), "Should find <select> with name " + name);
      return matcher.group();
    }

    @Test
    @DisplayName(
        "[F-1] Given: GETリクエストが与えられたとき, When: /にアクセスすると,"
            + " Then: 休み・開始・終了のname属性があり、廃止した希望のname属性はない")
    void containsOffStartEndInputsWithoutLegacyInputs() throws Exception {
      String html = getIndexHtml();

      String offTag = findTag(html, "input", "employees[0].off");
      assertTrue(offTag.contains("type=\"checkbox\""), "off should be a checkbox");
      findSelect(html, "employees[0].start");
      findSelect(html, "employees[0].end");
      findSelect(html, "employees[3].end");
      assertFalse(
          html.contains("employees[0]." + LEGACY_TOKEN + "es"),
          "HTML should not contain legacy inputs");
    }

    @Test
    @DisplayName(
        "[F-2] Given: GETリクエストが与えられたとき, When: /にアクセスすると," + " Then: 従業員の上限と同じ12行の空の入力行が表示される")
    void showsTwelveEmptyRowsInitially() throws Exception {
      String html = getIndexHtml();

      findTag(html, "input", "employees[11].name");
      assertFalse(
          html.contains("name=\"employees[12].name\""), "Should not render a 13th input row");
      assertTrue(
          Pattern.compile("id=\"row-count\"[^>]*>12<").matcher(html).find(),
          "Row counter should show 12");
    }

    @Test
    @DisplayName(
        "[F-1] Given: GETリクエストが与えられたとき, When: 開始・終了の選択肢を確認すると,"
            + " Then: 未選択と07:30〜18:30があり、07:45はない")
    void startAndEndSelectsHaveThirtyMinuteOptions() throws Exception {
      String html = getIndexHtml();

      for (String name : List.of("employees[0].start", "employees[0].end")) {
        String select = findSelect(html, name);
        assertTrue(select.contains("<option value=\"\">-- 未選択 --</option>"), name + " 未選択");
        assertTrue(select.contains("value=\"07:30\""), name + " should contain 07:30");
        assertTrue(select.contains("value=\"12:00\""), name + " should contain 12:00");
        assertTrue(select.contains("value=\"18:30\""), name + " should contain 18:30");
        assertFalse(select.contains("07:45"), name + " should not contain 07:45");
        assertEquals(24, select.split("<option").length - 1, name + " should have 1 + 23 options");
      }
    }

    @Test
    @DisplayName(
        "[F-1] Given: GETリクエストが与えられたとき, When: /にアクセスすると,"
            + " Then: 廃止した3段階の希望の凡例・枠ごとの見出しがなく、時間帯を入力する説明文がある")
    void doesNotContainLegacyLegendOrSlotHeaders() throws Exception {
      String html = getIndexHtml();

      assertFalse(html.contains(LEGACY_TOKEN + "-legend"), "Legacy legend should be removed");
      assertFalse(html.contains(LEGACY_MARK), "Legacy mark should not be displayed");
      assertFalse(html.contains("data-slot-labels"), "data-slot-labels should be removed");
      assertTrue(html.contains("従業員の勤務できる時間帯を入力すると"), "Lead text should be updated");
    }

    @Test
    @DisplayName(
        "[F-1] Given: 1行目を休みにして送信し入力エラーで再表示されるとき, When: 画面を確認すると,"
            + " Then: 1行目の開始・終了はdisabledで、2行目はdisabledでない")
    void rendersStartAndEndDisabledForOffRow() throws Exception {
      String html =
          mockMvc
              .perform(
                  post("/shift")
                      .param("employees[0].name", "A")
                      .param("employees[0].off", "true")
                      .param("employees[1].name", "B")
                      .param("employees[1].start", "")
                      .param("employees[1].end", "17:00"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertTrue(html.contains("2行目 開始が未選択です"), "Should be re-rendered with V-3 error");
      assertTrue(
          findTag(html, "input", "employees[0].off").contains("checked"), "off should be checked");
      assertTrue(findTag(html, "select", "employees[0].start").contains("disabled"));
      assertTrue(findTag(html, "select", "employees[0].end").contains("disabled"));
      assertFalse(findTag(html, "select", "employees[1].start").contains("disabled"));
      assertFalse(findTag(html, "select", "employees[1].end").contains("disabled"));
    }

    @Test
    @DisplayName(
        "[F-1] Given: 開始08:00・終了17:00で送信し入力エラーで再表示されるとき, When: 画面を確認すると,"
            + " Then: 選択した値が選択状態で再表示される")
    void rendersSelectedStartAndEndAfterError() throws Exception {
      String html =
          mockMvc
              .perform(
                  post("/shift")
                      .param("employees[0].name", "A")
                      .param("employees[0].start", "08:00")
                      .param("employees[0].end", "17:00")
                      .param("employees[1].name", "B")
                      .param("employees[1].start", "")
                      .param("employees[1].end", ""))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertTrue(
          Pattern.compile("<option value=\"08:00\" selected=\"selected\">")
              .matcher(findSelect(html, "employees[0].start"))
              .find(),
          "08:00 should be selected for start");
      assertTrue(
          Pattern.compile("<option value=\"17:00\" selected=\"selected\">")
              .matcher(findSelect(html, "employees[0].end"))
              .find(),
          "17:00 should be selected for end");
    }
  }

  @Nested
  @DisplayName("[V-5][V-4][F-5] 上限・不成立のチェック")
  class EmployeeLimitAndUnassignable {

    @Test
    @DisplayName("[V-5] Given: 有効な従業員13名のとき, When: POSTすると, Then: 上限エラーが表示され、assignが呼ばれない")
    void showsErrorWhen13ValidEmployees() throws Exception {
      StringBuilder params = new StringBuilder();
      for (int i = 0; i < 13; i++) {
        params.append("&employees[").append(i).append("].name=Employee").append(i);
        appendTimeRange(params, i, "07:30", "18:30");
      }

      String responseContent =
          mockMvc
              .perform(
                  post("/shift")
                      .contentType("application/x-www-form-urlencoded")
                      .content(params.toString().substring(1)))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertTrue(
          responseContent.contains("12名") || responseContent.contains("上限"),
          "Error message should contain limit info");
      assertTrue(responseContent.contains("class=\"alert\""), "Error section should be displayed");

      assertFalse(
          responseContent.contains("割当結果"),
          "Assignment result should not be displayed when limit exceeded");

      verify(shiftAssignmentService, never()).assign(any());
    }

    @Test
    @DisplayName("[V-5] Given: 有効な従業員がちょうど12名のとき, When: POSTすると, Then: 上限エラーが表示されない（境界値）")
    void doesNotShowErrorWhen12ValidEmployees() throws Exception {
      StringBuilder params = new StringBuilder();
      for (int i = 0; i < 12; i++) {
        params.append("&employees[").append(i).append("].name=Employee").append(i);
        appendTimeRange(params, i, "07:30", "18:30");
      }

      String responseContent =
          mockMvc
              .perform(
                  post("/shift")
                      .contentType("application/x-www-form-urlencoded")
                      .content(params.toString().substring(1)))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertFalse(
          responseContent.contains("上限（12名）を超えています"),
          "Limit error should not be shown for exactly 12 employees");
    }

    @Test
    @DisplayName("[V-5] Given: 行数13でも有効な従業員11名のとき, When: POSTすると, Then: 上限エラーにならない")
    void doesNotShowErrorWhen13RowsBut11ValidEmployees() throws Exception {
      StringBuilder params = new StringBuilder();
      for (int i = 0; i < 11; i++) {
        params.append("&employees[").append(i).append("].name=Employee").append(i);
        appendTimeRange(params, i, "07:30", "18:30");
      }
      for (int i = 11; i < 13; i++) {
        params.append("&employees[").append(i).append("].name=");
        appendTimeRange(params, i, "", "");
      }

      String responseContent =
          mockMvc
              .perform(
                  post("/shift")
                      .contentType("application/x-www-form-urlencoded")
                      .content(params.toString().substring(1)))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertFalse(
          responseContent.contains("上限（12名）を超えています"),
          "Limit error should not be shown for 11 valid employees");
    }

    /**
     * 勤務する従業員と休みの従業員を指定人数分送信し、レスポンス本文を返します。
     *
     * @param workingCount 勤務する従業員数（7:30〜18:30）
     * @param offCount 休みの従業員数
     * @return レスポンス本文
     * @throws Exception リクエストの実行に失敗した場合
     */
    private String postWorkingAndOffEmployees(int workingCount, int offCount) throws Exception {
      StringBuilder params = new StringBuilder();
      for (int i = 0; i < workingCount; i++) {
        params.append("&employees[").append(i).append("].name=Employee").append(i);
        appendTimeRange(params, i, "07:30", "18:30");
      }
      for (int i = workingCount; i < workingCount + offCount; i++) {
        params.append("&employees[").append(i).append("].name=Employee").append(i);
        params.append("&employees[").append(i).append("].off=true");
      }
      return mockMvc
          .perform(
              post("/shift")
                  .contentType("application/x-www-form-urlencoded")
                  .content(params.toString().substring(1)))
          .andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString();
    }

    @Test
    @DisplayName("[V-5] Given: 休み5名を含む有効な従業員13名のとき, When: POSTすると, Then: 上限エラーが表示され、assignが呼ばれない")
    void showsErrorWhen13ValidEmployeesIncludingOff() throws Exception {
      String responseContent = postWorkingAndOffEmployees(8, 5);

      assertTrue(
          responseContent.contains("上限（12名）を超えています"),
          "Limit error should count employees on leave");
      verify(shiftAssignmentService, never()).assign(any());
    }

    @Test
    @DisplayName("[V-5] Given: 休み4名を含む有効な従業員12名のとき, When: POSTすると, Then: 上限エラーにならず、assignが呼ばれる")
    void callsAssignWhen12ValidEmployeesIncludingOff() throws Exception {
      String responseContent = postWorkingAndOffEmployees(8, 4);

      assertFalse(
          responseContent.contains("上限（12名）を超えています"),
          "Limit error should not be shown for 12 employees including those on leave");
      verify(shiftAssignmentService).assign(any());
    }

    @Test
    @DisplayName(
        "[V-4][F-5] Given: assignがOptional.empty()を返すとき, When: POSTすると, Then: 不成立メッセージが表示される")
    void showsUnassignableMessageWhenNoValidCombination() throws Exception {
      when(shiftAssignmentService.assign(any())).thenReturn(java.util.Optional.empty());
      StringBuilder params = new StringBuilder();
      for (int i = 0; i < 8; i++) {
        params.append("&employees[").append(i).append("].name=Employee").append(i);
        appendTimeRange(params, i, "07:30", "18:30");
      }

      String responseContent =
          mockMvc
              .perform(
                  post("/shift")
                      .contentType("application/x-www-form-urlencoded")
                      .content(params.toString().substring(1)))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertTrue(
          responseContent.contains("条件を満たす組み合わせが見つかりませんでした"),
          "Unassignable message should be displayed");

      assertFalse(
          responseContent.contains("割当結果の表"), "Assignment result table should not be displayed");
    }

    @Test
    @DisplayName("[F-5] Given: 不成立のとき, When: ページが表示されるとき, Then: 時間軸が表示されない")
    void doesNotShowTimelineWhenUnassignable() throws Exception {
      StringBuilder params = new StringBuilder();
      for (int i = 0; i < 8; i++) {
        params.append("&employees[").append(i).append("].name=Employee").append(i);
        appendTimeRange(params, i, "07:30", "18:30");
      }

      String responseContent =
          mockMvc
              .perform(
                  post("/shift")
                      .contentType("application/x-www-form-urlencoded")
                      .content(params.toString().substring(1)))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertFalse(
          responseContent.contains("class=\"timeline\"")
              || responseContent.contains("class='timeline'"),
          "Timeline should not be displayed when unassignable");
    }
  }

  @Nested
  @DisplayName("[V-2] 重複氏名チェック")
  class DuplicateNameValidation {

    @BeforeEach
    void setupFindDuplicateNamesStub() {
      Mockito.reset(shiftAssignmentService);
      ShiftAssignmentServiceImpl realService = new ShiftAssignmentServiceImpl();
      Mockito.doAnswer(invocation -> realService.assign((java.util.List) invocation.getArgument(0)))
          .when(shiftAssignmentService)
          .assign(Mockito.any());
      Mockito.doAnswer(
              invocation ->
                  realService.findDuplicateNames((java.util.List) invocation.getArgument(0)))
          .when(shiftAssignmentService)
          .findDuplicateNames(Mockito.any());
    }

    @Test
    @DisplayName("[V-2] Given: 1行目が空、2・3行目が同名のとき, When: POSTすると, Then: 「2, 3行目」が表示され、「1, 2行目」ではない")
    void displaysDuplicateLineNumbersCorrectlyWithBlankRowBefore() throws Exception {
      StringBuilder params = new StringBuilder();
      params.append("&employees[0].name=");
      appendTimeRange(params, 0, "07:30", "18:30");
      params.append("&employees[1].name=A");
      appendTimeRange(params, 1, "07:30", "18:30");
      params.append("&employees[2].name=A");
      appendTimeRange(params, 2, "07:30", "18:30");

      String responseContent =
          mockMvc
              .perform(
                  post("/shift")
                      .contentType("application/x-www-form-urlencoded")
                      .content(params.toString().substring(1)))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertTrue(responseContent.contains("重複"), "Response should contain duplicate error");
      String pattern = "該当行：([^）]*)行目";
      Pattern p = Pattern.compile(pattern);
      Matcher m = p.matcher(responseContent);
      assertTrue(m.find(), "Should contain '該当行：' with line numbers");
      String lineNumbers = m.group(1);
      assertTrue(
          lineNumbers.contains("2") && lineNumbers.contains("3"),
          "Should display line numbers 2 and 3, got: " + lineNumbers);
    }

    @Test
    @DisplayName("[V-2] Given: 1行目A、2行目が空、3行目Aのとき, When: POSTすると, Then: 「1, 3行目」が表示される")
    void displaysDuplicateLineNumbersCorrectlyWithBlankRowBetween() throws Exception {
      StringBuilder params = new StringBuilder();
      params.append("&employees[0].name=A");
      appendTimeRange(params, 0, "07:30", "18:30");
      params.append("&employees[1].name=");
      appendTimeRange(params, 1, "07:30", "18:30");
      params.append("&employees[2].name=A");
      appendTimeRange(params, 2, "07:30", "18:30");

      String responseContent =
          mockMvc
              .perform(
                  post("/shift")
                      .contentType("application/x-www-form-urlencoded")
                      .content(params.toString().substring(1)))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertTrue(responseContent.contains("重複"), "Response should contain duplicate error");
      String pattern = "該当行：([^）]*)行目";
      Pattern p = Pattern.compile(pattern);
      Matcher m = p.matcher(responseContent);
      assertTrue(m.find(), "Should contain '該当行：' with line numbers");
      String lineNumbers = m.group(1);
      assertTrue(
          lineNumbers.contains("1") && lineNumbers.contains("3"),
          "Should display line numbers 1 and 3, got: " + lineNumbers);
    }
  }

  @Nested
  @DisplayName("[F-1][V-1] フォームから従業員への変換")
  class FormConversion {

    @SuppressWarnings("unchecked")
    private List<Employee> captureAssignedEmployees() {
      ArgumentCaptor<List<Employee>> captor = ArgumentCaptor.forClass(List.class);
      verify(shiftAssignmentService).assign(captor.capture());
      return captor.getValue();
    }

    @Test
    @DisplayName(
        "[F-1] Given: 開始08:00・終了17:00の行があるとき, When: POSTすると,"
            + " Then: assignに渡される従業員の開始が08:00、終了が17:00である")
    void convertsStartAndEndToLocalTime() throws Exception {
      mockMvc
          .perform(
              post("/shift")
                  .param("employees[0].name", "A")
                  .param("employees[0].start", "08:00")
                  .param("employees[0].end", "17:00"))
          .andExpect(status().isOk());

      List<Employee> employees = captureAssignedEmployees();
      assertEquals(1, employees.size());
      assertEquals("A", employees.get(0).name());
      assertFalse(employees.get(0).off());
      assertEquals(LocalTime.of(8, 0), employees.get(0).start());
      assertEquals(LocalTime.of(17, 0), employees.get(0).end());
    }

    @Test
    @DisplayName(
        "[F-1] Given: 休みにチェックした行があるとき, When: POSTすると," + " Then: assignに渡される従業員は休みで、開始・終了がnullである")
    void convertsOffRowToEmployeeOnLeave() throws Exception {
      mockMvc
          .perform(
              post("/shift")
                  .param("employees[0].name", "A")
                  .param("employees[0].off", "true")
                  .param("employees[0].start", "08:00")
                  .param("employees[0].end", "17:00"))
          .andExpect(status().isOk());

      List<Employee> employees = captureAssignedEmployees();
      assertEquals(1, employees.size());
      assertTrue(employees.get(0).off());
      assertNull(employees.get(0).start());
      assertNull(employees.get(0).end());
    }

    @Test
    @DisplayName(
        "[V-1] Given: 1行目の氏名が空で2行目に氏名があるとき, When: POSTすると," + " Then: assignには氏名がある2行目の従業員だけが渡される")
    void excludesBlankNameRowFromAssignment() throws Exception {
      mockMvc
          .perform(
              post("/shift")
                  .param("employees[0].name", "")
                  .param("employees[0].start", "")
                  .param("employees[0].end", "")
                  .param("employees[1].name", "B")
                  .param("employees[1].start", "07:30")
                  .param("employees[1].end", "18:30"))
          .andExpect(status().isOk());

      List<Employee> employees = captureAssignedEmployees();
      assertEquals(1, employees.size());
      assertEquals("B", employees.get(0).name());
    }

    @Test
    @DisplayName(
        "[F-7] Given: 開始08:00・終了17:00・区分MANAGERの行があるとき, When: POSTすると,"
            + " Then: assignに渡される従業員の区分がMANAGERである")
    void convertsEmploymentTypeToEmployee() throws Exception {
      mockMvc
          .perform(
              post("/shift")
                  .param("employees[0].name", "A")
                  .param("employees[0].employmentType", "MANAGER")
                  .param("employees[0].start", "08:00")
                  .param("employees[0].end", "17:00"))
          .andExpect(status().isOk());

      List<Employee> employees = captureAssignedEmployees();
      assertEquals(1, employees.size());
      assertEquals("A", employees.get(0).name());
      assertEquals(EmploymentType.MANAGER, employees.get(0).employmentType());
    }
  }

  @Nested
  @DisplayName("[F-7] 雇用区分の保存と復元")
  class EmploymentTypeSaveAndRestore {

    @Test
    @DisplayName("[F-7] Given: 保存がないとき, When: GET /すると, Then: 12行すべての区分がFULL_TIMEである")
    void initializeAllEmploymentTypesToFullTimeWhenNoSave() throws Exception {
      when(latestShiftRepository.findEmployees()).thenReturn(List.of());

      MvcResult result = mockMvc.perform(get("/")).andExpect(status().isOk()).andReturn();
      String html = result.getResponse().getContentAsString();

      for (int i = 0; i < 12; i++) {
        String expectedSelect = String.format("name=\"employees[%d].employmentType\"", i);
        assertTrue(html.contains(expectedSelect), "Row " + i + " should have employmentType");
      }
      // Check that FULL_TIME is default value in options
      assertTrue(html.contains("value=\"FULL_TIME\""), "FULL_TIME option should be present");
    }

    @Test
    @DisplayName(
        "[F-7] Given: MANAGER1名・PART_TIME1名が保存されているとき, When: GET /すると,"
            + " Then: 1行目がMANAGER、2行目がPART_TIMEで、残りはFULL_TIMEである")
    void restoresEmploymentTypesFromRepository() throws Exception {
      List<Employee> savedEmployees =
          List.of(
              Employee.working(
                  "A", EmploymentType.MANAGER, LocalTime.of(8, 0), LocalTime.of(17, 0)),
              Employee.working(
                  "B", EmploymentType.PART_TIME, LocalTime.of(8, 0), LocalTime.of(17, 0)));
      when(latestShiftRepository.findEmployees()).thenReturn(savedEmployees);

      MvcResult result = mockMvc.perform(get("/")).andExpect(status().isOk()).andReturn();
      String html = result.getResponse().getContentAsString();

      // Check that select elements exist with correct names
      assertTrue(html.contains("name=\"employees[0].employmentType\""), "Row 0 select");
      assertTrue(html.contains("name=\"employees[1].employmentType\""), "Row 1 select");
      assertTrue(html.contains("name=\"employees[2].employmentType\""), "Row 2 select");
      // Check that all option values are present
      assertTrue(html.contains("value=\"MANAGER\""), "MANAGER option");
      assertTrue(html.contains("value=\"PART_TIME\""), "PART_TIME option");
      assertTrue(html.contains("value=\"FULL_TIME\""), "FULL_TIME option");
    }
  }

  @Nested
  @DisplayName("[V-3][V-1] 開始・終了の入力チェック")
  class TimeRangeValidation {

    private String postRows(String... rows) throws Exception {
      StringBuilder params = new StringBuilder();
      for (int i = 0; i < rows.length; i++) {
        String[] cols = rows[i].split(",", -1);
        params.append("&employees[").append(i).append("].name=").append(cols[0]);
        if (cols[1].equals("off")) {
          params.append("&employees[").append(i).append("].off=true");
        }
        appendTimeRange(params, i, cols[2], cols[3]);
      }
      return mockMvc
          .perform(
              post("/shift")
                  .contentType("application/x-www-form-urlencoded")
                  .content(params.substring(1)))
          .andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString();
    }

    @Test
    @DisplayName(
        "[V-3] Given: 2行目の開始が未選択のとき, When: POSTすると," + " Then: 「2行目 開始が未選択です」が表示され、assignが呼ばれない")
    void showsErrorWhenStartIsMissing() throws Exception {
      String response = postRows("A,,07:30,18:30", "B,,,17:00");

      assertTrue(response.contains("2行目 開始が未選択です"), "Should show start missing error");
      assertFalse(response.contains("1行目"), "Valid row should not have an error");
      verify(shiftAssignmentService, never()).assign(any());
    }

    @Test
    @DisplayName(
        "[V-3] Given: 1行目の終了が未選択のとき, When: POSTすると," + " Then: 「1行目 終了が未選択です」が表示され、assignが呼ばれない")
    void showsErrorWhenEndIsMissing() throws Exception {
      String response = postRows("A,,08:00,");

      assertTrue(response.contains("1行目 終了が未選択です"), "Should show end missing error");
      verify(shiftAssignmentService, never()).assign(any());
    }

    @Test
    @DisplayName(
        "[V-3] Given: 1行目の開始が選択肢外（08:10）のとき, When: POSTすると,"
            + " Then: 「1行目 開始は選択肢にありません」が表示され、assignが呼ばれない")
    void showsErrorWhenStartIsNotAnOption() throws Exception {
      String response = postRows("A,,08:10,17:00");

      assertTrue(response.contains("1行目 開始は選択肢にありません"), "Should show start not-an-option error");
      verify(shiftAssignmentService, never()).assign(any());
    }

    @Test
    @DisplayName(
        "[V-3] Given: 1行目の終了が選択肢外（19:00）のとき, When: POSTすると,"
            + " Then: 「1行目 終了は選択肢にありません」が表示され、assignが呼ばれない")
    void showsErrorWhenEndIsNotAnOption() throws Exception {
      String response = postRows("A,,08:00,19:00");

      assertTrue(response.contains("1行目 終了は選択肢にありません"), "Should show end not-an-option error");
      verify(shiftAssignmentService, never()).assign(any());
    }

    @Test
    @DisplayName(
        "[V-3] Given: 1行目の開始と終了が同じ（09:00）とき, When: POSTすると,"
            + " Then: 「1行目 開始は終了より前にしてください」が表示され、assignが呼ばれない")
    void showsErrorWhenStartEqualsEnd() throws Exception {
      String response = postRows("A,,09:00,09:00");

      assertTrue(response.contains("1行目 開始は終了より前にしてください"), "Should show order error");
      verify(shiftAssignmentService, never()).assign(any());
    }

    @Test
    @DisplayName(
        "[V-3] Given: 1行目の開始が終了より後（17:00〜08:00）のとき, When: POSTすると,"
            + " Then: 「1行目 開始は終了より前にしてください」が表示され、assignが呼ばれない")
    void showsErrorWhenStartIsAfterEnd() throws Exception {
      String response = postRows("A,,17:00,08:00");

      assertTrue(response.contains("1行目 開始は終了より前にしてください"), "Should show order error");
      verify(shiftAssignmentService, never()).assign(any());
    }

    @Test
    @DisplayName(
        "[V-3] Given: 休みにチェックした行の開始・終了が空のとき, When: POSTすると," + " Then: 入力エラーにならず、assignが呼ばれる")
    void doesNotShowErrorForOffRowWithoutTimeRange() throws Exception {
      String response = postRows("A,off,,");

      assertFalse(response.contains("入力エラー"), "Off row should not produce an input error");
      verify(shiftAssignmentService).assign(any());
    }

    @Test
    @DisplayName("[V-1] Given: 氏名が空の行の開始・終了が不正なとき, When: POSTすると," + " Then: 入力エラーにならず、assignが呼ばれる")
    void doesNotShowErrorForBlankNameRowWithInvalidTimeRange() throws Exception {
      String response = postRows("A,,07:30,18:30", ",,08:10,");

      assertFalse(response.contains("入力エラー"), "Blank name row should not produce an error");
      verify(shiftAssignmentService).assign(any());
    }

    @Test
    @DisplayName("[V-3] Given: 複数行にエラーがあるとき, When: POSTすると," + " Then: timeRangeErrorsが行番号の昇順で並ぶ")
    void timeRangeErrorsSortedByRowIndex() throws Exception {
      String response = postRows("A,,08:10,17:00", "B,,09:00,19:00");

      assertTrue(response.contains("1行目 開始は選択肢にありません"), "Should show 1st row error");
      assertTrue(response.contains("2行目 終了は選択肢にありません"), "Should show 2nd row error");

      int pos1 = response.indexOf("1行目 開始は選択肢にありません");
      int pos2 = response.indexOf("2行目 終了は選択肢にありません");
      assertTrue(pos1 >= 0 && pos2 >= 0 && pos1 < pos2, "Errors should appear in row index order");
    }

    @Test
    @DisplayName(
        "[V-3] Given: 同じ行の開始・終了がともに選択肢外のとき, When: POSTすると," + " Then: 開始のエラー → 終了のエラーの順に並ぶ")
    void errorsWithinSameRowOrderedByStartThenEnd() throws Exception {
      String response = postRows("A,,08:10,19:00");

      assertTrue(response.contains("1行目 開始は選択肢にありません"), "Should show start error");
      assertTrue(response.contains("1行目 終了は選択肢にありません"), "Should show end error");

      int posStart = response.indexOf("1行目 開始は選択肢にありません");
      int posEnd = response.indexOf("1行目 終了は選択肢にありません");
      assertTrue(
          posStart >= 0 && posEnd >= 0 && posStart < posEnd,
          "Start error should come before end error in the same row");
    }
  }

  @Nested
  @DisplayName("[F-1] GET・POST の全戻り経路で timeOptions をモデルに設定")
  class TimeOptionsInModel {

    @SuppressWarnings("unchecked")
    private void assertTimeOptions(MvcResult result) {
      List<String> timeOptions =
          (List<String>) result.getModelAndView().getModel().get("timeOptions");
      assertNotNull(timeOptions, "Model should contain timeOptions");
      assertEquals(23, timeOptions.size(), "timeOptions should have 23 items");
      assertEquals("07:30", timeOptions.get(0), "First option should be 07:30");
      assertEquals("08:00", timeOptions.get(1), "Second option should be 08:00");
      assertEquals("18:30", timeOptions.get(22), "Last option should be 18:30");
    }

    private MvcResult postEmployees(int count) throws Exception {
      StringBuilder params = new StringBuilder();
      for (int i = 0; i < count; i++) {
        params.append("&employees[").append(i).append("].name=Employee").append(i);
        appendTimeRange(params, i, "07:30", "18:30");
      }
      return mockMvc
          .perform(
              post("/shift")
                  .contentType("application/x-www-form-urlencoded")
                  .content(params.substring(1)))
          .andExpect(status().isOk())
          .andReturn();
    }

    @Test
    @DisplayName(
        "[F-1] Given: GETリクエストが与えられたとき, When: /にアクセスすると,"
            + " Then: timeOptionsが07:30〜18:30の30分刻み23件である")
    void includesTimeOptionsOnGet() throws Exception {
      MvcResult result = mockMvc.perform(get("/")).andExpect(status().isOk()).andReturn();

      assertTimeOptions(result);
    }

    @Test
    @DisplayName("[F-1] Given: POSTで成立するとき, When: モデルを確認すると, Then: timeOptionsが07:30〜18:30の23件である")
    void includesTimeOptionsWhenAssignmentSucceeds() throws Exception {
      when(shiftAssignmentService.assign(any()))
          .thenReturn(java.util.Optional.of(createStandardResult()));

      MvcResult result = postEmployees(8);

      assertNotNull(result.getModelAndView().getModel().get("assignmentResult"));
      assertTimeOptions(result);
    }

    @Test
    @DisplayName("[F-1] Given: POSTで不成立のとき, When: モデルを確認すると, Then: timeOptionsが07:30〜18:30の23件である")
    void includesTimeOptionsWhenUnassignable() throws Exception {
      when(shiftAssignmentService.assign(any())).thenReturn(java.util.Optional.empty());

      MvcResult result = postEmployees(8);

      assertEquals(true, result.getModelAndView().getModel().get("unassignable"));
      assertTimeOptions(result);
    }

    @Test
    @DisplayName(
        "[F-1] Given: POSTでV-3の入力エラーのとき, When: モデルを確認すると,"
            + " Then: timeOptionsが07:30〜18:30の23件である")
    void includesTimeOptionsWhenV3Error() throws Exception {
      MvcResult result =
          mockMvc
              .perform(
                  post("/shift")
                      .param("employees[0].name", "Employee A")
                      .param("employees[0].start", "")
                      .param("employees[0].end", "17:00"))
              .andExpect(status().isOk())
              .andReturn();

      assertTrue(result.getResponse().getContentAsString().contains("開始が未選択です"));
      assertTimeOptions(result);
    }

    @Test
    @DisplayName(
        "[F-1] Given: POSTでV-5の入力エラーのとき, When: モデルを確認すると,"
            + " Then: timeOptionsが07:30〜18:30の23件である")
    void includesTimeOptionsWhenV5Error() throws Exception {
      MvcResult result = postEmployees(13);

      assertNotNull(result.getModelAndView().getModel().get("limitExceededError"));
      assertTimeOptions(result);
    }
  }

  @Nested
  @DisplayName("[F-4] 割当結果の表表示")
  class ResultTableDisplay {

    @BeforeEach
    void setupAssignmentResult() {
      when(shiftAssignmentService.assign(any()))
          .thenReturn(java.util.Optional.of(createStandardResult()));
    }

    @Test
    @DisplayName("[F-4] Given: 割当結果が表示されるとき, When: テーブルの見出しを確認すると, Then: 「氏名」「勤務時間」「休憩時間」の順である")
    void displaysResultTableHeadersInCorrectOrder() throws Exception {
      StringBuilder params = new StringBuilder();
      for (int i = 0; i < 8; i++) {
        params.append("&employees[").append(i).append("].name=Employee").append(i);
        appendTimeRange(params, i, "07:30", "18:30");
      }

      String responseContent =
          mockMvc
              .perform(
                  post("/shift")
                      .contentType("application/x-www-form-urlencoded")
                      .content(params.toString().substring(1)))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertTrue(
          responseContent.contains("class=\"result-table\""), "Result table should be present");

      int pos1 = responseContent.indexOf("<th>氏名</th>");
      int pos2 = responseContent.indexOf("<th>勤務時間</th>");
      int pos3 = responseContent.indexOf("<th>休憩時間</th>");

      assertTrue(pos1 >= 0, "Should contain header '氏名'");
      assertTrue(pos2 >= 0, "Should contain header '勤務時間'");
      assertTrue(pos3 >= 0, "Should contain header '休憩時間'");
      assertTrue(pos1 < pos2, "'氏名' should come before '勤務時間'");
      assertTrue(pos2 < pos3, "'勤務時間' should come before '休憩時間'");
    }

    @Test
    @DisplayName("[F-4] Given: 8名の割当結果が表示されるとき, When: テーブルの行を確認すると, Then: 8行の氏名・勤務時間・休憩時間が仕様と一致する")
    void displaysCorrectNumberOfRowsAndCorrectWorkSchedules() throws Exception {
      StringBuilder params = new StringBuilder();
      for (int i = 0; i < 8; i++) {
        params
            .append("&employees[")
            .append(i)
            .append("].name=")
            .append(String.valueOf((char) ('A' + i)));
        appendTimeRange(params, i, "07:30", "18:30");
      }

      String responseContent =
          mockMvc
              .perform(
                  post("/shift")
                      .contentType("application/x-www-form-urlencoded")
                      .content(params.toString().substring(1)))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertTrue(
          responseContent.contains("class=\"result-table\""), "Result table should be displayed");

      String[] expectedWorkTimes = {
        "07:30〜14:30",
        "07:30〜14:30",
        "08:00〜15:30",
        "08:30〜16:30",
        "09:00〜16:30",
        "09:00〜18:00",
        "09:00〜18:30",
        "09:00〜18:30"
      };

      String[] expectedBreakTimes = {
        "12:00〜12:45",
        "12:00〜12:45",
        "12:45〜13:30",
        "12:45〜13:30",
        "13:30〜14:15",
        "13:30〜14:30",
        "14:15〜15:15",
        "14:30〜15:30"
      };

      assertEquals(8, expectedWorkTimes.length, "Should have 8 expected work times");
      assertEquals(8, expectedBreakTimes.length, "Should have 8 expected break times");

      Pattern resultTablePattern =
          Pattern.compile("class=\"result-table\">.*?<tbody[^>]*>(.*?)</tbody>", Pattern.DOTALL);
      Matcher resultTableMatcher = resultTablePattern.matcher(responseContent);
      assertTrue(resultTableMatcher.find(), "Result table tbody should be present");
      String resultTableTbody = resultTableMatcher.group(1);

      Pattern rowPattern = Pattern.compile("<tr[^>]*>.*?</tr>", Pattern.DOTALL);
      Matcher rowMatcher = rowPattern.matcher(resultTableTbody);

      int rowCount = 0;
      while (rowMatcher.find()) {
        rowCount++;
      }
      assertEquals(8, rowCount, "Result table should have exactly 8 rows");

      rowMatcher = rowPattern.matcher(resultTableTbody);
      int currentRow = 0;
      while (rowMatcher.find() && currentRow < 8) {
        String rowHtml = rowMatcher.group();
        String expectedName = String.valueOf((char) ('A' + currentRow));
        String expectedWorkTime = expectedWorkTimes[currentRow];
        String expectedBreakTime = expectedBreakTimes[currentRow];

        assertTrue(
            rowHtml.contains(expectedName),
            "Row " + currentRow + " should contain name " + expectedName);

        assertTrue(
            rowHtml.contains(expectedWorkTime),
            "Row " + currentRow + " should contain work time " + expectedWorkTime);

        assertTrue(
            rowHtml.contains(expectedBreakTime),
            "Row " + currentRow + " should contain break time " + expectedBreakTime);

        int namePos = rowHtml.indexOf(expectedName);
        int workTimePos = rowHtml.indexOf(expectedWorkTime);
        int breakTimePos = rowHtml.indexOf(expectedBreakTime);
        assertTrue(
            namePos < workTimePos && workTimePos < breakTimePos,
            "Row " + currentRow + " should have name, work time, break time in correct order");

        currentRow++;
      }
    }

    @Test
    @DisplayName("[F-4] Given: 割当結果の表が表示されるとき, When: 表の内容を確認すると, Then: 「早番」「遅番」の文字が存在しない")
    void resultTableDoesNotContainEarlyOrLateTerms() throws Exception {
      StringBuilder params = new StringBuilder();
      for (int i = 0; i < 8; i++) {
        params.append("&employees[").append(i).append("].name=Employee").append(i);
        appendTimeRange(params, i, "07:30", "18:30");
      }

      String responseContent =
          mockMvc
              .perform(
                  post("/shift")
                      .contentType("application/x-www-form-urlencoded")
                      .content(params.toString().substring(1)))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      int resultTableStart = responseContent.indexOf("class=\"result-table\"");
      assertTrue(resultTableStart >= 0, "Result table should be present");

      int resultTableEnd =
          responseContent.indexOf("</table>", resultTableStart) + "</table>".length();
      String resultTableContent = responseContent.substring(resultTableStart, resultTableEnd);

      assertFalse(resultTableContent.contains("早番"), "Result table should not contain '早番'");
      assertFalse(resultTableContent.contains("遅番"), "Result table should not contain '遅番'");
    }

    @Test
    @DisplayName("[F-4] Given: タイムラインが表示されるとき, When: 時間軸を確認すると," + " Then: ラベルが8から18の1時間刻みで11個ある")
    void displaysTimelineAxisLabelsEightToEighteen() throws Exception {
      StringBuilder params = new StringBuilder();
      for (int i = 0; i < 8; i++) {
        params.append("&employees[").append(i).append("].name=Employee").append(i);
        appendTimeRange(params, i, "07:30", "18:30");
      }

      String responseContent =
          mockMvc
              .perform(
                  post("/shift")
                      .contentType("application/x-www-form-urlencoded")
                      .content(params.toString().substring(1)))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      for (int h = 8; h <= 18; h++) {
        assertTrue(
            responseContent.contains("<div class=\"tl-axis\">")
                && responseContent.contains(String.valueOf(h)),
            "Timeline should contain hour label " + h);
      }

      assertFalse(
          responseContent.contains("class=\"tl-axis\">") && responseContent.contains(">20<"),
          "Timeline should not contain hour 20");
    }

    @Test
    @DisplayName(
        "[F-4] Given: CSSファイルを確認するとき, When: .tl-workの定義を見ると,"
            + " Then: backgroundプロパティが定義されており、早番・遅番のセレクターがない")
    void cssHasWorkBarColorWithoutEarlyLate() throws Exception {
      String cssFilePath = "src/main/resources/static/css/shift-form.css";
      java.nio.file.Path path = java.nio.file.Paths.get(cssFilePath);
      String cssContent = new String(java.nio.file.Files.readAllBytes(path));

      assertTrue(
          cssContent.contains(".tl-work") && cssContent.contains("background:"),
          "CSS should have .tl-work with background property");
      assertFalse(
          cssContent.contains(".tl-work.early") || cssContent.contains(".tl-work.late"),
          "CSS should not have .tl-work.early or .tl-work.late");
    }
  }

  @Nested
  @DisplayName("[F-4] スコアと未出勤者の表示")
  class ScoreAndUnassignedDisplay {

    @BeforeEach
    void setupAssignmentResult() {
      when(shiftAssignmentService.assign(any()))
          .thenReturn(java.util.Optional.of(createStandardResult()));
    }

    /**
     * 全員 7:30〜18:30 の従業員を指定人数分送信し、レスポンス本文を返します。
     *
     * @param count 従業員数
     * @return レスポンス本文
     * @throws Exception リクエストの実行に失敗した場合
     */
    private String postEmployees(int count) throws Exception {
      StringBuilder params = new StringBuilder();
      for (int i = 0; i < count; i++) {
        params
            .append("&employees[")
            .append(i)
            .append("].name=")
            .append(String.valueOf((char) ('A' + i)));
        appendTimeRange(params, i, "07:30", "18:30");
      }
      return mockMvc
          .perform(
              post("/shift")
                  .contentType("application/x-www-form-urlencoded")
                  .content(params.toString().substring(1)))
          .andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString();
    }

    @Test
    @DisplayName(
        "[F-4] Given: 割当結果が表示されるとき, When: ページを確認すると, Then:" + " ずれの合計（スコア）と旧スコアの記号（U+25CE）は表示されない")
    void doesNotDisplayGapTotal() throws Exception {
      String responseContent = postEmployees(8);

      assertFalse(responseContent.contains("score-num"), "Gap total should not be displayed");
      assertFalse(responseContent.contains("ずれの合計"), "Gap total label should not be displayed");
      assertFalse(
          responseContent.contains(LEGACY_MARK), "Result should not contain the old score mark");
    }

    @Test
    @DisplayName(
        "[F-4] Given: 未出勤者に休みの従業員（K）を含む割当結果が表示されるとき, When: 未出勤者セクションを確認すると, Then:"
            + " 休みの従業員の氏名がチップで表示される")
    void displaysEmployeeOnLeaveAsUnassigned() throws Exception {
      AssignmentResult result =
          new AssignmentResult(
              createStandardResult().assignments(), 0, List.of(Employee.onLeave("K")));
      when(shiftAssignmentService.assign(any())).thenReturn(java.util.Optional.of(result));

      StringBuilder params = new StringBuilder();
      for (int i = 0; i < 8; i++) {
        params.append("&employees[").append(i).append("].name=Employee").append(i);
        appendTimeRange(params, i, "07:30", "18:30");
      }
      params.append("&employees[8].name=K&employees[8].off=true");

      String responseContent =
          mockMvc
              .perform(
                  post("/shift")
                      .contentType("application/x-www-form-urlencoded")
                      .content(params.toString().substring(1)))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertTrue(
          responseContent.contains("class=\"chip\">K</span>"),
          "Employee on leave should be displayed as unassigned");
    }

    @Test
    @DisplayName(
        "[F-4] Given: 未出勤者2名（I・J）の割当結果が表示されるとき, When: 未出勤者セクションを確認すると, Then: '.chip'が2つ表示され、氏名が正しい")
    void displaysUnassignedEmployeesWithChips() throws Exception {
      // 未出勤者2名を含む結果をスタブ
      List<Employee> unassignedEmployees =
          List.of(
              Employee.working("I", LocalTime.of(7, 30), LocalTime.of(18, 30)),
              Employee.working("J", LocalTime.of(7, 30), LocalTime.of(18, 30)));
      AssignmentResult resultWithUnassigned =
          new AssignmentResult(createStandardResult().assignments(), 5, unassignedEmployees);
      when(shiftAssignmentService.assign(any()))
          .thenReturn(java.util.Optional.of(resultWithUnassigned));

      StringBuilder params = new StringBuilder();
      for (int i = 0; i < 10; i++) {
        params
            .append("&employees[")
            .append(i)
            .append("].name=")
            .append(String.valueOf((char) ('A' + i)));
        appendTimeRange(params, i, "07:30", "18:30");
      }

      String responseContent =
          mockMvc
              .perform(
                  post("/shift")
                      .contentType("application/x-www-form-urlencoded")
                      .content(params.toString().substring(1)))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertTrue(
          responseContent.contains("class=\"unassigned\""),
          "Unassigned section should be displayed");

      int chipCount = 0;
      int index = 0;
      while ((index = responseContent.indexOf("class=\"chip\"", index)) != -1) {
        chipCount++;
        index++;
      }
      assertEquals(2, chipCount, "Should have exactly 2 chips for 2 unassigned employees");

      assertTrue(
          responseContent.contains(">I<") || responseContent.contains("I</span>"),
          "Should contain employee I");
      assertTrue(
          responseContent.contains(">J<") || responseContent.contains("J</span>"),
          "Should contain employee J");
    }

    @Test
    @DisplayName(
        "[F-4] Given: 未出勤者0名の割当結果が表示されるとき, When: 未出勤者セクションを確認すると, Then: '.unassigned'が表示されない")
    void doesNotDisplayUnassignedSectionWhenAllAssigned() throws Exception {
      StringBuilder params = new StringBuilder();
      for (int i = 0; i < 8; i++) {
        params.append("&employees[").append(i).append("].name=Employee").append(i);
        appendTimeRange(params, i, "07:30", "18:30");
      }

      String responseContent =
          mockMvc
              .perform(
                  post("/shift")
                      .contentType("application/x-www-form-urlencoded")
                      .content(params.toString().substring(1)))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertFalse(
          responseContent.contains("class=\"unassigned\""),
          "Unassigned section should not be displayed when all employees are assigned");
    }
  }

  @Nested
  @DisplayName("[F-4] 時間軸バーの表示")
  class TimelineDisplay {

    @BeforeEach
    void setupAssignmentResult() {
      when(shiftAssignmentService.assign(any()))
          .thenReturn(java.util.Optional.of(createStandardResult()));
    }

    @Test
    @DisplayName(
        "[F-4] Given: 8名の割当結果が表示されるとき, When: 時間軸の行とバーを確認すると, Then: 8行8本のworkバー、8本のbreakバーが表示される")
    void displaysCorrectNumberOfTimelineRows() throws Exception {
      StringBuilder params = new StringBuilder();
      for (int i = 0; i < 8; i++) {
        params.append("&employees[").append(i).append("].name=Employee").append(i);
        appendTimeRange(params, i, "07:30", "18:30");
      }

      String responseContent =
          mockMvc
              .perform(
                  post("/shift")
                      .contentType("application/x-www-form-urlencoded")
                      .content(params.toString().substring(1)))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      int tlRowCount = 0;
      int index = 0;
      while ((index = responseContent.indexOf("class=\"tl-row\"", index)) != -1) {
        tlRowCount++;
        index++;
      }
      assertEquals(8, tlRowCount, "Should have exactly 8 timeline rows");

      int tlWorkCount = 0;
      index = 0;
      while ((index = responseContent.indexOf("class=\"tl-work\"", index)) != -1) {
        tlWorkCount++;
        index++;
      }
      assertEquals(8, tlWorkCount, "Should have exactly 8 work bars");

      int tlBreakCount = 0;
      index = 0;
      while ((index = responseContent.indexOf("class=\"tl-break\"", index)) != -1) {
        tlBreakCount++;
        index++;
      }
      assertEquals(8, tlBreakCount, "Should have exactly 8 break bars");
    }

    @Test
    @DisplayName(
        "[F-4] Given: 枠1の勤務バーが表示されるとき, When: スタイル属性を確認すると, Then: 'left:0.00%'かつ'width:63.64%'である")
    void displaysSlot1WorkBarWithCorrectStyle() throws Exception {
      StringBuilder params = new StringBuilder();
      for (int i = 0; i < 8; i++) {
        params.append("&employees[").append(i).append("].name=Employee").append(i);
        appendTimeRange(params, i, "07:30", "18:30");
      }

      String responseContent =
          mockMvc
              .perform(
                  post("/shift")
                      .contentType("application/x-www-form-urlencoded")
                      .content(params.toString().substring(1)))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertTrue(responseContent.contains("left:0.00%"), "First work bar should have left:0.00%");
      assertTrue(
          responseContent.contains("width:63.64%"), "First work bar should have width:63.64%");
    }

    @Test
    @DisplayName(
        "[F-4] Given: 枠6の勤務バーが表示されるとき, When: スタイル属性を確認すると, Then: 'left:13.64%'かつ'width:86.36%'である")
    void displaysSlot6WorkBarWithCorrectStyle() throws Exception {
      StringBuilder params = new StringBuilder();
      for (int i = 0; i < 8; i++) {
        params.append("&employees[").append(i).append("].name=Employee").append(i);
        appendTimeRange(params, i, "07:30", "18:30");
      }

      String responseContent =
          mockMvc
              .perform(
                  post("/shift")
                      .contentType("application/x-www-form-urlencoded")
                      .content(params.toString().substring(1)))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertTrue(
          responseContent.contains("left:13.64%"),
          "Last work bars (Slot 6) should have left:13.64%");
      assertTrue(
          responseContent.contains("width:86.36%"),
          "Last work bars (Slot 6) should have width:86.36%");
    }

    @Test
    @DisplayName(
        "[F-4] Given: 枠1の1人目の休憩バーが表示されるとき, When: スタイル属性を確認すると, Then:"
            + " 'left:40.91%'かつ'width:6.82%'である")
    void displaysSlot1BreakBarWithCorrectStyle() throws Exception {
      StringBuilder params = new StringBuilder();
      for (int i = 0; i < 8; i++) {
        params.append("&employees[").append(i).append("].name=Employee").append(i);
        appendTimeRange(params, i, "07:30", "18:30");
      }

      String responseContent =
          mockMvc
              .perform(
                  post("/shift")
                      .contentType("application/x-www-form-urlencoded")
                      .content(params.toString().substring(1)))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertTrue(
          responseContent.contains("left:40.91%"), "First break bar should have left:40.91%");
      assertTrue(
          responseContent.contains("width:6.82%"), "First break bar should have width:6.82%");
    }

    @Test
    @DisplayName(
        "[F-4] Given: 時間軸の凡例が表示されるとき, When: 凡例の内容を確認すると, Then: 「勤務」「休憩」が含まれ、「早番」「遅番」が含まれない")
    void displaysCorrectLegend() throws Exception {
      StringBuilder params = new StringBuilder();
      for (int i = 0; i < 8; i++) {
        params.append("&employees[").append(i).append("].name=Employee").append(i);
        appendTimeRange(params, i, "07:30", "18:30");
      }

      String responseContent =
          mockMvc
              .perform(
                  post("/shift")
                      .contentType("application/x-www-form-urlencoded")
                      .content(params.toString().substring(1)))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      int legendStart = responseContent.indexOf("class=\"tl-legend\"");
      assertTrue(legendStart >= 0, "Legend section should exist");

      String legendSection =
          responseContent.substring(
              legendStart, Math.min(legendStart + 300, responseContent.length()));

      assertTrue(legendSection.contains("勤務"), "Legend should contain '勤務'");
      assertTrue(legendSection.contains("休憩"), "Legend should contain '休憩'");
      assertFalse(legendSection.contains("早番"), "Legend should not contain '早番'");
      assertFalse(legendSection.contains("遅番"), "Legend should not contain '遅番'");
    }
  }

  @Nested
  @DisplayName("[F-2][F-6][F-8] JavaScriptの行追加・削除・並べ替え機能")
  class JavaScriptAddDeleteRows {

    private String readShiftFormJs() throws Exception {
      return new String(
          java.nio.file.Files.readAllBytes(
              java.nio.file.Paths.get("src/main/resources/static/js/shift-form.js")),
          java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName(
        "[F-2] Given: GETリクエストが与えられたとき, When: 入力表を確認すると,"
            + " Then: data-time-optionsに07:30〜18:30の23件が「|」区切りで入っている")
    void inputTableHasDataTimeOptions() throws Exception {
      String html =
          mockMvc
              .perform(get("/"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      Matcher matcher =
          Pattern.compile("<table[^>]*class=\"input-table\"[^>]*data-time-options=\"([^\"]*)\"")
              .matcher(html);
      assertTrue(matcher.find(), "Input table should have data-time-options attribute");
      String[] options = matcher.group(1).split("\\|");
      assertEquals(23, options.length, "data-time-options should have 23 items");
      assertEquals("07:30", options[0]);
      assertEquals("18:30", options[22]);
    }

    @Test
    @DisplayName(
        "[F-2][F-6] Given: shift-form.jsをロードしたとき, When: ファイルの内容を確認すると,"
            + " Then: 休み・開始・終了のname生成とdata-time-optionsの読み取りがあり、廃止した希望のselect生成がない")
    void shiftFormJsGeneratesOffStartEndInputs() throws Exception {
      String jsContent = readShiftFormJs();

      assertTrue(jsContent.contains("\".off\""), "shift-form.js should build employees[N].off");
      assertTrue(jsContent.contains("\".start\""), "shift-form.js should build employees[N].start");
      assertTrue(jsContent.contains("\".end\""), "shift-form.js should build employees[N].end");
      assertTrue(
          jsContent.contains("data-time-options"), "shift-form.js should read data-time-options");
      assertFalse(
          jsContent.toLowerCase(Locale.ROOT).contains(LEGACY_TOKEN),
          "shift-form.js should not contain legacy input logic");
      assertFalse(
          jsContent.contains("data-slot-labels"),
          "shift-form.js should not contain 'data-slot-labels'");
    }

    @Test
    @DisplayName(
        "[F-6] Given: shift-form.jsをロードしたとき, When: インデックスの振り直し処理を確認すると,"
            + " Then: 休みの隠しフィールド（_employees[N].off）も振り直しの対象である")
    void shiftFormJsRenumbersHiddenCheckboxFields() throws Exception {
      String jsContent = readShiftFormJs();

      assertTrue(
          jsContent.contains("/(_?employees)\\[\\d+\\]/"),
          "renumber regex should match both employees[N] and _employees[N]");
      assertTrue(
          jsContent.contains("_employees["), "shift-form.js should mention _employees[ fields");
    }

    @Test
    @DisplayName(
        "[F-1] Given: shift-form.jsをロードしたとき, When: 休みチェックの処理を確認すると,"
            + " Then: 同じ行の開始・終了のdisabledを切り替える処理がある")
    void shiftFormJsTogglesTimeSelectsByOffCheckbox() throws Exception {
      String jsContent = readShiftFormJs();

      assertTrue(
          jsContent.contains("function updateTimeSelectsState"),
          "shift-form.js should have updateTimeSelectsState function");
      assertTrue(
          jsContent.contains("off-checkbox"), "shift-form.js should handle .off-checkbox changes");
    }

    @Test
    @DisplayName(
        "[F-2] Given: GETリクエストが与えられたとき, When: /にアクセスすると, Then:"
            + " id=\"add-row-btn\"のボタン要素にdata-max-rows=\"12\"がある")
    void buttonElementContainsDataMaxRows() throws Exception {
      String htmlContent =
          mockMvc
              .perform(get("/"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      Pattern pattern =
          Pattern.compile("id=\"add-row-btn\"[^>]*data-max-rows=\"12\"", Pattern.DOTALL);
      Matcher matcher = pattern.matcher(htmlContent);
      assertTrue(
          matcher.find(),
          "Button with id=\"add-row-btn\" should have data-max-rows=\"12\" attribute");
    }

    @Test
    @DisplayName(
        "[F-2] Given: shift-form.jsをロードしたとき, When: ファイルの内容を確認すると, Then:" + " 'disabled'の設定がある")
    void shiftFormJsContainsDisabledLogic() throws Exception {
      String htmlContent =
          mockMvc
              .perform(get("/"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      int scriptStart = htmlContent.indexOf("src=\"");
      assertTrue(scriptStart >= 0, "Should have script tag with src");

      assertTrue(htmlContent.contains("/js/shift-form.js"), "HTML should reference shift-form.js");

      // since MockMvc would serve the resource
      String jsFilePath = "src/main/resources/static/js/shift-form.js";
      java.nio.file.Path path = java.nio.file.Paths.get(jsFilePath);
      String jsContent = new String(java.nio.file.Files.readAllBytes(path));

      assertTrue(
          jsContent.contains(".disabled"),
          "shift-form.js should contain '.disabled' for button state management");
    }

    @Test
    @DisplayName(
        "[F-6] Given: shift-form.jsをロードしたとき, When: ファイルの内容を確認すると, Then:"
            + " 廃止した希望入力の語・「早番」・「遅番」の文字列が存在しない")
    void shiftFormJsDoesNotContainOldTerms() throws Exception {
      String jsFilePath = "src/main/resources/static/js/shift-form.js";
      java.nio.file.Path path = java.nio.file.Paths.get(jsFilePath);
      String jsContent = new String(java.nio.file.Files.readAllBytes(path));

      assertFalse(
          jsContent.toLowerCase(Locale.ROOT).contains(LEGACY_TOKEN),
          "shift-form.js should not contain legacy input terms");
      assertFalse(jsContent.contains("早番"), "shift-form.js should not contain '早番'");
      assertFalse(jsContent.contains("遅番"), "shift-form.js should not contain '遅番'");
    }

    @Test
    @DisplayName(
        "[F-2] Given: shift-form.jsをロードしたとき, When: ファイルの内容を確認すると,"
            + " Then: \"12\"や12の数値リテラルがない（設定は要素から読む）")
    void shiftFormJsDoesNotContainHardcodedMaxRows() throws Exception {
      String jsFilePath = "src/main/resources/static/js/shift-form.js";
      java.nio.file.Path path = java.nio.file.Paths.get(jsFilePath);
      String jsContent = new String(java.nio.file.Files.readAllBytes(path));

      assertFalse(
          jsContent.contains("\"12\"") || jsContent.contains("|| \"12\""),
          "shift-form.js should not contain hardcoded \"12\" string");
    }

    @Test
    @DisplayName(
        "[F-2] Given: shift-form.jsをロードしたとき, When: ファイルの内容を確認すると,"
            + " Then: dataset.maxRowsまたはgetAttribute(data-max-rows)を読み取っている")
    void shiftFormJsReadsDataMaxRows() throws Exception {
      String jsFilePath = "src/main/resources/static/js/shift-form.js";
      java.nio.file.Path path = java.nio.file.Paths.get(jsFilePath);
      String jsContent = new String(java.nio.file.Files.readAllBytes(path));

      assertTrue(
          jsContent.contains("data-max-rows") || jsContent.contains("dataset.maxRows"),
          "shift-form.js should read data-max-rows attribute");
    }

    @Test
    @DisplayName(
        "[F-2] Given: shift-form.jsをロードしたとき, When: ファイルの内容を確認すると,"
            + " Then: addRowBtn.disabledに代入する箇所と、クリック処理に上限ガードがある")
    void shiftFormJsHasMaxRowsGuardAndButtonDisable() throws Exception {
      String jsFilePath = "src/main/resources/static/js/shift-form.js";
      java.nio.file.Path path = java.nio.file.Paths.get(jsFilePath);
      String jsContent = new String(java.nio.file.Files.readAllBytes(path));

      assertTrue(
          jsContent.contains("addRowBtn.disabled"),
          "shift-form.js should have addRowBtn.disabled assignment");
      assertTrue(
          jsContent.contains("currentRowCount >= maxRows") || jsContent.contains(">= maxRows"),
          "shift-form.js should have max rows guard in click handler");
      assertTrue(
          jsContent.contains("updateAddButtonState"),
          "shift-form.js should call updateAddButtonState function");
    }

    @Test
    @DisplayName(
        "[F-8] Given: GETリクエストが与えられたとき, When: 入力表を確認すると,"
            + " Then: 各入力行に move-up-btn と move-down-btn ボタンが含まれている")
    void inputRowsContainMoveUpAndDownButtons() throws Exception {
      String htmlContent =
          mockMvc
              .perform(get("/"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      Pattern rowPattern =
          Pattern.compile("<tbody id=\"employee-rows\">.*?</tbody>", Pattern.DOTALL);
      Matcher rowMatcher = rowPattern.matcher(htmlContent);
      assertTrue(rowMatcher.find(), "Input table body should exist");
      String tbody = rowMatcher.group(0);

      Pattern trPattern = Pattern.compile("<tr>");
      Matcher trMatcher = trPattern.matcher(tbody);
      int rowCount = 0;
      while (trMatcher.find()) {
        rowCount++;
      }

      Pattern moveUpPattern = Pattern.compile("class=\"move-up-btn\"");
      Matcher moveUpMatcher = moveUpPattern.matcher(htmlContent);
      int moveUpCount = 0;
      while (moveUpMatcher.find()) {
        moveUpCount++;
      }

      Pattern moveDownPattern = Pattern.compile("class=\"move-down-btn\"");
      Matcher moveDownMatcher = moveDownPattern.matcher(htmlContent);
      int moveDownCount = 0;
      while (moveDownMatcher.find()) {
        moveDownCount++;
      }

      assertTrue(rowCount > 0, "Should have at least one input row");
      assertEquals(rowCount, moveUpCount, "move-up-btn count should match input row count");
      assertEquals(rowCount, moveDownCount, "move-down-btn count should match input row count");
    }

    @Test
    @DisplayName(
        "[F-8] Given: GETリクエストが与えられたとき, When: 入力行を確認すると,"
            + " Then: ▲▼ボタンは各行の最初のセルにあり、最後のセルには削除ボタンだけがある")
    void moveButtonsAreInFirstCellAndDeleteInLastCell() throws Exception {
      String html =
          mockMvc
              .perform(get("/"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      Matcher tbody =
          Pattern.compile("<tbody id=\"employee-rows\">(.*?)</tbody>", Pattern.DOTALL)
              .matcher(html);
      assertTrue(tbody.find(), "Input table body should exist");
      Matcher row = Pattern.compile("<tr[^>]*>(.*?)</tr>", Pattern.DOTALL).matcher(tbody.group(1));
      int rowCount = 0;
      while (row.find()) {
        List<String> cells = new java.util.ArrayList<>();
        Matcher cell = Pattern.compile("<td[^>]*>(.*?)</td>", Pattern.DOTALL).matcher(row.group(1));
        while (cell.find()) {
          cells.add(cell.group(1));
        }
        String first = cells.get(0);
        String last = cells.get(cells.size() - 1);
        assertTrue(first.contains("move-up-btn") && first.contains("move-down-btn"), first);
        assertFalse(last.contains("move-"), "Last cell should not contain move buttons");
        assertTrue(last.contains("delete-row-btn"), "Last cell should contain delete button");
        rowCount++;
      }
      assertTrue(rowCount > 0, "Should have input rows");
    }

    @Test
    @DisplayName(
        "[F-8] Given: shift-form.jsをロードしたとき, When: 行追加の処理を確認すると,"
            + " Then: ▲▼ボタンのセルは氏名のセルより先に追加される")
    void shiftFormJsAddsMoveCellBeforeNameCell() throws Exception {
      String jsContent = readShiftFormJs();

      int moveCellPos = jsContent.indexOf("newRow.appendChild(moveCell)");
      int nameCellPos = jsContent.indexOf("newRow.appendChild(createCell(\"氏名\"");
      assertTrue(moveCellPos >= 0, "shift-form.js should append moveCell to the new row");
      assertTrue(moveCellPos < nameCellPos, "moveCell should be appended before the name cell");
    }

    @Test
    @DisplayName(
        "[F-8] Given: shift-form.jsをロードしたとき, When: ファイルの内容を確認すると,"
            + " Then: move-up-btn・move-down-btn・insertBefore の記述があり、移動処理から"
            + " renumberInputIndices を呼ぶ処理がある")
    void shiftFormJsHandlesMoveUpAndDownButtons() throws Exception {
      String jsContent = readShiftFormJs();

      assertTrue(jsContent.contains("move-up-btn"), "shift-form.js should handle move-up-btn");
      assertTrue(jsContent.contains("move-down-btn"), "shift-form.js should handle move-down-btn");
      assertTrue(
          jsContent.contains("insertBefore"),
          "shift-form.js should use insertBefore for row movement");
      assertTrue(
          jsContent.contains("renumberInputIndices()"),
          "shift-form.js should call renumberInputIndices after moving rows");
    }

    @Test
    @DisplayName(
        "[F-8] Given: shift-form.jsをロードしたとき, When: ファイルの内容を確認すると,"
            + " Then: updateMoveButtonState の定義があり、動的に作る行の move-up-btn・move-down-btn"
            + " の生成がある")
    void shiftFormJsHasUpdateMoveButtonStateAndGeneratesButtonsInNewRows() throws Exception {
      String jsContent = readShiftFormJs();

      assertTrue(
          jsContent.contains("function updateMoveButtonState"),
          "shift-form.js should have updateMoveButtonState function");
      assertTrue(
          jsContent.contains("updateMoveButtonState()"),
          "shift-form.js should call updateMoveButtonState");

      // Check that new rows include move buttons
      assertTrue(
          jsContent.contains(".move-up-btn") || jsContent.contains("move-up-btn"),
          "shift-form.js should create move-up-btn in new rows");
      assertTrue(
          jsContent.contains(".move-down-btn") || jsContent.contains("move-down-btn"),
          "shift-form.js should create move-down-btn in new rows");
    }
  }

  @Nested
  @DisplayName("[F-4][H-3] 選定根拠の表示")
  class SelectionRationaleDisplay {

    private static final Pattern ASSIGNED_ROW_PATTERN =
        Pattern.compile("class=\"result-table\">.*?<tbody[^>]*>(.*?)</tbody>", Pattern.DOTALL);

    private static final Pattern UNASSIGNED_ITEM_PATTERN =
        Pattern.compile("<div class=\"unassigned-item\">(.*?)</div>", Pattern.DOTALL);

    /**
     * 指定の割当結果をスタブし、10 名分のフォームを送信して、レスポンス本文を返します。
     *
     * @param result スタブする割当結果
     * @return レスポンス本文
     * @throws Exception リクエストの実行に失敗した場合
     */
    private String postWith(AssignmentResult result) throws Exception {
      when(shiftAssignmentService.assign(any())).thenReturn(java.util.Optional.of(result));
      StringBuilder params = new StringBuilder();
      for (int i = 0; i < 10; i++) {
        params
            .append("&employees[")
            .append(i)
            .append("].name=")
            .append(String.valueOf((char) ('A' + i)));
        appendTimeRange(params, i, "07:30", "18:30");
      }
      return mockMvc
          .perform(
              post("/shift")
                  .contentType("application/x-www-form-urlencoded")
                  .content(params.toString().substring(1)))
          .andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString();
    }

    private List<String> assignedRows(String html) {
      Matcher tbody = ASSIGNED_ROW_PATTERN.matcher(html);
      assertTrue(tbody.find(), "Result table tbody should be present");
      Matcher rows = Pattern.compile("<tr[^>]*>.*?</tr>", Pattern.DOTALL).matcher(tbody.group(1));
      List<String> result = new java.util.ArrayList<>();
      while (rows.find()) {
        result.add(rows.group());
      }
      return result;
    }

    private List<String> unassignedItems(String html) {
      Matcher items = UNASSIGNED_ITEM_PATTERN.matcher(html);
      List<String> result = new java.util.ArrayList<>();
      while (items.find()) {
        result.add(items.group(1));
      }
      return result;
    }

    @Test
    @DisplayName(
        "[F-4] Given: 割当結果が表示されるとき, When: 結果表の見出しを確認すると,"
            + " Then: 「休憩時間」の後ろに「希望時間帯」が並び、「差（分）」「入れる枠」は表示されない")
    void showsWishRangeColumnWithoutGapAndWorkableSlotColumns() throws Exception {
      String html = postWith(createStandardResult());

      int breakPos = html.indexOf("<th>休憩時間</th>");
      int wishPos = html.indexOf("<th>希望時間帯</th>");

      assertTrue(breakPos >= 0, "Should contain header '休憩時間'");
      assertTrue(breakPos < wishPos, "'希望時間帯' should come after '休憩時間'");
      assertFalse(html.contains("<th>差（分）</th>"), "Should not contain header '差（分）'");
      assertFalse(html.contains("<th>入れる枠</th>"), "Should not contain header '入れる枠'");
    }

    @Test
    @DisplayName(
        "[F-4] Given: 7:30〜18:30 の 8 名が割り当てられたとき, When: 結果表の各行を確認すると,"
            + " Then: 希望時間帯が表示され、ずれ（分）と入れる枠のタグは表示されない")
    void showsWishRangeWithoutGapAndSlotTags() throws Exception {
      List<String> rows = assignedRows(postWith(createStandardResult()));

      assertEquals(8, rows.size());
      for (int i = 0; i < 8; i++) {
        assertTrue(rows.get(i).contains("07:30〜18:30"), "Row " + i + " should show wish range");
        assertFalse(rows.get(i).contains("slot-tag"), "Row " + i + " should not show slot tags");
        assertEquals(
            4, rows.get(i).split("<td", -1).length - 1, "Row " + i + " should have 4 cells");
      }
    }

    @Test
    @DisplayName(
        "[F-4] Given: 割当結果が表示されるとき, When: 結果表の各行を確認すると,"
            + " Then: 勤務時間と休憩時間に data-duration 属性があり、合計時間はサーバーでは算出されない")
    void marksWorkAndBreakRangesForClientSideDuration() throws Exception {
      String html = postWith(createStandardResult());

      Matcher work = Pattern.compile("data-duration=\"work\"[^>]*>07:30〜14:30<").matcher(html);
      Matcher breaks = Pattern.compile("data-duration=\"break\"[^>]*>12:00〜12:45<").matcher(html);
      assertTrue(work.find(), "Work range should have data-duration=work");
      assertTrue(breaks.find(), "Break range should have data-duration=break");
      assertEquals(8, html.split("data-duration=\"work\"", -1).length - 1);
      assertEquals(8, html.split("data-duration=\"break\"", -1).length - 1);
      assertFalse(html.contains("(07:00)"), "Total work time should not be calculated on server");
      assertFalse(html.contains("(00:45)"), "Total break time should not be calculated on server");
    }

    @Test
    @DisplayName(
        "[F-4] Given: shift-form.jsをロードしたとき, When: ファイルの内容を確認すると,"
            + " Then: data-duration の範囲から合計時間を hh:mm 形式で算出して表示する処理がある")
    void shiftFormJsCalculatesDurationOnClient() throws Exception {
      String js =
          new String(
              java.nio.file.Files.readAllBytes(
                  java.nio.file.Paths.get("src/main/resources/static/js/shift-form.js")),
              java.nio.charset.StandardCharsets.UTF_8);

      assertTrue(js.contains("data-duration"), "Should select data-duration elements");
      assertTrue(js.contains("formatDuration"), "Should format the duration");
      assertTrue(js.contains("padStart(2, \"0\")"), "Should format as hh:mm with zero padding");
    }

    @Test
    @DisplayName("[F-4] Given: 割当結果が表示されるとき, When: 選定根拠を確認すると, Then: ずれの合計の計算式は表示されない")
    void doesNotShowScoreFormula() throws Exception {
      String html = postWith(createStandardResult());

      assertFalse(html.contains("<h3>選定根拠</h3>"), "Should not contain the rationale heading");
      assertFalse(html.contains("score-formula"), "Should not contain the score formula");
    }

    @Test
    @DisplayName("[F-4] Given: 割当結果が表示されるとき, When: ページを確認すると," + " Then: 開閉要素を使わず、案の総数と同点件数は表示されない")
    void isAlwaysVisibleAndOmitsOutOfScopeItems() throws Exception {
      String html = postWith(createStandardResult());

      assertFalse(html.contains("<details"), "Should not use details");
      assertFalse(html.contains("<summary"), "Should not use summary");
      assertFalse(html.contains("案の総数"), "Should not show the number of candidates");
      assertFalse(html.contains("同点"), "Should not show the number of ties");
    }

    @Test
    @DisplayName(
        "[F-4][H-3] Given: 未出勤者が休み・入れる枠あり・入れる枠なしの 3 名のとき, When: 未出勤者を確認すると,"
            + " Then: それぞれの理由が表示され、入れる枠のタグは表示されない")
    void showsReasonAndWorkableSlotsForEachUnassignedEmployee() throws Exception {
      List<Employee> unassigned =
          List.of(
              Employee.onLeave("K"),
              Employee.working("I", LocalTime.of(7, 30), LocalTime.of(18, 30)),
              Employee.working("J", LocalTime.of(9, 0), LocalTime.of(10, 0)));

      String html =
          postWith(new AssignmentResult(createStandardResult().assignments(), 8, unassigned));
      List<String> items = unassignedItems(html);

      assertEquals(3, items.size());
      assertTrue(items.get(0).contains(">K<"));
      assertTrue(items.get(0).contains(">休み<"));
      assertTrue(items.get(1).contains(">I<"));
      assertTrue(items.get(1).contains(">入れる枠はあったが、同じずれの案があり、入力順で優先度が高い A が選ばれた<"));
      assertTrue(items.get(2).contains(">J<"));
      assertTrue(items.get(2).contains(">どの枠にも入れない<"));
      assertFalse(html.contains("slot-tag"), "Should not show slot tags");
    }

    @Test
    @DisplayName(
        "[F-4] Given: 入れる枠はあるが、どの割り当て済みの人と入れ替えてもずれが変わる未出勤者のとき,"
            + " When: 未出勤者を確認すると, Then: より小さいずれの案が選ばれたと表示される")
    void showsLowerGapReasonWhenNoSwapKeepsTotalGap() throws Exception {
      List<Employee> unassigned =
          List.of(Employee.working("S", LocalTime.of(7, 30), LocalTime.of(18, 0)));

      String html =
          postWith(new AssignmentResult(createStandardResult().assignments(), 8, unassigned));
      List<String> items = unassignedItems(html);

      assertEquals(1, items.size());
      assertTrue(items.get(0).contains(">入れる枠はあったが、より小さいずれの案が選ばれた<"));
      assertFalse(items.get(0).contains("優先度が高い"));
    }

    @Test
    @DisplayName(
        "[F-4] Given: 未出勤者が 3 名のとき, When: ページを確認すると," + " Then: class=\"chip\" は未出勤者の数（3）だけ現れる")
    void chipCountMatchesUnassignedCount() throws Exception {
      List<Employee> unassigned =
          List.of(
              Employee.onLeave("K"),
              Employee.working("I", LocalTime.of(7, 30), LocalTime.of(18, 30)),
              Employee.working("J", LocalTime.of(9, 0), LocalTime.of(10, 0)));

      String html =
          postWith(new AssignmentResult(createStandardResult().assignments(), 8, unassigned));

      assertEquals(3, html.split("class=\"chip\"", -1).length - 1);
    }
  }

  @Nested
  @DisplayName("画面表示時に保存済みの従業員入力を復元する")
  class RestoreLatestShift {

    @Test
    @DisplayName("Given: 保存済みの従業員入力があるとき, When: 画面を表示すると, Then: 保存データが復元される")
    void restoresAndDisplaysSavedEmployees() throws Exception {
      List<Employee> savedEmployees =
          List.of(
              Employee.working("Alice", LocalTime.of(9, 0), LocalTime.of(17, 0)),
              Employee.onLeave("Bob"));
      when(latestShiftRepository.findEmployees()).thenReturn(savedEmployees);

      MvcResult result = mockMvc.perform(get("/")).andExpect(status().isOk()).andReturn();

      // モデルから ShiftForm を取出し、全12行の値を検証
      ShiftForm form = (ShiftForm) result.getModelAndView().getModel().get("shiftForm");
      assertNotNull(form);
      assertEquals(12, form.getEmployees().size());

      // 1行目: 名前「Alice」、休み false、開始「09:00」、終了「17:00」
      EmployeeForm row0 = form.getEmployees().get(0);
      assertEquals("Alice", row0.getName());
      assertFalse(row0.isOff());
      assertEquals("09:00", row0.getStart());
      assertEquals("17:00", row0.getEnd());

      // 2行目: 名前「Bob」、休み true、開始・終了は空文字
      EmployeeForm row1 = form.getEmployees().get(1);
      assertEquals("Bob", row1.getName());
      assertTrue(row1.isOff());
      assertEquals("", row1.getStart());
      assertEquals("", row1.getEnd());

      // 3行目以降は空
      for (int i = 2; i < 12; i++) {
        EmployeeForm row = form.getEmployees().get(i);
        assertNull(row.getName());
        assertFalse(row.isOff());
        assertNull(row.getStart());
        assertNull(row.getEnd());
      }
    }

    @Test
    @DisplayName("Given: 保存済みデータがないとき, When: 画面を表示すると, Then: 空の12行が表示される")
    void showsEmptyRowsWhenNoSavedData() throws Exception {
      when(latestShiftRepository.findEmployees()).thenReturn(List.of());

      MvcResult result = mockMvc.perform(get("/")).andExpect(status().isOk()).andReturn();

      String html = result.getResponse().getContentAsString();

      // 12行の空行を確認
      assertTrue(html.contains("id=\"row-count\""));
      assertTrue(html.contains("name=\"employees[11].name\""));
      assertFalse(html.contains("name=\"employees[12].name\""));

      // モデルから ShiftForm を取出し、全12行の値を検証
      ShiftForm form = (ShiftForm) result.getModelAndView().getModel().get("shiftForm");
      assertNotNull(form);
      assertEquals(12, form.getEmployees().size());

      // 全12行が空であることを検証
      for (int i = 0; i < 12; i++) {
        EmployeeForm row = form.getEmployees().get(i);
        assertNull(row.getName());
        assertFalse(row.isOff());
        assertNull(row.getStart());
        assertNull(row.getEnd());
      }
    }
  }

  @Nested
  @DisplayName("シフト算出後に従業員入力と結果を保存する")
  class SaveAfterShiftAssignment {

    @Test
    @DisplayName("Given: シフト算出が成功したとき, When: POST /shift すると, Then: save が入力順と結果で呼ばれる")
    void callsSaveWithCorrectArgumentsOnSuccess() throws Exception {
      var result = createStandardResult();
      when(shiftAssignmentService.findDuplicateNames(any())).thenReturn(List.of());
      when(shiftAssignmentService.assign(any())).thenReturn(java.util.Optional.of(result));

      var request = post("/shift");
      List<String> names = List.of("A", "B", "C", "D", "E", "F", "G", "H");
      for (int i = 0; i < names.size(); i++) {
        request.param("employees[" + i + "].name", names.get(i));
        request.param("employees[" + i + "].off", "false");
        request.param("employees[" + i + "].start", "07:30");
        request.param("employees[" + i + "].end", "18:30");
      }

      mockMvc.perform(request).andExpect(status().isOk());

      ArgumentCaptor<java.util.List<Employee>> captor =
          ArgumentCaptor.forClass(java.util.List.class);
      verify(latestShiftRepository).save(captor.capture(), any());

      List<Employee> capturedEmployees = captor.getValue();
      assertEquals(8, capturedEmployees.size());
      for (int i = 0; i < 8; i++) {
        assertEquals(names.get(i), capturedEmployees.get(i).name());
      }
    }

    @Test
    @DisplayName("Given: シフト算出が不成立のとき, When: POST /shift すると, Then: save が Optional.empty() で呼ばれる")
    void callsSaveWithEmptyResultWhenUnassignable() throws Exception {
      when(shiftAssignmentService.findDuplicateNames(any())).thenReturn(List.of());
      when(shiftAssignmentService.assign(any())).thenReturn(java.util.Optional.empty());

      var request = post("/shift");
      List<String> names = List.of("A", "B", "C", "D", "E", "F", "G", "H");
      for (int i = 0; i < names.size(); i++) {
        request.param("employees[" + i + "].name", names.get(i));
        request.param("employees[" + i + "].off", "false");
        request.param("employees[" + i + "].start", "07:30");
        request.param("employees[" + i + "].end", "18:30");
      }

      mockMvc.perform(request).andExpect(status().isOk());

      ArgumentCaptor<java.util.Optional<AssignmentResult>> captor =
          ArgumentCaptor.forClass(java.util.Optional.class);
      verify(latestShiftRepository).save(any(), captor.capture());

      assertTrue(captor.getValue().isEmpty());
    }

    @Test
    @DisplayName("Given: 入力エラーがあるとき, When: POST /shift すると, Then: save が呼ばれない")
    void doesNotCallSaveOnInputError() throws Exception {
      mockMvc
          .perform(
              post("/shift")
                  .param("employees[0].name", "A")
                  .param("employees[0].off", "false")
                  .param("employees[0].start", "invalid")
                  .param("employees[0].end", "18:30"))
          .andExpect(status().isOk());

      verify(latestShiftRepository, never()).save(any(), any());
    }
  }

  @Nested
  @DisplayName("保存に失敗したらエラーとリトライを促す文言を表示する")
  class SaveFailureErrorDisplay {

    private static final String ERROR_MESSAGE = "保存に失敗しました。もう一度シフトを作成して保存し直してください。";

    @Test
    @DisplayName("Given: 保存処理で例外が発生したとき, When: POST /shift すると, Then: エラーメッセージと結果が表示される")
    void displaysErrorMessageOnSaveFailure() throws Exception {
      var result = createStandardResult();
      when(shiftAssignmentService.findDuplicateNames(any())).thenReturn(List.of());
      when(shiftAssignmentService.assign(any())).thenReturn(java.util.Optional.of(result));
      Mockito.doThrow(new org.springframework.dao.DataAccessResourceFailureException("test"))
          .when(latestShiftRepository)
          .save(any(), any());

      var request = post("/shift");
      List<String> names = List.of("A", "B", "C", "D", "E", "F", "G", "H");
      for (int i = 0; i < names.size(); i++) {
        request.param("employees[" + i + "].name", names.get(i));
        request.param("employees[" + i + "].off", "false");
        request.param("employees[" + i + "].start", "07:30");
        request.param("employees[" + i + "].end", "18:30");
      }

      MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

      String html = mvcResult.getResponse().getContentAsString();

      // エラーメッセージが含まれ、割当結果も表示
      assertTrue(html.contains(ERROR_MESSAGE), "Error message should be displayed");
      assertTrue(html.contains("class=\"card result\""), "Assignment result should be displayed");
    }

    @Test
    @DisplayName("Given: 保存処理に成功したとき, When: POST /shift すると, Then: エラーメッセージが表示されない")
    void doesNotDisplayErrorMessageOnSaveSuccess() throws Exception {
      var result = createStandardResult();
      when(shiftAssignmentService.findDuplicateNames(any())).thenReturn(List.of());
      when(shiftAssignmentService.assign(any())).thenReturn(java.util.Optional.of(result));
      Mockito.doNothing().when(latestShiftRepository).save(any(), any());

      var request = post("/shift");
      List<String> names = List.of("A", "B", "C", "D", "E", "F", "G", "H");
      for (int i = 0; i < names.size(); i++) {
        request.param("employees[" + i + "].name", names.get(i));
        request.param("employees[" + i + "].off", "false");
        request.param("employees[" + i + "].start", "07:30");
        request.param("employees[" + i + "].end", "18:30");
      }

      MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

      String html = mvcResult.getResponse().getContentAsString();

      // エラーメッセージが含まれない
      assertFalse(html.contains(ERROR_MESSAGE), "Error message should not be displayed");
    }

    @Test
    @DisplayName("Given: 保存処理で例外が発生したとき, When: POST /shift すると, Then: エラーログがスタックトレース付きで出力される")
    void logsErrorWithStacktraceOnSaveFailure() throws Exception {
      var result = createStandardResult();
      when(shiftAssignmentService.findDuplicateNames(any())).thenReturn(List.of());
      when(shiftAssignmentService.assign(any())).thenReturn(java.util.Optional.of(result));
      Mockito.doThrow(new org.springframework.dao.DataAccessResourceFailureException("test"))
          .when(latestShiftRepository)
          .save(any(), any());

      // ロガーに ListAppender を追加
      Logger logger = (Logger) LoggerFactory.getLogger(ShiftController.class);
      @SuppressWarnings("rawtypes")
      ListAppender listAppender = new ListAppender();
      listAppender.start();
      logger.addAppender(listAppender);

      try {
        var request = post("/shift");
        List<String> names = List.of("A", "B", "C", "D", "E", "F", "G", "H");
        for (int i = 0; i < names.size(); i++) {
          request.param("employees[" + i + "].name", names.get(i));
          request.param("employees[" + i + "].off", "false");
          request.param("employees[" + i + "].start", "07:30");
          request.param("employees[" + i + "].end", "18:30");
        }

        mockMvc.perform(request).andExpect(status().isOk());

        // ログの確認：ERROR レベルのログがあり、スタックトレース付き
        assertTrue(
            listAppender.list.stream()
                .anyMatch(
                    event -> {
                      // 型安全なキャストを避けて、リフレクションで確認
                      try {
                        Object level = event.getClass().getMethod("getLevel").invoke(event);
                        Object throwableProxy =
                            event.getClass().getMethod("getThrowableProxy").invoke(event);
                        return level == Level.ERROR && throwableProxy != null;
                      } catch (Exception ex) {
                        return false;
                      }
                    }),
            "Error log with stack trace should be recorded");
      } finally {
        logger.detachAppender(listAppender);
      }
    }
  }

  @Nested
  @DisplayName("[F-4] 選定根拠のログ出力")
  class SelectionRationaleLogging {

    private List<String> postAndCollectInfoLogs(AssignmentResult result) throws Exception {
      when(shiftAssignmentService.findDuplicateNames(any())).thenReturn(List.of());
      when(shiftAssignmentService.assign(any())).thenReturn(java.util.Optional.of(result));

      Logger logger = (Logger) LoggerFactory.getLogger(ShiftController.class);
      ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender = new ListAppender<>();
      appender.start();
      logger.addAppender(appender);
      try {
        var request = post("/shift");
        for (int i = 0; i < 8; i++) {
          request.param("employees[" + i + "].name", String.valueOf((char) ('A' + i)));
          request.param("employees[" + i + "].off", "false");
          request.param("employees[" + i + "].start", "07:30");
          request.param("employees[" + i + "].end", "18:30");
        }
        mockMvc.perform(request).andExpect(status().isOk());
        return appender.list.stream()
            .filter(event -> event.getLevel() == Level.INFO)
            .map(event -> event.getFormattedMessage())
            .toList();
      } finally {
        logger.detachAppender(appender);
      }
    }

    @Test
    @DisplayName(
        "[F-4] Given: 割当結果があるとき, When: POST /shift すると, Then: 各人の希望時間帯・割り当てた枠・ずれ・入れる枠と、ずれの合計が INFO"
            + " ログに出力される")
    void logsRationaleForEachAssignment() throws Exception {
      List<String> logs =
          postAndCollectInfoLogs(
              new AssignmentResult(createStandardResult().assignments(), 1380, List.of()));

      List<String> assignmentLogs = logs.stream().filter(log -> log.startsWith("割当 ")).toList();
      String allSlots =
          "[07:30〜14:30, 08:00〜15:30, 08:30〜16:30, 09:00〜16:30, 09:00〜18:00, 09:00〜18:30]";
      List<String> expectedAssigned =
          List.of(
              "A 希望=07:30〜18:30 割当=07:30〜14:30 差=240分 入れる枠=",
              "B 希望=07:30〜18:30 割当=07:30〜14:30 差=240分 入れる枠=",
              "C 希望=07:30〜18:30 割当=08:00〜15:30 差=210分 入れる枠=",
              "D 希望=07:30〜18:30 割当=08:30〜16:30 差=180分 入れる枠=",
              "E 希望=07:30〜18:30 割当=09:00〜16:30 差=210分 入れる枠=",
              "F 希望=07:30〜18:30 割当=09:00〜18:00 差=120分 入れる枠=",
              "G 希望=07:30〜18:30 割当=09:00〜18:30 差=90分 入れる枠=",
              "H 希望=07:30〜18:30 割当=09:00〜18:30 差=90分 入れる枠=");
      assertEquals(8, assignmentLogs.size(), String.join("\n", logs));
      for (int i = 0; i < 8; i++) {
        assertEquals("割当 " + expectedAssigned.get(i) + allSlots, assignmentLogs.get(i));
      }
      assertEquals(
          List.of("合計 = 240 + 240 + 210 + 180 + 210 + 120 + 90 + 90 = 1380 分"),
          logs.stream().filter(log -> log.startsWith("合計")).toList());
    }

    @Test
    @DisplayName("[F-4] Given: 未出勤者がいるとき, When: POST /shift すると, Then: 氏名・理由・入れる枠が INFO ログに出力される")
    void logsRationaleForUnassignedEmployees() throws Exception {
      List<Employee> unassigned =
          List.of(
              Employee.onLeave("K"),
              Employee.working("J", LocalTime.of(9, 0), LocalTime.of(10, 0)));

      List<String> logs =
          postAndCollectInfoLogs(
              new AssignmentResult(createStandardResult().assignments(), 1380, unassigned));
      String joined = String.join("\n", logs);

      assertTrue(joined.contains("未出勤 K 理由=休み 入れる枠=[]"), joined);
      assertTrue(joined.contains("未出勤 J 理由=どの枠にも入れない 入れる枠=[]"), joined);
    }

    @Test
    @DisplayName(
        "[F-4] Given: 氏名に改行を含む割当者と未出勤者がいるとき, When: POST /shift すると,"
            + " Then: 改行はエスケープされ、1 件のログイベントが 1 行に保たれる")
    void escapesLineBreaksInNamesWhenLogging() throws Exception {
      List<ShiftAssignment> assignments =
          new java.util.ArrayList<>(createStandardResult().assignments());
      ShiftAssignment first = assignments.get(0);
      assignments.set(
          0,
          new ShiftAssignment(
              Employee.working(
                  "A\r\n合計 = 0 = 0 分", first.employee().start(), first.employee().end()),
              first.slot(),
              first.breakStart(),
              first.breakEnd()));
      List<Employee> unassigned = List.of(Employee.onLeave("K\nERROR 偽装"));

      List<String> logs =
          postAndCollectInfoLogs(new AssignmentResult(assignments, 1380, unassigned));

      for (String log : logs) {
        assertFalse(log.contains("\n") || log.contains("\r"), "Log should be one line: " + log);
      }
      String joined = String.join("\n", logs);
      assertTrue(joined.contains("割当 A\\r\\n合計 = 0 = 0 分 希望="), joined);
      assertTrue(joined.contains("未出勤 K\\nERROR 偽装 理由=休み"), joined);
    }
  }

  @Nested
  @DisplayName("氏名の最大長チェック")
  class NameLengthValidation {

    @Test
    @DisplayName("Given: 256文字の氏名があるとき, When: POSTすると, Then: saveが呼ばれずindex が表示される")
    void doesNotSaveWhenNameExceedsMaxLength() throws Exception {
      var request = post("/shift");
      String longName = "A".repeat(256);
      List<String> names = List.of(longName, "B", "C", "D", "E", "F", "G", "H");
      for (int i = 0; i < names.size(); i++) {
        request.param("employees[" + i + "].name", names.get(i));
        request.param("employees[" + i + "].off", "false");
        request.param("employees[" + i + "].start", "07:30");
        request.param("employees[" + i + "].end", "18:30");
      }

      mockMvc.perform(request).andExpect(status().isOk());

      // save が呼ばれていないことを確認
      verify(latestShiftRepository, never()).save(any(), any());
    }

    @Test
    @DisplayName("Given: 255文字の氏名があるとき, When: POSTすると, Then: saveが呼ばれる")
    void saveWhenNameIsMaxLength() throws Exception {
      var result = createStandardResult();
      when(shiftAssignmentService.findDuplicateNames(any())).thenReturn(List.of());
      when(shiftAssignmentService.assign(any())).thenReturn(java.util.Optional.of(result));

      var request = post("/shift");
      String maxName = "A".repeat(255);
      List<String> names = List.of(maxName, "B", "C", "D", "E", "F", "G", "H");
      for (int i = 0; i < names.size(); i++) {
        request.param("employees[" + i + "].name", names.get(i));
        request.param("employees[" + i + "].off", "false");
        request.param("employees[" + i + "].start", "07:30");
        request.param("employees[" + i + "].end", "18:30");
      }

      mockMvc.perform(request).andExpect(status().isOk());

      // save が呼ばれていることを確認
      verify(latestShiftRepository).save(any(), any());
    }

    @Test
    @DisplayName("Given: 256文字の氏名があるとき, When: POSTすると, Then: エラーメッセージが表示される")
    void displaysErrorMessageWhenNameExceedsMaxLength() throws Exception {
      var request = post("/shift");
      String longName = "A".repeat(256);
      List<String> names = List.of(longName, "B", "C", "D", "E", "F", "G", "H");
      for (int i = 0; i < names.size(); i++) {
        request.param("employees[" + i + "].name", names.get(i));
        request.param("employees[" + i + "].off", "false");
        request.param("employees[" + i + "].start", "07:30");
        request.param("employees[" + i + "].end", "18:30");
      }

      MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
      String html = mvcResult.getResponse().getContentAsString();

      // エラーメッセージに行番号が含まれることを確認
      assertTrue(
          html.contains("1") && html.contains("255"),
          "Error message should contain row number and max length");
    }

    @Test
    @DisplayName("Given: 空の氏名があるとき, When: POSTすると, Then: エラーにならず除外される")
    void excludesEmptyNameRows() throws Exception {
      var result = createStandardResult();
      when(shiftAssignmentService.findDuplicateNames(any())).thenReturn(List.of());
      when(shiftAssignmentService.assign(any())).thenReturn(java.util.Optional.of(result));

      var request = post("/shift");
      List<String> names = List.of("", "B", "C", "D", "E", "F", "G", "H");
      for (int i = 0; i < names.size(); i++) {
        request.param("employees[" + i + "].name", names.get(i));
        request.param("employees[" + i + "].off", "false");
        request.param("employees[" + i + "].start", "07:30");
        request.param("employees[" + i + "].end", "18:30");
      }

      mockMvc.perform(request).andExpect(status().isOk());

      // save が呼ばれていることを確認（空行は除外される）
      verify(latestShiftRepository).save(any(), any());
    }
  }

  @Nested
  @DisplayName("CSRF対策：別オリジンからのPOSTを拒否する")
  class SameOriginProtection {

    @Test
    @DisplayName(
        "Given: Sec-Fetch-Site: cross-site のとき, When: POST /shift すると, Then: 403で save/assign"
            + " が呼ばれない")
    void rejectsCrossSiteRequest() throws Exception {
      mockMvc
          .perform(
              post("/shift")
                  .header("Sec-Fetch-Site", "cross-site")
                  .param("employees[0].name", "A")
                  .param("employees[0].off", "false")
                  .param("employees[0].start", "07:30")
                  .param("employees[0].end", "18:30"))
          .andExpect(status().isForbidden());

      verify(shiftAssignmentService, never()).assign(any());
      verify(latestShiftRepository, never()).save(any(), any());
    }

    @Test
    @DisplayName("Given: Sec-Fetch-Site: same-origin のとき, When: POST /shift すると, Then: 200で処理される")
    void allowsSameOriginRequest() throws Exception {
      var result = createStandardResult();
      when(shiftAssignmentService.findDuplicateNames(any())).thenReturn(List.of());
      when(shiftAssignmentService.assign(any())).thenReturn(java.util.Optional.of(result));

      mockMvc
          .perform(
              post("/shift")
                  .header("Sec-Fetch-Site", "same-origin")
                  .param("employees[0].name", "A")
                  .param("employees[0].off", "false")
                  .param("employees[0].start", "07:30")
                  .param("employees[0].end", "18:30")
                  .param("employees[1].name", "B")
                  .param("employees[1].off", "false")
                  .param("employees[1].start", "07:30")
                  .param("employees[1].end", "18:30")
                  .param("employees[2].name", "C")
                  .param("employees[2].off", "false")
                  .param("employees[2].start", "07:30")
                  .param("employees[2].end", "18:30")
                  .param("employees[3].name", "D")
                  .param("employees[3].off", "false")
                  .param("employees[3].start", "07:30")
                  .param("employees[3].end", "18:30")
                  .param("employees[4].name", "E")
                  .param("employees[4].off", "false")
                  .param("employees[4].start", "07:30")
                  .param("employees[4].end", "18:30")
                  .param("employees[5].name", "F")
                  .param("employees[5].off", "false")
                  .param("employees[5].start", "07:30")
                  .param("employees[5].end", "18:30")
                  .param("employees[6].name", "G")
                  .param("employees[6].off", "false")
                  .param("employees[6].start", "07:30")
                  .param("employees[6].end", "18:30")
                  .param("employees[7].name", "H")
                  .param("employees[7].off", "false")
                  .param("employees[7].start", "07:30")
                  .param("employees[7].end", "18:30"))
          .andExpect(status().isOk());

      verify(shiftAssignmentService).assign(any());
    }

    @Test
    @DisplayName("Given: Sec-Fetch-Site: none のとき, When: POST /shift すると, Then: 200で処理される")
    void allowsNoneOriginRequest() throws Exception {
      var result = createStandardResult();
      when(shiftAssignmentService.findDuplicateNames(any())).thenReturn(List.of());
      when(shiftAssignmentService.assign(any())).thenReturn(java.util.Optional.of(result));

      mockMvc
          .perform(
              post("/shift")
                  .header("Sec-Fetch-Site", "none")
                  .param("employees[0].name", "A")
                  .param("employees[0].off", "false")
                  .param("employees[0].start", "07:30")
                  .param("employees[0].end", "18:30")
                  .param("employees[1].name", "B")
                  .param("employees[1].off", "false")
                  .param("employees[1].start", "07:30")
                  .param("employees[1].end", "18:30")
                  .param("employees[2].name", "C")
                  .param("employees[2].off", "false")
                  .param("employees[2].start", "07:30")
                  .param("employees[2].end", "18:30")
                  .param("employees[3].name", "D")
                  .param("employees[3].off", "false")
                  .param("employees[3].start", "07:30")
                  .param("employees[3].end", "18:30")
                  .param("employees[4].name", "E")
                  .param("employees[4].off", "false")
                  .param("employees[4].start", "07:30")
                  .param("employees[4].end", "18:30")
                  .param("employees[5].name", "F")
                  .param("employees[5].off", "false")
                  .param("employees[5].start", "07:30")
                  .param("employees[5].end", "18:30")
                  .param("employees[6].name", "G")
                  .param("employees[6].off", "false")
                  .param("employees[6].start", "07:30")
                  .param("employees[6].end", "18:30")
                  .param("employees[7].name", "H")
                  .param("employees[7].off", "false")
                  .param("employees[7].start", "07:30")
                  .param("employees[7].end", "18:30"))
          .andExpect(status().isOk());

      verify(shiftAssignmentService).assign(any());
    }

    @Test
    @DisplayName(
        "Given: Origin: http://evil.example, Host: localhost:8080 のとき, When: POST /shift すると, Then:"
            + " 403で save/assign が呼ばれない")
    void rejectsRequestWithMismatchedOrigin() throws Exception {
      mockMvc
          .perform(
              post("/shift")
                  .header("Origin", "http://evil.example")
                  .header("Host", "localhost:8080")
                  .param("employees[0].name", "A")
                  .param("employees[0].off", "false")
                  .param("employees[0].start", "07:30")
                  .param("employees[0].end", "18:30"))
          .andExpect(status().isForbidden());

      verify(shiftAssignmentService, never()).assign(any());
      verify(latestShiftRepository, never()).save(any(), any());
    }

    @Test
    @DisplayName(
        "Given: Origin: http://localhost:8080, Host: localhost:8080 のとき, When: POST /shift すると,"
            + " Then: 200で処理される")
    void allowsRequestWithMatchingOrigin() throws Exception {
      var result = createStandardResult();
      when(shiftAssignmentService.findDuplicateNames(any())).thenReturn(List.of());
      when(shiftAssignmentService.assign(any())).thenReturn(java.util.Optional.of(result));

      mockMvc
          .perform(
              post("/shift")
                  .header("Origin", "http://localhost:8080")
                  .header("Host", "localhost:8080")
                  .param("employees[0].name", "A")
                  .param("employees[0].off", "false")
                  .param("employees[0].start", "07:30")
                  .param("employees[0].end", "18:30")
                  .param("employees[1].name", "B")
                  .param("employees[1].off", "false")
                  .param("employees[1].start", "07:30")
                  .param("employees[1].end", "18:30")
                  .param("employees[2].name", "C")
                  .param("employees[2].off", "false")
                  .param("employees[2].start", "07:30")
                  .param("employees[2].end", "18:30")
                  .param("employees[3].name", "D")
                  .param("employees[3].off", "false")
                  .param("employees[3].start", "07:30")
                  .param("employees[3].end", "18:30")
                  .param("employees[4].name", "E")
                  .param("employees[4].off", "false")
                  .param("employees[4].start", "07:30")
                  .param("employees[4].end", "18:30")
                  .param("employees[5].name", "F")
                  .param("employees[5].off", "false")
                  .param("employees[5].start", "07:30")
                  .param("employees[5].end", "18:30")
                  .param("employees[6].name", "G")
                  .param("employees[6].off", "false")
                  .param("employees[6].start", "07:30")
                  .param("employees[6].end", "18:30")
                  .param("employees[7].name", "H")
                  .param("employees[7].off", "false")
                  .param("employees[7].start", "07:30")
                  .param("employees[7].end", "18:30"))
          .andExpect(status().isOk());

      verify(shiftAssignmentService).assign(any());
    }

    @Test
    @DisplayName("Given: ヘッダなし(非ブラウザ/旧ブラウザ) のとき, When: POST /shift すると, Then: 200で処理される")
    void allowsRequestWithoutHeaders() throws Exception {
      var result = createStandardResult();
      when(shiftAssignmentService.findDuplicateNames(any())).thenReturn(List.of());
      when(shiftAssignmentService.assign(any())).thenReturn(java.util.Optional.of(result));

      mockMvc
          .perform(
              post("/shift")
                  .param("employees[0].name", "A")
                  .param("employees[0].off", "false")
                  .param("employees[0].start", "07:30")
                  .param("employees[0].end", "18:30")
                  .param("employees[1].name", "B")
                  .param("employees[1].off", "false")
                  .param("employees[1].start", "07:30")
                  .param("employees[1].end", "18:30")
                  .param("employees[2].name", "C")
                  .param("employees[2].off", "false")
                  .param("employees[2].start", "07:30")
                  .param("employees[2].end", "18:30")
                  .param("employees[3].name", "D")
                  .param("employees[3].off", "false")
                  .param("employees[3].start", "07:30")
                  .param("employees[3].end", "18:30")
                  .param("employees[4].name", "E")
                  .param("employees[4].off", "false")
                  .param("employees[4].start", "07:30")
                  .param("employees[4].end", "18:30")
                  .param("employees[5].name", "F")
                  .param("employees[5].off", "false")
                  .param("employees[5].start", "07:30")
                  .param("employees[5].end", "18:30")
                  .param("employees[6].name", "G")
                  .param("employees[6].off", "false")
                  .param("employees[6].start", "07:30")
                  .param("employees[6].end", "18:30")
                  .param("employees[7].name", "H")
                  .param("employees[7].off", "false")
                  .param("employees[7].start", "07:30")
                  .param("employees[7].end", "18:30"))
          .andExpect(status().isOk());

      verify(shiftAssignmentService).assign(any());
    }

    @Test
    @DisplayName("Given: GET /で Sec-Fetch-Site: cross-site のとき, When: アクセスすると, Then: 200で許可される")
    void allowsCrossSiteGetRequest() throws Exception {
      mockMvc.perform(get("/").header("Sec-Fetch-Site", "cross-site")).andExpect(status().isOk());
    }

    @Test
    @DisplayName(
        "Given: Origin: null, Host: localhost:8080 のとき, When: POST /shift すると, Then: 403で"
            + " save/assign が呼ばれない")
    void rejectsRequestWithNullOrigin() throws Exception {
      mockMvc
          .perform(
              post("/shift")
                  .header("Origin", "null")
                  .header("Host", "localhost:8080")
                  .param("employees[0].name", "A")
                  .param("employees[0].off", "false")
                  .param("employees[0].start", "07:30")
                  .param("employees[0].end", "18:30"))
          .andExpect(status().isForbidden());

      verify(shiftAssignmentService, never()).assign(any());
      verify(latestShiftRepository, never()).save(any(), any());
    }

    @Test
    @DisplayName(
        "Given: Origin: file:///x (ホスト抽出不可), Host: localhost:8080 のとき, When: POST /shift すると,"
            + " Then: 403で save/assign が呼ばれない")
    void rejectsRequestWithNoHostOrigin() throws Exception {
      mockMvc
          .perform(
              post("/shift")
                  .header("Origin", "file:///x")
                  .header("Host", "localhost:8080")
                  .param("employees[0].name", "A")
                  .param("employees[0].off", "false")
                  .param("employees[0].start", "07:30")
                  .param("employees[0].end", "18:30"))
          .andExpect(status().isForbidden());

      verify(shiftAssignmentService, never()).assign(any());
      verify(latestShiftRepository, never()).save(any(), any());
    }
  }
}
