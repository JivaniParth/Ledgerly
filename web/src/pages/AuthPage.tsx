import React, { useState } from 'react';
import { Lock, Mail, User, ShieldCheck, ArrowRight, AlertCircle, CheckCircle2, X } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { supabase } from '../lib/supabase';

export const AuthPage: React.FC = () => {
  const { login, signup } = useAuth();
  const [isLoginTab, setIsLoginTab] = useState<boolean>(true);

  // Form states
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Forgot password states
  const [showForgotModal, setShowForgotModal] = useState(false);
  const [forgotEmail, setForgotEmail] = useState('');
  const [forgotLoading, setForgotLoading] = useState(false);
  const [forgotStatus, setForgotStatus] = useState<{ success: boolean; msg: string } | null>(null);

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!forgotEmail) return;
    setForgotLoading(true);
    setForgotStatus(null);
    try {
      const { error: resetErr } = await supabase.auth.resetPasswordForEmail(forgotEmail);
      if (resetErr) {
        setForgotStatus({ success: false, msg: resetErr.message });
      } else {
        setForgotStatus({ success: true, msg: 'Password reset link sent to your email!' });
      }
    } catch (err: any) {
      setForgotStatus({ success: false, msg: err.message || 'Failed to send reset link.' });
    } finally {
      setForgotLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccessMsg(null);
    setLoading(true);

    if (isLoginTab) {
      const res = await login(email, password);
      setLoading(false);
      if (!res.success) {
        setError(res.error || 'Invalid credentials or login failed.');
      }
    } else {
      if (password !== confirmPassword) {
        setError('Passwords do not match.');
        setLoading(false);
        return;
      }
      if (password.length < 6) {
        setError('Password must be at least 6 characters.');
        setLoading(false);
        return;
      }

      const res = await signup(email, password, name);
      setLoading(false);
      if (res.success) {
        setSuccessMsg('Account created successfully! Logging in...');
      } else {
        setError(res.error || 'Sign up failed.');
      }
    }
  };

  return (
    <div className="min-h-screen bg-lightBg dark:bg-darkBg flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder rounded-3xl p-8 shadow-xl space-y-6">
        {/* App Logo & Header */}
        <div className="text-center space-y-2">
          <div className="w-14 h-14 rounded-2xl bg-finance-blue text-white flex items-center justify-center font-bold text-2xl shadow-lg mx-auto">
            L
          </div>
          <h1 className="text-2xl font-extrabold tracking-tight text-gray-900 dark:text-white">
            Ledgerly
          </h1>
          <p className="text-xs text-gray-500 dark:text-darkMuted">
            Personal Finance Management & Family Ledger
          </p>
        </div>

        {/* Tab Toggle */}
        <div className="flex bg-gray-100 dark:bg-darkCard p-1 rounded-xl">
          <button
            type="button"
            onClick={() => {
              setIsLoginTab(true);
              setError(null);
            }}
            className={`flex-1 py-2 text-xs font-bold rounded-lg transition-all ${
              isLoginTab
                ? 'bg-white dark:bg-darkSurface text-gray-900 dark:text-white shadow-sm'
                : 'text-gray-500 hover:text-gray-900 dark:hover:text-white'
            }`}
          >
            Sign In
          </button>
          <button
            type="button"
            onClick={() => {
              setIsLoginTab(false);
              setError(null);
            }}
            className={`flex-1 py-2 text-xs font-bold rounded-lg transition-all ${
              !isLoginTab
                ? 'bg-white dark:bg-darkSurface text-gray-900 dark:text-white shadow-sm'
                : 'text-gray-500 hover:text-gray-900 dark:hover:text-white'
            }`}
          >
            Create Account
          </button>
        </div>

        {error && (
          <div className="p-3.5 rounded-xl bg-red-500/10 border border-red-500/20 text-red-600 dark:text-red-400 text-xs flex items-center gap-2">
            <AlertCircle size={16} />
            <span>{error}</span>
          </div>
        )}

        {successMsg && (
          <div className="p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-600 dark:text-emerald-400 text-xs flex items-center gap-2">
            <CheckCircle2 size={16} />
            <span>{successMsg}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {!isLoginTab && (
            <div>
              <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
                Display Name
              </label>
              <div className="relative">
                <User size={16} className="absolute left-3.5 top-3 text-gray-400" />
                <input
                  type="text"
                  required
                  placeholder="e.g. Parth"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full pl-10 pr-3.5 py-2.5 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-medium focus:outline-none focus:ring-2 focus:ring-finance-blue"
                />
              </div>
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
              Email Address
            </label>
            <div className="relative">
              <Mail size={16} className="absolute left-3.5 top-3 text-gray-400" />
              <input
                type="email"
                required
                placeholder="name@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full pl-10 pr-3.5 py-2.5 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-medium focus:outline-none focus:ring-2 focus:ring-finance-blue"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
              Password
            </label>
            <div className="relative">
              <Lock size={16} className="absolute left-3.5 top-3 text-gray-400" />
              <input
                type="password"
                required
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full pl-10 pr-3.5 py-2.5 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-medium focus:outline-none focus:ring-2 focus:ring-finance-blue"
              />
            </div>
            {isLoginTab && (
              <div className="flex justify-end mt-1">
                <button
                  type="button"
                  onClick={() => {
                    setForgotEmail(email);
                    setForgotStatus(null);
                    setShowForgotModal(true);
                  }}
                  className="text-[11px] font-semibold text-finance-blue hover:underline"
                >
                  Forgot password?
                </button>
              </div>
            )}
          </div>

          {!isLoginTab && (
            <div>
              <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
                Confirm Password
              </label>
              <div className="relative">
                <Lock size={16} className="absolute left-3.5 top-3 text-gray-400" />
                <input
                  type="password"
                  required
                  placeholder="••••••••"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  className="w-full pl-10 pr-3.5 py-2.5 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-medium focus:outline-none focus:ring-2 focus:ring-finance-blue"
                />
              </div>
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 rounded-xl bg-finance-blue hover:bg-finance-blueDark text-white text-xs font-bold shadow-md transition-all flex items-center justify-center gap-2 disabled:opacity-50"
          >
            <span>{loading ? 'Please wait...' : isLoginTab ? 'Sign In to Ledgerly' : 'Create Account'}</span>
            <ArrowRight size={15} />
          </button>
        </form>

        <div className="p-3 rounded-xl bg-gray-50 dark:bg-darkCard/50 border border-lightBorder dark:border-darkBorder/60 text-[11px] text-gray-500 dark:text-darkMuted flex items-center gap-2">
          <ShieldCheck size={16} className="text-finance-blue shrink-0" />
          <span>
            Connected to your Ledgerly Supabase backend. All data syncs in real-time with your phone.
          </span>
        </div>
      </div>

      {/* Forgot Password Modal */}
      {showForgotModal && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="w-full max-w-sm bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder rounded-2xl p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-bold text-gray-900 dark:text-white">Reset Password</h3>
              <button
                type="button"
                onClick={() => setShowForgotModal(false)}
                className="text-gray-400 hover:text-gray-600 dark:hover:text-white"
              >
                <X size={18} />
              </button>
            </div>
            <p className="text-xs text-gray-500 dark:text-darkMuted">
              Enter your email address and we'll send you a link to reset your password.
            </p>
            <form onSubmit={handleResetPassword} className="space-y-3">
              <input
                type="email"
                required
                placeholder="name@example.com"
                value={forgotEmail}
                onChange={(e) => setForgotEmail(e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-medium focus:outline-none focus:ring-2 focus:ring-finance-blue"
              />
              {forgotStatus && (
                <p
                  className={`text-xs font-medium ${
                    forgotStatus.success ? 'text-green-600 dark:text-green-400' : 'text-red-500'
                  }`}
                >
                  {forgotStatus.msg}
                </p>
              )}
              <div className="flex gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowForgotModal(false)}
                  className="flex-1 py-2 rounded-xl border border-lightBorder dark:border-darkBorder text-xs font-semibold text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-darkCard"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={forgotLoading}
                  className="flex-1 py-2 rounded-xl bg-finance-blue text-white text-xs font-bold hover:bg-finance-blueDark disabled:opacity-50"
                >
                  {forgotLoading ? 'Sending...' : 'Send Link'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
