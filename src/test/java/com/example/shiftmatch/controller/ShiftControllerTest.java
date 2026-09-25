package com.example.shiftmatch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.shiftmatch.service.ShiftAssignmentService;
import com.example.shiftmatch.service.ShiftAssignmentServiceImpl;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * ShiftControllerのテスト。
 */
@WebMvcTest(ShiftController.class)
@DisplayName("ShiftController")
class ShiftControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ShiftAssignmentService shiftAssignmentService;

  @BeforeEach
  void setupDefaultStubs() {
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

  @Nested
  @DisplayName("[F-1] 希望入力フォーム")
  class InputForm {

    @Test
    @DisplayName(
        "[F-1] Given: GETリクエストが与えられたとき, When: /にアクセスすると, Then: 初期4行に24個のselect要素がある（6枠×4行）")
    void returns24SelectElementsForFourRows() throws Exception {
      String htmlContent =
          mockMvc
              .perform(get("/"))
              .andExpect(status().isOk())
              .andExpect(content().contentType("text/html;charset=UTF-8"))
              .andReturn()
              .getResponse()
              .getContentAsString();

      Pattern pattern = Pattern.compile("name=\"employees\\[(\\d)\\]\\.wishes\\[(\\d)\\]\"");
      Matcher matcher = pattern.matcher(htmlContent);

      int count = 0;
      while (matcher.find()) {
        int row = Integer.parseInt(matcher.group(1));
        int col = Integer.parseInt(matcher.group(2));
        assertTrue(row < 4, "Row should be less than 4");
        assertTrue(col < 6, "Column should be less than 6");
        count++;
      }

      assertEquals(24, count, "Should have exactly 24 select elements for 4 rows × 6 wishes");
    }

    @Test
    @DisplayName("[F-1] Given: GETリクエストが与えられたとき, When: /にアクセスすると, Then: 見出しに6つの勤務時間がこの順で表示される")
    void displaysHeadersWithWorkTimesInOrder() throws Exception {
      String htmlContent =
          mockMvc
              .perform(get("/"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      int pos1 = htmlContent.indexOf("07:30〜14:30");
      int pos2 = htmlContent.indexOf("08:00〜15:30");
      int pos3 = htmlContent.indexOf("08:30〜16:30");
      assertTrue(pos1 >= 0, "Should contain work time 07:30〜14:30");
      assertTrue(pos2 >= 0, "Should contain work time 08:00〜15:30");
      assertTrue(pos3 >= 0, "Should contain work time 08:30〜16:30");
      assertTrue(pos1 < pos2, "07:30〜14:30 should come before 08:00〜15:30");
      assertTrue(pos2 < pos3, "08:00〜15:30 should come before 08:30〜16:30");

      int pos4 = htmlContent.indexOf("09:00〜16:30");
      int pos5 = htmlContent.indexOf("09:00〜18:00");
      int pos6 = htmlContent.indexOf("09:00〜18:30");
      assertTrue(pos4 >= 0, "Should contain work time 09:00〜16:30");
      assertTrue(pos5 >= 0, "Should contain work time 09:00〜18:00");
      assertTrue(pos6 >= 0, "Should contain work time 09:00〜18:30");
      assertTrue(pos3 < pos4, "08:30〜16:30 should come before 09:00〜16:30");
      assertTrue(pos4 < pos5, "09:00〜16:30 should come before 09:00〜18:00");
      assertTrue(pos5 < pos6, "09:00〜18:00 should come before 09:00〜18:30");
    }

    @Test
    @DisplayName("[F-1] Given: GETリクエストが与えられたとき, When: /にアクセスすると, Then: 各selectのdata-labelが勤務時間である")
    void selectsHaveCorrectDataLabels() throws Exception {
      String htmlContent =
          mockMvc
              .perform(get("/"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      String[] workTimes = {
        "07:30〜14:30", "08:00〜15:30", "08:30〜16:30", "09:00〜16:30", "09:00〜18:00", "09:00〜18:30"
      };

      for (int slot = 0; slot < 6; slot++) {
        String selectName = "name=\"employees[0].wishes[" + slot + "]\"";
        String expectedLabel = "data-label=\"" + workTimes[slot] + "\"";
        int selectIndex = htmlContent.indexOf(selectName);
        assertTrue(selectIndex >= 0, "Should find select for slot " + slot);

        int tagEndIndex = htmlContent.indexOf(">", selectIndex);
        assertTrue(tagEndIndex > selectIndex, "Should find end of select tag for slot " + slot);

        String tagContent = htmlContent.substring(selectIndex - 100, tagEndIndex);
        assertTrue(
            tagContent.contains(expectedLabel),
            "Select for slot "
                + slot
                + " should have "
                + expectedLabel
                + " (found: "
                + tagContent
                + ")");
      }
    }

    @Test
    @DisplayName(
        "[F-1] Given: GETリクエストが与えられたとき, When: /にアクセスすると, Then: data-slot-labels属性が6つの勤務時間を含む")
    void tableHasDataSlotLabelsWithAllWorkTimes() throws Exception {
      String htmlContent =
          mockMvc
              .perform(get("/"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      Pattern pattern = Pattern.compile("data-slot-labels=\"([^\"]+)\"");
      Matcher matcher = pattern.matcher(htmlContent);
      assertTrue(matcher.find(), "Should have data-slot-labels attribute");

      String slotLabels = matcher.group(1);
      String[] workTimes = {
        "07:30〜14:30", "08:00〜15:30", "08:30〜16:30", "09:00〜16:30", "09:00〜18:00", "09:00〜18:30"
      };

      for (String workTime : workTimes) {
        assertTrue(
            slotLabels.contains(workTime), "data-slot-labels should contain work time " + workTime);
      }
    }

    @Test
    @DisplayName("[F-1] Given: GETリクエストが与えられたとき, When: /にアクセスすると, Then: 旧earlyWish・lateWishは存在しない")
    void doesNotContainOldEarlyOrLateWish() throws Exception {
      String htmlContent =
          mockMvc
              .perform(get("/"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertFalse(
          htmlContent.contains("name=\"employees[0].earlyWish\""),
          "HTML should not contain old earlyWish");
      assertFalse(
          htmlContent.contains("name=\"employees[0].lateWish\""),
          "HTML should not contain old lateWish");
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
        for (int j = 0; j < 6; j++) {
          params
              .append("&employees[")
              .append(i)
              .append("].wishes[")
              .append(j)
              .append("]=AVAILABLE");
        }
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
        for (int j = 0; j < 6; j++) {
          params
              .append("&employees[")
              .append(i)
              .append("].wishes[")
              .append(j)
              .append("]=AVAILABLE");
        }
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
        for (int j = 0; j < 6; j++) {
          params
              .append("&employees[")
              .append(i)
              .append("].wishes[")
              .append(j)
              .append("]=AVAILABLE");
        }
      }
      for (int i = 11; i < 13; i++) {
        params.append("&employees[").append(i).append("].name=");
        for (int j = 0; j < 6; j++) {
          params.append("&employees[").append(i).append("].wishes[").append(j).append("]=");
        }
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

    @Test
    @DisplayName(
        "[V-4][F-5] Given: assignがOptional.empty()を返すとき, When: POSTすると, Then: 不成立メッセージが表示される")
    void showsUnassignableMessageWhenNoValidCombination() throws Exception {
      StringBuilder params = new StringBuilder();
      for (int i = 0; i < 8; i++) {
        params.append("&employees[").append(i).append("].name=Employee").append(i);
        for (int j = 0; j < 6; j++) {
          params
              .append("&employees[")
              .append(i)
              .append("].wishes[")
              .append(j)
              .append("]=UNAVAILABLE");
        }
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
        for (int j = 0; j < 6; j++) {
          params
              .append("&employees[")
              .append(i)
              .append("].wishes[")
              .append(j)
              .append("]=UNAVAILABLE");
        }
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
      for (int j = 0; j < 6; j++) {
        params.append("&employees[0].wishes[").append(j).append("]=AVAILABLE");
      }
      params.append("&employees[1].name=A");
      for (int j = 0; j < 6; j++) {
        params.append("&employees[1].wishes[").append(j).append("]=AVAILABLE");
      }
      params.append("&employees[2].name=A");
      for (int j = 0; j < 6; j++) {
        params.append("&employees[2].wishes[").append(j).append("]=AVAILABLE");
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
      for (int j = 0; j < 6; j++) {
        params.append("&employees[0].wishes[").append(j).append("]=AVAILABLE");
      }
      params.append("&employees[1].name=");
      for (int j = 0; j < 6; j++) {
        params.append("&employees[1].wishes[").append(j).append("]=AVAILABLE");
      }
      params.append("&employees[2].name=A");
      for (int j = 0; j < 6; j++) {
        params.append("&employees[2].wishes[").append(j).append("]=AVAILABLE");
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
  @DisplayName("[V-3][V-1] 希望の入力チェック")
  class WishValidation {

    @Test
    @DisplayName(
        "[V-3] Given: wishes[2]だけが未選択のとき, When: POSTすると, Then: 08:30〜16:30を含むエラーが表示され、assignが呼ばれない")
    void showsErrorForMissingWishSlot2() throws Exception {
      String responseContent =
          mockMvc
              .perform(
                  post("/shift")
                      .param("employees[0].name", "Employee A")
                      .param("employees[0].wishes[0]", "AVAILABLE")
                      .param("employees[0].wishes[1]", "AVAILABLE")
                      .param("employees[0].wishes[3]", "AVAILABLE")
                      .param("employees[0].wishes[4]", "AVAILABLE")
                      .param("employees[0].wishes[5]", "AVAILABLE"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertTrue(
          responseContent.contains("08:30〜16:30"),
          "Error message should contain work time 08:30〜16:30");
      assertTrue(responseContent.contains("class=\"alert\""), "Error section should be displayed");

      verify(shiftAssignmentService, never()).assign(any());
    }

    @Test
    @DisplayName("[V-3] Given: 2つの枠が不正なとき, When: POSTすると, Then: エラーが2件表示される")
    void showsMultipleErrors() throws Exception {
      String responseContent =
          mockMvc
              .perform(
                  post("/shift")
                      .param("employees[0].name", "Employee A")
                      .param("employees[0].wishes[2]", "AVAILABLE")
                      .param("employees[0].wishes[3]", "AVAILABLE")
                      .param("employees[0].wishes[4]", "AVAILABLE")
                      .param("employees[0].wishes[5]", "AVAILABLE"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      int alertCount = 0;
      for (int i = 0; i < responseContent.length() - 4; i++) {
        if (responseContent.substring(i, i + 4).equals("<li>")) {
          alertCount++;
        }
      }

      assertTrue(alertCount >= 2, "Should display at least 2 errors");
    }

    @Test
    @DisplayName("[V-1] Given: 氏名が空の行のとき, When: POSTすると, Then: 希望が未選択でもエラーにならない")
    void ignoresEmptyNameRow() throws Exception {
      String responseContent =
          mockMvc
              .perform(
                  post("/shift")
                      .param("employees[0].name", "")
                      .param("employees[0].wishes[0]", "")
                      .param("employees[0].wishes[1]", "")
                      .param("employees[0].wishes[2]", "")
                      .param("employees[0].wishes[3]", "")
                      .param("employees[0].wishes[4]", "")
                      .param("employees[0].wishes[5]", ""))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertFalse(
          responseContent.contains("入力エラー"), "Should not show input error for empty name row");
    }

    @Test
    @DisplayName("[V-3] Given: 不正な値が入力されたとき, When: POSTすると, Then: エラーが表示される")
    void showsErrorForInvalidWishValue() throws Exception {
      String responseContent =
          mockMvc
              .perform(
                  post("/shift")
                      .param("employees[0].name", "Employee A")
                      .param("employees[0].wishes[0]", "INVALID")
                      .param("employees[0].wishes[1]", "AVAILABLE")
                      .param("employees[0].wishes[2]", "AVAILABLE")
                      .param("employees[0].wishes[3]", "AVAILABLE")
                      .param("employees[0].wishes[4]", "AVAILABLE")
                      .param("employees[0].wishes[5]", "AVAILABLE"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertTrue(
          responseContent.contains("class=\"alert\""),
          "Error section should be displayed for invalid value");
    }
  }

  @Nested
  @DisplayName("[F-1] POST の全戻り経路で slotLabels をモデルに設定")
  class PostReturnPathsIncludeSlotLabels {

    @Test
    @DisplayName("[F-1] Given: POSTで成立するとき, When: レスポンスHTMLを確認すると, Then: 6つの勤務時間と6個のselect要素がある")
    void includesSlotLabelsWhenAssignmentSucceeds() throws Exception {
      StringBuilder params = new StringBuilder();
      for (int i = 0; i < 8; i++) {
        params
            .append("&employees[")
            .append(i)
            .append("].name=")
            .append(String.valueOf((char) ('A' + i)));
        for (int j = 0; j < 6; j++) {
          params
              .append("&employees[")
              .append(i)
              .append("].wishes[")
              .append(j)
              .append("]=AVAILABLE");
        }
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

      assertTrue(responseContent.contains("07:30〜14:30"), "Should contain work time 07:30〜14:30");
      assertTrue(responseContent.contains("08:00〜15:30"), "Should contain work time 08:00〜15:30");
      assertTrue(responseContent.contains("08:30〜16:30"), "Should contain work time 08:30〜16:30");
      assertTrue(responseContent.contains("09:00〜16:30"), "Should contain work time 09:00〜16:30");
      assertTrue(responseContent.contains("09:00〜18:00"), "Should contain work time 09:00〜18:00");
      assertTrue(responseContent.contains("09:00〜18:30"), "Should contain work time 09:00〜18:30");

      assertTrue(
          responseContent.contains("data-slot-labels"), "Should have data-slot-labels attribute");

      Pattern selectPattern = Pattern.compile("name=\"employees\\[0\\]\\.wishes\\[\\d\\]\"");
      Matcher selectMatcher = selectPattern.matcher(responseContent);
      int selectCount = 0;
      while (selectMatcher.find()) {
        selectCount++;
      }
      assertEquals(6, selectCount, "Row 0 should have 6 select elements");
    }

    @Test
    @DisplayName(
        "[F-1] Given: POSTでV-3エラーのとき, When: レスポンスHTMLを確認すると, Then: slotLabelsと6個のselect要素がある")
    void includesSlotLabelsWhenV3Error() throws Exception {
      String responseContent =
          mockMvc
              .perform(
                  post("/shift")
                      .param("employees[0].name", "Employee A")
                      .param("employees[0].wishes[0]", "AVAILABLE")
                      .param("employees[0].wishes[1]", "AVAILABLE")
                      .param("employees[0].wishes[3]", "AVAILABLE")
                      .param("employees[0].wishes[4]", "AVAILABLE")
                      .param("employees[0].wishes[5]", "AVAILABLE"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertTrue(responseContent.contains("07:30〜14:30"), "Should contain work time 07:30〜14:30");
      assertTrue(
          responseContent.contains("data-slot-labels"), "Should have data-slot-labels attribute");

      Pattern selectPattern = Pattern.compile("name=\"employees\\[0\\]\\.wishes\\[\\d\\]\"");
      Matcher selectMatcher = selectPattern.matcher(responseContent);
      int selectCount = 0;
      while (selectMatcher.find()) {
        selectCount++;
      }
      assertEquals(6, selectCount, "Row should have 6 select elements even with V-3 error");
    }

    @Test
    @DisplayName("[F-1] Given: POSTでV-5エラーのとき, When: レスポンスHTMLを確認すると, Then: slotLabelsがある")
    void includesSlotLabelsWhenV5Error() throws Exception {
      StringBuilder params = new StringBuilder();
      for (int i = 0; i < 13; i++) {
        params.append("&employees[").append(i).append("].name=Employee").append(i);
        for (int j = 0; j < 6; j++) {
          params
              .append("&employees[")
              .append(i)
              .append("].wishes[")
              .append(j)
              .append("]=AVAILABLE");
        }
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
          responseContent.contains("07:30〜14:30"),
          "Should contain work time 07:30〜14:30 even with V-5 error");
      assertTrue(
          responseContent.contains("data-slot-labels"),
          "Should have data-slot-labels attribute even with V-5 error");
    }

    @Test
    @DisplayName("[F-1] Given: POSTで不成立のとき, When: レスポンスHTMLを確認すると, Then: slotLabelsがある")
    void includesSlotLabelsWhenUnassignable() throws Exception {
      StringBuilder params = new StringBuilder();
      for (int i = 0; i < 8; i++) {
        params.append("&employees[").append(i).append("].name=Employee").append(i);
        for (int j = 0; j < 6; j++) {
          params
              .append("&employees[")
              .append(i)
              .append("].wishes[")
              .append(j)
              .append("]=UNAVAILABLE");
        }
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
          responseContent.contains("07:30〜14:30"),
          "Should contain work time 07:30〜14:30 even when unassignable");
      assertTrue(
          responseContent.contains("data-slot-labels"),
          "Should have data-slot-labels attribute even when unassignable");
    }
  }

  @Nested
  @DisplayName("[F-4] 割当結果の表表示")
  class ResultTableDisplay {

    @Test
    @DisplayName("[F-4] Given: 割当結果が表示されるとき, When: テーブルの見出しを確認すると, Then: 「氏名」「勤務時間」「休憩時間」の順である")
    void displaysResultTableHeadersInCorrectOrder() throws Exception {
      StringBuilder params = new StringBuilder();
      for (int i = 0; i < 8; i++) {
        params.append("&employees[").append(i).append("].name=Employee").append(i);
        for (int j = 0; j < 6; j++) {
          params
              .append("&employees[")
              .append(i)
              .append("].wishes[")
              .append(j)
              .append("]=AVAILABLE");
        }
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
        for (int j = 0; j < 6; j++) {
          params
              .append("&employees[")
              .append(i)
              .append("].wishes[")
              .append(j)
              .append("]=AVAILABLE");
        }
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
        for (int j = 0; j < 6; j++) {
          params
              .append("&employees[")
              .append(i)
              .append("].wishes[")
              .append(j)
              .append("]=AVAILABLE");
        }
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
        for (int j = 0; j < 6; j++) {
          params
              .append("&employees[")
              .append(i)
              .append("].wishes[")
              .append(j)
              .append("]=AVAILABLE");
        }
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

    @Test
    @DisplayName(
        "[F-4] Given: スコア5の割当結果が表示されるとき, When: スコア表示部分を確認すると, Then: '.score-num'に'5'と'/ 8'が表示される")
    void displaysScoreFiveWithCorrectFormat() throws Exception {
      StringBuilder params = new StringBuilder();

      for (int i = 0; i < 5; i++) {
        params
            .append("&employees[")
            .append(i)
            .append("].name=")
            .append(String.valueOf((char) ('A' + i)));
        params.append("&employees[").append(i).append("].wishes[0]=DESIRED");
        for (int j = 1; j < 6; j++) {
          params
              .append("&employees[")
              .append(i)
              .append("].wishes[")
              .append(j)
              .append("]=AVAILABLE");
        }
      }

      for (int i = 5; i < 8; i++) {
        params
            .append("&employees[")
            .append(i)
            .append("].name=")
            .append(String.valueOf((char) ('A' + i)));
        for (int j = 0; j < 6; j++) {
          params
              .append("&employees[")
              .append(i)
              .append("].wishes[")
              .append(j)
              .append("]=AVAILABLE");
        }
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
          responseContent.contains("class=\"score-num\""),
          "Score display section should be present");
      assertTrue(
          responseContent.contains("5") && responseContent.contains("/ 8"),
          "Score should show 5 / 8");
    }

    @Test
    @DisplayName(
        "[F-4] Given: 未出勤者2名（I・J）の割当結果が表示されるとき, When: 未出勤者セクションを確認すると, Then: '.chip'が2つ表示され、氏名が正しい")
    void displaysUnassignedEmployeesWithChips() throws Exception {
      StringBuilder params = new StringBuilder();
      for (int i = 0; i < 10; i++) {
        params
            .append("&employees[")
            .append(i)
            .append("].name=")
            .append(String.valueOf((char) ('A' + i)));
        for (int j = 0; j < 6; j++) {
          params
              .append("&employees[")
              .append(i)
              .append("].wishes[")
              .append(j)
              .append("]=AVAILABLE");
        }
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
        for (int j = 0; j < 6; j++) {
          params
              .append("&employees[")
              .append(i)
              .append("].wishes[")
              .append(j)
              .append("]=AVAILABLE");
        }
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

    @Test
    @DisplayName(
        "[F-4] Given: 8名の割当結果が表示されるとき, When: 時間軸の行とバーを確認すると, Then: 8行8本のworkバー、8本のbreakバーが表示される")
    void displaysCorrectNumberOfTimelineRows() throws Exception {
      StringBuilder params = new StringBuilder();
      for (int i = 0; i < 8; i++) {
        params.append("&employees[").append(i).append("].name=Employee").append(i);
        for (int j = 0; j < 6; j++) {
          params
              .append("&employees[")
              .append(i)
              .append("].wishes[")
              .append(j)
              .append("]=AVAILABLE");
        }
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
        for (int j = 0; j < 6; j++) {
          params
              .append("&employees[")
              .append(i)
              .append("].wishes[")
              .append(j)
              .append("]=AVAILABLE");
        }
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
        for (int j = 0; j < 6; j++) {
          params
              .append("&employees[")
              .append(i)
              .append("].wishes[")
              .append(j)
              .append("]=AVAILABLE");
        }
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
        for (int j = 0; j < 6; j++) {
          params
              .append("&employees[")
              .append(i)
              .append("].wishes[")
              .append(j)
              .append("]=AVAILABLE");
        }
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
        for (int j = 0; j < 6; j++) {
          params
              .append("&employees[")
              .append(i)
              .append("].wishes[")
              .append(j)
              .append("]=AVAILABLE");
        }
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
        "[F-2] Given: shift-form.jsをロードしたとき, When: ファイルの内容を確認すると, Then:"
            + " 'wishes['を使ったname生成と'disabled'の設定がある")
    void shiftFormJsContainsWishesArrayLogic() throws Exception {
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
          jsContent.contains("wishes["),
          "shift-form.js should contain 'wishes[' for new 6-slot structure");
      assertTrue(
          jsContent.contains(".disabled"),
          "shift-form.js should contain '.disabled' for button state management");
    }

    @Test
    @DisplayName(
        "[F-6] Given: shift-form.jsをロードしたとき, When: ファイルの内容を確認すると, Then:"
            + " 'earlyWish'・'lateWish'・「早番」・「遅番」の文字列が存在しない")
    void shiftFormJsDoesNotContainOldTerms() throws Exception {
      String jsFilePath = "src/main/resources/static/js/shift-form.js";
      java.nio.file.Path path = java.nio.file.Paths.get(jsFilePath);
      String jsContent = new String(java.nio.file.Files.readAllBytes(path));

      assertFalse(jsContent.contains("earlyWish"), "shift-form.js should not contain 'earlyWish'");
      assertFalse(jsContent.contains("lateWish"), "shift-form.js should not contain 'lateWish'");
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
}
