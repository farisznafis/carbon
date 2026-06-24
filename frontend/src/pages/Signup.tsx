import { useSignup } from '@/contexts/SignupContext';
import { Check } from 'lucide-react';
import { SignupStep1 } from './signup/SignupStep1';
import { SignupStep2 } from './signup/SignupStep2';
import { SignupStep3 } from './signup/SignupStep3';
import { SignupStep4 } from './signup/SignupStep4';
import { cn } from '@/lib/utils';

const Signup = () => {
  const { currentStep } = useSignup();

  const steps = [
    {
      number: 1,
      label: '法人情報',
      title: '法人情報の入力',
      description: '会社情報、所在地、担当者情報を入力してください。',
    },
    {
      number: 2,
      label: '方法論',
      title: '参加方法論の選択',
      description: '参加したいプロジェクトと現在の活動情報を入力してください。',
    },
    {
      number: 3,
      label: '確認',
      title: '入力内容の確認',
      description: '登録前に入力内容をご確認ください。',
    },
    {
      number: 4,
      label: '完了',
      title: '登録完了',
      description: '確認メールの案内をご確認ください。',
    },
  ];

  const activeStep = steps.find((step) => step.number === currentStep) ?? steps[0];
  const progressPercent = (currentStep / steps.length) * 100;

  return (
    <div className="min-h-screen bg-[#f7faf8] px-4 py-6 text-slate-950 sm:px-6 lg:px-8">
      <div className="mx-auto grid w-full max-w-6xl gap-6 lg:grid-cols-[280px_minmax(0,1fr)] lg:gap-8">
        <aside className="hidden lg:block">
          <div className="sticky top-8 rounded-lg border border-emerald-900/10 bg-white/90 p-6 shadow-sm">
            <p className="text-sm font-medium text-emerald-700">GX Portal</p>
            <h1 className="mt-2 text-2xl font-semibold tracking-normal">
              アカウント作成
            </h1>
            <p className="mt-3 text-sm leading-6 text-slate-600">
              必要な情報を順番に入力して、登録申請を完了してください。
            </p>

            <ol className="mt-8 space-y-5">
              {steps.map((step) => {
                const isActive = currentStep === step.number;
                const isCompleted = currentStep > step.number;

                return (
                  <li key={step.number} className="flex gap-3">
                    <div className="flex flex-col items-center">
                      <div
                        className={cn(
                          'flex h-9 w-9 items-center justify-center rounded-full border text-sm font-semibold transition-colors',
                          isCompleted &&
                            'border-emerald-600 bg-emerald-600 text-white',
                          isActive &&
                            'border-emerald-700 bg-emerald-50 text-emerald-800',
                          !isActive &&
                            !isCompleted &&
                            'border-slate-200 bg-slate-50 text-slate-500',
                        )}
                      >
                        {isCompleted ? <Check className="h-4 w-4" /> : step.number}
                      </div>
                    </div>
                    <div className="min-w-0 pb-1">
                      <p
                        className={cn(
                          'text-sm font-semibold',
                          isActive || isCompleted ? 'text-slate-950' : 'text-slate-500',
                        )}
                      >
                        {step.label}
                      </p>
                      <p className="mt-1 text-xs leading-5 text-slate-500">
                        {step.title}
                      </p>
                    </div>
                  </li>
                );
              })}
            </ol>
          </div>
        </aside>

        <main className="min-w-0">
          <div className="rounded-lg border border-emerald-900/10 bg-white/90 p-4 shadow-sm lg:hidden">
            <div className="flex items-center justify-between gap-4">
              <div>
                <p className="text-xs font-semibold uppercase tracking-wide text-emerald-700">
                  アカウント作成
                </p>
                <h1 className="mt-1 text-xl font-semibold tracking-normal">
                  登録ステップ
                </h1>
              </div>
              <span className="shrink-0 rounded-full bg-emerald-50 px-3 py-1 text-sm font-semibold text-emerald-800">
                {currentStep}/{steps.length}
              </span>
            </div>
            <div className="mt-4 h-2 overflow-hidden rounded-full bg-slate-100">
              <div
                className="h-full rounded-full bg-emerald-600 transition-all duration-300"
                style={{ width: `${progressPercent}%` }}
              />
            </div>
            <div className="mt-3 grid grid-cols-4 gap-2">
              {steps.map((step) => (
                <div
                  key={step.number}
                  className={cn(
                    'h-1.5 rounded-full transition-colors',
                    currentStep >= step.number ? 'bg-emerald-600' : 'bg-slate-200',
                  )}
                  aria-label={step.label}
                />
              ))}
            </div>
          </div>

          <header className="mb-6 mt-6 rounded-lg border border-emerald-900/10 bg-white/90 px-5 py-5 shadow-sm sm:px-6 lg:mt-0">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
              <div>
                <p className="text-sm font-medium text-emerald-700">
                  ステップ {currentStep} / {steps.length}
                </p>
                <h2 className="mt-2 text-2xl font-semibold tracking-normal sm:text-3xl">
                  {activeStep.title}
                </h2>
                <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600">
                  {activeStep.description}
                </p>
              </div>
              <span className="inline-flex w-fit items-center rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1 text-sm font-medium text-emerald-800">
                {activeStep.label}
              </span>
            </div>
          </header>

          <div className="animate-slide-up">
            {currentStep === 1 && <SignupStep1 />}
            {currentStep === 2 && <SignupStep2 />}
            {currentStep === 3 && <SignupStep3 />}
            {currentStep === 4 && <SignupStep4 />}
          </div>
        </main>
      </div>
    </div>
  );
};

export default Signup;
