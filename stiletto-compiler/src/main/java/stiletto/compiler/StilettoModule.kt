package stiletto.compiler

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import stiletto.Module
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement


class StilettoModule(val element: TypeElement) {

   val moduleClassName: ClassName
   val moduleAnnotation = element.getAnnotation(Module::class.java)

   init {
      val elementClassName = ClassName.get(element)
      val componentSuffixIndex = elementClassName.simpleName().lastIndexOf("Component")
      val moduleSimpleName = if (componentSuffixIndex > 0) {
         elementClassName.simpleName().substring(componentSuffixIndex) + "Module"
      } else {
         elementClassName.simpleName() + "Module"
      }
      moduleClassName = ClassName.get(elementClassName.packageName(), moduleSimpleName)



   }


   fun generateClass() {
      val moduleTypeSpec = TypeSpec.classBuilder(moduleClassName.simpleName())
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(dagger.Module::class.java)

      JavaFile.builder(moduleClassName.packageName(), moduleTypeSpec.build()).build()
            .writeTo(ProcessorUtils.env.filer)
   }
}