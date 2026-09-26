package com.example.shiftmatch.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.shiftmatch.service.MonthlyShiftService;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * ShiftControllerのテスト。月間入力画面（画面1・2）向け。
 */
@WebMvcTest(ShiftController.class)
@DisplayName("ShiftController")
class ShiftControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private MonthlyShiftService monthlyShiftService;

  @MockitoBean private MonthlyFormConverter monthlyFormConverter;

  /**
   * 廃止した枠ごとの 3 段階の希望入力の名残を検出する語（小文字）。
   *
   * <p>ソースに廃止した入力の残骸がないことを文字列検索で確認できるよう、分割して記述します。
   */
  private static final String LEGACY_TOKEN = "wi" + "sh";

  @Nested
  @DisplayName("[F-1][F-2] 月間入力フォーム")
  class MonthlyInputForm {

    @Test
    @DisplayName("[F-1] Given: GET /が与えられたとき, When: アクセスすると, Then: 廃止した希望入力の語が存在しない")
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

    @Test
    @DisplayName(
        "[F-1][F-2] Given: GET /が与えられたとき, When: アクセスすると," + " Then: 12行の従業員入力行と基本シフトパネルがある")
    void containsTwelveEmployeeRows() throws Exception {
      String html =
          mockMvc
              .perform(get("/"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertTrue(html.contains("name=\"employees[0].name\""), "Should have employees[0].name");
      assertTrue(html.contains("name=\"employees[11].name\""), "Should have employees[11].name");
      assertFalse(
          html.contains("name=\"employees[12].name\""), "Should not have employees[12].name");
      assertTrue(html.contains("id=\"screen-1\""), "Should have screen-1");
      assertTrue(html.contains("id=\"screen-2\""), "Should have screen-2");
      assertTrue(html.contains("id=\"screen-3\""), "Should have screen-3");
    }

    @Test
    @DisplayName(
        "[F-1] Given: GET /が与えられたとき, When: 月変更と営業日情報を確認すると,"
            + " Then: targetMonthとmonth-summaryがある")
    void containsMonthSelectorAndSummary() throws Exception {
      String html =
          mockMvc
              .perform(get("/"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertTrue(html.contains("name=\"targetMonth\""), "Should have targetMonth input");
      assertTrue(html.contains("id=\"month-summary\""), "Should have month-summary");
      assertTrue(html.contains("id=\"prev-month-btn\""), "Should have prev-month button");
      assertTrue(html.contains("id=\"next-month-btn\""), "Should have next-month button");
    }

    @Test
    @DisplayName("[F-9] Given: GET /が与えられたとき, When: ナビゲーション要素を確認すると, Then: ステップボタンがある")
    void containsStepNavigation() throws Exception {
      String html =
          mockMvc
              .perform(get("/"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertTrue(html.contains("data-step=\"1\""), "Should have step 1 button");
      assertTrue(html.contains("data-step=\"2\""), "Should have step 2 button");
      assertTrue(html.contains("data-step=\"3\""), "Should have step 3 button");
      assertTrue(html.contains("id=\"prev-btn\""), "Should have prev button");
      assertTrue(html.contains("id=\"next-btn\""), "Should have next button");
    }

    @Test
    @DisplayName(
        "[F-6][F-8] Given: GET /が与えられたとき, When: 基本シフトパネルを確認すると," + " Then: 曜日ごとの休み・開始・終了がある")
    void containsBaseShiftPanel() throws Exception {
      String html =
          mockMvc
              .perform(get("/"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertTrue(html.contains("id=\"base-panels\""), "Should have base-panels");
      assertTrue(
          html.contains("name=\"employees[0].days[0].off\""),
          "Should have employees[0].days[0].off");
      assertTrue(
          html.contains("name=\"employees[0].days[0].start\""),
          "Should have employees[0].days[0].start");
      assertTrue(
          html.contains("name=\"employees[0].days[0].end\""),
          "Should have employees[0].days[0].end");
    }

    @Test
    @DisplayName("[F-11] Given: GET /が与えられたとき, When: 日ごとの希望パネルを確認すると, Then: カレンダーと日パネルがある")
    void containsDayAdjustmentElements() throws Exception {
      String html =
          mockMvc
              .perform(get("/"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();

      assertTrue(html.contains("id=\"calendar\""), "Should have calendar");
      assertTrue(html.contains("id=\"day-panel\""), "Should have day-panel");
      assertTrue(html.contains("id=\"adjustment-inputs\""), "Should have adjustment-inputs");
    }
  }
}
