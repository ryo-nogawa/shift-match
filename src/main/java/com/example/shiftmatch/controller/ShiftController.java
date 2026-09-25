package com.example.shiftmatch.controller;

import com.example.shiftmatch.domain.DuplicateNameError;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.InvalidWishError;
import com.example.shiftmatch.domain.ShiftSlot;
import com.example.shiftmatch.domain.Wish;
import com.example.shiftmatch.service.ShiftAssignmentService;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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

    List<String> slotLabels = new ArrayList<>();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
    for (ShiftSlot slot : ShiftSlot.values()) {
      LocalTime start = slot.startTime();
      LocalTime end = slot.endTime();
      slotLabels.add(start.format(formatter) + "〜" + end.format(formatter));
    }
    model.addAttribute("slotLabels", slotLabels);

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
    // 仕様上、入力表には最低 1 行を残す必要があるため、行が 1 件も送られなかった場合だけ空行を補う
    if (shiftForm.getEmployees().isEmpty()) {
      shiftForm.getEmployees().add(new EmployeeForm());
    }

    List<Employee> employees = convertToEmployees(shiftForm);

    // V-1: 氏名が空の行を処理対象から除外（エラーにしない）
    List<Employee> validEmployees =
        employees.stream().filter(emp -> emp.name() != null && !emp.name().isBlank()).toList();

    List<String> slotLabelsForError = new ArrayList<>();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
    for (ShiftSlot slot : ShiftSlot.values()) {
      LocalTime start = slot.startTime();
      LocalTime end = slot.endTime();
      slotLabelsForError.add(start.format(formatter) + "〜" + end.format(formatter));
    }

    // V-2: 重複チェック
    List<DuplicateNameError> duplicateErrors =
        shiftAssignmentService.findDuplicateNames(validEmployees);

    // V-3: 希望の有効性チェック
    List<InvalidWishError> wishErrors = new ArrayList<>();

    for (int i = 0; i < shiftForm.getEmployees().size(); i++) {
      EmployeeForm employee = shiftForm.getEmployees().get(i);
      if (employee.getName() == null || employee.getName().isBlank()) {
        continue;
      }

      List<String> wishes = employee.getWishes();
      for (int j = 0; j < slotLabelsForError.size(); j++) {
        String wish = (wishes != null && j < wishes.size()) ? wishes.get(j) : null;
        if (!isValidWish(wish)) {
          wishErrors.add(new InvalidWishError(i, slotLabelsForError.get(j)));
        }
      }
    }

    // V-5: 有効な従業員が13名以上のチェック
    boolean limitExceeded = validEmployees.size() > MAX_EMPLOYEE_COUNT;
    if (limitExceeded) {
      model.addAttribute(
          "limitExceededError", "従業員の入力行数が上限（" + MAX_EMPLOYEE_COUNT + "名）を超えています。入力行を減らしてください。");
    }

    if (!duplicateErrors.isEmpty() || !wishErrors.isEmpty() || limitExceeded) {
      model.addAttribute("wishErrors", wishErrors);
      model.addAttribute("duplicateErrors", duplicateErrors);
      model.addAttribute("shiftForm", shiftForm);
      return "index";
    }

    var result = shiftAssignmentService.assign(validEmployees);
    if (result.isPresent()) {
      model.addAttribute("assignmentResult", result.get());
    } else {
      model.addAttribute("unassignable", true);
    }

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
