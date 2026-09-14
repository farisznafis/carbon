import { createRoot } from "react-dom/client";
import App from "./App.tsx";
import "./index.css";
import { TooltipProvider } from "@radix-ui/react-tooltip";
import { QueryClientProvider } from "@tanstack/react-query";
import React from "react";
import { BrowserRouter } from "react-router-dom";
import { AuthProvider } from "./contexts/AuthContext.tsx";
import { SignupProvider } from "./contexts/SignupContext.tsx";
import { UsersProvider } from "./contexts/UsersContext.tsx";
import { queryClient } from '@/lib/queryClient';

createRoot(document.getElementById("root")).render(
    <React.StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <QueryClientProvider client={queryClient}>
          <TooltipProvider>
            <SignupProvider>
              <UsersProvider>
                <App />
              </UsersProvider>
            </SignupProvider>
          </TooltipProvider>
        </QueryClientProvider>
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>
);
