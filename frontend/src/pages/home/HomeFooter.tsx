import { Link } from 'react-router-dom';
import { paths } from '@/routes/paths';
import { HOME_SECTION_IDS } from './homeContent';

const HOME_NAVIGATION_LINKS = [
  { label: 'ご利用の流れ', href: `#${HOME_SECTION_IDS.flow}` },
  { label: '特徴', href: `#${HOME_SECTION_IDS.features}` },
  { label: '対応方法論', href: `#${HOME_SECTION_IDS.methods}` },
  { label: 'よくある質問', href: `#${HOME_SECTION_IDS.faq}` },
] as const;

// shortcut: company info, 利用規約 and プライバシーポリシー are left out until the real content and pages exist.
/** Renders the homepage footer with navigation and the contact form link. */
export function HomeFooter() {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="border-t border-border bg-muted/70">
      <div className="mx-auto w-full max-w-page px-4 py-10 sm:px-6 sm:py-12 lg:px-8 2xl:py-[calc(var(--section-gap)*2)]">
        <div
          className="
            grid
            gap-10
            md:grid-cols-2
            lg:grid-cols-[1fr_auto_auto]
            lg:gap-16
            2xl:gap-[calc(var(--section-gap)*3.5)]
          "
        >
          <div className="max-w-sm space-y-4">
            <div className="flex items-center gap-2">
              <span className="text-3xl font-bold tracking-tight text-foreground">
                <span className="text-primary-strong">G</span>X
              </span>
              <span className="text-3xl font-bold tracking-tight text-foreground">Project</span>
            </div>
            <p className="text-sm leading-6 text-muted-foreground">
              再生可能エネルギープロジェクトへの入会申請を、オンラインでかんたんに。
            </p>
          </div>

          <nav aria-label="ホームページ内ナビゲーション" className="space-y-4">
            <p className="font-bold text-foreground">ナビゲーション</p>
            <ul className="space-y-3 text-sm ">
              {HOME_NAVIGATION_LINKS.map((link) => (
                <li key={link.href}>
                  <a
                    href={link.href}
                    className="text-muted-foreground transition-colors hover:text-primary-strong focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                  >
                    {link.label}
                  </a>
                </li>
              ))}
            </ul>
          </nav>

          <div className="space-y-4 md:col-span-2 lg:col-span-1">
            <p className="font-bold text-foreground">お問い合わせ</p>
            <p className="text-sm leading-6 text-muted-foreground">
              サービスに関するご質問や導入のご相談は、
              <br className="hidden lg:block" />
              お問い合わせフォームからご連絡ください。
            </p>
            <Link
              to={paths.contact}
              className="inline-flex text-sm font-medium text-primary-strong transition-colors hover:underline [@media(pointer:coarse)]:min-h-11 [@media(pointer:coarse)]:items-center focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            >
              お問い合わせフォーム →
            </Link>
          </div>
        </div>

        <div className="mt-10 border-t border-border pt-6 text-sm text-muted-foreground">
          <p>© {currentYear} GX Project. All rights reserved.</p>
        </div>
      </div>
    </footer>
  );
}
