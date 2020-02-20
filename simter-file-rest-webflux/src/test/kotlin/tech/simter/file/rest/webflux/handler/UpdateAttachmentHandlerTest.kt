package tech.simter.file.rest.webflux.handler

import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono
import tech.simter.exception.ForbiddenException
import tech.simter.exception.NotFoundException
import tech.simter.exception.PermissionDeniedException
import tech.simter.file.core.AttachmentService
import tech.simter.file.core.domain.AttachmentUpdateInfo
import tech.simter.file.impl.domain.AttachmentUpdateInfoImpl
import tech.simter.file.rest.webflux.TestHelper.randomString
import tech.simter.file.rest.webflux.UnitTestConfiguration
import java.util.*

/**
 * Test [UpdateAttachmentHandler].
 *
 * @author zh
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@WebFluxTest
class UpdateAttachmentHandlerTest @Autowired constructor(
  private val client: WebTestClient,
  private val service: AttachmentService
) {
  private val id = UUID.randomUUID().toString()
  private val url = "/attachment/$id"

  private fun randomAttachmentDto4Update(): AttachmentUpdateInfo {
    return AttachmentUpdateInfoImpl().apply {
      name = randomString("name")
      upperId = UUID.randomUUID().toString()
      puid = randomString("puid")
    }
  }

  @Test
  fun success() {
    val dto = randomAttachmentDto4Update()
    every { service.update(id, dto) } returns Mono.empty()

    // invoke request
    client.patch().uri(url)
      .contentType(APPLICATION_JSON)
      .bodyValue(dto.data)
      .exchange()
      .expectStatus().isNoContent

    // verify
    verify { service.update(id, dto) }
  }

  @Test
  fun `Found nothing`() {
    // mock
    val dto = randomAttachmentDto4Update()
    every { service.update(id, dto) } returns Mono.error(NotFoundException(""))

    // invoke request
    client.patch().uri(url)
      .contentType(APPLICATION_JSON)
      .bodyValue(dto.data)
      .exchange()
      .expectStatus().isNotFound

    // verify
    verify { service.update(id, dto) }
  }

  @Test
  fun `Failed by permission denied`() {
    // mock
    val dto = randomAttachmentDto4Update()
    every { service.update(id, dto) } returns Mono.error(PermissionDeniedException(""))

    // invoke request
    client.patch().uri(url)
      .contentType(APPLICATION_JSON)
      .bodyValue(dto.data)
      .exchange()
      .expectStatus().isForbidden

    // verify
    verify { service.update(id, dto) }
  }

  @Test
  fun `Failed by across module`() {
    // mock
    val dto = randomAttachmentDto4Update()
    every { service.update(id, dto) } returns Mono.error(ForbiddenException(""))

    // invoke request
    client.patch().uri(url)
      .contentType(APPLICATION_JSON)
      .bodyValue(dto.data)
      .exchange()
      .expectStatus().isForbidden

    // verify
    verify { service.update(id, dto) }
  }
}