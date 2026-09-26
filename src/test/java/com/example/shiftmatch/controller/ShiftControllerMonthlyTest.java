package com.example.shiftmatch.controller;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.shiftmatch.service.MonthlyShiftService;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(ShiftController.class)
@DisplayName("ShiftController - 月間シフト作成")
class ShiftControllerMonthlyTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private MonthlyShiftService monthlyShiftService;

  @MockitoBean private MonthlyFormConverter monthlyFormConverter;

  @Test
  @DisplayName("[F-1][F-2] GET / が 200 で 'index' ビューを返す")
  void testGetIndexReturnsOkWithIndexView() throws Exception {
    mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("index"));
  }

  @Test
  @DisplayName("[F-1] GET / が今月を targetMonth に設定して返す")
  void testGetIndexReturnsCurrentMonthAsTargetMonth() throws Exception {
    String expectedMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

    mockMvc
        .perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(
            model()
                .attribute(
                    "shiftForm",
                    org.hamcrest.Matchers.hasProperty("targetMonth", equalTo(expectedMonth))));
  }

  @Test
  @DisplayName("[F-2] GET / が 12 人の従業員フォームを返す")
  void testGetIndexReturns12Employees() throws Exception {
    MvcResult result = mockMvc.perform(get("/")).andExpect(status().isOk()).andReturn();

    ShiftForm shiftForm = (ShiftForm) result.getModelAndView().getModel().get("shiftForm");

    org.junit.jupiter.api.Assertions.assertNotNull(shiftForm);
    org.junit.jupiter.api.Assertions.assertEquals(12, shiftForm.getEmployees().size());
  }

  @Test
  @DisplayName("[F-2] 各従業員の区分が FULL_TIME である")
  void testGetIndexEmployeesHaveFullTimeType() throws Exception {
    MvcResult result = mockMvc.perform(get("/")).andExpect(status().isOk()).andReturn();

    ShiftForm shiftForm = (ShiftForm) result.getModelAndView().getModel().get("shiftForm");

    org.junit.jupiter.api.Assertions.assertNotNull(shiftForm);
    for (EmployeeForm employee : shiftForm.getEmployees()) {
      org.junit.jupiter.api.Assertions.assertEquals("FULL_TIME", employee.getEmploymentType());
    }
  }

  @Test
  @DisplayName("[F-2] 各従業員が月〜金 5 日分の基本シフトを持つ")
  void testGetIndexEmployeesHaveFiveDays() throws Exception {
    MvcResult result = mockMvc.perform(get("/")).andExpect(status().isOk()).andReturn();

    ShiftForm shiftForm = (ShiftForm) result.getModelAndView().getModel().get("shiftForm");

    org.junit.jupiter.api.Assertions.assertNotNull(shiftForm);
    for (EmployeeForm employee : shiftForm.getEmployees()) {
      org.junit.jupiter.api.Assertions.assertEquals(5, employee.getDays().size());
    }
  }

  @Test
  @DisplayName("[F-2] 各従業員の基本シフトが 07:30〜18:30 で休みなし")
  void testGetIndexEmployeesHaveDefaultShiftTimes() throws Exception {
    MvcResult result = mockMvc.perform(get("/")).andExpect(status().isOk()).andReturn();

    ShiftForm shiftForm = (ShiftForm) result.getModelAndView().getModel().get("shiftForm");

    org.junit.jupiter.api.Assertions.assertNotNull(shiftForm);
    for (EmployeeForm employee : shiftForm.getEmployees()) {
      for (DayForm day : employee.getDays()) {
        org.junit.jupiter.api.Assertions.assertFalse(day.isOff());
        org.junit.jupiter.api.Assertions.assertEquals("07:30", day.getStart());
        org.junit.jupiter.api.Assertions.assertEquals("18:30", day.getEnd());
      }
    }
  }

  @Test
  @DisplayName("[F-9] GET / が timeOptions（23 個）を返す")
  void testGetIndexReturnsTimeOptions() throws Exception {
    mockMvc
        .perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("timeOptions", hasSize(23)));
  }

  @Test
  @DisplayName("[F-1] GET / が employmentTypes（3 個）を返す")
  void testGetIndexReturnsEmploymentTypes() throws Exception {
    mockMvc
        .perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("employmentTypes", hasSize(3)));
  }

  @Test
  @DisplayName("[F-1] GET / が initialStep を 1 に設定して返す")
  void testGetIndexReturnsInitialStep1() throws Exception {
    mockMvc
        .perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(model().attribute("initialStep", equalTo(1)));
  }
}
