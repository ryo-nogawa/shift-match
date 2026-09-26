package com.example.shiftmatch.controller;

import com.example.shiftmatch.domain.EmploymentType;
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

  /**
   * コンストラクタです。
   *
   * @param monthlyShiftService 月間シフト作成サービス
   * @param monthlyFormConverter フォーム変換サービス
   */
  @Autowired
  public ShiftController(
      MonthlyShiftService monthlyShiftService, MonthlyFormConverter monthlyFormConverter) {
    this.monthlyShiftService = monthlyShiftService;
    this.monthlyFormConverter = monthlyFormConverter;
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
   * <p>T4 で実装予定です。
   *
   * @param shiftForm フォームデータ
   * @param model モデルオブジェクト
   * @return ビュー名
   */
  @PostMapping("/shift")
  public String createShift(@ModelAttribute("shiftForm") ShiftForm shiftForm, Model model) {
    // TODO: T4 で実装
    model.addAttribute("shiftForm", shiftForm);
    model.addAttribute("initialStep", 1);
    return "index";
  }
}
