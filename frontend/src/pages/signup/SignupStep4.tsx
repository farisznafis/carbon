import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSignup } from '@/contexts/SignupContext';
import { FormCard } from '@/components/forms/form-layout';
import { Button } from '@/components/ui/button';
import { Mail, CheckCircle } from 'lucide-react';

export const SignupStep4 = () => {
  const navigate = useNavigate();
  const { signupData, resetSignup } = useSignup();

  useEffect(() => {
    // Save signup data to localStorage for later autofill
    localStorage.setItem('userSignupData', JSON.stringify(signupData));
  }, [signupData]);

  const handleContinue = () => {
    resetSignup();
    navigate('/login');
  };

  return (
    <FormCard>
      <div className="space-y-6 text-center">
      <div className="flex justify-center">
        <div className="h-20 w-20 rounded-full bg-success/10 flex items-center justify-center">
          <CheckCircle className="h-10 w-10 text-success" />
        </div>
      </div>

      <div>
        <h3 className="text-3xl font-semibold mb-2">登録完了！</h3>
        <p className="text-muted-foreground text-lg">
          ご登録ありがとうございます
        </p>
      </div>

      <div className="mx-auto max-w-md rounded-md border p-6">
          <div className="flex items-start space-x-4">
            <div className="h-12 w-12 rounded-lg bg-primary/10 flex items-center justify-center flex-shrink-0">
              <Mail className="h-6 w-6 text-primary" />
            </div>
            <div className="text-left">
              <h4 className="font-semibold mb-1">確認メールを送信しました</h4>
              <p className="text-sm text-muted-foreground">
                <strong>{signupData.email}</strong>に確認メールを送信しました。
                受信箱を確認し、確認リンクをクリックしてアカウントを有効化してください。
              </p>
            </div>
          </div>
      </div>

      <div className="space-y-3">
        <Button onClick={handleContinue} size="sm" className="w-full max-w-md">
          ログインへ続く
        </Button>
        <p className="text-sm text-muted-foreground">
          メールが届かない場合は、迷惑メールフォルダを確認するか、サポートにお問い合わせください。
        </p>
      </div>
      </div>
    </FormCard>
  );
};
