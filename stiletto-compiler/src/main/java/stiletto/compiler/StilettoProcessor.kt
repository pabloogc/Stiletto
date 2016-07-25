package stiletto.compiler

import stiletto.Stiletto.Module
import java.io.PrintWriter
import java.io.StringWriter
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter

object StilettoProcessor : AbstractProcessor() {

   override fun init(env: ProcessingEnvironment) {
      ProcessorUtils.env = env
   }

   override fun getSupportedAnnotationTypes(): MutableSet<String>? {
      return mutableSetOf(Module::class.java)
            .map { it.canonicalName }
            .toMutableSet()
   }

   override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
      try {
         roundEnv.getElementsAnnotatedWith(Module::class.java)
               .let { ElementFilter.typesIn(it) } //Poor man's filter
               .map { StilettoModule(it) }
               .forEach { it.generateClass() }
      } catch(ex: Exception) {
         val sw = StringWriter()
         ex.printStackTrace(PrintWriter(sw))
         logError(sw.toString()
               .lines()
               //Skip all the irrelevant gradle trace.
               .takeWhile { !it.contains("JavacProcessingEnvironment.callProcessor") }
               .joinToString("\n"))
      }
      return true
   }

}