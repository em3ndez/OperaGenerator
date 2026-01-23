# Agentic Migration Plan: Opera Generator → langchain4j-agentic

**Date:** 2026-01-23
**Target Branch:** `agentic-refactor`
**Goal:** Migrate Opera Generator from manual orchestration to LangChain4j's agentic framework

---

## Current Architecture Analysis

### Existing Workflow (Manual Orchestration)
```
IntegratedOperaGenerator.main()
  ↓
1. Conversation.generateOpera()          → Opera object
2. LibrettoWriter.saveCompleteOpera()   → Path
3. NarratorVoice.generate*()            → Audio files (optional)
4. GeminiImageGenerator.generateImages() → Images
6. ExternalToolsPreparer.generate*()    → Export packages
7. OperaCritic.reviewAndSave()          → Critique (optional)
```

**Characteristics:**
- **Sequential**: Each step depends on previous outputs
- **Stateful**: Opera object passed through pipeline
- **Branching**: Optional steps based on API key availability
- **Complex scene generation**: Alternates between GPT-5.2 and Claude Opus 4.5 using ChatMemory

---

## Target Architecture: Agentic Approach

### Recommended Pattern: **Sequential Workflow with Optional Loop**

**Why Sequential?**
- Natural fit for the existing pipeline structure
- Maintains predictable execution order
- Easy to debug and observe
- Can be extended with Loop for iterative refinement

**Why Add Loop?**
- Enable iterative quality improvement
- Critic feedback → scene regeneration/refinement
- Quality threshold gates (e.g., minimum score)

### Architecture Diagram
```
Sequential Workflow:
┌─────────────────────────┐
│ SceneGeneratorAgent     │ → generates Opera with scenes
├─────────────────────────┤
│ LibrettoWriterAgent     │ → saves formatted libretto
├─────────────────────────┤
│ ImageGeneratorAgent     │ → creates illustrations
├─────────────────────────┤
│ NarratorAgent           │ → generates audio (optional)
├─────────────────────────┤
│ ExportPreparerAgent     │ → creates export packages
├─────────────────────────┤
│ CriticAgent             │ → generates review (optional)
└─────────────────────────┘

Optional Future: Loop Workflow for Refinement
┌──────────────────────────────────┐
│  SceneGeneratorAgent             │
│         ↓                         │
│  CriticAgent (evaluate)          │
│         ↓                         │
│  Conditional: score > threshold? │
│    ↓ No        ↓ Yes              │
│  (loop)     (exit)               │
└──────────────────────────────────┘
```

---

## Agent Definitions

### 1. **SceneGeneratorAgent**
**Purpose:** Generate opera scenes alternating between GPT-5.2 and Claude Opus 4.5

**Inputs:**
- `operaTitle` (String, optional - AI suggests if blank)
- `numberOfScenes` (int, default: 5)
- `premise` (String, default: Hartford jungle premise)

**Outputs:**
- `opera` (Opera object with title, premise, scenes)

**Implementation Strategy:**
- Wrap existing `Conversation.generateOpera()` logic
- Preserve alternating model selection
- Keep ChatMemory pattern for scene continuity
- Use `@Agent` annotation with `outputKey = "opera"`

```java
public interface SceneGeneratorAgent {
    @UserMessage("""
        Generate an opera titled {{title}} with {{numberOfScenes}} scenes.
        Premise: {{premise}}
        Alternate between GPT-5.2 and Claude Opus 4.5 for each scene.
        Maintain continuity through ChatMemory.
        """)
    @Agent(outputKey = "opera")
    Opera generateOpera(
        @V("title") String title,
        @V("numberOfScenes") int numberOfScenes,
        @V("premise") String premise
    );
}
```

**Challenge:** The alternating model pattern doesn't map cleanly to standard @Agent methods.
**Solution:** Implement as custom tool or use planner pattern for model selection.

---

### 2. **LibrettoWriterAgent**
**Purpose:** Save opera to organized directory with automatic formatting

**Inputs:**
- `opera` (from AgenticScope or previous agent)

**Outputs:**
- `librettoPath` (Path to complete libretto file)
- `operaDirectory` (Path to opera directory)

**Implementation Strategy:**
- Wrap existing `LibrettoWriter.saveCompleteOpera()`
- Simple delegating agent - no LLM needed
- Store paths in AgenticScope for downstream agents

```java
public interface LibrettoWriterAgent {
    @Agent(outputKey = "librettoPath")
    Path saveOpera(@V("opera") Opera opera);
}
```

---

### 3. **ImageGeneratorAgent**
**Purpose:** Generate illustrations for all scenes using Nano Banana

**Inputs:**
- `opera` (Opera object with scenes)

**Outputs:**
- `imageCount` (int, number of images generated)
- `imagePaths` (List<Path>)

**Implementation Strategy:**
- Wrap `GeminiImageGenerator.generateImages()`
- Consider parallel sub-agents for concurrent image generation
- Maintain existing rate limiting (Semaphore pattern)

```java
public interface ImageGeneratorAgent {
    @Agent(outputKey = "imageCount")
    int generateImages(@V("opera") Opera opera);
}
```

**Future Enhancement:** Use `parallelBuilder()` to generate all images concurrently.

---

### 4. **NarratorAgent**
**Purpose:** Generate audio narration using ElevenLabs

**Inputs:**
- `opera` (Opera object)
- `operaDirectory` (Path)

**Outputs:**
- `audioFiles` (List<Path>)
- `narratedSceneCount` (int)

**Implementation Strategy:**
- Wrap `NarratorVoice` methods
- Conditional execution based on ELEVENLABS_API_KEY
- Generate introduction + scene narrations

```java
public interface NarratorAgent {
    @Agent(outputKey = "audioFiles")
    List<Path> generateNarration(
        @V("opera") Opera opera,
        @V("operaDirectory") Path directory
    );
}
```

**Conditional Execution:** Use `conditionalBuilder()` to skip if API key missing.

---

### 5. **ExportPreparerAgent**
**Purpose:** Create export packages for Suno AI and NotebookLM

**Inputs:**
- `opera` (Opera object)
- `operaDirectory` (Path)

**Outputs:**
- `sunoPromptsPath` (Path)
- `notebookLMPath` (Path)

**Implementation Strategy:**
- Wrap `ExternalToolsPreparer` methods
- Non-LLM agent (pure utility)

```java
public interface ExportPreparerAgent {
    @Agent(outputKey = "exportPaths")
    Map<String, Path> prepareExports(
        @V("opera") Opera opera,
        @V("operaDirectory") Path directory
    );
}
```

---

### 6. **CriticAgent**
**Purpose:** Generate AI critical review using Gemini 3 Pro

**Inputs:**
- `opera` (Opera object)
- `operaDirectory` (Path)

**Outputs:**
- `critique` (String, review text)
- `critiquePath` (Path to saved critique)
- `critiqueScore` (double, 0-10 quality rating)

**Implementation Strategy:**
- Wrap `OperaCritic.reviewAndSave()`
- Extract numerical score from critique
- Use for loop condition in future refinement workflow

```java
public interface CriticAgent {
    @UserMessage("""
        Read the opera "{{operaTitle}}" and provide a critical review.
        Evaluate: plot coherence, character development, lyrical quality.
        Include a numerical score from 0-10 at the end: "Final Score: X/10"
        """)
    @Agent(outputKey = "critique")
    String reviewOpera(
        @V("operaTitle") String title,
        @V("operaDirectory") Path directory
    );
}
```

**Future Use:** Loop condition → regenerate scenes if score < 7.0

---

## AgenticScope Shared State

```java
Map<String, Object> scope = new HashMap<>();

// Initial inputs
scope.put("operaTitle", title);
scope.put("numberOfScenes", numberOfScenes);
scope.put("premise", Conversation.defaultPremise());

// Outputs passed between agents
scope.put("opera", ...);           // Opera object
scope.put("librettoPath", ...);    // Path
scope.put("operaDirectory", ...);  // Path
scope.put("imageCount", ...);      // int
scope.put("audioFiles", ...);      // List<Path>
scope.put("critique", ...);        // String
scope.put("critiqueScore", ...);   // double
```

---

## Implementation Plan

### Phase 1: Sequential Migration (MVP)
**Goal:** Replace manual orchestration with Sequential workflow

**Tasks:**
1. ✅ Add `langchain4j-agentic` dependency to build.gradle.kts
2. Create agent interfaces for each step (6 agents)
3. Implement agent builders using existing classes
4. Build sequential workflow with `AgenticServices.sequenceBuilder()`
5. Replace `IntegratedOperaGenerator.main()` with agentic workflow invocation
6. Add `AgentListener` for observability/logging
7. Test end-to-end opera generation

**Success Criteria:**
- Generates complete opera (scenes, libretto, images, audio, critique)
- Output identical to current implementation
- Better logging through AgentListener

---

### Phase 2: Conditional Execution (Enhancement)
**Goal:** Skip optional steps based on API key availability

**Tasks:**
1. Wrap NarratorAgent in conditional: if ELEVENLABS_API_KEY exists
2. Wrap CriticAgent in conditional: if GOOGLEAI_API_KEY exists
3. Use `conditionalBuilder()` for branching logic

**Success Criteria:**
- Gracefully skips audio generation without ELEVENLABS_API_KEY
- Gracefully skips critique without GOOGLEAI_API_KEY

---

### Phase 3: Parallel Image Generation (Optimization)
**Goal:** Speed up image generation using parallel execution

**Tasks:**
1. Create individual ImageAgent for single scene
2. Use `parallelBuilder()` to invoke all scene images concurrently
3. Maintain rate limiting (Semaphore preserved in implementation)
4. Integrate parallel image workflow into main sequence

**Success Criteria:**
- Generates images faster than sequential approach
- Respects rate limits (no API throttling)

---

### Phase 4: Iterative Refinement Loop (Advanced)
**Goal:** Use critic feedback to iteratively improve scenes

**Tasks:**
1. Create SceneRefinerAgent (regenerates specific scenes)
2. Extract numerical score from CriticAgent output
3. Build loop workflow: Generate → Critique → Refine (if score < threshold)
4. Add loop termination: max iterations or score threshold
5. Add user option to enable/disable refinement loop

**Success Criteria:**
- Opera iteratively improves based on critique
- Loop terminates when quality threshold met (e.g., score ≥ 7.0)
- Doesn't loop indefinitely (max 3 iterations)

---

## Key Challenges & Solutions

### Challenge 1: Alternating Model Selection
**Problem:** Current `Conversation` alternates between GPT-5.2 and Claude using modulo logic. @Agent pattern doesn't naturally support this.

**Solutions:**
- **Option A:** Implement SceneGeneratorAgent as tool, keep existing logic
- **Option B:** Use Planner to dynamically select model per scene
- **Option C:** Create separate GPTSceneAgent and ClaudeSceneAgent, alternate manually

**Recommendation:** Option A for Phase 1 (preserve existing logic), explore Option B in Phase 4.

---

### Challenge 2: ChatMemory Continuity
**Problem:** Scenes must maintain continuity through ChatMemory. AgenticScope doesn't replace ChatMemory.

**Solution:**
- Keep ChatMemory inside SceneGeneratorAgent implementation
- ChatMemory is internal state, not shared via AgenticScope
- AgenticScope only shares final Opera object

---

### Challenge 3: Optional Steps (API Keys)
**Problem:** Some steps optional based on environment variables.

**Solution:**
- Use `conditionalBuilder()` with predicates
- Check env vars at workflow build time
- Example:
```java
.conditional(
    scope -> System.getenv("ELEVENLABS_API_KEY") != null,
    narratorAgent,
    noOpAgent
)
```

---

### Challenge 4: Non-LLM Agents
**Problem:** LibrettoWriter, ImageGenerator are utility classes, not LLM-based.

**Solution:**
- Agents don't require LLMs - they can be pure utility methods
- Use `@Agent` annotation without `@UserMessage`
- Implementation directly delegates to existing classes

---

## Dependencies Required

```kotlin
// build.gradle.kts additions
dependencies {
    // Agentic framework (experimental)
    implementation("dev.langchain4j:langchain4j-agentic:1.10.0")

    // Optional: Advanced patterns (if needed for Phase 4)
    // implementation("dev.langchain4j:langchain4j-agentic-patterns:1.10.0")
}

// Compilation flag for parameter name inference
tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}
```

---

## Testing Strategy

### Unit Tests
- Test each agent independently
- Mock dependencies (models, file I/O)
- Verify input/output contracts

### Integration Tests
- Test sequential workflow end-to-end
- Verify AgenticScope state passing
- Compare output to legacy implementation

### Observability Tests
- Verify AgentListener callbacks fire
- Test logging at each agent invocation
- Ensure error propagation works correctly

---

## Migration Risks & Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Breaking changes in experimental API | High | Pin to 1.10.0, monitor LangChain4j releases |
| Performance regression | Medium | Benchmark before/after, optimize if needed |
| ChatMemory behavior change | Medium | Thorough testing of scene continuity |
| Alternating model pattern breaks | High | Keep fallback to manual orchestration |
| Increased complexity | Low | Document thoroughly, provide migration guide |

---

## Success Metrics

**Phase 1 (Sequential):**
- ✅ Generates identical output to current implementation
- ✅ Better observability through AgentListener
- ✅ Cleaner separation of concerns (agents vs orchestration)

**Phase 2 (Conditional):**
- ✅ Graceful degradation without optional API keys
- ✅ Clear logging when skipping optional steps

**Phase 3 (Parallel):**
- ✅ 50%+ faster image generation
- ✅ No rate limit violations

**Phase 4 (Loop):**
- ✅ Iteratively improves opera quality
- ✅ Achieves 7.0+ scores consistently
- ✅ User control over refinement iterations

---

## Future Enhancements

### Supervisor Agent (Phase 5?)
- LLM dynamically plans execution order
- "I need to generate an opera about space pirates with 3 scenes, high-quality images, and audio narration"
- Supervisor decides: SceneGenerator → CriticLoop(3 iterations) → ImageGenerator(high quality) → Narrator

### Peer-to-Peer Agents (Phase 6?)
- Multiple critic agents debate quality
- Agents propose scene revisions independently
- Decentralized consensus on final version

### Goal-Oriented Planning
- Define goal: "Complete opera with 8/10 critic score"
- Planner calculates optimal path through agent graph
- Minimizes total agent invocations

---

## Next Steps

1. **Review this plan** with user
2. **Clear context** to start fresh
3. **Create branch** `agentic-refactor`
4. **Start Phase 1** implementation
5. **Test incrementally** after each agent

---

## Decisions Made (2026-01-23)

After discussion, the following decisions were made:

1. **Truly agentic approach**: Use Supervisor-based pattern with autonomous decision-making (not just wrapper agents)
2. **Model diversity (Option 2B)**: Multiple models with flexible assignment, not strict alternation
3. **Non-LLM utilities as tools**: LibrettoWriter, ImageGenerator, etc. are tools called by agents, not agents themselves
4. **Include parallel image generation**: Phase 3 is important for demo performance
5. **Skip refinement loop**: Phase 4 (critic feedback iteration) not needed for initial demo
6. **Keep legacy on main**: Separate `agentic-refactor` branch, preserve working manual orchestration on main
7. **Primary goal**: Educational demo for conference presentations and training courses

---

## Revised Implementation Scope

Based on decisions above, the implementation will include:
- **Phase 1**: Sequential workflow with Supervisor Agent
- **Phase 2**: Conditional execution (skip optional steps gracefully)
- **Phase 3**: Parallel image generation

**Not included initially:**
- Phase 4 (Refinement Loop)
- Phase 5+ (Advanced patterns)

---

**End of Migration Plan**
