import dev.limebeck.example.res.Res
import kotlin.test.Test
import kotlin.test.assertEquals

class ResourcesTest {
    @Test
    fun `Receive resources`() {
        assertEquals(1, Res.items.size)
        assertEquals("image/png", Res.image.type)
        assertEquals("image.png", Res.image.name)
    }
}
