/**
 * FakeGraph
 *
 * シンプルなアクセシブル棒グラフコンポーネント。
 * 与えられたデータ配列をもとに、相対的な高さのバーを描画します。
 *
 * 特徴:
 * - value の最大値を基準に、各バーの高さを相対比で計算
 * - ホバー / フォーカスでツールチップ表示
 * - 左右矢印キーでバーを移動可能（キーボード操作対応）
 *
 * 型:
 * export type FakeGraphBar = {
 *   label: string;        // バーのラベル（X 軸）
 *   value: number;        // 値（高さ計算に利用）
 *   color: string;        // バーの色（CSS color 値）
 *   description?: string; // 任意: ツールチップに表示する補足説明
 * };
 *
 * Props:
 * - data: FakeGraphBar[]
 *     描画するバーの配列。
 *
 * - ariaLabel?: string
 *     スクリーンリーダー用のグラフ全体ラベル。
 *     省略時は "Bar chart"。
 *
 * - height?: number
 *     グラフ描画領域の高さ（px）。バーはこの高さを基準に比率で描画。
 *     デフォルト: 240。
 *
 * 使用例:
 * const data: FakeGraphBar[] = [
 *   { label: "Jan", value: 10, color: "#4F46E5", description: "10 users" },
 *   { label: "Feb", value: 25, color: "#EC4899", description: "25 users" },
 *   { label: "Mar", value: 5,  color: "#10B981", description: "5 users" },
 * ];
 *
 * <FakeGraph
 *   data={data}
 *   ariaLabel="Monthly active users"
 *   height={200}
 * />
 */

import React, { useState, KeyboardEvent } from "react";

export type FakeGraphBar = {
  label: string;
  value: number;
  color: string;
  description?: string;
};

type FakeGraphProps = {
  data: FakeGraphBar[];
  ariaLabel?: string;
  height?: number; // px
};

export function FakeGraph({
  data,
  ariaLabel = "Bar chart",
  height = 240,
}: FakeGraphProps) {
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);

  const numericValues = data.map((d) => Number(d.value) || 0);
  const maxValue = Math.max(...numericValues, 1);

  const handleKeyDown =
    (index: number) =>
    (event: KeyboardEvent<HTMLButtonElement>): void => {
      if (event.key === "ArrowRight") {
        event.preventDefault();
        setHoveredIndex((index + 1) % data.length);
      } else if (event.key === "ArrowLeft") {
        event.preventDefault();
        setHoveredIndex((index - 1 + data.length) % data.length);
      }
    };

  return (
    <div aria-label={ariaLabel} role="group" style={{ width: "100%" }}>
      {/* ★ ここで height を必ず px にする */}
      <div
        style={{
          display: "flex",
          alignItems: "flex-end",
          gap: 12,
          height: `${height}px`,
          position: "relative",
          
        }}
      >
        {data.map((bar, index) => {
          const value = Number(bar.value) || 0;
          const heightPercent = (value / maxValue) * 100;
          const isActive = hoveredIndex === index;

          return (
            <div
              key={bar.label}
              style={{
                flex: 1,
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                position: "relative",
              }}
            >
              {isActive && (
                <div
                  role="dialog"
                  aria-label={`${bar.label} details`}
                  style={{
                    position: "absolute",
                    bottom: "100%",
                    marginBottom: 8,
                    padding: "6px 10px",
                    background: "white",
                    borderRadius: 6,
                    boxShadow: "0 4px 10px rgba(0,0,0,0.15)",
                    fontSize: 12,
                    zIndex: 10,
                    whiteSpace: "nowrap",
                  }}
                >
                  <div>
                    <strong>{bar.label}</strong>
                  </div>
                  <div>{value}</div>
                  {bar.description && (
                    <div style={{ opacity: 0.8 }}>{bar.description}</div>
                  )}
                </div>
              )}

              {/* ★ バー自体も px で高さを指定 */}
              <button
                type="button"
                style={{
                  width: "100%",
                  height: `${(height * heightPercent) / 100}px`,
                  backgroundColor: bar.color,
                  border: "none",
                  borderRadius: 4,
                  cursor: "pointer",
                  outlineOffset: 2,
                  minHeight: 12,
                }}
                aria-label={`${bar.label}, value ${value}`}
                onMouseEnter={() => setHoveredIndex(index)}
                onMouseLeave={() => setHoveredIndex(null)}
                onFocus={() => setHoveredIndex(index)}
                onBlur={() => setHoveredIndex(null)}
                onKeyDown={handleKeyDown(index)}
              />

              <div
                data-testid={`bar-label-${bar.label}`}
                style={{
                  marginTop: 6,
                  fontSize: 12,
                  textAlign: "center",
                  wordBreak: "keep-all",
                }}
              >
                {bar.label}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}