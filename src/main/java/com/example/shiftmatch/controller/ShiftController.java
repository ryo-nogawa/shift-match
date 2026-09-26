package com.example.shiftmatch.controller;

import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.HolidayDataUnavailableError;
import com.example.shiftmatch.domain.MonthlyShiftInput;
import com.example.shiftmatch.domain.MonthlyShiftResult;
import com.example.shiftmatch.domain.ShiftStorageException;
import com.example.shiftmatch.persistence.SavedMonthlyShift;
import com.example.shiftmatch.service.HolidayService;
import com.example.shiftmatch.service.MonthlyShiftService;
import com.example.shiftmatch.service.ShiftStorageService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * 月間シフト作成画面のコントローラーです。
 */
@Controller
public class ShiftController {

  private static final Logger LOGGER = LoggerFactory.getLogger(ShiftController.class);

  /** Thymeleaf のフラグメント指定（区切りがメソッド参照の検査に誤検出されないよう分割）。 */
  private static final String RESULT_FRAGMENT = "fragments/result :" + ": resultPanel";

  private static final String SAVE_ERROR_MESSAGE = "保存に失敗しました。もう一度シフトを作成して保存し直してください";

  private static final String DEFAULT_START_TIME = "07:30";

  private static final String DEFAULT_END_TIME = "18:30";

  private final MonthlyShiftService monthlyShiftService;

  private final MonthlyFormConverter monthlyFormConverter;

  private final HolidayService holidayService;

  private final MonthlyResultViewFactory monthlyResultViewFactory;

  private final ShiftStorageService shiftStorageService;

  private final SavedInputFormConverter savedInputFormConverter;

  /**
   * コンストラクタです。
   *
   * @param monthlyShiftService 月間シフト作成サービス
   * @param monthlyFormConverter フォーム変換サービス
   * @param holidayService 祝日サービス
   * @param monthlyResultViewFactory 結果画面の表示モデルの生成
   * @param shiftStorageService シフトの保存・復元サービス
   * @param savedInputFormConverter 保存済みの入力をフォームへ変換するコンバーター
   */
  @Autowired
  public ShiftController(
      MonthlyShiftService monthlyShiftService,
      MonthlyFormConverter monthlyFormConverter,
      HolidayService holidayService,
      MonthlyResultViewFactory monthlyResultViewFactory,
      ShiftStorageService shiftStorageService,
      SavedInputFormConverter savedInputFormConverter) {
    this.monthlyShiftService = monthlyShiftService;
    this.monthlyFormConverter = monthlyFormConverter;
    this.holidayService = holidayService;
    this.monthlyResultViewFactory = monthlyResultViewFactory;
    this.shiftStorageService = shiftStorageService;
    this.savedInputFormConverter = savedInputFormConverter;
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
    ShiftForm shiftForm =
        savedInputFormConverter.toForm(shiftStorageService.loadInput(), YearMonth.now());
    model.addAttribute("shiftForm", shiftForm);
    model.addAttribute("initialStep", 1);
    addSavedResult(shiftForm, model);

    return "index";
  }

  /**
   * 指定した月の保存済みシフトを、画面 3 の中身（フラグメント）だけで返します。
   *
   * <p>画面 1 で対象月を切り替えたあとに、保存済みのシフトを取得するために使います。
   *
   * @param month 対象月（YYYY-MM 形式）
   * @param model モデルオブジェクト
   * @return 画面 3 の中身のフラグメント
   * @throws ResponseStatusException month が不正、または祝日データの収録範囲外の場合（400）
   */
  @GetMapping("/shift/saved")
  public String savedShift(@RequestParam("month") String month, Model model) {
    YearMonth yearMonth =
        InputParsers.parseYearMonth(month)
            .filter(value -> CalendarController.isWithinHolidayDataRange(value))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));
    addSavedResult(yearMonth, model);
    return RESULT_FRAGMENT;
  }

  private void addSavedResult(ShiftForm shiftForm, Model model) {
    Optional<YearMonth> month = InputParsers.parseYearMonth(shiftForm.getTargetMonth());
    if (month.isEmpty()) {
      model.addAttribute("resultSource", "none");
      return;
    }
    addSavedResult(month.get(), model);
  }

  private void addSavedResult(YearMonth month, Model model) {
    Optional<SavedMonthlyShift> saved = shiftStorageService.load(month);
    if (saved.isEmpty()) {
      model.addAttribute("resultSource", "none");
      return;
    }
    MonthlyShiftResult result = saved.get().result();
    model.addAttribute("monthlyResult", result);
    model.addAttribute(
        "resultView",
        monthlyResultViewFactory.create(
            result, saved.get().employeeNames(), holidaysOrEmpty(result.month())));
    model.addAttribute("resultSource", "saved");
  }

  private Map<LocalDate, String> holidaysOrEmpty(YearMonth month) {
    try {
      return holidayService.holidaysOf(month);
    } catch (HolidayDataUnavailableError e) {
      LOGGER.warn("祝日データが取得できないため、祝日なしで表示します", e);
      return Map.of();
    }
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
    MonthlyShiftInput input = monthlyFormConverter.toInput(shiftForm);

    try {
      // シフト作成サービスを呼び出す
      var result = monthlyShiftService.create(input);
      model.addAttribute("monthlyResult", result);
      model.addAttribute(
          "resultView",
          monthlyResultViewFactory.create(
              result, validEmployeeNames(shiftForm), holidayService.holidaysOf(result.month())));
      model.addAttribute("initialStep", 3);
      model.addAttribute("resultSource", "fresh");
      saveOrReportFailure(input, result, model);
    } catch (com.example.shiftmatch.domain.InvalidMonthlyInputException e) {
      // エラーの場合
      model.addAttribute("inputErrors", e.errors());
      model.addAttribute("initialStep", 1);
      model.addAttribute("resultSource", "none");
    }

    // フォームは常にモデルに含める
    model.addAttribute("shiftForm", shiftForm);
    return "index";
  }

  private void saveOrReportFailure(
      MonthlyShiftInput input, MonthlyShiftResult result, Model model) {
    try {
      shiftStorageService.save(input, result);
    } catch (ShiftStorageException e) {
      LOGGER.error("シフトの保存に失敗しました", e);
      model.addAttribute("saveError", SAVE_ERROR_MESSAGE);
    }
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
