import { useLocation, useNavigate } from "react-router-dom";
import { useEffect } from "react";
import { useAuth } from "@/contexts/AuthContext"; 
import { getHomePath } from "@/utils/getHomePath";
import { logger } from "@/lib/logger";

const NotFound = () => {
  const location = useLocation();
  const { user } = useAuth(); 
  const navigate  = useNavigate(); 

  const destination = getHomePath(user); 

  useEffect(() => {
    logger.error("404:", location.pathname);
  }, [location.pathname]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-100">
      <div className="text-center">
        <h1 className="mb-4 text-4xl font-bold">404</h1>
        <p className="mb-4 text-xl text-gray-600">ページが見つかりません</p>
        <button
          onClick={() => navigate(destination, { replace: true })}
          className="text-blue-500 underline hover:text-blue-700"
        >
          ホームに戻る
        </button>
      </div>
    </div>
  );
};

export default NotFound;
