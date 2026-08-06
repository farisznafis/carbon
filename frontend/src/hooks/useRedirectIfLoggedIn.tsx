import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext"; // your auth context
import { paths } from "@/routes/paths";

export function useRedirectIfLoggedIn(enabled = true) {
  const navigate = useNavigate();
  const { user } = useAuth();

  useEffect(() => {
    if(enabled && user?.id) {
      if(user?.role == "master") {
        navigate(paths.masterDashboard, {replace: true}); 
      } else if (user.siteId) {
        navigate(paths.siteDashboard(user.siteId), {replace: true});
      } else {
        navigate(paths.userDashboard, {replace: true});
      }
    }
  }, [enabled, user, navigate]);
}
  
