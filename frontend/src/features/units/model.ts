export type Unit = {
  id: string;
  label: string;
  block: string;
  blockLabel?: string;
  floor: number;
  kind: "APARTMENT" | "SHOP" | "OFFICE";
  areaSqm: number;
};
export type Person = { id: string; name: string };
export type UnitRelationship = {
  id: string;
  unitId: string;
  personId: string;
  type: "OWNER" | "TENANT";
  startsOn: string;
  endsOn: string | null;
};
export type UnitCharge = {
  id: string;
  unitId: string;
  descriptionKey: string;
  period: string;
  dueOn: string;
  amountMinor: number;
};
export type RecordedPayment = {
  id: string;
  unitId: string;
  paidOn: string;
  amountMinor: number;
  allocations: { chargeId: string; amountMinor: number }[];
};
export type CommunitySnapshot = {
  communityName?: string;
  accounts?: import("../../api/generated").UnitAccount[];
  asOf: string;
  units: Unit[];
  people: Person[];
  relationships: UnitRelationship[];
  charges: UnitCharge[];
  payments: RecordedPayment[];
};

export function isCurrent(relationship: UnitRelationship, asOf: string) {
  // Relationship date boundaries are inclusive.
  return (
    relationship.startsOn <= asOf &&
    (!relationship.endsOn || relationship.endsOn >= asOf)
  );
}

export function unitSummary(snapshot: CommunitySnapshot, unitId: string) {
  const charges = snapshot.charges.filter((charge) => charge.unitId === unitId);
  const payments = snapshot.payments.filter(
    (payment) => payment.unitId === unitId,
  );
  const chargedMinor = charges.reduce(
    (total, charge) => total + charge.amountMinor,
    0,
  );
  const paidMinor = payments.reduce(
    (total, payment) => total + payment.amountMinor,
    0,
  );
  const allocatedMinor = payments
    .flatMap((payment) => payment.allocations)
    .reduce((total, allocation) => total + allocation.amountMinor, 0);
  const server = snapshot.accounts?.find((item) => item.unitId === unitId);
  return {
    charges,
    payments,
    chargedMinor: server?.chargedMinor ?? chargedMinor,
    paidMinor: server?.paidMinor ?? paidMinor,
    outstandingMinor: server?.outstandingMinor ?? chargedMinor - allocatedMinor,
    creditMinor: server?.creditMinor ?? paidMinor - allocatedMinor,
  };
}
