import type { AttachmentSource } from "../../shared/useAttachment";
export type PaymentNotificationStatus =
  "PENDING" | "REVIEWED" | "REJECTED" | "WITHDRAWN" | "RECORDED";

export type PaymentNotification = {
  id: string;
  version?: number;
  unitId: string;
  amountMinor: number;
  paymentDate: string;
  reference: string;
  note: string;
  receipt?: AttachmentSource;
  status: PaymentNotificationStatus;
  reviewNote: string;
};

export const maxReceiptBytes = 5 * 1024 * 1024;

// Monetary input is converted to integer minor units without floating-point multiplication.
export function parseAmountMinor(input: string): number | null {
  const normalized = input.trim().replace(",", ".");
  if (!/^\d{1,7}(\.\d{1,2})?$/.test(normalized)) return null;
  const [whole, fraction = ""] = normalized.split(".");
  const amount = Number(whole) * 100 + Number(fraction.padEnd(2, "0"));
  return Number.isSafeInteger(amount) && amount > 0 ? amount : null;
}

export function validReceipt(file: File): boolean {
  const supported = {
    "application/pdf": /\.pdf$/i,
    "image/png": /\.png$/i,
    "image/jpeg": /\.jpe?g$/i,
  };
  const extension = supported[file.type as keyof typeof supported];
  return (
    !!extension &&
    extension.test(file.name) &&
    file.size > 0 &&
    file.size <= maxReceiptBytes
  );
}
