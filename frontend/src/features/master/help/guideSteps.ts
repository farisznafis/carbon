// guideSteps.ts

export type GuideStep = {
  title: string;
  description: string;
  videoSrc?: string;    // 🔁 Replace with your actual video path or URL
  videoPoster?: string; // 🔁 Optional: thumbnail shown before the video plays
};

export const GUIDE_STEPS: GuideStep[] = [
  {
    title: 'アカウントを作成する',
    description: 'サインアップして企業情報を入力するところから始めましょう。',
    videoSrc: '/videos/step-1.mp4',           // 🔁 Replace this path
    videoPoster: '/images/step-1-thumb.jpg',  // 🔁 Replace or remove
  },
  {
    title: 'Set up your profile',
    description: 'Add your logo, address, and contact information.',
    videoSrc: '/videos/step-2.mp4',           // 🔁 Replace this path
    videoPoster: '/images/step-2-thumb.jpg',  // 🔁 Replace or remove
  },
  {
    title: 'Invite your team',
    description: 'Send invitations to your team members to join the workspace.',
    videoSrc: '/videos/step-3.mp4',           // 🔁 Replace this path
    // videoPoster not set — thumbnail is optional
  },
];