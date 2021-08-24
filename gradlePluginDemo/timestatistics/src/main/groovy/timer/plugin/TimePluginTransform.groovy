package joyy;

import com.android.build.api.transform.*;
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter;

class TimePluginTransform extends Transform {
    @Override
    public String getName() {
        return TimePluginTransform.class.getSimpleName()
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    /**
     * 指Transform要操作内容的范围，官方文档Scope有7种类型：
     * <p>
     * EXTERNAL_LIBRARIES        只有外部库
     * PROJECT                   只有项目内容
     * PROJECT_LOCAL_DEPS        只有项目的本地依赖(本地jar)
     * PROVIDED_ONLY             只提供本地或远程依赖项
     * SUB_PROJECTS              只有子项目。
     * SUB_PROJECTS_LOCAL_DEPS   只有子项目的本地依赖项(本地jar)。
     * TESTED_CODE               由当前变量(包括依赖项)测试的代码
     * SCOPE_FULL_PROJECT        整个项目
     */
    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        transformInvocation.inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput directoryInput ->
                if (directoryInput.file.isDirectory()) {
                    directoryInput.file.eachFileRecurse { File file ->
                        tranformFile(file)
                    }
                } else {
                    tranformFile(file)
                }
                // Transform 拷贝文件到 transforms 目录
                File dest = transformInvocation.outputProvider.getContentLocation(
                        directoryInput.getName(),
                        directoryInput.getContentTypes(),
                        directoryInput.getScopes(),
                        Format.DIRECTORY);
                // 将修改过的字节码copy到dest，实现编译期间干预字节码
                FileUtils.copyDirectory(directoryInput.getFile(), dest);
            }

            input.jarInputs.each { JarInput jarInput ->
                def jarName = jarInput.name
                def dest = transformInvocation.outputProvider.getContentLocation(jarName,
                        jarInput.contentTypes, jarInput.scopes, Format.JAR)

                FileUtils.copyFile(jarInput.getFile(), dest)
            }
        }

    }

    // 处理响应的文件
    private void tranformFile(File file) throws IOException {
        def name = file.name
        if (filerClass(name)) {
            ClassReader reader = new ClassReader(file.bytes)
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS)
            ClassVisitor visitor = new TimePluginClassVisitor(writer)
            reader.accept(visitor, ClassReader.EXPAND_FRAMES)

            byte[] code = writer.toByteArray()
            def classPath = file.parentFile.absolutePath + File.separator + name
            FileOutputStream fos = new FileOutputStream(classPath)
            fos.write(code)
            fos.close()
        }
    }

    private boolean filerClass(String name) {
        return name.endsWith("Activity.class")
    }
}
