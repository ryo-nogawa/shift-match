/**
 * シフト入力フォームの行を動的に追加する機能を提供します。
 */
document.addEventListener("DOMContentLoaded", function () {
  const addRowBtn = document.getElementById("add-row-btn");
  const employeeRows = document.getElementById("employee-rows");

  addRowBtn.addEventListener("click", function () {
    // 現在の行数を取得
    const currentRowCount = employeeRows.querySelectorAll("tr").length;

    // 新規行のHTML要素を作成
    const newRow = document.createElement("tr");

    // 氏名入力欄
    const nameCell = document.createElement("td");
    const nameInput = document.createElement("input");
    nameInput.type = "text";
    nameInput.name = "employees[" + currentRowCount + "].name";
    nameInput.placeholder = "氏名を入力";
    nameCell.appendChild(nameInput);

    // 早番希望セレクト
    const earlyCell = document.createElement("td");
    const earlySelect = document.createElement("select");
    earlySelect.name = "employees[" + currentRowCount + "].earlyWish";

    const emptyOption1 = document.createElement("option");
    emptyOption1.value = "";
    emptyOption1.textContent = "-- 未選択 --";

    const desiredOption1 = document.createElement("option");
    desiredOption1.value = "DESIRED";
    desiredOption1.textContent = "◎ 希望";

    const availableOption1 = document.createElement("option");
    availableOption1.value = "AVAILABLE";
    availableOption1.textContent = "○ 可能";

    const unavailableOption1 = document.createElement("option");
    unavailableOption1.value = "UNAVAILABLE";
    unavailableOption1.textContent = "× 不可";

    earlySelect.appendChild(emptyOption1);
    earlySelect.appendChild(desiredOption1);
    earlySelect.appendChild(availableOption1);
    earlySelect.appendChild(unavailableOption1);

    earlyCell.appendChild(earlySelect);

    // 遅番希望セレクト
    const lateCell = document.createElement("td");
    const lateSelect = document.createElement("select");
    lateSelect.name = "employees[" + currentRowCount + "].lateWish";

    const emptyOption2 = document.createElement("option");
    emptyOption2.value = "";
    emptyOption2.textContent = "-- 未選択 --";

    const desiredOption2 = document.createElement("option");
    desiredOption2.value = "DESIRED";
    desiredOption2.textContent = "◎ 希望";

    const availableOption2 = document.createElement("option");
    availableOption2.value = "AVAILABLE";
    availableOption2.textContent = "○ 可能";

    const unavailableOption2 = document.createElement("option");
    unavailableOption2.value = "UNAVAILABLE";
    unavailableOption2.textContent = "× 不可";

    lateSelect.appendChild(emptyOption2);
    lateSelect.appendChild(desiredOption2);
    lateSelect.appendChild(availableOption2);
    lateSelect.appendChild(unavailableOption2);

    lateCell.appendChild(lateSelect);

    // 新規行に各セルを追加
    newRow.appendChild(nameCell);
    newRow.appendChild(earlyCell);
    newRow.appendChild(lateCell);

    // テーブルに新規行を追加
    employeeRows.appendChild(newRow);
  });
});
