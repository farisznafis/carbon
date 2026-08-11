import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { ArrowRight, Shield, BarChart3, Users } from 'lucide-react';
import { useRedirectIfLoggedIn } from '@/hooks/useRedirectIfLoggedIn';

const FEATURES = [
  {
    icon: Shield,
    title: 'コンプライアンス追跡',
    description: 'リアルタイムモニタリングと自動レポートで環境規制に準拠。',
  },
  {
    icon: BarChart3,
    title: 'データ分析',
    description: '強力な分析とカスタマイズ可能なダッシュボードでモニタリングデータを可視化。',
  },
  {
    icon: Users,
    title: 'チームコラボレーション',
    description: 'ロールベースのアクセスとプロジェクト管理ダッシュボードでシームレスに協働。',
  },
] as const;

// ─── Animated Blobs ───────────────────────────────────────────────────────────

function AnimatedBlobs() {
  return (
    <div className="absolute inset-0 -z-10 overflow-hidden pointer-events-none">
      {/* Blob 1 — top-left, green */}
      <div className="blob blob-1" />
      {/* Blob 2 — bottom-right, teal */}
      <div className="blob blob-2" />
      {/* Blob 3 — center-right, emerald accent */}
      <div className="blob blob-3" />
    </div>
  );
}

// ─── Sub-components ───────────────────────────────────────────────────────────

function HeroSection() {
  const navigate = useNavigate();

  return (
    <section className="relative overflow-hidden py-20 lg:py-32">
      <AnimatedBlobs />

      <div className="container mx-auto px-4">
        <div className="mx-auto max-w-4xl text-center animate-slide-up space-y-6">

          {/* Headline */}
          <h1 className="text-4xl lg:text-6xl font-bold tracking-tight text-foreground">
            カーボンデータを
            <span className="bg-clip-text text-transparent bg-gradient-to-r from-primary to-accent mx-2">
              「意思決定」
            </span>
            に変えるプラットフォーム
          </h1>

          {/* Sub-copy */}
          <p className="text-lg text-muted-foreground max-w-xl mx-auto">
            排出量・カーボンクレジット・コンプライアンスを一つの画面で管理します。
          </p>

          {/* CTA */}
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Button
              size="lg"
              onClick={() => navigate('/signup')}
              className="gradient-primary text-base px-8"
            >
              無料で始める <ArrowRight className="ml-2 h-5 w-5" />
            </Button>
            <Button
              size="lg"
              variant="outline"
              onClick={() => navigate('/login')}
              className="text-base px-8"
            >
              ログイン
            </Button>
          </div>

          {/* ── Dashboard / Video Frame ──────────────────────────────────── */}
          <div className="relative mt-12 mx-auto max-w-5xl">

            {/* Glow behind the frame */}
            <div className="absolute -inset-4 rounded-3xl bg-gradient-to-r from-primary/20 to-accent/20 blur-2xl -z-10" />

            {/* Frame */}
            <div className="rounded-2xl border border-border/60 bg-card shadow-2xl overflow-hidden">
              {/*
                Replace the <video> with your actual asset.
                If using a static image for now, swap to <img>.
              */}
              <video
                src="/assets/dashboard-preview.mp4"
                autoPlay
                muted
                loop
                playsInline
                className="w-full h-auto object-cover"
              />

              {/*
                ── Fallback image (comment out <video> above and use this) ──
                <img
                  src="/assets/dashboard-preview.png"
                  alt="ダッシュボードプレビュー"
                  className="w-full h-auto object-cover"
                />
              */}
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

function FeatureCard({
  icon: Icon,
  title,
  description,
}: {
  icon: React.ElementType;
  title: string;
  description: string;
}) {
  return (
    <div className="p-8 rounded-2xl bg-card border hover:shadow-lg transition-shadow">
      <div className="h-12 w-12 rounded-lg gradient-primary flex items-center justify-center mb-4">
        <Icon className="h-6 w-6 text-white" />
      </div>
      <h3 className="mb-3">{title}</h3>
      <p className="text-muted-foreground">{description}</p>
    </div>
  );
}

function FeaturesSection() {
  return (
    <section className="py-20 bg-muted/30">
      <div className="container mx-auto px-4">
        <h2 className="text-center mb-16">GXを選ぶ理由</h2>
        <div className="grid md:grid-cols-3 gap-8 max-w-6xl mx-auto">
          {FEATURES.map((feature) => (
            <FeatureCard key={feature.title} {...feature} />
          ))}
        </div>
      </div>
    </section>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

const Hero = () => {
  useRedirectIfLoggedIn();

  return (
    <div className="min-h-screen">
      <HeroSection />
      <FeaturesSection />
    </div>
  );
};

export default Hero;