package com.example.shiftmatch.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.shiftmatch.controller.ShiftForm;
import com.example.shiftmatch.domain.Holiday;
import com.example.shiftmatch.persistence.HolidayRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** 保存から復元までの HTTP 結合テスト。実物の Controller・Service・Repository・H2 を通します。 */
@SpringBootTest(properties = "holiday.refresh-on-startup=false")
@AutoConfigureMockMvc
@DisplayName("保存と復元（HTTP）")
class PersistenceHttpTest {

  private static final List<String> SAVED_TABLES =
      List.of(
          "saved_day_unassigned",
          "saved_day_assignment",
          "saved_day",
          "saved_month_employee",
          "saved_adjustment",
          "saved_input_meta",
          "saved_input_base_shift",
          "saved_input_employee");

  private static final String SAVED_MESSAGE = "保存済みのシフトを表示しています";

  private static final String NOT_CREATED_MESSAGE = "この月のシフトはまだ作成されていません";

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcClient jdbcClient;
  @Autowired private HolidayRepository holidayRepository;

  @BeforeEach
  void prepareData() {
    SAVED_TABLES.forEach(table -> jdbcClient.sql("DELETE FROM " + table).update());
    holidayRepository.replaceAll(
        List.of(
            new Holiday(LocalDate.of(2026, 1, 1), "元日"),
            new Holiday(LocalDate.of(2026, 10, 12), "スポーツの日"),
            new Holiday(LocalDate.of(2027, 1, 1), "元日")));
  }

  /** 従業員 9 名（名前は prefix0〜prefix8）が全曜日 07:30〜18:30 で働ける入力を送る POST を作ります。 */
  private static MockHttpServletRequestBuilder createRequest(String month, String prefix) {
    MockHttpServletRequestBuilder request =
        post("/shift").header("Sec-Fetch-Site", "same-origin").param("targetMonth", month);
    for (int i = 0; i < 9; i++) {
      request
          .param("employees[" + i + "].name", prefix + i)
          .param("employees[" + i + "].employmentType", "PART_TIME");
      for (int day = 0; day < 5; day++) {
        request
            .param("employees[" + i + "].days[" + day + "].start", "07:30")
            .param("employees[" + i + "].days[" + day + "].end", "18:30");
      }
    }
    return request;
  }

  private void create(String month, String prefix) throws Exception {
    mockMvc.perform(createRequest(month, prefix)).andExpect(status().isOk());
  }

  private String savedHtml(String month) throws Exception {
    return mockMvc
        .perform(get("/shift/saved").param("month", month))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  @Nested
  class 正常系 {

    @Test
    @DisplayName(
        "[F-7] Given: 従業員と個別変更を送って作成した, When: GET / を開くと," + " Then: 従業員と個別変更が復元され、対象月は最後の月になる")
    void restoresInputAndLastMonthOnGet() throws Exception {
      mockMvc
          .perform(
              createRequest("2026-10", "E")
                  .param("adjustments[0].date", "2026-10-20")
                  .param("adjustments[0].employeeName", "E0")
                  .param("adjustments[0].off", "true"))
          .andExpect(status().isOk());

      MvcResult result = mockMvc.perform(get("/")).andExpect(status().isOk()).andReturn();

      ShiftForm form = (ShiftForm) result.getModelAndView().getModel().get("shiftForm");
      assertEquals("2026-10", form.getTargetMonth());
      assertEquals(12, form.getEmployees().size());
      assertEquals("E0", form.getEmployees().get(0).getName());
      assertEquals("PART_TIME", form.getEmployees().get(0).getEmploymentType());
      assertEquals("E8", form.getEmployees().get(8).getName());
      assertEquals("", form.getEmployees().get(9).getName());
      assertEquals(1, form.getAdjustments().size());
      assertEquals("2026-10-20", form.getAdjustments().get(0).getDate());
      assertEquals("E0", form.getAdjustments().get(0).getEmployeeName());
      assertTrue(form.getAdjustments().get(0).isOff());
      String html = result.getResponse().getContentAsString();
      assertTrue(html.contains(SAVED_MESSAGE));
    }

    @Test
    @DisplayName(
        "[F-7] Given: 10 月を作成したあと 11 月を作成した, When: GET /shift/saved で 10 月を取得すると,"
            + " Then: 10 月のシフトが保存済みとして見られる")
    void keepsPreviousMonthOfSameYear() throws Exception {
      create("2026-10", "E");
      create("2026-11", "F");

      String october = savedHtml("2026-10");

      assertTrue(october.contains(SAVED_MESSAGE));
      assertTrue(october.contains("id=\"result-summary\""));
      assertTrue(october.contains("E0"));
      assertTrue(savedHtml("2026-11").contains("F0"));
    }

    @Test
    @DisplayName("[F-7] Given: 10 月を作成済み, When: 同じ月を別の従業員で作り直すと, Then: 保存済みのシフトが置き換わる")
    void replacesSameMonth() throws Exception {
      create("2026-10", "E");
      create("2026-10", "F");

      String html = savedHtml("2026-10");

      assertTrue(html.contains("F0"));
      assertFalse(html.contains("E0"));
    }

    @Test
    @DisplayName(
        "[F-7] Given: 2026 年 10 月を作成済み, When: 2027 年 1 月を作成すると,"
            + " Then: 前の年のシフトは「まだ作成されていません」になる")
    void removesPreviousYear() throws Exception {
      create("2026-10", "E");
      create("2027-01", "F");

      String previousYear = savedHtml("2026-10");

      assertTrue(previousYear.contains(NOT_CREATED_MESSAGE));
      assertFalse(previousYear.contains("id=\"result-summary\""));
      assertTrue(savedHtml("2027-01").contains(SAVED_MESSAGE));
    }
  }

  @Nested
  class 異常系 {

    @Test
    @DisplayName(
        "[F-7] Given: 入力エラーになる POST, When: GET / を開くと," + " Then: 何も保存されず、空の 12 行のまま（保存済みシフトもない）")
    void savesNothingOnInputError() throws Exception {
      MockHttpServletRequestBuilder invalid =
          post("/shift")
              .header("Sec-Fetch-Site", "same-origin")
              .param("targetMonth", "2026-10")
              .param("employees[0].name", "E0")
              .param("employees[0].employmentType", "INVALID");
      mockMvc.perform(invalid).andExpect(status().isOk());

      MvcResult result = mockMvc.perform(get("/")).andExpect(status().isOk()).andReturn();

      ShiftForm form = (ShiftForm) result.getModelAndView().getModel().get("shiftForm");
      assertEquals(12, form.getEmployees().size());
      form.getEmployees().forEach(employee -> assertEquals("", employee.getName()));
      assertTrue(form.getAdjustments().isEmpty());
      assertTrue(result.getResponse().getContentAsString().contains(NOT_CREATED_MESSAGE));
    }
  }
}
