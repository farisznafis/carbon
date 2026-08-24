import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/button';

/** Defines the supported static/GIF and video sources for the hero preview. */
export type HeroMediaSource =
  | {
      kind: 'image';
      src: string;
      alt: string;
    }
  | {
      kind: 'video';
      src: string;
      label: string;
      poster?: string;
    };

type HeroMediaFrameProps = Readonly<{
  source: HeroMediaSource;
}>;

type HomeHeroSectionProps = Readonly<{
  signupPath: string;
}>;

/** Central replacement point for the homepage GIF, image, or video preview. */
// const HERO_MEDIA_SOURCE: HeroMediaSource = {
//   kind: 'image',
//   src: '/assets/hero/dashboard-preview.png',
//   alt: 'GXダッシュボードのプレビュー',
// };

const HERO_MEDIA_SOURCE: HeroMediaSource = {
  kind: 'video',
  src: '/assets/hero/dashboard-preview.mp4',
  label: 'GXダッシュボードの操作イメージ',
  poster: '/assets/hero/dashboard-preview.png',
};

/** Renders the responsive Figma media frame for either an image/GIF or video. */
export function HeroMediaFrame({ source }: HeroMediaFrameProps) {
  return (
    <div className="relative mx-auto w-full max-w-[1340px]">
      <img
        src="/assets/hero/glow-left.svg"
        alt=""
        aria-hidden="true"
        className="pointer-events-none absolute -left-[5%] -top-[12%] hidden h-[68%] w-[45%] -rotate-[15deg] object-fill sm:block"
      />
      <img
        src="/assets/hero/glow-right.svg"
        alt=""
        aria-hidden="true"
        className="pointer-events-none absolute -right-[2%] -top-[7%] hidden h-[70%] w-[38%] rotate-[88deg] object-fill sm:block"
      />

      <div className="relative mx-auto w-full max-w-[1200px] rounded-3xl border border-muted-foreground/60 bg-card p-3 shadow-app-md sm:p-6 lg:p-9">
        <div className="aspect-[1319/703] overflow-hidden border border-muted-foreground/60 bg-muted">
          {source.kind === 'image' ? (
            <img
              src={source.src}
              alt={source.alt}
              loading="eager"
              decoding="async"
              className="h-full w-full object-cover object-top"
            />
          ) : (
            <video
              src={source.src}
              poster={source.poster}
              title={source.label}
              autoPlay
              muted
              loop
              playsInline
              preload="metadata"
              className="h-full w-full object-cover object-top"
            >
              お使いのブラウザは動画の再生に対応していません。
            </video>
          )}
        </div>
      </div>
    </div>
  );
}

/** Renders the viewport-height homepage hero and its intentionally clipped preview. */
export function HomeHeroSection({ signupPath }: HomeHeroSectionProps) {
  return (
    <section
      aria-labelledby="home-hero-title"
      className="relative min-h-[calc(100dvh-4rem-1px)] overflow-hidden bg-card px-4 pb-28 pt-12 sm:px-6 sm:pb-32 sm:pt-16 lg:h-[calc(100dvh-4rem-1px)] lg:min-h-[700px] lg:px-8 lg:pb-0 lg:pt-20"
    >
      <div className="mx-auto flex w-full max-w-[1340px] flex-col items-center">
        <div className="animate-slide-up flex w-full max-w-[1240px] flex-col items-center text-center motion-reduce:animate-none">
          <h1
            id="home-hero-title"
            className="max-w-6xl text-3xl font-bold leading-tight tracking-tight text-foreground sm:text-4xl lg:text-5xl"
          >
            カーボンデータを「意思決定」に変えるプラットフォーム
          </h1>
          <p className="mt-4 max-w-3xl text-base leading-7 text-foreground sm:text-xl">
            排出量・カーボンクレジット・コンプライアンスを一つの画面で管理します。
          </p>
          <Button asChild size="lg" className="mt-5 px-8">
            <Link to={signupPath}>無料で始める</Link>
          </Button>
        </div>

        <div className="mt-12 w-full animate-slide-up motion-reduce:animate-none sm:mt-16">
          <HeroMediaFrame source={HERO_MEDIA_SOURCE} />
        </div>
      </div>
    </section>
  );
}
