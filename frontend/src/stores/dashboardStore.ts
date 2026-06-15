import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { Project, ProjectDetail } from '@/api/projects';
import type { Site } from '@/api/site';
import type { UserList } from '@/api/users';
import * as siteApi from '@/api/site';
import * as userApi from '@/api/users/list';
import { logger } from '@/lib/logger';


interface DashboardState {
  sites: Site[];
  setSites: (sites: Site[]) => void;
  editSite: (siteId: string, updatedData: Partial<Site>) => void;
  deleteSiteStore: (siteId: string) => void;
  getSiteById: (id: string) => Site | undefined;
  getUserSites: (userId: string) => Site[];
  getMasterSites: () => Site[];
  joinableSites: () => Site[];

  users: UserList[];
  setUsers: (users: UserList[]) => void;
  editUser: (userId: string, updatedData: Partial<UserList>) => void;
  deleteUserStore: (userId: string) => void;
  getUser: (userId: string) => UserList | undefined;

  projects: Project[];
  loading: boolean;

  currentProject: Project | ProjectDetail | null;

  fetchDashboardData: (force?: boolean) => Promise<void>;
  addSite: (data: Site) => void;
  addUser: (data: UserList) => void;
  resetDashboard: () => void; // clear store (used on logout)
  setCurrentProject: (project: Project | ProjectDetail) => void;
}

export const useDashboardStore = create<DashboardState>()(
  persist(
    (set, get) => ({
      sites: [],
      users: [],
      projects: [],

      loading: false,
      currentProject: null,

      setCurrentProject: (project) => set({ currentProject: project }),

      // Fetch only if empty
      fetchDashboardData: async (force = false) => {
        const { sites, users } = get();

        const hasUsers = Array.isArray(users) && users.length > 0;
        const hasSites = Array.isArray(sites) && sites.length > 0;

        if (!force && (hasSites || hasUsers)) return;

        set({ loading: true });

        try {
          const [siteResult, userResult] = await Promise.allSettled([
            siteApi.getSites(),
            userApi.getUsers(),
          ]);

          set((state) => ({
            sites:
              siteResult.status === "fulfilled"
                ? siteResult.value.sites
                : state.sites,
            users:
              userResult.status === "fulfilled"
                ? userResult.value.users
                : state.users,
          }));

          if (siteResult.status === "rejected") {
            logger.error("Failed to fetch sites:", siteResult.reason);
          }
          if (userResult.status === "rejected") {
            logger.error("Failed to fetch users:", userResult.reason);
          }
        } catch (error) {
          logger.error("Failed to fetch dashboard data:", error);
        } finally {
          set({ loading: false });
        }
      },

      // Add new site to the store
      addSite: (data) => {
        set({
          sites: [...get().sites, data],
        });
      },

      setSites: (sites) => set({ sites }),

      editSite: (siteId, updatedData) => {
        set((state) => ({
          sites: state.sites.map((site) =>
            site.id === siteId ? { ...site, ...updatedData } : site
          ),
        }));
      },

      deleteSiteStore: (siteId: string) => {
        set((state) => ({
          sites: state.sites.filter(site => site.id !== siteId), //remove deleted user
        }));
      },

      getSiteById: (siteId: string) => get().sites.find(s => s.id === siteId),

      getMasterSites: () => {
        const { users, sites } = get();

        const master = users.find(u => u.role === 'master');
        if (!master) return [];

        const assignedSiteIds = new Set( //collect all siteIds assigned to non-master users
          users
            .filter(u => u.role !== 'master')
            .map(u => u.siteId)
            .filter(Boolean) //remove nulls
        );

        return sites.filter(
          site =>
            (master.siteId && site.id === master.siteId) ||
            !assignedSiteIds.has(site.id)
        );
      },

      joinableSites() {
        const { sites, users } = get();

        const takenSiteIds = new Set(
          users
            .filter((u) => u.siteId !== null) 
            .map((u) => u.siteId),
        );

        return sites.filter((s) => !takenSiteIds.has(s.id));
      },


      // Add new user to the store
      addUser: (data) => {
        set({
          users: [...get().users, data],
        });
      },

      setUsers: (users) => set({ users }),

      editUser: (userId, updatedData) =>
        set((state) => ({
          users: state.users.map((user) =>
            user.id === userId ? { ...user, ...updatedData } : user
          ),
        })),

      getUser: (id: string) => {
        return get().users.find((user) => user.id === id);
      },

      deleteUserStore: (userId: string) => {
        const state = get();

        //find master
        const masterUser = state.users.find(u => u.role === "master");
        if (!masterUser) {
          logger.error("Master user was not found while deleting user");
          return;
        }
        const masterId = masterUser.id;

        set((state) => ({
          users: state.users.filter(user => user.id !== userId), //remove deleted user
          sites: state.sites.map(s => //finds previously assigned site and assigns to master user
            s.picId === userId ? { ...s, picId: masterId } : s
          ),
        }));
      },



      //call API for delete User
      // try {
      //   await dashboardApi.deleteUser(userId);
      //   // success, no further action needed
      // } catch (err) {
      //   // rollback on error
      //   set({ users: prevUsers });
      //   console.error("Failed to delete user:", err);
      // }


      getUserSites: (userId: string) => {
        const { users, sites } = get();
        const user = users.find(u => u.id === userId);
        if (!user) return [];

        if (user.role === "master") { //gets master sites
          const assignedSiteIds = new Set( //collect all siteIds assigned to non-master users
            users
              .filter(u => u.role !== 'master')
              .map(u => u.siteId)
              .filter(Boolean) //remove nulls
          );
          return sites.filter(
            site =>
              (user.siteId && site.id === user.siteId) ||
              !assignedSiteIds.has(site.id)
          );
        } else {
          //for　ユーザ: just their assigned site
          return sites.filter(site => site.id === user.siteId);
        }
      },

      addProject: (data) => set({ projects: [...get().projects, data] }),

      // Clear the store (run on logout)
      resetDashboard: () => set({
        sites: [],
        users: [],
        projects: [],
        currentProject: null,
        loading: false,
      }),

    }),
    {
      name: 'dashboard-storage', // localStorage key
    }
  )
);
