// NavBar.stories.tsx
import type { Meta, StoryObj } from '@storybook/react';
import { Navbar } from './Navbar';
import { MemoryRouter } from 'react-router-dom';
import { AuthProvider, useAuth } from '@/contexts/AuthContext';

const meta: Meta<typeof Navbar> = {
  title: 'Components/NavBar',
  component: Navbar,
  decorators: [
    (Story) => (
      <MemoryRouter>
        <AuthProvider>
          <Story />
        </AuthProvider>
      </MemoryRouter>
    ),
  ],
  argTypes: {
    links: { control: 'object' },
    user: { control: 'object' }, // if NavBar uses user as a prop
    onLogout: { action: 'logout clicked' },
  },
};
export default meta;

const WithLoggedInUser = ({ children }: { children: React.ReactNode }) => {
  const { login } = useAuth(); // 実装に応じて
  login("1345", "test@test.com", "12345677891989893");
  return <>{children}</>;
};


type Story = StoryObj<typeof Navbar>;

const baseArgs = {
  links: [
    { label: 'Home', to: '/' },
    { label: 'Profile', to: '/profile' },
  ],
};

// Logged out
export const Guest: Story = {
  args: {
    ...baseArgs,
    user: null,
  },
};

// ログイン済み
export const LoggedIn: Story = {
  decorators: [
    (Story) => (
      <WithLoggedInUser>
        <Story />
      </WithLoggedInUser>
    ),
  ],
  
};