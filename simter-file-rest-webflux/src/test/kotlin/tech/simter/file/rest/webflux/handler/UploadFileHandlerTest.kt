package tech.simter.file.rest.webflux.handler

import com.nhaarman.mockito_kotlin.any
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType.MULTIPART_FORM_DATA
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient.bindToRouterFunction
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.server.RouterFunctions.route
import reactor.core.publisher.Mono
import tech.simter.file.po.Attachment
import tech.simter.file.rest.webflux.handler.UploadFileHandler.Companion.REQUEST_PREDICATE
import tech.simter.file.service.AttachmentService
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit.SECONDS
import java.util.*

/**
 * Test UploadFileHandler.
 *
 * @author JF
 * @author RJ
 */
@SpringJUnitConfig(UploadFileHandler::class)
@EnableWebFlux
@MockBean(AttachmentService::class)
@SpyBean(UploadFileHandler::class)
@TestPropertySource(properties = ["app.file.root=target/files"])
internal class UploadFileHandlerTest @Autowired constructor(
  private val service: AttachmentService,
  @Value("\${app.file.root}") private val fileRootDir: String,
  private val handler: UploadFileHandler
) {
  private val client = bindToRouterFunction(route(REQUEST_PREDICATE, handler)).build()

  @Test
  @Throws(IOException::class)
  fun upload() {
    // mock MultipartBody
    val name = "logback-test"
    val ext = "xml"
    val builder = MultipartBodyBuilder()
    val file = ClassPathResource("$name.$ext")
    builder.part("fileData", file)
    builder.part("puid", "puid")
    builder.part("subgroup", "1")
    val parts = builder.build()

    // mock service.create return value
    val id = UUID.randomUUID().toString()
    val fileSize = file.contentLength()
    val attachment = Attachment(id, "/data", name, ext,
      fileSize, OffsetDateTime.now(), "Simter", "puid", 1)
    `when`(service.create(any())).thenReturn(Mono.just(attachment))

    // mock handler.newId return value
    `when`(handler.newId()).thenReturn(id)

    // invoke request
    val now = LocalDateTime.now().truncatedTo(SECONDS)
    client.post().uri("/")
      .contentType(MULTIPART_FORM_DATA)
      .contentLength(fileSize)
      .syncBody(parts)
      .exchange()
      .expectStatus().isNoContent
      .expectHeader().valueEquals("Location", "/$id")

    // 1. verify service.create method invoked
    verify(service).create(any())

    // 2. verify the saved file exists
    val yyyyMM = now.format(DateTimeFormatter.ofPattern("yyyyMM"))
    val files = File("$fileRootDir/$yyyyMM").listFiles()
    assertNotNull(files)
    assertTrue(files.isNotEmpty())
    var actualFile: File? = null
    for (f in files) {
      // extract dateTime and id from fileName: yyyyMMddTHHmmss-{id}.{ext}
      val index = f.name.indexOf("-")
      val dateTime = LocalDateTime.parse(f.name.substring(0, index),
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))
      val uuid = f.name.substring(index + 1, f.name.lastIndexOf("."))
      if (id == uuid && !dateTime.isBefore(now)) {
        actualFile = f
        break
      }
    }
    assertNotNull(actualFile)

    // 3. verify the saved file size
    assertEquals(actualFile!!.length(), fileSize)

    // 4. TODO verify the attachment
  }
}