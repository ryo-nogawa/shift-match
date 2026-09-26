package com.example.shiftmatch.controller;

import com.example.shiftmatch.domain.DuplicateNameError;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.InvalidTimeRangeError;
import com.example.shiftmatch.service.ShiftAssignmentService;
import jakarta.validation.Valid;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * シフト作成画面のコントローラーです。
 */
@Controller
public class ShiftController {

  private static final int MAX_EMPLOYEE_COUNT = 12;

  private static final String START_PROPERTY = "start";

  private static final Pattern TIME_RANGE_FIELD_PATTERN =
      Pattern.compile("employees\\[(\\d+)\\]\\.(start|end)");

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

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
    return TimeOptions.VALUES;
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
    for (int i = 0; i < MAX_EMPLOYEE_COUNT; i++) {
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
   * @param bindingResult バリデーション結果
   * @param model モデルオブジェクト
   * @return ビュー名
   */
  @PostMapping("/shift")
  public String createShift(
      @Valid @ModelAttribute("shiftForm") ShiftForm shiftForm,
      BindingResult bindingResult,
      Model model) {
    // 仕様上、入力表には最低 1 行を残す必要があるため、行が 1 件も送られなかった場合だけ空行を補う
    if (shiftForm.getEmployees().isEmpty()) {
      shiftForm.getEmployees().add(new EmployeeForm());
    }

    List<Employee> employees = convertToEmployees(shiftForm);

    List<Employee> validEmployees =
        employees.stream().filter(emp -> emp.name() != null && !emp.name().isBlank()).toList();

    // 空行を含む元のリストを渡す。サービス側が空行を除外しつつ元のインデックスを保持する
    List<DuplicateNameError> duplicateErrors = shiftAssignmentService.findDuplicateNames(employees);

    List<InvalidTimeRangeError> timeRangeErrors = toTimeRangeErrors(bindingResult);

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
   * BindingResult から開始・終了のエラーを InvalidTimeRangeError のリストに変換します。
   *
   * <p>行番号の昇順、同じ行では start → end の順に並べられます。
   *
   * @param bindingResult バリデーション結果
   * @return 開始・終了のエラーリスト（行順、プロパティ順）
   */
  private List<InvalidTimeRangeError> toTimeRangeErrors(BindingResult bindingResult) {
    List<TimeRangeFieldError> fieldErrors = new ArrayList<>();

    // employees[N].start / employees[N].end のフィールドエラーだけを集める
    for (FieldError error : bindingResult.getFieldErrors()) {
      Matcher matcher = TIME_RANGE_FIELD_PATTERN.matcher(error.getField());
      if (matcher.matches()) {
        fieldErrors.add(
            new TimeRangeFieldError(
                Integer.parseInt(matcher.group(1)), matcher.group(2), error.getDefaultMessage()));
      }
    }

    // 違反の集合には順序の保証がないため、行番号の昇順、同じ行では start → end の順に並べる
    fieldErrors.sort(
        Comparator.comparingInt((TimeRangeFieldError fieldError) -> fieldError.rowIndex())
            .thenComparing(fieldError -> !START_PROPERTY.equals(fieldError.property())));

    return fieldErrors.stream()
        .map(fieldError -> new InvalidTimeRangeError(fieldError.rowIndex(), fieldError.message()))
        .toList();
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

  /**
   * 開始・終了のフィールドエラーを、並べ替えのために行番号・項目名とあわせて保持するレコードです。
   *
   * @param rowIndex 行番号（0 始まり）
   * @param property 項目名（{@code start} または {@code end}）
   * @param message エラーメッセージ
   */
  private record TimeRangeFieldError(int rowIndex, String property, String message) {}
}
