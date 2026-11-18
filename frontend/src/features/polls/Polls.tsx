import { useEffect, useRef, useState, type FormEvent } from "react";
import { useTranslation } from "react-i18next";
import { Plus, RefreshCw, ArrowLeft, X } from "lucide-react";
import { api, ApiError, type Poll } from "../../api";
import { all, errorKey } from "../../api/server";
import { useWorkspace } from "../../workspace/Workspace";
import { useFormat } from "../../shared/useFormat";

export default function Polls() {
  const { t } = useTranslation();
  const { dateTime } = useFormat();
  const { communityId, isManager, can, mutate, busy } = useWorkspace();
  const [polls, setPolls] = useState<Poll[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [selectedId, setSelectedId] = useState("");
  const [creating, setCreating] = useState(false);
  const [options, setOptions] = useState(["", ""]);
  const [units, setUnits] = useState<string[]>([]);
  const [optionId, setOptionId] = useState("");
  const [pending, setPending] = useState(false);
  const [revision, setRevision] = useState(0);
  const writing = useRef(false);
  const readEpoch = useRef(0);
  const selected = polls.find((poll) => poll.id === selectedId);
  const manage = isManager && can("POLL_MANAGE");
  useEffect(() => {
    const controller = new AbortController();
    let timer: ReturnType<typeof setTimeout>;
    let running = false;
    const load = async () => {
      if (running || writing.current) return;
      running = true;
      const epoch = readEpoch.current;
      try {
        const items = await all<"listPolls", Poll>(
          "listPolls",
          communityId,
          controller.signal,
        );
        if (!controller.signal.aborted && epoch === readEpoch.current) {
          setPolls(
            items.sort((a, b) => b.createdAt.localeCompare(a.createdAt)),
          );
        }
      } catch (failure) {
        if (!controller.signal.aborted) {
          setError(errorKey(failure));
          if (
            failure instanceof ApiError &&
            [401, 403, 404].includes(failure.status)
          )
            setPolls([]);
        }
      } finally {
        running = false;
        if (!controller.signal.aborted) setLoading(false);
      }
    };
    const tick = async () => {
      if (document.visibilityState !== "hidden") await load();
      if (!controller.signal.aborted) timer = setTimeout(tick, 10000);
    };
    void tick();
    return () => {
      controller.abort();
      clearTimeout(timer);
    };
  }, [communityId, revision]);
  useEffect(() => {
    setUnits([]);
    setOptionId("");
    setNotice("");
  }, [selectedId]);
  useEffect(() => {
    if (selected)
      setUnits((previous) =>
        previous.filter((id) =>
          selected.eligibleUnits.some((unit) => unit.id === id && !unit.voted),
        ),
      );
  }, [selected?.version]);
  async function command(work: () => Promise<Poll>) {
    setPending(true);
    writing.current = true;
    readEpoch.current++;
    setError("");
    setNotice("");
    try {
      const result = await mutate(work);
      setPolls((previous) => [
        result,
        ...previous.filter((poll) => poll.id !== result.id),
      ]);
      setSelectedId(result.id);
      setCreating(false);
      setUnits([]);
      setOptionId("");
      setNotice(t("polls.saved"));
      return true;
    } catch (failure) {
      setError(
        failure instanceof ApiError && failure.code === "UNIT_ALREADY_VOTED"
          ? "polls.alreadyVoted"
          : errorKey(failure),
      );
      setRevision((value) => value + 1);
      return false;
    } finally {
      setPending(false);
      writing.current = false;
      readEpoch.current++;
    }
  }
  function create(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const submitter = (event.nativeEvent as SubmitEvent)
      .submitter as HTMLButtonElement | null;
    const date = new Date(String(data.get("closesAt")));
    if (Number.isNaN(date.getTime()) || date.getTime() <= Date.now()) {
      setError("polls.futureDate");
      return;
    }
    void command(() =>
      api.execute("createPoll", {
        params: { communityId },
        body: {
          title: String(data.get("title")).trim(),
          description: String(data.get("description") ?? "").trim(),
          audience: String(data.get("audience")) as "OWNERS" | "RESIDENTS",
          options: options.map((option) => option.trim()),
          closesAt: date.toISOString(),
          status: submitter?.value === "DRAFT" ? "DRAFT" : "OPEN",
        },
      }),
    );
  }
  const locked = busy || pending;
  return (
    <div className="polls-feature">
      <div className="poll-toolbar">
        <p>{t("polls.help")}</p>
        <div>
          <button
            className="secondary"
            disabled={locked}
            onClick={() => setRevision((value) => value + 1)}
          >
            <RefreshCw size={16} />
            {t("live.refresh")}
          </button>
          {manage && !creating && (
            <button
              className="primary"
              disabled={locked}
              onClick={() => {
                setCreating(true);
                setOptions(["", ""]);
                setError("");
                setNotice("");
              }}
            >
              <Plus size={16} />
              {t("polls.create")}
            </button>
          )}
        </div>
      </div>
      {error && (
        <p className="form-error feedback" role="alert">
          {t(error)}
        </p>
      )}
      {notice && (
        <p className="feedback" role="status">
          {notice}
        </p>
      )}
      {creating && manage && (
        <section className="panel poll-editor">
          <div className="section-heading">
            <h2>{t("polls.create")}</h2>
            <button
              className="icon-button"
              disabled={locked}
              aria-label={t("Close dialog")}
              onClick={() => setCreating(false)}
            >
              <X size={18} />
            </button>
          </div>
          <form onSubmit={create}>
            <fieldset disabled={locked}>
              <label>
                {t("polls.title")}
                <input name="title" required maxLength={200} />
              </label>
              <label>
                {t("polls.description")}
                <textarea name="description" maxLength={3000} />
              </label>
              <label>
                {t("polls.audience")}
                <select name="audience" aria-label={t("polls.audience")}>
                  <option value="RESIDENTS">{t("polls.RESIDENTS")}</option>
                  <option value="OWNERS">{t("polls.OWNERS")}</option>
                </select>
              </label>
              <label>
                {t("polls.closesAt")}
                <input
                  name="closesAt"
                  type="datetime-local"
                  required
                  defaultValue={new Date(
                    Date.now() +
                      86400000 -
                      new Date().getTimezoneOffset() * 60000,
                  )
                    .toISOString()
                    .slice(0, 16)}
                />
              </label>
              <fieldset className="poll-options">
                <legend>{t("polls.options")}</legend>
                {options.map((option, index) => (
                  <div key={index}>
                    <label>
                      {t("polls.option", { number: index + 1 })}
                      <input
                        required
                        maxLength={200}
                        value={option}
                        onChange={(event) =>
                          setOptions((previous) =>
                            previous.map((value, i) =>
                              i === index ? event.target.value : value,
                            ),
                          )
                        }
                      />
                    </label>
                    {options.length > 2 && (
                      <button
                        type="button"
                        className="text-button"
                        aria-label={t("polls.removeOption", {
                          number: index + 1,
                        })}
                        onClick={() =>
                          setOptions((previous) =>
                            previous.filter((_, i) => i !== index),
                          )
                        }
                      >
                        <X size={16} />
                      </button>
                    )}
                  </div>
                ))}
                <button
                  className="secondary"
                  type="button"
                  disabled={options.length >= 10}
                  onClick={() => setOptions((previous) => [...previous, ""])}
                >
                  {t("polls.addOption")}
                </button>
              </fieldset>
              <p>{t("polls.rules")}</p>
              <div className="form-actions">
                <button
                  type="button"
                  className="secondary"
                  onClick={() => setCreating(false)}
                >
                  {t("payments.cancel")}
                </button>
                <button className="secondary" type="submit" value="DRAFT">
                  {t("polls.saveDraft")}
                </button>
                <button className="primary" type="submit" value="OPEN">
                  {t("polls.publish")}
                </button>
              </div>
            </fieldset>
          </form>
        </section>
      )}
      {loading ? (
        <p role="status">{t("units.loading")}</p>
      ) : selected ? (
        <section className="panel poll-detail">
          <button
            className="text-button"
            disabled={locked}
            onClick={() => setSelectedId("")}
          >
            <ArrowLeft size={16} />
            {t("polls.back")}
          </button>
          <div className="section-heading">
            <div>
              <span className="tag">{t("polls." + selected.status)}</span>
              <h2>{selected.title}</h2>
              <p>{selected.description}</p>
              <small>
                {t("polls." + selected.audience)} · {t("polls.closesAt")}:{" "}
                {dateTime(selected.closesAt)}
              </small>
            </div>
            <div className="form-actions">
              {manage && selected.status === "DRAFT" && (
                <button
                  className="primary"
                  disabled={locked}
                  onClick={() =>
                    void command(() =>
                      api.execute("publishPoll", {
                        params: { communityId, pollId: selected.id },
                        body: { expectedVersion: selected.version },
                      }),
                    )
                  }
                >
                  {t("polls.publish")}
                </button>
              )}
              {manage && selected.status === "OPEN" && (
                <button
                  className="secondary"
                  disabled={locked}
                  onClick={() =>
                    void command(() =>
                      api.execute("closePoll", {
                        params: { communityId, pollId: selected.id },
                        body: { expectedVersion: selected.version },
                      }),
                    )
                  }
                >
                  {t("polls.close")}
                </button>
              )}
            </div>
          </div>
          <h3>{t("polls.results", { count: selected.totalVotes })}</h3>
          <p>{t("polls.liveResults")}</p>
          <div className="poll-results">
            {selected.results.map((result) => (
              <div key={result.optionId}>
                <div>
                  <strong>{result.label}</strong>
                  <span>{t("polls.voteCount", { count: result.votes })}</span>
                </div>
                <progress
                  max={Math.max(1, selected.totalVotes)}
                  value={result.votes}
                />
              </div>
            ))}
          </div>
          {selected.status === "OPEN" && (
            <form
              className="poll-voting"
              onSubmit={(event) => {
                event.preventDefault();
                if (!optionId || !units.length) return;
                void command(() =>
                  api.execute("votePoll", {
                    params: { communityId, pollId: selected.id },
                    body: {
                      optionId,
                      unitIds: units,
                      expectedVersion: selected.version,
                    },
                  }),
                );
              }}
            >
              <fieldset disabled={locked}>
                <legend>{t("polls.castVote")}</legend>
                <p>{t("polls.rules")}</p>
                {selected.eligibleUnits.length ? (
                  <>
                    <div className="poll-unit-picker">
                      {selected.eligibleUnits.map((unit) => (
                        <label key={unit.id}>
                          <input
                            type="checkbox"
                            checked={units.includes(unit.id)}
                            disabled={unit.voted}
                            onChange={(event) =>
                              setUnits((previous) =>
                                event.target.checked
                                  ? [...previous, unit.id]
                                  : previous.filter((id) => id !== unit.id),
                              )
                            }
                          />
                          {unit.label}
                          {unit.voted && <span>{t("polls.voted")}</span>}
                        </label>
                      ))}
                    </div>
                    <div className="poll-option-picker">
                      {selected.options.map((option) => (
                        <label key={option.id}>
                          <input
                            type="radio"
                            name="voteOption"
                            value={option.id}
                            checked={optionId === option.id}
                            onChange={() => setOptionId(option.id)}
                          />
                          {option.label}
                        </label>
                      ))}
                    </div>
                    <button
                      className="primary"
                      disabled={locked || !optionId || !units.length}
                    >
                      {t("polls.submit", { count: units.length })}
                    </button>
                  </>
                ) : (
                  <p>{t("polls.notEligible")}</p>
                )}
              </fieldset>
            </form>
          )}
          <h3>{t("polls.ballots")}</h3>
          <div className="table-scroll">
            <table className="poll-ballots">
              <thead>
                <tr>
                  {["UNIT", "polls.voter", "polls.choice", "polls.votedAt"].map(
                    (key) => (
                      <th key={key}>{t(key)}</th>
                    ),
                  )}
                </tr>
              </thead>
              <tbody>
                {selected.ballots.map((ballot) => (
                  <tr key={ballot.unitId}>
                    <td>{ballot.unitLabel}</td>
                    <td>{ballot.voterName}</td>
                    <td>
                      {
                        selected.options.find(
                          (option) => option.id === ballot.optionId,
                        )?.label
                      }
                    </td>
                    <td>{dateTime(ballot.votedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {!selected.ballots.length && (
            <p className="empty-state">{t("polls.noVotes")}</p>
          )}
        </section>
      ) : (
        <div className="poll-list">
          {polls.map((poll) => (
            <article className="panel" key={poll.id}>
              <span className="tag">{t("polls." + poll.status)}</span>
              <h2>{poll.title}</h2>
              <p>{poll.description}</p>
              <small>
                {t("polls." + poll.audience)} · {dateTime(poll.closesAt)}
              </small>
              <strong>
                {t("polls.voteCount", { count: poll.totalVotes })}
              </strong>
              <button
                className="secondary"
                disabled={locked}
                onClick={() => setSelectedId(poll.id)}
              >
                {t("polls.open")}
              </button>
            </article>
          ))}
          {!polls.length && !error && (
            <p className="empty-state">{t("polls.empty")}</p>
          )}
        </div>
      )}
    </div>
  );
}
