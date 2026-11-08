import { useEffect, useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { useSearchParams } from "wouter";
import { Plus, Wallet, X } from "lucide-react";
import { useWorkspace } from "../../workspace/Workspace";
import { useFormat } from "../../shared/useFormat";
import { parseAmountMinor, validReceipt } from "../payments/model";
import { allocatedTo, type FinanceCommand } from "./model";
import { repositories } from "../../api/repositories";
import { errorKey, financialWrite } from "../../api/server";
import { calendarDateInIstanbul } from "../../shared/calendar";
import { useSession } from "../../session/Session";
import AttachmentDownload from "../../shared/AttachmentDownload";

export default function Finance({ isManager }: { isManager: boolean }) {
  const { t } = useTranslation();
  const { moneyMinor, date } = useFormat();
  const { community, communityId, state, can, mutate, busy, refreshError } =
    useWorkspace();
  const { session } = useSession();
  const finance = state.finance;
  const [params] = useSearchParams();
  const notificationId = params.get("notification");
  const source = state.paymentNotifications.find(
    (item) => item.id === notificationId,
  );
  const [tab, setTab] = useState("charges");
  const [form, setForm] = useState<"charge" | "receipt" | "expense" | null>(
    null,
  );
  const [unitId, setUnitId] = useState(
    source?.unitId ?? community.units[0]?.id ?? "",
  );
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);
  const [correction, setCorrection] = useState<{
    type: "REVERSE_PAYMENT" | "CANCEL_CHARGE" | "REVERSE_EXPENSE_PAYMENT";
    id: string;
  } | null>(null);
  const [reason, setReason] = useState("");
  useEffect(() => {
    if (source && isManager) {
      setForm("receipt");
      setUnitId(source.unitId);
      setTab("receipts");
    }
  }, [notificationId, isManager, source?.id]);
  if (!isManager)
    return (
      <section className="panel route-state">
        <h2>{t("navigation.accessDenied")}</h2>
      </section>
    );
  const unitLabel = (id: string) =>
    community.units.find((unit) => unit.id === id)?.label ?? id;
  const today = calendarDateInIstanbul(new Date());
  async function commit(command: FinanceCommand) {
    try {
      const requiredVersion = (value: { version?: number } | undefined) => {
        if (value?.version === undefined)
          throw new Error("Missing server version");
        return value.version;
      };
      const send = (
        operation: string,
        body: unknown,
        work: (key: string) => Promise<unknown>,
      ) =>
        mutate(() =>
          financialWrite(
            session!.user.id + ":" + communityId + ":" + operation,
            body,
            work,
          ),
        );
      if (command.type === "CHARGE_BATCH") {
        const body = {
          items: command.items.map((item) => {
            const [year, month] = item.period.split("-").map(Number);
            return {
              unitId: item.unitId,
              description: item.description,
              amountMinor: item.amountMinor,
              currency: "TRY" as const,
              periodStart: item.period,
              periodEnd: new Date(Date.UTC(year, month, 0))
                .toISOString()
                .slice(0, 10),
              dueOn: item.dueOn,
              expectedAccountVersion: requiredVersion(
                state.unitAccounts.find(
                  (account) => account.unitId === item.unitId,
                ),
              ),
            };
          }),
        };
        await send("chargeBatch", body, (key) =>
          repositories.finance.createCharges(communityId, body, key),
        );
      } else if (command.type === "RECORD_PAYMENT") {
        const body = {
          ...command.payment,
          currency: "TRY" as const,
          expectedAccountVersion: requiredVersion(
            state.unitAccounts.find(
              (account) => account.unitId === command.payment.unitId,
            ),
          ),
        };
        await send("recordPayment", body, (key) =>
          repositories.finance.recordPayment(communityId, body, key),
        );
      } else if (command.type === "LINK_PAYMENT") {
        const notice = state.paymentNotifications.find(
          (item) => item.id === command.notificationId,
        );
        const payment = finance.payments.find((item) => item.id === command.id);
        const body = {
          paymentId: command.id,
          notificationId: command.notificationId,
          expectedVersion: requiredVersion(notice),
          expectedPaymentVersion: requiredVersion(payment),
        };
        await send("linkPayment", body, (key) =>
          repositories.paymentNotifications.link(
            communityId,
            command.notificationId,
            command.id,
            body.expectedVersion,
            body.expectedPaymentVersion,
            key,
          ),
        );
      } else if (
        command.type === "REVERSE_PAYMENT" ||
        command.type === "CANCEL_CHARGE" ||
        command.type === "REVERSE_EXPENSE_PAYMENT"
      ) {
        const record =
          command.type === "REVERSE_PAYMENT"
            ? finance.payments.find((item) => item.id === command.id)
            : command.type === "CANCEL_CHARGE"
              ? finance.charges.find((item) => item.id === command.id)
              : finance.expensePayments.find((item) => item.id === command.id);
        const body = {
          id: command.id,
          reason: command.reason,
          expectedVersion: requiredVersion(record),
        };
        await send(command.type, body, (key) =>
          command.type === "REVERSE_PAYMENT"
            ? repositories.finance.reversePayment(
                communityId,
                command.id,
                command.reason,
                body.expectedVersion,
                key,
              )
            : command.type === "CANCEL_CHARGE"
              ? repositories.finance.cancelCharge(
                  communityId,
                  command.id,
                  command.reason,
                  body.expectedVersion,
                  key,
                )
              : repositories.finance.reverseExpensePayment(
                  communityId,
                  command.id,
                  command.reason,
                  body.expectedVersion,
                  key,
                ),
        );
      } else if (command.type === "RECORD_EXPENSE") {
        const { document, ...expense } = command.expense;
        const body = {
          description: expense.description,
          category: expense.category,
          vendor: expense.vendor,
          incurredOn: expense.incurredOn,
          amountMinor: expense.amountMinor,
          currency: "TRY" as const,
          immediatePayment: command.accountId
            ? { accountId: command.accountId, paidOn: expense.incurredOn }
            : undefined,
        };
        await send("createExpense", { ...body, document }, (key) =>
          repositories.finance.createExpense(
            communityId,
            body,
            key,
            document instanceof File ? document : undefined,
          ),
        );
      } else if (command.type === "PAY_EXPENSE") {
        const expense = finance.expenses.find(
          (item) => item.id === command.expenseId,
        );
        if (!expense) throw new Error("Expense is unavailable");
        const body = {
          accountId: command.accountId,
          paidOn: command.paidOn,
          amountMinor: expense.amountMinor - (expense.paidMinor ?? 0),
          expectedVersion: requiredVersion(expense),
        };
        await send("payExpense:" + command.expenseId, body, (key) =>
          repositories.finance.payExpense(
            communityId,
            command.expenseId,
            body,
            key,
          ),
        );
      }
      if (["RECORD_PAYMENT", "LINK_PAYMENT"].includes(command.type))
        setTab("receipts");
      else if (command.type === "CHARGE_BATCH") setTab("charges");
      else if (["RECORD_EXPENSE", "PAY_EXPENSE"].includes(command.type))
        setTab("expenses");
      setError("");
      setSuccess(true);
      setForm(null);
      setCorrection(null);
      setReason("");
      return true;
    } catch (failure) {
      setError(errorKey(failure));
      return false;
    }
  }
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const amountMinor = parseAmountMinor(String(data.get("amount") ?? ""));
    const description = String(data.get("description") ?? "").trim();
    if (amountMinor === null) {
      setError("payments.errorAmount");
      return;
    }
    if (form === "charge") {
      const targets = data.getAll("target").map(String);
      const dueOn = String(data.get("dueOn"));
      const period = String(data.get("period")) + "-01";
      if (!targets.length) {
        setError("finance.chooseUnits");
        return;
      }
      commit({
        type: "CHARGE_BATCH",
        items: targets.map((id) => ({
          unitId: id,
          description,
          amountMinor,
          dueOn,
          period,
        })),
      });
    } else if (form === "receipt") {
      if (
        source &&
        (source.unitId !== unitId ||
          source.amountMinor !== amountMinor ||
          ["REJECTED", "WITHDRAWN"].includes(source.status))
      ) {
        setError("finance.invalidMatch");
        return;
      }
      const existingPaymentId = String(data.get("existingPaymentId") ?? "");
      if (source && existingPaymentId) {
        commit({
          type: "LINK_PAYMENT",
          id: existingPaymentId,
          notificationId: source.id,
          unitId,
          amountMinor,
        });
        return;
      }
      const allocations = finance.charges
        .filter((charge) => charge.unitId === unitId && !charge.cancelledAt)
        .flatMap((charge) => {
          const text = String(data.get(`allocation-${charge.id}`) ?? "").trim();
          if (!text || /^0([.,]0{1,2})?$/.test(text)) return [];
          const amount = parseAmountMinor(text);
          if (amount === null) throw new Error("payments.errorAmount");
          return [{ chargeId: charge.id, amountMinor: amount }];
        });
      commit({
        type: "RECORD_PAYMENT",
        payment: {
          unitId,
          amountMinor,
          allocations,
          paidOn: String(data.get("paidOn")),
          accountId: String(data.get("accountId")),
          reference: String(data.get("reference") ?? "").trim(),
          notificationId: source?.id,
        },
      });
    } else {
      const entry = data.get("document");
      const document = entry instanceof File && entry.name ? entry : undefined;
      if (document && !validReceipt(document)) {
        setError("payments.errorReceipt");
        return;
      }
      commit({
        type: "RECORD_EXPENSE",
        expense: {
          description,
          amountMinor,
          category: String(data.get("category")) as "OTHER",
          vendor: String(data.get("vendor") ?? "").trim(),
          incurredOn: String(data.get("incurredOn")),
          document,
        },
        accountId: data.get("paid") ? String(data.get("accountId")) : undefined,
      });
    }
  }
  return (
    <div className="finance-feature">
      <div className="finance-toolbar">
        {(["charge", "receipt", "expense"] as const)
          .filter((mode) =>
            can(
              mode === "charge"
                ? "CHARGE_CREATE"
                : mode === "receipt"
                  ? "PAYMENT_RECORD"
                  : "EXPENSE_WRITE",
            ),
          )
          .map((mode) => (
            <button
              key={mode}
              disabled={busy || !!refreshError}
              className="primary"
              onClick={() => {
                setForm(mode);
                setError("");
                setSuccess(false);
              }}
            >
              <Plus size={15} />
              {t(`finance.new.${mode}`)}
            </button>
          ))}
      </div>
      {success && (
        <p className="feedback" role="status">
          {t("finance.saved")}
        </p>
      )}
      {error && (
        <p className="form-error feedback" role="alert">
          {t(error)}
        </p>
      )}
      {form && (
        <section className="panel financial-form-panel">
          <div className="section-heading">
            <h2>{t(`finance.new.${form}`)}</h2>
            <button
              className="icon-button"
              aria-label={t("Close dialog")}
              onClick={() => setForm(null)}
            >
              <X size={18} />
            </button>
          </div>
          <form
            key={form + (source?.id ?? "")}
            className="financial-form"
            onSubmit={(event) => {
              try {
                submit(event);
              } catch {
                setError("payments.errorAmount");
              }
            }}
          >
            {form === "receipt" ? (
              <label>
                {t("UNIT")}
                <select
                  value={unitId}
                  disabled={!!source}
                  onChange={(event) => setUnitId(event.target.value)}
                >
                  {community.units.map((unit) => (
                    <option key={unit.id} value={unit.id}>
                      {unit.label}
                    </option>
                  ))}
                </select>
              </label>
            ) : (
              <label>
                {t("DESCRIPTION")}
                <input name="description" required maxLength={300} />
              </label>
            )}
            <label>
              {t("payments.amount")}
              <input
                key={source?.id ?? "amount"}
                name="amount"
                inputMode="decimal"
                required
                defaultValue={
                  form === "receipt" && source
                    ? String(source.amountMinor / 100)
                    : ""
                }
              />
            </label>
            {form === "charge" && (
              <>
                <label>
                  {t("finance.period")}
                  <input
                    name="period"
                    type="month"
                    defaultValue={today.slice(0, 7)}
                    required
                  />
                </label>
                <label>
                  {t("DUE DATE")}
                  <input
                    name="dueOn"
                    type="date"
                    required
                    defaultValue={today}
                  />
                </label>
                <fieldset className="audience-picker full-width">
                  <legend>{t("finance.targetUnits")}</legend>
                  {community.units.map((unit) => (
                    <label key={unit.id}>
                      <input type="checkbox" name="target" value={unit.id} />
                      {unit.label}
                    </label>
                  ))}
                </fieldset>
                <p className="demo-note full-width">{t("finance.batchHelp")}</p>
              </>
            )}
            {form === "receipt" && (
              <>
                {source && (
                  <label>
                    {t("finance.existingPayment")}
                    <select name="existingPaymentId">
                      <option value="">{t("finance.createNewPayment")}</option>
                      {finance.payments
                        .filter(
                          (payment) =>
                            payment.unitId === source.unitId &&
                            payment.amountMinor === source.amountMinor &&
                            !payment.reversedAt &&
                            !payment.notificationId,
                        )
                        .map((payment) => (
                          <option key={payment.id} value={payment.id}>
                            {payment.reference}
                          </option>
                        ))}
                    </select>
                  </label>
                )}
                <label>
                  {t("payments.date")}
                  <input
                    name="paidOn"
                    type="date"
                    required
                    defaultValue={source?.paymentDate ?? today}
                    max={today}
                  />
                </label>
                <label>
                  {t("payments.reference")}
                  <input
                    name="reference"
                    required
                    maxLength={100}
                    defaultValue={source?.reference ?? ""}
                  />
                </label>
                <label>
                  {t("finance.account")}
                  <select name="accountId">
                    {finance.accounts.map((account) => (
                      <option key={account.id} value={account.id}>
                        {t(account.name)}
                      </option>
                    ))}
                  </select>
                </label>
                <fieldset className="allocation-picker full-width">
                  <legend>{t("finance.allocations")}</legend>
                  <p>{t("finance.allocationHelp")}</p>
                  {finance.charges
                    .filter(
                      (charge) =>
                        charge.unitId === unitId &&
                        !charge.cancelledAt &&
                        charge.amountMinor > allocatedTo(finance, charge.id),
                    )
                    .map((charge) => (
                      <label key={charge.id}>
                        <span>
                          {charge.custom
                            ? charge.description
                            : t(charge.description)}{" "}
                          ·{" "}
                          {moneyMinor(
                            charge.amountMinor -
                              allocatedTo(finance, charge.id),
                          )}
                        </span>
                        <input
                          aria-label={t("finance.allocateTo", {
                            description: charge.custom
                              ? charge.description
                              : t(charge.description),
                          })}
                          name={`allocation-${charge.id}`}
                          inputMode="decimal"
                          placeholder="0"
                        />
                      </label>
                    ))}
                </fieldset>
                {source && (
                  <p className="demo-note full-width">
                    {t("finance.matching")} {source.id}
                  </p>
                )}
              </>
            )}
            {form === "expense" && (
              <>
                <label>
                  {t("announcements.category")}
                  <select name="category">
                    {["MAINTENANCE", "UTILITIES", "CLEANING", "OTHER"].map(
                      (category) => (
                        <option key={category} value={category}>
                          {t(`finance.category.${category}`)}
                        </option>
                      ),
                    )}
                  </select>
                </label>
                <label>
                  {t("finance.vendor")}
                  <input name="vendor" maxLength={150} />
                </label>
                <label>
                  {t("finance.incurredOn")}
                  <input
                    name="incurredOn"
                    type="date"
                    required
                    defaultValue={today}
                    max={today}
                  />
                </label>
                <label>
                  {t("finance.document")}
                  <input
                    name="document"
                    type="file"
                    accept="application/pdf,image/png,image/jpeg"
                  />
                </label>
                <label className="checkbox-label">
                  <input name="paid" type="checkbox" />
                  {t("finance.payNow")}
                </label>
                <label>
                  {t("finance.account")}
                  <select name="accountId">
                    {finance.accounts.map((account) => (
                      <option key={account.id} value={account.id}>
                        {t(account.name)}
                      </option>
                    ))}
                  </select>
                </label>
              </>
            )}
            <div className="form-actions full-width">
              <button
                className="secondary"
                type="button"
                onClick={() => setForm(null)}
              >
                {t("payments.cancel")}
              </button>
              <button
                className="primary"
                disabled={busy || !!refreshError}
                type="submit"
              >
                {t("finance.save")}
              </button>
            </div>
          </form>
        </section>
      )}
      <div className="unit-tabs">
        {["charges", "receipts", "expenses", "accounts"].map((value) => (
          <button
            key={value}
            className={tab === value ? "active" : ""}
            aria-pressed={tab === value}
            onClick={() => setTab(value)}
          >
            {t(`finance.tab.${value}`)}
          </button>
        ))}
      </div>
      {correction && (
        <section className="panel correction-panel">
          <h2>{t("finance.correction")}</h2>
          <p>{t("finance.correctionHelp")}</p>
          <label>
            {t("finance.reason")}
            <textarea
              required
              value={reason}
              onChange={(event) => setReason(event.target.value)}
              maxLength={500}
            />
          </label>
          <div className="form-actions">
            <button className="secondary" onClick={() => setCorrection(null)}>
              {t("payments.cancel")}
            </button>
            <button
              className="primary"
              disabled={!reason.trim() || busy || !!refreshError}
              onClick={() => commit({ ...correction, reason })}
            >
              {t("finance.confirmCorrection")}
            </button>
          </div>
        </section>
      )}
      <section className="panel">
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>{t("DESCRIPTION")}</th>
                <th>{t("AMOUNT")}</th>
                <th>{t("STATUS")}</th>
                <th>{t("ACTION")}</th>
              </tr>
            </thead>
            <tbody>
              {tab === "charges" &&
                finance.charges.map((charge) => (
                  <tr key={charge.id}>
                    <td>
                      <strong>
                        {charge.custom
                          ? charge.description
                          : t(charge.description)}
                      </strong>
                      <small>
                        {unitLabel(charge.unitId)} · {date(charge.dueOn)}
                      </small>
                      {charge.reason && <small>{charge.reason}</small>}
                    </td>
                    <td>{moneyMinor(charge.amountMinor)}</td>
                    <td>
                      {t(
                        charge.cancelledAt
                          ? "finance.cancelled"
                          : allocatedTo(finance, charge.id) ===
                              charge.amountMinor
                            ? "Paid"
                            : "Outstanding",
                      )}
                    </td>
                    <td>
                      {!charge.cancelledAt && can("CHARGE_CREATE") && (
                        <button
                          className="text-button"
                          disabled={busy || !!refreshError}
                          onClick={() => {
                            setCorrection({
                              type: "CANCEL_CHARGE",
                              id: charge.id,
                            });
                            setReason("");
                          }}
                        >
                          {t("finance.cancelCharge")}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              {tab === "receipts" &&
                finance.payments.map((payment) => (
                  <tr key={payment.id}>
                    <td>
                      <strong>{payment.reference}</strong>
                      <small>
                        {unitLabel(payment.unitId)} · {date(payment.paidOn)}
                      </small>
                      {payment.notificationId && (
                        <small>{payment.notificationId}</small>
                      )}
                      {payment.reason && <small>{payment.reason}</small>}
                    </td>
                    <td>{moneyMinor(payment.amountMinor)}</td>
                    <td>
                      {t(
                        payment.reversedAt
                          ? "finance.reversed"
                          : "finance.recorded",
                      )}
                    </td>
                    <td>
                      {!payment.reversedAt && can("PAYMENT_RECORD") && (
                        <button
                          className="text-button"
                          disabled={busy || !!refreshError}
                          onClick={() => {
                            setCorrection({
                              type: "REVERSE_PAYMENT",
                              id: payment.id,
                            });
                            setReason("");
                          }}
                        >
                          {t("finance.reverse")}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              {tab === "expenses" &&
                finance.expenses.map((expense) => {
                  const payment = finance.expensePayments.find(
                    (item) => item.expenseId === expense.id && !item.reversedAt,
                  );
                  return (
                    <tr key={expense.id}>
                      <td>
                        <strong>{expense.description}</strong>
                        <small>
                          {expense.vendor} · {date(expense.incurredOn)}
                        </small>
                        {expense.document && (
                          <AttachmentDownload file={expense.document} />
                        )}
                      </td>
                      <td>{moneyMinor(expense.amountMinor)}</td>
                      <td>
                        {t(
                          (expense.paidMinor ?? 0) >= expense.amountMinor
                            ? "Paid"
                            : "Outstanding",
                        )}
                      </td>
                      <td>
                        {!can("EXPENSE_WRITE") ? (
                          "?"
                        ) : payment ? (
                          <button
                            className="text-button"
                            disabled={busy || !!refreshError}
                            onClick={() => {
                              setCorrection({
                                type: "REVERSE_EXPENSE_PAYMENT",
                                id: payment.id,
                              });
                              setReason("");
                            }}
                          >
                            {t("finance.reverseExpense")}
                          </button>
                        ) : (
                          <button
                            className="text-button"
                            disabled={busy || !!refreshError}
                            onClick={() =>
                              commit({
                                type: "PAY_EXPENSE",
                                expenseId: expense.id,
                                accountId: finance.accounts[0]?.id ?? "",
                                paidOn: today,
                              })
                            }
                          >
                            {t("finance.payExpense")}
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })}
              {tab === "accounts" &&
                finance.accounts.map((account) => (
                  <tr key={account.id}>
                    <td>
                      <Wallet size={16} /> {t(account.name)}
                    </td>
                    <td>{moneyMinor(account.balanceMinor ?? 0)}</td>
                    <td>{t(`finance.kind.${account.kind}`)}</td>
                    <td>—</td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>
        {((tab === "expenses" && !finance.expenses.length) ||
          (tab === "receipts" && !finance.payments.length) ||
          (tab === "charges" && !finance.charges.length)) && (
          <p className="empty-state">{t("finance.empty")}</p>
        )}
      </section>
      {tab === "expenses" && finance.expensePayments.length > 0 && (
        <section className="panel expense-audit">
          <div className="section-heading">
            <h2>{t("finance.expenseHistory")}</h2>
          </div>
          {finance.expensePayments.map((payment) => (
            <article key={payment.id}>
              <strong>
                {
                  finance.expenses.find((item) => item.id === payment.expenseId)
                    ?.description
                }
              </strong>
              <span>
                {moneyMinor(payment.amountMinor)} · {date(payment.paidOn)} ·{" "}
                {t(
                  payment.reversedAt ? "finance.reversed" : "finance.recorded",
                )}
              </span>
              {payment.reason && <p>{payment.reason}</p>}
            </article>
          ))}
        </section>
      )}
      <div className="payment-summary finance-totals">
        <div>
          <span>{t("Outstanding charges")}</span>
          <strong>
            {moneyMinor(
              state.unitAccounts.reduce(
                (sum, account) => sum + account.outstandingMinor,
                0,
              ),
            )}
          </strong>
        </div>
        <div>
          <span>{t("finance.totalCredit")}</span>
          <strong>
            {moneyMinor(
              state.unitAccounts.reduce(
                (sum, account) => sum + account.creditMinor,
                0,
              ),
            )}
          </strong>
        </div>
      </div>
    </div>
  );
}
