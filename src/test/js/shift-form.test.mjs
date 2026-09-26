// 画面の JavaScript のうち DOM に依存しない関数のテスト。
// 依存を追加せずに Node 標準のテストランナーで実行する：node --test src/test/js/*.test.mjs
// （Maven のビルドには組み込んでいない）
import { test, describe } from "node:test";
import assert from "node:assert/strict";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const employeeList = require("../../main/resources/static/js/employee-list.js");
const dayAdjustments = require("../../main/resources/static/js/day-adjustments.js");
const stepNav = require("../../main/resources/static/js/step-nav.js");

describe("employee-list.js", () => {
  test("[8.5節] 並べ替え後の 3 行は、行の入力とパネルの入力が 0,1,2 に振り直される", () => {
    const row = (i) => ({
      rowFields: [{ name: `employees[${i}].name` }, { name: `employees[${i}].employmentType` }],
      panelFields: [
        { name: `employees[${i}].days[0].off` },
        { name: `_employees[${i}].days[0].off` },
        { name: `employees[${i}].days[4].end` },
      ],
    });
    // 画面上で 2 → 0 → 1 の順に並んだ状態（1 を削除した後の欠番 5 も含む）
    const rows = [row(2), row(0), row(5)];
    employeeList.renumber(rows);
    assert.deepEqual(
      rows.map((r) => r.rowFields.concat(r.panelFields).map((f) => f.name)),
      [0, 1, 2].map((i) => [
        `employees[${i}].name`,
        `employees[${i}].employmentType`,
        `employees[${i}].days[0].off`,
        `_employees[${i}].days[0].off`,
        `employees[${i}].days[4].end`,
      ])
    );
  });

  test("[8.1節] 要約：全曜日同じ時間帯で水曜が休み", () => {
    const d = { off: false, start: "07:30", end: "18:30" };
    const off = { off: true, start: "", end: "" };
    assert.equal(employeeList.summarize([d, d, off, d, d]), "07:30〜18:30／休：水");
  });

  test("[8.1節] 要約：時間帯が異なれば最も早い開始〜最も遅い終了", () => {
    assert.equal(
      employeeList.summarize([
        { off: false, start: "09:00", end: "15:00" },
        { off: false, start: "08:00", end: "14:30" },
        { off: false, start: "10:00", end: "18:00" },
        { off: true, start: "", end: "" },
        { off: true, start: "", end: "" },
      ]),
      "08:00〜18:00／休：木金"
    );
  });

  test("[8.1節] 要約：全曜日休み・休みなし", () => {
    const off = { off: true, start: "", end: "" };
    const d = { off: false, start: "07:30", end: "18:30" };
    assert.equal(employeeList.summarize([off, off, off, off, off]), "休：月火水木金");
    assert.equal(employeeList.summarize([d, d, d, d, d]), "07:30〜18:30");
  });

  test("[F-2][F-6][F-8] ボタンの有効・無効（端の▲▼、1 行で削除不可、12 行で追加不可）", () => {
    const three = employeeList.buttonStates(3);
    assert.deepEqual(
      three.rows.map((r) => [r.upDisabled, r.downDisabled, r.deleteDisabled]),
      [
        [true, false, false],
        [false, false, false],
        [false, true, false],
      ]
    );
    assert.equal(three.addDisabled, false);
    assert.equal(employeeList.buttonStates(1).rows[0].deleteDisabled, true);
    assert.equal(employeeList.buttonStates(12).addDisabled, true);
    assert.equal(employeeList.buttonStates(11).addDisabled, false);
  });
});

describe("day-adjustments.js", () => {
  const base = { off: false, start: "07:30", end: "18:30" };

  test("[F-11] 基本シフトと同じ内容にすると個別変更が消え、違えば残る", () => {
    const map = new Map();
    dayAdjustments.applyEdit(map, "2026-10-20", "A", { off: true, start: "", end: "" }, base);
    assert.equal(map.size, 1);
    dayAdjustments.applyEdit(map, "2026-10-20", "A", { off: false, start: "07:30", end: "18:30" }, base);
    assert.equal(map.size, 0);
    dayAdjustments.applyEdit(map, "2026-10-20", "A", { off: false, start: "09:00", end: "18:30" }, base);
    assert.deepEqual(map.get("2026-10-20|A"), { off: false, start: "09:00", end: "18:30" });
  });

  test("[F-11] 休みどうしは時刻が違っても同じとみなす", () => {
    const map = new Map([["2026-10-21|A", { off: false, start: "09:00", end: "18:00" }]]);
    dayAdjustments.applyEdit(
      map,
      "2026-10-21",
      "A",
      { off: true, start: "", end: "" },
      { off: true, start: "07:30", end: "18:30" }
    );
    assert.equal(map.size, 0);
  });

  test("[8.2節] この日を基本に戻すと、その日の個別変更だけがすべて消える", () => {
    const off = { off: true, start: "", end: "" };
    const map = new Map([
      ["2026-10-20|A", off],
      ["2026-10-20|B", off],
      ["2026-10-21|A", off],
    ]);
    dayAdjustments.resetDay(map, "2026-10-20");
    assert.deepEqual(Array.from(map.keys()), ["2026-10-21|A"]);
  });

  test("[V-9][8.5節] 送信用の一覧は対象月の分だけで、hidden は 0 から連番・従業員名入り", () => {
    const map = new Map([
      ["2026-11-02|A", { off: true, start: "", end: "" }],
      ["2026-10-21|B|x", { off: false, start: "09:00", end: "17:00" }],
      ["2026-10-20|A", { off: true, start: "", end: "" }],
    ]);
    const list = dayAdjustments.buildAdjustmentList(map, "2026-10");
    assert.equal(map.size, 3, "他の月の分は Map に残る");
    assert.deepEqual(
      dayAdjustments.toHiddenFields(list),
      [
        { name: "adjustments[0].date", value: "2026-10-20" },
        { name: "adjustments[0].employeeName", value: "A" },
        { name: "adjustments[0].off", value: "true" },
        { name: "adjustments[1].date", value: "2026-10-21" },
        { name: "adjustments[1].employeeName", value: "B|x" },
        { name: "adjustments[1].start", value: "09:00" },
        { name: "adjustments[1].end", value: "17:00" },
      ]
    );
  });

  test("[F-11] サーバーから戻った hidden 入力を Map に復元できる", () => {
    const map = dayAdjustments.parseHiddenFields([
      { name: "adjustments[1].date", value: "2026-10-21" },
      { name: "adjustments[0].date", value: "2026-10-20" },
      { name: "adjustments[0].employeeName", value: "A" },
      { name: "adjustments[0].off", value: "true" },
      { name: "adjustments[1].employeeName", value: "B" },
      { name: "adjustments[1].start", value: "09:00" },
      { name: "adjustments[1].end", value: "17:00" },
    ]);
    assert.deepEqual(Array.from(map.entries()), [
      ["2026-10-20|A", { off: true, start: "", end: "" }],
      ["2026-10-21|B", { off: false, start: "09:00", end: "17:00" }],
    ]);
  });

  test("[F-9][8.2節] カレンダーは月〜金をすべて並べ、祝日は祝日名つきで列がずれない", () => {
    // 2026-10：1 日は木曜、12 日（月）がスポーツの日
    const business = dayAdjustments
      .listWeekdays("2026-10")
      .filter((date) => date !== "2026-10-12");
    const weeks = dayAdjustments.buildCalendarWeeks("2026-10", business, [
      { date: "2026-10-12", name: "スポーツの日" },
    ]);
    assert.equal(weeks.length, 5);
    assert.deepEqual(
      weeks[0].map((c) => c && c.day),
      [null, null, null, 1, 2]
    );
    assert.deepEqual(
      weeks[2].map((c) => c.day),
      [12, 13, 14, 15, 16]
    );
    assert.equal(weeks[2][0].kind, "holiday");
    assert.equal(weeks[2][0].holidayName, "スポーツの日");
    assert.equal(weeks[2][1].kind, "business");
    assert.deepEqual(
      weeks[4].map((c) => c && c.day),
      [26, 27, 28, 29, 30]
    );
  });

  test("[F-9] 水曜が祝日の週でも木・金は木・金の列に置かれる", () => {
    // 2026-04-29（水）昭和の日
    const business = dayAdjustments
      .listWeekdays("2026-04")
      .filter((date) => date !== "2026-04-29");
    const weeks = dayAdjustments.buildCalendarWeeks("2026-04", business, [
      { date: "2026-04-29", name: "昭和の日" },
    ]);
    const last = weeks[weeks.length - 1];
    assert.deepEqual(
      last.map((c) => c && c.day),
      [27, 28, 29, 30, null]
    );
    assert.equal(last[2].kind, "holiday");
  });

  test("[8.2節] 月を変えて選択日が新しい月になければ先頭の営業日に切り替える", () => {
    assert.equal(
      dayAdjustments.chooseSelectedDate("2026-10-20", ["2026-11-02", "2026-11-04"]),
      "2026-11-02"
    );
    assert.equal(dayAdjustments.chooseSelectedDate("2026-11-04", ["2026-11-02", "2026-11-04"]), "2026-11-04");
    assert.equal(dayAdjustments.chooseSelectedDate("2026-10-20", []), null);
  });

  test("[8.2節] 変更人数は現在の有効な従業員の分だけ数える", () => {
    const off = { off: true, start: "", end: "" };
    const map = new Map([
      ["2026-10-20|A", off],
      ["2026-10-20|旧氏名", off],
    ]);
    assert.equal(dayAdjustments.countChanges(map, "2026-10-20", ["A", "B"]), 1);
  });
});

describe("step-nav.js", () => {
  test("[F-9] 月の前後移動（年またぎを含む）とラベル", () => {
    assert.equal(stepNav.shiftMonth("2026-01", -1), "2025-12");
    assert.equal(stepNav.shiftMonth("2026-12", 1), "2027-01");
    assert.equal(stepNav.monthLabel("2026-10"), "2026 年 10 月");
  });

  test("[8章] 画面ごとの「戻る」「次へ」の状態", () => {
    assert.deepEqual(stepNav.navState(1), {
      prevDisabled: true,
      nextHidden: false,
      nextType: "button",
      nextLabel: "次へ",
    });
    assert.equal(stepNav.navState(2).nextType, "submit");
    assert.equal(stepNav.navState(2).nextLabel, "1 か月分のシフトを作成");
    assert.equal(stepNav.navState(3).nextHidden, true);
  });
});
