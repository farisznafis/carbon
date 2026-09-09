import { Link } from 'react-router-dom';
import { ArrowRight, Mail } from 'lucide-react';
import { Button } from '@/components/ui/button';

type HomeCtaSectionProps = Readonly<{
  signupPath: string;
  contactPath: string;
}>;

/** Renders the final registration call-to-action from the Figma homepage. */
export function HomeCtaSection({
  signupPath,
  contactPath,
}: HomeCtaSectionProps) {
  return (
    <section className="bg-card px-4 py-14 sm:px-6 sm:py-16 lg:px-8 2xl:py-24">
      <div className="mx-auto grid w-full max-w-marketing overflow-hidden rounded-3xl bg-primary shadow-marketing lg:grid-cols-[minmax(0,1fr)_18rem] 2xl:grid-cols-[minmax(0,1fr)_22rem]">
        <div className="flex flex-col justify-center gap-7 p-8 text-primary-foreground sm:p-10 2xl:p-16">
          <div className="max-w-3xl space-y-4">
            <h2 className="text-balance text-3xl font-bold leading-tight tracking-tight sm:text-4xl 2xl:text-5xl">
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
              asChild
              size="lg"
              variant="outline"
              className="border-primary-foreground/70 bg-transparent text-primary-foreground"
            >
              <Link to={contactPath}>
                <Mail aria-hidden="true" />
                お問い合わせ
              </Link>
            </Button>
          </div>
        </div>

        <div className="relative hidden min-h-80 items-end justify-center overflow-hidden bg-card lg:flex 2xl:min-h-[24rem]">
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
