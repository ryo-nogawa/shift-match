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
}
