import { useEffect, useId, useRef, useState } from 'react';
import { AlertCircle } from 'lucide-react';
import { cn } from '@/lib/utils';

type ErrorSummaryItem = {
  section: string;
  label: string;
  message: string;
  /** Id of the element to focus when the item is pressed. */
  targetId?: string;
};

const FOCUSABLE =
  'input:not([disabled]):not([type="hidden"]), textarea:not([disabled]), select:not([disabled]), button:not([disabled]), [tabindex]:not([tabindex="-1"])';

let generatedTargetId = 0;

/** RequiredLabel / RequiredBadge append a "*"; it is not part of the name. */
function cleanText(text: string | null | undefined) {
  return (text ?? '').replace(/\*\s*$/, '').trim();
}

/** The control a field error belongs to, made addressable by id. */
function resolveTarget(error: HTMLElement, controlId: string | null): string | undefined {
  const control = controlId ? document.getElementById(controlId) : null;
  // Radio groups, checkbox lists and custom sections have no focusable control of their own.
  const target = control?.matches(FOCUSABLE)
    ? control
    : (control ?? error.closest('[data-form-item]') ?? error.closest('[data-form-section]'))
        ?.querySelector<HTMLElement>(FOCUSABLE);
  if (!target) return undefined;
  if (!target.id) target.id = `form-error-target-${++generatedTargetId}`;
  return target.id;
}

/** Reads every visible field error in the scope, in page order. */
function collectErrors(scope: Element, summary: Element): ErrorSummaryItem[] {
  const labels = Array.from(scope.querySelectorAll('label'));

  return Array.from(scope.querySelectorAll<HTMLElement>('[data-form-error]'))
    .filter((element) => !summary.contains(element))
    .map((element) => {
      // FormMessage is `${id}-form-item-message`; its control is `${id}-form-item`.
      const controlId = element.id.endsWith('-message') ? element.id.slice(0, -'-message'.length) : null;
      const label = controlId ? labels.find((candidate) => candidate.htmlFor === controlId) : undefined;
      const section = element.closest('[data-form-section]');

      return {
        section: cleanText(section?.querySelector('h3')?.textContent),
        label: cleanText(label?.textContent),
        message: element.textContent?.trim() ?? '',
        targetId: resolveTarget(element, controlId),
      };
    })
    .filter((item) => item.message);
}

function groupBySection(items: ErrorSummaryItem[]) {
  const groups: { section: string; items: ErrorSummaryItem[] }[] = [];
  for (const item of items) {
    const last = groups[groups.length - 1];
    if (last && last.section === item.section) last.items.push(item);
    else groups.push({ section: item.section, items: [item] });
  }
  return groups;
}

function jumpTo(item: ErrorSummaryItem) {
  const target = item.targetId ? document.getElementById(item.targetId) : null;
  if (!target) return;

  // Centering keeps the field clear of the sticky header without per-page offsets.
  target.scrollIntoView({ block: 'center', behavior: 'smooth' });
  target.focus({ preventScroll: true });
}

/**
 * Lists the form's errors right above its action buttons, where the user is
 * looking after a failed submit, and jumps to the field when an item is pressed.
 *
 * Place it as a sibling of the form sections (same parent as FormActions). It
 * picks up every FormMessage error and any element marked `data-form-error`,
 * so backend errors set with `form.setError` appear without extra wiring.
 *
 * When errors first appear: one error jumps straight to its field; two or more
 * focus the summary so the user sees everything that needs fixing.
 */
export function FormErrorSummary({ className }: Readonly<{ className?: string }>) {
  const ref = useRef<HTMLDivElement>(null);
  const titleId = useId();
  const [items, setItems] = useState<ErrorSummaryItem[]>([]);
  const previousCount = useRef(0);

  useEffect(() => {
    const summary = ref.current;
    const scope = summary?.parentElement;
    if (!summary || !scope) return;

    const update = () => {
      const next = collectErrors(scope, summary);
      // Our own re-render mutates the scope too; only a real change may set state, or this loops.
      setItems((current) => (JSON.stringify(current) === JSON.stringify(next) ? current : next));
    };

    update();
    const observer = new MutationObserver(update);
    observer.observe(scope, { childList: true, subtree: true, characterData: true });
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    if (previousCount.current === 0 && items.length === 1) {
      jumpTo(items[0]);
    } else if (previousCount.current === 0 && items.length > 1) {
      ref.current?.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
      ref.current?.focus({ preventScroll: true });
    }
    previousCount.current = items.length;
  }, [items]);

  return (
    <div
      ref={ref}
      tabIndex={-1}
      role="alert"
      aria-labelledby={items.length ? titleId : undefined}
      hidden={items.length === 0}
      className={cn(
        'rounded-lg border border-destructive/30 bg-destructive/5 p-4 outline-none focus-visible:ring-2 focus-visible:ring-destructive/40',
        className,
      )}
    >
      {items.length > 0 ? (
        <>
          <p
            id={titleId}
            className="flex items-center gap-2 text-sm font-bold text-destructive-strong"
          >
            <AlertCircle className="h-4 w-4 shrink-0" aria-hidden="true" />
            入力内容を確認してください（{items.length}件）
          </p>
          <div className="mt-3 space-y-3">
            {groupBySection(items).map((group, groupIndex) => (
              <div key={`${group.section}-${groupIndex}`}>
                {group.section ? (
                  <p className="text-small font-bold text-foreground">{group.section}</p>
                ) : null}
                <ul className="mt-1 space-y-1">
                  {group.items.map((item, index) => (
                    <li key={`${item.targetId ?? item.message}-${index}`}>
                      <button
                        type="button"
                        onClick={() => jumpTo(item)}
                        className="rounded-sm text-left text-sm text-destructive-strong underline underline-offset-2 hover:no-underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-destructive/40"
                      >
                        {item.label ? `${item.label}：${item.message}` : item.message}
                      </button>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </>
      ) : null}
    </div>
  );
}
