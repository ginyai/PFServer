package cn.pfcraft.server.remapper;

import cn.pfcraft.server.utils.Tuple;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import net.md_5.specialsource.JarMapping;
import net.md_5.specialsource.provider.JointProvider;
import org.objectweb.asm.*;

import java.util.HashMap;
import java.util.Set;

public class ReflectionTransformer extends ClassVisitor {
    public static final String DESC_ReflectionMethods = Type.getInternalName(ReflectionMethods.class);
    private static final Set<Tuple<String, String>> remappedClassMethods = ImmutableSet.of(
            Tuple.of("getField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;"),
            Tuple.of("getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;"),
            Tuple.of("getMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;"),
            Tuple.of("getDeclaredMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;"),
            //Todo:Tuple.of("getName","()Ljava/lang/String;"),
            //Todo:Tuple.of("getCanonicalName","()Ljava/lang/String;"),
            Tuple.of("getSimpleName", "()Ljava/lang/String;")
    );
    private static final Set<Tuple<String, String>> remappedLookupMethods = ImmutableSet.of(
            Tuple.of("findStatic", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;"),
            Tuple.of("findVirtual", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;"),
            Tuple.of("findSpecial", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;"),
            Tuple.of("findGetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;"),
            Tuple.of("findSetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;"),
            Tuple.of("findStaticGetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;"),
            Tuple.of("findStaticSetter", "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;"),
            Tuple.of("bind", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;")
    );

    public static JarMapping jarMapping;
    public static PFServerRemapper remapper;

    public static final HashMap<String, String> classDeMapping = Maps.newHashMap();
    public static final Multimap<String, String> methodDeMapping = ArrayListMultimap.create();
    public static final Multimap<String, String> fieldDeMapping = ArrayListMultimap.create();
    public static final Multimap<String, String> methodFastMapping = ArrayListMultimap.create();

    private static boolean disable = false;

    public static void init() {
        try {
            ReflectionUtils.getCallerClassloader();
        } catch (Throwable e) {
            new RuntimeException("Unsupported Java version, disabled reflection remap!", e).printStackTrace();
            disable = true;
        }
        jarMapping = MappingLoader.loadMapping();
        JointProvider provider = new JointProvider();
        provider.add(new ClassInheritanceProvider());
        jarMapping.setFallbackInheritanceProvider(provider);
        remapper = new PFServerRemapper(jarMapping);

        jarMapping.classes.forEach((k, v) -> classDeMapping.put(v, k));
        jarMapping.methods.forEach((k, v) -> methodDeMapping.put(v, k));
        jarMapping.fields.forEach((k, v) -> fieldDeMapping.put(v, k));
        jarMapping.methods.forEach((k, v) -> methodFastMapping.put(k.split("\\s+")[0], k));
    }

    private ReflectionTransformer(ClassVisitor cv) {
        super(Opcodes.ASM6, cv);
    }

    /**
     * Convert code from using Class.X methods to our remapped versions
     */
    public static byte[] transform(byte[] code) {//todo:Lookup.defineClass?
        if (disable) {
            return code;
        }
        ClassReader reader = new ClassReader(code); // Turn from bytes into visitor
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ClassVisitor visitor = new ReflectionTransformer(writer);
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if ("java/net/URLClassLoader".equals(superName)) {
            super.visit(version, access, name, signature, "cn/pfcraft/server/remapper/PFServerURLClassLoader", interfaces);
        } else {
            super.visit(version, access, name, signature, superName, interfaces);
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return new RefMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions));
    }

    private static class RefMethodVisitor extends MethodVisitor {

        RefMethodVisitor(MethodVisitor mv) {
            super(Opcodes.ASM6, mv);
        }
        //todo: Class # getName getCanonicalName
        //todo: MethodHandles.Lookup # findClass  ?

        @Override
        public void visitTypeInsn(int opcode, String type) {
            if (opcode == Opcodes.NEW && "java/net/URLClassLoader".equals(type)) {
                super.visitTypeInsn(opcode, "cn/pfcraft/server/remapper/PFServerURLClassLoader");
            } else {
                super.visitTypeInsn(opcode, type);
            }
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (opcode == Opcodes.INVOKESTATIC && "java/lang/Class".equals(owner) && "forName".equals(name)) {
                //Class#forName     Do we need this here?
                super.visitMethodInsn(opcode, DESC_ReflectionMethods, name, desc, itf);
            } else if (opcode == Opcodes.INVOKESPECIAL && "java/net/URLClassLoader".equals(owner) && "<init>".equals(name)) {
                //replace init super URLClassLoader
                super.visitMethodInsn(opcode, "cn/pfcraft/server/remapper/PFServerURLClassLoader", name, desc, itf);
            } else if (opcode == Opcodes.INVOKEVIRTUAL && "java/lang/Class".equals(owner) && remappedClassMethods.contains(Tuple.of(name, desc))) {
                //Class#remappedClassMethods
                super.visitMethodInsn(Opcodes.INVOKESTATIC, DESC_ReflectionMethods, name, desc.replaceFirst("\\(", "(Ljava/lang/Class;"), itf);
            } else if (opcode == Opcodes.INVOKEVIRTUAL && "java/lang/reflect/Method".equals(owner) && "getName".equals(name) && "()Ljava/lang/String;".equals(desc)) {
                //Method#getName
                super.visitMethodInsn(Opcodes.INVOKESTATIC, DESC_ReflectionMethods, name, "(Ljava/lang/reflect/Method;)Ljava/lang/String;", itf);
            } else if (opcode == Opcodes.INVOKEVIRTUAL && "java/lang/reflect/Field".equals(owner) && "getName".equals(name) && "()Ljava/lang/String;".equals(desc)) {
                //Field#getName
                super.visitMethodInsn(Opcodes.INVOKESTATIC, DESC_ReflectionMethods, name, "(Ljava/lang/reflect/Field;)Ljava/lang/String;", itf);
            } else if (opcode == Opcodes.INVOKEVIRTUAL && "java/lang/invoke/MethodHandles$Lookup".equals(owner) && remappedLookupMethods.contains(Tuple.of(name, desc))) {
                //MethodHandles.Lookup
                super.visitMethodInsn(Opcodes.INVOKESTATIC, DESC_ReflectionMethods, name, desc.replaceFirst("\\(", "(Ljava/lang/invoke/MethodHandles\\$Lookup;"), itf);
            } else if (opcode == Opcodes.INVOKESPECIAL && "javax/script/ScriptEngineManager".equals(owner) && "<init>".equals(name) && "()V".equals(desc)) {
                //ScriptEngineManager todo: new ScriptEngineManager()?
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/ClassLoader", "getSystemClassLoader", "()Ljava/lang/ClassLoader;", false);
                super.visitMethodInsn(opcode, owner, name, "(Ljava/lang/ClassLoader;)V", itf);
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }
    }
}
