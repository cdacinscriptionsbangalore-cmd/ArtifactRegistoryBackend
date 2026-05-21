import React from "react";
import { useLocation } from "react-router-dom";
import { setPostLoginRedirect } from "@/utils/postLoginRedirect";
// import { GoogleOAuthProvider, GoogleLogin } from "@react-oauth/google";
// import { jwtDecode } from "jwt-decode";
// import { redirect } from "react-router-dom";

const redirectURL = window._env_?.VITE_REDIRECT_URL || import.meta.env.VITE_REDIRECT_URL;
const adminLoginRedirectURL = window._env_?.VITE_ADMIN_LOGIN_REDIRECT_URL
  || import.meta.env.VITE_ADMIN_LOGIN_REDIRECT_URL
  || "/api/oauth2/admin/login";
const adminRegisterRedirectURL = window._env_?.VITE_ADMIN_REGISTER_REDIRECT_URL
  || import.meta.env.VITE_ADMIN_REGISTER_REDIRECT_URL
  || "/api/oauth2/admin/register";
const OAUTH_CALLBACK_GUARD_KEY = "auth:oauth-callback-processed";

const AuthPage: React.FC = () => {
  const location = useLocation();

  const getSafeRedirectPath = () => {
    const next = new URLSearchParams(location.search).get("next") || "";
    if (next.startsWith("/") && !next.startsWith("//")) {
      return next;
    }

    const from = location.state && typeof (location.state as { from?: unknown }).from === "string"
      ? (location.state as { from: string }).from
      : "";

    if (from.startsWith("/") && !from.startsWith("//")) {
      return from;
    }

    return null;
  };

  // const handleLoginSuccess = (credentialResponse: any) => {
  //   if (credentialResponse.credential) {
  //     const decoded: any = jwtDecode(credentialResponse.credential);
  //     console.log("User Info:", decoded);
  //     // You can send credentialResponse.credential to your backend for verification
  //   }
  // };

  // const handleLoginFailure = () => {
  //   console.error("Login Failed");
  // };
  const prepareOAuthRedirect = () => {
    const redirectPath = getSafeRedirectPath();

    if (redirectPath) {
      setPostLoginRedirect(redirectPath);
    }

    // Reset callback guard before initiating a new OAuth round-trip.
    sessionStorage.removeItem(OAUTH_CALLBACK_GUARD_KEY);
  };

  const handleGoogleLogin = () => {
    prepareOAuthRedirect();
    window.location.href = redirectURL;
  }

  const handleAdminLogin = () => {
    prepareOAuthRedirect();
    window.location.href = adminLoginRedirectURL;
  };

  const handleAdminRegister = () => {
    prepareOAuthRedirect();
    window.location.href = adminRegisterRedirectURL;
  };

  return (
    <div className="flex items-center justify-center" style={{ minHeight: "62vh" }}>
      <div className="bg-white shadow-lg rounded-2xl p-10 w-full max-w-md text-center">
        <h2 className="text-2xl font-bold text-gray-800 mb-6">
          Welcome Back 👋
        </h2>
        <p className="text-gray-600 mb-6">
          Sign in or create an account with Google
        </p>
        {/* <GoogleOAuthProvider clientId="962264895991-93et5a8stepe4osg77oj9gh0am4cc897.apps.googleusercontent.com">
          <GoogleLogin
            onSuccess={handleLoginSuccess}
            onError={handleLoginFailure}
            shape="pill"
            text="signin_with"
            width="100%"
          />
        </GoogleOAuthProvider> */}

        <button
          onClick={handleGoogleLogin}


          className={`
            w-full flex items-center justify-center gap-3 px-6 py-3 
            border border-gray-300 rounded-full text-gray-700 font-medium
            hover:bg-gray-50 hover:shadow-md transition-all duration-200
            focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2
            cursor-pointer
          `}
        >
          {/* Google Icon */}
          <svg
            className="w-5 h-5"
            viewBox="0 0 24 24"
          >
            <path
              fill="#4285F4"
              d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
            />
            <path
              fill="#34A853"
              d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
            />
            <path
              fill="#FBBC05"
              d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
            />
            <path
              fill="#EA4335"
              d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
            />
          </svg>

          {(
            <div className="flex items-center gap-2">
              {/* <div className="animate-spin rounded-full h-4 w-4 border-2 border-gray-300 border-t-gray-600"></div> */}
              <span>Continue With Google</span>
            </div>
          )}

        </button>

        <div className="mt-4 space-y-3">
          <button
            onClick={handleAdminLogin}
            className="w-full rounded-full border border-gray-300 px-6 py-3 font-medium text-gray-700 transition-all duration-200 hover:bg-gray-50 hover:shadow-md"
          >
            Continue As Admin
          </button>

          <button
            onClick={handleAdminRegister}
            className="w-full rounded-full border border-dashed border-gray-300 px-6 py-3 font-medium text-gray-700 transition-all duration-200 hover:bg-gray-50 hover:shadow-md"
          >
            Request Admin Access
          </button>
        </div>

        <div className="mt-6 text-sm text-gray-500">
          By continuing, you agree to our{" "}
          <a href="#" className="text-blue-500 underline">
            Terms of Service
          </a>{" "}
          and{" "}
          <a href="#" className="text-blue-500 underline">
            Privacy Policy
          </a>
        </div>
      </div>
    </div>
  );
};

export default AuthPage;
