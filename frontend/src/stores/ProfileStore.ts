import { create } from 'zustand';
import type { ProfileCompanyViewModel, ProfileViewModel } from '@/api/profile';

interface ProfileStore {
  profile: ProfileViewModel | null;
  company: ProfileCompanyViewModel | null;
  loading: boolean;
  setProfile: (data: ProfileViewModel) => void;
  clearProfile: () => void;
  setCompany: (data: ProfileCompanyViewModel | null) => void;
  editCompany: (patch: Partial<ProfileCompanyViewModel>) => void;
  clearCompany: () => void;
}

export const useProfileStore = create<ProfileStore>((set) => ({
  profile: null,
  company: null,
  loading: false,

  setProfile: (data) =>
    set({
      profile: data,
      company: data.company,
    }),

  clearProfile: () => set({ profile: null, company: null }),

  setCompany: (data) => set({ company: data }),

  clearCompany: () => set({ company: null }),

  editCompany: (patch) =>
    set((state) =>
      state.company
        ? {
            company: {
              ...state.company,
              ...patch,
            },
          }
        : state,
    ),
}));
