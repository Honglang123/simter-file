package tech.simter.file.impl.dao.r2dbc

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.r2dbc.core.DatabaseClient
import reactor.kotlin.test.test
import tech.simter.file.core.FileDao
import tech.simter.file.impl.dao.r2dbc.TestHelper.insert
import tech.simter.file.test.TestHelper.randomFileId

/**
 * Test [FileDaoImpl.get]
 *
 * @author RJ
 */
@SpringBootTest(classes = [UnitTestConfiguration::class])
class GetMethodImplTest @Autowired constructor(
  private val client: DatabaseClient,
  private val dao: FileDao
) {
  @Test
  fun `get nothing`() {
    dao.get(randomFileId()).test().verifyComplete()
  }

  @Test
  fun `get it`() {
    // prepare data
    val file = insert(client = client)

    // verify exists
    dao.get(file.id)
      .test()
      .expectNext(file)
      .verifyComplete()
  }
}