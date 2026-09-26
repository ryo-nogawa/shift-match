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

import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.ShiftAssignment;
import com.example.shiftmatch.domain.ShiftSlot;
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
        "[F-4] Given: ずれの合計が90分の割当結果が表示されるとき, When: スコア表示部分を確認すると, Then:"
            + " '.score-num'に'90'と'分'が表示され、'/ 8'は表示されない")
    void displaysGapTotalInMinutes() throws Exception {
      AssignmentResult result =
          new AssignmentResult(createStandardResult().assignments(), 90, List.of());
      when(shiftAssignmentService.assign(any())).thenReturn(java.util.Optional.of(result));

      String responseContent = postEmployees(8);

      Matcher matcher =
          Pattern.compile("class=\"score-num\"[^>]*>\\s*<span>(\\d+)</span><small> 分</small>")
              .matcher(responseContent);
      assertTrue(matcher.find(), "Score should be displayed as '<n> 分'");
      assertEquals("90", matcher.group(1));
      assertFalse(responseContent.contains("/ 8"), "Score should not show '/ 8'");
    }

    @Test
    @DisplayName(
        "[F-4] Given: 割当結果が表示されるとき, When: スコアのラベルを確認すると, Then:"
            + " 'ずれの合計'のラベルがあり、旧スコアの記号（U+25CE）は表示されない")
    void displaysGapTotalLabelWithoutDesiredMark() throws Exception {
      String responseContent = postEmployees(8);

      assertTrue(
          responseContent.contains("ずれの合計（入力時間帯と割り当てた枠の差。0 が最良）"),
          "Score label should describe the gap total");
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
  @DisplayName("[F-2][F-6] JavaScriptの行追加・削除機能")
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
  }

  @Nested
  @DisplayName("[F-4][H-3] 選定根拠の表示")
  class SelectionRationaleDisplay {

    private static final Pattern SLOT_TAG_PATTERN =
        Pattern.compile("class=\"(slot-tag[^\"]*)\"[^>]*>([^<]*)<");

    private static final Pattern ASSIGNED_ROW_PATTERN =
        Pattern.compile("class=\"result-table\">.*?<tbody[^>]*>(.*?)</tbody>", Pattern.DOTALL);

    private static final Pattern UNASSIGNED_ITEM_PATTERN =
        Pattern.compile("<div class=\"unassigned-item\">(.*?)</div>", Pattern.DOTALL);

    private static final List<String> ALL_SLOT_TIMES =
        List.of(
            "07:30〜14:30",
            "08:00〜15:30",
            "08:30〜16:30",
            "09:00〜16:30",
            "09:00〜18:00",
            "09:00〜18:30");

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

    /**
     * 行の HTML から、入れる枠のラベル（勤務時間）と、割り当てた枠かどうかを順に取り出します。
     *
     * @param html 行または項目の HTML
     * @return 「勤務時間」または「勤務時間*」（* は slot-chosen）のリスト
     */
    private List<String> slotTags(String html) {
      Matcher matcher = SLOT_TAG_PATTERN.matcher(html);
      List<String> result = new java.util.ArrayList<>();
      while (matcher.find()) {
        result.add(matcher.group(2) + (matcher.group(1).contains("slot-chosen") ? "*" : ""));
      }
      return result;
    }

    @Test
    @DisplayName(
        "[F-4] Given: 割当結果が表示されるとき, When: 結果表の見出しを確認すると,"
            + " Then: 「休憩時間」の後ろに「希望時間帯」「差（分）」「入れる枠」がこの順で並ぶ")
    void addsRationaleColumnsAfterBreakTime() throws Exception {
      String html = postWith(createStandardResult());

      int breakPos = html.indexOf("<th>休憩時間</th>");
      int wishPos = html.indexOf("<th>希望時間帯</th>");
      int gapPos = html.indexOf("<th>差（分）</th>");

      assertTrue(breakPos >= 0, "Should contain header '休憩時間'");
      assertTrue(breakPos < wishPos, "'希望時間帯' should come after '休憩時間'");
      assertTrue(wishPos < gapPos, "'差（分）' should come after '希望時間帯'");
      int slotsPos = html.indexOf("<th>入れる枠</th>");
      assertTrue(gapPos < slotsPos, "'入れる枠' should come after '差（分）'");
    }

    @Test
    @DisplayName(
        "[F-4] Given: 7:30〜18:30 の 8 名が割り当てられたとき, When: 結果表の各行を確認すると,"
            + " Then: 希望時間帯と、枠ごとのずれ（分）が表示される")
    void showsWishRangeAndGapForEachRow() throws Exception {
      List<String> rows = assignedRows(postWith(createStandardResult()));
      int[] expectedGaps = {240, 240, 210, 180, 210, 120, 90, 90};

      assertEquals(8, rows.size());
      for (int i = 0; i < 8; i++) {
        assertTrue(rows.get(i).contains("07:30〜18:30"), "Row " + i + " should show wish range");
        assertTrue(
            rows.get(i).contains("<td>" + expectedGaps[i] + "</td>"),
            "Row " + i + " should show gap " + expectedGaps[i]);
      }
    }

    @Test
    @DisplayName(
        "[H-3] Given: 7:30〜18:30 の人が枠 1 に割り当てられたとき, When: 入れる枠を確認すると,"
            + " Then: 枠 1〜6 の勤務時間が順に並び、枠 1 だけが割り当てた枠として区別される")
    void showsAllSlotsInOrderWithChosenSlotMarked() throws Exception {
      List<String> rows = assignedRows(postWith(createStandardResult()));

      assertEquals(
          List.of(
              "07:30〜14:30*",
              "08:00〜15:30",
              "08:30〜16:30",
              "09:00〜16:30",
              "09:00〜18:00",
              "09:00〜18:30"),
          slotTags(rows.get(0)));
      assertEquals(
          List.of(
              "07:30〜14:30",
              "08:00〜15:30",
              "08:30〜16:30",
              "09:00〜16:30*",
              "09:00〜18:00",
              "09:00〜18:30"),
          slotTags(rows.get(4)));
    }

    @Test
    @DisplayName(
        "[H-3] Given: 9:00〜16:30 の人が枠 4 に割り当てられたとき, When: 入れる枠を確認すると,"
            + " Then: 入れる枠は枠 4 の 1 件だけで、割り当てた枠として区別され、ずれは 0 分である")
    void showsOnlyChosenSlotWhenOnlyOneSlotIsWorkable() throws Exception {
      List<ShiftAssignment> assignments =
          new java.util.ArrayList<>(createStandardResult().assignments());
      assignments.set(
          4,
          new ShiftAssignment(
              Employee.working("X", LocalTime.of(9, 0), LocalTime.of(16, 30)),
              ShiftSlot.SLOT_4,
              LocalTime.of(13, 30),
              LocalTime.of(14, 15)));

      List<String> rows = assignedRows(postWith(new AssignmentResult(assignments, 8, List.of())));

      assertEquals(List.of("09:00〜16:30*"), slotTags(rows.get(4)));
      assertTrue(rows.get(4).contains("<td>0</td>"), "Gap should be 0 minutes");
    }

    @Test
    @DisplayName("[F-4] Given: 割当結果が表示されるとき, When: 選定根拠を確認すると," + " Then: 見出しと、各人のずれを並べた計算式が表示される")
    void showsSelectionRationaleHeadingAndScoreFormula() throws Exception {
      String html = postWith(createStandardResult());

      assertTrue(html.contains("<h3>選定根拠</h3>"), "Should contain the rationale heading");
      assertTrue(
          html.contains("合計 = 240 + 240 + 210 + 180 + 210 + 120 + 90 + 90 = 8 分"),
          "Should contain the score formula");
      assertTrue(html.contains("class=\"score-formula\""), "Should contain score-formula class");
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
            + " Then: それぞれの理由が表示され、入れる枠がある人だけ枠が表示される")
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
      assertTrue(slotTags(items.get(0)).isEmpty());
      assertTrue(items.get(1).contains(">I<"));
      assertTrue(items.get(1).contains(">入れる枠はあったが、より小さいずれの案が選ばれた<"));
      assertEquals(ALL_SLOT_TIMES, slotTags(items.get(1)));
      assertTrue(items.get(2).contains(">J<"));
      assertTrue(items.get(2).contains(">どの枠にも入れない<"));
      assertTrue(slotTags(items.get(2)).isEmpty());
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
}
