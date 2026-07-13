## Summary

<!-- What changed and why (1–3 bullets). Keep it clear — release notes are derived from this. -->

-

## Impact (required)

Apply **exactly one** GitHub label before merge. Changelog, README Upcoming, and the post-release in-app notification use this to prioritize:

| Label | Use when |
|:--|:--|
| `user-impact` | Listeners will notice (UI, playback, search, downloads, onboarding, etc.) |
| `backend-fix` | Server / proxy / infra only — CHANGELOG yes; usually skip README & notification |
| `non-user-impact` | CI, docs, tooling, internal refactors with no user-facing change |

Check the matching box **and** add the label on the PR (`gh pr edit --add-label user-impact`, etc.):

- [ ] `user-impact`
- [ ] `backend-fix`
- [ ] `non-user-impact`

## Test plan

- [ ] Built / installed locally (`./gradlew installDebug`) when UI or app behavior changed
- [ ] Manual checks for the user-visible paths touched by this PR

## Notes (optional)

<!-- Screenshots, rollout risks, follow-ups. -->
