# EN-R-002 individual activity — complete worksheet reference

Every section below is one source worksheet, in source order. Content is retained from `source/EN-R-002_kobetukatudou_md/`.

---

## Worksheet 01: `01_change_history.md`

# 01. 変更履歴

> Excel worksheet 1/10 (visible) from the uploaded EN-R-002 individual activity list. Version history, read-only.


## 変更履歴

| Ver | 改定日 | 改定内容 |
| --- | --- | --- |
| 1 | 2025.7 | 当リストのVer設定開始 |
| 1.1 | 2026.2 | 係数シート<br>・系統電力の排出係数を更新<br>会員情報(蓄電池なし)<br>・対象可否を自動判定から選択式に変更<br>・プロジェクト登録要件のうち自動判定だったものを選択式に変更<br>・認証対象期間終了日(退会日)の式を更新<br>・網掛けの条件付き書式を解除<br>・追加性評価に投資額やランニングコスト確認書類と補助金情報の記入欄を追加<br>会員情報(蓄電池あり)<br>・対象可否を自動判定から選択式に変更<br>・プロジェクト登録要件のうち自動判定だったものを選択式に変更<br>・認証対象期間終了日(退会日)の式を更新<br>・網掛けの条件付き書式を解除<br>・追加性評価に投資額やランニングコスト確認書類と補助金情報の記入欄を追加 |
| 1.2 | 2026.7 | 会員情報(蓄電池なし)<br>会員情報(蓄電池あり)<br>・追加的設備情報や基準日で記入する日付の定義を明確化。<br>・追加性評価における総投資額の注記を明確化。 |

The source worksheet has additional blank rows for future changes; it is not a member-entry form.

---

## Worksheet 02: `02_introduction.md`

# 02. はじめに

> Excel worksheet 2/10 (visible) from the uploaded EN-R-002 individual activity list. Instructional/reference sheet. No fillable form.


## ■各種基準日

| Original worksheet heading | Rule / explanation |
| --- | --- |
| ◆設備が入会申込日の2年前以降に稼働していること | 設備稼働開始（発電開始）から２年以内に入会申込していること。 （あるいは入会申込後に設備稼働開始していること） |
| ◆モニタリングが可能になった日 | 連系開始日（売電開始日） |
| ◆認証開始日 | 入会申込日もしくはモニタリングが可能になった日のいずれか遅い日 |
| ＜発電量＞ | 設備稼働開始日（発電開始日）～モニタリング実施日（会員がモニターを撮影した日） |
| ・モニターでモニタリングの場合 | 連系開始日（売電開始日）～モニタリング実施日（会員がモニターを撮影した日） |
| ・電力会社発行の明細の場合 | 連系開始日（売電開始日）～明細書に記載の検針日 |

## ■ 会員の削減活動開始日について

| Case | Rule / reminder |
| --- | --- |
| 【設備稼働開始後に入会を申し込む場合】 | 設備稼働開始日が入会申し込み日の2年前の日以降であれば、入会することができる。 |
| 【設備稼働開始前に入会を申し込む場合】 | 設備稼働開始日は入会申し込み日以降となるので、入会することができる。 ※入会後、設備の稼働が開始していることを確認する。 |
| 【入会不可のケース】 | 設備稼働開始日が入会申し込み日の2年前の日より前の場合、入会することができません。 |

## ■ 原単位算定のためのモニタリング期間

> 設備の仕様上、発電量及び売電量の積算値は任意の日付からの 把握はできない場合は、設備稼働開始日及び売電開始日からの積算値しか把握 できないため、以下のモニタリング期間にて原単位を算出することが可能。

| Illustration / method | Original section |
| --- | --- |
| Case A | ＜発電量・売電量ともにパワコン・モニター表示値にてモニタリングする場合＞ |
| Case B | ＜発電量はパワコン・モニター表示値、売電量は電力会社発行の明細書にてモニタリングする場合＞ |

The original worksheet includes timeline illustrations. These are explanatory graphics, not separate fields to fill.

---

## Worksheet 03: `03_cover.md`

# 03. 表紙

> Excel worksheet 3/10 (visible) from the uploaded EN-R-002 individual activity list. Cover and certification-period form.


## ■プロジェクト情報

| 項目 | 入力 / 値 | Type |
| --- | --- | --- |
| プロジェクト番号 | ________________ | Fill |
| プロジェクト名称 | ________________ | Fill |
| プログラム型運営・管理者 | ________________ | Fill |
| 適用方法論 | EN-R-002「太陽光発電設備の導入」 | Fixed |
| 削減活動の実施者 | ________________ | Fill |
| 認証対象期間の開始日 | ________________ | Fill |
| 認証対象期間の終了日 | ________________ | Fill |

## ■認証を申請する期間（本認証申請）

| 項目 | 入力 / 値 | Type |
| --- | --- | --- |
| 認証回数 | ________________ | Fill |
| 認証を申請する期間の開始日 | ________________ | Fill |
| 認証を申請する期間の終了日 | ________________ | Fill |
| 認証を申請する期間の日数 | [AUTO] | Auto formula |

## ■削減活動の開始日

| 項目 | 入力 / 値 | Type |
| --- | --- | --- |
| 削減活動の開始日（モニタリング報告書に記載）※ | ________________ | Fill |

## ■モニタリングデータ

| 項目 | 蓄電池なし | 蓄電池あり |
| --- | --- | --- |
| 会員数 | ________________ | ________________ |
| （サンプリングの場合のみ記入）<br>必要なサンプル数 | ________________ | ________________ |
| 調査依頼数 | ________________ | ________________ |
| 回答数 | ________________ | ________________ |
| 原単位(発電） | [AUTO] | [AUTO] |
| 原単位(売電） | [AUTO] | [AUTO] |
| 全体の原単位（発電） | [AUTO] | ________________ |
| 全体の原単位（売電） | [AUTO] | ________________ |

- Note on start date: ※今回の認証申請の対象としているプロジェクト実施後の設備の中で、設備稼働開始日が最も古い日付をご記入ください。
- Source color legend: 入力が必要なセル（プルダウンから選択）, 入力が必要なセル（自由記入）, 入力が不要なセル（自動入力）, 入力が任意のセル, 項目名など

---

## Worksheet 04: `04_emission_factors.md`

# 04. 係数シート

> Excel worksheet 4/10 (visible) from the uploaded EN-R-002 individual activity list. Parameters and emission-factor reference. Keep original source/version notes.


## ■付随的排出量（パワコンの使用に伴う電力の使用）

| 項目 | 値 / 入力 | 単位 |
| --- | --- | --- |
| デフォルト値 | 0.02 | kWh/kWh |

## ■蓄電池経由率デフォルト値

| 項目 | 値 / 入力 | 単位 |
| --- | --- | --- |
| 蓄電池容量閾値 | 4 | kwh |
| 蓄電池容量≦閾値 | 60 | % |
| 蓄電池容量＞閾値 | 70 | % |

## ■系統電力の排出係数

> デフォルト値　※対象年度の値または最新値をご確認下さい。

| 年度 | 全電源排出係数 | 単位 |
| --- | --- | --- |
| 2013 | 0.57 | kg-CO2/ｋWh |
| 2014 | 0.554 | kg-CO2/ｋWh |
| 2015 | 0.531 | kg-CO2/ｋWh |
| 2016 | 0.518 | kg-CO2/ｋWh |
| 2017 | 0.496 | kg-CO2/ｋWh |
| 2018 | 0.462 | kg-CO2/ｋWh |
| 2019 | 0.445 | kg-CO2/ｋWh |
| 2020 | 0.433 | kg-CO2/ｋWh |
| 2021 | 0.434 | kg-CO2/ｋWh |
| 2022 | 0.438 | kg-CO2/ｋWh |
| 2023 | 0.423 | kg-CO2/ｋWh |
| 2024 | ________________ | kg-CO2/ｋWh |
| 2025 | ________________ | kg-CO2/ｋWh |
| 2026 | ________________ | kg-CO2/ｋWh |
| 2027 | ________________ | kg-CO2/ｋWh |
| 2028 | ________________ | kg-CO2/ｋWh |
| 2029 | ________________ | kg-CO2/ｋWh |
| 2030 | ________________ | kg-CO2/ｋWh |
| 2031 | ________________ | kg-CO2/ｋWh |
| 2032 | ________________ | kg-CO2/ｋWh |
| 2033 | ________________ | kg-CO2/ｋWh |
| 2034 | ________________ | kg-CO2/ｋWh |
| 2035 | ________________ | kg-CO2/ｋWh |
| 2036 | ________________ | kg-CO2/ｋWh |
| 2037 | ________________ | kg-CO2/ｋWh |
| 2038 | ________________ | kg-CO2/ｋWh |
| 2039 | ________________ | kg-CO2/ｋWh |
| 2040 | ________________ | kg-CO2/ｋWh |
| 2041 | ________________ | kg-CO2/ｋWh |
| 2042 | ________________ | kg-CO2/ｋWh |
| 2043 | ________________ | kg-CO2/ｋWh |
| 2044 | ________________ | kg-CO2/ｋWh |
| 2045 | ________________ | kg-CO2/ｋWh |
| 2046 | ________________ | kg-CO2/ｋWh |
| 2047 | ________________ | kg-CO2/ｋWh |
| 2048 | ________________ | kg-CO2/ｋWh |
| 2049 | ________________ | kg-CO2/ｋWh |
| 2050 | ________________ | kg-CO2/ｋWh |

【出典】EN-R-002 Ver.3.5; 【出典】EN-R-002 Ver.3.5

【出典】Ｊ－クレジット制度　排出削減・除去プロジェクト用モニタリング・算定規程　Ver. 5.5

Important: the workbook explicitly prints methodology/source versions that may differ from other methodology PDFs. They are reproduced, not silently updated.

---

## Worksheet 05: `05_calculation_summary.md`

# 05. 算定結果

> Excel worksheet 5/10 (visible) from the uploaded EN-R-002 individual activity list. Consolidated calculation: one line per reporting window.


## 小計

> ※各暦年と年度に該当する算定シートの排出削減量、再生可能エネルギー量、非化石エネルギー量の合計値を関数を用いて参照してください。参照されていない場合受領いたしません。

| 対象となる期間 | 排出削減量 (ｔ-CO2) | 再生可能エネルギー量（MWh） | 非化石エネルギー量（kWh） |
| --- | --- | --- | --- |
| 2016年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2016年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2017年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2017年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2018年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2018年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2019年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2019年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2020年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2020年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2021年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2021年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2022年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2022年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2023年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2023年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2024年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2024年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2025年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2025年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2026年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2026年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2027年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2027年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2028年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2028年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2029年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2029年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2030年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2030年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2031年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2031年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2032年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2032年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2033年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2033年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2034年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2034年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2035年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2035年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2036年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2036年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2037年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2037年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2038年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2038年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2039年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2039年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2040年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2040年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2041年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2041年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2042年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2042年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2043年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2043年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2044年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2044年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2045年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2045年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2046年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2046年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2047年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2047年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2048年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2048年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2049年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2049年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2050年1月～3月 | [LINK FROM CALC] | ________________ | [AUTO] |
| 2050年4月～12月 | [LINK FROM CALC] | ________________ | [AUTO] |

## モニタリング報告書への転記

> モニタリング報告書へは下記緑色のセル内の数値を反映ください。

| 対象となる期間 | 排出削減量 (ｔ-CO2) | 再生可能エネルギー量（MWh） | 非化石エネルギー量（kWh） |
| --- | --- | --- | --- |
| 2016年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2016年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2017年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2017年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2018年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2018年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2019年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2019年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2020年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2020年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2021年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2021年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2022年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2022年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2023年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2023年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2024年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2024年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2025年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2025年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2026年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2026年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2027年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2027年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2028年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2028年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2029年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2029年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2030年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2030年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2031年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2031年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2032年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2032年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2033年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2033年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2034年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2034年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2035年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2035年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2036年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2036年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2037年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2037年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2038年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2038年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2039年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2039年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2040年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2040年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2041年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2041年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2042年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2042年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2043年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2043年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2044年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2044年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2045年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2045年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2046年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2046年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2047年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2047年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2048年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2048年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2049年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2049年4月～12月 | [AUTO] | [AUTO] | [AUTO] |
| 2050年1月～3月 | [AUTO] | [AUTO] | [AUTO] |
| 2050年4月～12月 | [AUTO] | [AUTO] | [AUTO] |

## Total for monitoring report

| 項目 | Total / output |
| --- | --- |
| 排出削減量 (ｔ-CO2) | [AUTO] |
| 再生可能エネルギー量（MWh） | [AUTO] |
| 非化石エネルギー量（kWh） | [AUTO] |

The editable subtotal cells for emission reduction and renewable energy should be linked to the period-specific calculation sheets, as directed by the original workbook. Formula computations are not executable in Markdown.

---

## Worksheet 06: `06_member_information_no_battery.md`

# 06. 会員情報(蓄電池なし)

> Excel worksheet 6/10 (visible) from the uploaded EN-R-002 individual activity list. One real record per activity/equipment serial number; 30 prepared IDs. Blank fields are fillable; formulas noted as AUTO.


## Sheet instructions and record rules

> 必要事項を記入して下さい。他プロジェクトとの重複登録有無の確認に使用します。<br>●対象可否<br>プロジェクト登録要件やその他事項を考慮して会員の対象可否を選択してください。<br>●ID<br>・個別活動実績報告リストの中で一意になるように附番をお願いいたします。（同一IDが複数シートに登場しないようにしてください。）<br>●設置場所の住所<br>・都道府県から正確な表記で記入をお願いします。<br>・丁目、番地は、半角のハイフンを使用してご記入ください。<br>●機器固有番号<br>・製品の表示、保証書等に記載されている通りにご記入ください。<br>●型番<br>・契約書、保証書等に記載されているとおりにご記入ください。

> 【注意事項】<br>・漢字、平仮名、片仮名は全角で記入をお願いいたします。<br>・数字、英字は半角で記入をお願いします。<br>・1会員あたり、機器固有番号が複数になる場合は、機器固有番号ごとに１行ずつお書きください。<br>・クレジット認証の際に、認証申請期間は全会員で統一いただくよう、お願いいたします。


## Activity records (30 original rows)

| ID | 対象可否 | 入会申込日 | 会員名 | 設置場所住所 |
| --- | --- | --- | --- | --- |
| 1-1 | ________________ | ________________ | ________________ | ________________ |
| 1-2 | ________________ | ________________ | ________________ | ________________ |
| 1-3 | ________________ | ________________ | ________________ | ________________ |
| 1-4 | ________________ | ________________ | ________________ | ________________ |
| 1-5 | ________________ | ________________ | ________________ | ________________ |
| 1-6 | ________________ | ________________ | ________________ | ________________ |
| 1-7 | ________________ | ________________ | ________________ | ________________ |
| 1-8 | ________________ | ________________ | ________________ | ________________ |
| 1-9 | ________________ | ________________ | ________________ | ________________ |
| 1-10 | ________________ | ________________ | ________________ | ________________ |
| 1-11 | ________________ | ________________ | ________________ | ________________ |
| 1-12 | ________________ | ________________ | ________________ | ________________ |
| 1-13 | ________________ | ________________ | ________________ | ________________ |
| 1-14 | ________________ | ________________ | ________________ | ________________ |
| 1-15 | ________________ | ________________ | ________________ | ________________ |
| 1-16 | ________________ | ________________ | ________________ | ________________ |
| 1-17 | ________________ | ________________ | ________________ | ________________ |
| 1-18 | ________________ | ________________ | ________________ | ________________ |
| 1-19 | ________________ | ________________ | ________________ | ________________ |
| 1-20 | ________________ | ________________ | ________________ | ________________ |
| 1-21 | ________________ | ________________ | ________________ | ________________ |
| 1-22 | ________________ | ________________ | ________________ | ________________ |
| 1-23 | ________________ | ________________ | ________________ | ________________ |
| 1-24 | ________________ | ________________ | ________________ | ________________ |
| 1-25 | ________________ | ________________ | ________________ | ________________ |
| 1-26 | ________________ | ________________ | ________________ | ________________ |
| 1-27 | ________________ | ________________ | ________________ | ________________ |
| 1-28 | ________________ | ________________ | ________________ | ________________ |
| 1-29 | ________________ | ________________ | ________________ | ________________ |
| 1-30 | ________________ | ________________ | ________________ | ________________ |

For each row above, fill the relevant detailed sections below. Field names and order follow the original worksheet columns.

## 会員基本情報

| Excel column | 入力項目 (original heading) | 値 / 入力 | Type / choice |
| --- | --- | --- | --- |
| D | 対象可否 | ________________ | Selection: 可,不可 |
| E | 入会申込日<br>(※プログラム型プロジェクトとして登録申請を行った日以降でなければ入会できない。)<br>（例 2025/4/5） | ________________ | Fill |
| F | 会員名 | ________________ | Fill |
| G | 設置場所住所 (必ず都道府県名から記載)<br>（例　東京都千代田区○○9-9-9） | ________________ | Fill |
| H | 【事業者の場合のみ記入】<br>カーボンニュートラル行動計画への参加有無 | ________________ | Selection: 参加していない,参加している |
| I | 【事業者の場合のみ記入】<br>省エネ法特定事業者の該当有無<br>（該当する場合は、省エネ法の報告時の留意点を会員に対して説明すること） | ________________ | Selection: 非該当,該当→説明済み |
| J | 【事業者の場合のみ記入】<br>温対法特定排出者の該当有無<br>（該当する場合は、温対法の報告時の留意点を会員に対して説明すること） | ________________ | Selection: 非該当,該当→説明済み |

## パワーコンディショナー設備情報

| Excel column | 入力項目 (original heading) | 値 / 入力 | Type / choice |
| --- | --- | --- | --- |
| K | メーカー | ________________ | Fill |
| L | 型番 | ________________ | Fill |
| M | 台数<br>（原則1） | 1 | Default/preset |
| N | 機器固有番号 <br>(シリアル番号等) | ________________ | Fill |
| O | パワーコンディショナー<br>夜間待機電力 (kW) | ________________ | Fill |
| P | 分類B（国際規格・業界標準等に基づく校正を実施）<br>or<br>分類C（誤差率を加味） | ________________ | Selection: B,C |
| Q | （分類Cの場合）<br>最大誤差率（%) | ________________ | Fill |

## 太陽光発電<br>設備情報

| Excel column | 入力項目 (original heading) | 値 / 入力 | Type / choice |
| --- | --- | --- | --- |
| R | 公称最大出力<br> (kW) | ________________ | Fill |

## ICT機器

| Excel column | 入力項目 (original heading) | 値 / 入力 | Type / choice |
| --- | --- | --- | --- |
| S | 消費電力<br> (kW)<br>※複数ある場合は、出力の合計 | ________________ | Fill |

## 追加的設備情報

> ※方法論適用条件１の「設置済みの太陽光発電設備に対して追加的な設備投資を実施」した場合のみ記入

| Excel column | 入力項目 (original heading) | 値 / 入力 | Type / choice |
| --- | --- | --- | --- |
| T | 追加的設備による登録か | ________________ | Selection: ○,× |
| U | 追加的設備が最初に稼働した日<br>（例 2025/4/5） | ________________ | Fill |
| V | 種類 | ________________ | Fill |
| W | 台数 | ________________ | Fill |
| X | 機器固有番号 <br>(シリアル番号車台番号等) | ________________ | Fill |

## 基準日 / Dates

| Excel column | 入力項目 (original heading) | 値 / 入力 | Type / choice |
| --- | --- | --- | --- |
| Y | 太陽光発電設備が最初に稼働した日<br>例 2025/4/5） | ________________ | Fill |
| Z | 温室効果ガス排出量の削減をもたらす活動が実質的に開始された日(太陽光発電設備または追加的設備が最初に稼働した日<br>例 2025/4/5 | ________________ | Fill |
| AA | ベースラインの<br>見直しによる<br>延長済みか | ________________ | Selection: ○,× |
| AB | 開始日<br>（入会申込日又はプロジェクトが実施された日のいずれか遅い日） | [AUTO] | Calculated |
| AC | 終了日（退会日）<br>（※途中で退会する場合は退会日を直接入力すること） | [AUTO] | Calculated |

## 追加性評価

| Excel column | 入力項目 (original heading) | 値 / 入力 | Type / choice |
| --- | --- | --- | --- |
| AD | 想定自家消費量(kWh/年） | ________________ | Fill |
| AE | 導入前1年間の<br>平均電力単価<br>（円/kWh） | ________________ | Fill |
| AF | 想定売電量<br>(kWh/年） | ________________ | Fill |
| AG | 売電単価<br>（円/kWh） | ________________ | Fill |
| AH | 総投資額（円）<br>※個別活動が追加的な設備投資で要件を満たした場合、当該追加的な設備の導入費用 | ________________ | Fill |
| AI | 補助金額（円） | ________________ | Fill |
| AJ | 投資回収年数（年） | [AUTO] | Calculated |
| AK | 投資額やランニングコストの確認書類 | ________________ | Fill |
| AL | 補助金名称 | ________________ | Fill |
| AM | 補助金<br>交付団体 | ________________ | Fill |

## プロジェクト登録要件

| Excel column | 入力項目 (original heading) | 値 / 入力 | Type / choice |
| --- | --- | --- | --- |
| AN | 日本国内で実施 | ________________ | Selection: ○,× |
| AO | 入会申し込み日の２年前の日以降に実施 | ________________ | Selection: ○,× |
| AP | 追加性を有する | ________________ | Selection: ○,× |
| AQ | 方法論の適用条件を満たしている | ________________ | Selection: ○,× |
| AR | 削減活動の二重登録が無い | ________________ | Selection: ○,× |
| AS | 他の削減活動と方法論、モニタリング項目が共通である | ________________ | Selection: ○,× |

## モニタリング（サンプリング対象か否か、モニタリング方法に応じて必要箇所のみ入力）

| Excel column | 入力項目 (original heading) | 値 / 入力 | Type / choice |
| --- | --- | --- | --- |
| AT | サンプリング対象の会員は"対象"を選択する。全数調査の場合は、すべて”対象”をご選択ください。 | ________________ | Selection: 対象,非対象 |
| AU | A：測定機器の電力量計が出力から先に設置されており、パワーコンディショナーにおける電力使用量を差し引いた発電電力量をモニタリングしている<br>B：測定機器の電力量計が出力より手前に設置されており、パワーコンディショナーにおける電力使用量が加味されていない発電電力量をモニタリングしている | ________________ | Selection: A,B |
| AV | 発電量 | ________________ | Monitoring method; Selection: モニター表示器,パワーコンディショナーの表示器,ICT機器による取得データ,電力会社発行の明細書 |
| AW | 売電量 | ________________ | Monitoring method; Selection: モニター表示器,パワーコンディショナーの表示器,ICT機器による取得データ,電力会社発行の明細書 |
| AX | 今回認証のモニタリング開始日<br>積算値データの取得日 | ________________ | Fill |
| AY | 電力会社発行の明細に記載された検針日 | ________________ | Fill |
| AZ | 発電量(kWh) | ________________ | Fill |
| BA | 売電量(kWh) | ________________ | Fill |
| BB | 今回認証のモニタリング終了日<br>積算値データの取得日 | ________________ | Fill |
| BC | 電力会社発行の明細に記載された検針日 | ________________ | Fill |
| BD | 発電量(kWh) | ________________ | Fill |
| BE | 売電量(kWh) | ________________ | Fill |
| BF | 発電量積算値<br>(kWh) | [AUTO] | Calculated |
| BG | 発電量原単位<br>(kWh/kW/日） | [AUTO] | Calculated |
| BH | 売電量積算値<br>(kWh) | [AUTO] | Calculated |
| BI | 売電量原単位<br>(kWh/kW/日） | [AUTO] | Calculated |

## Monitoring unit indicators / group aggregates

| 項目 | 計算結果 | Type |
| --- | --- | --- |
| 発電量原単位<br>(kWh/kW/日） | [AUTO] | Individual record |
| 売電量原単位<br>(kWh/kW/日） | [AUTO] | Individual record |
| 発電量原単位 (kWh/kW/日) | [AUTO] | Sample-group average |
| 売電量原単位 (kWh/kW/日) | [AUTO] | Sample-group average |

Workbook formulas, unit-calculation logic, and data validation remain in the original XLSX. Blank Markdown values are placeholders, not official data.

---

## Worksheet 07: `07_calculation_no_battery.md`

# 07. ＜YYYY年（M~M月）＞算定シート(蓄電池なし)

> Excel worksheet 7/10 (visible) from the uploaded EN-R-002 individual activity list. Period-specific calculation form. The original calculation rows are formula-linked to the matching member-information sheet.


## ■　基本情報

| 項目 | 値 / 入力 | 単位 |
| --- | --- | --- |
| 本シートの算定開始日 | ________________ |  |
| 本シートの算定終了日 | ________________ |  |
| 本シートの暦年 | [AUTO] | 年 |
| 本シートの年度 | [AUTO] | 年度 |
| 本シートで使用する排出係数の年度 | ________________ | 年度 |
| 系統電力排出係数（全電源） | ________________ | kg-CO2/ｋWh |
| 発電量原単位 | [AUTO] | kWh/kW/日 |
| 売電量原単位 | [AUTO] | kWh/kW/日 |

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
| I | 算定開始日 | [AUTO] |
| J | 算定終了日 | [AUTO] |
| K | 算定日数 | [AUTO] |
| L | 発電量 (kWh) | [AUTO] |
| M | 売電量 (kWh) | [AUTO] |
| N | パワコン排出量<br>※夜間<br>(kg-CO2) | [AUTO] |
| O | パワコン排出量<br>(kg-CO2) | [AUTO] |
| P | IoT端末の排出量<br>（kg-CO2) | [AUTO] |
| Q | プロジェクト実施後排出量<br>(kg-CO2) | [AUTO] |
| R | ベースライン排出量<br>(kg-CO2) | [AUTO] |
| S | 排出削減量<br>(kg-CO2) | [AUTO] |
| T | 再生可能エネルギー量<br>(MWh) | [AUTO] |

## Per-activity calculation register (30 rows)

| ID | 算定開始日 | 算定終了日 | 発電量 (kWh) | 売電量 (kWh) | 排出削減量<br>(kg-CO2) |
| --- | --- | --- | --- | --- | --- |
| 1-1 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 1-2 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 1-3 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 1-4 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 1-5 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 1-6 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 1-7 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 1-8 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 1-9 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 1-10 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 1-11 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 1-12 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 1-13 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 1-14 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 1-15 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 1-16 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 1-17 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 1-18 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 1-19 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 1-20 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 1-21 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 1-22 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 1-23 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 1-24 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 1-25 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 1-26 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 1-27 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 1-28 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 1-29 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |
| 1-30 | [AUTO] | [AUTO] | [AUTO] | [AUTO] | [AUTO] |

The original sheet has more computed columns than the compact register above; the exact full set appears in the field dictionary. Do not manually overwrite formula cells in Excel.

---

## Worksheet 08: `08_member_information_with_battery.md`

# 08. 会員情報(蓄電池あり)

> Excel worksheet 8/10 (visible) from the uploaded EN-R-002 individual activity list. One real record per activity/equipment serial number; 30 prepared IDs. Blank fields are fillable; formulas noted as AUTO.


## Sheet instructions and record rules

> 必要事項を記入して下さい。他プロジェクトとの重複登録有無の確認に使用します。<br>●対象可否<br>プロジェクト登録要件やその他事項を考慮して会員の対象可否を選択してください。<br>●ID<br>・個別活動実績報告リストの中で一意になるように附番をお願いいたします。（同一IDが複数シートに登場しないようにしてください。）<br>●設置場所の住所<br>・都道府県から正確な表記で記入をお願いします。<br>・丁目、番地は、半角のハイフンを使用してご記入ください。<br>●機器固有番号<br>・製品の表示、保証書等に記載されている通りにご記入ください。<br>●型番<br>・契約書、保証書等に記載されているとおりにご記入ください。

> 【注意事項】<br>・漢字、平仮名、片仮名は全角で記入をお願いいたします。<br>・数字、英字は半角で記入をお願いします。<br>・1会員あたり、機器固有番号が複数になる場合は、機器固有番号ごとに１行ずつお書きください。<br>・クレジット認証の際に、認証申請期間は全会員で統一いただくよう、お願いいたします。


## Activity records (30 original rows)

| ID | 対象可否 | 入会申込日 | 会員名 | 設置場所住所 |
| --- | --- | --- | --- | --- |
| 2-1 | ________________ | ________________ | ________________ | ________________ |
| 2-2 | ________________ | ________________ | ________________ | ________________ |
| 2-3 | ________________ | ________________ | ________________ | ________________ |
| 2-4 | ________________ | ________________ | ________________ | ________________ |
| 2-5 | ________________ | ________________ | ________________ | ________________ |
| 2-6 | ________________ | ________________ | ________________ | ________________ |
| 2-7 | ________________ | ________________ | ________________ | ________________ |
| 2-8 | ________________ | ________________ | ________________ | ________________ |
| 2-9 | ________________ | ________________ | ________________ | ________________ |
| 2-10 | ________________ | ________________ | ________________ | ________________ |
| 2-11 | ________________ | ________________ | ________________ | ________________ |
| 2-12 | ________________ | ________________ | ________________ | ________________ |
| 2-13 | ________________ | ________________ | ________________ | ________________ |
| 2-14 | ________________ | ________________ | ________________ | ________________ |
| 2-15 | ________________ | ________________ | ________________ | ________________ |
| 2-16 | ________________ | ________________ | ________________ | ________________ |
| 2-17 | ________________ | ________________ | ________________ | ________________ |
| 2-18 | ________________ | ________________ | ________________ | ________________ |
| 2-19 | ________________ | ________________ | ________________ | ________________ |
| 2-20 | ________________ | ________________ | ________________ | ________________ |
| 2-21 | ________________ | ________________ | ________________ | ________________ |
| 2-22 | ________________ | ________________ | ________________ | ________________ |
| 2-23 | ________________ | ________________ | ________________ | ________________ |
| 2-24 | ________________ | ________________ | ________________ | ________________ |
| 2-25 | ________________ | ________________ | ________________ | ________________ |
| 2-26 | ________________ | ________________ | ________________ | ________________ |
| 2-27 | ________________ | ________________ | ________________ | ________________ |
| 2-28 | ________________ | ________________ | ________________ | ________________ |
| 2-29 | ________________ | ________________ | ________________ | ________________ |
| 2-30 | ________________ | ________________ | ________________ | ________________ |

For each row above, fill the relevant detailed sections below. Field names and order follow the original worksheet columns.

## 会員基本情報

| Excel column | 入力項目 (original heading) | 値 / 入力 | Type / choice |
| --- | --- | --- | --- |
| D | 対象可否 | ________________ | Selection: 可,不可 |
| E | 入会申込日<br>(※プログラム型プロジェクトとして登録申請を行った日以降でなければ入会できない。)<br>（例 2025/4/5） | ________________ | Fill |
| F | 会員名 | ________________ | Fill |
| G | 設置場所住所 (必ず都道府県名から記載)<br>（例　東京都千代田区○○9-9-9） | ________________ | Fill |
| H | 【事業者の場合のみ記入】<br>カーボンニュートラル行動計画への参加有無 | ________________ | Selection: 参加していない,参加している |
| I | 【事業者の場合のみ記入】<br>省エネ法特定事業者の該当有無<br>（該当する場合は、省エネ法の報告時の留意点を会員に対して説明すること） | ________________ | Selection: 非該当,該当→説明済み |
| J | 【事業者の場合のみ記入】<br>温対法特定排出者の該当有無<br>（該当する場合は、温対法の報告時の留意点を会員に対して説明すること） | ________________ | Selection: 非該当,該当→説明済み |

## パワーコンディショナー設備情報

| Excel column | 入力項目 (original heading) | 値 / 入力 | Type / choice |
| --- | --- | --- | --- |
| K | メーカー | ________________ | Fill |
| L | 型番 | ________________ | Fill |
| M | 台数<br>（原則1） | 1 | Default/preset |
| N | 機器固有番号 <br>(シリアル番号等) | ________________ | Fill |
| O | パワーコンディショナー<br>夜間待機電力 (kW) | ________________ | Fill |
| P | 分類B（国際規格・業界標準等に基づく校正を実施）<br>or<br>分類C（誤差率を加味） | ________________ | Selection: B,C |
| Q | （分類Cの場合）<br>最大誤差率（%) | ________________ | Fill |

## 太陽光発電設備

| Excel column | 入力項目 (original heading) | 値 / 入力 | Type / choice |
| --- | --- | --- | --- |
| R | 公称最大出力<br> (kW) | ________________ | Fill |

## 蓄電池

| Excel column | 入力項目 (original heading) | 値 / 入力 | Type / choice |
| --- | --- | --- | --- |
| S | 蓄電池容量<br>（ｋWh) | ________________ | Fill |
| T | 充電効率<br>(%) | ________________ | Fill |
| U | 放電効率<br>(%) | ________________ | Fill |

## ICT機器

| Excel column | 入力項目 (original heading) | 値 / 入力 | Type / choice |
| --- | --- | --- | --- |
| V | 消費電力<br> (kW)<br>※複数ある場合は、出力の合計 | ________________ | Fill |

## 追加的設備情報

> ※方法論適用条件１の「設置済みの太陽光発電設備に対して追加的な設備投資を実施」した場合のみ記入

| Excel column | 入力項目 (original heading) | 値 / 入力 | Type / choice |
| --- | --- | --- | --- |
| W | 追加的設備による登録か | ________________ | Selection: ○,× |
| X | 追加的設備が最初に稼働した日<br>（例 2025/4/5） | ________________ | Fill |
| Y | 種類 | ________________ | Fill |
| Z | 台数 | ________________ | Fill |
| AA | 機器固有番号 <br>(シリアル番号車台番号等) | ________________ | Fill |

## 基準日 / Dates

| Excel column | 入力項目 (original heading) | 値 / 入力 | Type / choice |
| --- | --- | --- | --- |
| AB | 太陽光発電設備が最初に稼働した日<br>例 2025/4/5） | ________________ | Fill |
| AC | 温室効果ガス排出量の削減をもたらす活動が実質的に開始された日(太陽光発電設備または追加的設備が最初に稼働した日<br>例 2025/4/5 | ________________ | Fill |
| AD | ベースラインの<br>見直しによる<br>延長済みか | ________________ | Selection: ○,× |
| AE | 開始日<br>（入会申込日又はプロジェクトが実施された日のいずれか遅い日） | [AUTO] | Calculated |
| AF | 終了日（退会日）<br>（※途中で退会する場合は退会日を直接入力すること） | [AUTO] | Calculated |

## 追加性評価

| Excel column | 入力項目 (original heading) | 値 / 入力 | Type / choice |
| --- | --- | --- | --- |
| AG | 想定自家消費量(kWh/年） | ________________ | Fill |
| AH | 導入前1年間の<br>平均電力単価<br>（円/kWh） | ________________ | Fill |
| AI | 想定売電量<br>(kWh/年） | ________________ | Fill |
| AJ | 売電単価<br>（円/kWh） | ________________ | Fill |
| AK | 総投資額（円）<br>※個別活動が追加的な設備投資で要件を満たした場合、当該追加的な設備の導入費用 | ________________ | Fill |
| AL | 補助金額（円） | ________________ | Fill |
| AM | 投資回収年数（年） | [AUTO] | Calculated |
| AN | 投資額やランニングコストの確認書類 | ________________ | Fill |
| AO | 補助金名称 | ________________ | Fill |
| AP | 補助金<br>交付団体 | ________________ | Fill |

## プロジェクト登録要件

| Excel column | 入力項目 (original heading) | 値 / 入力 | Type / choice |
| --- | --- | --- | --- |
| AQ | 日本国内で実施 | ________________ | Selection: ○,× |
| AR | 入会申し込み日の２年前の日以降に実施 | ________________ | Selection: ○,× |
| AS | 追加性を有する | ________________ | Selection: ○,× |
| AT | 方法論の適用条件を満たしている | ________________ | Selection: ○,× |
| AU | 削減活動の二重登録が無い | ________________ | Selection: ○,× |
| AV | 他の削減活動と方法論、モニタリング項目が共通である | ________________ | Selection: ○,× |

## モニタリング（サンプリング対象か否か、モニタリング方法に応じて必要箇所のみ入力）

| Excel column | 入力項目 (original heading) | 値 / 入力 | Type / choice |
| --- | --- | --- | --- |
| AW | サンプリング対象の会員は"対象"を選択する。全数調査の場合は、すべて”対象”をご選択ください。 | ________________ | Selection: 対象,非対象 |
| AX | A：測定機器の電力量計が出力から先に設置されており、パワーコンディショナーにおける電力使用量を差し引いた発電電力量をモニタリングしている<br>B：測定機器の電力量計が出力より手前に設置されており、パワーコンディショナーにおける電力使用量が加味されていない発電電力量をモニタリングしている | ________________ | Selection: A,B |
| AY | 発電量 | ________________ | Monitoring method; Selection: モニター表示器,パワーコンディショナーの表示器,ICT機器による取得データ,電力会社発行の明細書 |
| AZ | 売電量 | ________________ | Monitoring method; Selection: モニター表示器,パワーコンディショナーの表示器,ICT機器による取得データ,電力会社発行の明細書 |
| BA | 今回認証のモニタリング開始日<br>積算値データの取得日 | ________________ | Fill |
| BB | 電力会社発行の明細に記載された検針日 | ________________ | Fill |
| BC | 発電量(kWh) | ________________ | Fill |
| BD | 売電量(kWh) | ________________ | Fill |
| BE | 今回認証のモニタリング終了日<br>積算値データの取得日 | ________________ | Fill |
| BF | 電力会社発行の明細に記載された検針日 | ________________ | Fill |
| BG | 発電量(kWh) | ________________ | Fill |
| BH | 売電量(kWh) | ________________ | Fill |
| BI | 発電量積算値<br>(kWh) | [AUTO] | Calculated |
| BJ | 発電量原単位<br>(kWh/kW/日） | [AUTO] | Calculated |
| BK | 売電量積算値<br>(kWh) | [AUTO] | Calculated |
| BL | 売電量原単位<br>(kWh/kW/日） | [AUTO] | Calculated |

## Monitoring unit indicators / group aggregates

| 項目 | 計算結果 | Type |
| --- | --- | --- |
| 発電量原単位<br>(kWh/kW/日） | [AUTO] | Individual record |
| 売電量原単位<br>(kWh/kW/日） | [AUTO] | Individual record |
| 発電量原単位 (kWh/kW/日) | [AUTO] | Sample-group average |
| 売電量原単位 (kWh/kW/日) | [AUTO] | Sample-group average |

Workbook formulas, unit-calculation logic, and data validation remain in the original XLSX. Blank Markdown values are placeholders, not official data.

---

## Worksheet 09: `09_calculation_with_battery.md`

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

---

## Worksheet 10: `10_sampling_reference.md`

# 10. ＜参考＞サンプル数

> Excel worksheet 10/10 (visible) from the uploaded EN-R-002 individual activity list. Reference and sample-size calculation: inputs, fixed constants, and resulting lookup table.


## ■サンプリングにおける必要サンプル数

| 項目 | 値 / 入力 |
| --- | --- |
| n: 必要サンプル数 | ________________ |
| N: 全母集団数 | ________________ |
| k: 正規分布の棄却限界値 (90%信頼区間を想定し、1.65) | 1.65 |
| CI: 標本誤差 (90%信頼区間で標本誤差±10%を想定し、0.2) | 0.2 |
| s: 母集団の分散の推定値 (分布を0~1の範囲に規格化すると、s=0.5のとき二項分布は最大値をとる。) | 0.5 |

## Population / computed sample count

| 会員数<br>（母数） | 必要サンプル数 | 割合 |
| --- | --- | --- |
| ________________ | [AUTO] | [AUTO] |

## Sample-size reference table

| 母数 | 必要サンプル数 | 割合 |
| --- | --- | --- |
| 1 | [AUTO] | [AUTO] |
| 5 | [AUTO] | [AUTO] |
| 10 | [AUTO] | [AUTO] |
| 15 | [AUTO] | [AUTO] |
| 20 | [AUTO] | [AUTO] |
| 25 | [AUTO] | [AUTO] |
| 30 | [AUTO] | [AUTO] |
| 35 | [AUTO] | [AUTO] |
| 40 | [AUTO] | [AUTO] |
| 45 | [AUTO] | [AUTO] |
| 50 | [AUTO] | [AUTO] |
| 55 | [AUTO] | [AUTO] |
| 60 | [AUTO] | [AUTO] |
| 65 | [AUTO] | [AUTO] |
| 70 | [AUTO] | [AUTO] |
| 75 | [AUTO] | [AUTO] |
| 80 | [AUTO] | [AUTO] |
| 85 | [AUTO] | [AUTO] |
| 90 | [AUTO] | [AUTO] |
| 95 | [AUTO] | [AUTO] |
| 100 | [AUTO] | [AUTO] |
| 150 | [AUTO] | [AUTO] |
| 200 | [AUTO] | [AUTO] |
| 250 | [AUTO] | [AUTO] |
| 300 | [AUTO] | [AUTO] |
| 350 | [AUTO] | [AUTO] |
| 400 | [AUTO] | [AUTO] |
| 450 | [AUTO] | [AUTO] |
| 500 | [AUTO] | [AUTO] |
| 1000 | [AUTO] | [AUTO] |
| 1500 | [AUTO] | [AUTO] |
| 2000 | [AUTO] | [AUTO] |
| 2500 | [AUTO] | [AUTO] |
| 3000 | [AUTO] | [AUTO] |
| 3500 | [AUTO] | [AUTO] |
| 4000 | [AUTO] | [AUTO] |
| 4500 | [AUTO] | [AUTO] |
| 5000 | [AUTO] | [AUTO] |
| 6000 | [AUTO] | [AUTO] |
| 7000 | [AUTO] | [AUTO] |
| 8000 | [AUTO] | [AUTO] |
| 9000 | [AUTO] | [AUTO] |
| 10000 | [AUTO] | [AUTO] |
| 100000 | [AUTO] | [AUTO] |
| 1000000 | [AUTO] | [AUTO] |

Values marked AUTO are calculated by source workbook formulas. This reference sheet is not an individual activity input record.
