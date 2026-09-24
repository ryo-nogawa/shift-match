package com.example.shiftmatch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
