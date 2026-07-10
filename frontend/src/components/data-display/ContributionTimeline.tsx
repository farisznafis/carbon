import React, { useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { Calendar } from 'lucide-react';
import { cn } from '@/lib/utils';

interface ContributionData {
  timestamp: string;
  projectTitle: string;
  contributions: number;
  impactMetric: string;
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

  const defaultData: ContributionData[] = [
    {
      timestamp: '2025-03-18',
      projectTitle: '太陽光発電設備の導入',
      contributions: 0,
      impactMetric: 'Saved 12.4kg CO2',
    },
    {
      timestamp: '2025-03-19',
      projectTitle: '太陽光発電設備の導入',
      contributions: 3,
      impactMetric: '91.8% Accuracy',
    },
    {
      timestamp: '2025-03-20',
      projectTitle: '太陽光発電設備の導入',
      contributions: 5,
      impactMetric: 'Saved $18.50',
    },
    {
      timestamp: '2025-03-21',
      projectTitle: '太陽光発電設備の導入',
      contributions: 10,
      impactMetric: 'PUE reduced to 1.18',
    },
    {
      timestamp: '2025-03-22',
      projectTitle: '太陽光発電設備の導入',
      contributions: 9,
      impactMetric: 'Seeded 120 labels',
    },
    {
      timestamp: '2025-03-23',
      projectTitle: '太陽光発電設備の導入',
      contributions: 13,
      impactMetric: 'Saved 24.8kg CO2',
    },
    {
      timestamp: '2025-03-24',
      projectTitle: '太陽光発電設備の導入',
      contributions: 14,
      impactMetric: 'Saved $45.20',
    },
  ];

  const timelineData = data && data.length > 0 ? data : defaultData;

  const width = 600;
  const height = 180;
  const paddingX = 50;
  const paddingY = 30;
  const chartWidth = width - paddingX * 2;
  const chartHeight = height - paddingY * 2;
  const maxVal = Math.max(...timelineData.map((d) => d.contributions), 10);

  const getX = (index: number) => {
    if (timelineData.length <= 1) return paddingX + chartWidth / 2;
    return paddingX + (index / (timelineData.length - 1)) * chartWidth;
  };

  const getY = (value: number) => {
    return paddingY + chartHeight - (value / maxVal) * chartHeight;
  };

  const linePath = timelineData
    .map((d, index) => `${getX(index)},${getY(d.contributions)}`)
    .join(' L ');
  const fullPath = `M ${linePath}`;

  const formatDateLabel = (value: string) => {
    const first = String(value || '').split(' ')[0];

    if (first.includes('-')) {
      const parts = first.split('-');
      if (parts.length >= 3) return `${parts[1]}/${parts[2]}`;
    }

    const match = first.match(/(\d{4})\D?(\d{1,2})\D?(\d{1,2})/);
    if (match) {
      return `${match[2].padStart(2, '0')}/${match[3].padStart(2, '0')}`;
    }

    return first;
  };

  const tooltipLeft =
    hoveredIdx === null || timelineData.length <= 1
      ? '2%'
      : `${Math.min(Math.max((hoveredIdx / (timelineData.length - 1)) * 80, 2), 65)}%`;

  return (
    <div
      className={cn(
        'group relative flex h-full flex-col overflow-hidden rounded-2xl border border-border bg-card p-5 shadow-app-sm',
        className,
      )}
    >
      <div className='pointer-events-none absolute right-0 top-0 h-32 w-32 rounded-full bg-primary/5 blur-3xl' />

      <div className='mb-4'>
        <div className='space-y-1'>
          <h3 className='leading-8 text-foreground'>
            貢献のグラフ
          </h3>
          <p className='text-subtle text-muted-foreground'>
            日次のコミット数とモデル実行回数を記録しています。
          </p>
        </div>
      </div>

      <div
        className='relative w-full flex-1 overflow-hidden'
        style={{ height: `${height}px` }}
      >
        <svg
          viewBox={`0 0 ${width} ${height}`}
          className='h-full w-full overflow-visible'
        >
          <line
            x1={paddingX}
            y1={height - paddingY}
            x2={width - paddingX}
            y2={height - paddingY}
            stroke='hsl(var(--foreground))'
            strokeWidth='0.75'
          />
          <line
            x1={paddingX}
            y1={paddingY}
            x2={paddingX}
            y2={height - paddingY}
            stroke='hsl(var(--foreground))'
            strokeWidth='0.75'
          />

          <motion.path
            initial={{ pathLength: 0 }}
            animate={{ pathLength: 1 }}
            transition={{ duration: 0.8, ease: 'easeOut' }}
            d={fullPath}
            fill='none'
            stroke='hsl(var(--primary))'
            strokeWidth='1.5'
            strokeLinecap='round'
            strokeLinejoin='round'
          />

          {timelineData.map((item, index) => {
            const cx = getX(index);
            const cy = getY(item.contributions);
            const isHovered = hoveredIdx === index;

            return (
              <g key={`${item.timestamp}-${index}`}>
                <circle
                  cx={cx}
                  cy={cy}
                  r='14'
                  fill='transparent'
                  className='cursor-pointer'
                  onMouseEnter={() => setHoveredIndex(index)}
                  onMouseLeave={() => setHoveredIndex(null)}
                />
                <circle
                  cx={cx}
                  cy={cy}
                  r={isHovered ? '7' : '5'}
                  fill='hsl(var(--card))'
                  stroke='hsl(var(--primary))'
                  strokeWidth='2'
                  className='pointer-events-none drop-shadow-[0_2px_4px_hsl(var(--primary)/0.3)] transition-all duration-200'
                />
                {isHovered && (
                  <circle
                    cx={cx}
                    cy={cy}
                    r='11'
                    fill='none'
                    stroke='hsl(var(--primary))'
                    strokeWidth='1'
                    className='pointer-events-none animate-ping opacity-60'
                  />
                )}
              </g>
            );
          })}

          {timelineData.map((item, index) => (
            <text
              key={`${item.timestamp}-label-${index}`}
              x={getX(index)}
              y={height - paddingY + 14}
              fill='hsl(var(--primary))'
              fontSize='10'
              fontFamily='monospace'
              textAnchor='middle'
              className='font-semibold opacity-80'
            >
              {formatDateLabel(item.timestamp)}
            </text>
          ))}

          <text
            x={paddingX - 10}
            y={paddingY + 4}
            fill='hsl(var(--primary))'
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
            fill='hsl(var(--primary))'
            fontSize='8'
            fontFamily='monospace'
            textAnchor='end'
            className='opacity-60'
          >
            0
          </text>
        </svg>

        <AnimatePresence>
          {hoveredIdx !== null && (
            <motion.div
              initial={{ opacity: 0, y: 10, scale: 0.95 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: 10, scale: 0.95 }}
              className='pointer-events-none absolute z-30 w-56 rounded-xl border border-border bg-card p-3.5 text-left shadow-app-md'
              style={{
                left: tooltipLeft,
                top: '5%',
              }}
            >
              <div className='text-subtle mb-1.5 flex items-center gap-1.5 border-b border-border pb-1.5 font-bold text-primary'>
                <Calendar className='h-3 w-3' />
                <span>{timelineData[hoveredIdx].timestamp}</span>
              </div>

              <span className='text-table block font-bold leading-tight text-foreground'>
                {timelineData[hoveredIdx].projectTitle}
              </span>

              <div className='text-subtle mt-2 space-y-1'>
                <div className='flex justify-between text-muted-foreground'>
                  <span>貢献数</span>
                  <div className='flex items-center gap-1'>
                    <span className='font-mono font-bold text-primary'>
                      {timelineData[hoveredIdx].contributions}
                    </span>
                    <span>件</span>
                  </div>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}
