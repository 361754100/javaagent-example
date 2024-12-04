package com.lzy.javaagent;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;

/**
 * @author liuzhengyang
 * 2022/4/13
 */
public class AOPTransformer implements ClassFileTransformer {

    private final String className;

    public AOPTransformer(String className) {
        this.className = className;
    }

    /**
     * 注意这里的className是 a/b/C这样的而不是a.b.C
     */
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (className == null) {
            // 返回null表示不修改类字节码，和返回classfileBuffer是一样的效果。
            return null;
        }
        if (className.equals(this.className.replace('.', '/'))) {
            ClassPool classPool = ClassPool.getDefault();
            classPool.appendClassPath(new LoaderClassPath(loader));
            classPool.appendSystemPath();
            try {
                CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
                CtMethod[] declaredMethods = ctClass.getDeclaredMethods();
                for (CtMethod declaredMethod : declaredMethods) {
                    if(declaredMethod == null) {
                        continue;
                    }
                    // 在方法开头插入获取当前时间的代码
                    declaredMethod.addLocalVariable("startTime", CtClass.longType);
                    declaredMethod.insertBefore("{ startTime = System.currentTimeMillis(); }");

                    // 在方法结尾插入计算时间差的代码并输出
                    declaredMethod.insertAfter("{ System.out.println(\"" + declaredMethod.getName() + " execution time: \" + (System.currentTimeMillis() - startTime) + \"ms\"); }");
                }
                return ctClass.toBytecode();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return classfileBuffer;
    }
}
