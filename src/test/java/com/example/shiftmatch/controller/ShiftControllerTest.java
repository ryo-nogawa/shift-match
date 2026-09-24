package com.example.shiftmatch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.shiftmatch.domain.DuplicateNameError;
import com.example.shiftmatch.service.ShiftAssignmentService;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@WebMvcTest(ShiftController.class)
class ShiftControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ShiftAssignmentService shiftAssignmentService;

  @Nested
  class GetIndexTest {
    @Test
    @DisplayName("[F-1] Given: 初期状態のとき, When: GET / を実行すると, Then: 4行の空フォームが表示されること")
    void shouldDisplayInitialForm() throws Exception {
      MvcResult result =
          mockMvc
              .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/"))
              .andReturn();

      assertEquals(200, result.getResponse().getStatus());

      String viewName = (String) result.getModelAndView().getViewName();
      assertEquals("index", viewName);

      ShiftForm shiftForm = (ShiftForm) result.getModelAndView().getModel().get("shiftForm");
      assertNotNull(shiftForm);
      assertEquals(4, shiftForm.getEmployees().size());
    }

    @Test
    @DisplayName(
        "[F-6] Given: GET / で初期表示するとき, When: 画面を取得すると, "
            + "Then: 入力行（4行）と同数の「削除」ボタン（class=\"delete-row-btn\"）が含まれる")
    void shouldDisplayDeleteButtonsInInitialForm() throws Exception {
      MvcResult result =
          mockMvc
              .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/"))
              .andReturn();

      assertEquals(200, result.getResponse().getStatus());
      String body = result.getResponse().getContentAsString();

      Pattern deleteButtonPattern = Pattern.compile("class=\"delete-row-btn\"");
      Matcher deleteButtonMatcher = deleteButtonPattern.matcher(body);
      int deleteButtonCount = 0;
      while (deleteButtonMatcher.find()) {
        deleteButtonCount++;
      }

      assertEquals(4, deleteButtonCount, "削除ボタンが4個含まれていること");
    }
  }

  @Nested
  class PostShiftTest {
    @Test
    @DisplayName(
        "[V-3] Given: 氏名が入力されていて早番希望が未選択のとき, When: POST /shift を実行すると, "
            + "Then: レスポンス本文に不正エラーを示す文言が含まれる")
    void shouldShowErrorWhenEarlyWishIsEmpty() throws Exception {
      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("employees[0].name", "太郎");
      params.add("employees[0].earlyWish", "");
      params.add("employees[0].lateWish", "AVAILABLE");

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();
      assertTrue(body.contains("不正") || body.contains("エラー"));
    }

    @Test
    @DisplayName(
        "[V-3] Given: 氏名が入力されていて遅番希望が不正値のとき, When: POST /shift を実行すると, "
            + "Then: レスポンス本文に不正エラーを示す文言が含まれる")
    void shouldShowErrorWhenLateWishIsInvalid() throws Exception {
      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("employees[0].name", "太郎");
      params.add("employees[0].earlyWish", "DESIRED");
      params.add("employees[0].lateWish", "INVALID_VALUE");

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();
      assertTrue(body.contains("不正") || body.contains("エラー"));
    }

    @Test
    @DisplayName(
        "[V-3] Given: 氏名が空の行の早番・遅番希望が未選択のとき, When: POST /shift を実行すると, " + "Then: エラーにならない")
    void shouldNotShowErrorForEmptyNameRow() throws Exception {
      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("employees[0].name", "");
      params.add("employees[0].earlyWish", "");
      params.add("employees[0].lateWish", "");

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();
      // 不正エラーが表示されてはいけない
      assertEquals(200, result.getResponse().getStatus());
      assertTrue(!body.contains("入力エラー"));
      java.util.List<?> wishErrors =
          (java.util.List<?>) result.getModelAndView().getModel().get("wishErrors");
      assertTrue(wishErrors != null && wishErrors.isEmpty());
    }

    @Test
    @DisplayName(
        "[V-3] Given: 氏名が空白のみの行の早番・遅番希望が未選択のとき, When: POST /shift を実行すると, " + "Then: エラーにならない")
    void shouldNotShowErrorForBlankNameRow() throws Exception {
      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("employees[0].name", "   ");
      params.add("employees[0].earlyWish", "");
      params.add("employees[0].lateWish", "");

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();
      // 不正エラーが表示されてはいけない
      assertEquals(200, result.getResponse().getStatus());
      assertTrue(!body.contains("入力エラー"));
    }

    @Test
    @DisplayName(
        "[V-2] Given: 氏名が重複しているとき, When: POST /shift を実行すると, "
            + "Then: レスポンス本文に重複エラーを示す文言が含まれ、assign が呼び出されないこと")
    void shouldShowErrorAndNotCallAssignWhenNamesAreDuplicated() throws Exception {
      org.mockito.Mockito.when(
              shiftAssignmentService.findDuplicateNames(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.List.of(new DuplicateNameError("太郎", java.util.List.of(0, 1))));

      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("employees[0].name", "太郎");
      params.add("employees[0].earlyWish", "DESIRED");
      params.add("employees[0].lateWish", "AVAILABLE");
      params.add("employees[1].name", "太郎");
      params.add("employees[1].earlyWish", "AVAILABLE");
      params.add("employees[1].lateWish", "DESIRED");

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();
      assertTrue(body.contains("重複") || body.contains("エラー"));
      assertTrue(body.contains("1") && body.contains("2") && body.contains("行目"));
      verify(shiftAssignmentService, never()).assign(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName(
        "[F-3] Given: V-2・V-3 いずれのエラーもないとき, When: POST /shift を実行すると, "
            + "Then: ShiftAssignmentService#assign が 1 回呼び出されること")
    void shouldCallAssignWhenNoErrorsExist() throws Exception {
      org.mockito.Mockito.when(
              shiftAssignmentService.findDuplicateNames(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.List.of());
      org.mockito.Mockito.when(shiftAssignmentService.assign(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.Optional.empty());

      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("employees[0].name", "太郎");
      params.add("employees[0].earlyWish", "DESIRED");
      params.add("employees[0].lateWish", "AVAILABLE");

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      assertEquals(200, result.getResponse().getStatus());
      verify(shiftAssignmentService).assign(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("[F-4] Given: 有効な割当が存在するとき, When: POST /shift を実行すると, Then: 結果が表形式で表示されること")
    void shouldDisplayResultInTableFormatWhenAssignmentSucceeds() throws Exception {
      com.example.shiftmatch.domain.AssignmentResult assignmentResult =
          new com.example.shiftmatch.domain.AssignmentResult(
              java.util.List.of(
                  new com.example.shiftmatch.domain.Employee("太郎", null, null),
                  new com.example.shiftmatch.domain.Employee("花子", null, null)),
              java.util.List.of(
                  new com.example.shiftmatch.domain.Employee("次郎", null, null),
                  new com.example.shiftmatch.domain.Employee("美咲", null, null)),
              3,
              java.util.List.of(new com.example.shiftmatch.domain.Employee("五郎", null, null)));

      org.mockito.Mockito.when(
              shiftAssignmentService.findDuplicateNames(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.List.of());
      org.mockito.Mockito.when(shiftAssignmentService.assign(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.Optional.of(assignmentResult));

      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("employees[0].name", "太郎");
      params.add("employees[0].earlyWish", "DESIRED");
      params.add("employees[0].lateWish", "AVAILABLE");

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();
      assertTrue(body.contains("太郎"));
      assertTrue(body.contains("花子"));
      assertTrue(body.contains("次郎"));
      assertTrue(body.contains("美咲"));
      assertTrue(body.contains("五郎"));
      assertTrue(body.contains("3"));
    }

    @Test
    @DisplayName(
        "[T-1][F-4] Given: 早番2名・遅番2名の有効な割当が存在するとき, When: POST /shift を実行すると, "
            + "Then: レスポンス本文に4つの休憩時刻（13:00~14:00、14:00~15:00、15:00~16:00、16:00~17:00）が含まれること")
    void shouldDisplayBreakTimesInResultWhenAssignmentSucceeds() throws Exception {
      com.example.shiftmatch.domain.AssignmentResult assignmentResult =
          new com.example.shiftmatch.domain.AssignmentResult(
              java.util.List.of(
                  new com.example.shiftmatch.domain.Employee(
                      "太郎",
                      com.example.shiftmatch.domain.Wish.DESIRED,
                      com.example.shiftmatch.domain.Wish.AVAILABLE),
                  new com.example.shiftmatch.domain.Employee(
                      "花子",
                      com.example.shiftmatch.domain.Wish.AVAILABLE,
                      com.example.shiftmatch.domain.Wish.DESIRED)),
              java.util.List.of(
                  new com.example.shiftmatch.domain.Employee(
                      "次郎",
                      com.example.shiftmatch.domain.Wish.DESIRED,
                      com.example.shiftmatch.domain.Wish.AVAILABLE),
                  new com.example.shiftmatch.domain.Employee(
                      "美咲",
                      com.example.shiftmatch.domain.Wish.AVAILABLE,
                      com.example.shiftmatch.domain.Wish.DESIRED)),
              4,
              java.util.List.of());

      org.mockito.Mockito.when(
              shiftAssignmentService.findDuplicateNames(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.List.of());
      org.mockito.Mockito.when(shiftAssignmentService.assign(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.Optional.of(assignmentResult));

      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("employees[0].name", "太郎");
      params.add("employees[0].earlyWish", "DESIRED");
      params.add("employees[0].lateWish", "AVAILABLE");
      params.add("employees[1].name", "花子");
      params.add("employees[1].earlyWish", "AVAILABLE");
      params.add("employees[1].lateWish", "DESIRED");
      params.add("employees[2].name", "次郎");
      params.add("employees[2].earlyWish", "DESIRED");
      params.add("employees[2].lateWish", "AVAILABLE");
      params.add("employees[3].name", "美咲");
      params.add("employees[3].earlyWish", "AVAILABLE");
      params.add("employees[3].lateWish", "DESIRED");

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();

      int resultSectionStart = body.indexOf("割当結果");
      String resultSection = body.substring(resultSectionStart);

      Pattern tableRowPattern = Pattern.compile("<tr>.*?</tr>", Pattern.DOTALL);
      Matcher tableRowMatcher = tableRowPattern.matcher(resultSection);
      List<String> rows = new ArrayList<>();
      while (tableRowMatcher.find()) {
        rows.add(tableRowMatcher.group());
      }

      List<String> dataRows = rows.subList(1, rows.size());
      assertEquals(4, dataRows.size(), "結果表のデータ行は4行であること");

      assertTrue(
          dataRows.get(0).contains("08:00〜17:00")
              && dataRows.get(0).contains("太郎")
              && Pattern.compile("13:00.*?〜.*?14:00", Pattern.DOTALL)
                  .matcher(dataRows.get(0))
                  .find(),
          "行0（08:00〜17:00・太郎・13:00〜14:00）が見つかりません");

      assertTrue(
          dataRows.get(1).contains("08:00〜17:00")
              && dataRows.get(1).contains("花子")
              && Pattern.compile("14:00.*?〜.*?15:00", Pattern.DOTALL)
                  .matcher(dataRows.get(1))
                  .find(),
          "行1（08:00〜17:00・花子・14:00〜15:00）が見つかりません");

      assertTrue(
          dataRows.get(2).contains("12:00〜21:00")
              && dataRows.get(2).contains("次郎")
              && Pattern.compile("15:00.*?〜.*?16:00", Pattern.DOTALL)
                  .matcher(dataRows.get(2))
                  .find(),
          "行2（12:00〜21:00・次郎・15:00〜16:00）が見つかりません");

      assertTrue(
          dataRows.get(3).contains("12:00〜21:00")
              && dataRows.get(3).contains("美咲")
              && Pattern.compile("16:00.*?〜.*?17:00", Pattern.DOTALL)
                  .matcher(dataRows.get(3))
                  .find(),
          "行3（12:00〜21:00・美咲・16:00〜17:00）が見つかりません");
    }

    @Test
    @DisplayName(
        "[F-4] Given: 有効な割当が存在するとき, When: POST /shift を実行すると, "
            + "Then: 割当結果の表ヘッダが「氏名」・「勤務時間」・「休憩」の順で出力され、「枠」を含まないこと")
    void shouldDisplayWorkHoursHeaderInResultTable() throws Exception {
      com.example.shiftmatch.domain.AssignmentResult assignmentResult =
          new com.example.shiftmatch.domain.AssignmentResult(
              java.util.List.of(
                  new com.example.shiftmatch.domain.Employee(
                      "太郎",
                      com.example.shiftmatch.domain.Wish.DESIRED,
                      com.example.shiftmatch.domain.Wish.AVAILABLE),
                  new com.example.shiftmatch.domain.Employee(
                      "花子",
                      com.example.shiftmatch.domain.Wish.AVAILABLE,
                      com.example.shiftmatch.domain.Wish.DESIRED)),
              java.util.List.of(
                  new com.example.shiftmatch.domain.Employee(
                      "次郎",
                      com.example.shiftmatch.domain.Wish.DESIRED,
                      com.example.shiftmatch.domain.Wish.AVAILABLE),
                  new com.example.shiftmatch.domain.Employee(
                      "美咲",
                      com.example.shiftmatch.domain.Wish.AVAILABLE,
                      com.example.shiftmatch.domain.Wish.DESIRED)),
              4,
              java.util.List.of());

      org.mockito.Mockito.when(
              shiftAssignmentService.findDuplicateNames(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.List.of());
      org.mockito.Mockito.when(shiftAssignmentService.assign(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.Optional.of(assignmentResult));

      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("employees[0].name", "太郎");
      params.add("employees[0].earlyWish", "DESIRED");
      params.add("employees[0].lateWish", "AVAILABLE");
      params.add("employees[1].name", "花子");
      params.add("employees[1].earlyWish", "AVAILABLE");
      params.add("employees[1].lateWish", "DESIRED");
      params.add("employees[2].name", "次郎");
      params.add("employees[2].earlyWish", "DESIRED");
      params.add("employees[2].lateWish", "AVAILABLE");
      params.add("employees[3].name", "美咲");
      params.add("employees[3].earlyWish", "AVAILABLE");
      params.add("employees[3].lateWish", "DESIRED");

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();

      int resultSectionStart = body.indexOf("割当結果");
      String resultSection = body.substring(resultSectionStart);

      Pattern headerRowPattern = Pattern.compile("<tr>.*?</tr>", Pattern.DOTALL);
      Matcher headerRowMatcher = headerRowPattern.matcher(resultSection);
      headerRowMatcher.find();

      String headerRow = headerRowMatcher.group();

      int framePos = headerRow.indexOf("枠");
      int namePos = headerRow.indexOf("氏名");
      int workHoursPos = headerRow.indexOf("勤務時間");
      int breakPos = headerRow.indexOf("休憩");

      assertTrue(framePos == -1, "ヘッダに「枠」が含まれていないこと");
      assertTrue(namePos > -1 && workHoursPos > -1 && breakPos > -1, "「氏名」・「勤務時間」・「休憩」が含まれていること");
      assertTrue(
          namePos < workHoursPos && workHoursPos < breakPos, "ヘッダが「氏名」・「勤務時間」・「休憩」の順で出力されていること");
    }

    @Test
    @DisplayName(
        "[F-4] Given: 早番2名・遅番2名の有効な割当が存在するとき, When: POST /shift を実行すると, "
            + "Then: 早番の行に08:00〜17:00、遅番の行に12:00〜21:00が表示され、「早番」「遅番」は表示されないこと")
    void shouldDisplayWorkHoursForEachShift() throws Exception {
      com.example.shiftmatch.domain.AssignmentResult assignmentResult =
          new com.example.shiftmatch.domain.AssignmentResult(
              java.util.List.of(
                  new com.example.shiftmatch.domain.Employee(
                      "太郎",
                      com.example.shiftmatch.domain.Wish.DESIRED,
                      com.example.shiftmatch.domain.Wish.AVAILABLE),
                  new com.example.shiftmatch.domain.Employee(
                      "花子",
                      com.example.shiftmatch.domain.Wish.AVAILABLE,
                      com.example.shiftmatch.domain.Wish.DESIRED)),
              java.util.List.of(
                  new com.example.shiftmatch.domain.Employee(
                      "次郎",
                      com.example.shiftmatch.domain.Wish.DESIRED,
                      com.example.shiftmatch.domain.Wish.AVAILABLE),
                  new com.example.shiftmatch.domain.Employee(
                      "美咲",
                      com.example.shiftmatch.domain.Wish.AVAILABLE,
                      com.example.shiftmatch.domain.Wish.DESIRED)),
              4,
              java.util.List.of());

      org.mockito.Mockito.when(
              shiftAssignmentService.findDuplicateNames(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.List.of());
      org.mockito.Mockito.when(shiftAssignmentService.assign(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.Optional.of(assignmentResult));

      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("employees[0].name", "太郎");
      params.add("employees[0].earlyWish", "DESIRED");
      params.add("employees[0].lateWish", "AVAILABLE");
      params.add("employees[1].name", "花子");
      params.add("employees[1].earlyWish", "AVAILABLE");
      params.add("employees[1].lateWish", "DESIRED");
      params.add("employees[2].name", "次郎");
      params.add("employees[2].earlyWish", "DESIRED");
      params.add("employees[2].lateWish", "AVAILABLE");
      params.add("employees[3].name", "美咲");
      params.add("employees[3].earlyWish", "AVAILABLE");
      params.add("employees[3].lateWish", "DESIRED");

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();

      int resultSectionStart = body.indexOf("割当結果");
      String resultSection = body.substring(resultSectionStart);

      Pattern tableRowPattern = Pattern.compile("<tr>.*?</tr>", Pattern.DOTALL);
      Matcher tableRowMatcher = tableRowPattern.matcher(resultSection);
      List<String> rows = new ArrayList<>();
      while (tableRowMatcher.find()) {
        rows.add(tableRowMatcher.group());
      }

      List<String> dataRows = rows.subList(1, rows.size());
      assertEquals(4, dataRows.size(), "結果表のデータ行は4行であること");

      assertTrue(
          dataRows.get(0).contains("08:00〜17:00")
              && dataRows.get(0).contains("太郎")
              && !dataRows.get(0).contains("早番"),
          "行0（08:00〜17:00・太郎）が同一行内に含まれ、「早番」は含まれていないこと");

      assertTrue(
          dataRows.get(1).contains("08:00〜17:00")
              && dataRows.get(1).contains("花子")
              && !dataRows.get(1).contains("早番"),
          "行1（08:00〜17:00・花子）が同一行内に含まれ、「早番」は含まれていないこと");

      assertTrue(
          dataRows.get(2).contains("12:00〜21:00")
              && dataRows.get(2).contains("次郎")
              && !dataRows.get(2).contains("遅番"),
          "行2（12:00〜21:00・次郎）が同一行内に含まれ、「遅番」は含まれていないこと");

      assertTrue(
          dataRows.get(3).contains("12:00〜21:00")
              && dataRows.get(3).contains("美咲")
              && !dataRows.get(3).contains("遅番"),
          "行3（12:00〜21:00・美咲）が同一行内に含まれ、「遅番」は含まれていないこと");
    }

    @Test
    @DisplayName(
        "[F-4] Given: 有効な割当が存在するとき, When: POST /shift を実行すると, "
            + "Then: 割当結果の表のデータ行で、各行内で氏名が勤務時間より前に出力されること")
    void shouldDisplayNameBeforeWorkHoursInDataRows() throws Exception {
      com.example.shiftmatch.domain.AssignmentResult assignmentResult =
          new com.example.shiftmatch.domain.AssignmentResult(
              java.util.List.of(
                  new com.example.shiftmatch.domain.Employee(
                      "太郎",
                      com.example.shiftmatch.domain.Wish.DESIRED,
                      com.example.shiftmatch.domain.Wish.AVAILABLE),
                  new com.example.shiftmatch.domain.Employee(
                      "花子",
                      com.example.shiftmatch.domain.Wish.AVAILABLE,
                      com.example.shiftmatch.domain.Wish.DESIRED)),
              java.util.List.of(
                  new com.example.shiftmatch.domain.Employee(
                      "次郎",
                      com.example.shiftmatch.domain.Wish.DESIRED,
                      com.example.shiftmatch.domain.Wish.AVAILABLE),
                  new com.example.shiftmatch.domain.Employee(
                      "美咲",
                      com.example.shiftmatch.domain.Wish.AVAILABLE,
                      com.example.shiftmatch.domain.Wish.DESIRED)),
              4,
              java.util.List.of());

      org.mockito.Mockito.when(
              shiftAssignmentService.findDuplicateNames(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.List.of());
      org.mockito.Mockito.when(shiftAssignmentService.assign(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.Optional.of(assignmentResult));

      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("employees[0].name", "太郎");
      params.add("employees[0].earlyWish", "DESIRED");
      params.add("employees[0].lateWish", "AVAILABLE");
      params.add("employees[1].name", "花子");
      params.add("employees[1].earlyWish", "AVAILABLE");
      params.add("employees[1].lateWish", "DESIRED");
      params.add("employees[2].name", "次郎");
      params.add("employees[2].earlyWish", "DESIRED");
      params.add("employees[2].lateWish", "AVAILABLE");
      params.add("employees[3].name", "美咲");
      params.add("employees[3].earlyWish", "AVAILABLE");
      params.add("employees[3].lateWish", "DESIRED");

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();

      int resultSectionStart = body.indexOf("割当結果");
      String resultSection = body.substring(resultSectionStart);

      Pattern tableRowPattern = Pattern.compile("<tr>.*?</tr>", Pattern.DOTALL);
      Matcher tableRowMatcher = tableRowPattern.matcher(resultSection);
      List<String> rows = new ArrayList<>();
      while (tableRowMatcher.find()) {
        rows.add(tableRowMatcher.group());
      }

      List<String> dataRows = rows.subList(1, rows.size());
      assertEquals(4, dataRows.size(), "結果表のデータ行は4行であること");

      for (int i = 0; i < dataRows.size(); i++) {
        String row = dataRows.get(i);
        int namePos = row.indexOf(i < 2 ? (i == 0 ? "太郎" : "花子") : (i == 2 ? "次郎" : "美咲"));
        int workHoursPos = row.indexOf(i < 2 ? "08:00〜17:00" : "12:00〜21:00");
        assertTrue(
            namePos > -1 && workHoursPos > -1 && namePos < workHoursPos,
            "行" + i + "で氏名が勤務時間より前に出力されていること");
      }
    }

    @Test
    @DisplayName(
        "[セキュリティー] Given: 有効な氏名を持つ行が上限（20名）を超えるとき, When: POST /shift を実行すると, "
            + "Then: assign が呼び出されず、上限超過のエラーメッセージが表示されること")
    void shouldRejectWhenEmployeeCountExceedsLimit() throws Exception {
      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      for (int i = 0; i < 21; i++) {
        params.add("employees[" + i + "].name", "従業員" + i);
        params.add("employees[" + i + "].earlyWish", "DESIRED");
        params.add("employees[" + i + "].lateWish", "AVAILABLE");
      }

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();
      assertTrue(body.contains("上限"));
      verify(shiftAssignmentService, never()).assign(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName(
        "[F-6] Given: 2行を送信して POST /shift で再表示するとき, When: 入力行が再描画されると, "
            + "Then: 送信した行数と同じ2個の「削除」ボタンが含まれ、削除した行は復活しない")
    void shouldDisplayDeleteButtonsInPostResponse() throws Exception {
      org.mockito.Mockito.when(
              shiftAssignmentService.findDuplicateNames(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.List.of());
      org.mockito.Mockito.when(shiftAssignmentService.assign(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.Optional.empty());

      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("employees[0].name", "太郎");
      params.add("employees[0].earlyWish", "DESIRED");
      params.add("employees[0].lateWish", "AVAILABLE");
      params.add("employees[1].name", "花子");
      params.add("employees[1].earlyWish", "AVAILABLE");
      params.add("employees[1].lateWish", "");

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();

      Pattern deleteButtonPattern = Pattern.compile("class=\"delete-row-btn\"");
      Matcher deleteButtonMatcher = deleteButtonPattern.matcher(body);
      int deleteButtonCount = 0;
      while (deleteButtonMatcher.find()) {
        deleteButtonCount++;
      }

      assertEquals(2, deleteButtonCount, "送信した行数（2行）と同数の削除ボタンが含まれること");
    }

    @Test
    @DisplayName("[F-5] Given: 条件を満たす組み合わせがないとき, When: POST /shift を実行すると, Then: 不成立メッセージが表示されること")
    void shouldDisplayUnassignableMessageWhenNoValidCombinationExists() throws Exception {
      org.mockito.Mockito.when(
              shiftAssignmentService.findDuplicateNames(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.List.of());
      org.mockito.Mockito.when(shiftAssignmentService.assign(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.Optional.empty());

      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("employees[0].name", "太郎");
      params.add("employees[0].earlyWish", "DESIRED");
      params.add("employees[0].lateWish", "AVAILABLE");

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();
      assertTrue(body.contains("条件を満たす組み合わせが見つかりませんでした。"));
    }

    @Test
    @DisplayName(
        "[V-4] Given: 従業員パラメータが一切送られないとき, When: POST /shift を実行すると, "
            + "Then: 例外を投げずにステータス200で不成立と扱われること")
    void shouldHandleEmptyEmployeeListWithoutException() throws Exception {
      org.mockito.Mockito.when(
              shiftAssignmentService.findDuplicateNames(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.List.of());
      org.mockito.Mockito.when(shiftAssignmentService.assign(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.Optional.empty());

      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      assertEquals(200, result.getResponse().getStatus());
      String body = result.getResponse().getContentAsString();
      assertTrue(body.contains("条件を満たす組み合わせが見つかりませんでした。"));
    }

    @Test
    @DisplayName(
        "[F-6] Given: 従業員パラメータが一切送られないとき, When: POST /shift で再表示すると, "
            + "Then: 入力行が最低1行残り、削除ボタンが1個含まれる")
    void shouldKeepOneRowWhenNoEmployeeParametersArePosted() throws Exception {
      org.mockito.Mockito.when(
              shiftAssignmentService.findDuplicateNames(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.List.of());
      org.mockito.Mockito.when(shiftAssignmentService.assign(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.Optional.empty());

      MvcResult result =
          mockMvc
              .perform(
                  MockMvcRequestBuilders.post("/shift")
                      .params(new LinkedMultiValueMap<String, String>()))
              .andReturn();

      String body = result.getResponse().getContentAsString();
      assertTrue(body.contains("name=\"employees[0].name\""));
      Matcher matcher = Pattern.compile("class=\"delete-row-btn\"").matcher(body);
      int count = 0;
      while (matcher.find()) {
        count++;
      }
      assertEquals(1, count, "削除ボタンが1個含まれること");
    }
  }
}
