import { cn } from '@/lib/utils';

type HomeSectionHeadingProps = Readonly<{
  id?: string;
  title: string;
  description?: string;
  align?: 'left' | 'center';
  className?: string;
}>;

/** Renders consistent marketing-section typography across the public homepage. */
export function HomeSectionHeading({
  id,
  title,
  description,
  align = 'center',
  className,
}: HomeSectionHeadingProps) {
  return (
    <div
      className={cn(
        'space-y-3',
        align === 'center' ? 'text-center' : 'text-left',
        className,
      )}
    >
      <h2
        id={id}
        className="text-2xl font-bold leading-tight tracking-tight text-foreground sm:text-home-section-title"
      >
        {title}
      </h2>
      {description ? (
        <p className="mx-auto max-w-4xl text-base leading-7 text-muted-foreground sm:text-lg">
          {description}
        </p>
      ) : null}
    </div>
  );
}
