package com.vora.agent.mapping;

import org.objectweb.asm.commons.Remapper;





public final class ChainedRemapper extends Remapper {

    private final Remapper first;
    private final Remapper second;

    public ChainedRemapper(Remapper first, Remapper second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public String map(String internalName) {
        return second.map(first.map(internalName));
    }

    @Override
    public String mapMethodName(String owner, String name, String descriptor) {
        String owner2 = first.map(owner);
        String name2 = first.mapMethodName(owner, name, descriptor);
        String desc2 = first.mapMethodDesc(descriptor);
        return second.mapMethodName(owner2, name2, desc2);
    }

    @Override
    public String mapFieldName(String owner, String name, String descriptor) {
        String owner2 = first.map(owner);
        String name2 = first.mapFieldName(owner, name, descriptor);
        String desc2 = first.mapDesc(descriptor);
        return second.mapFieldName(owner2, name2, desc2);
    }

    @Override
    public String mapSignature(String signature, boolean typeSignature) {
        String once = first.mapSignature(signature, typeSignature);
        return second.mapSignature(once, typeSignature);
    }
}
