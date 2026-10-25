import React, { useState } from 'react';
import { Lock, Mail, CheckCircle2, AlertCircle, LogOut } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export const ProfileView: React.FC = () => {
  const { user, userName, updateDisplayName, updatePassword, logout } = useAuth();

  const [editingName, setEditingName] = useState(userName);
  const [isEditingName, setIsEditingName] = useState(false);
  const [nameSaving, setNameSaving] = useState(false);
  const [nameMsg, setNameMsg] = useState<string | null>(null);

  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordSaving, setPasswordSaving] = useState(false);
  const [passwordMsg, setPasswordMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(
    null
  );

  const handleSaveName = async () => {
    if (!editingName.trim()) return;
    setNameSaving(true);
    const res = await updateDisplayName(editingName.trim());
    setNameSaving(false);
    if (res.success) {
      setIsEditingName(false);
      setNameMsg('Display name updated successfully!');
      setTimeout(() => setNameMsg(null), 3000);
    }
  };

  const handleSavePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (newPassword.length < 6) {
      setPasswordMsg({ type: 'error', text: 'Password must be at least 6 characters long.' });
      return;
    }
    if (newPassword !== confirmPassword) {
      setPasswordMsg({ type: 'error', text: 'Passwords do not match.' });
      return;
    }

    setPasswordSaving(true);
    const res = await updatePassword(newPassword);
    setPasswordSaving(false);
    if (res.success) {
      setPasswordMsg({ type: 'success', text: 'Password updated successfully!' });
      setNewPassword('');
      setConfirmPassword('');
      setTimeout(() => setPasswordMsg(null), 4000);
    } else {
      setPasswordMsg({ type: 'error', text: res.error || 'Failed to update password.' });
    }
  };

  return (
    <div className="max-w-2xl space-y-6">
      <div>
        <h2 className="text-xl font-bold text-gray-900 dark:text-white">Account Profile</h2>
        <p className="text-xs text-gray-500 dark:text-darkMuted mt-0.5">
          Manage your account credentials and personal preferences.
        </p>
      </div>

      {/* ── Profile Details Card ─────────────────────────────────────── */}
      <div className="p-6 rounded-2xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder shadow-sm space-y-5">
        <div className="flex items-center gap-4">
          <div className="w-16 h-16 rounded-full bg-finance-blue text-white font-bold flex items-center justify-center text-2xl shadow-md">
            {userName.charAt(0).toUpperCase()}
          </div>
          <div>
            <h3 className="text-lg font-bold text-gray-900 dark:text-white">{userName}</h3>
            <p className="text-xs text-gray-500 dark:text-darkMuted flex items-center gap-1.5 mt-0.5">
              <Mail size={13} />
              <span>{user?.email || 'user@example.com'}</span>
            </p>
          </div>
        </div>

        {nameMsg && (
          <div className="p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-600 dark:text-emerald-400 text-xs flex items-center gap-2">
            <CheckCircle2 size={15} />
            <span>{nameMsg}</span>
          </div>
        )}

        <div className="pt-2 border-t border-lightBorder dark:border-darkBorder/60 space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold text-gray-700 dark:text-gray-300">Display Name</p>
              <p className="text-xs text-gray-500 dark:text-darkMuted">
                This name appears on family ledger items and shared transaction cards.
              </p>
            </div>
            {!isEditingName && (
              <button
                onClick={() => {
                  setEditingName(userName);
                  setIsEditingName(true);
                }}
                className="px-3.5 py-1.5 rounded-xl border border-lightBorder dark:border-darkBorder text-xs font-semibold text-gray-700 dark:text-darkMuted hover:bg-gray-50 dark:hover:bg-darkCard transition-colors"
              >
                Edit Name
              </button>
            )}
          </div>

          {isEditingName && (
            <div className="flex gap-2 pt-1">
              <input
                type="text"
                value={editingName}
                onChange={(e) => setEditingName(e.target.value)}
                className="flex-1 px-3 py-1.5 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-semibold"
              />
              <button
                onClick={() => setIsEditingName(false)}
                className="px-3 py-1.5 rounded-xl border border-lightBorder dark:border-darkBorder text-xs font-semibold"
              >
                Cancel
              </button>
              <button
                onClick={handleSaveName}
                disabled={nameSaving}
                className="px-4 py-1.5 rounded-xl bg-finance-blue text-white text-xs font-bold shadow disabled:opacity-50"
              >
                {nameSaving ? 'Saving...' : 'Save'}
              </button>
            </div>
          )}
        </div>
      </div>

      {/* ── Change Password Form ──────────────────────────────────────── */}
      <div className="p-6 rounded-2xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder shadow-sm space-y-4">
        <div className="flex items-center gap-2">
          <Lock size={18} className="text-finance-blue" />
          <h3 className="font-bold text-sm text-gray-900 dark:text-white">Security & Password</h3>
        </div>

        {passwordMsg && (
          <div
            className={`p-3 rounded-xl border text-xs flex items-center gap-2 ${
              passwordMsg.type === 'success'
                ? 'bg-emerald-500/10 border-emerald-500/20 text-emerald-600 dark:text-emerald-400'
                : 'bg-red-500/10 border-red-500/20 text-red-600 dark:text-red-400'
            }`}
          >
            {passwordMsg.type === 'success' ? <CheckCircle2 size={15} /> : <AlertCircle size={15} />}
            <span>{passwordMsg.text}</span>
          </div>
        )}

        <form onSubmit={handleSavePassword} className="space-y-3">
          <div>
            <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
              New Password
            </label>
            <input
              type="password"
              required
              placeholder="Minimum 6 characters"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-medium"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
              Confirm New Password
            </label>
            <input
              type="password"
              required
              placeholder="Re-enter new password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-medium"
            />
          </div>

          <div className="pt-2">
            <button
              type="submit"
              disabled={passwordSaving}
              className="px-4 py-2 rounded-xl bg-finance-blue hover:bg-finance-blueDark text-white text-xs font-bold shadow disabled:opacity-50"
            >
              {passwordSaving ? 'Updating Password...' : 'Update Password'}
            </button>
          </div>
        </form>
      </div>

      {/* ── Logout Section ───────────────────────────────────────────── */}
      <div className="p-6 rounded-2xl bg-red-500/5 dark:bg-red-500/10 border border-red-500/20 flex items-center justify-between">
        <div>
          <h4 className="font-bold text-sm text-red-600 dark:text-red-400">Sign Out of Ledgerly</h4>
          <p className="text-xs text-gray-500 dark:text-darkMuted mt-0.5">
            End your desktop session. Your credentials remain safe and synced on your phone.
          </p>
        </div>
        <button
          onClick={logout}
          className="px-4 py-2 rounded-xl bg-finance-red hover:bg-finance-redDark text-white text-xs font-bold shadow flex items-center gap-1.5 transition-colors"
        >
          <LogOut size={15} />
          <span>Log Out</span>
        </button>
      </div>
    </div>
  );
};
