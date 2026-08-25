import { useRedirectIfLoggedIn } from '@/hooks/useRedirectIfLoggedIn';
import { paths } from '@/routes/paths';
import { HomeBenefitsSection } from './home/HomeBenefitsSection';
import { HomeCtaSection } from './home/HomeCtaSection';
import { HomeFaqSection } from './home/HomeFaqSection';
import { HomeFlowSection } from './home/HomeFlowSection';
import { HomeFooter } from './home/HomeFooter';
import {
  HeroMediaFrame,
  HomeHeroSection,
  type HeroMediaSource,
} from './home/HomeHeroSection';
import { HomeMethodsSection } from './home/HomeMethodsSection';

export { HeroMediaFrame };
export type { HeroMediaSource };

/** Composes the public homepage and redirects authenticated users as before. */
const Hero = () => {
  useRedirectIfLoggedIn();

  return (
    <div className="min-h-dvh overflow-x-clip bg-card">
      <HomeHeroSection signupPath={paths.signup} />
      <HomeBenefitsSection />
      <HomeFlowSection />
      <HomeMethodsSection />
      <HomeFaqSection />
      <HomeCtaSection signupPath={paths.signup} />
      <HomeFooter />
    </div>
  );
};

export default Hero;
