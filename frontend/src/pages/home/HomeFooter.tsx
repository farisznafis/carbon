import { Mail, MapPin, Phone } from 'lucide-react';
import {
  HOME_CONTACT_DETAILS,
  HOME_LEGAL_LABELS,
  HOME_SECTION_IDS,
} from './homeContent';

const HOME_NAVIGATION_LINKS = [
  { label: 'ご利用の流れ', href: `#${HOME_SECTION_IDS.flow}` },
  { label: '特徴', href: `#${HOME_SECTION_IDS.features}` },
  { label: '対応方法論', href: `#${HOME_SECTION_IDS.methods}` },
  { label: 'よくある質問', href: `#${HOME_SECTION_IDS.faq}` },
] as const;

/** Renders the homepage footer with navigation, contact, and legal information. */
export function HomeFooter() {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="border-t border-border bg-muted/70">
      <div className="mx-auto w-full max-w-page px-4 py-10 sm:px-6 sm:py-12 lg:px-8 2xl:py-16">
        <div className="grid gap-10 md:grid-cols-2 lg:grid-cols-[1.2fr_0.8fr_1fr] lg:gap-16 2xl:gap-24">
          <div className="max-w-sm space-y-4">
            <div className="flex items-center gap-2">
              <span className="text-3xl font-bold tracking-tight text-foreground">
                <span className="text-primary">G</span>X
              </span>
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

          <address className="space-y-4 not-italic md:col-span-2 lg:col-span-1">
            <p className="font-semibold text-foreground">接触</p>
            <ul className="space-y-3 text-sm font-semibold text-foreground">
              <li className="flex items-start gap-3">
                <MapPin
                  className="mt-0.5 h-5 w-5 shrink-0"
                  aria-hidden="true"
                />
                <span>
                  <span className="block">{HOME_CONTACT_DETAILS.postalCode}</span>
                  <span className="block">{HOME_CONTACT_DETAILS.address}</span>
                </span>
              </li>
              <li>
                <a
                  href={`mailto:${HOME_CONTACT_DETAILS.email}`}
                  className="flex w-fit items-center gap-3 transition-colors hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                >
                  <Mail className="h-5 w-5 shrink-0" aria-hidden="true" />
                  {HOME_CONTACT_DETAILS.email}
                </a>
              </li>
              <li>
                <a
                  href={`tel:${HOME_CONTACT_DETAILS.phoneHref}`}
                  className="flex w-fit items-center gap-3 transition-colors hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                >
                  <Phone className="h-5 w-5 shrink-0" aria-hidden="true" />
                  {HOME_CONTACT_DETAILS.phoneDisplay}
                </a>
              </li>
            </ul>
          </address>
        </div>

        <div className="mt-10 flex flex-col gap-4 border-t border-border pt-6 text-sm text-muted-foreground sm:flex-row sm:items-center sm:justify-between">
          <p>© {currentYear} GX Project. All rights reserved.</p>
          <div className="flex flex-wrap gap-x-6 gap-y-2">
            {HOME_LEGAL_LABELS.map((label) => (
              <span key={label}>{label}</span>
            ))}
          </div>
        </div>
      </div>
    </footer>
  );
}
