package com.example.shiftmatch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    @DisplayName("[F-1] Given: 初期状態のとき, When: GET / を実行すると, Then: CSS ファイルへの link タグが含まれること")
    void shouldIncludeCssLink() throws Exception {
      MvcResult result =
          mockMvc
              .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/"))
              .andReturn();

      assertEquals(200, result.getResponse().getStatus());
      String body = result.getResponse().getContentAsString();

      assertTrue(
          body.contains("<link")
              && body.contains("/css/shift-form.css")
              && body.contains("stylesheet"),
          "本文に shift-form.css への stylesheet link が含まれていること");
    }

    @Test
    @DisplayName("[F-1] Given: 初期状態のとき, When: GET / を実行すると, Then: shift-form.css がクラスパス上に存在すること")
    void shouldHaveCssFileOnClasspath() {
      org.springframework.core.io.ClassPathResource cssResource =
          new org.springframework.core.io.ClassPathResource("static/css/shift-form.css");
      assertTrue(cssResource.exists(), "shift-form.css がクラスパス上に存在すること");
    }

    @Test
    @DisplayName(
        "[F-1] Given: 初期状態のとき, When: GET / を実行すると, "
            + "Then: ページの骨格（hero・card・ボタン）が C 案のクラス構成で表示されること")
    void shouldDisplayCardUiWithDesignClasses() throws Exception {
      MvcResult result =
          mockMvc
              .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/"))
              .andReturn();

      assertEquals(200, result.getResponse().getStatus());
      String body = result.getResponse().getContentAsString();

      assertTrue(body.contains("class=\"app\""), "本文に class=\"app\" を持つ要素が含まれること");
      assertTrue(body.contains("class=\"hero\""), "本文に class=\"hero\" が含まれること");
      assertTrue(
          body.contains("Shift Match") && body.contains("class=\"eyebrow\""),
          "本文に class=\"eyebrow\" 内に「Shift Match」が含まれること");
      assertTrue(body.contains("<h1>") && body.contains("シフト作成"), "本文に <h1> で「シフト作成」が含まれること");
      assertTrue(
          body.contains("class=\"lead\"") && body.contains("早番 2 名・遅番 2 名の最適な割り当て案を提案します"),
          "本文に class=\"lead\" 内に「早番 2 名・遅番 2 名の最適な割り当て案を提案します」が含まれること");
      assertTrue(
          body.contains("class=\"card\"") && body.contains("method=\"post\""),
          "本文に class=\"card\" を持つフォームが含まれること");
      assertTrue(
          body.contains("class=\"btn ghost\"") && body.contains("id=\"add-row-btn\""),
          "本文に class=\"btn ghost\" の「行を追加」ボタン（id=\"add-row-btn\"）が含まれること");
      assertTrue(
          body.contains("class=\"btn primary\"") && body.contains("type=\"submit\""),
          "本文に class=\"btn primary\" の送信ボタンが含まれること");
      assertTrue(body.contains("class=\"wish-legend\""), "本文に class=\"wish-legend\" が含まれること");
      assertTrue(body.contains("id=\"row-count\""), "本文に従業員数の表示用 id=\"row-count\" が含まれること");
    }

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
        "[F-1] Given: 初期状態のとき, When: GET / を実行すると, "
            + "Then: 入力行の各 <td> に data-label と <select> に data-value が含まれること")
    void shouldDisplayDataLabelsAndDataValues() throws Exception {
      MvcResult result =
          mockMvc
              .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/"))
              .andReturn();

      assertEquals(200, result.getResponse().getStatus());
      String body = result.getResponse().getContentAsString();

      assertTrue(body.contains("data-label=\"氏名\""), "本文に data-label=\"氏名\" が含まれること");
      assertTrue(body.contains("data-label=\"早番希望\""), "本文に data-label=\"早番希望\" が含まれること");
      assertTrue(body.contains("data-label=\"遅番希望\""), "本文に data-label=\"遅番希望\" が含まれること");
      assertTrue(body.contains("data-value"), "本文に <select> の data-value が含まれること");
    }

    @Test
    @DisplayName(
        "[F-1] Given: 初期状態のとき, When: GET / を実行すると, "
            + "Then: 入力表の見出しに 8:00〜17:00 と 12:00〜21:00 の時間帯が表示されること")
    void shouldDisplayTimeRangeInTableHeaders() throws Exception {
      MvcResult result =
          mockMvc
              .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/"))
              .andReturn();

      assertEquals(200, result.getResponse().getStatus());
      String body = result.getResponse().getContentAsString();

      assertTrue(body.contains("8:00〜17:00"), "本文に 8:00〜17:00 が含まれること");
      assertTrue(body.contains("12:00〜21:00"), "本文に 12:00〜21:00 が含まれること");
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
        "[F-1] Given: 入力に不正値があるとき, When: POST /shift で再表示されると, "
            + "Then: 再表示時の <select> に入力済みの data-value が含まれること")
    void shouldPreserveDataValueWhenReDisplayingAfterError() throws Exception {
      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("employees[0].name", "太郎");
      params.add("employees[0].earlyWish", "DESIRED");
      params.add("employees[0].lateWish", "UNAVAILABLE");
      params.add("employees[1].name", "太郎");
      params.add("employees[1].earlyWish", "AVAILABLE");
      params.add("employees[1].lateWish", "AVAILABLE");

      org.mockito.Mockito.when(
              shiftAssignmentService.findDuplicateNames(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.List.of(new DuplicateNameError("太郎", java.util.List.of(0, 1))));

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();

      assertTrue(
          body.contains("value=\"DESIRED\" selected")
              || body.contains("selected value=\"DESIRED\""),
          "再表示時に早番の選択値 DESIRED が selected 属性で反映されていること");
      assertTrue(
          body.contains("value=\"UNAVAILABLE\" selected")
              || body.contains("selected value=\"UNAVAILABLE\""),
          "再表示時に遅番の選択値 UNAVAILABLE が selected 属性で反映されていること");
    }

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
            + "Then: 割当結果の表ヘッダが「氏名」・「勤務時間」・「休憩時間」の順で出力され、「枠」を含まないこと")
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
      int breakTimePos = headerRow.indexOf("休憩時間");

      assertTrue(framePos == -1, "ヘッダに「枠」が含まれていないこと");
      assertTrue(
          namePos > -1 && workHoursPos > -1 && breakTimePos > -1, "「氏名」・「勤務時間」・「休憩時間」が含まれていること");
      assertTrue(
          namePos < workHoursPos && workHoursPos < breakTimePos,
          "ヘッダが「氏名」・「勤務時間」・「休憩時間」の順で出力されていること");
      int breakOnlyPos = headerRow.indexOf("<th>休憩</th>");
      assertTrue(breakOnlyPos == -1, "「<th>休憩</th>」（「時間」なし）が出力されていないこと");
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

  @Nested
  class UiDesignTest {

    // ヘルパーメソッド

    private com.example.shiftmatch.domain.AssignmentResult createAssignmentResult(
        int score, com.example.shiftmatch.domain.Employee... unassignedEmployees) {
      return new com.example.shiftmatch.domain.AssignmentResult(
          java.util.List.of(
              new com.example.shiftmatch.domain.Employee("太郎", null, null),
              new com.example.shiftmatch.domain.Employee("花子", null, null)),
          java.util.List.of(
              new com.example.shiftmatch.domain.Employee("次郎", null, null),
              new com.example.shiftmatch.domain.Employee("美咲", null, null)),
          score,
          java.util.List.of(unassignedEmployees));
    }

    private void stubAssignSuccess(com.example.shiftmatch.domain.AssignmentResult result) {
      org.mockito.Mockito.when(
              shiftAssignmentService.findDuplicateNames(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.List.of());
      org.mockito.Mockito.when(shiftAssignmentService.assign(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.Optional.of(result));
    }

    private void stubAssignEmpty() {
      org.mockito.Mockito.when(
              shiftAssignmentService.findDuplicateNames(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.List.of());
      org.mockito.Mockito.when(shiftAssignmentService.assign(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.Optional.empty());
    }

    private MultiValueMap<String, String> createValidParams() {
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
      return params;
    }

    private MultiValueMap<String, String> createValidParamsSingleEmployee() {
      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("employees[0].name", "太郎");
      params.add("employees[0].earlyWish", "DESIRED");
      params.add("employees[0].lateWish", "AVAILABLE");
      return params;
    }

    @Test
    @DisplayName(
        "[F-1] Given: 従業員数が上限を超えるとき, When: POST /shift を実行すると, "
            + "Then: .alert と role=\"alert\" が含まれるエラーが表示されること")
    void shouldDisplayLimitExceededErrorWithAlertMarkup() throws Exception {
      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      for (int i = 0; i < 21; i++) {
        params.add("employees[" + i + "].name", "従業員" + i);
        params.add("employees[" + i + "].earlyWish", "DESIRED");
        params.add("employees[" + i + "].lateWish", "AVAILABLE");
      }

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();
      assertTrue(body.contains("class=\"alert\""), "本文に class=\"alert\" が含まれること");
      assertTrue(body.contains("role=\"alert\""), "本文に role=\"alert\" が含まれること");
    }

    @Test
    @DisplayName(
        "[F-1] Given: 氏名が重複しているとき, When: POST /shift を実行すると, "
            + "Then: .alert と role=\"alert\" が含まれるエラーが表示されること")
    void shouldDisplayDuplicateErrorWithAlertMarkup() throws Exception {
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
      assertTrue(body.contains("class=\"alert\""), "本文に class=\"alert\" が含まれること");
      assertTrue(body.contains("role=\"alert\""), "本文に role=\"alert\" が含まれること");
    }

    @Test
    @DisplayName(
        "[F-1] Given: 希望が不正値のとき, When: POST /shift を実行すると, "
            + "Then: .alert と role=\"alert\" が含まれるエラーが表示されること")
    void shouldDisplayWishErrorWithAlertMarkup() throws Exception {
      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("employees[0].name", "太郎");
      params.add("employees[0].earlyWish", "DESIRED");
      params.add("employees[0].lateWish", "INVALID_VALUE");

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();
      assertTrue(body.contains("class=\"alert\""), "本文に class=\"alert\" が含まれること");
      assertTrue(body.contains("role=\"alert\""), "本文に role=\"alert\" が含まれること");
    }

    @Test
    @DisplayName(
        "[F-5] Given: 条件を満たす組み合わせがないとき, When: POST /shift を実行すると, "
            + "Then: 不成立の事実のみが表示され、補足文や対応案は表示されないこと")
    void shouldDisplayOnlyUnassignableMessageWithoutSupplementalText() throws Exception {
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
      assertTrue(body.contains("class=\"card result\""), "本文に class=\"card result\" が含まれること");
      assertTrue(body.contains("class=\"empty\""), "本文に class=\"empty\" が含まれること");
      assertTrue(body.contains("class=\"empty-icon\""), "本文に class=\"empty-icon\" が含まれること");
      assertTrue(body.contains("条件を満たす組み合わせが見つかりませんでした。"), "本文に既存のメッセージが含まれること");
      assertFalse(body.contains("希望（×）を見直すか、従業員を追加してください。"), "本文に補足文が含まれないこと");

      // class="empty" 要素の範囲を特定
      int emptyStart = body.indexOf("class=\"empty\"");
      int emptyEnd = body.indexOf("</div>", emptyStart);
      String emptySection = body.substring(emptyStart, emptyEnd);
      assertFalse(emptySection.contains("<small>"), "class=\"empty\" の要素に <small> が含まれないこと");
    }

    @Test
    @DisplayName(
        "[F-4] Given: 有効な割当が存在するとき, When: POST /shift を実行すると, "
            + "Then: class=\"empty\" が出力されていないこと")
    void shouldNotDisplayEmptyClassWhenAssignmentSucceeds() throws Exception {
      com.example.shiftmatch.domain.AssignmentResult assignmentResult = createAssignmentResult(4);
      stubAssignSuccess(assignmentResult);

      MultiValueMap<String, String> params = createValidParams();

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();
      assertTrue(!body.contains("class=\"empty\""), "成立時に class=\"empty\" が含まれていないこと");
    }

    @Test
    @DisplayName(
        "[F-4] Given: 早番2名・遅番2名・スコア3・未出勤者1名の割当が存在するとき, When: POST /shift を実行すると, "
            + "Then: class=\"score-num\" がスコア3と \" / 4\" を含むこと")
    void shouldDisplayScoreInScoreNum() throws Exception {
      com.example.shiftmatch.domain.AssignmentResult assignmentResult =
          createAssignmentResult(3, new com.example.shiftmatch.domain.Employee("五郎", null, null));
      stubAssignSuccess(assignmentResult);

      MultiValueMap<String, String> params = createValidParamsSingleEmployee();

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();
      assertTrue(
          body.contains("class=\"score-num\"") && body.contains("3") && body.contains(" / 4"),
          "class=\"score-num\" がスコア3と / 4 を含むこと");
    }

    @Test
    @DisplayName(
        "[F-4] Given: 早番2名・遅番2名の割当が存在するとき, When: POST /shift を実行すると, "
            + "Then: class=\"result-table\" が含まれること")
    void shouldDisplayResultTable() throws Exception {
      com.example.shiftmatch.domain.AssignmentResult assignmentResult =
          createAssignmentResult(3, new com.example.shiftmatch.domain.Employee("五郎", null, null));
      stubAssignSuccess(assignmentResult);

      MultiValueMap<String, String> params = createValidParamsSingleEmployee();

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();
      assertTrue(body.contains("class=\"result-table\""), "class=\"result-table\" が含まれること");
    }

    @Test
    @DisplayName(
        "[F-4] Given: 早番2名・遅番2名の割当が存在するとき, When: POST /shift を実行すると, "
            + "Then: pill early が 2 つ・pill late が 2 つ含まれること")
    void shouldDisplayPillEarlyAndLateTwice() throws Exception {
      com.example.shiftmatch.domain.AssignmentResult assignmentResult =
          createAssignmentResult(3, new com.example.shiftmatch.domain.Employee("五郎", null, null));
      stubAssignSuccess(assignmentResult);

      MultiValueMap<String, String> params = createValidParamsSingleEmployee();

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();
      Pattern pillEarlyPattern = Pattern.compile("pill early");
      Pattern pillLatePattern = Pattern.compile("pill late");
      Matcher pillEarlyMatcher = pillEarlyPattern.matcher(body);
      Matcher pillLateMatcher = pillLatePattern.matcher(body);
      int pillEarlyCount = 0;
      int pillLateCount = 0;
      while (pillEarlyMatcher.find()) {
        pillEarlyCount++;
      }
      while (pillLateMatcher.find()) {
        pillLateCount++;
      }
      assertEquals(2, pillEarlyCount, "pill early が 2 つ含まれること");
      assertEquals(2, pillLateCount, "pill late が 2 つ含まれること");
    }

    @Test
    @DisplayName(
        "[F-4] Given: 早番2名・遅番2名・未出勤者1名(五郎)の割当が存在するとき, When: POST /shift を実行すると, "
            + "Then: class=\"unassigned\" の中に class=\"chip\" があり、五郎が含まれること")
    void shouldDisplayUnassignedEmployeeAsChip() throws Exception {
      com.example.shiftmatch.domain.AssignmentResult assignmentResult =
          createAssignmentResult(3, new com.example.shiftmatch.domain.Employee("五郎", null, null));
      stubAssignSuccess(assignmentResult);

      MultiValueMap<String, String> params = createValidParamsSingleEmployee();

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();
      assertTrue(
          body.contains("class=\"unassigned\"")
              && body.contains("class=\"chip\"")
              && body.contains("五郎"),
          "class=\"unassigned\" の中に class=\"chip\" があり、五郎が含まれること");
    }

    @Test
    @DisplayName(
        "[F-4] Given: 早番2名・遅番2名・未出勤者0名の割当が存在するとき, When: POST /shift を実行すると, "
            + "Then: class=\"unassigned\" が含まれていないこと")
    void shouldNotDisplayUnassignedWhenNoUnassignedEmployees() throws Exception {
      com.example.shiftmatch.domain.AssignmentResult assignmentResult = createAssignmentResult(4);
      stubAssignSuccess(assignmentResult);

      MultiValueMap<String, String> params = createValidParamsSingleEmployee();

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();
      assertTrue(
          !body.contains("class=\"unassigned\""), "未出勤者がいない場合、class=\"unassigned\" が含まれていないこと");
    }

    @Test
    @DisplayName(
        "[F-4] Given: 早番2名・遅番2名の有効な割当が存在するとき, When: POST /shift を実行すると, Then: class=\"timeline\" が"
            + " 1 つ、class=\"tl-row\" がちょうど 4 つ、tl-name に太郎・花子・次郎・美咲が順に含まれること")
    void shouldDisplayTimelineWithCorrectRows() throws Exception {
      com.example.shiftmatch.domain.AssignmentResult assignmentResult =
          createAssignmentResult(3, new com.example.shiftmatch.domain.Employee("五郎", null, null));
      stubAssignSuccess(assignmentResult);

      MultiValueMap<String, String> params = createValidParamsSingleEmployee();
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
      Pattern timelinePattern = Pattern.compile("class=\"timeline\"");
      Pattern tlRowPattern = Pattern.compile("class=\"tl-row\"");
      Matcher timelineMatcher = timelinePattern.matcher(body);
      Matcher tlRowMatcher = tlRowPattern.matcher(body);
      int timelineCount = 0;
      int tlRowCount = 0;
      while (timelineMatcher.find()) {
        timelineCount++;
      }
      while (tlRowMatcher.find()) {
        tlRowCount++;
      }
      assertEquals(1, timelineCount, "class=\"timeline\" が 1 つ含まれること");
      assertEquals(4, tlRowCount, "class=\"tl-row\" が 4 つ含まれること");

      // タイムラインの範囲を切り出し、tl-name から氏名を抽出
      int timelineStart = body.indexOf("class=\"timeline\"");
      int resultTableStart = body.indexOf("class=\"result-table\"", timelineStart);
      String timelineSection = body.substring(timelineStart, resultTableStart);

      java.util.List<String> extractedNames = new java.util.ArrayList<>();
      java.util.regex.Pattern pattern =
          java.util.regex.Pattern.compile("<span class=\"tl-name\">([^<]*)</span>");
      java.util.regex.Matcher matcher = pattern.matcher(timelineSection);
      while (matcher.find()) {
        extractedNames.add(matcher.group(1));
      }

      assertEquals(
          java.util.List.of("太郎", "花子", "次郎", "美咲"),
          extractedNames,
          "tl-name に太郎・花子・次郎・美咲が順に含まれること");
    }

    @Test
    @DisplayName(
        "[F-4] Given: 早番2名・遅番2名の有効な割当が存在するとき, When: POST /shift を実行すると, Then: tl-work early が"
            + " left:0.00%;width:69.23% で 2 つ、tl-work late が left:30.77%;width:69.23% で 2 つ含まれること")
    void shouldDisplayTimelineWorkBarsWithCorrectStyles() throws Exception {
      com.example.shiftmatch.domain.AssignmentResult assignmentResult =
          createAssignmentResult(3, new com.example.shiftmatch.domain.Employee("五郎", null, null));
      stubAssignSuccess(assignmentResult);

      MultiValueMap<String, String> params = createValidParamsSingleEmployee();
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
      Pattern earlyStylePattern =
          Pattern.compile("tl-work early.*?left:0\\.00%;width:69\\.23%", Pattern.DOTALL);
      Pattern lateStylePattern =
          Pattern.compile("tl-work late.*?left:30\\.77%;width:69\\.23%", Pattern.DOTALL);
      Matcher earlyMatcher = earlyStylePattern.matcher(body);
      Matcher lateMatcher = lateStylePattern.matcher(body);
      int earlyCount = 0;
      int lateCount = 0;
      while (earlyMatcher.find()) {
        earlyCount++;
      }
      while (lateMatcher.find()) {
        lateCount++;
      }
      assertEquals(2, earlyCount, "tl-work early が left:0.00%;width:69.23% で 2 つ含まれること");
      assertEquals(2, lateCount, "tl-work late が left:30.77%;width:69.23% で 2 つ含まれること");
    }

    @Test
    @DisplayName(
        "[F-4] Given: 早番2名・遅番2名の有効な割当が存在するとき, When: POST /shift を実行すると, "
            + "Then: tl-break が left:38.46%;width:7.69% / 46.15% / 53.85% / 61.54% で 4 つ含まれること")
    void shouldDisplayTimelineBreakBarsWithCorrectStyles() throws Exception {
      com.example.shiftmatch.domain.AssignmentResult assignmentResult =
          createAssignmentResult(3, new com.example.shiftmatch.domain.Employee("五郎", null, null));
      stubAssignSuccess(assignmentResult);

      MultiValueMap<String, String> params = createValidParamsSingleEmployee();
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
      assertTrue(
          body.contains("tl-break")
              && body.contains("left:38.46%;width:7.69%")
              && body.contains("left:46.15%;width:7.69%")
              && body.contains("left:53.85%;width:7.69%")
              && body.contains("left:61.54%;width:7.69%"),
          "tl-break が 4 つの休憩時刻スタイルで含まれること");
    }

    @Test
    @DisplayName(
        "[F-4] Given: 条件を満たす組み合わせがないとき, When: POST /shift を実行すると, "
            + "Then: class=\"timeline\" が含まれていないこと")
    void shouldNotDisplayTimelineWhenNoValidCombination() throws Exception {
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
      assertTrue(!body.contains("class=\"timeline\""), "不成立時に class=\"timeline\" が含まれていないこと");
    }

    @Test
    @DisplayName(
        "[F-4] Given: 早番2名・遅番2名の有効な割当が存在するとき, When: POST /shift を実行すると, Then: class=\"tl-axis\""
            + " に目盛り left が 0.00%, 15.38%, 30.77%, 46.15%, 61.54%, 76.92%, 92.31% で 7 つ")
    void shouldDisplayTimelineAxisWithCorrectScales() throws Exception {
      com.example.shiftmatch.domain.AssignmentResult assignmentResult =
          createAssignmentResult(3, new com.example.shiftmatch.domain.Employee("五郎", null, null));
      stubAssignSuccess(assignmentResult);

      MultiValueMap<String, String> params = createValidParamsSingleEmployee();
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
      assertTrue(body.contains("class=\"tl-axis\""), "class=\"tl-axis\" が含まれること");
      assertTrue(
          body.contains("left:0.00%")
              && body.contains("left:15.38%")
              && body.contains("left:30.77%")
              && body.contains("left:46.15%")
              && body.contains("left:61.54%")
              && body.contains("left:76.92%")
              && body.contains("left:92.31%"),
          "目盛りの 7 つの left 値が含まれること");
      assertTrue(
          body.contains(">8<")
              && body.contains(">10<")
              && body.contains(">12<")
              && body.contains(">14<")
              && body.contains(">16<")
              && body.contains(">18<")
              && body.contains(">20<"),
          "目盛りの 8-20 の偶数文字が含まれること");
    }

    @Test
    @DisplayName(
        "[F-4] Given: 早番2名・遅番2名の有効な割当が存在するとき, When: POST /shift を実行すると, "
            + "Then: class=\"tl-legend\" に「早番」「遅番」「休憩」が含まれること")
    void shouldDisplayTimelineLegend() throws Exception {
      com.example.shiftmatch.domain.AssignmentResult assignmentResult =
          createAssignmentResult(3, new com.example.shiftmatch.domain.Employee("五郎", null, null));
      stubAssignSuccess(assignmentResult);

      MultiValueMap<String, String> params = createValidParamsSingleEmployee();
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
      assertTrue(body.contains("class=\"tl-legend\""), "class=\"tl-legend\" が含まれること");
      assertTrue(
          body.contains("早番") && body.contains("遅番") && body.contains("休憩"), "凡例に早番・遅番・休憩が含まれること");
      assertTrue(
          body.contains("class=\"lg early\"")
              && body.contains("class=\"lg late\"")
              && body.contains("class=\"lg brk\""),
          "凡例に lg early, lg late, lg brk が含まれること");
    }

    @Test
    @DisplayName(
        "[F-4] Given: 有効な割当が存在するとき, When: POST /shift を実行すると, " + "Then: 割当結果の表に「早番」「遅番」が含まれていないこと")
    void shouldNotDisplayEarlyLateTextInResultTable() throws Exception {
      com.example.shiftmatch.domain.AssignmentResult assignmentResult =
          createAssignmentResult(3, new com.example.shiftmatch.domain.Employee("五郎", null, null));
      stubAssignSuccess(assignmentResult);

      MultiValueMap<String, String> params = createValidParamsSingleEmployee();
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
      int tableStart = body.indexOf("<table class=\"result-table\">");
      int tableEnd = body.indexOf("</table>", tableStart);
      assertTrue(tableStart > -1 && tableEnd > tableStart, "result-table が存在すること");
      String tableContent = body.substring(tableStart, tableEnd);
      assertTrue(
          !tableContent.contains("早番") && !tableContent.contains("遅番"), "表内に早番・遅番が含まれていないこと");
    }
  }
}
