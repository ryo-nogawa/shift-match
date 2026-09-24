package com.example.shiftmatch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.shiftmatch.domain.DuplicateNameError;
import com.example.shiftmatch.service.ShiftAssignmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@WebMvcTest(ShiftController.class)
class ShiftControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ShiftAssignmentService shiftAssignmentService;

  @Nested
  class GetIndexTest {
    @Test
    @DisplayName("[F-1] Given: 初期状態のとき, When: GET / を実行すると, Then: 4行の空フォームが表示されること")
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

  @Nested
  class PostShiftTest {
    @Test
    @DisplayName(
        "[V-3] Given: 氏名が入力されていて早番希望が未選択のとき, When: POST /shift を実行すると, "
            + "Then: レスポンス本文に不正エラーを示す文言が含まれる")
    void shouldShowErrorWhenEarlyWishIsEmpty() throws Exception {
      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("employees[0].name", "太郎");
      params.add("employees[0].earlyWish", "");
      params.add("employees[0].lateWish", "AVAILABLE");

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();
      assertTrue(body.contains("不正") || body.contains("エラー"));
    }

    @Test
    @DisplayName(
        "[V-3] Given: 氏名が入力されていて遅番希望が不正値のとき, When: POST /shift を実行すると, "
            + "Then: レスポンス本文に不正エラーを示す文言が含まれる")
    void shouldShowErrorWhenLateWishIsInvalid() throws Exception {
      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("employees[0].name", "太郎");
      params.add("employees[0].earlyWish", "DESIRED");
      params.add("employees[0].lateWish", "INVALID_VALUE");

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();
      assertTrue(body.contains("不正") || body.contains("エラー"));
    }

    @Test
    @DisplayName(
        "[V-3] Given: 氏名が空の行の早番・遅番希望が未選択のとき, When: POST /shift を実行すると, " + "Then: エラーにならない")
    void shouldNotShowErrorForEmptyNameRow() throws Exception {
      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("employees[0].name", "");
      params.add("employees[0].earlyWish", "");
      params.add("employees[0].lateWish", "");

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();
      // 不正エラーが表示されてはいけない
      assertEquals(200, result.getResponse().getStatus());
      assertTrue(!body.contains("入力エラー"));
      java.util.List<?> wishErrors =
          (java.util.List<?>) result.getModelAndView().getModel().get("wishErrors");
      assertTrue(wishErrors != null && wishErrors.isEmpty());
    }

    @Test
    @DisplayName(
        "[V-3] Given: 氏名が空白のみの行の早番・遅番希望が未選択のとき, When: POST /shift を実行すると, " + "Then: エラーにならない")
    void shouldNotShowErrorForBlankNameRow() throws Exception {
      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("employees[0].name", "   ");
      params.add("employees[0].earlyWish", "");
      params.add("employees[0].lateWish", "");

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();
      // 不正エラーが表示されてはいけない
      assertEquals(200, result.getResponse().getStatus());
      assertTrue(!body.contains("入力エラー"));
    }

    @Test
    @DisplayName(
        "[V-2] Given: 氏名が重複しているとき, When: POST /shift を実行すると, "
            + "Then: レスポンス本文に重複エラーを示す文言が含まれ、assign が呼び出されないこと")
    void shouldShowErrorAndNotCallAssignWhenNamesAreDuplicated() throws Exception {
      org.mockito.Mockito.when(
              shiftAssignmentService.findDuplicateNames(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.List.of(new DuplicateNameError("太郎", java.util.List.of(0, 1))));

      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("employees[0].name", "太郎");
      params.add("employees[0].earlyWish", "DESIRED");
      params.add("employees[0].lateWish", "AVAILABLE");
      params.add("employees[1].name", "太郎");
      params.add("employees[1].earlyWish", "AVAILABLE");
      params.add("employees[1].lateWish", "DESIRED");

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();
      assertTrue(body.contains("重複") || body.contains("エラー"));
      assertTrue(body.contains("1") && body.contains("2") && body.contains("行目"));
      verify(shiftAssignmentService, never()).assign(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName(
        "[F-3] Given: V-2・V-3 いずれのエラーもないとき, When: POST /shift を実行すると, "
            + "Then: ShiftAssignmentService#assign が 1 回呼び出されること")
    void shouldCallAssignWhenNoErrorsExist() throws Exception {
      org.mockito.Mockito.when(
              shiftAssignmentService.findDuplicateNames(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.List.of());
      org.mockito.Mockito.when(shiftAssignmentService.assign(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.Optional.empty());

      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("employees[0].name", "太郎");
      params.add("employees[0].earlyWish", "DESIRED");
      params.add("employees[0].lateWish", "AVAILABLE");

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      assertEquals(200, result.getResponse().getStatus());
      verify(shiftAssignmentService).assign(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("[F-4] Given: 有効な割当が存在するとき, When: POST /shift を実行すると, Then: 結果が表形式で表示されること")
    void shouldDisplayResultInTableFormatWhenAssignmentSucceeds() throws Exception {
      com.example.shiftmatch.domain.AssignmentResult assignmentResult =
          new com.example.shiftmatch.domain.AssignmentResult(
              java.util.List.of(
                  new com.example.shiftmatch.domain.Employee("太郎", null, null),
                  new com.example.shiftmatch.domain.Employee("花子", null, null)),
              java.util.List.of(
                  new com.example.shiftmatch.domain.Employee("次郎", null, null),
                  new com.example.shiftmatch.domain.Employee("美咲", null, null)),
              3,
              java.util.List.of(new com.example.shiftmatch.domain.Employee("五郎", null, null)));

      org.mockito.Mockito.when(
              shiftAssignmentService.findDuplicateNames(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.List.of());
      org.mockito.Mockito.when(shiftAssignmentService.assign(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.Optional.of(assignmentResult));

      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("employees[0].name", "太郎");
      params.add("employees[0].earlyWish", "DESIRED");
      params.add("employees[0].lateWish", "AVAILABLE");

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();
      assertTrue(body.contains("太郎"));
      assertTrue(body.contains("花子"));
      assertTrue(body.contains("次郎"));
      assertTrue(body.contains("美咲"));
      assertTrue(body.contains("五郎"));
      assertTrue(body.contains("3"));
    }

    @Test
    @DisplayName(
        "[T-1][F-4] Given: 早番2名・遅番2名の有効な割当が存在するとき, When: POST /shift を実行すると, "
            + "Then: レスポンス本文に4つの休憩時刻（13:00~14:00、14:00~15:00、15:00~16:00、16:00~17:00）が含まれること")
    void shouldDisplayBreakTimesInResultWhenAssignmentSucceeds() throws Exception {
      com.example.shiftmatch.domain.AssignmentResult assignmentResult =
          new com.example.shiftmatch.domain.AssignmentResult(
              java.util.List.of(
                  new com.example.shiftmatch.domain.Employee(
                      "太郎",
                      com.example.shiftmatch.domain.Wish.DESIRED,
                      com.example.shiftmatch.domain.Wish.AVAILABLE),
                  new com.example.shiftmatch.domain.Employee(
                      "花子",
                      com.example.shiftmatch.domain.Wish.AVAILABLE,
                      com.example.shiftmatch.domain.Wish.DESIRED)),
              java.util.List.of(
                  new com.example.shiftmatch.domain.Employee(
                      "次郎",
                      com.example.shiftmatch.domain.Wish.DESIRED,
                      com.example.shiftmatch.domain.Wish.AVAILABLE),
                  new com.example.shiftmatch.domain.Employee(
                      "美咲",
                      com.example.shiftmatch.domain.Wish.AVAILABLE,
                      com.example.shiftmatch.domain.Wish.DESIRED)),
              4,
              java.util.List.of());

      org.mockito.Mockito.when(
              shiftAssignmentService.findDuplicateNames(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.List.of());
      org.mockito.Mockito.when(shiftAssignmentService.assign(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.Optional.of(assignmentResult));

      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("employees[0].name", "太郎");
      params.add("employees[0].earlyWish", "DESIRED");
      params.add("employees[0].lateWish", "AVAILABLE");
      params.add("employees[1].name", "花子");
      params.add("employees[1].earlyWish", "AVAILABLE");
      params.add("employees[1].lateWish", "DESIRED");
      params.add("employees[2].name", "次郎");
      params.add("employees[2].earlyWish", "DESIRED");
      params.add("employees[2].lateWish", "AVAILABLE");
      params.add("employees[3].name", "美咲");
      params.add("employees[3].earlyWish", "AVAILABLE");
      params.add("employees[3].lateWish", "DESIRED");

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();
      assertTrue(body.contains("13:00") && body.contains("14:00"));
      assertTrue(body.contains("14:00") && body.contains("15:00"));
      assertTrue(body.contains("15:00") && body.contains("16:00"));
      assertTrue(body.contains("16:00") && body.contains("17:00"));
    }

    @Test
    @DisplayName(
        "[セキュリティー] Given: 有効な氏名を持つ行が上限（20名）を超えるとき, When: POST /shift を実行すると, "
            + "Then: assign が呼び出されず、上限超過のエラーメッセージが表示されること")
    void shouldRejectWhenEmployeeCountExceedsLimit() throws Exception {
      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      for (int i = 0; i < 21; i++) {
        params.add("employees[" + i + "].name", "従業員" + i);
        params.add("employees[" + i + "].earlyWish", "DESIRED");
        params.add("employees[" + i + "].lateWish", "AVAILABLE");
      }

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();
      assertTrue(body.contains("上限"));
      verify(shiftAssignmentService, never()).assign(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("[F-5] Given: 条件を満たす組み合わせがないとき, When: POST /shift を実行すると, Then: 不成立メッセージが表示されること")
    void shouldDisplayUnassignableMessageWhenNoValidCombinationExists() throws Exception {
      org.mockito.Mockito.when(
              shiftAssignmentService.findDuplicateNames(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.List.of());
      org.mockito.Mockito.when(shiftAssignmentService.assign(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.Optional.empty());

      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("employees[0].name", "太郎");
      params.add("employees[0].earlyWish", "DESIRED");
      params.add("employees[0].lateWish", "AVAILABLE");

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      String body = result.getResponse().getContentAsString();
      assertTrue(body.contains("条件を満たす組み合わせが見つかりませんでした。"));
    }

    @Test
    @DisplayName(
        "[V-4] Given: 従業員パラメータが一切送られないとき, When: POST /shift を実行すると, "
            + "Then: 例外を投げずにステータス200で不成立と扱われること")
    void shouldHandleEmptyEmployeeListWithoutException() throws Exception {
      org.mockito.Mockito.when(
              shiftAssignmentService.findDuplicateNames(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.List.of());
      org.mockito.Mockito.when(shiftAssignmentService.assign(org.mockito.ArgumentMatchers.any()))
          .thenReturn(java.util.Optional.empty());

      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

      MvcResult result =
          mockMvc.perform(MockMvcRequestBuilders.post("/shift").params(params)).andReturn();

      assertEquals(200, result.getResponse().getStatus());
      String body = result.getResponse().getContentAsString();
      assertTrue(body.contains("条件を満たす組み合わせが見つかりませんでした。"));
    }
  }
}
