package com.example.shiftmatch.service;

import com.example.shiftmatch.domain.MonthlyShiftInput;
import com.example.shiftmatch.domain.MonthlyShiftResult;
import com.example.shiftmatch.domain.ShiftStorageException;
import com.example.shiftmatch.persistence.MonthlyShiftRepository;
import com.example.shiftmatch.persistence.SavedMonthlyShift;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

/**
 * 入力と決定したシフトの保存・復元を行うサービス実装。
 *
 * <p>永続化は {@link MonthlyShiftRepository} に委譲します。
 */
@Service
public class ShiftStorageServiceImpl implements ShiftStorageService {

  private final MonthlyShiftRepository repository;

  /**
   * リポジトリを注入してインスタンスを生成します。
   *
   * @param repository 月間シフトのリポジトリ
   */
  public ShiftStorageServiceImpl(MonthlyShiftRepository repository) {
    this.repository = repository;
  }

  @Override
  public void save(MonthlyShiftInput input, MonthlyShiftResult result) {
    List<String> employeeNames =
        input.employees().stream()
            .map(employee -> employee.name())
            .filter(name -> name != null && !name.isEmpty())
            .toList();
    try {
      repository.save(input, result, employeeNames);
    } catch (DataAccessException e) {
      throw new ShiftStorageException("シフトの保存に失敗しました", e);
    }
  }

  @Override
  public Optional<SavedMonthlyShift> load(YearMonth month) {
    return repository.findShift(month);
  }

  @Override
  public SavedInput loadInput() {
    return new SavedInput(
        repository.findEmployees(), repository.findAdjustments(), repository.findLastTargetMonth());
  }
}
