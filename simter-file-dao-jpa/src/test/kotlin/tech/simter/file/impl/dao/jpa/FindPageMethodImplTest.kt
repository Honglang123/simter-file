package tech.simter.file.impl.dao.jpa

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import reactor.kotlin.test.test
import tech.simter.file.core.AttachmentDao
import tech.simter.file.impl.dao.jpa.TestHelper.randomAttachmentId
import tech.simter.file.impl.dao.jpa.TestHelper.randomAttachmentPo
import tech.simter.reactive.test.jpa.ReactiveDataJpaTest
import tech.simter.reactive.test.jpa.TestEntityManager
import java.time.OffsetDateTime

/**
 * Test [AttachmentDao.find].
 *
 * @author RJ
 */
@SpringJUnitConfig(UnitTestConfiguration::class)
@ReactiveDataJpaTest
class FindPageMethodImplTest @Autowired constructor(
  val rem: TestEntityManager,
  val dao: AttachmentDao
) {
  @Test
  fun `found nothing`() {
    // clean
    rem.executeUpdate { it.createQuery("delete from AttachmentPo") }

    // invoke
    dao.find(PageRequest.of(0, 25))
      .test()
      .consumeNextWith { page ->
        assertTrue(page.content.isEmpty())
        assertEquals(0, page.number)
        assertEquals(25, page.size)
        assertEquals(0, page.totalPages)
        assertEquals(0, page.totalElements)
      }
      .verifyComplete()
  }

  @Test
  fun `found something`() {
    // clean
    rem.executeUpdate { it.createQuery("delete from AttachmentPo") }

    // prepare data
    val now = OffsetDateTime.now()
    val base = randomAttachmentPo()
    val po1 = base.copy(id = randomAttachmentId(), createOn = now.minusDays(1))
    val po2 = base.copy(id = randomAttachmentId(), createOn = now)
    rem.persist(po1, po2)

    // invoke
    val actual = dao.find(PageRequest.of(0, 25, Sort.by(Sort.Direction.DESC, "createOn")))

    // verify
    actual.test()
      .consumeNextWith { page ->
        assertEquals(0, page.number)
        assertEquals(25, page.size)
        assertEquals(1, page.totalPages)
        assertEquals(2L, page.totalElements)
        assertEquals(2, page.content.size)
        assertEquals(po2, page.content[0])
        assertEquals(po1, page.content[1])
      }
      .verifyComplete()
  }
}