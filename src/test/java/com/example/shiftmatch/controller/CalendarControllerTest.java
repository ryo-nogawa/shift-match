package com.example.shiftmatch.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.shiftmatch.service.HolidayService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CalendarController.class)
class CalendarControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private HolidayService holidayService;

  @Test
  @DisplayName("[F-9] 営業日と祝日の一覧が JSON で返る")
  void testGetCalendarReturnsBusinessDaysAndHolidays() throws Exception {
    // Arrange
    YearMonth month = YearMonth.of(2026, 10);
    List<LocalDate> businessDays =
        List.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 12));
    Map<LocalDate, String> holidays = new HashMap<>();
    holidays.put(LocalDate.of(2026, 10, 12), "スポーツの日");

    when(holidayService.isSupported(month)).thenReturn(true);
    when(holidayService.businessDays(month)).thenReturn(businessDays);
    when(holidayService.holidaysOf(month)).thenReturn(holidays);

    // Act & Assert
    mockMvc
        .perform(get("/calendar").param("month", "2026-10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.month", equalTo("2026-10")))
        .andExpect(jsonPath("$.businessDayCount", equalTo(3)))
        .andExpect(jsonPath("$.holidayCount", equalTo(1)))
        .andExpect(jsonPath("$.businessDays[0]", equalTo("2026-10-01")))
        .andExpect(jsonPath("$.businessDays[1]", equalTo("2026-10-02")))
        .andExpect(jsonPath("$.businessDays[2]", equalTo("2026-10-12")))
        .andExpect(jsonPath("$.holidays[0].date", equalTo("2026-10-12")))
        .andExpect(jsonPath("$.holidays[0].name", equalTo("スポーツの日")));
  }

  @Test
  @DisplayName("[F-9] month が不正形式のとき 400 でメッセージを返す")
  void testGetCalendarWithInvalidMonthFormat() throws Exception {
    // Act & Assert
    mockMvc
        .perform(get("/calendar").param("month", "invalid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", equalTo("対象月を判定できません。祝日データにない月です。")));
  }

  @Test
  @DisplayName("[F-9] isSupported が false のとき 400 でメッセージを返す")
  void testGetCalendarWhenMonthNotSupported() throws Exception {
    // Arrange
    YearMonth month = YearMonth.of(2025, 1);
    when(holidayService.isSupported(month)).thenReturn(false);

    // Act & Assert
    mockMvc
        .perform(get("/calendar").param("month", "2025-01"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", equalTo("対象月を判定できません。祝日データにない月です。")));
  }
}
