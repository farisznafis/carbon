import { createRoot } from "react-dom/client";
import App from "./App.tsx";
import "./index.css";
import { TooltipProvider } from "@radix-ui/react-tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ApiError } from '@/api/_shared/errors';
import React from "react";
import { BrowserRouter } from "react-router-dom";
import { AuthProvider } from "./contexts/AuthContext.tsx";
import { SignupProvider } from "./contexts/SignupContext.tsx";
import { UsersProvider } from "./contexts/UsersContext.tsx";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      /** Avoids repeating requests that the backend has explicitly forbidden. */
      retry: (failureCount, error) =>
        !(error instanceof ApiError && error.status === 403) && failureCount < 3,
    },
  },
});

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
