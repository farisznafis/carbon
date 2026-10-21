import { Link } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import { Button } from '@/components/ui/button';

type HomeCtaSectionProps = Readonly<{
  signupPath: string;
}>;

/** Renders the final registration call-to-action on the public homepage. */
export function HomeCtaSection({ signupPath }: HomeCtaSectionProps) {
  return (
    <section className='bg-card px-4 py-14 sm:px-6 sm:py-16 lg:px-8 2xl:py-marketing-section'>
      <div
        className='
          relative
          mx-auto
          w-full
          max-w-page
          overflow-hidden
          rounded-3xl
          bg-[linear-gradient(130deg,hsl(var(--blue-900)),hsl(var(--blue-800))_70%,hsl(var(--blue-700)))]
          shadow-marketing
        '
      >
        {/* Decorative brand ring */}
        <div
          aria-hidden='true'
          className='
            pointer-events-none
            absolute
            -right-24
            -top-28
            h-64
            w-64
            rounded-full
            border-[50px]
            border-[hsl(var(--blue-400)/0.12)]
          '
        />

        <div
          className='
            relative
            z-10
            flex
            flex-col
            gap-section
            p-8
            sm:p-10
            lg:p-[clamp(2rem,2.5vw,3rem)]
            lg:flex-row
            lg:items-center
            lg:justify-between
          '
        >
          <div className='max-w-3xl'>
            <p
              className='
                mb-2
                text-small
                font-bold
                text-[hsl(var(--blue-200))]
              '
            >
              プロジェクト入会
            </p>

            <h2
              className='
                text-balance
                text-3xl
                font-bold
                leading-tight
                tracking-tight
                text-white
                sm:text-marketing-title
              '
            >
              プロジェクトへの入会を、もっとかんたんに
            </h2>

            <p
              className='
                mt-4
                max-w-2xl
                text-base
                leading-7
                text-white
                sm:text-lg
              '
            >
              参加可能なプロジェクトの確認から入会届の提出まで、
              オンラインでスムーズに進められます。
            </p>
          </div>

          <Button
            asChild
            size='lg'
            variant='outline'
            className='
              shrink-0
              border-white
              bg-white
              text-primary-strong
              hover:border-[hsl(var(--blue-50))]
              hover:bg-[hsl(var(--blue-50))]
              hover:text-primary-strong
            '
          >
            <Link to={signupPath}>
              今すぐ登録する
              <ArrowRight aria-hidden='true' />
            </Link>
          </Button>
        </div>
      </div>
    </section>
  );
}
