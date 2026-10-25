import React, { useState } from 'react';
import { Users, Plus, Edit2, Trash2, ArrowRightLeft, History } from 'lucide-react';
import { FamilyMember, Transaction } from '../../lib/types';
import { formatCurrency } from '../../lib/calculations';

interface FamilyHubViewProps {
  familyMembers: FamilyMember[];
  transactions: Transaction[];
  onAddFamilyMember: (name: string, relationship: string) => Promise<any>;
  onUpdateFamilyMember: (member: FamilyMember) => Promise<any>;
  onDeleteFamilyMember: (id: number) => Promise<any>;
  onNavigateToFilteredTransactions: (isFamily: boolean, id: number) => void;
  onOpenTransferModalWithMember?: (memberId: number) => void;
}

export const FamilyHubView: React.FC<FamilyHubViewProps> = ({
  familyMembers,
  transactions,
  onAddFamilyMember,
  onUpdateFamilyMember,
  onDeleteFamilyMember,
  onNavigateToFilteredTransactions,
  onOpenTransferModalWithMember,
}) => {
  const [showAddModal, setShowAddModal] = useState(false);
  const [editingMember, setEditingMember] = useState<FamilyMember | null>(null);
  const [name, setName] = useState('');
  const [relationship, setRelationship] = useState('Spouse');

  const relationshipOptions = ['Spouse', 'Parent', 'Child', 'Sibling', 'Relative', 'Other'];

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;

    if (editingMember) {
      await onUpdateFamilyMember({
        ...editingMember,
        name: name.trim(),
        relationship,
      });
    } else {
      await onAddFamilyMember(name.trim(), relationship);
    }

    setShowAddModal(false);
    setEditingMember(null);
    setName('');
  };

  return (
    <div className="space-y-6">
      {/* ── Header ───────────────────────────────────────────────────── */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-gray-900 dark:text-white">Family Financial Hub</h2>
          <p className="text-xs text-gray-500 dark:text-darkMuted mt-0.5">
            Manage shared finances, track inter-family transfers, and audit mutual balances.
          </p>
        </div>
        <button
          onClick={() => {
            setName('');
            setRelationship('Spouse');
            setShowAddModal(true);
          }}
          className="flex items-center gap-2 px-4 py-2 rounded-xl bg-finance-blue text-white text-xs font-bold shadow hover:bg-finance-blueDark transition-all"
        >
          <Plus size={16} />
          <span>Add Member</span>
        </button>
      </div>

      {/* ── Member Cards Grid ───────────────────────────────────────── */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
        {familyMembers.length === 0 ? (
          <div className="col-span-full py-12 text-center text-xs text-gray-500 dark:text-darkMuted border border-dashed border-lightBorder dark:border-darkBorder rounded-2xl">
            No family members added yet. Click "+ Add Member" to begin tracking shared family expenses!
          </div>
        ) : (
          familyMembers.map((member) => {
            const memberFamilyTxs = transactions.filter(
              (t) => t.family_member_id === member.id && t.type === 'FAMILY_TRANSFER'
            );
            const totalGiven = memberFamilyTxs
              .filter((t) => t.direction === 'GIVEN_TO_THEM')
              .reduce((sum, t) => sum + Math.abs(t.amount || 0), 0);

            const totalReceived = memberFamilyTxs
              .filter((t) => t.direction === 'RECEIVED_FROM_THEM')
              .reduce((sum, t) => sum + Math.abs(t.amount || 0), 0);

            return (
              <div
                key={member.id}
                className="p-5 rounded-2xl bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder shadow-sm flex flex-col justify-between space-y-4"
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-11 h-11 rounded-full bg-finance-blue/10 dark:bg-finance-blue/20 text-finance-blue font-bold flex items-center justify-center text-base">
                      {member.name.charAt(0).toUpperCase()}
                    </div>
                    <div>
                      <h4 className="font-bold text-base text-gray-900 dark:text-white">
                        {member.name}
                      </h4>
                      <span className="inline-block px-2 py-0.5 mt-0.5 rounded-full text-[10px] font-semibold bg-gray-100 dark:bg-darkCard text-gray-700 dark:text-gray-300">
                        {member.relationship}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-1">
                    <button
                      onClick={() => {
                        setEditingMember(member);
                        setName(member.name);
                        setRelationship(member.relationship);
                      }}
                      className="p-1 rounded text-gray-400 hover:text-finance-blue"
                      title="Edit"
                    >
                      <Edit2 size={14} />
                    </button>
                    <button
                      onClick={() => onDeleteFamilyMember(member.id)}
                      className="p-1 rounded text-gray-400 hover:text-red-500"
                      title="Delete"
                    >
                      <Trash2 size={14} />
                    </button>
                  </div>
                </div>

                {/* Financial Summary */}
                <div className="grid grid-cols-2 gap-2 pt-2 border-t border-lightBorder dark:border-darkBorder/60">
                  <div className="p-2.5 rounded-xl bg-gray-50 dark:bg-darkCard/50">
                    <p className="text-[10px] text-gray-500 dark:text-darkMuted uppercase font-semibold">
                      Given
                    </p>
                    <p className="text-xs font-bold text-finance-red">
                      {formatCurrency(totalGiven)}
                    </p>
                  </div>
                  <div className="p-2.5 rounded-xl bg-gray-50 dark:bg-darkCard/50">
                    <p className="text-[10px] text-gray-500 dark:text-darkMuted uppercase font-semibold">
                      Received
                    </p>
                    <p className="text-xs font-bold text-finance-green">
                      {formatCurrency(totalReceived)}
                    </p>
                  </div>
                </div>

                {/* Action Buttons */}
                <div className="flex items-center gap-2 pt-1">
                  <button
                    onClick={() => onNavigateToFilteredTransactions(true, member.id)}
                    className="flex-1 py-1.5 px-3 rounded-lg border border-lightBorder dark:border-darkBorder text-gray-700 dark:text-darkMuted text-xs font-semibold hover:bg-gray-50 dark:hover:bg-darkCard flex items-center justify-center gap-1.5 transition-colors"
                  >
                    <History size={13} />
                    <span>View History</span>
                  </button>
                  {onOpenTransferModalWithMember && (
                    <button
                      onClick={() => onOpenTransferModalWithMember(member.id)}
                      className="py-1.5 px-3 rounded-lg bg-finance-blue text-white text-xs font-bold shadow hover:bg-finance-blueDark transition-all flex items-center gap-1"
                    >
                      <ArrowRightLeft size={13} />
                      <span>Transfer</span>
                    </button>
                  )}
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* ── Add / Edit Member Modal ───────────────────────────────────── */}
      {(showAddModal || editingMember) && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-darkSurface border border-lightBorder dark:border-darkBorder rounded-2xl w-full max-w-sm p-6 shadow-2xl space-y-4">
            <h3 className="font-bold text-base text-gray-900 dark:text-white">
              {editingMember ? 'Edit Family Member' : 'Add Family Member'}
            </h3>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
                  Full Name
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Priya Jivani"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-semibold"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-gray-700 dark:text-gray-300 mb-1">
                  Relationship
                </label>
                <select
                  value={relationship}
                  onChange={(e) => setRelationship(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-lightBorder dark:border-darkBorder bg-gray-50 dark:bg-darkCard text-xs font-semibold"
                >
                  {relationshipOptions.map((opt) => (
                    <option key={opt} value={opt}>
                      {opt}
                    </option>
                  ))}
                </select>
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => {
                    setShowAddModal(false);
                    setEditingMember(null);
                  }}
                  className="px-3.5 py-1.5 rounded-lg border border-lightBorder dark:border-darkBorder text-xs font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-1.5 rounded-lg bg-finance-blue text-white text-xs font-bold shadow hover:bg-finance-blueDark"
                >
                  Save Member
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
