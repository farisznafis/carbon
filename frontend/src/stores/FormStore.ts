import type { Site } from "@/api/site";
import type { User, UserProfile } from "@/api/users";
import { create } from "zustand";

export type TempUser = {
    id?: string; // optional for edits
    firstName?: string;
    firstNameKana?: string;
    lastName?: string;
    lastNameKana?: string;
    email?: string;
    phoneNumber?: string;
    position?: string;
};

export type tempSite = {
    id?: string;
    siteName?: string;
    postalCode?: string;
    prefectureName?: string;
    cityName?: string;
    blockName?: string; //町域・丁目
    addressDetail?: string;
    address?: string;
    picFirstName?: string;
    picLastName?: string;
    picEmail?: string;
    picPhoneNumber?: string;
    picPosition?: string;
    companyId?: string;
    project?: string[];
}

export type tempProfile = {
    id?: string; // optional for edits
    firstName?: string;
    firstNameKana?: string;
    lastName?: string;
    lastNameKana?: string;
    email?: string;
    phoneNumber?: string;
    position?: string;
}

// FormStore.ts
type UserFormStore = {
    tempUser: Partial<UserProfile> | null;
    setTempUser: (data: Partial<UserProfile>) => void;
    clearTempUser: () => void;
};

export const useUserFormStore = create<UserFormStore>((set) => ({
    tempUser: null,
    setTempUser: (data) => set({ tempUser: data }),
    clearTempUser: () => set({ tempUser: null }),
}));

type SiteFormStore = {
    tempSite: Partial<Site> | null;
    setTempSite: (data: Partial<Site>) => void; 
    clearTempSite: () => void;  
}

export const useSiteFormStore = create<SiteFormStore>((set) => ({
    tempSite: null,
    setTempSite: (data) => set({ tempSite: data }),
    clearTempSite: () => set({ tempSite: null }),
}));

type ProfileFormStore = {
    tempProfile: Partial<UserProfile> | null; 
    setTempProfile: (data: Partial<UserProfile>) => void,
    clearTempProfile: () => void,
}

export const useProfileStore = create<ProfileFormStore>((set) => ({
    tempProfile: null,
    setTempProfile: (data) => set({ tempProfile: data }),
    clearTempProfile: () => set({ tempProfile: null }),
}));


/* Explanation:
This code defines a Zustand store for managing temporary user form data. It includes:
- A `tempUser` state to hold the current form data.
- A `setTempUser` action to update the `tempUser` state.
- A `clearTempUser` action to reset the `tempUser` state to null.
This store can be used across components to persist user form data during multi-step forms or edits.
*/

/* example usage in user registration/edit components: 
const { tempUser, clearTempUser } = useFormStore();
const addUser = useUserStore((s) => s.addUser);       // for register
const updateUser = useUserStore((s) => s.updateUser); // for edit

const handleConfirmRegister = async () => {
  if (!tempUser) return;
  await addUser(tempUser);
  clearTempUser();
  navigate("/dashboard");
};

const handleConfirmEdit = async (id: string) => {
  if (!tempUser) return;
  await updateUser(id, tempUser);
  clearTempUser();
  navigate("/dashboard");
}; 
*/
