import React, { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import api from "../../services/api";

const VerifyEmail = () => {
  const [searchParams] = useSearchParams();
  const [status, setStatus] = useState("verifying");
  const [message, setMessage] = useState("");

  useEffect(() => {
    const token = searchParams.get("token");

    if (!token) {
      setStatus("error");
      setMessage("No verification token found.");
      return;
    }

    const verify = async () => {
      try {
        const response = await api.get(`/auth/public/verify-email?token=${token}`);
        setStatus("success");
        setMessage(response.data.message);
      } catch (error) {
        setStatus("error");
        setMessage(
          error.response?.data?.message ||
            "Verification failed. The link may be expired or already used."
        );
      }
    };

    verify();
  }, [searchParams]);

  return (
    <div className="min-h-[calc(100vh-74px)] flex justify-center items-center">
      <div className="sm:w-[450px] w-[360px] shadow-custom py-8 sm:px-8 px-4 text-center">
        <h1 className="font-montserrat font-bold text-2xl mb-4">
          Email Verification
        </h1>

        {status === "verifying" && (
          <p className="text-slate-600">Verifying your email, please wait...</p>
        )}

        {status === "success" && (
          <>
            <p className="text-green-600 mb-6">{message}</p>
            <Link className="underline hover:text-black text-sm" to="/login">
              Go to Login
            </Link>
          </>
        )}

        {status === "error" && (
          <>
            <p className="text-red-500 mb-6">{message}</p>
            <Link className="underline hover:text-black text-sm" to="/login">
              Back to Login
            </Link>
          </>
        )}
      </div>
    </div>
  );
};

export default VerifyEmail;
