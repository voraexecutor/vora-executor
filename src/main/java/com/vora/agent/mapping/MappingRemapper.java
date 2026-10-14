package com.vora.agent.mapping;

import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.SignatureRemapper;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureWriter;






public final class MappingRemapper extends Remapper {

    private final Mapping mapping;
    private final ClassHierarchy hierarchy;

    public MappingRemapper(Mapping mapping) {
        this(mapping, null);
    }

    public MappingRemapper(Mapping mapping, ClassHierarchy hierarchy) {
        this.mapping = mapping;
        this.hierarchy = hierarchy;
    }

    public Mapping mapping() {
        return mapping;
    }

    @Override
    public String map(String internalName) {
        return mapping.mapClass(internalName);
    }

    @Override
    public String mapMethodName(String owner, String name, String descriptor) {
        String mapped = mapping.mapMethod(owner, name, descriptor);
        if (mapped != null) return mapped;
        if (hierarchy != null) {
            for (String ancestor : hierarchy.ancestors(owner)) {
                mapped = mapping.mapMethod(ancestor, name, descriptor);
                if (mapped != null) return mapped;
            }
        }
        return name;
    }

    @Override
    public String mapFieldName(String owner, String name, String descriptor) {
        String mapped = mapping.mapField(owner, name, descriptor);
        if (mapped != null) return mapped;
        if (hierarchy != null) {
            for (String ancestor : hierarchy.ancestors(owner)) {
                mapped = mapping.mapField(ancestor, name, descriptor);
                if (mapped != null) return mapped;
            }
        }
        return name;
    }

    @Override
    public String mapSignature(String signature, boolean typeSignature) {
        if (signature == null) return null;
        SignatureWriter writer = new SignatureWriter();
        SignatureRemapper remapper = new SignatureRemapper(writer, this);
        SignatureReader reader = new SignatureReader(signature);
        if (typeSignature) {
            reader.acceptType(remapper);
        } else {
            reader.accept(remapper);
        }
        return writer.toString();
    }
}
