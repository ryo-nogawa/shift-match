package com.example.shiftmatch.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    @DisplayName("[F-1] Given: GETリクエストが与えられたとき, When: /にアクセスすると, Then: 6つのwishes列を持つHTMLが返される")
    void returnsHtmlWithSixWishesColumns() throws Exception {
      String htmlContent =
          mockMvc
              .perform(get("/"))
              .andExpect(status().isOk())
              .andExpect(content().contentType("text/html;charset=UTF-8"))
              .andReturn()
              .getResponse()
              .getContentAsString();

      // 確認：新しい6枠の願い列が存在する
      assertTrue(
          htmlContent.contains("name=\"employees[0].wishes[0]\""),
          "HTML should contain name=\"employees[0].wishes[0]\"");
      assertTrue(
          htmlContent.contains("name=\"employees[0].wishes[5]\""),
          "HTML should contain name=\"employees[0].wishes[5]\"");

      // 確認：旧早番・遅番の列が存在しない
      assertFalse(
          htmlContent.contains("name=\"employees[0].earlyWish\""),
          "HTML should not contain old earlyWish");
      assertFalse(
          htmlContent.contains("name=\"employees[0].lateWish\""),
          "HTML should not contain old lateWish");

      // 確認：6つの勤務時間がヘッダーに表示される
      assertTrue(htmlContent.contains("07:30〜14:30"), "HTML should contain shift time 07:30〜14:30");
      assertTrue(htmlContent.contains("08:00〜15:30"), "HTML should contain shift time 08:00〜15:30");
      assertTrue(htmlContent.contains("08:30〜16:30"), "HTML should contain shift time 08:30〜16:30");
      assertTrue(htmlContent.contains("09:00〜16:30"), "HTML should contain shift time 09:00〜16:30");
      assertTrue(htmlContent.contains("09:00〜18:00"), "HTML should contain shift time 09:00〜18:00");
      assertTrue(htmlContent.contains("09:00〜18:30"), "HTML should contain shift time 09:00〜18:30");
    }
  }
}
