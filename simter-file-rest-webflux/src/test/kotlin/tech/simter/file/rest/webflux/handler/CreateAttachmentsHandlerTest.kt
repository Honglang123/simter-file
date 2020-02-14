package tech.simter.file.rest.webflux.handler

import com.nhaarman.mockito_kotlin.anyVararg
import com.nhaarman.mockito_kotlin.verify
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient.bindToRouterFunction
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.server.RouterFunctions.route
import reactor.core.publisher.Flux
import reactor.core.publisher.toFlux
import tech.simter.exception.ForbiddenException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.domain.AttachmentDto4Create
import tech.simter.file.rest.webflux.Utils.randomInt
import tech.simter.file.rest.webflux.Utils.randomString
import tech.simter.file.rest.webflux.handler.CreateAttachmentsHandler.Companion.REQUEST_PREDICATE
import tech.simter.file.core.AttachmentService
import java.util.*

/**
 * Test [CreateAttachmentsHandler].
 *
 * @author zh
 */
@SpringJUnitConfig(CreateAttachmentsHandler::class)
@EnableWebFlux
@MockBean(AttachmentService::class)
internal class CreateAttachmentsHandlerTest @Autowired constructor(
  private val service: AttachmentService,
  handler: CreateAttachmentsHandler
) {
  private val client = bindToRouterFunction(route(REQUEST_PREDICATE, handler)).build()

  private fun randomAttachmentDto4Create(): AttachmentDto4Create {
    return AttachmentDto4Create().apply {
      id = UUID.randomUUID().toString()
      name = randomString("name")
      upperId = UUID.randomUUID().toString()
      path = randomString("path")
      puid = randomString("puid")
    }
  }

  @Test
  fun createMultiple() {
    // mock
    val dtos = List(randomInt(1, 3)) { randomAttachmentDto4Create() }
    val ids = dtos.map { it.id!! }
    `when`(service.create(anyVararg())).thenReturn(ids.toFlux())

    // invoke
    client.post().uri("/attachment").contentType(APPLICATION_JSON_UTF8).syncBody(dtos.map { it.data })
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(APPLICATION_JSON_UTF8)
      .expectBody().apply {
        ids.forEachIndexed { index, id ->
          jsonPath("$[$index]").isEqualTo(id)
        }
      }

    // verify
    verify(service).create(anyVararg())
  }

  @Test
  fun failedByPermissionDenied() {
    // mock
    val dtos = List(randomInt(1, 3)) { randomAttachmentDto4Create() }
    `when`(service.create(anyVararg())).thenReturn(Flux.error(PermissionDeniedException()))

    // invoke
    client.post().uri("/attachment").contentType(APPLICATION_JSON_UTF8).syncBody(dtos.map { it.data })
      .exchange().expectStatus().isForbidden

    // verify
    verify(service).create(anyVararg())
  }

  @Test
  fun failedByAcrossModule() {
    // mock
    val dtos = List(randomInt(1, 3)) { randomAttachmentDto4Create() }
    `when`(service.create(anyVararg())).thenReturn(Flux.error(ForbiddenException()))

    // invoke
    client.post().uri("/attachment").contentType(APPLICATION_JSON_UTF8).syncBody(dtos.map { it.data })
      .exchange().expectStatus().isForbidden

    // verify
    verify(service).create(anyVararg())
  }
}