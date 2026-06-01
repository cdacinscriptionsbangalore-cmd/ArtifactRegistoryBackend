import { useContext, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { authClient } from "@/utils/http/clients/authClient.client";
import AuthContext from "@/context/AuthContext";
import cdacRoundLogo from '@/assets/cdacroundlogo.png';
import { getPostLoginRedirect } from "@/utils/postLoginRedirect";

const MAX_REFRESH_RETRIES = 3;
const RETRY_DELAY_MS = 700;
const OAUTH_CALLBACK_GUARD_KEY = "auth:oauth-callback-processed";

const wait = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

const OAuthCallback = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { loginSuccess } = useContext(AuthContext);

  useEffect(() => {
    // React StrictMode runs effects twice in development.
    // Guard prevents a second callback pass from overriding the first successful redirect.
    if (sessionStorage.getItem(OAUTH_CALLBACK_GUARD_KEY) === "1") {
      return;
    }
    sessionStorage.setItem(OAUTH_CALLBACK_GUARD_KEY, "1");

    const navigateToLoginWithNext = () => {
      sessionStorage.removeItem(OAUTH_CALLBACK_GUARD_KEY);
      const next = getPostLoginRedirect();
      if (next) {
        navigate(`/login?next=${encodeURIComponent(next)}`, { replace: true });
      } else {
        navigate("/login", { replace: true });
      }
    };

    const completeLogin = async () => {
      try {
        const status = searchParams.get("status");
        const flow = searchParams.get("flow");

        if (status === "pending" && flow === "admin_register") {
          navigate("/login?admin_request=pending", { replace: true });
          return;
        }

        if (status === "denied" && flow === "admin_login") {
          navigate("/login?admin_access=denied", { replace: true });
          return;
        }

        if (status && status !== "success") {
          throw new Error(`OAuth callback returned unsupported status: ${status}`);
        }

        let accessToken: string | null = null;
        let lastError: unknown = null;

        for (let attempt = 1; attempt <= MAX_REFRESH_RETRIES; attempt++) {
          try {
            console.log(`OAuthCallback: refresh attempt ${attempt}/${MAX_REFRESH_RETRIES}`);
            const res = await authClient.post("/oauth2/authenticated/refresh-token");
            console.log("OAuthCallback: refresh response:", res && res.data);

            accessToken = res?.data?.data?.accessToken || res?.data?.auth_token || res?.data?.token || null;
            console.log("OAuthCallback: computed accessToken:", accessToken);

            if (accessToken) break;
            lastError = new Error("No access token found in refresh response");
          } catch (error) {
            lastError = error;
            console.warn(`OAuthCallback: refresh attempt ${attempt} failed`, {
              message: (error as any)?.message,
              status: (error as any)?.response?.status,
            });
          }

          if (attempt < MAX_REFRESH_RETRIES) {
            await wait(RETRY_DELAY_MS * attempt);
          }
        }

        if (accessToken) {
          loginSuccess(accessToken);

          const savedRedirect = getPostLoginRedirect();

          if (savedRedirect && savedRedirect.startsWith("/") && !savedRedirect.startsWith("//")) {
            navigate(savedRedirect, { replace: true });
          } else {
            navigate("/home", { replace: true });
          }
        } else {
          throw lastError || new Error("OAuth callback failed without an access token");
        }
      } catch (error) {
        console.error("Error completing OAuth login:", {
          message: (error as any)?.message,
          response: (error as any)?.response?.data,
          status: (error as any)?.response?.status,
        });
        navigateToLoginWithNext();
      }
    };

    completeLogin();
  }, [loginSuccess, navigate, searchParams]);

  return (
    <div className="min-h-screen bg-secondary-background flex items-center justify-center">
      <div className='flex flex-col items-center'>
        {/* <FaSpinner className="animate-spin text-4xl text-[#66B0FF]" /> */}
        <img src={cdacRoundLogo} className="mr-3 mb-4 size-20 cdacSpinner" />
        <div className="text-[#000000] text-lg">Logging in...</div>
      </div>
    </div>
  )

};

export default OAuthCallback;
