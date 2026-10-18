import { useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, Pause, Play } from 'lucide-react';

import { Button } from '@/components/ui/button';

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

const HERO_MEDIA_SOURCE: HeroMediaSource = {
  kind: 'video',
  src: '/assets/hero/dashboard-carbonzero.mp4',
  label: 'Carbon Zeroのプロダクトイメージを表現したモーション映像',
};

/**
 * Hero media preview.
 * Uses the shared Carbon design-system tokens for
 * border, radius, shadow, color, and interaction states.
 */
export function HeroMediaFrame({ source }: HeroMediaFrameProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [isPlaying, setIsPlaying] = useState(false);

  const toggleVideo = async () => {
    const video = videoRef.current;

    if (!video) return;

    if (video.paused) {
      await video.play();
    } else {
      video.pause();
    }
  };

  return (
    <div className="relative">
      <div
        className="
          relative
          aspect-[4/5]
          overflow-hidden
          rounded-lg
          border
          border-border
          bg-foreground
          shadow-app-md
          sm:aspect-[16/10]
          lg:min-h-[clamp(360px,25.5vw,490px)]
        "
      >
        {source.kind === 'image' ? (
          <img
            src={source.src}
            alt={source.alt}
            loading="eager"
            decoding="async"
            className="h-full w-full object-cover"
          />
        ) : (
          <video
            ref={videoRef}
            src={source.src}
            poster={source.poster}
            title={source.label}
            autoPlay
            muted
            loop
            playsInline
            preload="metadata"
            onPlay={() => setIsPlaying(true)}
            onPause={() => setIsPlaying(false)}
            className="h-full w-full object-cover object-top"
          >
            お使いのブラウザは動画の再生に対応していません。
          </video>
        )}

        {/* Subtle overlay for visual depth. */}
        <div
          aria-hidden="true"
          className="
            pointer-events-none
            absolute
            inset-0
            bg-gradient-to-b
            from-transparent
            via-transparent
            to-foreground/20
          "
        />

        {source.kind === 'video' ? (
          <div className="absolute right-4 top-4 z-10">
            <Button
              type="button"
              variant="secondary"
              size="icon"
              onClick={toggleVideo}
              aria-label={isPlaying ? '動画を一時停止' : '動画を再生'}
              className="backdrop-blur-sm"
            >
              {isPlaying ? (
                <Pause aria-hidden="true" />
              ) : (
                <Play aria-hidden="true" />
              )}
            </Button>
          </div>
        ) : null}
      </div>
    </div>
  );
}

/** Renders the responsive public hero and its primary actions. */
export function HomeHeroSection({
  signupPath,
}: HomeHeroSectionProps) {
  return (
    <section
      id="top"
      aria-labelledby="home-hero-title"
      className="
        relative
        min-h-[calc(100dvh-64px)]
        overflow-hidden
        bg-gradient-to-b
        from-card
        via-card
        to-accent/30
      "
    >
      {/* Subtle decorative background element. */}
      <div
        aria-hidden="true"
        className="
          pointer-events-none
          absolute
          -right-[180px]
          top-0
          hidden
          h-[620px]
          w-[620px]
          rounded-full
          border
          border-primary/10
          lg:block
        "
      />

      <div
        className="
          relative
          z-10
          mx-auto
          grid
          w-full
          max-w-page
          grid-cols-1
          items-center
          gap-10
          px-4
          pb-10
          pt-14
          sm:px-6
          sm:pt-20
          lg:grid-cols-[minmax(0,0.82fr)_minmax(520px,1.18fr)]
          lg:gap-12
          lg:px-8
          lg:py-[72px]
          xl:gap-[clamp(48px,3.75vw,72px)]
        "
      >
        {/* Left: message and CTA */}
        <div className="animate-slide-up motion-reduce:animate-none">
          <h1
            id="home-hero-title"
            className="
              max-w-[720px]
              text-4xl
              font-bold
              leading-tight
              tracking-tight
              text-foreground
              sm:text-5xl
              lg:text-hero-title
            "
          >
            カーボンデータを、
            <span className="whitespace-nowrap text-primary">
              「意思決定」
            </span>
            に変える。
          </h1>

          <p
            className="
              mt-6
              max-w-xl
              text-base
              leading-7
              text-muted-foreground
              sm:text-lg
            "
          >
            排出量、拠点、設備、証憑、モニタリング。
            <br className="hidden sm:block" />
            分散しがちなJ-クレジット業務を、
            判断につながるひとつの運用へ。
          </p>

          <div className="mt-8 flex flex-col gap-3 sm:flex-row">
            <Button
              asChild
              size="lg"
              variant="outline"
              className="w-full sm:w-auto"
            >
              <a href="#features">
                機能を見る
              </a>
            </Button>

            <Button
              asChild
              size="lg"
              className="w-full sm:w-auto"
            >
              <Link to={signupPath}>
                無料ではじめる
                <ArrowRight aria-hidden="true" />
              </Link>
            </Button>
          </div>
        </div>

        {/* Right: product video */}
        <div className="animate-slide-up motion-reduce:animate-none">
          <HeroMediaFrame source={HERO_MEDIA_SOURCE} />
        </div>
      </div>
    </section>
  );
}
