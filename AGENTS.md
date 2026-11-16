# Project delivery workflow

- Complete and report one backlog task per run.
- Create new task branches from the latest `dev` branch.
- Target task pull requests at `dev` and merge completed tasks into `dev` for now.
- Merge into `main` only when the user explicitly requests a release or promotion.
- Preserve unrelated local changes and run the repository's required checks before merging.
