package com.example.shiftmatch.service;

import com.example.shiftmatch.domain.EmployeeProfile;

/**
 * 有効な従業員のプロファイルと元のインデックスを保持します。
 */
record ValidEmployeeInfo(EmployeeProfile profile, int originalIndex) {}
