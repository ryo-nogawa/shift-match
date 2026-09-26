package com.example.shiftmatch.persistence;

import com.example.shiftmatch.domain.Holiday;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 祝日データを永続化するリポジトリ。
 *
 * <p>日本の祝日データを H2 データベースに保存・取得します。
 */
@Repository
public class HolidayRepository {

  private final JdbcClient jdbcClient;

  /**
   * データベースクライアントを注入してインスタンスを生成します。
   *
   * @param jdbcClient データベースアクセス用のクライアント
   */
  public HolidayRepository(JdbcClient jdbcClient) {
    this.jdbcClient = jdbcClient;
  }

  /**
   * 祝日データを全件置き換えます。
   *
   * <p>既存データは全削除してから新しいデータを登録するため、途中で失敗しても元のデータが残ります。
   *
   * @param holidays 祝日データのリスト
   */
  @Transactional
  public void replaceAll(List<Holiday> holidays) {
    jdbcClient.sql("DELETE FROM holiday").update();
    for (Holiday holiday : holidays) {
      jdbcClient
          .sql("INSERT INTO holiday (holiday_date, name) VALUES (?, ?)")
          .params(holiday.date(), holiday.name())
          .update();
    }
  }

  /**
   * 指定した年の祝日を取得します。
   *
   * <p>祝日は日付の昇順で返されます。
   *
   * @param year 年（例：2026）
   * @return 祝日のリスト（日付順）
   */
  public List<Holiday> findByYear(int year) {
    return jdbcClient
        .sql(
            "SELECT holiday_date, name FROM holiday"
                + " WHERE YEAR(holiday_date) = ?"
                + " ORDER BY holiday_date")
        .params(year)
        .query(
            (rs, rowNum) ->
                new Holiday(rs.getObject("holiday_date", LocalDate.class), rs.getString("name")))
        .list();
  }

  /**
   * 指定した年の祝日データが保存済みかどうかを確認します。
   *
   * <p>その年の祝日が 1 件以上保存されている場合に true を返します。
   *
   * @param year 年（例：2026）
   * @return その年の祝日データが保存済みの場合は true、そうでない場合は false
   */
  public boolean existsInYear(int year) {
    Integer count =
        jdbcClient
            .sql("SELECT COUNT(*) FROM holiday WHERE YEAR(holiday_date) = ?")
            .params(year)
            .query(Integer.class)
            .single();
    return count != null && count > 0;
  }
}
