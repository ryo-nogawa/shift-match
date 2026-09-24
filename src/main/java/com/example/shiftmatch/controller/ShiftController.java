package com.example.shiftmatch.controller;

import com.example.shiftmatch.domain.DuplicateNameError;
import com.example.shiftmatch.domain.Employee;
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

  private static final int MAX_EMPLOYEE_COUNT = 12;

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
    // 入力行数が上限を超える場合は、DoS 攻撃への耐性を保つため処理を中断する
    int validEmployeeCount = 0;
    for (EmployeeForm employee : shiftForm.getEmployees()) {
      if (employee.getName() != null && !employee.getName().isBlank()) {
        validEmployeeCount++;
      }
    }
    if (validEmployeeCount > MAX_EMPLOYEE_COUNT) {
      model.addAttribute(
          "limitExceededError", "従業員の入力行数が上限（" + MAX_EMPLOYEE_COUNT + "名）を超えています。入力行を減らしてください。");
      model.addAttribute("shiftForm", shiftForm);
      return "index";
    }

    // 仕様上、入力表には最低 1 行を残す必要があるため、行が 1 件も送られなかった場合だけ空行を補う
    if (shiftForm.getEmployees().isEmpty()) {
      shiftForm.getEmployees().add(new EmployeeForm());
    }

    List<Employee> employees = convertToEmployees(shiftForm);

    List<DuplicateNameError> duplicateErrors = shiftAssignmentService.findDuplicateNames(employees);

    // V-1 では氏名が空の行をエラーにせず処理対象から除く必要があるため、V-3 のチェック時も空行は対象外とする
    List<InvalidWishError> wishErrors = new ArrayList<>();
    for (int i = 0; i < shiftForm.getEmployees().size(); i++) {
      EmployeeForm employee = shiftForm.getEmployees().get(i);
      if (employee.getName() == null || employee.getName().isBlank()) {
        continue;
      }

      // 6 つの枠ごとの希望をチェック
      List<String> wishes = employee.getWishes();
      String[] workTimes = {
        "07:30〜14:30", "08:00〜15:30", "08:30〜16:30", "09:00〜16:30", "09:00〜18:00", "09:00〜18:30"
      };
      for (int j = 0; j < 6; j++) {
        String wish = (wishes != null && j < wishes.size()) ? wishes.get(j) : null;
        if (!isValidWish(wish)) {
          // エラーメッセージには枠の勤務時間を表示
          wishErrors.add(new InvalidWishError(i, workTimes[j]));
        }
      }
    }

    // V-2・V-3 いずれのエラーもない場合のみ assign を呼び出し、割当案を算出する
    if (wishErrors.isEmpty() && duplicateErrors.isEmpty()) {
      var result = shiftAssignmentService.assign(employees);
      if (result.isPresent()) {
        model.addAttribute("assignmentResult", result.get());
      } else {
        model.addAttribute("unassignable", true);
      }
    }

    model.addAttribute("wishErrors", wishErrors);
    model.addAttribute("duplicateErrors", duplicateErrors);
    model.addAttribute("shiftForm", shiftForm);

    return "index";
  }

  /**
   * ShiftForm を Employee のリストに変換します。
   *
   * @param shiftForm フォームデータ
   * @return Employee のリスト
   */
  private List<Employee> convertToEmployees(ShiftForm shiftForm) {
    List<Employee> employees = new ArrayList<>();
    for (EmployeeForm form : shiftForm.getEmployees()) {
      List<Wish> wishes = new ArrayList<>();
      List<String> wishStrings = form.getWishes();
      for (int i = 0; i < 6; i++) {
        String wish = (wishStrings != null && i < wishStrings.size()) ? wishStrings.get(i) : null;
        wishes.add(convertStringToWish(wish));
      }
      employees.add(new Employee(form.getName(), wishes));
    }
    return employees;
  }

  /**
   * 文字列を Wish enum に変換します。変換できない場合は UNAVAILABLE を返します。
   *
   * @param wish 希望値（文字列）
   * @return Wish enum
   */
  private Wish convertStringToWish(String wish) {
    if (wish == null || wish.isEmpty()) {
      return Wish.UNAVAILABLE;
    }
    for (Wish w : Wish.values()) {
      if (w.name().equals(wish)) {
        return w;
      }
    }
    return Wish.UNAVAILABLE;
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
