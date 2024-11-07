# FAQ

Common questions and their answers!

## The Compiled Graph Is thread safe?

Yes. the Compiled Graph is thread safe. It work on a new (and not shared) copy of state for each execution.

## Is the Compiled Graph + Checkpoints + MemorySaver thread safe ?

Yes. But the state consistency between parallel calls to  `CompiledGraph.updateState()` is up to you.
