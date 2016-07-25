package stiletto.compiler

import com.squareup.javapoet.*
import dagger.Component
import dagger.Provides
import stiletto.ProvidedBy
import stiletto.Stiletto.Module
import java.util.*
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror


class StilettoModule(val annotatedClass: TypeElement) {

   companion object {
      val PROVISION_ANNOTATIONS = arrayOf(
            ProvidedBy.Runtime::class.java,
            ProvidedBy.Injection::class.java,
            ProvidedBy.NewInstance::class.java
      )
   }

   val insideDaggerComponent: Boolean

   val moduleClassName: ClassName
   val builderClassName: ClassName

   val moduleAnnotation = annotatedClass.getAnnotation(Module::class.java)
   val providedTypes: List<ProvidedType>

   init {
      insideDaggerComponent = annotatedClass.hasAnnotation(Component::class.java)

      val elementClassName = ClassName.get(annotatedClass)
      val componentSuffixIndex = elementClassName.simpleName().lastIndexOf("Component")
      val moduleSimpleName = if (componentSuffixIndex > 0) {
         elementClassName.simpleName().substring(0, componentSuffixIndex) + "Module"
      } else {
         elementClassName.simpleName() + "Module"
      }

      moduleClassName = ClassName.get(elementClassName.packageName(), moduleSimpleName)
      builderClassName = ClassName.get(elementClassName.packageName(), moduleSimpleName, "Builder")

      providedTypes = ArrayList<ProvidedType>().let { methods ->
         annotatedClass.allEnclosedElements
               .filter { it.isMethod }
               .map { it as ExecutableElement }
               .forEach {
                  if (it.hasAnnotation(ProvidedBy.Runtime::class.java))
                     methods += ProvidedType.Runtime(this, it)
                  if (it.hasAnnotation(ProvidedBy.Injection::class.java))
                     methods += ProvidedType.Injection(this, it)
                  if (it.hasAnnotation(ProvidedBy.NewInstance::class.java))
                     methods += ProvidedType.NewInstance(this, it)
               }
         methods
      }
   }

   fun generateClass() {
      val moduleTypeSpec = TypeSpec.classBuilder(moduleClassName.simpleName())
            .addAnnotation(dagger.Module::class.java)
            .addModifiers(Modifier.PUBLIC)

      generateNewInstanceProvisions(moduleTypeSpec)
      generateInjectionProvisions(moduleTypeSpec)
      generateRuntimeProvisions(moduleTypeSpec)

      JavaFile.builder(moduleClassName.packageName(), moduleTypeSpec.build()).build()
            .writeTo(ProcessorUtils.env.filer)
   }

   fun generateNewInstanceProvisions(moduleTypeSpec: TypeSpec.Builder) {
      val newInstanceTypes = providedTypes.filter { it is ProvidedType.NewInstance }
            .map { it as ProvidedType.NewInstance }

      newInstanceTypes.forEach {
         moduleTypeSpec.addMethod(it.provisionMethodTemplate.toBuilder()
               .addParameters(it.constructorParameters)
               .apply {
                  val joinedParams = it.constructorParameters.joinToString(separator = ", ") { it.name }
                  addStatement("return new \$T($joinedParams)", it.implementationTypeName)
               }
               .build())
      }
   }

   fun generateInjectionProvisions(moduleTypeSpec: TypeSpec.Builder) {
      val abstractTypes = providedTypes.filter { it is ProvidedType.Injection }
            .map { it as ProvidedType.Injection }

      abstractTypes.forEach {
         moduleTypeSpec.addMethod(it.provisionMethodTemplate.toBuilder()
               .addParameter(ParameterSpec.builder(it.implementationTypeName, it.uniqueName)
                     .addAnnotations(it.parameterQualifiers)
                     .build())
               .addStatement("return ${it.uniqueName}")
               .build())
      }
   }

   fun generateRuntimeProvisions(moduleTypeSpec: TypeSpec.Builder) {
      val runtimeTypes = providedTypes.filter { it is ProvidedType.Runtime }

      val builderTypeSpec = TypeSpec.classBuilder(builderClassName.simpleName())
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)

      val builderBuildMethod = MethodSpec.methodBuilder("build")
            .addModifiers(Modifier.PUBLIC)

      val moduleConstructorMethod = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)

      //Generate the corresponding builder method and field
      runtimeTypes.forEach {
         builderTypeSpec
               .addField(it.typeName, it.uniqueName, Modifier.PRIVATE)
               .addMethod(MethodSpec.methodBuilder(it.uniqueName)
                     .addModifiers(Modifier.PUBLIC)
                     .addParameter(it.typeName, it.uniqueName)
                     .addStatement("this.${it.uniqueName} = ${it.uniqueName}")
                     .addStatement("return this")
                     .returns(builderClassName)
                     .build())

         moduleTypeSpec
               .addField(it.typeName, it.uniqueName, Modifier.PRIVATE, Modifier.FINAL)

         moduleConstructorMethod
               .addParameter(it.typeName, it.uniqueName)
               .addStatement("this.${it.uniqueName} = ${it.uniqueName}")

         if (!it.nullable) { //Add null check
            builderBuildMethod.addStatement(
                  "if(${it.uniqueName} == null) " +
                        "throw new NullPointerException(\"${it.uniqueName} == null\")")
         }

         //Add the @Provide method
         moduleTypeSpec.addMethod(it.provisionMethodTemplate.toBuilder()
               .addStatement("return this.${it.uniqueName}")
               .build())
      }

      builderTypeSpec
            .addMethod(builderBuildMethod
                  .addStatement("return new \$T(\$L)", moduleClassName,
                        runtimeTypes.joinToString(separator = ", ") { it.uniqueName })
                  .returns(moduleClassName)
                  .build())

      moduleTypeSpec.addMethod(moduleConstructorMethod.build())

      //builder helper
      moduleTypeSpec.addMethod(MethodSpec.methodBuilder("builder")
            .addModifiers(Modifier.STATIC)
            .addStatement("return new \$T()", builderClassName)
            .returns(builderClassName)
            .build())

      //if all types are nullable add the create method shortcut
      if (runtimeTypes.isEmpty() || runtimeTypes.all { it.nullable }) {
         moduleTypeSpec.addMethod(MethodSpec.methodBuilder("create")
               .addJavadoc("Create the module with default values (null, 0)")
               .addModifiers(Modifier.STATIC)
               .addStatement("return builder().build()")
               .returns(moduleClassName)
               .build())
      }

      moduleTypeSpec.addType(builderTypeSpec.build())

   }

   sealed class ProvidedType(val stilettoModule: StilettoModule,
                             val annotatedMethod: ExecutableElement) {

      val typeElement: Element
      val typeName: TypeName
      val uniqueName: String
      val provisionMethodName: String
      val nullable: Boolean
      val provisionMethodTemplate: MethodSpec
      val parameterQualifiers: List<AnnotationSpec>

      init {
         typeElement = annotatedMethod.returnType.asElement()
         typeName = annotatedMethod.returnType.toTypeName()
         uniqueName = annotatedMethod.simpleName.toString()
         nullable = annotatedMethod.isNullable
         provisionMethodName = "provide${uniqueName.capitalize()}"

         if (annotatedMethod.parameters.isNotEmpty() && stilettoModule.insideDaggerComponent) {
            logError("Methods annotated with @ProvidedBy " +
                  "can't have parameters when declared " +
                  "alongside the dagger @Component", annotatedMethod)
         }

         //Carry the annotations from the parameter, we don't care about the type,
         //its never used
         if (annotatedMethod.parameters.size == 1) {
            parameterQualifiers = annotatedMethod.parameters[0].copyAnnotations()
         } else {
            parameterQualifiers = emptyList()
         }

         provisionMethodTemplate = MethodSpec.methodBuilder(provisionMethodName)
               .addModifiers(Modifier.PUBLIC)
               .addAnnotation(Provides::class.java)
               .addAnnotations(annotatedMethod.copyAnnotations(*PROVISION_ANNOTATIONS))
               .returns(typeName)
               .build()
      }

      class Runtime(stilettoModule: StilettoModule, annotatedMethod: ExecutableElement)
      : ProvidedType(stilettoModule, annotatedMethod)

      class Injection(stilettoModule: StilettoModule, annotatedMethod: ExecutableElement)
      : ProvidedType(stilettoModule, annotatedMethod) {

         val implementationClassElement: TypeElement
         val implementationClassMirror: TypeMirror
         val implementationTypeName: TypeName

         init {
            val annotation = annotatedMethod.getAnnotation(ProvidedBy.Injection::class.java)
            implementationClassMirror = annotation.typeMirror { this.value }
            implementationClassElement = implementationClassMirror.asTypeElement()
            implementationTypeName = implementationClassMirror.toTypeName()

            if (!(implementationClassMirror isSubtypeOf annotatedMethod.returnType)) {
               logError("Injected type $implementationTypeName " +
                     "is not a subtype of ${annotatedMethod.returnType.toTypeName()}", annotatedMethod)
            }
         }
      }

      class NewInstance(stilettoModule: StilettoModule, annotatedMethod: ExecutableElement)
      : ProvidedType(stilettoModule, annotatedMethod) {

         val implementationClassElement: TypeElement
         val implementationClassMirror: TypeMirror
         val implementationTypeName: TypeName
         val constructorParameters: List<ParameterSpec>

         init {
            val annotation = annotatedMethod.getAnnotation(ProvidedBy.NewInstance::class.java)
            implementationClassMirror = annotation.typeMirror { this.value }.let {
               //Default value, assume the return type is the implementation
               if (it.isSameType(elementForName("java.lang.Void").asType())) annotatedMethod.returnType
               else it //The specified value
            }
            implementationClassElement = implementationClassMirror.asTypeElement()
            implementationTypeName = implementationClassMirror.toTypeName()

            if (!(implementationClassMirror isSubtypeOf annotatedMethod.returnType)) {
               logError("Implementation type $implementationTypeName " +
                     "is not a subtype of ${annotatedMethod.returnType.toTypeName()}", annotatedMethod)
            }

            if (implementationClassElement.isAbstract || implementationClassElement.isInterface)
               logError("Can't create instances of abstract classes or interfaces " +
                     "for $implementationTypeName", annotatedMethod)

            val constructors = implementationClassElement.enclosedElements
                  .filter { it.isConstructor }
                  .map { it as ExecutableElement }
                  .filter {
                     !it.modifiers.any {
                        //Skip private or protected constructors for validation
                        it == Modifier.PRIVATE || it == Modifier.PROTECTED
                     }
                  }

            if (constructors.isEmpty()) logError("No public or package constructors " +
                  "for $implementationTypeName " +
                  "required by @ProvidedBy.NewInstance", annotatedMethod)

            if (constructors.size != 1) logError("More than one constructor available " +
                  "for $implementationTypeName " +
                  "@ProvidedBy.NewInstance can't chose one", annotatedMethod)

            constructorParameters = constructors[0].parameters.map {
               ParameterSpec.builder(it.asType().toTypeName(), it.simpleName.toString())
                     .addAnnotations(it.copyAnnotations())
                     .build()
            }
         }
      }
   }
}