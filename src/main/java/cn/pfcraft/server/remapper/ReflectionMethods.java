package cn.pfcraft.server.remapper;

import cn.pfcraft.server.PFServer;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

public class ReflectionMethods {

    private final static ConcurrentHashMap<String, String> fieldGetNameCache = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<String, String> methodGetNameCache = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<String, String> simpleNameGetNameCache = new ConcurrentHashMap<>();

    private static String findNMSParent(Class<?> clazz) {//todo:interface?
        while (clazz != null) {
            if (clazz.getName().startsWith("net.minecraft.")) {
                return Type.getInternalName(clazz);
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    // Class.forName
    public static Class<?> forName(String className) throws ClassNotFoundException {
        return forName(className, true, ReflectionUtils.getCallerClassloader());
    }

    public static Class<?> forName(String className, boolean initialize, ClassLoader classLoader) throws ClassNotFoundException {
        if (!className.startsWith("net.minecraft.server." + PFServer.getNativeVersion())) return Class.forName(className, initialize, classLoader);
        className = ReflectionTransformer.jarMapping.classes.getOrDefault(className.replace('.', '/'), className).replace('/', '.');
        return Class.forName(className, initialize, classLoader);
    }

    // Get Fields
    public static Field getField(Class<?> inst, String name) throws NoSuchFieldException, SecurityException {
        String className = findNMSParent(inst);
        if(className == null) {
            return inst.getField(name);
        } else {
            return inst.getField(ReflectionTransformer.remapper.mapFieldName(RemapUtils.reverseMap(className), name, null));
        }
    }

    public static Field getDeclaredField(Class<?> inst, String name) throws NoSuchFieldException, SecurityException {
        if (!inst.getName().startsWith("net.minecraft.")) return inst.getDeclaredField(name);
        return inst.getDeclaredField(ReflectionTransformer.remapper.mapFieldName(RemapUtils.reverseMap(inst), name, null));
    }

    // Get Methods
    public static Method getMethod(Class<?> inst, String name, Class<?>...parameterTypes) throws NoSuchMethodException, SecurityException {
        if (!inst.getName().startsWith("net.minecraft.")) return inst.getMethod(name, parameterTypes);
        return inst.getMethod(RemapUtils.mapMethod(inst, name, parameterTypes), parameterTypes);
    }

    public static Method getDeclaredMethod(Class<?> inst, String name, Class<?>...parameterTypes) throws NoSuchMethodException, SecurityException {
        if (!inst.getName().startsWith("net.minecraft.")) return inst.getDeclaredMethod(name, parameterTypes);
        return inst.getDeclaredMethod(RemapUtils.mapMethod(inst, name, parameterTypes), parameterTypes);
    }

    // getName
    public static String getName(Field field) {
        if (!field.getDeclaringClass().getName().startsWith("net.minecraft.")) return field.getName();
        String hash = String.valueOf(field.hashCode());
        String cache = fieldGetNameCache.get(hash);
        if (cache != null) return cache;
        String retn = RemapUtils.demapFieldName(field);
        fieldGetNameCache.put(hash, retn);
        return retn;
    }

    public static String getName(Method method) {
        if (!method.getDeclaringClass().getName().startsWith("net.minecraft.")) return method.getName();
        String hash = String.valueOf(method.hashCode());
        String cache = methodGetNameCache.get(hash);
        if (cache != null) return cache;
        String retn = RemapUtils.demapMethodName(method);
        methodGetNameCache.put(hash, retn);
        return retn;
    }

    // getSimpleName
    public static String getSimpleName(Class<?> inst) {
        if (!inst.getName().startsWith("net.minecraft.")) return inst.getSimpleName();
        String hash = String.valueOf(inst.hashCode());
        String cache = simpleNameGetNameCache.get(hash);
        if (cache != null) return cache;
        String[] name = RemapUtils.reverseMapExternal(inst).split("\\.");
        String retn = name[name.length - 1];
        simpleNameGetNameCache.put(hash, retn);
        return retn;
    }

    // ClassLoader.loadClass
    public static Class<?> loadClass(ClassLoader inst, String className) throws ClassNotFoundException {
        if (className.startsWith("net.minecraft."))
            className = RemapUtils.mapClass(className.replace('.', '/')).replace('/', '.');
        return inst.loadClass(className);
    }

    // MethodHandles.Lookup
    public static MethodHandle findStatic(MethodHandles.Lookup lookup, Class<?> refc, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
        if (!(refc.getName().startsWith("net.minecraft."))) return lookup.findStatic(refc, name, type);
        return lookup.findStatic(refc, RemapUtils.mapMethod(refc, name, type.parameterArray()), type);
    }

    public static MethodHandle findVirtual(MethodHandles.Lookup lookup, Class<?> refc, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
        if (!(refc.getName().startsWith("net.minecraft."))) return lookup.findVirtual(refc, name, type);
        return lookup.findVirtual(refc, RemapUtils.mapMethod(refc, name, type.parameterArray()), type);
    }

    public static MethodHandle findSpecial(MethodHandles.Lookup lookup, Class<?> refc, String name, MethodType type, Class<?> specialCaller) throws NoSuchMethodException, IllegalAccessException {
        if (!(refc.getName().startsWith("net.minecraft."))) return lookup.findSpecial(refc, name, type, specialCaller);
        return lookup.findSpecial(refc, RemapUtils.mapMethod(refc, name, type.parameterArray()), type, specialCaller);
    }

    public static MethodHandle findGetter(MethodHandles.Lookup lookup, Class<?> refc, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
        if (!(refc.getName().startsWith("net.minecraft."))) return lookup.findGetter(refc, name, type);
        return lookup.findGetter(refc, ReflectionTransformer.remapper.mapFieldName(RemapUtils.reverseMap(refc), name, null), type);
    }

    public static MethodHandle findSetter(MethodHandles.Lookup lookup, Class<?> refc, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
        if (!(refc.getName().startsWith("net.minecraft."))) return lookup.findSetter(refc, name, type);
        return lookup.findSetter(refc, ReflectionTransformer.remapper.mapFieldName(RemapUtils.reverseMap(refc), name, null), type);
    }

    public static MethodHandle findStaticGetter(MethodHandles.Lookup lookup, Class<?> refc, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
        if (!(refc.getName().startsWith("net.minecraft."))) return lookup.findStaticGetter(refc, name, type);
        return lookup.findStaticGetter(refc, ReflectionTransformer.remapper.mapFieldName(RemapUtils.reverseMap(refc), name, null), type);
    }

    public static MethodHandle findStaticSetter(MethodHandles.Lookup lookup, Class<?> refc, String name, Class<?> type) throws NoSuchFieldException, IllegalAccessException {
        if (!(refc.getName().startsWith("net.minecraft."))) return lookup.findStaticSetter(refc, name, type);
        return lookup.findStaticSetter(refc, ReflectionTransformer.remapper.mapFieldName(RemapUtils.reverseMap(refc), name, null), type);
    }

    public static MethodHandle bind(MethodHandles.Lookup lookup, Object receiver, String name, MethodType type) throws NoSuchMethodException, IllegalAccessException {
        if (!(receiver.getClass().getName().startsWith("net.minecraft.")))
            return lookup.bind(receiver, name, type);//todo:handle override nms methods
        return lookup.bind(receiver, RemapUtils.mapMethod(receiver.getClass(), name, type.parameterArray()), type);
    }

}
