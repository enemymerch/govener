import type { AttachmentSource } from "../../shared/useAttachment";
export type Charge = {
  version?: number;
  allocatedMinor?: number;
  outstandingMinor?: number;
  status?: string;
  id: string;
  unitId: string;
  description: string;
  custom?: boolean;
  period: string;
  dueOn: string;
  amountMinor: number;
  cancelledAt?: string;
  reason?: string;
};
export type RecordedPayment = {
  version?: number;
  id: string;
  unitId: string;
  amountMinor: number;
  paidOn: string;
  accountId: string;
  reference: string;
  allocations: { chargeId: string; amountMinor: number }[];
  notificationId?: string;
  reversedAt?: string;
  reason?: string;
};
export type FinancialAccount = {
  version?: number;
  balanceMinor?: number;
  id: string;
  name: string;
  openingMinor: number;
  kind: "BANK" | "CASH";
};
export type Expense = {
  version?: number;
  paidMinor?: number;
  id: string;
  description: string;
  category: "MAINTENANCE" | "UTILITIES" | "CLEANING" | "OTHER";
  vendor: string;
  incurredOn: string;
  amountMinor: number;
  document?: AttachmentSource;
  cancelledAt?: string;
  reason?: string;
};
export type ExpensePayment = {
  version?: number;
  id: string;
  expenseId: string;
  accountId: string;
  amountMinor: number;
  paidOn: string;
  reversedAt?: string;
  reason?: string;
};
export type FinanceState = {
  charges: Charge[];
  payments: RecordedPayment[];
  expenses: Expense[];
  expensePayments: ExpensePayment[];
  accounts: FinancialAccount[];
};
export type FinanceCommand =
  | { type: "CHARGE_BATCH"; items: Omit<Charge, "id">[] }
  | { type: "RECORD_PAYMENT"; payment: Omit<RecordedPayment, "id"> }
  | {
      type: "LINK_PAYMENT";
      id: string;
      notificationId: string;
      unitId: string;
      amountMinor: number;
    }
  | { type: "REVERSE_PAYMENT"; id: string; reason: string }
  | { type: "CANCEL_CHARGE"; id: string; reason: string }
  | { type: "RECORD_EXPENSE"; expense: Omit<Expense, "id">; accountId?: string }
  | {
      type: "PAY_EXPENSE";
      expenseId: string;
      accountId: string;
      paidOn: string;
    }
  | { type: "REVERSE_EXPENSE_PAYMENT"; id: string; reason: string };
export function allocatedTo(state: FinanceState, chargeId: string) {
  return (
    state.charges.find((item) => item.id === chargeId)?.allocatedMinor ?? 0
  );
}
