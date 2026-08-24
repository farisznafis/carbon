import { HOME_PENDING_LINKS, HOME_SECTION_IDS } from './homeContent';

const HOME_NAVIGATION_LINKS = [
  { label: '特徴', href: `#${HOME_SECTION_IDS.features}` },
  { label: 'ご利用の流れ', href: `#${HOME_SECTION_IDS.flow}` },
  { label: '対応方法論', href: `#${HOME_SECTION_IDS.methods}` },
  { label: 'よくある質問', href: `#${HOME_SECTION_IDS.faq}` },
] as const;

/** Renders the homepage-only footer without exposing placeholder contact data. */
export function HomeFooter() {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="border-t border-border bg-muted/70">
      <div className="mx-auto w-full max-w-marketing px-4 py-12 sm:px-6 sm:py-16 lg:px-8">
        <div className="grid gap-10 sm:grid-cols-2 lg:grid-cols-[1.2fr_0.8fr_0.8fr_1fr]">
          <div className="max-w-sm space-y-4">
            <div className="flex items-center gap-2">
              <span className="text-3xl font-bold text-primary">GX</span>
              <span className="text-xl font-bold text-foreground">Project</span>
            </div>
            <p className="text-sm leading-6 text-muted-foreground">
              再生可能エネルギープロジェクトへの参加申請を、オンラインでかんたんに。
            </p>
          </div>

          <nav aria-label="ホームページ内ナビゲーション" className="space-y-4">
            <p className="font-semibold text-foreground">ナビゲーション</p>
            <ul className="space-y-3 text-sm">
              {HOME_NAVIGATION_LINKS.map((link) => (
                <li key={link.href}>
                  <a
                    href={link.href}
                    className="text-muted-foreground transition-colors hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  >
                    {link.label}
                  </a>
                </li>
              ))}
            </ul>
          </nav>

          <div className="space-y-4">
            <p className="font-semibold text-foreground">会社</p>
            <ul className="space-y-3 text-sm">
              {HOME_PENDING_LINKS.map((label) => (
                <li key={label} className="flex flex-wrap items-center gap-2">
                  <span className="text-muted-foreground">{label}</span>
                  <span className="rounded-full bg-card px-2 py-0.5 text-xs text-muted-foreground">
                    準備中
                  </span>
                </li>
              ))}
            </ul>
          </div>

          <div className="space-y-4">
            <p className="font-semibold text-foreground">お問い合わせ</p>
            <p className="text-sm leading-6 text-muted-foreground">
              お問い合わせ窓口は現在準備中です。公開後、このページからご案内します。
            </p>
          </div>
        </div>

        <div className="mt-12 border-t border-border pt-6 text-sm text-muted-foreground">
          © {currentYear} GX Project. All rights reserved.
        </div>
      </div>
    </footer>
  );
}
