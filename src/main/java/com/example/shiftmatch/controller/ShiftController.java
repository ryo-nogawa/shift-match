package com.example.shiftmatch.controller;

import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.DuplicateNameError;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.InvalidEmploymentTypeError;
import com.example.shiftmatch.domain.InvalidNameError;
import com.example.shiftmatch.domain.InvalidTimeRangeError;
import com.example.shiftmatch.domain.ShiftAssignment;
import com.example.shiftmatch.domain.ShiftSlot;
import com.example.shiftmatch.persistence.LatestShiftRepository;
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
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
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

  private static final Logger LOGGER = LoggerFactory.getLogger(ShiftController.class);

  private static final int MAX_EMPLOYEE_COUNT = 12;

  private static final String START_PROPERTY = "start";

  private static final Pattern TIME_RANGE_FIELD_PATTERN =
      Pattern.compile("employees\\[(\\d+)\\]\\.(start|end)");

  private static final Pattern NAME_FIELD_PATTERN = Pattern.compile("employees\\[(\\d+)\\]\\.name");

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  private final ShiftAssignmentService shiftAssignmentService;

  private final LatestShiftRepository latestShiftRepository;

  /**
   * コンストラクタです。
   *
   * @param shiftAssignmentService シフト算出サービス
   * @param latestShiftRepository 最新シフト結果リポジトリ
   */
  @Autowired
  public ShiftController(
      ShiftAssignmentService shiftAssignmentService, LatestShiftRepository latestShiftRepository) {
    this.shiftAssignmentService = shiftAssignmentService;
    this.latestShiftRepository = latestShiftRepository;
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
   * 雇用区分の選択肢（常勤、パート、管理職）をモデルに設定します。
   *
   * <p>GET と POST の全経路でモデルに含まれるよう、{@code @ModelAttribute} を使用します。
   *
   * @return 雇用区分の選択肢のリスト
   */
  @ModelAttribute("employmentTypes")
  public List<EmploymentType> employmentTypes() {
    return List.of(EmploymentType.FULL_TIME, EmploymentType.PART_TIME, EmploymentType.MANAGER);
  }

  /**
   * 初期フォームを表示します。保存済みの従業員入力がある場合は復元します。
   *
   * @param model モデルオブジェクト
   * @return ビュー名
   */
  @GetMapping("/")
  public String index(Model model) {
    ShiftForm shiftForm = new ShiftForm();
    List<EmployeeForm> employees = new ArrayList<>();

    // 保存済みの従業員を読み出す
    List<Employee> savedEmployees = latestShiftRepository.findEmployees();

    // 保存済みの従業員をフォームに詰める
    for (Employee savedEmployee : savedEmployees) {
      EmployeeForm form = new EmployeeForm();
      form.setName(savedEmployee.name());
      form.setEmploymentType(savedEmployee.employmentType().name());
      form.setOff(savedEmployee.off());
      if (!savedEmployee.off()) {
        form.setStart(savedEmployee.start().format(TIME_FORMATTER));
        form.setEnd(savedEmployee.end().format(TIME_FORMATTER));
      } else {
        form.setStart("");
        form.setEnd("");
      }
      employees.add(form);
    }

    // 不足分を空行で補う（最大12行）
    while (employees.size() < MAX_EMPLOYEE_COUNT) {
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

    List<InvalidNameError> nameErrors = toNameErrors(bindingResult);

    List<InvalidEmploymentTypeError> employmentTypeErrors = toEmploymentTypeErrors(shiftForm);

    boolean limitExceeded = validEmployees.size() > MAX_EMPLOYEE_COUNT;
    if (limitExceeded) {
      model.addAttribute(
          "limitExceededError", "従業員の入力行数が上限（" + MAX_EMPLOYEE_COUNT + "名）を超えています。入力行を減らしてください。");
    }

    if (!duplicateErrors.isEmpty()
        || !timeRangeErrors.isEmpty()
        || !nameErrors.isEmpty()
        || !employmentTypeErrors.isEmpty()
        || limitExceeded) {
      model.addAttribute("duplicateErrors", duplicateErrors);
      model.addAttribute("timeRangeErrors", timeRangeErrors);
      model.addAttribute("nameErrors", nameErrors);
      model.addAttribute("employmentTypeErrors", employmentTypeErrors);
      model.addAttribute("shiftForm", shiftForm);
      return "index";
    }

    var result = shiftAssignmentService.assign(validEmployees);

    // 入力エラーがなく算出まで完了したときに保存
    try {
      latestShiftRepository.save(validEmployees, result);
    } catch (DataAccessException e) {
      LOGGER.error("最新シフトの保存に失敗しました", e);
      model.addAttribute("saveError", "保存に失敗しました。もう一度シフトを作成して保存し直してください。");
    }

    if (result.isPresent()) {
      logRationale(result.get());
      model.addAttribute("assignmentResult", result.get());
    } else {
      model.addAttribute("unassignable", true);
    }

    model.addAttribute("shiftForm", shiftForm);
    return "index";
  }

  /**
   * 割り当ての判断根拠（各人のずれ・入れる枠・未出勤者の理由・ずれの合計）を INFO ログに出力します。
   *
   * <p>画面には表示しないため、後から根拠を追えるようにログへ残します。
   *
   * @param result 割当結果
   */
  private void logRationale(AssignmentResult result) {
    for (ShiftAssignment assignment : result.assignments()) {
      Employee employee = assignment.employee();
      LOGGER.info(
          "割当 {} 希望={}〜{} 割当={} 差={}分 入れる枠={}",
          escapeControlCharacters(employee.name()),
          employee.start().format(TIME_FORMATTER),
          employee.end().format(TIME_FORMATTER),
          formatSlot(assignment.slot()),
          assignment.gapMinutes(),
          formatSlots(employee.workableSlots()));
    }
    for (Employee employee : result.unassignedEmployees()) {
      LOGGER.info(
          "未出勤 {} 理由={} 入れる枠={}",
          escapeControlCharacters(employee.name()),
          escapeControlCharacters(result.unassignedReasonLabel(employee)),
          formatSlots(employee.workableSlots()));
    }
    LOGGER.info("合計 = {} = {} 分", join(result.gapMinutesList()), result.score());
  }

  /**
   * ログ出力用に、改行などの制御文字を可視文字列へ変換します。
   *
   * <p>氏名は利用者が入力した任意の文字列のため、そのまま出力すると偽のログ行を挿入できてしまいます。 画面や保存する値は変えず、ログへ渡す直前にだけ変換します。
   *
   * @param value 変換前の文字列
   * @return 制御文字を 改行は {@code \r}・{@code \n}、その他は 16 進数 4 桁のエスケープ表記に変換した文字列
   */
  private String escapeControlCharacters(String value) {
    StringBuilder escaped = new StringBuilder();
    for (char c : value.toCharArray()) {
      if (c == '\r') {
        escaped.append("\\r");
      } else if (c == '\n') {
        escaped.append("\\n");
      } else if (Character.isISOControl(c)) {
        escaped.append(String.format("\\u%04x", (int) c));
      } else {
        escaped.append(c);
      }
    }
    return escaped.toString();
  }

  private String join(List<Integer> values) {
    return values.stream().map(value -> String.valueOf(value)).collect(Collectors.joining(" + "));
  }

  private String formatSlots(List<ShiftSlot> slots) {
    return slots.stream().map(slot -> formatSlot(slot)).collect(Collectors.joining(", ", "[", "]"));
  }

  private String formatSlot(ShiftSlot slot) {
    return slot.startTime().format(TIME_FORMATTER) + "〜" + slot.endTime().format(TIME_FORMATTER);
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
   * BindingResult から氏名のエラーを InvalidNameError のリストに変換します。
   *
   * <p>行番号の昇順で並べられます。
   *
   * @param bindingResult バリデーション結果
   * @return 氏名のエラーリスト（行順）
   */
  private List<InvalidNameError> toNameErrors(BindingResult bindingResult) {
    List<NameFieldError> fieldErrors = new ArrayList<>();

    for (FieldError error : bindingResult.getFieldErrors()) {
      Matcher matcher = NAME_FIELD_PATTERN.matcher(error.getField());
      if (matcher.matches()) {
        fieldErrors.add(
            new NameFieldError(Integer.parseInt(matcher.group(1)), error.getDefaultMessage()));
      }
    }

    fieldErrors.sort(Comparator.comparingInt(fieldError -> fieldError.rowIndex()));

    return fieldErrors.stream()
        .map(fieldError -> new InvalidNameError(fieldError.rowIndex(), fieldError.message()))
        .toList();
  }

  /**
   * 雇用区分の入力エラーを検証します（V-7）。
   *
   * <p>従業員名が入力された行で、雇用区分が FULL_TIME, PART_TIME, MANAGER 以外の場合はエラーです。
   * 従業員名が空の行の不正な区分はエラーにしません。
   *
   * @param shiftForm フォームデータ
   * @return 雇用区分エラーのリスト
   */
  private List<InvalidEmploymentTypeError> toEmploymentTypeErrors(ShiftForm shiftForm) {
    List<InvalidEmploymentTypeError> errors = new ArrayList<>();

    for (int i = 0; i < shiftForm.getEmployees().size(); i++) {
      EmployeeForm form = shiftForm.getEmployees().get(i);
      // 従業員名が空の行は検査しない（V-1で除外）
      if (form.getName() == null || form.getName().isBlank()) {
        continue;
      }
      // 雇用区分が 3 択以外の場合はエラー
      if (EmploymentType.parse(form.getEmploymentType()).isEmpty()) {
        errors.add(new InvalidEmploymentTypeError(i, "雇用区分は「常勤」「パート」「管理職」から選択してください。"));
      }
    }

    return errors;
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
      EmploymentType employmentType =
          EmploymentType.parse(form.getEmploymentType()).orElse(EmploymentType.FULL_TIME);
      if (off) {
        employees.add(Employee.onLeave(form.getName(), employmentType));
      } else {
        employees.add(Employee.working(form.getName(), employmentType, start, end));
      }
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

  /**
   * 氏名のフィールドエラーを、並べ替えのために行番号とあわせて保持するレコードです。
   *
   * @param rowIndex 行番号（0 始まり）
   * @param message エラーメッセージ
   */
  private record NameFieldError(int rowIndex, String message) {}
}
