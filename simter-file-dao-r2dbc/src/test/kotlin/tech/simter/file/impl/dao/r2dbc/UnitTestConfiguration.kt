package tech.simter.file.impl.dao.r2dbc

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

/**
 * All configuration for this module.
 *
 * @author RJ
 */
@Configuration
@Import(
  tech.simter.r2dbc.R2dbcConfiguration::class,
  ModuleConfiguration::class
)
@ComponentScan("tech.simter.embeddeddatabase")
class UnitTestConfiguration