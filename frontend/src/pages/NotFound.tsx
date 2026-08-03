import { useLocation, useNavigate } from "react-router-dom";
import { useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext"; 
import { getHomePath } from "@/utils/getHomePath";
import { logger } from "@/lib/logger";
import { Button } from "@/components/ui/button";

const NotFound = () => {
  const location = useLocation();
  const { user } = useAuth(); 
  const navigate  = useNavigate(); 

  const destination = getHomePath(user); 

  useEffect(() => {
    logger.error("404:", location.pathname);
  }, [location.pathname]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4">
      <div className="text-center">
        <h1 className="mb-4 text-4xl font-bold">404</h1>
        <p className="mb-4 text-xl text-foreground">ページが見つかりません</p>
        <Button
          type="button"
          variant="link"
          onClick={() => navigate(destination, { replace: true })}
        >
          ホームに戻る
        </Button>
      </div>
    </div>
  );
};

export default NotFound;
