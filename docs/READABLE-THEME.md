# Readable default theme

The manager/resident application and administration panel use a readable default theme intended to support older residents as well as other users.

- Base text is 16 CSS pixels. Existing small labels and supporting text use at least 1rem, including mobile layouts. Relative font sizes support browser zoom and user font preferences.
- Arial with system fallbacks replaces decorative web fonts. Headings retain their hierarchy without tight letter spacing.
- Muted foregrounds are darker; forms and panels have clearer boundaries. Green remains the action and selected-navigation color. Summary cards use soft green, blue, peach and lavender backgrounds with dark text; finance summaries and administration lists use matching accents.
- Buttons and ordinary form controls have a minimum height of 48 CSS pixels. Checkboxes and radio controls are 24 pixels.
- Keyboard focus is marked by a visible blue outline. Sidebar content scrolls when larger text exceeds the available height.
- Mobile headings and actions can wrap; wide data tables retain their horizontal scrolling containers.

Validation includes both frontend production builds, existing browser workflows, and visual inspection at desktop and 390px mobile widths. This update is not a claim of complete accessibility conformance; screen-reader, contrast and zoom audits across all states remain useful follow-up work.
