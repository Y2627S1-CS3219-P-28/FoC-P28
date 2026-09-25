# frontend

The FoC web UI: Next.js 16 (App Router) + TypeScript + Tailwind CSS 4 + shadcn/ui.
It only renders UI and calls the backend services through the gateway. See the root
[AGENTS.md](../AGENTS.md) section 10 for conventions.

## Run

```bash
# Everything in containers (recommended): from the repo root
docker compose up --build            # UI at http://localhost:8080

# Hot-reload dev server against the containerised gateway/emulators
npm install
FOC_API_BASE_URL=http://localhost:8080 \
FOC_FIREBASE_AUTH_EMULATOR_URL=http://localhost:9099 \
npm run dev                          # http://localhost:3000
```

## Structure

```text
src/
├── app/                  # routes: app/<feature>/page.tsx
├── components/
│   ├── ui/               # shadcn/ui components (npx shadcn@latest add <name>)
│   ├── providers/        # config + auth context
│   └── <feature>/        # feature components
├── config/navigation.ts  # register your feature's nav entry here
├── hooks/use-api.ts      # authenticated fetch through the gateway
└── lib/                  # api client, firebase, runtime config
```

## Configuration

Read on the server at request time (`src/lib/runtime-config.ts`), so one image works in
every environment:

| Variable | Meaning |
| --- | --- |
| `FOC_ENVIRONMENT` | `local` / `staging` / `production` (shown as a badge outside production) |
| `FOC_API_BASE_URL` | gateway origin; empty = same origin |
| `FOC_FIREBASE_PROJECT_ID`, `FOC_FIREBASE_API_KEY`, `FOC_FIREBASE_AUTH_DOMAIN`, `FOC_FIREBASE_APP_ID` | Firebase web config |
| `FOC_FIREBASE_AUTH_EMULATOR_URL` | Auth emulator URL (local only) |
