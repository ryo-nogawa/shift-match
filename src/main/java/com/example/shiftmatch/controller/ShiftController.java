package com.example.shiftmatch.controller;

import com.example.shiftmatch.domain.InvalidWishError;
import com.example.shiftmatch.domain.Wish;
import com.example.shiftmatch.service.ShiftAssignmentService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * シフト作成画面のコントローラーです。
 */
@Controller
public class ShiftController {

  private final ShiftAssignmentService shiftAssignmentService;

  /**
   * コンストラクタです。
   *
   * @param shiftAssignmentService シフト算出サービス
   */
  @Autowired
  public ShiftController(ShiftAssignmentService shiftAssignmentService) {
    this.shiftAssignmentService = shiftAssignmentService;
  }

  /**
   * 初期フォームを表示します。
   *
   * @param model モデルオブジェクト
   * @return ビュー名
   */
  @GetMapping("/")
  public String index(Model model) {
    ShiftForm shiftForm = new ShiftForm();
    List<EmployeeForm> employees = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      employees.add(new EmployeeForm());
    }
    shiftForm.setEmployees(employees);
    model.addAttribute("shiftForm", shiftForm);
    return "index";
  }

  /**
   * シフトを作成します。
   *
   * @param shiftForm フォームデータ
   * @param model モデルオブジェクト
   * @return ビュー名
   */
  @PostMapping("/shift")
  public String createShift(@ModelAttribute("shiftForm") ShiftForm shiftForm, Model model) {
    // V-3: 希望値の不正チェック
    List<InvalidWishError> wishErrors = new ArrayList<>();
    for (int i = 0; i < shiftForm.getEmployees().size(); i++) {
      EmployeeForm employee = shiftForm.getEmployees().get(i);
      // 氏名が空の行は V-3 対象外
      if (employee.getName() == null || employee.getName().isEmpty()) {
        continue;
      }

      // 早番希望のチェック
      if (!isValidWish(employee.getEarlyWish())) {
        wishErrors.add(new InvalidWishError(i, "早番希望"));
      }

      // 遅番希望のチェック
      if (!isValidWish(employee.getLateWish())) {
        wishErrors.add(new InvalidWishError(i, "遅番希望"));
      }
    }

    model.addAttribute("wishErrors", wishErrors);
    model.addAttribute("shiftForm", shiftForm);

    return "index";
  }

  /**
   * 希望値が有効かどうかを判定します。
   *
   * @param wish 希望値
   * @return 有効な場合 true、無効な場合 false
   */
  private boolean isValidWish(String wish) {
    if (wish == null || wish.isEmpty()) {
      return false;
    }
    return Arrays.stream(Wish.values()).anyMatch(w -> w.name().equals(wish));
  }
}
