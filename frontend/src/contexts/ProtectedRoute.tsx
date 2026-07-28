// ProtectedRoute.tsx
// Routes nested here require the user to be logged in to view

import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "@/contexts/AuthContext";

export const ProtectedRoute = () => {
  const { isLoggedIn } = useAuth();

  if (!isLoggedIn()) {
    return <Navigate to="/login" replace />;
  }

  // Render nested routes here
  return <Outlet />;
};
