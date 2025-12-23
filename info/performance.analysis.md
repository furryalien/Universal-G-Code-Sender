// Derived from https://abseil.io/fast/hints.html?utm_source=tldrdev
You are an expert software performance engineer. Analyze this application using the performance‑tuning principles described in the “Abseil Performance Hints” document by Jeff Dean and Sanjay Ghemawat.

Your task:
1. Review the entire codebase, including:
   - Application code
   - Libraries and utilities
   - Data structures
   - Algorithms
   - Tests
   - Benchmarks
   - Logs and metrics (if present)

2. Apply the performance‑engineering recommendations from the document, including but not limited to:
   - Choosing faster alternatives when they do not harm readability
   - Identifying hot paths vs. cold paths
   - Reducing unnecessary allocations
   - Improving memory layout and cache locality
   - Using compact data structures, inlined storage, arenas, and batched storage where appropriate
   - Replacing nested maps with flatter structures when beneficial
   - Preferring indices over pointers when it improves locality
   - Using view types to avoid copying
   - Adding bulk APIs to reduce boundary‑crossing overhead
   - Applying algorithmic improvements (e.g., reducing O(N²) behavior)
   - Adding fast paths for common cases
   - Hoisting work out of loops
   - Deferring expensive work until needed
   - Precomputing reusable values
   - Eliminating redundant checks and repeated parsing
   - Improving hashing and lookup performance
   - Reducing lock contention and improving thread‑safety boundaries
   - Identifying opportunities for micro‑optimizations that collectively yield large wins
   - Ensuring test code focuses on asymptotic behavior and avoids unnecessary slowness

3. For each recommended improvement:
   - Identify where in the application it applies
   - Explain why it applies
   - Propose the specific code changes
   - Implement the changes when safe
   - Provide or update tests to ensure zero regressions
   - Add microbenchmarks where appropriate
   - Estimate expected performance impact using back‑of‑the‑envelope reasoning
   - Provide a confidence score for each recommendation (0–100%)
   - Provide a regression‑risk score (0–100%)

4. Produce a final report summarizing:
   - All applied recommendations
   - All proposed but not‑applied recommendations (and why)
   - Expected performance improvements
   - Any new tests or benchmarks added
   - Any structural or algorithmic risks
   - Overall confidence in correctness and performance gains

Your output should be:
- Safe to apply incrementally
- Fully regression‑tested
- Clear about assumptions and tradeoffs
- Focused on measurable performance improvements
