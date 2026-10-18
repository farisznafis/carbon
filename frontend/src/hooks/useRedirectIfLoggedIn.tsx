import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";
import { getHomePath } from "@/utils/getHomePath";

/** Redirects an authenticated account from public auth pages to its role home. */
export function useRedirectIfLoggedIn(enabled = true) {
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuth();

  useEffect(() => {
    if (enabled && isAuthenticated && user?.id) {
      navigate(getHomePath(user), { replace: true });
    }
  }, [enabled, isAuthenticated, user, navigate]);
}
