import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { ArrowRight, Shield, BarChart3, Users } from 'lucide-react';
import { useRedirectIfLoggedIn } from '@/hooks/useRedirectIfLoggedIn';

const Hero = () => {
  const navigate = useNavigate();

  useRedirectIfLoggedIn(); 

  return (
    <div className="min-h-screen">
      {/* Hero Section */}
      <section className="relative overflow-hidden py-20 lg:py-32">
        <div className="container mx-auto px-4">
          <div className="mx-auto max-w-4xl text-center animate-slide-up">
            <h1 className="mb-6 text-5xl lg:text-7xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-primary to-accent">
              demo
            </h1>
            <p className="mb-8 text-xl text-muted-foreground max-w-2xl mx-auto">
              test
            </p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <Button 
                size="lg" 
                onClick={() => navigate('/signup')}
                className="gradient-primary text-lg px-8"
              >
                始める <ArrowRight className="ml-2 h-5 w-5" />
              </Button>
              <Button 
                size="lg" 
                variant="outline"
                onClick={() => navigate('/login')}
                className="text-lg px-8"
              >
                ログイン
              </Button>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-20 bg-muted/30">
        <div className="container mx-auto px-4">
          <h2 className="text-center mb-16">GXを選ぶ理由</h2>
          <div className="grid md:grid-cols-3 gap-8 max-w-6xl mx-auto">
            <div className="p-8 rounded-2xl bg-card border hover:shadow-lg transition-shadow">
              <div className="h-12 w-12 rounded-lg gradient-primary flex items-center justify-center mb-4">
                <Shield className="h-6 w-6 text-white" />
              </div>
              <h3 className="mb-3">コンプライアンス追跡</h3>
              <p className="text-muted-foreground">
                リアルタイムモニタリングと自動レポートで環境規制に準拠。
              </p>
            </div>

            <div className="p-8 rounded-2xl bg-card border hover:shadow-lg transition-shadow">
              <div className="h-12 w-12 rounded-lg gradient-primary flex items-center justify-center mb-4">
                <BarChart3 className="h-6 w-6 text-white" />
              </div>
              <h3 className="mb-3">データ分析</h3>
              <p className="text-muted-foreground">
                強力な分析とカスタマイズ可能なダッシュボードでモニタリングデータを可視化。
              </p>
            </div>

            <div className="p-8 rounded-2xl bg-card border hover:shadow-lg transition-shadow">
              <div className="h-12 w-12 rounded-lg gradient-primary flex items-center justify-center mb-4">
                <Users className="h-6 w-6 text-white" />
              </div>
              <h3 className="mb-3">チームコラボレーション</h3>
              <p className="text-muted-foreground">
                ロールベースのアクセスとプロジェクト管理ツールでシームレスに協働。
              </p>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
};

export default Hero;
