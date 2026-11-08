# TODO

This document tracks topics for future consideration. An unchecked item is not an approved implementation requirement.

## Domain model

- [ ] **TODO-001: Revisit independent block records.**
  - **Status:** Deferred for future discussion; no implementation is planned now.
  - **Current decision:** Keep `blockId` and `blockLabel` on `Unit`. Do not introduce a separate `Block` entity at this stage.
  - **Current behavior:** `Community` stores neither a block list nor a block count. A count can be derived from distinct `blockId` values on its units.
  - **Limitations to reconsider:** A block without any units cannot be represented, and its label is repeated across unit records.
  - **Future question:** If blocks need their own lifecycle or attributes, evaluate a separate `Block` record linked by `communityId`. Decide whether the benefits justify the added complexity before implementation.
  - **Context:** [Domain and DDD guide](DOMAIN-DDD-GUIDE.md).
