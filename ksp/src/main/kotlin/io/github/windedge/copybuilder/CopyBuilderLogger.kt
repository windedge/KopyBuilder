package io.github.windedge.copybuilder

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode

class CopyBuilderLogger(private val kspLogger: KSPLogger, private val loggingType: Int) : KSPLogger by kspLogger {
    override fun error(message: String, symbol: KSNode?) {
        when (loggingType) {
            0 -> {
                //Do nothing
            }

            1 -> {
                //Throw compile errors for CopyBuilder
                kspLogger.error("CopyBuilder: $message", symbol!!)
            }

            2 -> {
                // Turn errors into compile warnings
                kspLogger.warn("CopyBuilder: $message", symbol!!)
            }
        }

    }

}