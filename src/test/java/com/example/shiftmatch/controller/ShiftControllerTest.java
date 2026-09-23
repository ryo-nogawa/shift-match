package com.example.shiftmatch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.example.shiftmatch.service.ShiftAssignmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(ShiftController.class)
class ShiftControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ShiftAssignmentService shiftAssignmentService;

  @Nested
  class GetIndexTest {
    @Test
    @DisplayName("[F-1] GET / で初期フォームを表示する")
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
  }
}
