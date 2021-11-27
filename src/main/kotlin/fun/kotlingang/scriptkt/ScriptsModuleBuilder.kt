package `fun`.kotlingang.scriptkt

import `fun`.kotlingang.scriptkt.annotation.ExperimentalScriptKtApi
import `fun`.kotlingang.scriptkt.annotation.ScriptKtDSL
import `fun`.kotlingang.scriptkt.annotation.UnsafeArgumentsInput
import `fun`.kotlingang.scriptkt.compilation.CompilationFeature
import `fun`.kotlingang.scriptkt.compilation.MutableCompilationConfiguration
import `fun`.kotlingang.scriptkt.evaluation.EvaluationFeature
import `fun`.kotlingang.scriptkt.evaluation.MutableEvaluationConfiguration
import java.io.File
import kotlin.reflect.KClass

@ExperimentalScriptKtApi
@ScriptKtDSL
public fun moduleKt(mainScript: ScriptKt, block: ScriptsModuleBuilder.() -> Unit): NonCompiledConfiguredScriptsModule {
    val builder = ScriptsModuleBuilderImpl(MutableCompilationConfiguration(), MutableEvaluationConfiguration())
    builder.apply(block)
    return NonCompiledConfiguredScriptsModule(
        mainScript, builder.compilationConfiguration, builder.otherScripts, builder.evaluationConfiguration
    )
}

@ScriptKtDSL
@ExperimentalScriptKtApi
public interface ScriptsModuleBuilder {
    @UnsafeArgumentsInput
    public fun <T : Any> setBaseClass(kClass: KClass<T>, arguments: List<Any?>)
    public fun <T : Any> addImplicitReceiver(kClass: KClass<T>, instance: T)
    public fun <T : Any> provideProperty(name: String, kClass: KClass<T>, instance: T)
    public fun addClasspath(files: Collection<File>)
    public fun addScript(scriptKt: ScriptKt)
    public fun defaultImports(imports: List<String>)

    public val compilation: CompilationConfigurationBuilder
    public val evaluation: EvaluationConfigurationBuilder

    /**
     * Compilation configuration DSL. Used only for installing features due to safe dsl at [ScriptsModuleBuilder]
     */
    public interface CompilationConfigurationBuilder {
        /**
         * Compilation feature installing.
         * @param feature - feature to install.
         */
        public fun install(feature: CompilationFeature)
    }

    public interface EvaluationConfigurationBuilder {
        /**
         * Compilation feature installing.
         * @param feature - feature to install.
         */
        public fun install(feature: EvaluationFeature)
    }
}

@OptIn(ExperimentalScriptKtApi::class)
public fun ScriptsModuleBuilder.defaultImports(vararg imports: String) {
    defaultImports(imports.toList())
}

@OptIn(ExperimentalScriptKtApi::class)
public inline fun <reified T : Any> ScriptsModuleBuilder.provideProperty(name: String, instance: T) {
    provideProperty(name, T::class, instance)
}

@OptIn(ExperimentalScriptKtApi::class)
@UnsafeArgumentsInput
public inline fun <reified T : Any> ScriptsModuleBuilder.setBaseClass(arguments: List<Any?>) {
    setBaseClass(T::class, arguments)
}

@OptIn(ExperimentalScriptKtApi::class)
@UnsafeArgumentsInput
public inline fun <reified T : Any> ScriptsModuleBuilder.setBaseClass(vararg arguments: Any?) {
    setBaseClass(T::class, arguments.toList())
}

@OptIn(ExperimentalScriptKtApi::class)
@UnsafeArgumentsInput
public inline fun <reified T : Any> ScriptsModuleBuilder.addImplicitReceiver(instance: T) {
    addImplicitReceiver(T::class, instance)
}

@OptIn(ExperimentalScriptKtApi::class)
internal class ScriptsModuleBuilderImpl(
    val compilationConfiguration: MutableCompilationConfiguration,
    val evaluationConfiguration: MutableEvaluationConfiguration
) : ScriptsModuleBuilder {

    val otherScripts: MutableList<ScriptKt> = mutableListOf()

    @UnsafeArgumentsInput
    override fun <T : Any> setBaseClass(kClass: KClass<T>, arguments: List<Any?>) {
        compilationConfiguration.baseClass = kClass
        evaluationConfiguration.baseClassArguments.clear()
        evaluationConfiguration.baseClassArguments.addAll(arguments)
    }

    @OptIn(UnsafeArgumentsInput::class)
    override fun <T : Any> addImplicitReceiver(kClass: KClass<T>, instance: T) {
        compilationConfiguration.implicitReceivers += kClass
        evaluationConfiguration.implicitReceivers += instance
    }

    @OptIn(UnsafeArgumentsInput::class)
    override fun <T : Any> provideProperty(name: String, kClass: KClass<T>, instance: T) {
        compilationConfiguration.providedProperties[name] = kClass
        evaluationConfiguration.providedProperties[name] = instance
    }

    override fun addClasspath(files: Collection<File>) {
        compilationConfiguration.classpath.addAll(files)
    }

    override fun addScript(scriptKt: ScriptKt) {
        otherScripts += scriptKt
    }

    override fun defaultImports(imports: List<String>) {
        compilationConfiguration.defaultImports.addAll(imports)
    }

    override val compilation: ScriptsModuleBuilder.CompilationConfigurationBuilder =
        object : ScriptsModuleBuilder.CompilationConfigurationBuilder {
            override fun install(feature: CompilationFeature) {
                compilationConfiguration.features += feature
            }
        }
    override val evaluation: ScriptsModuleBuilder.EvaluationConfigurationBuilder =
        object : ScriptsModuleBuilder.EvaluationConfigurationBuilder {
            override fun install(feature: EvaluationFeature) {
                evaluationConfiguration.features += feature
            }
        }
}