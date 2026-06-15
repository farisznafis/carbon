import React, { createContext, useContext, useEffect, useState } from "react";
import type { Site } from "@/api/site";

type SiteContextValue = {
    site: Site;
    setSite: (site: Site) => void;
    clearSite: () => void;
};

const STORAGE_KEY = "assignedSite";

const SiteContext = createContext<SiteContextValue | undefined>(undefined);

export const SiteProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    // Initialize site state from localStorage
    const [site, setSiteState] = useState<Site>(() => {
        try {
            if (typeof window === "undefined") return null;
            const raw = localStorage.getItem(STORAGE_KEY);
            return raw ? (JSON.parse(raw) as Site) : null;
        } catch {
            return null;
        }
    });
    // Sync site state to localStorage
    useEffect(() => {
        try {
            if (site === null) {
                localStorage.removeItem(STORAGE_KEY);
            } else {
                localStorage.setItem(STORAGE_KEY, JSON.stringify(site));
            }
        } catch {
            // ignore storage errors
        }
    }, [site]);

    const setSite = (newSite: Site) => setSiteState(newSite);
    const clearSite = () => setSiteState(null);

    // Provide site context to children
    return (
        <SiteContext.Provider value={{ site, setSite, clearSite }}>
            {children}
        </SiteContext.Provider>
    );
};

export function useSite(): SiteContextValue {
    const ctx = useContext(SiteContext);
    if (!ctx) {
        throw new Error("useSite must be used within a SiteProvider");
    }
    return ctx;
}
