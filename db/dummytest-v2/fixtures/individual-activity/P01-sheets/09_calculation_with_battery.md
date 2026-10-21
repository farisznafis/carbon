# Filled sheet — 09_calculation_with_battery

| Item | Dummy value |
|---|---|
| Project | P01 / P000001 |
| 対象期間 | 2026-08-01 – 2033-03-31 |
| 注意 | テストデータ / 架空書類 / 公式提出不可 |
| E01 | 株式会社さくらエナジー / 東京都千代田区丸の内1丁目２番３号 / TS-PCS-35 / IT-S01-PCS-1 / battery=有 / status=APPROVED |
| E02 | 株式会社さくらエナジー / 東京都千代田区丸の内2丁目２番３号 / TS-PCS-40 / IT-S02-PCS-1 / battery=有 / status=APPROVED |
| E04 | 株式会社みなと物流 / 神奈川県横浜市西区みなとみらい2丁目２番３号 / TS-PCS-50 / IT-S04-PCS-1 / battery=有 / status=APPROVED |
| E05 | 株式会社あおば製作所 / 宮城県仙台市青葉区中央1丁目２番３号 / TS-PCS-55 / IT-S05-PCS-1 / battery=有 / status=APPROVED |
| E07 | 株式会社東都印刷 / 東京都品川区東品川1丁目２番３号 / TS-PCS-65 / IT-S07-PCS-1 / battery=有 / status=OPEN |
| E08 | 株式会社信州精密 / 長野県松本市中央1丁目２番３号 / TS-PCS-70 / IT-S08-PCS-1 / battery=有 / status=REJECTED |

---

## Complete source-form representation

# 09. ＜YYYY年（M~M月）＞算定シート(蓄電池あり)

> Excel worksheet 9/10 (visible) from the uploaded EN-R-002 individual activity list. Period-specific calculation form. The original calculation rows are formula-linked to the matching member-information sheet.


## ■　基本情報

| 項目 | 値 / 入力 | 単位 |
| --- | --- | --- |
| 本シートの算定開始日 | ________________ |  |
| 本シートの算定終了日 | ________________ |  |
| 本シートの暦年 | [AUTO] | 年 |
| 本シートの年度 | [AUTO] | 年度 |
| 使用する排出係数の年度 | ________________ | 年度 |
| 系統電力排出係数（全電源） | ________________ | kg-CO2/ｋWh |
| 発電量原単位 | [AUTO] | kWh/kW/日 |
| 売電量原単位 | [AUTO] | kWh/kW/日 |
| パワコン使用電力デフォルト値 | [AUTO] | kWh/kWh |
| 蓄電池経由率デフォルト値 | [AUTO] | kWh |
| 蓄電池容量≦閾値 | [AUTO] | % |
| 蓄電池容量＞閾値 | [AUTO] | % |

## ■　算定結果

| 項目 | 結果 | 単位 |
| --- | --- | --- |
| 排出削減量 | [AUTO] | kg-CO2 |
| 再生可能エネルギー量 | [AUTO] | MWh |

## ID

| Excel column | 個別活動項目 | Source / entry |
| --- | --- | --- |
| C | ID | [AUTO] |
| D | 対象可否 | [AUTO] |
| E | 出力(kW) | [AUTO] |
| F | 発電電力量のモニタリング位置 | [AUTO] |
| G | パワーコンディショナー<br>夜間待機電力 (kW) | [AUTO] |
| H | ICT機器消費電力<br> (kW) | [AUTO] |
| I | 蓄電池容量(kWh) | [AUTO] |
| J | 蓄電池<br>充電効率 (%) | [AUTO] |
| K | 蓄電池<br>放電効率 (%) | [AUTO] |
| L | 算定開始日 | [AUTO] |
| M | 算定終了日 | [AUTO] |
| N | 算定日数 | [AUTO] |
| O | 発電量 (kWh) | [AUTO] |
| P | 売電量 (kWh) | [AUTO] |
| Q | パワコン排出量<br>※夜間<br>(kg-CO2) | [AUTO] |
| R | パワコン排出量<br>(kg-CO2) | [AUTO] |
| S | 蓄電池排出量<br>(kg-CO2) | [AUTO] |
| T | IoT端末の排出量<br>（kg-CO2) | [AUTO] |
| U | プロジェクト実施後排出量<br>(kg-CO2) | [AUTO] |
| V | ベースライン排出量<br>(kg-CO2) | [AUTO] |
| W | 排出削減量<br>(kg-CO2) | [AUTO] |
| X | 再生可能エネルギー量<br>(MWh) | [AUTO] |

## Per-activity calculation register (30 rows)

| ID | 算定開始日 | 算定終了日 | 発電量 (kWh) | 売電量 (kWh) | 排出削減量<br>(kg-CO2) |
| --- | --- | --- | --- | --- | --- |
| 2-1 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 2-2 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 2-3 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 2-4 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 2-5 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 2-6 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 2-7 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 2-8 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 2-9 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 2-10 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 2-11 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 2-12 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 2-13 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 2-14 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 2-15 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 2-16 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 2-17 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 2-18 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 2-19 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 2-20 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 2-21 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 2-22 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 2-23 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 2-24 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 2-25 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 2-26 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 2-27 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 2-28 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 2-29 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 2-30 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |

The original sheet has more computed columns than the compact register above; the exact full set appears in the field dictionary. Do not manually overwrite formula cells in Excel.

