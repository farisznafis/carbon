import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Calendar, GitCommit, HelpCircle, Leaf, Sparkles } from 'lucide-react';
import { cn } from '@/lib/utils';

interface ContributionData {
  timestamp: string;
  projectTitle: string;
  contributions: number; // code contributions or model iterations
  impactMetric: string; // e.g., "Saved 4.5kg CO2", "PUE 1.12"
}

interface ContributionTimelineProps {
  data?: ContributionData[];
  className?: string;
}

export default function ContributionTimeline({
  data,
  className,
}: ContributionTimelineProps) {
  const [hoveredIdx, setHoveredIndex] = useState<number | null>(null);

  // Fallback high-end seeded timeline data representing daily/weekly model deployment velocity and code commits
  const defaultData: ContributionData[] = [
    {
      timestamp: '2025年03月18日',
      projectTitle: 'タイトル１',
      // projectTitle: 'Eco-Model Optimizer',
      contributions: 0,
      impactMetric: 'Saved 12.4kg CO2',
    },
    {
      timestamp: '2025年03月19日',
      projectTitle: 'タイトル２',
      // projectTitle: 'Forestry AI Canopy',
      contributions: 3,
      impactMetric: '91.8% Accuracy',
    },
    {
      timestamp: '2025年03月20日',
      projectTitle: 'タイトル３',
      // projectTitle: 'Solar Grid Arbitrage',
      contributions: 5,
      impactMetric: 'Saved $18.50',
    },
    {
      timestamp: '2025年03月21日',
      projectTitle: 'タイトル４',
      // projectTitle: 'Green Server Cooling',
      contributions: 10,
      impactMetric: 'PUE reduced to 1.18',
    },
    {
      timestamp: '2025年03月22日',
      projectTitle: 'タイトル５',
      // projectTitle: 'Ocean Plastic Classifier',
      contributions: 9,
      impactMetric: 'Seeded 120 labels',
    },
    {
      timestamp: '2025年03月23日',
      projectTitle: 'タイトル６',
      // projectTitle: 'Eco-Model Optimizer',
      contributions: 13,
      impactMetric: 'Saved 24.8kg CO2',
    },
    {
      timestamp: '2025年03月24日',
      projectTitle: 'タイトル１',
      // projectTitle: 'Green Server Cooling',
      contributions: 14,
      impactMetric: 'Saved $45.20',
    },
  ];

  const timelineData = data && data.length > 0 ? data : defaultData;

  // SVG dimensions
  const width = 600;
  const height = 180;
  const paddingX = 50;
  const paddingY = 30;

  const chartWidth = width - paddingX * 2;
  const chartHeight = height - paddingY * 2;

  // Calculate scales
  const maxVal = Math.max(...timelineData.map((d) => d.contributions), 10);
  const minVal = 0;
  const range = maxVal - minVal;

  const getX = (index: number) => {
    if (timelineData.length <= 1) return paddingX + chartWidth / 2;
    return paddingX + (index / (timelineData.length - 1)) * chartWidth;
  };

  const getY = (val: number) => {
    return paddingY + chartHeight - (val / maxVal) * chartHeight;
  };

  // Generate linear connection path (straight line segments)
  const linePath = timelineData
    .map((d, i) => `${getX(i)},${getY(d.contributions)}`)
    .join(' L ');
  const fullPath = `M ${linePath}`;

  return (
    <div
      className={cn(
        'group relative flex h-full flex-col overflow-hidden rounded-2xl border border-emerald-200 bg-white p-5 shadow-sm',
        className,
      )}
    >
      {/* Subtle Background Glow */}
      <div className='absolute top-0 right-0 w-32 h-32 bg-emerald-500/5 rounded-full blur-3xl pointer-events-none' />

      {/* Header */}
      <div className='flex justify-between items-center mb-4'>
        <div>
          <h3 className='text-xs font-bold text-emerald-900 tracking-wide uppercase flex items-center gap-2'>
            <span className='w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse' />
            {/* Sustainable Contribution Velocity & Model Iterations */}
              貢献のグラフ
             (Contribution Graph)
          </h3>
          <span className='text-[10px] text-emerald-700 block mt-0.5'>
            {/* Tracking daily software commits & optimization model runs in harmony. */}
            日次のコミット数とモデル実行回数を記録しています。
          </span>
        </div>

        {/* <div className="flex items-center gap-2 text-[10px] text-emerald-700 bg-emerald-50 border border-emerald-200 px-2.5 py-1 rounded-xl">
          <GitCommit className="w-3.5 h-3.5" />
          <span className="font-bold">Straight Linear Flow</span>
        </div> */}
      </div>

      {/* SVG Canvas */}
      <div
        className='relative w-full flex-1 overflow-hidden'
        style={{ height: `${height}px` }}
      >
        <svg
          viewBox={`0 0 ${width} ${height}`}
          className='w-full h-full overflow-visible'
        >
          {/* Subtle nature axes lines (No grid lines as per spec) */}
          {/* X Axis */}
          <line
            x1={paddingX}
            y1={height - paddingY}
            x2={width - paddingX}
            y2={height - paddingY}
            stroke='#000000' // axis lines
            strokeWidth='0.75'
            // className="opacity-40"
          />
          {/* Y Axis */}
          <line
            x1={paddingX}
            y1={paddingY}
            x2={paddingX}
            y2={height - paddingY}
            stroke='#000000' // axis lines
            strokeWidth='0.75'
            // className="opacity-40"
          />

          {/* Straight Linear Connections (Straight path segments, no monotone curves) */}
          <motion.path
            initial={{ pathLength: 0 }}
            animate={{ pathLength: 1 }}
            transition={{ duration: 0.8, ease: 'easeOut' }}
            d={fullPath}
            fill='none'
            stroke='#22c55e' // line path
            strokeWidth='1.5'
            strokeLinecap='round'
            strokeLinejoin='round'
          />

          {/* Nodes/Data Points (Heavily emphasized circular bullets along the line) */}
          {timelineData.map((d, i) => {
            const cx = getX(i);
            const cy = getY(d.contributions);
            const isHovered = hoveredIdx === i;

            return (
              <g key={i}>
                {/* Interactive Hotspot */}
                <circle
                  cx={cx}
                  cy={cy}
                  r='14'
                  fill='transparent'
                  className='cursor-pointer'
                  onMouseEnter={() => setHoveredIndex(i)}
                  onMouseLeave={() => setHoveredIndex(null)}
                />

                {/* Soft nature-themed circular node (bullet points along the line) */}
                {/* Soft cream / light mint solid fill with clean outline */}
                <circle
                  cx={cx}
                  cy={cy}
                  r={isHovered ? '7' : '5'}
                  fill='#ecfdf5' // light mint / soft cream
                  stroke='#10b981' // clean emerald outline
                  strokeWidth='2'
                  className='transition-all duration-200 pointer-events-none drop-shadow-[0_2px_4px_rgba(16,185,129,0.3)]'
                />

                {/* Subtle outer pulse effect for hovered nodes */}
                {isHovered && (
                  <circle
                    cx={cx}
                    cy={cy}
                    r='11'
                    fill='none'
                    stroke='#10b981'
                    strokeWidth='1'
                    className='animate-ping opacity-60 pointer-events-none'
                  />
                )}
              </g>
            );
          })}

          {/* X Axis Date/Time Labels */}
          {timelineData.map((d, i) => {
            const dateStr = (() => {
              const raw = String(d.timestamp || '');
              const first = raw.split(' ')[0];

              // ISO-like YYYY-MM-DD -> MM/DD
              if (first.includes('-')) {
                const parts = first.split('-');
                if (parts.length >= 3) return `${parts[1]}/${parts[2]}`;
              }

              // Attempt to extract numbers from formats like 2025年03月18日
              const m = first.match(/(\d{4})\D?(\d{1,2})\D?(\d{1,2})/);
              if (m) return `${m[2].padStart(2, '0')}/${m[3].padStart(2, '0')}`;

              // Fallback: show whole string
              return first;
            })();

            return (
              <text
                key={i}
                x={getX(i)}
                y={height - paddingY + 14}        // move a bit below the axis if needed
                fill='#047857'
                fontSize='10'                     // increased from 8 -> 10
                fontFamily='monospace'
                textAnchor='middle'
                className='opacity-80 font-semibold'
              >
                {dateStr}
              </text>
            );
          })}

          {/* Y Axis Max/Min Limits */}
          <text
            x={paddingX - 10}
            y={paddingY + 4}
            fill='#047857'
            fontSize='8'
            fontFamily='monospace'
            textAnchor='end'
            className='opacity-60'
          >
            {maxVal}
          </text>
          <text
            x={paddingX - 10}
            y={height - paddingY + 2}
            fill='#047857'
            fontSize='8'
            fontFamily='monospace'
            textAnchor='end'
            className='opacity-60'
          >
            0
          </text>
        </svg>

        {/* Custom Interactive Tooltip Overlay */}
        <AnimatePresence>
          {hoveredIdx !== null && (
            <motion.div
              initial={{ opacity: 0, y: 10, scale: 0.95 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: 10, scale: 0.95 }}
              className='absolute bg-white border border-emerald-200 rounded-xl p-3.5 shadow-lg z-30 pointer-events-none text-left w-56'
              style={{
                left: `${Math.min(Math.max((hoveredIdx / (timelineData.length - 1)) * 80, 2), 65)}%`,
                top: '5%',
              }}
            >
              <div className='flex items-center gap-1.5 text-[9px] font-bold text-emerald-700 uppercase tracking-wider border-b border-emerald-950/40 pb-1.5 mb-1.5'>
                <Calendar className='w-3 h-3' />
                <span>{timelineData[hoveredIdx].timestamp}</span>
              </div>

              <span className='text-[11px] font-bold text-emerald-900 block leading-tight'>
                {timelineData[hoveredIdx].projectTitle}
              </span>

              <div className='mt-2 space-y-1 text-[10px]'>
                <div className='flex justify-between text-slate-400'>
                  <span>Contributions:</span>
                  <span className='text-emerald-400 font-mono font-bold'>
                    {timelineData[hoveredIdx].contributions}
                  </span>
                </div>
                {/* <div className='flex justify-between text-slate-400'>
                  <span>Eco-Impact:</span>
                  <span className='text-cyan-400 font-semibold'>
                    {timelineData[hoveredIdx].impactMetric}
                  </span>
                </div> */}
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}
