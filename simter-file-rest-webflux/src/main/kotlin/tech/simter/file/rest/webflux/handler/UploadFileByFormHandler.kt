package tech.simter.file.rest.webflux.handler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.FormFieldPart
import org.springframework.http.codec.multipart.Part
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicate
import org.springframework.web.reactive.function.server.RequestPredicates.POST
import org.springframework.web.reactive.function.server.RequestPredicates.contentType
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.noContent
import reactor.core.publisher.Mono
import tech.simter.file.po.Attachment
import tech.simter.file.service.AttachmentService
import java.io.File
import java.net.URI
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.HashMap


/**
 * The [HandlerFunction] for upload file by from.
 *
 * Request: (form submit with <input type="file">)
 *
 * ```
 * POST {context-path}/
 * Content-Type        : multipart/form-data; boundary=----{boundary}
 * Content-Length      : {len}
 *
 * ------{boundary}
 * Content-Disposition: form-data; name="{input-name}"; filename="{file-name}.{type}"
 * Content-Type: {media-type}
 *
 * {file-data}
 * ------{boundary}
 * Content-Disposition: form-data; name="puid"
 *
 * {puid}
 * ------{boundary}
 * Content-Disposition: form-data; name="upperId"
 *
 * {upperId}
 * ------{boundary}--
 * ```
 *
 * Response:
 *
 * ```
 * 204 No Content
 * Location : {context-path}/{id}
 * ```
 *
 * [More](https://github.com/simter/simter-file/wiki/Upload-One-File-By-Form)
 *
 * @author JF
 * @author RJ
 */
@Component
class UploadFileByFormHandler @Autowired constructor(
  @Value("\${simter.file.root}") private val fileRootDir: String,
  private val attachmentService: AttachmentService
) : HandlerFunction<ServerResponse> {
  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return request
      .bodyToFlux(Part::class.java)
      .filter { it is FilePart || it is FormFieldPart }
      .collectList()
      // 1. extract data in request body
      .map {
        // build Map by data in list
        val formDataMap = HashMap<String, Any>()
        for (part in it) {
          if (part is FormFieldPart && "puid" == part.name()) {
            formDataMap["puid"] = if (part.value() != "") part.value() else ""
          }
          if (part is FormFieldPart && "upperId" == part.name()) {
            formDataMap["upperId"] = if (part.value().matches(Regex("\\d+"))) part.value() else "EMPTY"
          }
          if (part is FilePart) formDataMap["fileData"] = part
        }
        formDataMap
      }
      // 2. save file to disk
      .flatMap {
        // get the FilePart by the Map
        val fileData = it["fileData"] as FilePart
        // convert to Attachment instance
        val attachment = toAttachment(fileData.headers().contentLength, fileData.filename(), it["puid"] as String, it["upperId"] as String)

        val file = File("$fileRootDir/${attachment.path}")
        val fileDir = file.parentFile
        if (!fileDir.exists()) {
          if (!fileDir.mkdirs())  // create file directory if not exists
            throw IllegalAccessException("Failed to create parents dir: ${fileDir.absolutePath}")
        }
        if (!file.createNewFile()) throw IllegalAccessException("Failed to create file: ${file.absolutePath}")

        // save to disk
        fileData.transferTo(file)
          .then(Mono.just(if (attachment.size != -1L) attachment else attachment.copy(size = file.length())))
      }
      // 3. save attachment
      .flatMap { attachmentService.save(it).thenReturn(it) }
      // 4. return response
      .flatMap { noContent().location(URI.create("/${it.id}")).build() }
  }

  private fun toAttachment(fileSize: Long, filename: String, puid: String, upperId: String): Attachment {
    val id = newId()
    val now = OffsetDateTime.now()
    val lastDotIndex = filename.lastIndexOf(".")
    val type = filename.substring(lastDotIndex + 1)
    val path = "${now.format(DateTimeFormatter.ofPattern("yyyyMM"))}/${now.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"))}-$id.$type"
    return Attachment(id = id, path = path, name = filename.substring(0, lastDotIndex), type = type, size = fileSize,                               // file size
      createOn = now, creator = "Simter", modifyOn = now, modifier = "Simter", puid = puid, upperId = upperId)
  }

  /** Generate a new [Attachment] id */
  fun newId() = UUID.randomUUID().toString()

  companion object {
    /** The default [RequestPredicate] */
    val REQUEST_PREDICATE: RequestPredicate = POST("/").and(contentType(MediaType.MULTIPART_FORM_DATA))
  }
}