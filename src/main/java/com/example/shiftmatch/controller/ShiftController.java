package com.example.shiftmatch.controller;

import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.service.HolidayService;
import com.example.shiftmatch.service.MonthlyShiftService;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 月間シフト作成画面のコントローラーです。
 */
@Controller
public class ShiftController {

  private static final int DEFAULT_EMPLOYEE_COUNT = 12;

  private static final String DEFAULT_START_TIME = "07:30";

  private static final String DEFAULT_END_TIME = "18:30";

  private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

  private final MonthlyShiftService monthlyShiftService;

  private final MonthlyFormConverter monthlyFormConverter;

  private final HolidayService holidayService;

  private final MonthlyResultViewFactory monthlyResultViewFactory;

  /**
   * コンストラクタです。
   *
   * @param monthlyShiftService 月間シフト作成サービス
   * @param monthlyFormConverter フォーム変換サービス
   * @param holidayService 祝日サービス
   * @param monthlyResultViewFactory 結果画面の表示モデルの生成
   */
  @Autowired
  public ShiftController(
      MonthlyShiftService monthlyShiftService,
      MonthlyFormConverter monthlyFormConverter,
      HolidayService holidayService,
      MonthlyResultViewFactory monthlyResultViewFactory) {
    this.monthlyShiftService = monthlyShiftService;
    this.monthlyFormConverter = monthlyFormConverter;
    this.holidayService = holidayService;
    this.monthlyResultViewFactory = monthlyResultViewFactory;
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
   * 月間シフト作成フォームを初期表示します。
   *
   * @param model モデルオブジェクト
   * @return ビュー名
   */
  @GetMapping("/")
  public String index(Model model) {
    ShiftForm shiftForm = new ShiftForm();

    // 対象月は今月に設定
    shiftForm.setTargetMonth(YearMonth.now().format(MONTH_FORMATTER));

    // 12 行の従業員フォームを初期化
    List<EmployeeForm> employees = new ArrayList<>();
    for (int i = 0; i < DEFAULT_EMPLOYEE_COUNT; i++) {
      EmployeeForm employee = new EmployeeForm();
      employee.setName(""); // 名前は空
      employee.setEmploymentType("FULL_TIME"); // 区分は常勤

      // 月〜金の 5 日分の基本シフト（デフォルト：休みなし、07:30〜18:30）
      List<DayForm> days = new ArrayList<>();
      for (int d = 0; d < 5; d++) {
        DayForm day = new DayForm();
        day.setOff(false);
        day.setStart(DEFAULT_START_TIME);
        day.setEnd(DEFAULT_END_TIME);
        days.add(day);
      }
      employee.setDays(days);

      employees.add(employee);
    }

    shiftForm.setEmployees(employees);
    model.addAttribute("shiftForm", shiftForm);
    model.addAttribute("initialStep", 1);

    return "index";
  }

  /**
   * 月間シフトを作成します。
   *
   * @param shiftForm フォームデータ
   * @param model モデルオブジェクト
   * @return ビュー名
   */
  @PostMapping("/shift")
  public String createShift(@ModelAttribute("shiftForm") ShiftForm shiftForm, Model model) {
    // employees が空の場合は 1 行を補う
    if (shiftForm.getEmployees().isEmpty()) {
      EmployeeForm emptyEmployee = new EmployeeForm();
      emptyEmployee.setEmploymentType("FULL_TIME");
      List<DayForm> days = new ArrayList<>();
      for (int d = 0; d < 5; d++) {
        DayForm day = new DayForm();
        day.setOff(false);
        day.setStart(DEFAULT_START_TIME);
        day.setEnd(DEFAULT_END_TIME);
        days.add(day);
      }
      emptyEmployee.setDays(days);
      shiftForm.getEmployees().add(emptyEmployee);
    }

    // フォームをドメインモデルに変換
    com.example.shiftmatch.domain.MonthlyShiftInput input = monthlyFormConverter.toInput(shiftForm);

    try {
      // シフト作成サービスを呼び出す
      var result = monthlyShiftService.create(input);
      model.addAttribute("monthlyResult", result);
      model.addAttribute(
          "resultView",
          monthlyResultViewFactory.create(
              result, validEmployeeNames(shiftForm), holidayService.holidaysOf(result.month())));
      model.addAttribute("initialStep", 3);
    } catch (com.example.shiftmatch.domain.InvalidMonthlyInputException e) {
      // エラーの場合
      model.addAttribute("inputErrors", e.errors());
      model.addAttribute("initialStep", 1);
    }

    // フォームは常にモデルに含める
    model.addAttribute("shiftForm", shiftForm);
    return "index";
  }

  private static List<String> validEmployeeNames(ShiftForm shiftForm) {
    List<String> names = new ArrayList<>();
    for (EmployeeForm employee : shiftForm.getEmployees()) {
      String name = employee.getName();
      if (name != null && !name.isBlank()) {
        names.add(name);
      }
    }
    return names;
  }
}
