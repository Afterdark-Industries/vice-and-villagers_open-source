package com.trippycraft.substance;

@FunctionalInterface
public interface EffectStageResolver<S, E extends Enum<E>> {
    E resolve(S state);
}
