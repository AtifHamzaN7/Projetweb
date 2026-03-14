# Projetweb

Brief handover guide for running and understanding the project.

## What This Project Contains

- `frontend/`: Angular application for the user interface.
- `facade/`: Spring Boot backend exposing the API.
- `db/`: local HSQLDB database, SQL schema, and startup scripts.
- `script-test.mjs`: AI-assisted Java test generator for the Maven backend.
- `scriptdependance`: helper script that finds internal Java dependencies with `jdeps`.
- `the-code-assistant.ipynb`: notebook version of the AI assistant work.
- `the-code-assistant-1.html`: exported HTML view of the notebook.
- `Front-assistant.ipynb`: frontend assistant
- `Backend.txt`: quick backend test commands with `curl`.
- `comp.sh`: old WAR packaging helper script.
- `facade.war`: prebuilt WAR artifact.

## Prerequisites

You should have:

- Java 17
- Node.js + npm
- Git
- Python 3.10+
- Jupyter Notebook or JupyterLab

Useful extras:

- `jdeps` (usually included with the JDK)
- Angular CLI: `npm install -g @angular/cli`
- OpenRouter API key if you want to run the AI notebooks

Notes:

- HSQLDB is already included in `db/hsqldb/`, so no separate DB install is needed.
- Maven Wrapper is already included in `facade/mvnw`, so Maven does not need to be installed globally.

## How To Run The Project

### 1. Start the database

From `db/`:

```bash
source start.sh
```

Optional first-time schema load:

```bash
source populate.sh
```

Optional DB console:

```bash
source console.sh
```

### 2. Start the backend

From `facade/`:

```bash
./mvnw spring-boot:run
```

Backend runs on `http://localhost:8080`.

### 3. Start the frontend

From `frontend/`:

```bash
npm install
npm start
```

Frontend is served with Angular dev server, usually on `http://localhost:4200`.

## About `the-code-assistant`

There are two notebook assistants:

- `the-code-assistant.ipynb`: backend/code assistant
- `Front-assistant.ipynb`: frontend assistant

How to run them:

1. (Optional)
```bash
jupyter notebook
```
2. Open `the-code-assistant.ipynb` or `Front-assistant.ipynb`.
3. Run the cells in order.

Environment needed for the notebooks:

- Python 3 with kernel `python3`
- Jupyter Notebook or JupyterLab
- `python-dotenv`
- `requests`
- `langchain-openai`
- Java 17 for `the-code-assistant.ipynb`
- Maven available in `PATH` for `the-code-assistant.ipynb`
- Node.js + npm for `Front-assistant.ipynb`
- `OPENROUTER_API_KEY` in `.env` if you want the AI calls to work

Notes:

- `the-code-assistant.ipynb` is the useful code assistant for the backend workflow.
- `Front-assistant.ipynb` exists, but it did not generate good results.
- `the-code-assistant-1.html` is only an exported HTML view of the backend notebook.

Related helper scripts:

- `script-test.mjs`: automated test-generation helper
- `scriptdependance`: dependency analysis helper used by the test workflow

These scripts are for testing/CI support. In your workflow, tests run automatically when code is pushed to the remote repository.

## Main Folder Guide

### `frontend/src/app/`

- `home/`: landing page
- `auth/`, `conn/`: authentication and login views
- `adherent/`: member-related page
- `recipes/`, `recipe-details/`, `add-recipe/`: recipe listing, details, and creation
- `events/`, `event-detail/`, `add-event/`: event listing, details, and creation
- `forum/`: discussions, messages, and forum flows
- `a-propos/`, `contact/`: static information pages
- `services/`: Angular services calling the backend API
- `models/`: frontend data models
- `shared/`: reusable UI parts such as navbar, footer, and cards

### `facade/src/main/java/n7/facade/`

- `*Controller`: REST endpoints
- `*Repository`: Spring Data access layer
- entity classes like `Adherent`, `Recette`, `Event`, `Discussion`, `Message`, `Comment`, `Ingredient`: backend domain model
- `FacadeApplication`: Spring Boot entry point
- `ServletInitializer`: WAR deployment entry point

### `db/`

- `db.sql`: database schema / initial SQL
- `start.sh`: starts HSQLDB on port `9005`
- `populate.sh`: executes `db.sql`
- `console.sh`: opens the HSQLDB GUI console
- `auth.rc`: DB connection config
- `mydb.*`: local persisted database files
- `hsqldb/`: embedded HSQLDB distribution

## Useful Files

- `Backend.txt`: ready-to-use backend API test examples
- `scriptdependance`: prints internal Java dependencies for one class
- `comp.sh`: manual Java/WAR packaging helper from an older setup
