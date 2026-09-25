package com.example.shiftmatch.controller;

import com.example.shiftmatch.domain.DuplicateNameError;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.InvalidTimeRangeError;
import com.example.shiftmatch.service.ShiftAssignmentService;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  private static final LocalTime FIRST_TIME_OPTION = LocalTime.of(7, 30);

  private static final LocalTime LAST_TIME_OPTION = LocalTime.of(18, 30);

  private static final int TIME_OPTION_STEP_MINUTES = 30;

  /** 開始・終了の選択肢（07:30〜18:30 の 30 分刻み、HH:mm）。 */
  private static final List<String> TIME_OPTIONS = createTimeOptions();

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
   * 開始・終了の選択肢（07:30〜18:30 の 30 分刻み、HH:mm）をモデルに設定します。
   *
   * <p>GET と POST の全経路でモデルに含まれるよう、{@code @ModelAttribute} を使用します。
   *
   * @return 時刻の選択肢のリスト
   */
  @ModelAttribute("timeOptions")
  public List<String> timeOptions() {
    return TIME_OPTIONS;
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
    // 仕様上、入力表には最低 1 行を残す必要があるため、行が 1 件も送られなかった場合だけ空行を補う
    if (shiftForm.getEmployees().isEmpty()) {
      shiftForm.getEmployees().add(new EmployeeForm());
    }

    List<Employee> employees = convertToEmployees(shiftForm);

    List<Employee> validEmployees =
        employees.stream().filter(emp -> emp.name() != null && !emp.name().isBlank()).toList();

    // 空行を含む元のリストを渡す。サービス側が空行を除外しつつ元のインデックスを保持する
    List<DuplicateNameError> duplicateErrors = shiftAssignmentService.findDuplicateNames(employees);

    List<InvalidTimeRangeError> timeRangeErrors = validateTimeRanges(shiftForm);

    boolean limitExceeded = validEmployees.size() > MAX_EMPLOYEE_COUNT;
    if (limitExceeded) {
      model.addAttribute(
          "limitExceededError", "従業員の入力行数が上限（" + MAX_EMPLOYEE_COUNT + "名）を超えています。入力行を減らしてください。");
    }

    if (!duplicateErrors.isEmpty() || !timeRangeErrors.isEmpty() || limitExceeded) {
      model.addAttribute("duplicateErrors", duplicateErrors);
      model.addAttribute("timeRangeErrors", timeRangeErrors);
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
   * 開始・終了の選択肢を作成します。
   *
   * @return 07:30 から 18:30 までの 30 分刻みの時刻（HH:mm）のリスト
   */
  private static List<String> createTimeOptions() {
    List<String> options = new ArrayList<>();
    for (LocalTime time = FIRST_TIME_OPTION;
        !time.isAfter(LAST_TIME_OPTION);
        time = time.plusMinutes(TIME_OPTION_STEP_MINUTES)) {
      options.add(time.format(TIME_FORMATTER));
    }
    return List.copyOf(options);
  }

  /**
   * 開始・終了の入力をチェックします（V-3）。
   *
   * <p>氏名が入力され、休みでない行だけを対象にします（V-1）。
   *
   * @param shiftForm フォームデータ
   * @return 入力エラーのリスト（行順）。エラーがなければ空
   */
  private List<InvalidTimeRangeError> validateTimeRanges(ShiftForm shiftForm) {
    List<InvalidTimeRangeError> errors = new ArrayList<>();
    List<EmployeeForm> forms = shiftForm.getEmployees();
    for (int i = 0; i < forms.size(); i++) {
      EmployeeForm form = forms.get(i);
      if (form.getName() == null || form.getName().isBlank() || form.isOff()) {
        continue;
      }
      String startError = validateTimeOption(form.getStart(), "開始");
      String endError = validateTimeOption(form.getEnd(), "終了");
      if (startError != null) {
        errors.add(new InvalidTimeRangeError(i, startError));
      }
      if (endError != null) {
        errors.add(new InvalidTimeRangeError(i, endError));
      }
      if (startError == null && endError == null && form.getStart().compareTo(form.getEnd()) >= 0) {
        errors.add(new InvalidTimeRangeError(i, "開始は終了より前にしてください"));
      }
    }
    return errors;
  }

  /**
   * 時刻が選択肢のいずれかであるかをチェックします。
   *
   * @param time 時刻（HH:mm）
   * @param label 項目名（「開始」または「終了」）
   * @return エラーメッセージ。問題がなければ null
   */
  private String validateTimeOption(String time, String label) {
    if (time == null || time.isEmpty()) {
      return label + "が未選択です";
    }
    if (!TIME_OPTIONS.contains(time)) {
      return label + "は選択肢にありません";
    }
    return null;
  }

  /**
   * ShiftForm を Employee のリストに変換します。
   *
   * <p>開始・終了は {@code HH:mm} として解析し、空・不正な文字列は {@code null} にします。休みの行は開始・終了を無視します。
   *
   * @param shiftForm フォームデータ
   * @return Employee のリスト（空行を含む、入力順）
   */
  private List<Employee> convertToEmployees(ShiftForm shiftForm) {
    List<Employee> employees = new ArrayList<>();
    for (EmployeeForm form : shiftForm.getEmployees()) {
      boolean off = form.isOff();
      LocalTime start = off ? null : parseTimeOrNull(form.getStart());
      LocalTime end = off ? null : parseTimeOrNull(form.getEnd());
      employees.add(new Employee(form.getName(), off, start, end));
    }
    return employees;
  }

  /**
   * 時刻文字列を LocalTime に変換します。変換できない場合は null を返します。
   *
   * @param time 時刻文字列（HH:mm 形式）
   * @return LocalTime、または変換できない場合は null
   */
  private LocalTime parseTimeOrNull(String time) {
    if (time == null || time.isEmpty()) {
      return null;
    }
    try {
      return LocalTime.parse(time, TIME_FORMATTER);
    } catch (DateTimeParseException e) {
      return null;
    }
  }
}
