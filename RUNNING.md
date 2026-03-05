# Running the Opera Generator

## `main` — Manual Orchestration
```bash
./gradlew run                          # Full pipeline (5 scenes, AI-suggested title)
./gradlew run --args='"My Title" 3'    # Custom title, 3 scenes
```

## `langchain4j-agentic` — LLM Supervisor
```bash
# Agentic mode (supervisor decides everything)
./gradlew run -PmainClass=com.kousenit.agentic.AgenticOperaGenerator

# Manual mode (code controls sequence)
./gradlew run -PmainClass=com.kousenit.agentic.AgenticOperaGenerator --args='--manual'

# With custom scene count
./gradlew run -PmainClass=com.kousenit.agentic.AgenticOperaGenerator --args='3'
```

## `embabel` — GOAP Planner (Spring Shell)
```bash
./gradlew bootRun
# Then in the shell:
#   create-opera                    # Agentic GOAP mode
#   generate-opera --scenes 3      # Manual mode
#   run-production                  # After manual generation
```

## `claude-teams` — Agent Orchestrator
```bash
# Individual pipeline steps (what the agents call)
./gradlew generateScenes
./gradlew generateImages -PoperaJson=path/to/opera.json
./gradlew generateNarration -PoperaJson=path/to/opera.json
./gradlew generateCritique -PoperaJson=path/to/opera.json

# Or run the full app normally
./gradlew run
```
The Claude Teams approach is designed to be run *by* Claude Code agents,
not directly — but the Gradle tasks work standalone too.
