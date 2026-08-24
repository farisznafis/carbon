import { Link } from 'react-router-dom';
import { ArrowRight, Mail } from 'lucide-react';
import { Button } from '@/components/ui/button';

type HomeCtaSectionProps = Readonly<{
  signupPath: string;
}>;

/** Renders the final registration call-to-action from the Figma homepage. */
export function HomeCtaSection({ signupPath }: HomeCtaSectionProps) {
  return (
    <section className="bg-card px-4 py-16 sm:px-6 sm:py-20 lg:px-8 lg:py-24">
      <div className="mx-auto grid w-full max-w-marketing overflow-hidden rounded-3xl bg-primary shadow-marketing lg:grid-cols-[minmax(0,1fr)_22rem]">
        <div className="flex flex-col justify-center gap-7 p-8 text-primary-foreground sm:p-12 lg:p-16">
          <div className="max-w-3xl space-y-4">
            <h2 className="text-3xl font-bold leading-tight tracking-tight sm:text-4xl lg:text-5xl">
              プロジェクト参加を、もっとかんたんに
            </h2>
            <p className="text-base leading-7 text-primary-foreground/90 sm:text-lg">
              参加可能なプロジェクトの確認から入会届の提出まで、オンラインでスムーズに進められます。
            </p>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row">
            <Button asChild size="lg" variant="secondary">
              <Link to={signupPath}>
                今すぐ登録する
                <ArrowRight aria-hidden="true" />
              </Link>
            </Button>
            <Button
              type="button"
              size="lg"
              variant="outline"
              disabled
              className="border-primary-foreground/70 bg-transparent text-primary-foreground"
            >
              <Mail aria-hidden="true" />
              お問い合わせ（準備中）
            </Button>
          </div>
        </div>

        <div className="relative hidden min-h-[24rem] items-end justify-center overflow-hidden bg-card lg:flex">
          <div className="absolute inset-x-0 bottom-0 h-[92%] overflow-hidden rounded-tl-[10rem] bg-card">
            <img
              src="/assets/home/cta-guide.png"
              alt="GXプロジェクトへの参加をご案内する人物"
              loading="eager"
              className="h-full w-full object-contain object-bottom"
            />
          </div>
        </div>
      </div>
    </section>
  );
}
