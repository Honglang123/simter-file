package tech.simter.file.dao.jpa

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import tech.simter.exception.NotFoundException
import tech.simter.file.dao.AttachmentDao
import tech.simter.file.dto.AttachmentDtoWithChildren
import tech.simter.file.po.Attachment
import java.io.File
import javax.persistence.EntityManager
import javax.persistence.NoResultException
import javax.persistence.PersistenceContext
import javax.persistence.Query

/**
 * The JPA implementation of [AttachmentDao].
 *
 * @author RJ
 * @author zh
 */
@Component
class AttachmentDaoImpl @Autowired constructor(
  @Value("\${simter.file.root}") private val fileRootDir: String,
  @PersistenceContext private val em: EntityManager,
  private val repository: AttachmentJpaRepository
) : AttachmentDao {
  override fun update(id: String, data: Map<String, Any?>): Mono<Void> {
    return if (data.isEmpty()) {
      Mono.empty()
    } else {
      Mono.fromSupplier {
        em.createQuery("update Attachment set ${data.keys.joinToString(", ") { "$it = :$it" }} where id =:id")
          .apply { data.forEach { key, value -> setParameter(key, value) } }
          .setParameter("id", id)
          .executeUpdate()
      }.delayUntil { em.clear().toMono() }
        .flatMap { if (it > 0) Mono.empty<Void>() else Mono.error(NotFoundException()) }
    }
  }

  override fun findDescendents(id: String): Flux<AttachmentDtoWithChildren> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getFullPath(id: String): Mono<String> {
    val sql = """
      with recursive p(id, path, upper_id) as (
        select id, concat(path, ''), upper_id from st_attachment where id = :id
        union
        select a.id, concat(a.path, '/', p.path), a.upper_id from st_attachment as a
        join p on a.id = p.upper_id
      )
      select p.path from p where upper_id is null
    """.trimIndent()
    return Mono.fromSupplier { em.createNativeQuery(sql).setParameter("id", id).singleResult as String }
      .onErrorResume(NoResultException::class.java) { Mono.empty() }
  }

  override fun get(id: String): Mono<Attachment> {
    return Mono.justOrEmpty(repository.findById(id))
  }

  override fun find(pageable: Pageable): Mono<Page<Attachment>> {
    return Mono.justOrEmpty(repository.findAll(pageable))
  }

  @Suppress("UNCHECKED_CAST")
  override fun find(puid: String, upperId: String?): Flux<Attachment> {
    val hasSubgroup = null != upperId
    val sql = """
      select a from Attachment a
      where puid = :puid ${if (hasSubgroup) "and upperId = :upperId" else ""}
      order by createOn desc
    """.trimIndent()
    val query: Query = em.createQuery(sql, Attachment::class.java).setParameter("puid", puid)
    if (hasSubgroup) query.setParameter("upperId", upperId)
    return Flux.fromIterable(query.resultList as List<Attachment>)
  }

  override fun save(vararg attachments: Attachment): Mono<Void> {
    repository.saveAll(attachments.asIterable())
    return Mono.empty()
  }

  override fun delete(vararg ids: String): Mono<Void> {
    if (!ids.isEmpty()) {
      val attachments = repository.findAllById(ids.toList())
      if (!attachments.isEmpty()) {
        repository.deleteAll(attachments)
        attachments.forEach { File("$fileRootDir/${it.path}").delete() }
      }
    }
    return Mono.empty<Void>()
  }
}
