/*
 * Copyright (C) filoghost and contributors
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Matches paired artifacts by unchanged instruction structure, rejecting ambiguous matches. */
public class GenerateSpigotMappings {

    private static final String[] CLASSES = {
            "world.entity.Entity", "world.entity.EntityType", "world.phys.Vec3", "network.FriendlyByteBuf",
            "network.protocol.game.ClientboundAddEntityPacket", "network.protocol.game.ClientboundSetEntityDataPacket",
            "network.protocol.game.ClientboundRemoveEntitiesPacket", "network.protocol.game.ClientboundSetPassengersPacket",
            "network.protocol.game.ClientboundTeleportEntityPacket", "world.entity.PositionMoveRotation",
            "network.syncher.SynchedEntityData$DataValue", "network.syncher.EntityDataAccessor", "server.level.ServerPlayer",
            "server.network.ServerCommonPacketListenerImpl", "network.Connection", "network.protocol.Packet",
            "network.protocol.game.ServerboundInteractPacket", "server.level.ServerLevel", "world.entity.decoration.ArmorStand",
            "world.entity.item.ItemEntity", "world.entity.monster.Slime", "nbt.TagParser", "nbt.NbtOps",
            "util.datafix.DataFixers", "util.datafix.fixes.References", "resources.RegistryOps", "core.HolderLookup$Provider",
            "world.item.ItemStack", "core.Registry", "core.RegistryAccess", "resources.ResourceKey",
            "SharedConstants", "server.Bootstrap", "core.registries.BuiltInRegistries", "network.RegistryFriendlyByteBuf",
            "network.codec.StreamCodec", "network.codec.StreamEncoder", "network.codec.StreamDecoder"
    };

    private static final String[] FIELDS = {"ENTITY_COUNTER", "ARMOR_STAND", "ITEM", "SLIME", "DATA_SHARED_FLAGS_ID",
            "DATA_CUSTOM_NAME", "DATA_CUSTOM_NAME_VISIBLE", "DATA_CLIENT_FLAGS", "DATA_ITEM", "ID_SIZE", "connection",
            "channel", "entityId", "CODEC", "INSTANCE", "ITEM_STACK", "STREAM_CODEC", "REGISTRY"};
    private static final String[] METHODS = {"writeVarInt", "create", "send", "parseCompoundFully", "parseTag",
            "getDataFixer", "getValueOrThrow", "tryDetectVersion", "bootStrap", "fromRegistryOfRegistries",
            "getY", "getData", "packedItems", "value", "encode", "decode", "getX", "getZ"};

    public static void main(String[] args) throws Exception {
        Map<String, ClassNode> runtime = readJar(args[0]);
        Map<String, ClassNode> mojang = readJar(args[1]);
        Map<String, List<ClassNode>> byFingerprint = new HashMap<>();
        for (ClassNode node : runtime.values()) {
            if (node.name.startsWith("net/minecraft/")) {
                byFingerprint.computeIfAbsent(fingerprint(node), ignored -> new ArrayList<>()).add(node);
            }
        }
        Properties properties = new Properties();
        for (String name : CLASSES) {
            String binaryName = "net/minecraft/" + name.replace('.', '/');
            ClassNode source = mojang.get(binaryName);
            if (source == null) {
                continue;
            }
            ClassNode target = runtime.get(binaryName);
            if (target == null || !fingerprint(source).equals(fingerprint(target))) {
                List<ClassNode> candidates = byFingerprint.get(fingerprint(source));
                if (candidates == null || candidates.size() != 1) {
                    throw new IllegalStateException("Ambiguous/missing structural match for " + binaryName + ": "
                            + (candidates == null ? "none" : candidates.stream().map(node -> node.name).toList()));
                }
                target = candidates.get(0);
            }
            properties.setProperty("class." + source.name.replace('/', '.'), target.name.replace('/', '.'));
            for (int index = 0; index < source.fields.size(); index++) {
                FieldNode field = source.fields.get(index);
                if (List.of(FIELDS).contains(field.name)) {
                    properties.setProperty("field." + target.name.replace('/', '.') + "." + field.name,
                            target.fields.get(index).name);
                }
            }
            for (int index = 0; index < source.methods.size(); index++) {
                MethodNode method = source.methods.get(index);
                if (List.of(METHODS).contains(method.name)) {
                    MethodNode mapped = target.methods.get(index);
                    properties.setProperty("method." + target.name.replace('/', '.') + "." + method.name
                            + arguments(mapped.desc), mapped.name);
                }
            }
        }
        String craftServer = runtime.keySet().stream().filter(name -> name.endsWith("/CraftServer"))
                .findFirst().orElseThrow();
        properties.setProperty("serverPackage", craftServer.substring(0, craftServer.lastIndexOf('/')).replace('/', '.'));
        Path path = Path.of(args[2]);
        Files.createDirectories(path.getParent());
        try (java.io.Writer writer = Files.newBufferedWriter(path)) {
            properties.store(writer, "Verified paired Spigot artifacts; no inferred field or method aliases");
        }
        System.out.println("Generated " + path + " (" + properties.size() + " verified aliases)");
    }

    private static Map<String, ClassNode> readJar(String path) throws Exception {
        Map<String, ClassNode> result = new HashMap<>();
        try (JarFile jar = new JarFile(path)) {
            for (JarEntry entry : java.util.Collections.list(jar.entries())) {
                if (entry.getName().endsWith(".class")) {
                    try (InputStream input = jar.getInputStream(entry)) {
                        ClassNode node = new ClassNode();
                        new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                        result.put(node.name, node);
                    }
                }
            }
        }
        return result;
    }

    private static String fingerprint(ClassNode node) throws Exception {
        StringBuilder result = new StringBuilder().append(node.access);
        for (FieldNode field : node.fields) {
            result.append('|').append(field.access).append(normalize(field.desc));
        }
        for (MethodNode method : node.methods) {
            result.append(';').append(method.access).append(normalize(method.desc));
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction.getOpcode() >= 0) {
                    result.append(',').append(instruction.getOpcode());
                    if (instruction instanceof IntInsnNode integer) {
                        result.append(':').append(integer.operand);
                    } else if (instruction instanceof LdcInsnNode literal && literal.cst instanceof Number) {
                        result.append(':').append(literal.cst);
                    }
                }
            }
        }
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(result.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    private static String normalize(String descriptor) {
        return descriptor.replaceAll("L[^;]+;", "L;");
    }

    private static String arguments(String descriptor) {
        StringBuilder result = new StringBuilder("(");
        for (Type type : Type.getArgumentTypes(descriptor)) {
            result.append(type.getClassName()).append(',');
        }
        return result.append(')').toString();
    }

}
