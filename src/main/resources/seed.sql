-- 初期データ：従業員 12 名（休み 3 名、勤務時間帯はまばら）
-- 保存済みの入力がある（saved_employee が空でない）ときは投入しないため、
-- 再起動しても利用者が保存した内容を上書きしません。
-- 8 名以上が勤務可能で、枠 1〜6 をすべて満たす割り当て案が存在するように組んでいます。
-- 雇用区分：常勤5名、パート4名、管理職3名
INSERT INTO saved_employee (row_index, name, employment_type, off, start_time, end_time)
SELECT row_index, name, employment_type, off, start_time, end_time
FROM (VALUES
  (0,  '佐藤 太郎',   'FULL_TIME', FALSE, TIME '07:30', TIME '15:00'),
  (1,  '鈴木 一郎',   'FULL_TIME', FALSE, TIME '07:30', TIME '14:30'),
  (2,  '高橋 美咲',   'PART_TIME', FALSE, TIME '08:00', TIME '16:00'),
  (3,  '田中 花子',   'FULL_TIME', TRUE,  CAST(NULL AS TIME), CAST(NULL AS TIME)),
  (4,  '伊藤 健太',   'PART_TIME', FALSE, TIME '08:30', TIME '17:00'),
  (5,  '渡辺 陽子',   'MANAGER',   FALSE, TIME '09:00', TIME '16:30'),
  (6,  '山本 大輔',   'PART_TIME', TRUE,  CAST(NULL AS TIME), CAST(NULL AS TIME)),
  (7,  '中村 さくら', 'FULL_TIME', FALSE, TIME '09:00', TIME '18:00'),
  (8,  '小林 翔太',   'MANAGER',   FALSE, TIME '09:00', TIME '18:30'),
  (9,  '加藤 恵子',   'FULL_TIME', FALSE, TIME '08:00', TIME '18:30'),
  (10, '吉田 直樹',   'MANAGER',   TRUE,  CAST(NULL AS TIME), CAST(NULL AS TIME)),
  (11, '山田 由美',   'PART_TIME', FALSE, TIME '10:00', TIME '17:00')
) AS seed(row_index, name, employment_type, off, start_time, end_time)
WHERE NOT EXISTS (SELECT 1 FROM saved_employee);
