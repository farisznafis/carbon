import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext"; // your auth context

export function useRedirectIfLoggedIn() {
  const navigate = useNavigate();
  const { user } = useAuth();

  useEffect(() => {
    if(user?.id) {
      if(user?.role == "master") {
        navigate("/master/dashboard", {replace: true}); 
      } else if (user.siteId) {
        navigate(`/sites/${user.siteId}/dashboard`, {replace: true});
      } else {
        navigate("/", {replace: true});
      }
    }
  }, [user, navigate]); 
}
  
