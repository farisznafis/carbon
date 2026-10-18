import React, { useState } from 'react';
import type { ReactNode, MouseEvent } from 'react';
import { z } from 'zod';

const inlineHelpSchema = z.object({
  /** Content shown inside the help dialog */
  content: z.union([z.string(), z.custom<ReactNode>()]),
  /** Optional className to customize wrapper */
  className: z.string().optional(),
});
export type InlineHelpProps = z.infer<typeof inlineHelpSchema>;

/**
 * InlineHelp
 * Small inline “?” icon that reveals a help dialog on click.
 * Dialog appears to the right and is dismissible via “x” or outside click.
 */
export const InlineHelp: React.FC<InlineHelpProps> = (rawProps) => {
  const props = inlineHelpSchema.parse(rawProps);
  const { content, className } = props;
  const [open, setOpen] = useState(false);

  const toggle = (e: MouseEvent) => {
    e.stopPropagation();
    setOpen((o) => !o);
  };

  const close = () => setOpen(false);

  // close when clicking anywhere in the document
  React.useEffect(() => {
    if (!open) return;
    const handle = () => close();
    window.addEventListener('click', handle);
    return () => window.removeEventListener('click', handle);
  }, [open]);

  return (
    <span
      className={className}
      style={{
        position: 'relative',
        display: 'inline-flex',
        alignItems: 'center',
      }}
      onClick={(e) => e.stopPropagation()}
    >
      <button
        type='button'
        onClick={toggle}
        // Keeps the 16px dot but gives it a 44px hit area.
        className="relative before:absolute before:-inset-[14px] before:content-['']"
        style={{
          // smaller overall size
          width: '1rem',
          height: '1rem',
          borderRadius: '9999px',
          border: 'none',
          backgroundColor: '#6b7280',
          color: '#ffffff',
          // smaller font size for "?"
          fontSize: '0.7rem',
          fontWeight: 600,
          cursor: 'pointer',
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          padding: 0,
        }}
      >
        ?
      </button>

      {open && (
        <div
          role='dialog'
          style={{
            position: 'absolute',
            top: '0',
            left: '100%',
            marginLeft: '0.5rem',
            zIndex: 20,
            // width: '120px',
            maxWidth: '12rem',
            minWidth: '12rem',
            backgroundColor: '#ffffff',
            borderRadius: '0.375rem',
            boxShadow: '0 10px 15px -3px rgba(0,0,0,0.1)',
            border: '1px solid #e5e7eb',
            padding: '0.5rem 1.5rem 0.75rem 0.75rem',
            fontSize: '0.875rem',
          }}
        >
          {/* top-right X */}
          <button
            type='button'
            onClick={close}
            style={{
              position: 'absolute',
              top: '0.25rem',
              right: '0.4rem',
              border: 'none',
              background: 'transparent',
              cursor: 'pointer',
              // darker and larger
              fontSize: '1rem',
              color: '#111827',
              lineHeight: 1,
              padding: 0,
            }}
          >
            ×
          </button>

          <div style={{ wordWrap: 'break-word', paddingTop: '0.25rem' }}>
            {content}
          </div>
        </div>
      )}
    </span>
  );
};

/* ================= Example Usage ================= */

const Example = () => (
  <p>
    Username
    <InlineHelp
      content={
        <>
          This will be visible to other users. Use 3–20 characters and avoid
          personal info.
        </>
      }
    />
  </p>
);
