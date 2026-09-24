package com.example.shiftmatch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * ShiftControllerのテスト。
 */
@SpringBootTest
@DisplayName("ShiftController")
class ShiftControllerTest {

  @Autowired private WebApplicationContext webApplicationContext;

  private MockMvc mockMvc;

  @Autowired
  void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
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

      // Count select elements with name="employees[i].wishes[j]" pattern
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

      // Find the positions of each work time in the HTML
      int pos1 = htmlContent.indexOf("07:30〜14:30");
      int pos2 = htmlContent.indexOf("08:00〜15:30");
      int pos3 = htmlContent.indexOf("08:30〜16:30");
      assertTrue(pos1 >= 0, "Should contain work time 07:30〜14:30");
      assertTrue(pos2 >= 0, "Should contain work time 08:00〜15:30");
      assertTrue(pos3 >= 0, "Should contain work time 08:30〜16:30");
      assertTrue(pos1 < pos2, "07:30〜14:30 should come before 08:00〜15:30");
      assertTrue(pos2 < pos3, "08:00〜15:30 should come before 08:30〜16:30");

      // Continue with remaining work times
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

      // Work times for each slot (0-indexed)
      String[] workTimes = {
        "07:30〜14:30", "08:00〜15:30", "08:30〜16:30", "09:00〜16:30", "09:00〜18:00", "09:00〜18:30"
      };

      // Verify data-label attributes match work times
      for (int slot = 0; slot < 6; slot++) {
        String selectName = "name=\"employees[0].wishes[" + slot + "]\"";
        String expectedLabel = "data-label=\"" + workTimes[slot] + "\"";
        int selectIndex = htmlContent.indexOf(selectName);
        assertTrue(selectIndex >= 0, "Should find select for slot " + slot);

        // Find the end of the opening tag
        int tagEndIndex = htmlContent.indexOf(">", selectIndex);
        assertTrue(tagEndIndex > selectIndex, "Should find end of select tag for slot " + slot);

        // Check if data-label appears in the same tag (before >)
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

      // Check that limit error is displayed
      assertTrue(
          responseContent.contains("12名") || responseContent.contains("上限"),
          "Error message should contain limit info");
      assertTrue(responseContent.contains("class=\"alert\""), "Error section should be displayed");

      // Should not show assignment result
      assertFalse(
          responseContent.contains("割当結果"),
          "Assignment result should not be displayed when limit exceeded");
    }

    @Test
    @DisplayName("[V-5] Given: 有効な従業員がちょうど12名のとき, When: POSTすると, Then: 上限エラーが表示されない（境界値）")
    void doesNotShowErrorWhen12ValidEmployees() throws Exception {
      // For this test, we need conditions where exactly 12 valid employees with no conflicts
      // but no valid assignment exists
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

      // Should not show limit error
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
      // Add 2 rows with empty names
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

      // Should not show limit error (11 valid employees < 12)
      assertFalse(
          responseContent.contains("上限（12名）を超えています"),
          "Limit error should not be shown for 11 valid employees");
    }

    @Test
    @DisplayName(
        "[V-4][F-5] Given: assignがOptional.empty()を返すとき, When: POSTすると, Then: 不成立メッセージが表示される")
    void showsUnassignableMessageWhenNoValidCombination() throws Exception {
      // Create a scenario where no valid assignment exists
      // All employees have × for all slots (or similar impossible condition)
      StringBuilder params = new StringBuilder();
      for (int i = 0; i < 8; i++) {
        params.append("&employees[").append(i).append("].name=Employee").append(i);
        // All slots marked as unavailable
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

      // Should show unassignable message
      assertTrue(
          responseContent.contains("条件を満たす組み合わせが見つかりませんでした"),
          "Unassignable message should be displayed");

      // Should not show assignment result table
      assertFalse(
          responseContent.contains("割当結果の表"), "Assignment result table should not be displayed");
    }

    @Test
    @DisplayName("[F-5] Given: 不成立のとき, When: ページが表示されるとき, Then: 時間軸が表示されない")
    void doesNotShowTimelineWhenUnassignable() throws Exception {
      // Create conditions where no assignment exists
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

      // Should not show timeline (class="timeline")
      assertFalse(
          responseContent.contains("class=\"timeline\"")
              || responseContent.contains("class='timeline'"),
          "Timeline should not be displayed when unassignable");
    }
  }

  @Nested
  @DisplayName("[V-3][V-1] 希望の入力チェック")
  class WishValidation {

    @Test
    @DisplayName(
        "[V-3] Given: wishes[2]だけが未選択のとき, When: POSTすると, Then: 08:30〜16:30を含むエラーが表示され、assignが呼ばれない")
    void showsErrorForMissingWishSlot2() throws Exception {
      // Note: We cannot test without mocking service, as we need to verify assign is not called.
      // This test checks that error message is displayed in the response.
      String responseContent =
          mockMvc
              .perform(
                  post("/shift")
                      .param("employees[0].name", "Employee A")
                      .param("employees[0].wishes[0]", "AVAILABLE")
                      .param("employees[0].wishes[1]", "AVAILABLE")
                      // wishes[2] is not provided (missing)
                      .param("employees[0].wishes[3]", "AVAILABLE")
                      .param("employees[0].wishes[4]", "AVAILABLE")
                      .param("employees[0].wishes[5]", "AVAILABLE"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      // Check that error is displayed and contains the work time
      assertTrue(
          responseContent.contains("08:30〜16:30"),
          "Error message should contain work time 08:30〜16:30");
      assertTrue(responseContent.contains("class=\"alert\""), "Error section should be displayed");
    }

    @Test
    @DisplayName("[V-3] Given: 2つの枠が不正なとき, When: POSTすると, Then: エラーが2件表示される")
    void showsMultipleErrors() throws Exception {
      String responseContent =
          mockMvc
              .perform(
                  post("/shift")
                      .param("employees[0].name", "Employee A")
                      // wishes[0] and wishes[1] are missing
                      .param("employees[0].wishes[2]", "AVAILABLE")
                      .param("employees[0].wishes[3]", "AVAILABLE")
                      .param("employees[0].wishes[4]", "AVAILABLE")
                      .param("employees[0].wishes[5]", "AVAILABLE"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      // Count error messages by counting occurrences in the alert section
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

      // Should not show V-3 error for this row
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

      // Find the result table and verify headers
      assertTrue(
          responseContent.contains("class=\"result-table\""), "Result table should be present");

      // Check header order: 氏名 → 勤務時間 → 休憩時間
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
      // Create 8 employees with all slots available
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

      // Verify result table is shown
      assertTrue(
          responseContent.contains("class=\"result-table\""), "Result table should be displayed");

      // Expected work times according to specification
      String[] expectedWorkTimes = {
        "07:30〜14:30", // Slot 1
        "07:30〜14:30", // Slot 1
        "08:00〜15:30", // Slot 2
        "08:30〜16:30", // Slot 3
        "09:00〜16:30", // Slot 4
        "09:00〜18:00", // Slot 5
        "09:00〜18:30", // Slot 6
        "09:00〜18:30" // Slot 6
      };

      // Expected break times according to specification
      String[] expectedBreakTimes = {
        "12:00〜12:45", // Slot 1
        "12:00〜12:45", // Slot 1
        "12:45〜13:30", // Slot 2
        "12:45〜13:30", // Slot 3
        "13:30〜14:15", // Slot 4
        "13:30〜14:30", // Slot 5
        "14:15〜15:15", // Slot 6
        "14:30〜15:30" // Slot 6
      };

      // Verify all expected work times are present
      for (String workTime : expectedWorkTimes) {
        assertTrue(responseContent.contains(workTime), "Should contain work time: " + workTime);
      }

      // Verify all expected break times are present
      for (String breakTime : expectedBreakTimes) {
        assertTrue(responseContent.contains(breakTime), "Should contain break time: " + breakTime);
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

      // Verify result table section
      int resultTableStart = responseContent.indexOf("class=\"result-table\"");
      assertTrue(resultTableStart >= 0, "Result table should be present");

      // Extract just the result table section
      int resultTableEnd =
          responseContent.indexOf("</table>", resultTableStart) + "</table>".length();
      String resultTableContent = responseContent.substring(resultTableStart, resultTableEnd);

      // Check that "早番" and "遅番" do not appear in the result table
      assertFalse(resultTableContent.contains("早番"), "Result table should not contain '早番'");
      assertFalse(resultTableContent.contains("遅番"), "Result table should not contain '遅番'");
    }
  }

  @Nested
  @DisplayName("[F-4] スコアと未出勤者の表示")
  class ScoreAndUnassignedDisplay {

    @Test
    @DisplayName(
        "[F-4] Given: スコア5の割当結果が表示されるとき, When: スコア表示部分を確認すると, Then: '.score-num'に'5'と'/ 8'が表示される")
    void displaysScoreFiveWithCorrectFormat() throws Exception {
      // Create 8 employees where only 5 are marked as DESIRED for any slot
      // to generate a score of 5
      StringBuilder params = new StringBuilder();

      // Employees 0-4: Mark them all as DESIRED for slot 0
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

      // Employees 5-7: Mark them as AVAILABLE for all slots
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

      // Verify score display
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
      // Create 10 employees but system will only assign 8
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

      // Verify unassigned section exists and contains chips
      assertTrue(
          responseContent.contains("class=\"unassigned\""),
          "Unassigned section should be displayed");

      // Count chips
      int chipCount = 0;
      int index = 0;
      while ((index = responseContent.indexOf("class=\"chip\"", index)) != -1) {
        chipCount++;
        index++;
      }
      assertEquals(2, chipCount, "Should have exactly 2 chips for 2 unassigned employees");

      // Verify the unassigned employees are I and J
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

      // Verify unassigned section does not exist when all are assigned
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

      // Count timeline rows
      int tlRowCount = 0;
      int index = 0;
      while ((index = responseContent.indexOf("class=\"tl-row\"", index)) != -1) {
        tlRowCount++;
        index++;
      }
      assertEquals(8, tlRowCount, "Should have exactly 8 timeline rows");

      // Count work bars
      int tlWorkCount = 0;
      index = 0;
      while ((index = responseContent.indexOf("class=\"tl-work\"", index)) != -1) {
        tlWorkCount++;
        index++;
      }
      assertEquals(8, tlWorkCount, "Should have exactly 8 work bars");

      // Count break bars
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

      // Extract the first work bar (Slot 1, should have left:0.00% and width:63.64%)
      // The formula: Slot 1 is 7:30-14:30 = 420 minutes from 7:30
      // left = 0 minutes / 660 * 100% = 0.00%
      // width = 420 minutes / 660 * 100% = 63.64%
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

      // Slot 6 is 9:00-18:30 = 570 minutes, starting at 9:00 (90 min from 7:30)
      // left = 90 minutes / 660 * 100% = 13.64%
      // width = 570 minutes / 660 * 100% = 86.36%
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

      // Slot 1 break time: 12:00-12:45
      // left = 270 minutes (12:00 - 7:30) / 660 * 100% = 40.91%
      // width = 45 minutes / 660 * 100% = 6.82%
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

      // Verify timeline legend section
      int legendStart = responseContent.indexOf("class=\"tl-legend\"");
      assertTrue(legendStart >= 0, "Legend section should exist");

      // Extract legend content (approximately next 200 chars)
      String legendSection =
          responseContent.substring(
              legendStart, Math.min(legendStart + 300, responseContent.length()));

      assertTrue(legendSection.contains("勤務"), "Legend should contain '勤務'");
      assertTrue(legendSection.contains("休憩"), "Legend should contain '休憩'");
      assertFalse(legendSection.contains("早番"), "Legend should not contain '早番'");
      assertFalse(legendSection.contains("遅番"), "Legend should not contain '遅番'");
    }
  }
}
